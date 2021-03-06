package org.activityinfo.ui.client.page.entry.form;

import com.extjs.gxt.ui.client.widget.ListRenderer;
import com.google.common.base.Strings;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import org.activityinfo.legacy.shared.model.LocationDTO;

/**
 * Renders location search results
 */
public class LocationResultsRenderer extends ListRenderer<LocationDTO> {

    @Override
    protected void renderItem(LocationDTO locationDTO, SafeHtmlBuilder html) {
        html.appendHtmlConstant("<div class=locSerResult>");
        html.appendHtmlConstant("<div class=locSerMarker>");
        html.appendEscaped(Strings.nullToEmpty(locationDTO.getMarker()));
        html.appendHtmlConstant("</div>");

        html.appendHtmlConstant("<div class=locSerWrap>");
        html.appendHtmlConstant("<div class=locSerName>");
        html.appendEscaped(locationDTO.getName());
        html.appendHtmlConstant("</div>");

        if(locationDTO.hasAxe()) {
            html.appendHtmlConstant("<div class=locSerAxe>");
            html.appendEscaped(locationDTO.getAxe());
            html.appendHtmlConstant("</div>");
        }
        html.appendHtmlConstant("</div>");
        html.appendHtmlConstant("</div>");
    }
}
