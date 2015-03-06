package i5.las2peer.services.queryVisualization.encoding;

import i5.las2peer.security.Context;

import java.sql.Types;
import java.util.Iterator;

/**
 * VisualizationXML.java
 * <br>
 * Transforms/Converts a method result into an XML-String, where each row is stored as String-Array (using a subset of the syntax/semantic the LAS-HTTP-Connector)
 * <br>
 * Data Format:<br>
 * - XML with the same (/subset of the) syntax/semantic as used by the HTTP-LAS-Connector<br>
 * - Each row is stored as String array parameters element<br>
 * - The first row contains the column names<br>
 * - The second row denotes the data type of the column as String as used in Java, e.g. "String", "long", "Date", etc.
 * 
 */
public class VisualizationXML extends Visualization {

	public VisualizationXML() {
		super(VisualizationType.XML);
	}
	
	public String generate(MethodResult methodResult, String[] visualizationParameters){
		try {
			
			if(methodResult == null) {
				throw new Exception("Tried to transform an invalid methodResult into XML!");
			}
			
			String[] columnNames =methodResult.getColumnNames();
			Integer[] columnTypes = methodResult.getColumnDatatypes();
			Iterator<Object[]> iterator = methodResult.getRowIterator();
			int columnCount = columnTypes.length;
			
			String xmlRowStart = "\t<param type=\"Array\" class=\"String\" length=\"" + columnCount + "\">\n";
			String xmlRowEnd = "\t</param>\n";

			String xmlStart = "<?xml version=\"1.0\"?>\n<rows>\n";
			String xmlEnd = "\n</rows>";
			
			StringBuilder xmlResult = new StringBuilder();
			
			// add the column names
			xmlResult.append(xmlRowStart);
			for(int i=0; i<columnCount; i++) {
				xmlResult.append("\t\t<element><![CDATA["+columnNames[i]+"]]></element>\n");
			}
			xmlResult.append(xmlRowEnd);
			
			// add the column data types
			xmlResult.append(xmlRowStart);
			for(int i=0; i<columnCount; i++) {
				xmlResult.append("\t\t<element><![CDATA[");
				
				switch(columnTypes[i]) {
					case Types.BOOLEAN:
						xmlResult.append("Boolean");
						break;
					case Types.DATE:
					case Types.TIME:
					case Types.TIMESTAMP:
						xmlResult.append("Date"); // better idea?
						break;
					case Types.BIGINT:
					case Types.DECIMAL:
					case Types.NUMERIC:
						xmlResult.append("Long"); // better idea?
						break;
					case Types.DOUBLE:
						xmlResult.append("Double");
						break;
					case Types.REAL:
					case Types.FLOAT:
						xmlResult.append("Float");
						break;
					case Types.INTEGER:
					case Types.SMALLINT:
						xmlResult.append("Integer");
						break;
					default:
						xmlResult.append("String");
						break;
				};
				xmlResult.append("]]></element>\n");
			}
			xmlResult.append(xmlRowEnd);
			

			
			while(iterator.hasNext()) {
				Object[] currentRow = iterator.next();
				// add the row...
				xmlResult.append(xmlRowStart);
				for(int i=0; i<columnCount; i++) {
					if (currentRow[i] instanceof String) {
						String s = (String)currentRow[i];
						s = s.replace("]]>", "]]]]><![CDATA[>"); //Otherwise problems with xml transportation
						xmlResult.append("\t\t<element><![CDATA["+s+"]]></element>\n");
					} else {
						xmlResult.append("\t\t<element><![CDATA["+currentRow[i]+"]]></element>\n");
					}
				}
				xmlResult.append(xmlRowEnd);
			}
						
			return xmlStart + xmlResult.toString().trim() + xmlEnd;
		
		}
		catch (Exception e) {
			Context.logMessage(this, e.getMessage());
			try {
				return  super.visualizationException.generate(e, "Encoding into XML format failed.");
			}
			catch(Exception ex) {
				Context.logError(this, ex.getMessage());
				return "Unknown/handled error occurred!";
			}
		}
	}
	
	//Always true
	public boolean check(MethodResult methodResult, String[] visualizationParameters) {
		return true;
	}
	
}
