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
			resultHTML = "<div id=\"" + randomNodeId + "\"></div>\n";
			resultHTML += "<script>\n";			
			resultHTML += "var qv_script = document.createElement('script');\n";
			resultHTML += "qv_script.src = 'https://www.google.com/jsapi?callback=qv_loadChart';\n";
			resultHTML += "qv_script.type = 'text/javascript';\n";
			resultHTML += "document.getElementsByTagName('head')[0].appendChild(qv_script);\n";
			resultHTML += "function qv_loadChart(){\n";
			resultHTML += "google.load('visualization', '1', {packages: ['imagechart'], callback: qv_drawChart});\n";
			resultHTML += "}\n";
			resultHTML += "function qv_drawChart() {\n";
			
			//Radar Chart
			if(columnCount < 1)
				throw new Exception("Cannot draw radar-chart with only one column!");
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
				//First entry has to be a String
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
			
            //calculation of overall max value -> optionsScaleMax
			resultHTML += "var optionsScaleMax = 0;\n";
			resultHTML += "for(var i = 1, numOfCols=data.getNumberOfColumns(); i<numOfCols; i++){\n";
		    resultHTML += "  optionsScaleMax = optionsScaleMax < data.getColumnRange(i).max ? data.getColumnRange(i).max : optionsScaleMax;\n";
		    resultHTML += "}\n";
		    resultHTML += "optionsScaleMax = Math.ceil(optionsScaleMax);\n";

            //add zeros if value does not exist in new datatable
		    resultHTML += "for(var i = 0, numOfRowsNew = data.getNumberOfRows(); i<numOfRowsNew; i++)\n";
		    resultHTML += "for(var j = 1, numOfColsNew = data.getNumberOfColumns(); j<numOfColsNew; j++){\n";
		    resultHTML += "  if(data.getValue(i,j) == null) data.setValue(i,j,0);\n";
		    resultHTML += "}\n";	
		    
		    resultHTML += "    var optionsLabels = '0:';\n";
		    resultHTML += "    var optionsScale = '';\n";
		    resultHTML += "    var optionsLineWidth = '';\n";
		    resultHTML += "    var optionsLegend = 'r|l';\n";
		    resultHTML += "     var optionsLegendSize = '000000,12';\n";

		    resultHTML += "    for(var i = 0, numOfRows = data.getNumberOfRows(); i<numOfRows; i++){\n";
		    resultHTML += "      optionsLabels += '|' + data.getValue(i,0);\n";
		    resultHTML += "    }\n";

		    resultHTML += "    data.removeColumn(0);\n";

		    resultHTML += "    for(var i = 0, numOfCols=data.getNumberOfColumns(); i<numOfCols; i++){\n";
		    resultHTML += "        optionsScale += '0,'+optionsScaleMax+(i==numOfCols-1 ? '' : ',');\n";
		    resultHTML += "        optionsLineWidth += '2' + (i==numOfCols-1 ? '' : '|');\n";
		    resultHTML += "    }\n";
 
		    resultHTML += "    optionsLegend = 't|a';\n";
		    resultHTML += "    optionsLegendSize = '000000,10';\n";
                
		    resultHTML += "   var options = {\n";
		    resultHTML += "      cht: 'r',\n";
		    resultHTML += "      chxr: '1,0,'+optionsScaleMax+','+optionsScaleMax/10,\n";
		    resultHTML += "      chds: optionsScale,\n";
		    resultHTML += "      chs: '450x320',\n"; 
		    resultHTML += "      chls: optionsLineWidth,\n";
		    resultHTML += "      chxt: 'x,y',\n";
		    resultHTML += "      chxl: optionsLabels,\n";
		    resultHTML += "      chdlp: optionsLegend,\n";
		    resultHTML += "      chdls: optionsLegendSize\n";
		    resultHTML += "    };\n";

		    resultHTML += "    var view = new google.visualization.DataView(data);\n";
		    resultHTML += "    view.setRows(view.getViewRows().concat([0]));\n";
		    resultHTML += "var chart = new google.visualization.ImageChart(document.getElementById('" + randomNodeId + "'));\n";
	        resultHTML += "chart.draw(view.toDataTable(), options);\n";
		        	
			resultHTML += "}\n</script>";
			
			return resultHTML;
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
