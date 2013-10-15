package i5.las2peer.services.queryVisualization.query;

import i5.las2peer.execution.L2pThread;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.Node;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;

/**
 * 
 * QueryManager.java
 *<br>
 * A Manager that stores and fetches queries from the network.
 * 
 */
public class QueryManager {
	
	private Envelope storedQuery = null;
	
	
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
	 * get the anonymous agent
	 * @return anonymous agent
	 */
	protected Agent getAnonymousAgent () {
		return getL2pThread().getContext().getLocalNode().getAnonymous();
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
	 * @param queryKey
	 */
	public static String getEnvelopeId (String queryKey) {
		return "query-" + queryKey;
	}	
	
	
	public QueryManager() {
	}
	
	// add a query to the p2p storage
	public boolean storeQuery(Query query) {
		Query[] queryArray = new Query[1];
		queryArray[0] = query;
		try {
			Agent anonymous = getAnonymousAgent();
			storedQuery = Envelope.createClassIdEnvelope(new Query[0], getEnvelopeId(query.getKey()), getAnonymousAgent());
			storedQuery.open(anonymous);
			storedQuery.updateContent ( queryArray );
			storedQuery.addSignature(anonymous);
			storedQuery.store();
			logMessage("stored query: " + query.getKey());	
			return true;
		} catch (Exception e) {
			logMessage("Error storing query! " + e);
			System.out.println ( "QV critical:");
			e.printStackTrace();
			return false;
		}
	}
	
	// get a query from the p2p storage
	public Query getQuery(String queryKey) {
		try {
			storedQuery = Context.getCurrent().getStoredObject(Query[].class, getEnvelopeId (queryKey));
			storedQuery.open(getAnonymousAgent());
			Query[] queryArray = storedQuery.getContent(Query[].class);
			return queryArray[0];
		} catch ( Exception e ) {
			logMessage("Failed to load query with key! " + queryKey + " Exception: " +e);
			e.printStackTrace();
			return null;
		}
	}
}
