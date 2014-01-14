package org.activityinfo.client.importer.data;

import java.util.List;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter.DEFAULT;

import com.google.common.collect.Lists;

/** 
 * An import source pasted in to a text field by the user.
 *
 */
public class PastedImportSource implements ImportSource {

	private static final char QUOTE_CHAR = '"';
	private String text;
	//private List<Integer> rowStarts;
	private List<ImportColumnDescriptor> columns;
	private List<ImportRow> rows;
	
	private String delimeter;

	public PastedImportSource(String text) {
		this.text = text;
	}

	
	@Override
	public List<ImportColumnDescriptor> getColumns() {
		ensureParsed();
		return columns;
	}
	
	private void ensureParsed() {
		if(rows == null) {
			parseRows();
		}
	}
	
	private void parseRows() {
		
		this.rows = Lists.newArrayList();
		int headerEnds = text.indexOf('\n');
		String headerRow = text.substring(0, headerEnds);
		this.delimeter = guessDelimeter(headerRow);
		
		String[] headers = parseRow(headerRow);
		parseHeaders(headers);
		
		int rowStarts = headerEnds + 1;
		while(true) {
			int rowEnds = text.indexOf('\n', rowStarts);
			if(rowEnds == -1) {
				return;
			}
			
			rows.add(new PastedImportRow(parseRow(text.substring(rowStarts, rowEnds))));
			rowStarts = rowEnds + 1;
		}
	}


	private String[] parseRow(String row) {
		row = maybeRemoveCarriageReturn(row);
		boolean usesQuote = row.indexOf(QUOTE_CHAR) != -1;
		if(usesQuote) {
			String[] cols = new String[columns.size()];
			int colIndex = 0;
			boolean quoted = false;
			char delimiterChar = delimeter.charAt(0);
			StringBuilder col = new StringBuilder();
			
			int charIndex = 0;
			int numChars = row.length();
			while(charIndex < numChars) {
				char c = row.charAt(charIndex);
				if(c == QUOTE_CHAR) {
					if(charIndex+1 < numChars && row.charAt(charIndex+1) == QUOTE_CHAR) {
						col.append(QUOTE_CHAR);
						charIndex += 2;
					} else {
						quoted = !quoted;
						charIndex ++;
					}
				} else if(!quoted && c == delimiterChar) {
					cols[colIndex] = col.toString();
					col.setLength(0);
					charIndex++;
					colIndex++;
					if(colIndex >= cols.length) {
						return cols;
					}
				} else {
					col.append(c);
					charIndex ++;
				}
			}
			
			// final column
			cols[colIndex] = col.toString();
			
			return cols;
			
		} else {
			return row.split(delimeter);
		}
	}
	
	private String guessDelimeter(String headerRow) {
		if(headerRow.contains("\t")) {
			return "\t";
		} else {
			return ",";
		}
	}

	private void parseHeaders(String headers[]) {
		columns = Lists.newArrayList();
		for(int i=0;i!=headers.length;++i) {
			ImportColumnDescriptor column = new ImportColumnDescriptor();
			column.setIndex(i);
			column.setHeader(headers[i]);
			columns.add(column);
		}
	}
	
    @Override
	public List<ImportRow> getRows() {
    	ensureParsed();
		return rows;
	}
    
    public String get(int row, int column) {
    	ensureParsed();
    	return rows.get(row).getColumnValue(column);
    }

	private String maybeRemoveCarriageReturn(String row) {
        if(row.endsWith("\r")) {
            return row.substring(0, row.length() - 1);
        } else {
            return row;
        }
    }

	@Override
	public String getColumnHeader(Integer columnIndex) {
		return columns.get(columnIndex).getHeader();
	}
}
