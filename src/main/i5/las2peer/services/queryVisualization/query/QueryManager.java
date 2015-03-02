package i5.las2peer.services.queryVisualization.query;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import i5.las2peer.execution.L2pThread;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.Node;
import i5.las2peer.security.Agent;
import i5.las2peer.services.queryVisualization.QueryVisualizationService;
import i5.las2peer.services.queryVisualization.database.SQLDatabase;
import i5.las2peer.services.queryVisualization.database.SQLDatabaseType;
import i5.las2peer.services.queryVisualization.encoding.VisualizationType;

/**
 * 
 * QueryManager.java
 *<br>
 * A Manager that stores and fetches queries from the network.
 * 
 */
public class QueryManager {
	
	private HashMap<String, Query> userDatabaseMap = new HashMap<String, Query>();
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

	// get a list of the names of all databases of the user
	public List<Query> getQueries() {
		ResultSet queries;
		long user = getL2pThread().getContext().getMainAgent().getId();
		try {
			storageDatabase.connect();
			PreparedStatement p = storageDatabase.prepareStatement(
					"SELECT * FROM `QUERIES` WHERE USER = ?;");
			p.setLong(1, user);
			queries = p.executeQuery();
			LinkedList<Query> settings = new LinkedList<Query>();
			try {
				while (queries.next()) {
					String title = queries.getString("VISUALIZATION_TITLE");
					String height = queries.getString("VISUALIZATION_HEIGHT");
					String width = queries.getString("VISUALIZATION_WIDTH");
					Query setting = new Query(user,  SQLDatabaseType.getSQLDatabaseType(queries.getInt("JDBCINFO")),
							queries.getString("USERNAME"), queries.getString("PASSWORD"), queries.getString("DATABASE_NAME"),
							queries.getString("HOST"), queries.getInt("PORT"), queries.getString("QUERY_STATEMENT"),
							queries.getBoolean("USE_CACHE"), queries.getInt("MODIFICATION_TYPE"),
							VisualizationType.valueOf(queries.getString("VISUALIZATION_TYPE")), new String[] {title, width, height},
							queries.getString("KEY"));
					settings.add(setting);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return settings;
		} catch ( Exception e ) {
			logMessage("Failed to get the users' SQL settings. " + e.getMessage());
		} finally {
			storageDatabase.disconnect();
		}
		return null;
	}
}
