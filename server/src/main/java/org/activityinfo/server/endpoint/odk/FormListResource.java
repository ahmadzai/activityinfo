package org.activityinfo.server.endpoint.odk;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.activityinfo.server.command.DispatcherSync;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/formList")
public class FormListResource {

    private final FormLister formLister;

    @Inject
    public FormListResource(OdkAuthProvider authProvider, DispatcherSync dispatcher) {
        this.formLister = new FormLister(authProvider, dispatcher);
    }

    @GET
    @Produces(MediaType.TEXT_XML)
    public Response formList(@Context UriInfo uri) throws Exception {
        return formLister.formList(uri, Optional.<Integer>absent());
    }
}