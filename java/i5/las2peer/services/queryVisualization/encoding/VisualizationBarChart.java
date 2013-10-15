package i5.las2peer.services.queryVisualization.encoding;

import i5.las2peer.security.Context;

import java.sql.Types;
import java.util.HashSet;
import java.util.Iterator;

/**
 * VisualizationBarChart.java
 * <br>
 * Transforms/Converts a methodResult into a Google Bar Chart.
 * <br>
 * Data is represented as:
 * <br>
 * 				 ------------ Second Column
 * <br>
 * First Column  ...
 * <br>
 * 				 ------------ Last Column
 * 
 */
public class VisualizationBarChart extends Visualization {

	public VisualizationBarChart() {
		super(VisualizationType.GOOGLEBARCHART);
	}
	
	public String generate(MethodResult methodResult, String[] visualizationParameters){
		String resultHTML = null;
		
		try {
			if(methodResult == null) {
				throw new Exception("Tried to transform an invalid (method) result set into Google Chart Code!");
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
			resultHTML += "google.load('visualization', '1', {packages: ['corechart'], callback: qv_drawChart});\n";
			resultHTML += "}\n";
			resultHTML += "function qv_drawChart() {\n";
			
			//Bar Chart
			if(columnCount < 2)
				throw new Exception("Cannot draw bar-chart with only one column!");
			resultHTML += "var data = google.visualization.arrayToDataTable([\n";
			
			//Column Names
			resultHTML += "[";
			for(int i = 0; i < columnCount-1; i++){
				resultHTML += "'" + columnNames[i] + "', ";
			}
			
			resultHTML += "'" + columnNames[columnCount-1] + "'],\n";
			
			String[] currentRowEntries = new String[columnCount];
			while(iterator.hasNext()){
				Object[] currentRow = iterator.next();
				for(int i = 0; i < columnCount; i++){
					currentRowEntries[i] = currentRow[i].toString();
				}
				
				//Treat first entry as a string
				resultHTML += "['" + currentRowEntries[0] + "', ";
				
				for(int j = 1; j < columnCount-1; j++){
					resultHTML += currentRowEntries[j] + ", ";
				}
				if(iterator.hasNext())
					resultHTML += currentRowEntries[columnCount-1] + "],\n";
				else
					//Last Entry
					resultHTML += currentRowEntries[columnCount-1] + "]\n";
			}
			resultHTML += "]);\n";
			
			
	        
	        resultHTML += "var options = {\n";
	        resultHTML += "'title':'"+visualizationParameters[0]+"',\n";
	        resultHTML += "};\n";
	        
	        resultHTML += "var chart = new google.visualization.BarChart(document.getElementById('" + randomNodeId + "'));\n";
	        resultHTML += "chart.draw(data, options);\n";
		        	
			resultHTML += "}\n</script>";
			
			return resultHTML;
		}
		catch (Exception e) {
			Context.logMessage(this, e.getMessage());
			try {
				return  super.visualizationException.generate(e, "Encoding into Bar Chart failed.");
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
		if(!methodResult.getRowIterator().hasNext()) return false; //needs at least one column
		if(numOfCols < 2) return false;
		if(visualizationParameters == null || visualizationParameters.length != 3) return false;
		
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
