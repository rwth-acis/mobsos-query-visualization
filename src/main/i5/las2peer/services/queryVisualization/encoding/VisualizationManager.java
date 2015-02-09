package i5.las2peer.services.queryVisualization.encoding;

import i5.las2peer.security.Context;

import java.util.HashMap;

/**
 * VisualizationManager.java
 * <br>
 * Manager Class (singleton) for the visualizations.
 * 
 */
public class VisualizationManager {

	private static VisualizationManager _instance = null;
	private HashMap<VisualizationType, Visualization> _registeredVisualizations = new HashMap<VisualizationType, Visualization>();
	
	private VisualizationManager() {}
	
	public static VisualizationManager getInstance() {
		if(_instance == null){
			_instance = new VisualizationManager();
		}
		return _instance;
	}
	
	public void registerVisualization(Visualization v) {
		VisualizationType type = v.getType();
		if(!_registeredVisualizations.containsKey(type)){
			_registeredVisualizations.put(type, v);	
		} else {
			Context.logMessage(this, "Visualization not registered since its name was already registered!");
		}
	}

	public Visualization getVisualization(VisualizationType type){
		if(_registeredVisualizations.containsKey(type)){
			return ((Visualization) _registeredVisualizations.get(type));	
		} else {
			return null;
		}
	}
	
}
