package org.activityinfo.ui.client.component.form.field.attachment;
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

import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.http.client.*;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.shared.Log;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.attachment.Attachment;

/**
 * @author yuriyz on 8/12/14.
 */
public class AttachmentUploadRow extends Composite {

    public interface ValueChangedCallback {
        void fireValueChanged();
    }

    interface OurUiBinder extends UiBinder<FormPanel, AttachmentUploadRow> {
    }

    private static OurUiBinder ourUiBinder = GWT.create(OurUiBinder.class);

    private boolean readOnly;
    private String servingUrl = null;
    private final Attachment attachment;
    private final ResourceId resourceId;

    @UiField
    Button removeButton;
    @UiField
    AnchorElement fileName;
    @UiField
    HTMLPanel thumbnailContainer;

    public AttachmentUploadRow(Attachment attachment, ResourceId resourceId) {
        initWidget(ourUiBinder.createAndBindUi(this));

        this.attachment = attachment;
        this.resourceId = resourceId;

        fetchServingUrl();
    }

    private void setFileName() {
        boolean hasFile = !Strings.isNullOrEmpty(servingUrl);

        fileName.setHref(hasFile ? servingUrl : "#");
        fileName.setInnerSafeHtml(SafeHtmlUtils.fromString(
                hasFile ? " " + attachment.getFilename() : " " + I18N.CONSTANTS.noFileSelected()));
    }

    public Button getRemoveButton() {
        return removeButton;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public Attachment getValue() {
        return attachment;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        this.removeButton.setEnabled(!readOnly);
    }

    private void fetchServingUrl() {
        try {
            RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, Uploader.getBaseUrl(attachment.getBlobId(), resourceId) + "/blobUrl");
            requestBuilder.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    servingUrl = response.getText();
                    setState();
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    servingUrl = null;
                    Log.error("Failed to fetch attachment serving url. ", exception);
                    setState();
                }
            });
        } catch (RequestException e) {
            servingUrl = null;
            Log.error("Failed to send request for fetching serving url. ", e);
            setState();
        }
    }

    public boolean isValid() {
        return !Strings.isNullOrEmpty(servingUrl);
    }

    private void setState() {
        setThumbnail();
        setFileName();
    }

    @Override
    public Widget asWidget() {
        return this;
    }

    private void setThumbnail() {
        thumbnailContainer.clear();

        if (attachment.getMimeType().contains("pdf")) {
            appendThumbnailImage("icons.filePdf");
        } else {
            appendThumbnailImage("icons.file");
        }
    }

    private void appendThumbnailImage(String iconClassName) {
        thumbnailContainer.add(new HTML("<span class=\"{" + iconClassName + "}\"/>"));
    }
}
