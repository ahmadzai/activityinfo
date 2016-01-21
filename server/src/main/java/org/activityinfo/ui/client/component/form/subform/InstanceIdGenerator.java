package org.activityinfo.ui.client.component.form.subform;
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

import org.activityinfo.model.date.DateRange;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.resource.ResourceId;

/**
 * @author yuriyz on 01/20/2016.
 */
public class InstanceIdGenerator {

    private InstanceIdGenerator() {
    }

    public static ResourceId periodId(DateRange range) {
        return ResourceId.valueOf(ResourceId.GENERATED_ID_DOMAIN + "_period_" + range.getStart().getTime() + "_" + range.getEnd().getTime());
    }

    public static ResourceId newCollectionId() {
        return ResourceId.generateId();
    }

    public static ResourceId keyId(ResourceId subFormId) {
        return ResourceId.valueOf("_key_" + subFormId.asString());
    }

    public static FormInstance newUnkeyedInstance(ResourceId subFormId) {
        return new FormInstance(newCollectionId(), keyId(subFormId));
    }

    public static FormInstance newKeyedInstance(DateRange dateRange, ResourceId subFormId) {
        return new FormInstance(periodId(dateRange), keyId(subFormId));
    }
}
