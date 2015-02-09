package i5.las2peer.services.queryVisualization.database;

import i5.las2peer.execution.L2pThread;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.Node;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;
import i5.las2peer.services.queryVisualization.QueryVisualizationService;
import i5.las2peer.services.queryVisualization.encoding.ModificationType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * SQLFilterManager.java
 *<br>
 * This class manages custom created filters of a user.
 */
public class SQLFilterManager {
	
	private HashMap<String, SQLFilterSettings> userFilterMap = new HashMap<String, SQLFilterSettings>();
	private HashMap<String, String> loadedFilterValues = new HashMap<String, String>();
		
	private Envelope storedFilters = null;
	
	/*************** "service" helper methods *************************/
	
	/**
	 * get the current l2p thread
	 * @return the L2pThread we're currently running in
	 */
	public final L2pThread getL2pThread () {
		Thread t = Thread.currentThread();
		
		if (! ( t instanceof L2pThread ))
			throw new IllegalStateException ( "Not executed in a L2pThread environment!");
		
		return (L2pThread) t;
	}
	
	/** 
	 * get the currently active agent
	 * @return active agent
	 */
	protected Agent getActiveAgent () {
		return getL2pThread().getContext().getMainAgent();
	}
	
	
	/**
	 * write a log message
	 * 
	 * @param message
	 */
	protected void logMessage ( String message ) {
		getActiveNode().observerNotice(Event.SERVICE_MESSAGE, this.getClass().getName() + ": " + message);
	}
	

	/**
	 * get the currently active l2p node (from the current thread context)
	 * 
	 * @return 	the currently active las2peer node
	 */
	protected Node getActiveNode() {
		return getL2pThread().getContext().getLocalNode();
	}
	
	/************************** end of service helper methods ************************************/
	
	
	/**
	 * get an id String for the envelope stored for an user
	 * 
	 * @param user
	 */
	public static String getEnvelopeId ( Agent user ) {
		return "userFilters-" + user.getId();
	}
	
	
	public SQLFilterManager() {
		
		// get the user's security object which contains the database information
		Agent user = getActiveAgent();
		
		SQLFilterSettings[] settings = null;

		try {
			storedFilters = Context.getCurrent().getStoredObject(SQLFilterSettings[].class, getEnvelopeId ( user) );
			storedFilters.open();
			settings = storedFilters.getContent(SQLFilterSettings[].class);
		} catch ( Exception e ) {
			try {
				storedFilters = Envelope.createClassIdEnvelope(new SQLFilterSettings[0], getEnvelopeId(user), user);
				storedFilters.open();
				storedFilters.addSignature(getActiveAgent());
				storedFilters.store();
				logMessage("Failed to get the users' SQL settings from the net -- using a fresh one! " + e.getMessage());			
			} catch ( Exception e2 ) {
				logMessage("Failed to generate a new envelope for storing user filters! " + e2 );
			}
		}
		
		if(settings == null || settings.length <= 0) {
			// there no database settings available yet...
		} else {
			for ( SQLFilterSettings setting: settings )
				userFilterMap.put ( setting.getKey(), setting);
		}
		
	}
	
	public boolean addFilter(String filterKey, String SQLQuery, String databaseKey) throws Exception {
		try {			
			//TODO: sanity checks for the parameters
			if(exitsFilter(filterKey)) {
				throw new Exception("Filter with key " + filterKey + " already exists!");
			}
			
			SQLFilterSettings filterSettings = new SQLFilterSettings(filterKey, SQLQuery, databaseKey);
			userFilterMap.put(filterSettings.getKey(), filterSettings);
			storeFilterList();
			
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
			throw e;
		}
	}
	
	public boolean deleteFilter(String filterKey) throws Exception {
		try {
			if(!exitsFilter(filterKey)) {
				throw new Exception("Filter with key " + filterKey + " does not exists!");
			}
			
			if(userFilterMap != null && userFilterMap.containsKey(filterKey)) {
				// delete from hash map
				userFilterMap.remove(filterKey);
				storeFilterList();
			}
			
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
			throw e;
		}
	}
	
	public boolean exitsFilter(String key) {
		try {
			return (userFilterMap.get(key) != null);
		}
		catch(Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
		}
		return false;
	}
	
	public int getFilterCount() {
		return this.userFilterMap.size();
	}
	
	public List<String> getFilterKeyList() {
		try {
			LinkedList<String> keyList = new LinkedList<String>();
			Iterator<String> iterator = this.userFilterMap.keySet().iterator();
			while(iterator.hasNext()) {
				keyList.add(iterator.next());
			}
			
			return keyList;
		}
		catch(Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
		}
		return null;
	}
	
	
	public String getDatabaseKey(String filterKey){
		String databaseKey = this.userFilterMap.get(filterKey).getDatabaseKey();
		return databaseKey;
	}
	
	
	public String getFilterValues(String filterKey, Integer visualizationTypeIndex, QueryVisualizationService agent) throws Exception {
		try {
			String filterValues = loadedFilterValues.get(filterKey + ":" + visualizationTypeIndex);
			
			if(filterValues == null) {
				// load them
				SQLFilterSettings filterSettings = userFilterMap.get(filterKey);
				
				if(filterSettings == null) {
					// the requested filter is not known/defined
					throw new Exception("The requested filter is not known/configured! sRequested:" + filterKey);
				}
				
				// get the filter values from the database...
				String query = filterSettings.getQuery();
				String databaseKey = filterSettings.getDatabaseKey();
				filterValues = agent.createQueryString(query, null, databaseKey, true, ModificationType.IDENTITIY.ordinal(), visualizationTypeIndex, null,false);

				// store/cache the filter values (note: the output format is added in case the values for the same filter are requested multiple times but in different output formats)
				loadedFilterValues.put(filterKey + ":" + visualizationTypeIndex, filterValues);
			}
			
			return filterValues;
		}
		catch(Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
			throw e;
		}
	}
	
	
	public void storeFilterList () {
		SQLFilterSettings[] filters = userFilterMap.values().toArray(new SQLFilterSettings[0]);
		
		try {
			storedFilters = Context.getCurrent().getStoredObject(SQLFilterSettings[].class, getEnvelopeId ( getActiveAgent()) );
			storedFilters.open();
			storedFilters.updateContent ( filters );
			storedFilters.addSignature(getActiveAgent());
			storedFilters.store();
		} catch (Exception e) {
			logMessage("Error storing the filter list! " + e);
			System.out.println ( "QV critical:");
			e.printStackTrace();
		}
	}
	
}
