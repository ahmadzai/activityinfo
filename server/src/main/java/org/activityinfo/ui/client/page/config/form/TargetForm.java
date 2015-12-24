package org.activityinfo.ui.client.page.config.form;

/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.extjs.gxt.ui.client.binding.FieldBinding;
import com.extjs.gxt.ui.client.binding.FormBinding;
import com.extjs.gxt.ui.client.widget.form.DateField;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.google.gwt.core.client.GWT;
import org.activityinfo.i18n.shared.UiConstants;
import org.activityinfo.legacy.shared.model.PartnerDTO;
import org.activityinfo.legacy.shared.model.ProjectDTO;
import org.activityinfo.legacy.shared.model.UserDatabaseDTO;
import org.activityinfo.ui.client.widget.legacy.MappingComboBox;
import org.activityinfo.ui.client.widget.legacy.MappingComboBoxBinding;

public class TargetForm extends FormPanel {

    private FormBinding binding;

    public TargetForm(UserDatabaseDTO database) {

        binding = new FormBinding(this);

        UiConstants constants = GWT.create(UiConstants.class);

        TextField<String> nameField = new TextField<String>();
        nameField.setFieldLabel(constants.name());
        nameField.setMaxLength(255);
        nameField.setAllowBlank(false);
        binding.addFieldBinding(new FieldBinding(nameField, "name"));
        this.add(nameField);

        DateField fromDateField = new DateField();
        fromDateField.setFieldLabel(constants.fromDate());
        fromDateField.setAllowBlank(false);
        binding.addFieldBinding(LocalDateBinding.create(fromDateField, "fromDate"));
        this.add(fromDateField);

        DateField toDateField = new DateField();
        toDateField.setFieldLabel(constants.toDate());
        toDateField.setAllowBlank(false);
        binding.addFieldBinding(LocalDateBinding.create(toDateField, "toDate"));
        this.add(toDateField);

        MappingComboBox<Integer> projectCombo = new MappingComboBox<Integer>();
        projectCombo.setFieldLabel(constants.project());
        projectCombo.setAllowBlank(true);
        projectCombo.addNone();
        for (ProjectDTO projectDTO : database.getProjects()) {
            projectCombo.add(projectDTO.getId(), projectDTO.getName());
        }
        binding.addFieldBinding(new MappingComboBoxBinding(projectCombo, "projectId"));
        this.add(projectCombo);

        MappingComboBox<Integer> partnerCombo = new MappingComboBox<Integer>();
        partnerCombo.addNone();
        for (PartnerDTO partner : database.getPartners()) {
            partnerCombo.add(partner.getId(), partner.getName());
        }
        partnerCombo.setAllowBlank(true);
        partnerCombo.setFieldLabel(constants.partner());
        binding.addFieldBinding(new MappingComboBoxBinding(partnerCombo, "partnerId"));
        this.add(partnerCombo);
    }

    public FormBinding getBinding() {
        return binding;
    }

}
