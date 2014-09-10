package org.activityinfo.legacy.shared.command;

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

import org.activityinfo.legacy.shared.command.result.XmlResult;

/**
 * Returns the XML definition of the
 * {@link org.activityinfo.legacy.shared.reports.model.Report ReportModel} for a given
 * {@link org.activityinfo.server.database.hibernate.entity.ReportDefinition}
 * database entity.
 *
 * @author Alex Bertram
 */
public class GetReportDef implements Command<XmlResult> {

    private int id;

    protected GetReportDef() {

    }

    /**
     * @param id The id of the
     *           {@link org.activityinfo.server.database.hibernate.entity.ReportDefinition}
     *           database entity for which to return the XML definition.
     */
    public GetReportDef(int id) {
        this.id = id;
    }

    /**
     * @return The id of the
     * {@link org.activityinfo.server.database.hibernate.entity.ReportDefinition}
     * database entity for which to return the XML definition.
     */
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}