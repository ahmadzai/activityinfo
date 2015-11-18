package org.activityinfo.service.blob;

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.images.*;
import com.google.appengine.tools.cloudstorage.*;
import com.google.appengine.tools.cloudstorage.GcsFileOptions.Builder;
import com.google.apphosting.api.ApiProxy;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.sun.jersey.api.core.InjectParam;
import org.activityinfo.model.auth.AuthenticatedUser;
import org.activityinfo.model.resource.Record;
import org.activityinfo.model.resource.Resources;
import org.activityinfo.server.DeploymentEnvironment;
import org.activityinfo.server.util.blob.DevAppIdentityService;
import org.activityinfo.service.DeploymentConfiguration;
import org.activityinfo.service.gcs.GcsAppIdentityServiceUrlSigner;
import org.joda.time.Period;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

@Path("/service/blob")
public class GcsBlobFieldStorageService implements BlobFieldStorageService {

    private static final int ONE_MEGABYTE = 1 << 20;
    private static final Logger LOGGER = Logger.getLogger(GcsBlobFieldStorageService.class.getName());

    public static final int MAX_BLOB_LENGTH_IN_MEGABYTES = 100;

    private String bucketName;
    private AppIdentityService appIdentityService;

    @Inject
    public GcsBlobFieldStorageService(DeploymentConfiguration config) {
        this.bucketName = config.getBlobServiceBucketName();

        this.appIdentityService = DeploymentEnvironment.isAppEngineDevelopment() ?
                new DevAppIdentityService(config) : AppIdentityServiceFactory.getAppIdentityService();

        try {
            LOGGER.info("Service account: " + appIdentityService.getServiceAccountName());
        } catch (ApiProxy.CallNotFoundException e) {
            // ignore: fails in local tests, bug in LocalServiceTestHelper?
        }
    }

    @Override
    public URI getBlobUrl(BlobId blobId) {
        GcsAppIdentityServiceUrlSigner signer = new GcsAppIdentityServiceUrlSigner();
        try {
            return new URI(signer.getSignedUrl("GET", bucketName + "/" + blobId.asString()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void put(AuthenticatedUser authenticatedUser, String contentDisposition, String mimeType, BlobId blobId,
                    ByteSource byteSource) throws IOException {
        GcsFilename gcsFilename = new GcsFilename(bucketName, blobId.asString());
        GcsService gcsService = GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());

        GcsFileOptions gcsFileOptions = new Builder().
                contentDisposition(contentDisposition).
                mimeType(mimeType).
                build();
        GcsOutputChannel channel = gcsService.createOrReplace(gcsFilename, gcsFileOptions);

        try (OutputStream outputStream = Channels.newOutputStream(channel)) {
            byteSource.copyTo(outputStream);
        }
    }

    @GET
    @Path("{blobId}/image")
    @Override
    public Response getImage(@InjectParam AuthenticatedUser user,
                             @PathParam("blobId") BlobId blobId) throws IOException {
        GcsFilename gcsFilename = new GcsFilename(bucketName, blobId.asString());
        GcsService gcsService = GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());
        GcsInputChannel gcsInputChannel = gcsService.openPrefetchingReadChannel(gcsFilename, 0, ONE_MEGABYTE);
        GcsFileMetadata metadata = gcsService.getMetadata(gcsFilename);

        try (InputStream inputStream = Channels.newInputStream(gcsInputChannel)) {
            return Response.ok(ByteStreams.toByteArray(inputStream)).type(metadata.getOptions().getMimeType()).build();
        }
    }

    @GET
    @Path("{blobId}/image_url")
    @Override
    public Response getImageUrl(@InjectParam AuthenticatedUser user,
                                @PathParam("blobId") BlobId blobId,
                                @QueryParam("image_size") int imageSize) throws IOException {

        BlobKey blobKey = blobKey(blobId);
        ImagesService imagesService = ImagesServiceFactory.getImagesService();

        String scaledUrl = imagesService.getServingUrl(ServingUrlOptions.Builder.withBlobKey(blobKey).imageSize(imageSize));
        String originalUrl = imagesService.getServingUrl(ServingUrlOptions.Builder.withBlobKey(blobKey));

        Record record = new Record();
        record.set("scaled_url", scaledUrl);
        record.set("original_url", originalUrl);

        return Response.ok(Resources.toJsonObject(record).toString()).type(MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("{blobId}/thumbnail")
    @Override
    public Response getThumbnail(@InjectParam AuthenticatedUser user,
                                 @PathParam("blobId") BlobId blobId,
                                 @QueryParam("width") int width,
                                 @QueryParam("height") int height) {

        ImagesService imagesService = ImagesServiceFactory.getImagesService();

        Image image = ImagesServiceFactory.makeImageFromBlob(blobKey(blobId));

        Transform resize = ImagesServiceFactory.makeResize(width, height);
        Image newImage = imagesService.applyTransform(resize, image);

        String mimeType = "image/" + newImage.getFormat().name().toLowerCase();
        return Response.ok(newImage.getImageData()).type(mimeType).build();
    }

    private BlobKey blobKey(BlobId blobId) {
        BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
        return blobstoreService.createGsBlobKey("/gs/" + bucketName + "/" + blobId.asString());
    }

    @POST
    @Path("credentials/{blobId}")
    @Override
    public Response getUploadCredentials(@InjectParam AuthenticatedUser user,
                                         @PathParam("blobId") BlobId blobId) {
        if (user == null || user.isAnonymous()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }

        UploadCredentials uploadCredentials = new GcsUploadCredentialBuilder(appIdentityService).
                setBucket(bucketName).
                setKey(blobId.asString()).
                setMaxContentLengthInMegabytes(MAX_BLOB_LENGTH_IN_MEGABYTES).
                expireAfter(Period.minutes(10)).
                build();

        return Response.ok(uploadCredentials.asJson()).
                type(MediaType.APPLICATION_JSON).
                build();
    }

    public void setTestBucketName() {
        Preconditions.checkState(bucketName == null);
        bucketName = appIdentityService.getDefaultGcsBucketName();
    }

}
