package org.activityinfo.ui.client.page.entry.location;

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

import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.widget.ListView;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Element;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.shared.model.LocationDTO;
import org.activityinfo.ui.client.page.entry.form.resources.SiteFormResources;

import java.util.Arrays;

/**
 * Show a list of locations
 */
public class SearchListView extends ListView<LocationDTO> {

    public SearchListView(final LocationSearchPresenter presenter) {
        super();

        setStore(presenter.getStore());
        setDisplayProperty("name");
        setTemplate(SiteFormResources.INSTANCE.locationTemplate().getText());
        addStyleName(SiteFormResources.INSTANCE.style().locationSearchResults());
        setItemSelector(".locSerResult");
        setBorders(false);
        setStyleAttribute("overflow", "visible");
        setLoadingText(SafeHtmlUtils.fromSafeConstant(I18N.CONSTANTS.loading()));


        getSelectionModel().addSelectionChangedListener(new SelectionChangedListener<LocationDTO>() {
            @Override
            public void selectionChanged(SelectionChangedEvent<LocationDTO> se) {
                presenter.select(this, se.getSelectedItem());
            }
        });

        addListener(Events.DoubleClick, new Listener<ListViewEvent<LocationDTO>>() {

            @Override
            public void handleEvent(ListViewEvent<LocationDTO> be) {
                presenter.accept();
            }
        });

        presenter.addListener(Events.Select, new Listener<LocationEvent>() {

            @Override
            public void handleEvent(LocationEvent event) {
                if (event.getSource() != SearchListView.this) {
                    onResultSelected(event);
                }
            }
        });

        SiteFormResources.INSTANCE.style().ensureInjected();
    }

    /**
     * Location result was selected externally
     */
    private void onResultSelected(LocationEvent event) {
        select(event.getLocation());
        scrollIntoView(event.getLocation());
    }

    private void select(LocationDTO location) {
        getSelectionModel().setSelection(Arrays.asList(location));
    }

    private void scrollIntoView(LocationDTO location) {
        int index = store.indexOf(location);
        if (index >= 0) {
            Element element = getElement(index);
            if (element != null) {
                element.scrollIntoView();
            }
        }
    }
}
