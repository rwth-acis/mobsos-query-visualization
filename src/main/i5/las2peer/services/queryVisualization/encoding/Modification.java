package i5.las2peer.services.queryVisualization.encoding;

import i5.las2peer.execution.L2pServiceException;


/**
 * Modification.java
 * <br>
 * Abstract Modification Class.
 */
public abstract class Modification {

	private final ModificationType _type;
	
	public Modification(ModificationType type){
		this._type = type;
	}
	
	public ModificationType getType(){
		return _type;
	}
	
	/**
	 * Applies the modification function onto the query result.
	 * 
	 * @param methodResult a methodResult instance
	 * 
	 * @return the modified method result
	 */
	public abstract MethodResult apply(MethodResult methodResult) throws L2pServiceException;
	
	/**
	 * Checks, if the Modification can be applied to the method Result.
	 * 
	 * @return true if it can be
	 */
	public abstract boolean check(MethodResult methodResult);
}
