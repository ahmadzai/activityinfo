package org.activityinfo.shared.dto;

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

import com.extjs.gxt.ui.client.data.BaseModelData;

public class TargetValueDTO extends BaseModelData implements EntityDTO {

    public static final String ENTITY_NAME = "TargetValue";

    public void setTargetId(int targetId) {
        set("targetId", targetId);
    }

    public int getTargetId() {
        return (Integer) get("targetId");
    }

    public void setIndicatorId(int indicatorId) {
        set("indicatorId", indicatorId);
    }

    public int getIndicatorId() {
        return (Integer) get("indicatorId");
    }

    public Double getValue() {
        return (Double) get("value");
    }

    public void setValue(Double value) {
        set("value", value);
    }

    public void setName(String name) {
        set("name", name);
    }

    @Override
    public String getName() {
        return (String) get("name");
    }

    /*
     * no unique autogenerated ID for the table. use targetID + indicatorID
     * instead.
     */
    @Override
    public int getId() {
        return 0;
    }

    @Override
    public String getEntityName() {
        return ENTITY_NAME;
    }
}
