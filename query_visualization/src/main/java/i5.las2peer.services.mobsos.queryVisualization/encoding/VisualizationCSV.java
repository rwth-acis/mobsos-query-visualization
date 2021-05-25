package i5.las2peer.services.mobsos.queryVisualization.encoding;

import java.util.ListIterator;

import i5.las2peer.api.Context;
import i5.las2peer.api.logging.MonitoringEvent;

/**
 * VisualizationCSV.java <br>
 * Transforms/Converts a methodResult into a CSV String as used in the US (Separators: "," and "\n") <br>
 * - Values of the same column are separated by a "," <br>
 * - Rows are separated by a "\n" <br>
 * - The first row contains the column names <br>
 * - The second row contains the column data type (as number) as defined in Types (sql)
 * 
 */
public class VisualizationCSV extends Visualization {

	public VisualizationCSV() {
		super(VisualizationType.CSV);
	}

	@Override
	public String generate(MethodResult methodResult, String[] visualizationParameters) {
		try {
			if (methodResult == null) {
				throw new Exception("Tried to transform an invalid (method) result set into CSV-Format!");
			}

			String[] columnNames = methodResult.getColumnNames();
			Integer[] columnTypes = methodResult.getColumnDatatypes();
			ListIterator<Object[]> iterator = methodResult.getRowIterator();
			int columnCount = columnTypes.length;

			StringBuilder result = new StringBuilder();

			// add the column names
			for (int i = 0; i < (columnCount - 1); i++) {
				result.append(columnNames[i]).append(",");
			}
			result.append(columnNames[columnCount - 1].toString()).append("\n");

			// add the column datatypes
			for (int i = 0; i < (columnCount - 1); i++) {
				result.append(columnTypes[i].toString()).append(",");
			}
			result.append(columnTypes[columnCount - 1].toString()).append("\n");

			while (iterator.hasNext()) {
				StringBuilder row = new StringBuilder();
				Object[] currentRow = iterator.next();
				for (int i = 0; i < (columnCount - 1); i++) {
					row.append(currentRow[i].toString()).append(",");
				}
				row.append(currentRow[columnCount - 1].toString());

				result.append(row).append("\n");
			}

			return result.toString();
		} catch (Exception e) {
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, e.getMessage().toString());
			try {
				return super.visualizationException.generate(e, "Encoding into CSV format failed.");
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
