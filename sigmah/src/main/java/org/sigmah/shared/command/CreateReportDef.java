/*
 * All Sigmah code is released under the GNU General Public License v3
 * See COPYRIGHT.txt and LICENSE.txt.
 */

package org.sigmah.shared.command;

import org.sigmah.shared.command.result.CreateResult;
import org.sigmah.shared.report.model.Report;


/**
 *
 * Creates a new Report Definition
 *
 * Returns {@link org.sigmah.shared.command.result.CreateResult}
 *
 * @author Alex Bertram
 */
public class CreateReportDef implements Command<CreateResult>{
	
	private String xml;
	private Integer databaseId;
	private Report report;
	
	protected CreateReportDef() {
		
	}

	public CreateReportDef(int databaseId, String xml) {
		super();
		this.databaseId = databaseId;
		this.xml = xml;
	}
	
	public CreateReportDef(Report report){
		super();
		this.databaseId = 0;
		this.report = report;
	}

	public String getXml() {
		return xml;
	}

	public void setXml(String xml) {
		this.xml = xml;
	}

	public Integer getDatabaseId() {
		return databaseId;
	}

	public void setDatabaseId(Integer databaseId) {
		this.databaseId = databaseId;
	}

	public Report getReport() {
		return report;
	}

	public void setReport(Report report) {
		this.report = report;
	}

}
