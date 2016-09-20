package i5.las2peer.services.mobsos.queryVisualization.encoding;

/**
 * Visualization.java <br>
 * Abstract Visualization Class. Contains a visualizationException instance.
 */
public abstract class Visualization {

	private final VisualizationType _type;
	public final VisualizationException visualizationException;

	public Visualization(VisualizationType type) {
		this._type = type;
		this.visualizationException = VisualizationException.getInstance();
	}

	public VisualizationType getType() {
		return _type;
	}

	/**
	 * Generates the Visualization (Library Code, XML, CSV, etc.).
	 * 
	 * @param methodResult
	 *            the method result
	 * @param visualizationParamters
	 *            an array containing additional information about the
	 *            visualization
	 * 
	 * @return the generated output
	 */
	public abstract String generate(MethodResult methodResult, String[] visualizationParamters);

	/**
	 * Checks, if the Visualization is suitable for the passed method result.
	 * 
	 * @param methodResult
	 *            the method result
	 * @param visualizationParamters
	 *            parameters for the visualization
	 * 
	 * @return true if visualization is suitable
	 */
	public abstract boolean check(MethodResult methodResult, String[] visualizationParamters);

}
