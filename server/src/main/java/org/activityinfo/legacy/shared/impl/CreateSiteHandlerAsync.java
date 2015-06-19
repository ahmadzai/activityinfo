package org.activityinfo.legacy.shared.impl;

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

import com.bedatadriven.rebar.sql.client.SqlResultCallback;
import com.bedatadriven.rebar.sql.client.SqlResultSet;
import com.bedatadriven.rebar.sql.client.SqlTransaction;
import com.bedatadriven.rebar.sql.client.query.SqlInsert;
import com.bedatadriven.rebar.sql.client.query.SqlQuery;
import com.bedatadriven.rebar.sql.client.query.SqlUpdate;
import com.bedatadriven.rebar.time.calendar.LocalDate;
import com.extjs.gxt.ui.client.data.RpcMap;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.activityinfo.legacy.shared.command.CreateSite;
import org.activityinfo.legacy.shared.command.UpdateSite;
import org.activityinfo.legacy.shared.command.result.CreateResult;
import org.activityinfo.legacy.shared.command.result.VoidResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.legacy.shared.model.AttributeDTO;
import org.activityinfo.legacy.shared.model.IndicatorDTO;

import java.util.Date;
import java.util.Map.Entry;

/**
 * Handles the creation of a new entity on the client side, and queues the
 * command for later transmission to the server.
 * <p/>
 * Currently only supports updates to the SiteDTO view.
 */
public class CreateSiteHandlerAsync implements CommandHandlerAsync<CreateSite, CreateResult> {

    @Override
    public void execute(final CreateSite cmd,
                        final ExecutionContext context,
                        final AsyncCallback<CreateResult> callback) {

        if (cmd.hasNestedCommand()) {
            executeNestedCommand(cmd, context);
        }

        // Determine whether this site already exists
        SqlQuery.select("siteid").from(Tables.SITE).where("siteId").equalTo(cmd.getSiteId())
        .execute(context.getTransaction(), new SqlResultCallback() {
            @Override
            public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                if(results.getRows().isEmpty()) {
                    createNewSite(cmd, context, callback);
                } else {
                    updateExistingSite(cmd, context, callback);
                }
            }
        });
    }

    private void updateExistingSite(final CreateSite cmd, ExecutionContext context, final AsyncCallback<CreateResult> callback) {
        UpdateSite updateSite = new UpdateSite(cmd.getSiteId(), cmd.getProperties());
        context.execute(updateSite, new AsyncCallback<VoidResult>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(VoidResult result) {
                callback.onSuccess(new CreateResult(cmd.getSiteId()));
            }
        });
    }

    private void createNewSite(CreateSite cmd, ExecutionContext context, AsyncCallback<CreateResult> callback) {
        insertSite(context.getTransaction(), cmd);

        // we only create a reporting period if this is a one-off activity
        Integer reportingPeriodId = cmd.getReportingPeriodId();
        if (reportingPeriodId != null) {
            insertReportingPeriod(context.getTransaction(), cmd);
        }

        callback.onSuccess(new CreateResult(cmd.getSiteId()));
    }

    private void insertSite(SqlTransaction tx, CreateSite cmd) {

        RpcMap properties = cmd.getProperties();

        // deal with the possibility that we've already received this command
        // but its completion was not acknowledged because of network problems
        tx.executeSql("delete from indicatorvalue Where ReportingPeriodId in " +
                      "(select reportingperiodid from reportingperiod where siteid=" + cmd.getSiteId() + ")");
        SqlUpdate.delete(Tables.REPORTING_PERIOD).where("SiteId", cmd.getSiteId()).execute(tx);
        SqlUpdate.delete(Tables.SITE_HISTORY).where("siteid", cmd.getSiteId()).execute(tx);
        SqlUpdate.delete(Tables.SITE).where("SiteId", cmd.getSiteId()).execute(tx);

        SqlInsert.insertInto(Tables.SITE)
                 .value("SiteId", cmd.getSiteId())
                 .value("LocationId", cmd.getLocationId())
                 .value("ActivityId", cmd.getActivityId())
                 .value("Date1", properties.get("date1"))
                 .value("Date2", properties.get("date2"))
                 .value("Comments", properties.get("comments"))
                 .value("PartnerId", properties.get("partnerId"))
                 .value("ProjectId", properties.get("projectId"))
                 .value("DateCreated", new Date())
                 .value("DateEdited", new Date())
                 .value("timeEdited", new Date().getTime())
                 .execute(tx);

        insertAttributeValues(tx, cmd);
    }

    private void insertAttributeValues(SqlTransaction tx, CreateSite cmd) {

        for (Entry<String, Object> property : cmd.getProperties().getTransientMap().entrySet()) {
            if (property.getKey().startsWith(AttributeDTO.PROPERTY_PREFIX) && property.getValue() != null) {

                SqlInsert.insertInto(Tables.ATTRIBUTE_VALUE)
                         .value("AttributeId", AttributeDTO.idForPropertyName(property.getKey()))
                         .value("SiteId", cmd.getSiteId())
                         .value("Value", property.getValue())
                         .execute(tx);
            }
        }
    }

    private int insertReportingPeriod(SqlTransaction tx, CreateSite cmd) {

        int reportingPeriodId = cmd.getReportingPeriodId();
        SqlInsert.insertInto(Tables.REPORTING_PERIOD)
                 .value("ReportingPeriodId", reportingPeriodId)
                 .value("SiteId", cmd.getSiteId())
                 .value("Date1", cmd.getProperties().get("date1"))
                 .value("Date2", cmd.getProperties().get("date2"))
                 .value("DateCreated", new Date())
                 .value("DateEdited", new Date())
                 .execute(tx);

        insertIndicatorValues(tx, cmd);

        return reportingPeriodId;
    }

    private void insertIndicatorValues(SqlTransaction tx, CreateSite cmd) {
        for (Entry<String, Object> property : cmd.getProperties().getTransientMap().entrySet()) {
            if (property.getKey().startsWith(IndicatorDTO.PROPERTY_PREFIX) && property.getValue() != null) {

                Object value = property.getValue();

                SqlInsert sqlInsert = SqlInsert.insertInto(Tables.INDICATOR_VALUE)
                        .value("IndicatorId", IndicatorDTO.indicatorIdForPropertyName(property.getKey()))
                        .value("ReportingPeriodId", cmd.getReportingPeriodId());

                if (value instanceof Number) {
                    if (value instanceof Double && ((Double)value).isNaN()) {
                        throw new RuntimeException("It's not allowed to send Double.NaN values for update, indicatorId: " + IndicatorDTO.indicatorIdForPropertyName(property.getKey()));
                    }

                    sqlInsert.value("Value", value).execute(tx);
                } else if (value instanceof String) {
                    sqlInsert.value("TextValue", value).execute(tx);
                } else if (value instanceof Date) {
                    sqlInsert.value("DateValue", value).execute(tx);
                } else if (value instanceof LocalDate) {
                    sqlInsert.value("DateValue", value).execute(tx);
                } else if (value instanceof Boolean) {
                    sqlInsert.value("BooleanValue", value).execute(tx);
                }
            }
        }
    }

    private void executeNestedCommand(final CreateSite cmd, final ExecutionContext context) {
        context.execute(cmd.getNestedCommand(), new AsyncCallback<VoidResult>() {
            @Override
            public void onSuccess(VoidResult result) {
                // continue creating the site
            }

            @Override
            public void onFailure(Throwable caught) {
                throw new CommandException("can't execute nested command while creating site", caught);
            }
        });
    }
}
