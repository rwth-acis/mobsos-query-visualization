package i5.las2peer.services.mobsos.queryVisualization.encoding;

import java.util.HashMap;

import i5.las2peer.api.Context;
import i5.las2peer.api.logging.MonitoringEvent;

/**
 * ModificationManager.java <br>
 * Manager Class (singleton) for the modifications.
 */
public class ModificationManager {

	private static ModificationManager _instance = null;
	private HashMap<ModificationType, Modification> _registeredModifications = new HashMap<>();

	private ModificationManager() {
	}

	public static ModificationManager getInstance() {
		if (_instance == null) {
			_instance = new ModificationManager();
		}
		return _instance;
	}

	public void registerModification(Modification m) {
		ModificationType type = m.getType();
		if (!_registeredModifications.containsKey(type)) {
			_registeredModifications.put(type, m);
		} else {
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
					"Not registered Modificationsince its name was already registered!");
		}
	}

	public Modification getModification(ModificationType type) {
		if (_registeredModifications.containsKey(type)) {
			return (_registeredModifications.get(type));
		} else {
			return null;
		}
	}

}