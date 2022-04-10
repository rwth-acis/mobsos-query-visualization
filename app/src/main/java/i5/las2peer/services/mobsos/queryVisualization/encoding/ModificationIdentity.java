package i5.las2peer.services.mobsos.queryVisualization.encoding;

/**
 * ModificationIdentitiy.java <br>
 * The Identity Modification does not do anything with the data (Identity).
 * 
 */
public class ModificationIdentity extends Modification {

	public ModificationIdentity() {
		super(ModificationType.IDENTITIY);
	}

	/**
	 * Returns the methodResult as it is.
	 * 
	 * @param methodResult
	 *            a methodResult instance
	 * 
	 * @return the modified method result
	 */
	public MethodResult apply(MethodResult methodResult) {
		return methodResult;
	}

	/**
	 * Always true.
	 * 
	 * @return true
	 */
	public boolean check(MethodResult methodResult) {
		return true;
	}

}
