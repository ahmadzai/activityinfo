package org.activityinfo.ui.store.remote.client;

import com.google.common.base.Function;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.activityinfo.model.resource.Resource;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.resource.ResourceNode;
import org.activityinfo.model.resource.ResourceTree;
import org.activityinfo.model.table.TableData;
import org.activityinfo.model.table.TableModel;
import org.activityinfo.promise.Promise;
import org.activityinfo.service.store.RemoteStoreService;
import org.activityinfo.service.store.UpdateResult;
import org.activityinfo.ui.store.remote.client.resource.*;
import org.activityinfo.ui.store.remote.client.table.JsTableDataBuilder;

import java.util.List;
import java.util.logging.Logger;

public class RemoteStoreServiceImpl implements RemoteStoreService {

    private static final Logger LOGGER = Logger.getLogger(RemoteStoreServiceImpl.class.getName());

    private RestEndpoint store;

    public RemoteStoreServiceImpl(RestEndpoint store) {
        this.store = store;
    }

    @Override
    public Promise<Resource> get(ResourceId resourceId) {
        return store.resolve("resource").resolve(resourceId.asString()).get(new ResourceParser());
    }

    @Override
    public Promise<List<ResourceNode>> queryRoots() {
        return store.resolve("query").resolve("roots").get(new ResourceNodeListParser());
    }

    @Override
    public Promise<TableData> queryTable(TableModel tableModel) {
        return store.resolve("query").resolve("table")
                .postJson(ResourceSerializer.toJson(tableModel.asRecord()))
                .then(new JsTableDataBuilder());
    }

    public Promise<UpdateResult> put(final Resource resource) {
        return store
                .resolve("resource")
                .resolve(resource.getId().asString())
                .putJson(ResourceSerializer.toJson(resource))
                .then(new Function<Response, UpdateResult>() {
                    @Override
                    public UpdateResult apply(Response input) {
                        return UpdateResultParser.parse(input);
                    }
                });
    }


    @Override
    public Promise<ResourceTree> queryTree(ResourceId rootId) {

        JSONObject request = new JSONObject();
        request.put("rootId", new JSONString(rootId.asString()));

        return store.resolve("query")
                .resolve("tree")
                .postJson(request.toString())
                .then(new ResourceTreeParser());
    }
}