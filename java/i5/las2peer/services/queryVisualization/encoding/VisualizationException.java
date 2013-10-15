package i5.las2peer.services.queryVisualization.encoding;

import i5.las2peer.security.Context;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.Types;


/**
 * VisualizationException.java
 * <br>
 * Static class that is called  if a problem with the visualization has occurred.
 * Logs the exception and returns a string with an optional message.
 * 
 */
public class VisualizationException {
	
	private static VisualizationException _instance = null;
	
	public static VisualizationException getInstance() {
		if(_instance == null){
			_instance = new VisualizationException();
		}
		return _instance;
	}
	
	public String generate(Exception e, String message){
		try {
			// Parameter checks/default values
			if(e == null) {
				e = new Exception("No exception available but VisualizationException called!");
			}
			if(message == null) {
				message = e.getMessage();
			}
			
			if(e.getStackTrace() != null) {
				// get the stacktrace
				Writer result = new StringWriter();
				PrintWriter printWriter = new PrintWriter(result);
				e.printStackTrace(printWriter);
				message += "\n\nDeveloper/Maintainer Information:\n\n" + result.toString();
			}
			
			if(message == null) {
				message = "No message available.";
			}
			
			// reset the method result
			MethodResult result = new MethodResult();
			// set the error data
			String[] columnNames = {"QueryVisualizationServiceException", "Type", "Code", "Message"};
			Integer[] columnTypes = {Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR};
			
			Object[] infoRow = new Object[4];
			infoRow[0] = "Exception";
			
			if(e.getClass() != null) {
				infoRow[1] = e.getClass().getName();
			}
			else {
				infoRow[1] = "Unknown";
			}
			
			if(e instanceof SQLException) {
				infoRow[2] = new Integer(((SQLException) e).getErrorCode());
			}
			else {
				infoRow[2] = Integer.MIN_VALUE;
			}		
			infoRow[3] = message;

			result.setColumnNames(columnNames);
			result.setColumnDatatypes(columnTypes);
			result.addRow(infoRow);
			
			//Returning the information of the "error"-methodResult as a string (for now..)
			//TODO
			String returnResult = "The Query has lead to an error.\nMessage: " + infoRow[3] +"\n" ;
			returnResult += "Error Type: " + infoRow[1] +"\n" ;
			if(e instanceof SQLException){
				returnResult += "SQL-ErrorCode: " + infoRow[2] +"\n" ;
			}
			return returnResult;
		}
		catch(Exception exception) {
			// now, that is really weird...
			exception.printStackTrace();
			Context.logError(this, "exceptionToMethodResult"  + exception);
			Exception ex = new Exception("Converting exception to result failed!");

			//TODO: risk of infinite loops!
			return generate(ex,null);
		}
	}
	
}
