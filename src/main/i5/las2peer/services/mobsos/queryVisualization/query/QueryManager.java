package i5.las2peer.services.mobsos.queryVisualization.query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import i5.las2peer.api.Context;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.services.mobsos.queryVisualization.QueryVisualizationService;
import i5.las2peer.services.mobsos.queryVisualization.database.DBDoesNotExistException;
import i5.las2peer.services.mobsos.queryVisualization.database.SQLDatabase;
import i5.las2peer.services.mobsos.queryVisualization.database.SQLDatabaseManager;
import i5.las2peer.services.mobsos.queryVisualization.database.SQLDatabaseSettings;
import net.minidev.json.JSONObject;

/**
 * 
 * QueryManager.java <br>
 * A Manager that stores and fetches queries from the network.
 * 
 */
public class QueryManager {

	private HashMap<String, Query> userQueryMap = new HashMap<>();
	private SQLDatabase storageDatabase = null;
	private QueryVisualizationService service = null;

	/*************** "service" helper methods *************************/

	/**
	 * write a log message
	 * 
	 * @param message Message that will be logged
	 */
	protected void logMessage(String message) {
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, this.getClass().getName() + ": " + message);
	}

	/**************************
	 * end of service helper methods
	 ************************************/

	/**
	 * Constructor
	 * 
	 * @param service instance of the qv service
	 * @param dbm Database
	 */
	public QueryManager(QueryVisualizationService service, SQLDatabase dbm) {
		storageDatabase = dbm;
		this.service = service;
		// get the user's security object which contains the database
		// information

		Query[] settings = null;

		try {
			Connection c = storageDatabase.getConnection();
			PreparedStatement p = c.prepareStatement("SELECT * FROM QUERIES WHERE USER = ?;");
			p.setString(1, Context.get().getMainAgent().getIdentifier());
			ResultSet databases = p.executeQuery();
			settings = Query.fromResultSet(databases);
			c.close();
		} catch (Exception e) {
			logMessage("Failed to get the users' SQL settings. " + e.getMessage());
		}

		for (Query setting : settings) {
			userQueryMap.put(setting.getKey(), setting);
		}
	}

	// add a query to the p2p storage
	public boolean storeQuery(Query query) {
		Query[] queryArray = new Query[1];
		queryArray[0] = query;
		try {
			String title = query.getTitle();
			for (Query q : userQueryMap.values()) {
				if (q.getTitle().equals(title)) {
					query.setKey(q.getKey());
					break;
				}
			}

			Connection c = storageDatabase.getConnection();
			PreparedStatement p = c.prepareStatement(Query.getReplace());
			query.prepareStatement(p);
			p.executeUpdate();
			userQueryMap.put(query.getKey(), query);
			logMessage("stored query: " + query.getKey());
			c.close();
			return true;
		} catch (Exception e) {
			logMessage("Error storing query! " + e);
			System.out.println("QV critical:");
			e.printStackTrace();
			return false;
		}
	}

	// get a query from the p2p storage
	public Query getQuery(String queryKey) throws Exception {
		try {
			UserAgent u = (UserAgent) Context.get().getMainAgent();
			if (u.getLoginName().equals("anonymous")) {

				Connection c = storageDatabase.getConnection();
				PreparedStatement p = c.prepareStatement("SELECT * FROM QUERIES WHERE `KEY` = ?;");
				p.setString(1, queryKey);
				ResultSet databases = p.executeQuery();
				Query[] settings = Query.fromResultSet(databases);
				c.close();
				if (settings.length > 0) {
					return settings[0];
				}
				throw new Exception("The requested Query is not known! (Requested:" + queryKey);
			}

			Query query = userQueryMap.get(queryKey);

			if (query != null) {
				// TODO: check that the database is still open/valid
			} else {
				query = userQueryMap.get(queryKey);

				if (query == null) {
					// the requested query is not known
					String keyListString = "";
					Iterator<String> iterator = getQueryKeyList().iterator();
					while (iterator.hasNext()) {
						keyListString += " " + iterator.next();
					}

					throw new Exception("The requested Query is not known! (Requested:" + queryKey + ", Available: "
							+ keyListString + ")");
				}
			}
			return query;
		} catch (Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
			throw e;
		}
	}

	public void databaseDeleted(String dbKey) {
		String db;
		try {
			db = service.databaseManagerMap.get(Context.get().getMainAgent().getIdentifier()).getDatabaseInstance(dbKey)
					.getDatabase();
		} catch (Exception e1) {
			return;
		}
		for (Query q : userQueryMap.values()) {
			if (q.getDatabaseName().equals(db)) {
				try {
					removeQ(q.getKey());
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * Remove given database from the database
	 * 
	 * @param queryKey Key of the query which will be removed
	 */
	public void removeQ(String queryKey) {
		try {

			Connection c = storageDatabase.getConnection();
			PreparedStatement s = c.prepareStatement("DELETE FROM `QUERIES` WHERE ((`KEY` = ? AND `USER` = ?))");
			s.setString(1, queryKey);
			s.setString(2, Context.get().getMainAgent().getIdentifier());
			s.executeUpdate();
			c.close();
		} catch (Exception e) {
			logMessage("Error removing the Query! " + e);
			System.out.println("QV critical:");
			e.printStackTrace();
		} finally {
			userQueryMap.remove(queryKey);
		}
	}

	// get a list of the names of all queries of the user
	public List<String> getQueryKeyList() {
		try {
			LinkedList<String> keyList = new LinkedList<>();
			Iterator<String> iterator = this.userQueryMap.keySet().iterator();
			while (iterator.hasNext()) {
				keyList.add(iterator.next());
			}

			return keyList;
		} catch (Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
		}
		return null;
	}

	// returns a list of all database settings elements
	public List<Query> getQueryList() {
		try {
			LinkedList<Query> settingsList = new LinkedList<>();
			Iterator<Query> iterator = this.userQueryMap.values().iterator();
			while (iterator.hasNext()) {
				settingsList.add(iterator.next());
			}
			settingsList.sort(new QueryComparator());
			return settingsList;
		} catch (Exception e) {
			e.printStackTrace();
			logMessage(e.getMessage());
		}
		return null;
	}

	public SQLDatabaseSettings getDBSettings(Query q) {
		String databaseName = q.getDatabaseName();
		SQLDatabaseManager dbm = service.databaseManagerMap.get(Context.get().getMainAgent().getIdentifier());
		for (SQLDatabaseSettings db : dbm.getDatabaseSettingsList()) {
			if (databaseName.equals(db.getDatabase())) {
				return db;
			}
		}
		return null;
	}

	public JSONObject toJSON(Query query) throws DBDoesNotExistException {
		JSONObject o = new JSONObject();
		o.put("key", query.getKey());
		try {
			o.put("db", getDBSettings(query).getKey());
		} catch (Exception e) {
			throw new DBDoesNotExistException("Database " + query.getDatabaseKey() + " does not exist.");
		}
		o.put("query", query.getQueryStatement());
		o.put("modtypei", query.getModificationTypeIndex());
		o.put("format", query.getVisualizationTypeIndex().toString());
		o.put("title", query.getTitle());
		o.put("width", query.getWidth());
		o.put("height", query.getHeight());
		return o;
	}

	class QueryComparator implements Comparator<Query> {
		@Override
		public int compare(Query q1, Query q2) {
			return q1.getTitle().compareTo(q2.getTitle());
		}
	}
}
