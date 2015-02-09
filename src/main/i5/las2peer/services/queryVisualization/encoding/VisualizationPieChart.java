package i5.las2peer.services.queryVisualization.encoding;

import i5.las2peer.security.Context;

import java.sql.Types;
import java.util.HashSet;
import java.util.Iterator;

/**
 * VisualizationPieChart.java
 *<br>
 * Transforms/Converts a methodResult into a Google Pie Chart.
 * 
 */
public class VisualizationPieChart extends Visualization {

	public VisualizationPieChart() {
		super(VisualizationType.GOOGLEPIECHART);
	}
	
	public String generate(MethodResult methodResult, String[] visualizationParameters){
		String resultHTML = null;
		
		try {
			if(methodResult == null) {
				throw new Exception("Tried to transform an invalid (method) result set into Google Piechart Code!");
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
			
			//Pie Chart
			resultHTML += "var data = new google.visualization.DataTable();\n";
			if(columnCount != 2)
				throw new Exception("Cannot draw pie-chart with more than two columns as input!");
			
			resultHTML += "data.addColumn('string', '" + columnNames[0] + "');\n";
			resultHTML += "data.addColumn('number', '" + columnNames[1] + "');\n";
			resultHTML += "data.addRows([\n";
			
			
			while(iterator.hasNext()){
				Object[] currentRow = iterator.next();
				String firstCell = currentRow[0].toString();
				String secondCell = currentRow[1].toString();
				if(firstCell == null || firstCell.equals("null"))
					firstCell = "";
				if(iterator.hasNext()){
					resultHTML += "['" + firstCell + "'," + secondCell + "],\n";
				}
				else{ //Last Entry, close Array
					resultHTML += "['" + firstCell + "'," + secondCell + "]\n]";
				}
			}
			resultHTML += ");\n";
	        
	        resultHTML += "var options = {\n";
	        resultHTML += "'title':'"+visualizationParameters[0]+"',\n";
	        resultHTML += "};\n";
	        
	        resultHTML += "var chart = new google.visualization.PieChart(document.getElementById('" + randomNodeId + "'));\n";
	        resultHTML += "chart.draw(data, options);\n";
		        	
			resultHTML += "}\n</script>";
			
			return resultHTML;
		}
		catch (Exception e) {
			Context.logMessage(this, e.getMessage());
			try {
				 return super.visualizationException.generate(e, "Encoding into Pie Chart failed.");
			}
			catch(Exception ex) {
				Context.logMessage(this, ex.getMessage());
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
		
		if(numOfCols != 2) return false;
		if(visualizationParameters == null || visualizationParameters.length != 3) return false;
		if(!methodResult.getRowIterator().hasNext()) return false; //needs at least one column
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
