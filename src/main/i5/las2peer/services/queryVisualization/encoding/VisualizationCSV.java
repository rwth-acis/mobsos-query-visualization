package i5.las2peer.services.queryVisualization.encoding;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;

import java.util.ListIterator;

/**
 * VisualizationCSV.java
 * <br>
 * Transforms/Converts a methodResult into a CSV String as used in the US (Separators: "," and "\n")
 * <br>
 * - Values of the same column are separated by a ","
 * <br>
 * - Rows are separated by a "\n"
 * <br>
 * - The first row contains the column names
 * <br>
 * - The second row contains the column data type (as number) as defined in Types (sql)
 * 
 */
public class VisualizationCSV extends Visualization {

	public VisualizationCSV() {
		super(VisualizationType.CSV);
	}
	
	public String generate(MethodResult methodResult, String[] visualizationParameters){
		try {
			if(methodResult == null) {
				throw new Exception("Tried to transform an invalid (method) result set into CSV-Format!");
			}
		
			String[] columnNames =methodResult.getColumnNames();
			Integer[] columnTypes = methodResult.getColumnDatatypes();
			ListIterator<Object[]> iterator = methodResult.getRowIterator();
			int columnCount = columnTypes.length;
			
			StringBuilder result = new StringBuilder();
			
			// add the column names
			for(int i=0;i<(columnCount-1);i++) {
				result.append(columnNames[i]).append(",");
			}
			result.append(columnNames[columnCount-1].toString()).append("\n");
			
			// add the column datatypes
			for(int i=0;i<(columnCount-1);i++) {
				result.append(columnTypes[i].toString()).append(",");
			}
			result.append(columnTypes[columnCount-1].toString()).append("\n");
			
			while(iterator.hasNext()) {
				StringBuilder row = new StringBuilder();
				Object[] currentRow = iterator.next();
				for(int i = 0; i < (columnCount-1); i++) {
					row.append(currentRow[i].toString()).append(",");
				}
				row.append(currentRow[columnCount-1].toString());
				
				result.append(row).append("\n");
			}
			
			return result.toString();
		}
		catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.getMessage().toString());
			try {
				return  super.visualizationException.generate(e, "Encoding into CSV format failed.");
			}
			catch(Exception ex) {
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, ex.getMessage().toString());
				return "Unknown/handled error occurred!";
			}
		}
	}
	
	//Always true
	public boolean check(MethodResult methodResult, String[] visualizationParameters) {
		return true;
	}
	
}
