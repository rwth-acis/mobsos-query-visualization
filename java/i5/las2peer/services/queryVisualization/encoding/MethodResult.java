package i5.las2peer.services.queryVisualization.encoding;

import i5.las2peer.security.Context;

import java.util.Iterator;
import java.util.LinkedList;


/**
 * MethodResult.java
 * <br>
 * Helper class to store method or sql result in a general (java object) format
 * until they are transformed into a final output format, such as XML.
 * <br>
 * A Method Result Class consists of the row-length, the column names and datatypes and the rows as a Linked List of Objects.
 */
public class MethodResult {
	private int rowLength = -1;
	private String[] columnNames = null;
	private Integer[] columnDatatypes = null;
	private LinkedList<Object[]> rowList = null;
	
	public MethodResult() {
		this.rowLength = -1;
		this.columnNames = null;
		this.columnDatatypes = null;
		this.rowList = new LinkedList<Object[]>();
	}
	
	public String[] getColumnNames() {
		return this.columnNames;
	}
	
	public void setColumnNames(String[] columnNames) {
		this.columnNames = columnNames;
		this.rowLength = columnNames.length;
	}
	
	/**
	 * Sets a single column name. Useful, if the result contains
	 * one column only.
	 * 
	 * @param columnName The column name
	 */
	public void setColumnName(String columnName) {
		this.columnNames = new String[1];
		this.columnNames[0] = columnName;
		this.rowLength = 1;
	}
	
	public Integer[] getColumnDatatypes() {
		return this.columnDatatypes;
	}
	
	public void setColumnDatatypes(Integer[] columnDatatypes) {
		this.columnDatatypes = columnDatatypes;
		this.rowLength = columnDatatypes.length;
	}
	
	/**
	 * Sets a single column data type. Useful, if the result contains
	 * one column only.
	 * 
	 * @param columnDatatype The column datatype
	 */
	public void setColumnDatatype(Integer columnDatatype) {
		this.columnDatatypes = new Integer[1];
		this.columnDatatypes[0] = columnDatatype;
		this.rowLength = 1;
	}
	
	public Iterator<Object[]> getRowIterator() {
		if(this.rowList != null) {
			return this.rowList.iterator();
		}
		else {
			Context.logError(this,"MethodResult: this.rowList == null");
			return null;
		}
	}
	
	public LinkedList<Object[]> getRowList() {
		return this.rowList;
	}
	
	public void addRow(Object[] rowValueArray) {
		if(this.rowList != null) {
			if(rowValueArray.length != this.rowLength) {
				//TODO: exception
			}
			rowList.add(rowValueArray);
		}
		else {
			Context.logError(this,"MethodResult: this.rowList == null");
		}
	}
	
	
	/**
	 * A basic String Representation of the Method Result.<br>
	 * This is not a Visualization but can be used for debugging and error handling.
	 * 
	 */
	@Override
	public String toString() {
		try {
			if(this.rowLength < 0) {
				throw new Exception("Negative row length!");
			}
			if(this.columnNames == null || this.columnNames.length <0) {
				throw new Exception("Invalid column names!");
			}
			if(this.columnDatatypes == null || this.columnDatatypes.length <0) {
				throw new Exception("Invalid column datatypes!");
			}
			if(this.rowList == null) {
				throw new Exception("Invalid rowlist!");
			}
			if(this.columnNames.length != this.columnDatatypes.length) {
				throw new Exception("Column name count does not match the datatype count!");
			}
			
			String string = "Row count: " + this.rowLength;
			
			int columnCount = this.columnNames.length;
			for(int i=0; i<columnCount; i++) {
				string += "| " + this.columnNames[i];
			}
			
			for(int i=0; i<columnCount; i++) {
				string += "-";
			}
			string += "\n";
			
			columnCount = this.columnDatatypes.length;
			for(int i=0; i<columnCount; i++) {
				string += "| " + this.columnDatatypes[i];
			}
			
			for(int i=0; i<columnCount; i++) {
				string += "=";
			}
			string += "\n";
			
			Iterator<Object[]> rowIterator = this.getRowIterator();
			while(rowIterator.hasNext()) {
				Object[] currentRow = rowIterator.next();
				
				if(currentRow.length != columnCount) {
					throw new Exception("A row has an invalid number of columns!");
				}
				
				for(int i=0; i<currentRow.length; i++) {
					string += "| " + currentRow[i].toString();
				}
				string += "\n";
			}
			
			return string;
		}
		catch(Exception e) {
			return "toString failed: " + e.getMessage() + e.getStackTrace().toString();
		}
	}
}
