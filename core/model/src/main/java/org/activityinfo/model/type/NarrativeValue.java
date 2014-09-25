package org.activityinfo.model.type;

import org.activityinfo.model.resource.IsRecord;
import org.activityinfo.model.resource.Record;
import org.activityinfo.model.resource.Records;
import org.activityinfo.model.type.primitive.HasStringValue;

public class NarrativeValue implements FieldValue, IsRecord, HasStringValue {

    private String text;

    public NarrativeValue(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public FieldTypeClass getTypeClass() {
        return NarrativeType.TYPE_CLASS;
    }


    @Override
    public Record asRecord() {
        return Records.builder()
                .set(TYPE_CLASS_FIELD_NAME, getTypeClass().getId())
                .set("text", text)
                .build();
    }

    @Override
    public String asString() {
        return text;
    }

    public static NarrativeValue fromRecord(Record record) {
        return new NarrativeValue(record.getString("text"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NarrativeValue that = (NarrativeValue) o;

        if (text != null ? !text.equals(that.text) : that.text != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return text != null ? text.hashCode() : 0;
    }

}
