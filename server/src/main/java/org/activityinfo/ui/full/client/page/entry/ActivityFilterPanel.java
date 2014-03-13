package org.activityinfo.ui.full.client.page.entry;

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

import com.extjs.gxt.ui.client.data.DataReader;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.shared.command.Filter;
import org.activityinfo.legacy.shared.command.GetSchema;
import org.activityinfo.legacy.shared.model.ActivityDTO;
import org.activityinfo.legacy.shared.model.SchemaDTO;
import org.activityinfo.legacy.shared.model.UserDatabaseDTO;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.ui.full.client.filter.FilterPanel;
import org.activityinfo.ui.full.client.icon.IconImageBundle;
import org.activityinfo.ui.full.client.page.PageState;
import org.activityinfo.ui.full.client.page.common.nav.Link;
import org.activityinfo.ui.full.client.page.common.nav.LinkTreePanel;
import org.activityinfo.ui.full.client.page.common.nav.Navigator;
import org.activityinfo.ui.full.client.page.entry.place.DataEntryPlace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityFilterPanel extends ContentPanel implements FilterPanel {

    private LinkTreePanel tree;
    private Filter currentFilter = new Filter();

    public ActivityFilterPanel(Dispatcher dispatcher) {

        setHeadingHtml(I18N.CONSTANTS.activities());
        setLayout(new FitLayout());

        tree = new LinkTreePanel(new TreeProxy(dispatcher), "activityFilter");
        tree.getSelectionModel().addSelectionChangedListener(
                new SelectionChangedListener<Link>() {

                    @Override
                    public void selectionChanged(SelectionChangedEvent<Link> se) {

                        if (se.getSelectedItem() != null) {
                            PageState pageState = se.getSelectedItem()
                                    .getPageState();
                            if (pageState instanceof DataEntryPlace) {
                                currentFilter = ((DataEntryPlace) pageState).getFilter();
                                ValueChangeEvent.fire(ActivityFilterPanel.this, currentFilter);
                            }
                        }
                    }
                });
        tree.getStore().getLoader().load();
        add(tree);
    }

    @Override
    public Filter getValue() {
        return currentFilter;
    }

    @Override
    public void setValue(Filter value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setValue(Filter value, boolean fireEvents) {
        // TODO Auto-generated method stub

    }

    @Override
    public HandlerRegistration addValueChangeHandler(
            ValueChangeHandler<Filter> handler) {
        return addHandler(handler, ValueChangeEvent.getType());
    }

    @Override
    public void applyBaseFilter(Filter filter) {
        // do nothing
    }

    private static class TreeProxy implements Navigator {

        private final Dispatcher dispatcher;

        public TreeProxy(Dispatcher dispatcher) {
            super();
            this.dispatcher = dispatcher;
        }

        @Override
        public void load(DataReader<List<Link>> reader, Object parent,
                         final AsyncCallback<List<Link>> callback) {
            if (parent == null) {
                dispatcher.execute(new GetSchema(),
                        new AsyncCallback<SchemaDTO>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                callback.onFailure(caught);
                            }

                            @Override
                            public void onSuccess(SchemaDTO schema) {
                                callback.onSuccess(buildTree(schema));
                            }
                        });
            } else {
                List<Link> list = new ArrayList<Link>();
                List<ModelData> children = ((Link) parent).getChildren();
                for (ModelData child : children) {
                    list.add((Link) child);
                }
                callback.onSuccess(list);
            }
        }

        private List<Link> buildTree(SchemaDTO schema) {
            List<Link> list = new ArrayList<Link>();
            for (UserDatabaseDTO db : schema.getDatabases()) {
                if (db.getActivities().size() != 0) {

                    Link dbLink = Link.to(new DataEntryPlace(db))
                            .labeled(db.getName())
                            .usingKey(databaseKey(db))
                            .withIcon(IconImageBundle.ICONS.database())
                            .build();

                    Map<String, Link> categories = new HashMap<String, Link>();
                    for (ActivityDTO activity : db.getActivities()) {

                        Link actLink = Link
                                .to(new DataEntryPlace(activity))
                                .labeled(activity.getName())
                                .withIcon(IconImageBundle.ICONS.table()).build();

                        if (activity.getCategory() != null) {
                            Link category = categories.get(activity
                                    .getCategory());
                            if (category == null) {
                                category = Link
                                        .folderLabelled(activity.getCategory())
                                        .usingKey(categoryKey(activity, categories))
                                        .build();
                                categories
                                        .put(activity.getCategory(), category);
                                dbLink.add(category);
                            }
                            category.add(actLink);
                        } else {
                            dbLink.add(actLink);
                        }
                    }
                    list.add(dbLink);
                }
            }
            return list;
        }

        private String categoryKey(ActivityDTO activity,
                                   Map<String, Link> categories) {
            return "category" + activity.getDatabase().getId()
                    + activity.getCategory() + categories.size();
        }

        private String databaseKey(UserDatabaseDTO db) {
            return "database" + db.getId();
        }

        @Override
        public String getHeading() {
            return I18N.CONSTANTS.activities();
        }

        @Override
        public boolean hasChildren(Link parent) {
            return parent.getChildCount() != 0;
        }

        @Override
        public String getStateId() {
            return "activityFilter";
        }
    }
}
