package i5.las2peer.services.mobsos.queryVisualization.encoding;

import java.util.HashMap;

import i5.las2peer.api.Context;
import i5.las2peer.api.logging.MonitoringEvent;

/**
 * VisualizationManager.java <br>
 * Manager Class (singleton) for the visualizations.
 * 
 */
public class VisualizationManager {

	private static VisualizationManager _instance = null;
	private HashMap<VisualizationType, Visualization> _registeredVisualizations = new HashMap<>();

	private VisualizationManager() {
	}

	public static VisualizationManager getInstance() {
		if (_instance == null) {
			_instance = new VisualizationManager();
		}
		return _instance;
	}

	public void registerVisualization(Visualization v) {
		VisualizationType type = v.getType();
		if (!_registeredVisualizations.containsKey(type)) {
			_registeredVisualizations.put(type, v);
		} else {
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
					"Visualization not registered since its name was already registered!");
		}
	}

	public Visualization getVisualization(VisualizationType type) {
		if (_registeredVisualizations.containsKey(type)) {
			return (_registeredVisualizations.get(type));
		} else {
			return null;
		}
	}

}
