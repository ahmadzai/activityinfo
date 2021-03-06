package org.activityinfo.ui.client.component.formdesigner;
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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import org.activityinfo.core.client.ResourceLocator;
import org.activityinfo.model.form.*;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.promise.Promise;
import org.activityinfo.ui.client.component.form.field.FormFieldWidget;
import org.activityinfo.ui.client.component.formdesigner.container.FieldWidgetContainer;
import org.activityinfo.ui.client.component.formdesigner.container.SectionWidgetContainer;
import org.activityinfo.ui.client.component.formdesigner.container.WidgetContainer;
import org.activityinfo.ui.client.component.formdesigner.drop.NullValueUpdater;
import org.activityinfo.ui.client.component.formdesigner.event.WidgetContainerSelectionEvent;
import org.activityinfo.ui.client.component.formdesigner.header.HeaderPanel;
import org.activityinfo.ui.client.component.formdesigner.palette.FieldPalette;
import org.activityinfo.ui.client.component.formdesigner.properties.PropertiesPanel;
import org.activityinfo.ui.client.page.HasNavigationCallback;
import org.activityinfo.ui.client.page.NavigationCallback;
import org.activityinfo.ui.client.util.GwtUtil;
import org.activityinfo.ui.client.widget.Button;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * @author yuriyz on 07/04/2014.
 */
public class FormDesignerPanel extends Composite implements ScrollHandler, HasNavigationCallback, FormSavedGuard.HasSavedGuard {

    private final static OurUiBinder uiBinder = GWT
            .create(OurUiBinder.class);

    interface OurUiBinder extends UiBinder<Widget, FormDesignerPanel> {
    }

    private final Map<ResourceId, WidgetContainer> containerMap = Maps.newHashMap();
    private ScrollPanel scrollAncestor;
    private WidgetContainer selectedWidgetContainer;
    private HasNavigationCallback savedGuard = null;

    @UiField
    HTMLPanel containerPanel;
    @UiField
    FlowPanel dropPanel;
    @UiField
    PropertiesPanel propertiesPanel;
    @UiField
    HeaderPanel headerPanel;
    @UiField
    FieldPalette fieldPalette;
    @UiField
    Button saveButton;
    @UiField
    HTML statusMessage;
    @UiField
    HTML spacer;
    @UiField
    HTML paletteSpacer;

    public FormDesignerPanel(final ResourceLocator resourceLocator, @Nonnull final FormClass formClass) {
        FormDesignerStyles.INSTANCE.ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        propertiesPanel.setVisible(false);

        addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                scrollAncestor = GwtUtil.getScrollAncestor(FormDesignerPanel.this);
                scrollAncestor.addScrollHandler(FormDesignerPanel.this);
            }
        });
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                final FormDesigner formDesigner = new FormDesigner(FormDesignerPanel.this, resourceLocator, formClass);
                savedGuard = formDesigner.getSavedGuard();
                List<Promise<Void>> promises = Lists.newArrayList();
                buildWidgetContainers(formDesigner, formClass, 0, promises);
                Promise.waitAll(promises).then(new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        // ugly but we still have exception like: unsupportedoperationexception: domain is not supported.
                        fillPanel(formClass, formDesigner);
                    }

                    @Override
                    public void onSuccess(Void result) {
                        fillPanel(formClass, formDesigner);
                    }
                });

            }
        });
    }

    public void bind(EventBus eventBus) {
        eventBus.addHandler(WidgetContainerSelectionEvent.TYPE, new WidgetContainerSelectionEvent.Handler() {
            @Override
            public void handle(WidgetContainerSelectionEvent event) {
                selectedWidgetContainer = event.getSelectedItem();
                calcSpacerHeight();
            }
        });
    }

    private void fillPanel(final FormClass formClass, final FormDesigner formDesigner) {

        formClass.traverse(formClass, new TraverseFunction() {
            @Override
            public void apply(FormElement element, FormElementContainer container) {
                if (element instanceof FormField) {
                    FormField formField = (FormField) element;
                    WidgetContainer widgetContainer = containerMap.get(formField.getId());
                    if (widgetContainer != null) { // widget container may be null if domain is not supported, should be removed later
                        Widget widget = widgetContainer.asWidget();
                        formDesigner.getDragController().makeDraggable(widget, widgetContainer.getDragHandle());
                        dropPanel.add(widget);
                    }
                } else if (element instanceof FormSection) {
                    FormSection section = (FormSection) element;
                    WidgetContainer widgetContainer = containerMap.get(section.getId());
                    Widget widget = widgetContainer.asWidget();
                    formDesigner.getDragController().makeDraggable(widget, widgetContainer.getDragHandle());
                    dropPanel.add(widget);

                } else {
                    throw new UnsupportedOperationException("Unknown form element.");
                }
            }
        });
    }

    private void buildWidgetContainers(final FormDesigner formDesigner, FormElementContainer container, int depth, List<Promise<Void>> promises) {
        for (FormElement element : container.getElements()) {
            if (element instanceof FormSection) {
                FormSection formSection = (FormSection) element;
                containerMap.put(formSection.getId(), new SectionWidgetContainer(formDesigner, formSection));
                buildWidgetContainers(formDesigner, formSection, depth + 1, promises);
            } else if (element instanceof FormField) {
                final FormField formField = (FormField) element;
                Promise<Void> promise = formDesigner.getFormFieldWidgetFactory().createWidget(formDesigner.getFormClass(), formField, NullValueUpdater.INSTANCE).then(new Function<FormFieldWidget, Void>() {
                    @Nullable
                    @Override
                    public Void apply(@Nullable FormFieldWidget input) {
                        containerMap.put(formField.getId(), new FieldWidgetContainer(formDesigner, input, formField));
                        return null;
                    }
                });
                promises.add(promise);
            }
        }
    }

    @Override
    public void onScroll(ScrollEvent event) {
        calcSpacerHeight();
    }

    int previousVerticalScrollPosition = 0;
    private void calcSpacerHeight() {
        int verticalScrollPosition = scrollAncestor.getVerticalScrollPosition();
        if (verticalScrollPosition > Metrics.MAX_VERTICAL_SCROLL_POSITION) {
            int height = verticalScrollPosition - Metrics.MAX_VERTICAL_SCROLL_POSITION;

            int panelHeight = dropPanel.getOffsetHeight();
            int propertiesColumnHeight = propertiesPanel.getOffsetHeight() + spacer.getOffsetHeight();
            if (propertiesColumnHeight > panelHeight && verticalScrollPosition > previousVerticalScrollPosition) {
                // AI-924 : avoid never ending scrolls
                return;
            }

//            int selectedWidgetTop = 0;
//            if (selectedWidgetContainer != null) {
//                selectedWidgetTop = selectedWidgetContainer.asWidget().getAbsoluteTop();
//            }
//            if (selectedWidgetTop < 0) {
//                height = height + selectedWidgetTop;
//            }

            //GWT.log("verticalPos = " + verticalScrollPosition + ", height = " + height + ", selectedWidgetTop = " + selectedWidgetTop);
            spacer.setHeight(height + "px");
            paletteSpacer.setHeight(height + "px");
        } else {
            spacer.setHeight("0px");
            paletteSpacer.setHeight("0px");
        }
        previousVerticalScrollPosition = verticalScrollPosition;
    }

    public Map<ResourceId, WidgetContainer> getContainerMap() {
        return containerMap;
    }

    public FlowPanel getDropPanel() {
        return dropPanel;
    }

    public PropertiesPanel getPropertiesPanel() {
        return propertiesPanel;
    }

    public HeaderPanel getHeaderPanel() {
        return headerPanel;
    }

    public FieldPalette getFieldPalette() {
        return fieldPalette;
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public HTML getStatusMessage() {
        return statusMessage;
    }

    public HasNavigationCallback getSavedGuard() {
        return savedGuard;
    }

    public void setSavedGuard(HasNavigationCallback savedGuard) {
        this.savedGuard = savedGuard;
    }

    @Override
    public void navigate(NavigationCallback callback) {
        if (savedGuard != null) {
            savedGuard.navigate(callback);
        }
    }
}
