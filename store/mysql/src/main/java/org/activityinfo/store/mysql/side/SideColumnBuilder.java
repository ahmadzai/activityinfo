package org.activityinfo.store.mysql.side;


import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.activityinfo.model.type.FieldType;
import org.activityinfo.model.type.FieldValue;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.model.type.number.QuantityType;
import org.activityinfo.model.type.primitive.TextType;
import org.activityinfo.service.store.CursorObserver;
import org.activityinfo.store.mysql.collections.ActivityField;
import org.activityinfo.store.mysql.cursor.QueryExecutor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SideColumnBuilder {
    private static final Logger LOGGER = Logger.getLogger(SideColumnBuilder.class.getName());

    /**
     * Maps indicatorId to the value buffer
     */
    private Map<Integer, ValueBuffer> fieldMap = Maps.newHashMap();
    
    private final String newLine;
    
    public SideColumnBuilder() {
        this.newLine = "\n";
    }
    
    public void add(ActivityField field, final CursorObserver<FieldValue> observer) {
        ValueBuffer buffer = createBuffer(field.getFormField().getType(), observer);
        
        LOGGER.log(Level.INFO, field.getId() + ": Created buffer of type " + buffer.getClass().getSimpleName());
        
        fieldMap.put(field.getId(), buffer);
    }

    private ValueBuffer createBuffer(FieldType type, CursorObserver<FieldValue> observer) {

        if(type instanceof QuantityType) {
            return new QuantityBuffer((QuantityType) type, observer);

        } else if(type instanceof TextType) {
            return new TextBuffer(observer);

        } else if(type instanceof EnumType) {
            return new AttributeBuffer((EnumType) type, observer);

        } else {
            throw new IllegalArgumentException("type: " + type);
        }
    }


    public void sitesIndicators(int activityId, QueryExecutor executor) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT site.siteId, iv.indicatorId, iv.value, iv.textValue").append(newLine);
        sql.append("FROM site").append(newLine);
        sql.append("LEFT JOIN reportingperiod rp ON (site.siteId = rp.siteId)").append(newLine);
        sql.append("LEFT JOIN indicatorvalue iv ON (rp.reportingPeriodId = iv.reportingPeriodId)").append(newLine);
        sql.append("WHERE site.dateDeleted is null AND site.activityId=").append(activityId).append(newLine);
        sql.append("  AND (iv.indicatorId IS NULL OR iv.indicatorId IN (");
        Joiner.on(", ").appendTo(sql, fieldMap.keySet());
        sql.append("))").append(newLine);
        sql.append("ORDER BY site.siteId");

        System.out.println(sql);
        
        execute(executor, sql);
    }
    
    public void attributes(int activityId, QueryExecutor executor) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT site.siteId, a.attributeGroupId, av.attributeId, av.value").append(newLine);
        sql.append("FROM site").append(newLine);
        sql.append("LEFT JOIN attributevalue av ON (site.siteId = av.siteId)").append(newLine);
        sql.append("LEFT JOIN attribute a ON (av.attributeId = a.attributeId)").append(newLine);
        sql.append("WHERE site.dateDeleted IS NULL AND site.activityId=").append(activityId).append(newLine);
        sql.append("  AND (a.attributeGroupId IS NULL OR a.attributeGroupId IN (");
        Joiner.on(", ").appendTo(sql, fieldMap.keySet());
        sql.append("))").append(newLine);
        sql.append("ORDER BY site.siteId");
        
        execute(executor, sql);
    }
    
    private void execute(QueryExecutor executor, StringBuilder sql) throws SQLException {
        int lastRowId = -1;

        ValueBuffer buffers[] = bufferArray();

        try(ResultSet rs = executor.query(sql.toString())) {
            while(rs.next()) {
                int rowId = rs.getInt(ValueBuffer.ROW_ID_COLUMN);
                if(rowId != lastRowId && lastRowId != -1) {
                    for(int i=0;i!=buffers.length;++i) {
                        buffers[i].next();
                    }
                }
                int fieldId = rs.getInt(ValueBuffer.FIELD_ID_COLUMN);
                if(!rs.wasNull()) {
                    fieldMap.get(fieldId).set(rs);
                }
                lastRowId = rowId;
            }
            if(lastRowId != -1) {
                for(int i=0;i!=buffers.length;++i) {
                    buffers[i].next();
                }
            }
            for(int i=0;i!=buffers.length;++i) {
                buffers[i].done();
            }
        }
    }

    private ValueBuffer[] bufferArray() {
        List<ValueBuffer> buffers = Lists.newArrayList(fieldMap.values());
        return buffers.toArray(new ValueBuffer[buffers.size()]);
    }

    public boolean isEmpty() {
        return fieldMap.isEmpty();
    }

}
