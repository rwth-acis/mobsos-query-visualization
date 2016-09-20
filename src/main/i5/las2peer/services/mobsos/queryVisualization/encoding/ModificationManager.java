package i5.las2peer.services.mobsos.queryVisualization.encoding;


import java.util.HashMap;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;

/**
 * ModificationManager.java
 *<br>
 * Manager Class (singleton) for the modifications.
 */
public class ModificationManager {

	private static ModificationManager _instance = null;
	private HashMap<ModificationType, Modification> _registeredModifications = new HashMap<ModificationType, Modification>();
	
	private ModificationManager() {}
	
	public static ModificationManager getInstance() {
		if(_instance == null){
			_instance = new ModificationManager();
		}
		return _instance;
	}
	
	public void registerModification(Modification m) {
		ModificationType type = m.getType();
		if(!_registeredModifications.containsKey(type)){
			_registeredModifications.put(type, m);	
		} else {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Not registered Modificationsince its name was already registered!");
		}
	}

	public Modification getModification(ModificationType type){
		if(_registeredModifications.containsKey(type)){
			return ((Modification) _registeredModifications.get(type));	
		} else {
			return null;
		}
	}
	
}