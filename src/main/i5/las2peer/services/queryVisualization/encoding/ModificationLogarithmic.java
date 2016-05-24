package i5.las2peer.services.queryVisualization.encoding;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.HashSet;
import java.util.ListIterator;

/**
 * ModificationLogarithmic.java
 * <br>
 * The Logarithmic Modification logarithmises the method result.
 */
public class ModificationLogarithmic extends Modification {
	
	public ModificationLogarithmic() {
		super(ModificationType.LOGARITHMIC);
	}
	
	/**
	 * Applies a logarithmic function to the methodResult.
	 * 
	 * @param methodResult a methodResult instance
	 * 
	 * @return the modified method result
	 */
	public MethodResult apply(MethodResult methodResult){
		
		MethodResult result = new MethodResult();
		result.setColumnNames(methodResult.getColumnNames());
		
		int numOfCols = methodResult.getColumnDatatypes().length;
		Integer[] oldColumnsTypes = methodResult.getColumnDatatypes();
		Integer[] newColumnTypes = new Integer[numOfCols];
		newColumnTypes[0] = oldColumnsTypes[0];
		for (int i = 1; i < numOfCols; i++) {
			newColumnTypes[i] = Types.DOUBLE;
		}
		result.setColumnDatatypes(newColumnTypes);
		
		Object[] oldRow;
		Object[] newRow;
		double value;
		for( ListIterator<Object[]> iterator = methodResult.getRowIterator(); iterator.hasNext();){
			oldRow = iterator.next();
			newRow = new Object[numOfCols];
			newRow[0] = oldRow[0];
			for (int i = 1; i < numOfCols; i++) {
				value = getDoubleValue(oldColumnsTypes[i], oldRow[i]);
				newRow[i] = value != 0 ? Math.log10(value) : 0;
			}
			result.addRow(newRow);
		}	
	
		return result;	
	}
	
	private double getDoubleValue(int type, Object o){
		double result;
		switch(type) {
			case Types.BIGINT:
				result = ((Long) o).doubleValue();
				break;
			case Types.DECIMAL:
			case Types.NUMERIC:
				result = ((BigDecimal) o).doubleValue();
				break;
			case Types.DOUBLE:
				result = (Double) o;						
				break;
			case Types.REAL:
			case Types.FLOAT:
				result = (Float) o;						
				break;
			case Types.INTEGER:
				result = (Integer) o;						
				break;
			case Types.SMALLINT:
				result = (Short) o;						
				break;
		    default:
		    	result = 0.0d;
		}		
		return result;
	}
	
	/**
	 * Checks, if the method result can be logarithmised.
	 * 
	 * @return true if every column but the first one are numeric instances.
	 */
	public boolean check(MethodResult methodResult){
		Integer[] columnDatatypes = methodResult.getColumnDatatypes(); 
		int numOfCols = columnDatatypes.length;
		
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
