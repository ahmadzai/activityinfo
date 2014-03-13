package org.activityinfo.ui.full.client.report.editor.map.layerOptions;

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

import com.google.gwt.user.client.Event;
import org.activityinfo.legacy.shared.command.Filter;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.reports.shared.model.DateRange;
import org.activityinfo.ui.full.client.filter.FilterResources;
import org.activityinfo.ui.full.client.filter.FilterWidget;
import org.activityinfo.ui.full.client.filter.SelectionCallback;

public class DateFilterWidget extends FilterWidget {

    private DateFilterMenu menu;

    public DateFilterWidget() {
        dimensionSpan.setInnerHTML(I18N.CONSTANTS.dates());
        stateSpan.setInnerText(I18N.CONSTANTS.allDates());
    }

    @Override
    public void updateView() {
        if (getValue().getMinDate() == null && getValue().getMaxDate() == null) {
            setState(I18N.CONSTANTS.allDates());
        } else if (getValue().getMinDate() == null) {
            setState(FilterResources.MESSAGES.beforeDate(getValue()
                    .getMaxDate()));
        } else if (getValue().getMaxDate() == null) {
            setState(FilterResources.MESSAGES
                    .afterDate(getValue().getMinDate()));
        } else {
            setState(FilterResources.MESSAGES.betweenDates(getValue()
                    .getMinDate(), getValue().getMaxDate()));
        }
    }

    @Override
    public void choose(Event event) {
        if (menu == null) {
            menu = new DateFilterMenu();
        }
        menu.showAt(event.getClientX(), event.getClientY(),
                new SelectionCallback<DateRange>() {

                    @Override
                    public void onSelected(DateRange selection) {
                        Filter filter = new Filter(getValue());
                        filter.setDateRange(selection);

                        setValue(filter);
                    }
                });
    }

}
