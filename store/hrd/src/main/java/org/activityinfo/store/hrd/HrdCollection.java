package org.activityinfo.store.hrd;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Query;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.resource.Resource;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.resource.ResourceUpdate;
import org.activityinfo.service.store.CollectionPermissions;
import org.activityinfo.service.store.ColumnQueryBuilder;
import org.activityinfo.service.store.ResourceCollection;
import org.activityinfo.store.hrd.entity.Datastore;
import org.activityinfo.store.hrd.entity.FormRecordEntity;
import org.activityinfo.store.hrd.entity.FormRecordKey;
import org.activityinfo.store.hrd.entity.FormRootKey;
import org.activityinfo.store.hrd.op.CreateOrUpdateForm;
import org.activityinfo.store.hrd.op.CreateOrUpdateRecord;
import org.activityinfo.store.hrd.op.QueryOperation;

import java.util.List;

/**
 * Collection-backed by the AppEngine High-Replication Datastore (HRD)
 */
public class HrdCollection implements ResourceCollection {

    private Datastore datastore;
    private FormClass formClass;

    public HrdCollection(Datastore datastore, FormClass formClass) {
        this.datastore = datastore;
        this.formClass = formClass;
    }

    @Override
    public CollectionPermissions getPermissions(int userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Resource> get(ResourceId resourceId) {
        FormRecordKey key = new FormRecordKey(resourceId);
        Optional<FormRecordEntity> submission = datastore.loadIfPresent(key);

        if(submission.isPresent()) {
            FormInstance instance = submission.get().toFormInstance(formClass);
            Resource resource = instance.asResource();
            return Optional.of(resource);
        
        } else {
            return Optional.absent();
        }
    }

    @Override
    public FormClass getFormClass() {
        return formClass;
    }

    @Override
    public void updateFormClass(FormClass formClass) {
        datastore.execute(new CreateOrUpdateForm(formClass));
    }

    @Override
    public void add(ResourceUpdate update) {
        datastore.execute(new CreateOrUpdateRecord(formClass.getId(), update));
    }

    @Override
    public void update(final ResourceUpdate update) {
        datastore.execute(new CreateOrUpdateRecord(formClass.getId(), update));
    }
    

    @Override
    public ColumnQueryBuilder newColumnQuery() {
        return new HrdQueryColumnBuilder(datastore.unwrap(), FormRootKey.key(formClass.getId()), formClass);
    }

    @Override
    public long cacheVersion() {
        return 0;
    }

    public FormInstance getSubmission(ResourceId resourceId) throws EntityNotFoundException {
        FormRecordKey key = new FormRecordKey(resourceId);
        FormRecordEntity submission = datastore.load(key);
        
        return submission.toFormInstance(formClass);
    }

    public Iterable<FormInstance> getSubmissionsOfParent(ResourceId parentId) {
        return query(FormRecordEntity.parentFilter(parentId));
    }

    public Iterable<FormInstance> getSubmissions() {
        return query(null);
    }

    private Iterable<FormInstance> query(Query.FilterPredicate filter) {
        FormRootKey rootKey = new FormRootKey(formClass.getId());
        final Query query = new Query(FormRecordEntity.KIND, rootKey.raw());
        query.setFilter(filter);

        return datastore.execute(new QueryOperation<List<FormInstance>>() {
            @Override
            public List<FormInstance> execute(Datastore datastore) {
                List<FormInstance> instances = Lists.newArrayList();
                for (Entity entity : datastore.prepare(query).asIterable()) {
                    FormRecordEntity submission = new FormRecordEntity(entity);
                    instances.add(submission.toFormInstance(formClass));
                }
                return instances;
            }
        });
    }
}