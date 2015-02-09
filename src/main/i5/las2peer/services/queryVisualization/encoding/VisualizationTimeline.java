package i5.las2peer.services.queryVisualization.encoding;

import i5.las2peer.security.Context;

import java.sql.Date;
import java.sql.Time;
import java.sql.Types;
import java.util.HashSet;
import java.util.Iterator;

/**
 * VisualizationTimeline.java
 *<br>
 * Transforms/Converts a methodResult into a Google Timeline-Chart.
 * 
 */
public class VisualizationTimeline extends Visualization {

	public VisualizationTimeline() {
		super(VisualizationType.GOOGLETIMELINECHART);
	}
	
	public String generate(MethodResult methodResult, String[] visualizationParameters){
		String resultHTML = null;
		
		try {
			if(methodResult == null) {
				throw new Exception("Tried to transform an invalid (method) result set into Google Timeline Code!");
			}
			
			String[] columnNames = methodResult.getColumnNames();
			Integer[] columnTypes = methodResult.getColumnDatatypes();
			Iterator<Object[]> iterator = methodResult.getRowIterator();
			int columnCount = columnTypes.length;
			
			String randomNodeId = getRandomId(10,  true);
			
			// The Basic HTML-Code needed for every visualization
			resultHTML = "<div id='" + randomNodeId + "' style='height: "+visualizationParameters[1]+"; width: "+visualizationParameters[2]+";'></div>\n";
			resultHTML += "<script>\n";			
			resultHTML += "var qv_script = document.createElement('script');\n";
			resultHTML += "qv_script.src = 'https://www.google.com/jsapi?callback=qv_loadChart';\n";
			resultHTML += "qv_script.type = 'text/javascript';\n";
			resultHTML += "document.getElementsByTagName('head')[0].appendChild(qv_script);\n";
			resultHTML += "function qv_loadChart(){\n";
			resultHTML += "google.load('visualization', '1', {packages: ['annotatedtimeline'], callback: qv_drawChart});\n";
			resultHTML += "}\n";
			resultHTML += "function qv_drawChart() {\n";
			
			//Timeline
			resultHTML += "var data = new google.visualization.DataTable();\n";
			
			//Column names and types
			String columnTypeString = "string";
			for(int i = 0; i < columnCount; i++){
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
				resultHTML += "data.addColumn('" + columnTypeString + "', '" + columnNames[i] + "');\n";
			}
			resultHTML += "data.addRows([\n";
			
			
			// add the individual rows
			while(iterator.hasNext()) {
				resultHTML += "[";
				
				Object[] currentRow = iterator.next();
				for(int i = 0; i < columnCount; i++) {
					if(i>0) resultHTML += ", ";
					switch(columnTypes[i]) {
						case Types.DATE:
							//TODO: this is wrong, it starts counting the month at 0...								
							resultHTML += " new Date(" + ((Date) currentRow[i]).getTime() + ")";
							break;
						case Types.TIME:
						case Types.TIMESTAMP:
							resultHTML += " new Date(" + ((Time) currentRow[i]).getTime() + ")";
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
							resultHTML += currentRow[i];
							break;
						default:
							String value = (String) currentRow[i];
							resultHTML += "\"" + value + "\"";
							break;
					};
				}
				if(iterator.hasNext()){
					resultHTML += "],\n";
				}
				else{
					resultHTML += "]\n]);\n"; //Last entry
				}
			}
	        
	        resultHTML += "var options = {\n";
	        resultHTML += "displayAnnotations: true\n";
	        resultHTML += "};\n";
	        
	        resultHTML += "var chart = new google.visualization.AnnotatedTimeLine(document.getElementById('" + randomNodeId + "'));\n";
	        resultHTML += "chart.draw(data, options);\n";
		        	
			resultHTML += "}\n</script>";
			
			return resultHTML;
		}
		catch (Exception e) {
			Context.logMessage(this, e.getMessage());
			try {
				 return super.visualizationException.generate(e, "Encoding into Timeline Chart failed.");
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
		Integer[] columnDatatypes = methodResult.getColumnDatatypes(); 
		int numOfCols = columnDatatypes.length;
		
		if(numOfCols < 2) return false;
		if(visualizationParameters == null || visualizationParameters.length != 3) return false;
		if(!methodResult.getRowIterator().hasNext()) return false; //needs at least one column
		if(!columnDatatypes[0].equals(Types.DATE)) return false;
		HashSet<Integer> acceptedValues = new HashSet<Integer>();
		acceptedValues.add(Types.BIGINT);
		acceptedValues.add(Types.DECIMAL);	
		acceptedValues.add(Types.DOUBLE);
		acceptedValues.add(Types.REAL);		
		acceptedValues.add(Types.INTEGER);
		acceptedValues.add(Types.SMALLINT);

		for(int i = 1; i < numOfCols; i++){
			if(!acceptedValues.contains(columnDatatypes[i])) return false;
		}
		return true;
	}
	
}