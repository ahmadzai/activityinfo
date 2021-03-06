package org.activityinfo.server.endpoint.odk;

import com.google.api.client.util.Maps;
import com.google.appengine.repackaged.com.google.api.client.util.Strings;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import com.google.inject.Inject;
import org.activityinfo.legacy.shared.command.CreateLocation;
import org.activityinfo.legacy.shared.command.result.VoidResult;
import org.activityinfo.model.auth.AuthenticatedUser;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.legacy.KeyGenerator;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.FieldType;
import org.activityinfo.model.type.FieldValue;
import org.activityinfo.model.type.ReferenceType;
import org.activityinfo.model.type.ReferenceValue;
import org.activityinfo.model.type.attachment.Attachment;
import org.activityinfo.model.type.attachment.AttachmentValue;
import org.activityinfo.model.type.geo.GeoPoint;
import org.activityinfo.model.type.geo.GeoPointType;
import org.activityinfo.model.type.primitive.TextValue;
import org.activityinfo.server.authentication.ServerSideAuthProvider;
import org.activityinfo.server.command.DispatcherSync;
import org.activityinfo.server.command.ResourceLocatorSync;
import org.activityinfo.server.database.hibernate.entity.Activity;
import org.activityinfo.server.database.hibernate.entity.User;
import org.activityinfo.server.endpoint.odk.xform.LegacyXFormInstance;
import org.activityinfo.server.endpoint.odk.xform.XFormInstance;
import org.activityinfo.server.endpoint.odk.xform.XFormInstanceImpl;
import org.activityinfo.service.blob.BlobId;
import org.activityinfo.service.blob.GcsBlobFieldStorageService;
import org.w3c.dom.Element;

import javax.inject.Provider;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.*;
import static org.activityinfo.model.legacy.CuidAdapter.*;
import static org.activityinfo.server.endpoint.odk.OdkFieldValueParserFactory.fromFieldType;
import static org.activityinfo.server.endpoint.odk.OdkHelper.isLocation;

@Path("/submission")
public class FormSubmissionResource {

    private static final Logger LOGGER = Logger.getLogger(FormSubmissionResource.class.getName());

    final private DispatcherSync dispatcher;
    final private ResourceLocatorSync locator;
    final private AuthenticationTokenService authenticationTokenService;
    final private ServerSideAuthProvider authProvider;                  // Necessary for 2.8 XForms, remove afterwards
    final private Provider<EntityManager> entityManager;                  // Necessary for 2.8 XForms, remove afterwards
    final private GcsBlobFieldStorageService blobFieldStorageService;
    final private InstanceIdService instanceIdService;
    final private SubmissionArchiver submissionArchiver;

    @Inject
    public FormSubmissionResource(DispatcherSync dispatcher,
                                  ResourceLocatorSync locator,
                                  AuthenticationTokenService authenticationTokenService,
                                  ServerSideAuthProvider authProvider,  // Necessary for 2.8 XForms, remove afterwards
                                  Provider<EntityManager> entityManager,  // Necessary for 2.8 XForms, remove afterwards
                                  GcsBlobFieldStorageService blobFieldStorageService,
                                  InstanceIdService instanceIdService,
                                  SubmissionArchiver submissionArchiver) {
        this.dispatcher = dispatcher;
        this.locator = locator;
        this.authenticationTokenService = authenticationTokenService;
        this.authProvider = authProvider;                               // Necessary for 2.8 XForms, remove afterwards
        this.entityManager = entityManager;                             // Necessary for 2.8 XForms, remove afterwards
        this.blobFieldStorageService = blobFieldStorageService;
        this.instanceIdService = instanceIdService;
        this.submissionArchiver = submissionArchiver;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_XML)
    public Response submit(byte bytes[]) {
        final boolean legacy;                                           // Necessary for 2.8 XForms, remove afterwards
        AuthenticatedUser user;
        XFormInstance instance;
        FormClass formClass;

        try {
            instance = new XFormInstanceImpl(bytes);
            user = authenticationTokenService.authenticate(instance.getAuthenticationToken());
            formClass = locator.getFormClass(instance.getFormClassId());
        } catch (IllegalStateException illegalStateException) {         // Necessary for 2.8 XForms, remove afterwards
            if ("Cannot find element userID".equals(illegalStateException.getMessage())) {
                LOGGER.log(Level.INFO, "User ID not found, trying to parse submission as legacy form instance");
                instance = new LegacyXFormInstance(bytes);
                int formClassId = getLegacyIdFromCuid(instance.getFormClassId());
                User owner = entityManager.get().find(Activity.class, formClassId).getDatabase().getOwner();
                authProvider.set(owner);
                user = new AuthenticatedUser("", (int) owner.getId(), "@");
                formClass = locator.getFormClass(instance.getFormClassId());
            } else throw illegalStateException;                         // Legacy code ends here
        }

        legacy = instance instanceof LegacyXFormInstance;               // Necessary for 2.8 XForms, remove afterwards

        ResourceId formId = newLegacyFormInstanceId(formClass.getId());
        FormInstance formInstance = new FormInstance(formId, formClass.getId());
        String instanceId = instance.getId();

        LOGGER.log(Level.INFO, "Saving XForm " + instance.getId() + " as " + formId);

        for (FormField formField : formClass.getFields()) {
            Optional<Element> element = instance.getFieldContent(formField.getId());
            if (element.isPresent()) {
                formInstance.set(formField.getId(), tryParse(formInstance, formField, element.get(), legacy));
            } else if (isLocation(formClass, formField)) {
                FieldType fieldType = formField.getType();
                Optional<Element> gpsField = instance.getFieldContent(field(formClass.getId(), GPS_FIELD));
                Optional<Element> nameField = instance.getFieldContent(field(formClass.getId(), LOCATION_NAME_FIELD));

                if (fieldType instanceof ReferenceType && gpsField.isPresent() && nameField.isPresent()) {

                    ResourceId locationFieldId = field(formClass.getId(), LOCATION_FIELD);
                    int newLocationId = new KeyGenerator().generateInt();
                    ResourceId locationFormClassId = Iterables.getOnlyElement(((ReferenceType) fieldType).getRange());
                    int locationTypeId = getLegacyIdFromCuid(locationFormClassId);
                    FieldValue fieldValue = new ReferenceValue(locationInstanceId(newLocationId));
                    String name = OdkHelper.extractText(nameField.get());

                    if (Strings.isNullOrEmpty(name)) {
                        throw new WebApplicationException(
                                Response.status(BAD_REQUEST).
                                        entity("Name value for location field is blank. ").
                                        build());
                    }

                    GeoPoint geoPoint = parseLocation(gpsField.get(), legacy);

                    formInstance.set(locationFieldId, fieldValue);
                    createLocation(newLocationId, locationTypeId, name, geoPoint);
                }
            }
        }

        if (!instanceIdService.exists(instanceId)) {
            for (FieldValue fieldValue : formInstance.getFieldValueMap().values()) {
                if (fieldValue instanceof AttachmentValue) {
                    persist(user, instance, (AttachmentValue) fieldValue);
                }
            }

            locator.persist(formInstance);
            instanceIdService.submit(instanceId);
        }

        // Backup the original XForm in case something went wrong with processing
        submissionArchiver.backup(formClass.getId(), formId, ByteSource.wrap(bytes));

        return Response.status(CREATED).build();
    }

    private FieldValue tryParse(FormInstance formInstance, FormField formField, Element element, boolean legacy) {
        try {
            OdkFieldValueParser odkFieldValueParser = fromFieldType(formField.getType(), legacy);
            return odkFieldValueParser.parse(element);

        } catch (Exception e) {
            String text = OdkHelper.extractText(element);

            if (text == null) {
                LOGGER.log(Level.SEVERE, "Malformed Element in form instance prevents parsing", e);
            } else if (!text.equals("")) {
                LOGGER.log(Level.WARNING, "Can't parse form instance contents, storing as text", e);
                formInstance.set(formField.getId(), TextValue.valueOf(text));
            }
        }
        return null;
    }

    private GeoPoint parseLocation(Element element, boolean legacy) {
        try {
            OdkFieldValueParser odkFieldValueParser = fromFieldType(GeoPointType.INSTANCE, legacy);
            return (GeoPoint) odkFieldValueParser.parse(element);
        } catch (Exception e) {
            return null;
        }
    }

    private void persist(AuthenticatedUser user, XFormInstance instance, AttachmentValue fieldValue) {
        Attachment attachment = fieldValue.getValues().get(0);
        if (attachment.getFilename() != null) {
            try {
                BodyPart bodyPart = ((XFormInstanceImpl) instance).findBodyPartByFilename(attachment.getFilename());

                String mimeType = bodyPart.getContentType();
                attachment.setMimeType(mimeType);

                blobFieldStorageService.put(user, bodyPart.getDisposition(), mimeType,
                        new BlobId(attachment.getBlobId()), instance.getFormClassId(),
                        bodyPart.getInputStream());

            } catch (MessagingException messagingException) {
                LOGGER.log(Level.SEVERE, "Unable to parse input", messagingException);
                throw new WebApplicationException(Response.status(BAD_REQUEST).build());
            } catch (IOException ioException) {
                LOGGER.log(Level.SEVERE, "Could not write attachment to GCS", ioException);
                throw new WebApplicationException(Response.status(SERVICE_UNAVAILABLE).build());
            }
        }
    }

    private VoidResult createLocation(int id, int locationTypeId, String name, GeoPoint geoPoint) {
        Map<String, Object> properties = Maps.newHashMap();

        properties.put("id", id);
        properties.put("locationTypeId", locationTypeId);
        properties.put("name", name);

        if (geoPoint != null) {
            properties.put("latitude", geoPoint.getLatitude());
            properties.put("longitude", geoPoint.getLongitude());
        }

        return dispatcher.execute(new CreateLocation(properties));
    }
}
