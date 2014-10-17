package org.activityinfo.model.annotation.processor;

import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.record.Record;
import org.activityinfo.model.record.Records;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class BazTest {

    @Test
    public void testDefaultBoolean() {

        Record record = Records.builder()
            .set("label", "My name")
            .build();

        Baz baz = BazClass.INSTANCE.toBean(record);
        assertThat(baz.getLabel(), equalTo("My name"));
        assertThat(baz.isVisible(), equalTo(true));
    }

    @Test
    public void testBoolean() {

        Record record = Records.builder()
            .set("label", "My name")
            .set("visible", false)
            .build();

        Baz baz = BazClass.INSTANCE.toBean(record);
        assertThat(baz.getLabel(), equalTo("My name"));
        assertThat(baz.isVisible(), equalTo(false));
    }

    @Test
    public void toRecord() {
        Baz baz = new Baz();
        baz.setLabel("A");
        baz.setVisible(true);

        Record record = BazClass.INSTANCE.toRecord(baz);
        assertThat(record.getString("label"), equalTo("A"));
        assertThat(record.getBoolean("visible"), equalTo(true));
    }

    @Test
    public void listItems() {
        Baz baz = new Baz();
        baz.setLabel("B");
        baz.setVisible(true);
        baz.getChildren().add(new Foo("B1"));

        Record record = BazClass.INSTANCE.toRecord(baz);
        assertThat(record.getRecordList("children").size(), equalTo(1));
        assertThat(record.getRecordList("children").get(0).getString("name"), equalTo("B1"));
    }

    @Test
    public void formClassTest() {
        FormClass formClass = BazClass.INSTANCE.get();
        assertThat(formClass.getFields().size(), equalTo(3));
    }
}