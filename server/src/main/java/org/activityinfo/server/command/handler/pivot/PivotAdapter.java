package org.activityinfo.server.command.handler.pivot;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.activityinfo.legacy.shared.command.DimensionType;
import org.activityinfo.legacy.shared.command.Filter;
import org.activityinfo.legacy.shared.command.PivotSites;
import org.activityinfo.legacy.shared.command.result.Bucket;
import org.activityinfo.legacy.shared.reports.content.DimensionCategory;
import org.activityinfo.legacy.shared.reports.model.AdminDimension;
import org.activityinfo.legacy.shared.reports.model.AttributeGroupDimension;
import org.activityinfo.legacy.shared.reports.model.DateDimension;
import org.activityinfo.legacy.shared.reports.model.Dimension;
import org.activityinfo.model.formTree.FormTree;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.query.ColumnModel;
import org.activityinfo.model.query.ColumnSet;
import org.activityinfo.model.query.ColumnView;
import org.activityinfo.model.query.QueryModel;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.expr.ExprValue;
import org.activityinfo.service.store.BatchingFormTreeBuilder;
import org.activityinfo.service.store.CollectionCatalog;
import org.activityinfo.store.query.impl.ColumnSetBuilder;

import java.util.*;
import java.util.logging.Logger;

/**
 * Executes a legacy PivotSites query against the new API
 */
public class PivotAdapter {
    
    private static final Logger LOGGER = Logger.getLogger(PivotAdapter.class.getName());

    private final IndicatorOracle indicatorOracle;
    private final CollectionCatalog catalog;
    private final PivotSites command;
    private final Filter filter;

    /**
     * Maps indicator ids to form class ids
     */
    private final List<ActivityMetadata> activities;

    private Map<ResourceId, FormTree> formTrees;
    
    private List<DimBinding> groupBy;
    private Optional<IndicatorDimBinding> indicatorDimension = Optional.absent();

    private final Map<Object, Bucket> buckets = Maps.newHashMap();

    private final Stopwatch metadataTime = Stopwatch.createUnstarted();
    private final Stopwatch treeTime = Stopwatch.createUnstarted();
    private final Stopwatch queryTime = Stopwatch.createUnstarted();
    private final Stopwatch aggregateTime = Stopwatch.createUnstarted();
    

    public PivotAdapter(IndicatorOracle indicatorOracle, CollectionCatalog catalog, PivotSites command) throws InterruptedException {
        this.indicatorOracle = indicatorOracle;
        this.catalog = catalog;
        this.command = command;
        this.filter = command.getFilter();


        // Mapping from indicator id -> activityId
        metadataTime.start();
        activities = indicatorOracle.fetch(filter);
        metadataTime.stop();

        // Query form trees: needed to determine attribute mapping
        formTrees = queryFormTrees();

        // Define the dimensions we're pivoting by
        groupBy = new ArrayList<>();
        for (Dimension dimension : command.getDimensions()) {
            if(dimension.getType() == DimensionType.Indicator) {
                indicatorDimension = Optional.of(new IndicatorDimBinding());
            } else {
                groupBy.add(bindingFor(dimension));
            }
        }
    }

    private Map<ResourceId, FormTree> queryFormTrees() {
        
        treeTime.start();
        
        Set<ResourceId> formIds = new HashSet<>();
        for(ActivityMetadata activity : activities) {
            formIds.add(activity.getFormClassId());
        }

        BatchingFormTreeBuilder builder = new BatchingFormTreeBuilder(catalog);
        Map<ResourceId, FormTree> trees = builder.queryTrees(formIds);
        
        treeTime.stop();
        
        return trees;
        
    }

    private DimBinding bindingFor(Dimension dimension) {
        switch (dimension.getType()) {
            case Partner:
                return new PartnerDimBinding();
            case Project:
                return new ProjectDimBinding();
            
            case Activity:
                return new ActivityDimBinding();
            case ActivityCategory:
                return new ActivityCategoryDimBinding();
            
            case Date:
                return new DateDimBinding((DateDimension) dimension);
            
            case AttributeGroup:
                return new AttributeDimBinding((AttributeGroupDimension) dimension, formTrees.values());
            
            case AdminLevel:
                return new AdminDimBinding((AdminDimension) dimension);
            
            case Site:
                return new SiteDimBinding();
            
            case Database:
            case Status:
            case Attribute:
                break;
            case Location:
                break;
            case Target:
                break;
        }
        throw new UnsupportedOperationException("Unsupported dimension " + dimension);
    }

    public PivotSites.PivotResult execute() {

        for (ActivityMetadata activity : activities) {
            switch (command.getValueType()) {
                case INDICATOR:
                    executeIndicatorValuesQuery(activity);
                    break;
                case TOTAL_SITES:
                case DIMENSION:
                    executeSiteCountQuery(activity);
                    break;
            }
        }

        LOGGER.info(String.format("Pivot timings: metadata %s, trees: %s, query: %s, aggregate: %s",
                metadataTime, treeTime, queryTime, aggregateTime));


        return new PivotSites.PivotResult(Lists.newArrayList(buckets.values()));
    }


    private void executeIndicatorValuesQuery(ActivityMetadata activity) {
        int activityId = activity.getId();
        ResourceId formClassId = activity.getFormClassId();
        FormTree formTree = formTrees.get(activity.getFormClassId());
        QueryModel queryModel = new QueryModel(formClassId);

        // Add Indicators
        for (IndicatorMetadata indicator : activity.getIndicators()) {
            queryModel.selectExpr(indicator.getFieldExpression()).as(indicator.getAlias());
        }

        // Add dimensions columns as needed
        for (DimBinding dimension : groupBy) {
            for (ColumnModel columnModel : dimension.getColumnQuery(formTree)) {
                queryModel.addColumn(columnModel);
            }
        }
        
        // declare the filter
        queryModel.setFilter(composeFilter(activity));

        // Query the table 
        queryTime.start();
        ColumnSetBuilder builder = new ColumnSetBuilder(catalog);
        ColumnSet columnSet = builder.build(queryModel);
        
        queryTime.stop();
        
        aggregateTime.start();
        
        // Now add the results to the buckets
        DimensionCategory[][] categories = extractCategories(activity, formTree, columnSet);

        for (IndicatorMetadata indicator : activity.getIndicators()) {
            ColumnView measureView = columnSet.getColumnView(indicator.getAlias());
            DimensionCategory indicatorCategory = null;

            if (indicatorDimension.isPresent()) {
                indicatorCategory = indicatorDimension.get().category(formTree, indicator.getId());
            }

            for (int i = 0; i < columnSet.getNumRows(); i++) {

                double value = measureView.getDouble(i);
                if (!Double.isNaN(value)) {

                    Bucket bucket = new Bucket();
                    bucket.setCount(1);
                    bucket.setSum(value);
                    bucket.setAggregationMethod(indicator.getAggregation());

                    if (indicatorDimension.isPresent()) {
                        bucket.setCategory(indicatorDimension.get().getModel(), indicatorCategory);
                    }

                    for (int j = 0; j < groupBy.size(); j++) {
                        bucket.setCategory(groupBy.get(j).getModel(), categories[j][i]);
                    }

                    addBucket(bucket);
                }
            }
        }
        aggregateTime.stop();
    }
    
    private void executeSiteCountQuery(ActivityMetadata activity) {
        Preconditions.checkState(!indicatorDimension.isPresent());

        int activityId = activity.getId();
        ResourceId formClassId = activity.getFormClassId();
        FormTree formTree = formTrees.get(activity.getFormClassId());
        QueryModel queryModel = new QueryModel(activity.getFormClassId());

        
        // Add dimensions columns as needed
        for (DimBinding dimension : groupBy) {
            for (ColumnModel columnModel : dimension.getColumnQuery(formTree)) {
                queryModel.addColumn(columnModel);
            }
        }

        // Query the table 
        queryTime.start();
        ColumnSetBuilder builder = new ColumnSetBuilder(catalog);
        ColumnSet columnSet = builder.build(queryModel);
        queryTime.stop();

        aggregateTime.start();

        // Now add the results to the buckets
        DimensionCategory[][] categories = extractCategories(activity, formTree, columnSet);

        for (int i = 0; i < columnSet.getNumRows(); i++) {

            Bucket bucket = new Bucket();
            bucket.setCount(1);
            bucket.setAggregationMethod(2);


            for (int j = 0; j < groupBy.size(); j++) {
                bucket.setCategory(groupBy.get(j).getModel(), categories[j][i]);
            }
            
            addBucket(bucket);
        }
        aggregateTime.stop();
    }

    private ExprValue composeFilter(ActivityMetadata activity) {
        StringBuilder filter = new StringBuilder();
        appendFilter("_id", CuidAdapter.SITE_DOMAIN, DimensionType.Site, filter);
        appendFilter("partner", CuidAdapter.PARTNER_DOMAIN, DimensionType.Partner, filter);
        appendFilter("project", CuidAdapter.PROJECT_DOMAIN, DimensionType.Project, filter);
        appendFilter("location", CuidAdapter.LOCATION_DOMAIN, DimensionType.Location, filter);
        
        
        
        
        if(filter.length() > 0) {
            LOGGER.info("Filter: " + filter);
            return new ExprValue(filter.toString());
        } else {
            return null;
        }
    }


    private void appendFilter(String fieldName, char domain, DimensionType type, StringBuilder filter) {
        Set<Integer> ids = this.filter.getRestrictions(type);
        if(!ids.isEmpty()) {
            if(filter.length() > 0) {
                filter.append(" && ");
            }
            filter.append("(");
            boolean needsComma = false;
            for (Integer id : ids) {
                if(needsComma) {
                    filter.append(" || ");
                } 
                filter.append(fieldName);
                filter.append("==");
                filter.append("'").append(CuidAdapter.cuid(domain, id).asString()).append("'");
                needsComma = true;
            }
            filter.append(")");
        }
    }
    
    private DimensionCategory[][] extractCategories(ActivityMetadata activity, FormTree formTree, ColumnSet columnSet) {
        DimensionCategory[][] array = new DimensionCategory[groupBy.size()][];

        for (int i = 0; i < groupBy.size(); i++) {
            array[i] = groupBy.get(i).extractCategories(activity, formTree, columnSet);
        }
        return array;
    }


    public void addBucket(Bucket bucket) {
        Bucket existing = buckets.get(bucket.getKey());
        if (existing == null) {
            buckets.put(bucket.getKey(), bucket);
        } else {
            existing.add(bucket);
        }
    }

}