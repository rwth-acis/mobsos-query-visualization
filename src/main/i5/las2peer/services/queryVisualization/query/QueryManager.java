package i5.las2peer.services.queryVisualization.query;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import i5.las2peer.execution.L2pThread;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.Node;
import i5.las2peer.security.Agent;
import i5.las2peer.services.queryVisualization.QueryVisualizationService;
import i5.las2peer.services.queryVisualization.database.SQLDatabase;

/**
 * 
 * QueryManager.java
 *<br>
 * A Manager that stores and fetches queries from the network.
 * 
 */
public class QueryManager {
	
	private SQLDatabase storageDatabase = null;
	
	
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
	
	public QueryManager(QueryVisualizationService service, SQLDatabase dbm) {
		storageDatabase = dbm;
	}
	
	// add a query to the p2p storage
	public boolean storeQuery(Query query) {
		Query[] queryArray = new Query[1];
		queryArray[0] = query;
		try {
			storageDatabase.connect();
			PreparedStatement p = storageDatabase.prepareStatement(Query.getReplace());
			query.prepareStatement(p);
			p.executeUpdate();
			storageDatabase.disconnect();
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
			storageDatabase.connect();
			PreparedStatement s = storageDatabase.prepareStatement("SELECT * FROM `QUERIES` WHERE `USER` = ? AND `KEY` = ?");
			s.setLong(1, getL2pThread().getContext().getMainAgent().getId());
			s.setString(2, queryKey);
			ResultSet r = s.executeQuery();
			Query[] queryArray = Query.fromResultSet(r);
			storageDatabase.disconnect();

			return queryArray[0];
		} catch ( Exception e ) {
			logMessage("Failed to load query with key! " + queryKey + " Exception: " +e);
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Remove given database from the database
	 */
	public void removeQ(String queryKey) {
		try {
			storageDatabase.connect();
			PreparedStatement s = storageDatabase.prepareStatement("DELETE FROM `QUERIES` WHERE ((`KEY` = ? AND `USER` = ?))");
			s.setString(1, queryKey);
			s.setLong(2, getL2pThread().getContext().getMainAgent().getId());
			s.executeUpdate();
			storageDatabase.disconnect();
		} catch (Exception e) {
			logMessage("Error removing the Query! " + e);
			System.out.println ( "QV critical:");
			e.printStackTrace();
		}
	}
}
