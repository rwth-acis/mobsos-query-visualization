package i5.las2peer.services.queryVisualization.encoding;

import i5.las2peer.security.Context;

import java.sql.Date;
import java.sql.Time;
import java.sql.Types;
import java.util.Iterator;


/**
 * VisualizationGoogleTable.java
 * <br>
 * Transforms/Converts a methodResult into a Google Table.
 * 
 */
public class VisualizationGoogleTable extends Visualization {

	public VisualizationGoogleTable() {
		super(VisualizationType.GOOGLETABLE);
	}
	
	public String generate(MethodResult methodResult, String[] visualizationParameters){
		
		StringBuilder resultHTML = new StringBuilder();
		
		try {
			if(methodResult == null) {
				throw new Exception("Tried to transform an invalid (method) result set into Google Table Code!");
			}
			
			String[] columnNames = methodResult.getColumnNames();
			Integer[] columnTypes = methodResult.getColumnDatatypes();
			Iterator<Object[]> iterator = methodResult.getRowIterator();
			
			int columnCount = columnTypes.length;
			
			String randomNodeId = getRandomId(10,  true);
			
			// The Basic HTML-Code needed for every visualization
			resultHTML.append("<div id='" + randomNodeId + "' style='height: "+visualizationParameters[1]+"; width: "+visualizationParameters[2]+";'></div>\n");
			resultHTML.append("<script>\n");			
			resultHTML.append("var qv_script = document.createElement('script');\n");
			resultHTML.append("qv_script.src = 'https://www.google.com/jsapi?callback=qv_loadChart';\n");
			resultHTML.append("qv_script.type = 'text/javascript';\n");
			resultHTML.append("document.getElementsByTagName('head')[0].appendChild(qv_script);\n");
			resultHTML.append("function qv_loadChart(){\n");
			resultHTML.append("google.load('visualization', '1', {packages: ['table'], callback: qv_drawChart});\n");
			resultHTML.append("}\n");
			resultHTML.append("function qv_drawChart() {\n");
			
			resultHTML.append("var data = new google.visualization.DataTable();\n");
			
			//Column names and types
			for(int i = 0; i < columnCount; i++){
				String columnTypeString = "string";
				switch(columnTypes[i]) {
				case Types.BOOLEAN:
					columnTypeString = "boolean";
					break;
				case Types.DATE:
					columnTypeString = "date";
					break;
				case Types.TIME:
				case Types.TIMESTAMP:
					columnTypeString = "datetime";
					break;
				case Types.BIGINT:
				case Types.DECIMAL:
				case Types.DOUBLE:
				case Types.FLOAT:
				case Types.INTEGER:
				case Types.NUMERIC:
				case Types.REAL:
				case Types.SMALLINT:
					columnTypeString = "number";
					break;
				default:
					// do nothing, just treat it as string
					break;
				};
				resultHTML.append("data.addColumn('" + columnTypeString + "', '" + columnNames[i] + "');\n");
			}
			resultHTML.append("data.addRows([\n");
			
			
			// add the individual rows
			while(iterator.hasNext()) {
				resultHTML.append("[");
				
				Object[] currentRow = iterator.next();
				for(int i = 0; i < columnCount; i++) {
					if(i>0) resultHTML.append(", ");
					switch(columnTypes[i]) {
						case Types.DATE:
							//TODO: this is wrong, it starts counting the month at 0...								
							try {
								resultHTML.append(" new Date(").append(((Date) currentRow[i]).getTime()).append(")");
							} catch (Exception e) {
								resultHTML.append(" null");
							}
							break;
						case Types.TIME:
						case Types.TIMESTAMP:
							try {
								resultHTML.append(" new Date(").append(((Time) currentRow[i]).getTime()).append(")");
							} catch (Exception e) {
								resultHTML.append(" null");
							}
							break;
						case Types.BOOLEAN:
						case Types.BIGINT:
						case Types.DECIMAL:
						case Types.NUMERIC:
						case Types.DOUBLE:
						case Types.REAL:
						case Types.FLOAT:
						case Types.INTEGER:
						case Types.SMALLINT:
							resultHTML.append(currentRow[i]);
							break;
						default:
							String value = (String) currentRow[i];
							resultHTML.append("\"").append(value).append("\"");
							break;
					};
				}
				if(iterator.hasNext()){
					resultHTML.append("],\n");
				}
				else{
					resultHTML.append("]\n"); //Last entry
				}
			}

			resultHTML.append("]);\n"); //Last entry
			
	        resultHTML.append("var chart = new google.visualization.Table(document.getElementById('" + randomNodeId + "'));\n");
	        resultHTML.append("chart.draw(data, null);\n");
		        	
			resultHTML.append("}\n</script>");
			
			return resultHTML.toString();
		}
		catch (Exception e) {
			Context.logMessage(this, e.getMessage());
			try {
				return  super.visualizationException.generate(e, "Encoding into Google Table Chart failed.");
			}
			catch(Exception ex) {
				Context.logError(this, ex.getMessage());
				return "Unknown/handled error occurred!";
			}
		}
	}
	
	private String getRandomId(int length, boolean startWithLetter){
		String text = "";
		String possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		
		if(startWithLetter) {
			text += possible.charAt((int) Math.floor(Math.random() * 52));
			length--;
		}
		
	    for(int i=0; i < length; i++)
	        text += possible.charAt((int) Math.floor(Math.random() * possible.length()));

	    return text;
		
	}

	public boolean check(MethodResult methodResult, String[] visualizationParameters) {
		if(visualizationParameters == null || visualizationParameters.length != 3) return false;
		return true;
	}
	
}
