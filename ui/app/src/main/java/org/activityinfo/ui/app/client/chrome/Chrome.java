package org.activityinfo.ui.app.client.chrome;

import com.google.gwt.dom.client.Style;
import org.activityinfo.ui.app.client.store.AppStores;
import org.activityinfo.ui.style.BaseStyles;
import org.activityinfo.ui.vdom.shared.html.HtmlTag;
import org.activityinfo.ui.vdom.shared.tree.PropMap;
import org.activityinfo.ui.vdom.shared.tree.VNode;

import static org.activityinfo.ui.app.client.chrome.LeftPanel.leftPanel;
import static org.activityinfo.ui.app.client.chrome.MainPanel.mainPanel;
import static org.activityinfo.ui.style.PagePreLoader.preLoader;
import static org.activityinfo.ui.vdom.shared.html.H.*;

public class Chrome {

    public static final String ROOT_ID = "root";

    /**
     * Renders the page skeleton
     */
    public static VNode renderPage(PageContext pageContext, AppStores app) {

        return html(
                head(
                        meta(charset(UTF_8_CHARSET)),
                        meta(viewport(DEVICE_WIDTH, 1.0, 1.0)),
                        title(pageContext.getApplicationTitle()),
                        link(stylesheet(pageContext.getStylesheetUrl())),
                        script(pageContext.getBootstrapScriptUrl())),
                body(
                    preLoader(),
                    historyIFrame()
                ));
    }


    public static VNode mainSection(AppStores app) {
        return section(id(ROOT_ID),
            leftPanel(app),
            mainPanel(app),
            rightPanel()
        );
    }

    private static VNode historyIFrame() {
        PropMap map = new PropMap();
        map.set("src", "javascript:''");
        map.setId("__gwt_historyFrame");
        map.setStyle(style().setPosition(Style.Position.ABSOLUTE).width(0).height(0).border(0));

        return new VNode(HtmlTag.IFRAME, map);
    }

    private static VNode rightPanel() {
        return div(BaseStyles.RIGHTPANEL);
    }

}