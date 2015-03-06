package i5.las2peer.services.queryVisualization.encoding;


import i5.las2peer.security.Context;

import java.sql.Types;
import java.util.HashSet;
import java.util.Iterator;

/**
 * VisualizationRadarChart.java
 *<br>
 * Transforms/Converts a methodResult into a Google Radar-Chart.
 * 
 */
public class VisualizationRadarChart extends Visualization {

	public VisualizationRadarChart() {
		super(VisualizationType.GOOGLERADARCHART);
	}
	
	public String generate(MethodResult methodResult, String[] visualizationParameters){
		StringBuilder resultHTML = new StringBuilder();
		
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
			resultHTML.append("<div id=\"" + randomNodeId + "\"></div>\n");
			resultHTML.append("<script>\n");			
			resultHTML.append("var qv_script = document.createElement('script');\n");
			resultHTML.append("qv_script.src = 'https://www.google.com/jsapi?callback=qv_loadChart';\n");
			resultHTML.append("qv_script.type = 'text/javascript';\n");
			resultHTML.append("document.getElementsByTagName('head')[0].appendChild(qv_script);\n");
			resultHTML.append("function qv_loadChart(){\n");
			resultHTML.append("google.load('visualization', '1', {packages: ['imagechart'], callback: qv_drawChart});\n");
			resultHTML.append("}\n");
			resultHTML.append("function qv_drawChart() {\n");
			
			//Radar Chart
			if(columnCount < 1)
				throw new Exception("Cannot draw radar-chart with only one column!");
			resultHTML.append("var data = google.visualization.arrayToDataTable([\n");
			
			//Column Names
			resultHTML.append("[");
			for(int i = 0; i < columnCount-1; i++){
				resultHTML.append("'" + columnNames[i]).append("', ");
			}
			
			resultHTML.append("'" + columnNames[columnCount-1]).append("'],\n");
			
			String[] currentRowEntries = new String[columnCount];
			while(iterator.hasNext()){
				Object[] currentRow = iterator.next();
				for(int i = 0; i < columnCount; i++){
					currentRowEntries[i] = currentRow[i].toString();
				}
				//First entry has to be a String
				resultHTML.append("['" + currentRowEntries[0]).append("', ");
				for(int j = 1; j < columnCount-1; j++){
					resultHTML.append(currentRowEntries[j]).append(", ");
				}
				if(iterator.hasNext())
					resultHTML.append(currentRowEntries[columnCount-1]).append("],\n");
				else
					//Last Entry
					resultHTML.append(currentRowEntries[columnCount-1]).append("]\n");
			}
			resultHTML.append("]);\n");
			
            //calculation of overall max value -> optionsScaleMax
			resultHTML.append("var optionsScaleMax = 0;\n");
			resultHTML.append("for(var i = 1, numOfCols=data.getNumberOfColumns(); i<numOfCols; i++){\n");
		    resultHTML.append("  optionsScaleMax = optionsScaleMax < data.getColumnRange(i).max ? data.getColumnRange(i).max : optionsScaleMax;\n");
		    resultHTML.append("}\n");
		    resultHTML.append("optionsScaleMax = Math.ceil(optionsScaleMax);\n");

            //add zeros if value does not exist in new datatable
		    resultHTML.append("for(var i = 0, numOfRowsNew = data.getNumberOfRows(); i<numOfRowsNew; i++)\n");
		    resultHTML.append("for(var j = 1, numOfColsNew = data.getNumberOfColumns(); j<numOfColsNew; j++){\n");
		    resultHTML.append("  if(data.getValue(i,j) == null) data.setValue(i,j,0);\n");
		    resultHTML.append("}\n");	
		    
		    resultHTML.append("    var optionsLabels = '0:';\n");
		    resultHTML.append("    var optionsScale = '';\n");
		    resultHTML.append("    var optionsLineWidth = '';\n");
		    resultHTML.append("    var optionsLegend = 'r|l';\n");
		    resultHTML.append("     var optionsLegendSize = '000000,12';\n");

		    resultHTML.append("    for(var i = 0, numOfRows = data.getNumberOfRows(); i<numOfRows; i++){\n");
		    resultHTML.append("      optionsLabels += '|' + data.getValue(i,0);\n");
		    resultHTML.append("    }\n");

		    resultHTML.append("    data.removeColumn(0);\n");

		    resultHTML.append("    for(var i = 0, numOfCols=data.getNumberOfColumns(); i<numOfCols; i++){\n");
		    resultHTML.append("        optionsScale += '0,'+optionsScaleMax+(i==numOfCols-1 ? '' : ',');\n");
		    resultHTML.append("        optionsLineWidth += '2' + (i==numOfCols-1 ? '' : '|');\n");
		    resultHTML.append("    }\n");
 
		    resultHTML.append("    optionsLegend = 't|a';\n");
		    resultHTML.append("    optionsLegendSize = '000000,10';\n");
                
		    resultHTML.append("   var options = {\n");
		    resultHTML.append("      cht: 'r',\n");
		    resultHTML.append("      chxr: '1,0,'+optionsScaleMax+','+optionsScaleMax/10,\n");
		    resultHTML.append("      chds: optionsScale,\n");
		    resultHTML.append("      chs: '450x320',\n"); 
		    resultHTML.append("      chls: optionsLineWidth,\n");
		    resultHTML.append("      chxt: 'x,y',\n");
		    resultHTML.append("      chxl: optionsLabels,\n");
		    resultHTML.append("      chdlp: optionsLegend,\n");
		    resultHTML.append("      chdls: optionsLegendSize\n");
		    resultHTML.append("    };\n");

		    resultHTML.append("    var view = new google.visualization.DataView(data);\n");
		    resultHTML.append("    view.setRows(view.getViewRows().concat([0]));\n");
		    resultHTML.append("var chart = new google.visualization.ImageChart(document.getElementById('" + randomNodeId).append("'));\n");
	        resultHTML.append("chart.draw(view.toDataTable(), options);\n");
		        	
			resultHTML.append("}\n</script>");
			
			return resultHTML.toString();
		}
		catch (Exception e) {
			Context.logMessage(this, e.getMessage());
			try {
				return super.visualizationException.generate(e, "Encoding into Line Chart failed.");
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
