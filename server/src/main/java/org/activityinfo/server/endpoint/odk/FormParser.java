package org.activityinfo.server.endpoint.odk;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class FormParser {

    public SiteFormData parse(String xml) {
        SiteFormData data = null;

        Element root = getRoot(xml);
        if (root != null) {
            data = new SiteFormData();
            NodeList list = root.getElementsByTagName("*");
            for (int i = 0; i < list.getLength(); i++) {
                Element element = (Element) list.item(i);
                String name = element.getTagName();
                if ("instanceID".equals(name)) {
                    data.setInstanceID(string(element));
                } else if ("activity".equals(name)) {
                    data.setActivity(integer(element));
                } else if ("partner".equals(name)) {
                    data.setPartner(integer(element));
                } else if ("locationname".equals(name)) {
                    data.setLocationname(string(element));
                } else if ("gps".equals(name)) {
                    data.setGps(string(element));
                } else if ("date1".equals(name)) {
                    data.setDate1String(string(element));
                    data.setDate1(date(element));
                } else if ("date2".equals(name)) {
                    data.setDate2String(string(element));
                    data.setDate2(date(element));
                } else if ("comments".equals(name)) {
                    data.setComments(string(element));
                } else if (name.startsWith("indicator-")) {
                    data.addIndicator(integer(name.substring(10)), element.getTextContent());
                } else if (name.startsWith("attributeGroup-")) {
                    data.addAttributeGroup(integer(name.substring(15)), element.getTextContent());
                }
            }
        }
        return data;
    }

    private Element getRoot(String xml) {
        Element root = null;
        try {
            InputSource source = new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8")));
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            root = builder.parse(source).getDocumentElement();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return root;
    }

    private String string(Element element) {
        String result = element.getTextContent();
        if (result == null) {
            result = "";
        }
        return result;
    }

    private int integer(Element element) {
        return integer(element.getTextContent());
    }

    private int integer(String s) {
        int result = 0;
        try {
            result = Integer.parseInt(s);
        } catch (Exception e) {
            // just return 0;
        }
        return result;
    }

    private Date date(Element element) {
        String date = element.getTextContent();
        if (date == null || date.isEmpty()) {
            return new Date();
        }
        Date result = null;
        try {
            result = new SimpleDateFormat("yyyy-MM-dd").parse(date);
        } catch (Exception e) {
            // just return null;
        }
        return result;
    }
}
