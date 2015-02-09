package i5.las2peer.services.queryVisualization.encoding;

import i5.las2peer.security.Context;

import java.util.HashMap;

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
			Context.logMessage(this, "Not registered Modificationsince its name was already registered!");
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