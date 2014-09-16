package org.activityinfo.store.hrd;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.ImplicitTransactionManagementPolicy;
import com.google.apphosting.api.ApiProxy;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.jersey.api.core.InjectParam;
import org.activityinfo.model.auth.AccessControlRule;
import org.activityinfo.model.auth.AuthenticatedUser;
import org.activityinfo.model.resource.FolderProjection;
import org.activityinfo.model.resource.Resource;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.resource.ResourceNode;
import org.activityinfo.model.resource.Resources;
import org.activityinfo.service.store.FolderRequest;
import org.activityinfo.service.store.ResourceNotFound;
import org.activityinfo.service.store.ResourceStore;
import org.activityinfo.service.store.UpdateResult;
import org.activityinfo.store.hrd.entity.ReadTransaction;
import org.activityinfo.store.hrd.entity.Snapshot;
import org.activityinfo.store.hrd.entity.UpdateTransaction;
import org.activityinfo.store.hrd.entity.Workspace;
import org.activityinfo.store.hrd.entity.WorkspaceTransaction;
import org.activityinfo.store.hrd.index.AcrIndex;
import org.activityinfo.store.hrd.index.WorkspaceIndex;
import org.activityinfo.store.hrd.index.WorkspaceLookup;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

public class HrdResourceStore implements ResourceStore {
    private final static long TIME_LIMIT_MILLISECONDS = 10 * 1000L;


    private final DatastoreService datastore;
    private final ClientIdProvider clientIdProvider = new ClientIdProvider();
    private final WorkspaceLookup workspaceLookup = new WorkspaceLookup();

    public HrdResourceStore() {
        this.datastore = DatastoreServiceFactory.getDatastoreService(DatastoreServiceConfig.Builder
            .withImplicitTransactionManagementPolicy(ImplicitTransactionManagementPolicy.NONE));
    }

    @Override
    public long generateClientId(AuthenticatedUser user) {
        return clientIdProvider.getNext();
    }


    private WorkspaceTransaction begin(Workspace workspace, AuthenticatedUser user) {
        return new UpdateTransaction(workspace, datastore, user);
    }


    private WorkspaceTransaction beginRead(Workspace workspace, AuthenticatedUser user) {
        return new ReadTransaction(workspace, datastore, user);
    }


    @GET
    @Path("resource/{id}")
    @Produces("application/json")
    @Override
    public Resource get(@InjectParam AuthenticatedUser user, @PathParam("id") ResourceId resourceId) {
        try {
            Workspace workspace = workspaceLookup.lookup(resourceId);
            try(WorkspaceTransaction tx = beginRead(workspace, user)) {
                for (AccessControlRule acr : AcrIndex.queryRules(tx, resourceId)) {
                    final Boolean access = hasAccess(acr, user, resourceId);
                    if (access != null) {
                        if (access) {
                            return workspace.getLatestContent(resourceId).get(tx);
                        } else {
                            throw new WebApplicationException(UNAUTHORIZED);
                        }
                    }
                }

                throw new WebApplicationException(UNAUTHORIZED);
            }
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFound(resourceId);
        }
    }



    @Override
    public List<Resource> getAccessControlRules(@InjectParam AuthenticatedUser user,
                                                @PathParam("id") ResourceId resourceId) {

         return Lists.newArrayList(AcrIndex.queryRules(datastore, resourceId));
    }

    @Override
    public UpdateResult put(@InjectParam AuthenticatedUser user,
                            @PathParam("id") ResourceId resourceId,
                            Resource resource) {

       return put(user, resource);
    }


    @Override
    public UpdateResult put(AuthenticatedUser user, Resource resource) {
        long newVersion;

        Workspace workspace = workspaceLookup.lookup(resource.getId());

        try (WorkspaceTransaction tx = begin(workspace, user)) {
            for (AccessControlRule acr : AcrIndex.queryRules(tx, resource.getId())) {
                final Boolean access = hasAccess(acr, user, resource.getId());
                if (access != null) {
                    if (access) {
                        newVersion = workspace.createResource(tx, resource);
                        tx.commit();
                        return UpdateResult.committed(resource.getId(), newVersion);
                    } else {
                        throw new WebApplicationException(UNAUTHORIZED);
                    }
                }
            }

            throw new WebApplicationException(UNAUTHORIZED);
        }
    }

    @Override
    public UpdateResult create(AuthenticatedUser user, Resource resource) {
        long newVersion;

        if(resource.getOwnerId().equals(Resources.ROOT_ID)) {
            Workspace workspace = new Workspace(resource.getId());
            try(WorkspaceTransaction tx = begin(workspace, user)) {
                newVersion = workspace.createWorkspace(tx, resource);
                tx.commit();
                return UpdateResult.committed(resource.getId(), newVersion);
            }

        } else {

            Workspace workspace = workspaceLookup.lookup(resource.getOwnerId());

            try (WorkspaceTransaction tx = begin(workspace, user)) {
                for (AccessControlRule acr : AcrIndex.queryRules(tx, resource.getId())) {
                    final Boolean access = hasAccess(acr, user, resource.getId());
                    if (access != null) {
                        if (access) {
                            try {
                                workspace.getLatestContent(resource.getId()).get(tx);
                                return UpdateResult.rejected();
                            } catch (EntityNotFoundException e) {
                                newVersion = workspace.createResource(tx, resource);
                                tx.commit();

                                // Cache immediately so that subsequent will be able to find the resource
                                // if it takes a while for the indices to catch up
                                workspaceLookup.cache(resource.getId(), workspace);

                                return UpdateResult.committed(resource.getId(), newVersion);
                            }
                        } else {
                            throw new WebApplicationException(UNAUTHORIZED);
                        }
                    }
                }
            }

            throw new WebApplicationException(UNAUTHORIZED);
        }
    }


    @Override
    public FolderProjection queryTree(@InjectParam AuthenticatedUser user,
                                      FolderRequest request) {

        Workspace workspace = workspaceLookup.lookup(request.getRootId());

        try(WorkspaceTransaction tx = beginRead(workspace, user)) {

            ResourceNode rootNode = workspace.getLatestContent(request.getRootId()).getAsNode(tx);
            rootNode.getChildren().addAll(workspace.getFolderIndex().queryFolderItems(tx, rootNode.getId()));

            return new FolderProjection(rootNode);

        } catch (EntityNotFoundException e) {
            throw new ResourceNotFound(request.getRootId());
        }
    }

    @Override
    public List<ResourceNode> getOwnedOrSharedWorkspaces(@InjectParam AuthenticatedUser user) {
        return WorkspaceIndex.queryUserWorkspaces(datastore, user, workspaceLookup);
    }


    // TODO Authorization must be added, the requested ResourceGroup must be respected, etc.
    public List<Resource> getUpdates(@InjectParam AuthenticatedUser user, ResourceId workspaceId, long version) {
        ApiProxy.Environment environment = ApiProxy.getCurrentEnvironment();
        Map<ResourceId, Snapshot> snapshots = Maps.newLinkedHashMap();

        Workspace workspace = new Workspace(workspaceId);

        try(WorkspaceTransaction tx = beginRead(workspace, user)) {

            for (Snapshot snapshot : Snapshot.getSnapshotsAfter(tx, version)) {

                // We want the linked list to be sorted based on the most recent insertion of a resource
                snapshots.remove(snapshot.getResourceId());
                snapshots.put(snapshot.getResourceId(), snapshot);

                if (environment.getRemainingMillis() < TIME_LIMIT_MILLISECONDS) {
                    break;
                }
            }

            try {
                List<Resource> resources = Lists.newArrayListWithCapacity(snapshots.size());

                for (Snapshot snapshot : snapshots.values()) {
                    Resource resource = snapshot.get(tx);
                    if (AccessControlRule.CLASS_ID.toString().equals(resource.get("classId"))) {
                        final Boolean access = hasAccess(null, user, resource);
                        if (access != null && access) resources.add(resource);
                    } else {
                        for (AccessControlRule acr : AcrIndex.queryRules(tx, resource.getId())) {
                            final Boolean access = hasAccess(acr, user, resource);
                            if (access == null) continue;
                            else if (access) resources.add(resource);
                            else break;
                        }
                    }
                }

                return resources;
            } catch (EntityNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static Boolean hasAccess(AccessControlRule acr, AuthenticatedUser user, ResourceId resourceId) {
        if (!acr.getResourceId().equals(resourceId)) return null;
        if (!acr.getPrincipalId().equals(user.getUserResourceId())) return null;
        return acr.isOwner() || "true".equals(acr.getViewCondition());
    }

    private static Boolean hasAccess(AccessControlRule acr, AuthenticatedUser user, Resource resource) {
        final ResourceId resourceId;

        if (acr == null) {
            acr = AccessControlRule.fromResource(resource);
            resourceId = acr.getResourceId();
        } else {
            resourceId = resource.getId();
        }

        return hasAccess(acr, user, resourceId);
    }
}
