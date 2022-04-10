package i5.las2peer.services.mobsos.queryVisualization.encoding;

import java.util.ListIterator;

import i5.las2peer.api.Context;
import i5.las2peer.api.logging.MonitoringEvent;

/**
 * VisualizationHTMLTable.java <br>
 * Transforms/Converts a methodResult into a HTML Table (or HTML List if there is only one column in the row list)
 * Warning: The HTML table/list does not contain/list the datatypes of the cells/columns! <br>
 * - Simple HTML-Table with a small border <br>
 * - The first row contains the column names <br>
 * - There is no denotation of the columns' datatypes
 * 
 */
public class VisualizationHTMLTable extends Visualization {

	public VisualizationHTMLTable() {
		super(VisualizationType.HTMLTABLE);
	}

	@Override
	public String generate(MethodResult methodResult, String[] visualizationParameters) {

		StringBuilder resultHTML = null;

		try {
			if (methodResult == null) {
				throw new Exception("Tried to transform an invalid (method) result set into HTML!");
			}

			String[] columnNames = methodResult.getColumnNames();
			Integer[] columnTypes = methodResult.getColumnDatatypes();
			ListIterator<Object[]> iterator = methodResult.getRowIterator();
			int columnCount = columnTypes.length;

			if (columnCount < 2) {
				// Create a HTML list
				resultHTML = new StringBuilder("<ul>\n");

				// add the elements
				while (iterator.hasNext()) {
					Object[] currentRow = iterator.next();
					for (int i = 0; i < columnCount; i++) {
						String cellValue = currentRow[i].toString();
						if (cellValue == null || cellValue.equals("null")) {
							cellValue = "";
						}
						resultHTML.append("\t<li>").append(cellValue).append("</li>\n");
					}
				}
				resultHTML.append("</ul>\n");
			} else {
				// Create a HTML Table
				resultHTML = new StringBuilder("<table border=\"1\">\n\t<tr>\n");

				// add the heading/column names
				for (int i = 0; i < columnCount; i++) {
					resultHTML.append("\t\t<th>").append(columnNames[i]).append("</th>\n");
				}
				resultHTML.append("\t</tr>\n");

				// the the row values
				while (iterator.hasNext()) {
					resultHTML.append("\t<tr>\n");
					Object[] currentRow = iterator.next();
					for (int i = 0; i < columnCount; i++) {
						String cellValue = currentRow[i].toString();

						if (cellValue == null || cellValue.equals("null")) {
							cellValue = "";
						}

						resultHTML.append("\t\t<td>").append(cellValue).append("</td>\n");
					}
					resultHTML.append("\t</tr>\n");
				}
				resultHTML.append("</table>\n");
			}

			return resultHTML.toString();
		} catch (Exception e) {
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, e.getMessage().toString());
			try {
				return super.visualizationException.generate(e, "Encoding into HTML format failed.");
			} catch (Exception ex) {
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, ex.getMessage().toString());
				return "Unknown/handled error occurred!";
			}
		}
	}

	// Always true
	@Override
	public boolean check(MethodResult methodResult, String[] visualizationParameters) {
		return true;
	}

	@Override
	public byte[] generatePNG(MethodResult methodResult, String[] visualizationParamters) {
		// TODO Auto-generated method stub
		return null;
	}

}
