package org.activityinfo.ui.client.page.config;

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

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.ImplementedBy;
import com.google.inject.Inject;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.client.state.StateProvider;
import org.activityinfo.legacy.shared.Log;
import org.activityinfo.legacy.shared.command.AddPartner;
import org.activityinfo.legacy.shared.command.RemovePartner;
import org.activityinfo.legacy.shared.command.result.CreateResult;
import org.activityinfo.legacy.shared.command.result.DuplicateCreateResult;
import org.activityinfo.legacy.shared.command.result.RemoveFailedResult;
import org.activityinfo.legacy.shared.command.result.RemoveResult;
import org.activityinfo.legacy.shared.model.PartnerDTO;
import org.activityinfo.legacy.shared.model.UserDatabaseDTO;
import org.activityinfo.ui.client.AppEvents;
import org.activityinfo.ui.client.EventBus;
import org.activityinfo.ui.client.page.PageId;
import org.activityinfo.ui.client.page.PageState;
import org.activityinfo.ui.client.page.common.dialog.FormDialogCallback;
import org.activityinfo.ui.client.page.common.dialog.FormDialogTether;
import org.activityinfo.ui.client.page.common.grid.AbstractGridPresenter;
import org.activityinfo.ui.client.page.common.grid.GridView;
import org.activityinfo.ui.client.page.common.toolbar.UIActions;

import java.util.ArrayList;

/**
 * @author Alex Bertram
 */
public class DbPartnerEditor extends AbstractGridPresenter<PartnerDTO> implements DbPage {
    public static final PageId PAGE_ID = new PageId("partners");

    @ImplementedBy(DbPartnerGrid.class)
    public interface View extends GridView<DbPartnerEditor, PartnerDTO> {

        public void init(DbPartnerEditor editor, UserDatabaseDTO db, ListStore<PartnerDTO> store);

        public FormDialogTether showAddDialog(PartnerDTO partner, FormDialogCallback callback);
    }

    private final Dispatcher service;
    private final EventBus eventBus;
    private final View view;

    private UserDatabaseDTO db;
    private ListStore<PartnerDTO> store;

    @Inject
    public DbPartnerEditor(EventBus eventBus, Dispatcher service, StateProvider stateMgr, View view) {
        super(eventBus, stateMgr, view);
        this.service = service;
        this.eventBus = eventBus;
        this.view = view;
    }

    @Override
    public void go(UserDatabaseDTO db) {
        this.db = db;

        store = new ListStore<PartnerDTO>();
        store.setSortField("name");
        store.setSortDir(Style.SortDir.ASC);
        store.add(new ArrayList<PartnerDTO>(db.getPartners()));

        view.init(this, db, store);
        view.setActionEnabled(UIActions.DELETE, false);
    }

    @Override
    protected String getStateId() {
        return "PartnersGrid";
    }

    // public void onSelectionChanged(PartnerDTO selectedItem) {
    // this.view.setActionEnabled(UIActions.delete, selectedItem != null);
    // }

    @Override
    protected void onAdd() {
        final PartnerDTO newPartner = new PartnerDTO();
        this.view.showAddDialog(newPartner, new FormDialogCallback() {

            @Override
            public void onValidated(final FormDialogTether dlg) {

                service.execute(new AddPartner(db.getId(), newPartner), dlg, new AsyncCallback<CreateResult>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Log.debug("DbPartnerEditor caught exception while executing command AddPartner: ", caught);
                    }

                    @Override
                    public void onSuccess(CreateResult result) {
                        if (result instanceof DuplicateCreateResult) {
                            Log.debug("DbPartnerEditor tried to add partner '" + newPartner.getName() +
                                      "' to database " + db.getId() + " but it already exists");
                            MessageBox.alert(I18N.CONSTANTS.newPartner(), I18N.CONSTANTS.duplicatePartner(), null);
                        } else {
                            Log.debug("DbPartnerEditor added new partner '" + newPartner.getName() +
                                      "' to database " + db.getId());
                            newPartner.setId(result.getNewId());
                            store.add(newPartner);
                            eventBus.fireEvent(AppEvents.SCHEMA_CHANGED);
                            dlg.hide();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onDeleteConfirmed(final PartnerDTO model) {
        service.execute(new RemovePartner(db.getId(), model.getId()),
                view.getDeletingMonitor(),
                new AsyncCallback<RemoveResult>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Log.debug("DbPartnerEditor caught exception while executing command RemovePartner: ", caught);
                    }

                    @Override
                    public void onSuccess(RemoveResult result) {
                        if (result instanceof RemoveFailedResult) {
                            Log.debug("DbPartnerEditor tried to remove partner '" + model.getName() +
                                      "' from database " + db.getId() + " but there's data associated with it");
                            MessageBox.alert(I18N.CONSTANTS.removePartner(),
                                    I18N.MESSAGES.partnerHasDataWarning(model.getName()),
                                    null);
                        } else {
                            Log.debug("DbPartnerEditor removed partner '" + model.getName() +
                                      "' from database " + db.getId());
                            store.remove(model);
                        }
                    }
                });

    }

    @Override
    public PageId getPageId() {
        return PAGE_ID;
    }

    @Override
    public Object getWidget() {
        return view;
    }

    @Override
    public boolean navigate(PageState place) {
        return false;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void onSelectionChanged(ModelData selectedItem) {
        this.view.setActionEnabled(UIActions.DELETE, selectedItem != null);
    }
}
