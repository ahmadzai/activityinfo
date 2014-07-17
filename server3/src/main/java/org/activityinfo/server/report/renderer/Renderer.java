package org.activityinfo.server.report.renderer;

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

import org.activityinfo.legacy.shared.reports.model.ReportElement;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @see org.activityinfo.legacy.shared.reports.model.ReportElement
 */
public interface Renderer {

    /**
     * Renders a <code>ReportElement</code> to an <code>OutputStream</code>
     *
     * @param element The <code>ReportElement</code> to render
     * @param os      The <code>OutputStream</code> to which to render the element.
     * @throws IOException upon an underlying exception.
     */
    public void render(ReportElement element, OutputStream os) throws IOException;

    /**
     * Gets the MIME type of the output generated by this renderer.
     *
     * @return the MIME type of the output generated by this renderer.
     */
    String getMimeType();

    /**
     * Gets the file suffix for this type of output. Should include the dot: for
     * example, ".pdf" or ".png"
     *
     * @return the suffix to be appended to the filename of files created by
     * this renderer.
     */
    String getFileSuffix();

}
