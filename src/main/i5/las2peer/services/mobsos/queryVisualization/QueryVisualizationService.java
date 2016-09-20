package i5.las2peer.services.mobsos.queryVisualization;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import i5.las2peer.execution.L2pServiceException;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.mobsos.queryVisualization.caching.MethodResultCache;
import i5.las2peer.services.mobsos.queryVisualization.database.DBDoesNotExistException;
import i5.las2peer.services.mobsos.queryVisualization.database.DoesNotExistException;
import i5.las2peer.services.mobsos.queryVisualization.database.SQLDatabase;
import i5.las2peer.services.mobsos.queryVisualization.database.SQLDatabaseManager;
import i5.las2peer.services.mobsos.queryVisualization.database.SQLDatabaseSettings;
import i5.las2peer.services.mobsos.queryVisualization.database.SQLDatabaseType;
import i5.las2peer.services.mobsos.queryVisualization.database.SQLFilterManager;
import i5.las2peer.services.mobsos.queryVisualization.database.StringPair;
import i5.las2peer.services.mobsos.queryVisualization.encoding.MethodResult;
import i5.las2peer.services.mobsos.queryVisualization.encoding.Modification;
import i5.las2peer.services.mobsos.queryVisualization.encoding.ModificationIdentity;
import i5.las2peer.services.mobsos.queryVisualization.encoding.ModificationLogarithmic;
import i5.las2peer.services.mobsos.queryVisualization.encoding.ModificationManager;
import i5.las2peer.services.mobsos.queryVisualization.encoding.ModificationNormalization;
import i5.las2peer.services.mobsos.queryVisualization.encoding.ModificationType;
import i5.las2peer.services.mobsos.queryVisualization.encoding.Visualization;
import i5.las2peer.services.mobsos.queryVisualization.encoding.VisualizationBarChart;
import i5.las2peer.services.mobsos.queryVisualization.encoding.VisualizationCSV;
import i5.las2peer.services.mobsos.queryVisualization.encoding.VisualizationException;
import i5.las2peer.services.mobsos.queryVisualization.encoding.VisualizationGoogleTable;
import i5.las2peer.services.mobsos.queryVisualization.encoding.VisualizationHTMLTable;
import i5.las2peer.services.mobsos.queryVisualization.encoding.VisualizationJSON;
import i5.las2peer.services.mobsos.queryVisualization.encoding.VisualizationLineChart;
import i5.las2peer.services.mobsos.queryVisualization.encoding.VisualizationManager;
import i5.las2peer.services.mobsos.queryVisualization.encoding.VisualizationPieChart;
import i5.las2peer.services.mobsos.queryVisualization.encoding.VisualizationRadarChart;
import i5.las2peer.services.mobsos.queryVisualization.encoding.VisualizationTimeline;
import i5.las2peer.services.mobsos.queryVisualization.encoding.VisualizationType;
import i5.las2peer.services.mobsos.queryVisualization.encoding.VisualizationXML;
import i5.las2peer.services.mobsos.queryVisualization.query.Query;
import i5.las2peer.services.mobsos.queryVisualization.query.QueryManager;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 * LAS2peer Service
 * 
 * This is a template for a very basic LAS2peer service that uses the LAS2peer
 * Web-Connector for RESTful access to it.
 * 
 */
@Path("QVS")
@SwaggerDefinition(info = @Info(title = "Query Visualization Service", version = "1.0", description = "This service can be used to visualize queries on RDB's", termsOfService = "https://github.com/rwth-acis/LAS2peer-Query-Visualization-Service", contact = @Contact(name = "Dominik Renzel", url = "", email = "renzel@dbis.rwth-aachen.de"), license = @License(name = "MIT", url = "https://raw.githubusercontent.com/rwth-acis/LAS2peer-Query-Visualization-Service/master/LICENSE")))

public class QueryVisualizationService extends RESTService {

	/*** configuration ***/
	public static final String DEFAULT_DATABASE_KEY = "storage";
	public static final String DEFAULT_HOST = "This value should be replaced automatically by configuration file!";
	public static final int DEFAULT_PORT = 0; // This value should be replaced
												// automatically by
												// configuration file!
	public static final String DEFAULT_DATABASE = "This value should be replaced automatically by configuration file!";
	public static final String DEFAULT_USER = "This value should be replaced automatically by configuration file!";
	public static final String DEFAULT_PASSWORD = "This value should be replaced automatically by configuration file!";

	public static final String DEFAULT_RESULT_TIMEOUT = "90";
	public static final long DEFAULT_SESSION_TIMEOUT = 3600000; // 1h
	public static final long DEFAULT_TIDY_SLEEPTIME_MS = 300000; // 5 min

	protected String stDbKey = DEFAULT_DATABASE_KEY;
	protected String stDbHost = DEFAULT_HOST;
	protected int stDbPort = DEFAULT_PORT;
	protected String stDbDatabase = DEFAULT_DATABASE;
	protected String stDbUser = DEFAULT_USER;
	protected String stDbPassword = DEFAULT_PASSWORD;

	protected String exKey;
	protected String exHost;
	protected int exPort;
	protected String exDatabase;
	protected String exUser;
	protected String exPassword;
	protected String exType;

	protected String resultTimeout = DEFAULT_RESULT_TIMEOUT;

	private SQLDatabaseSettings databaseSettings = null;
	private SQLDatabase storageDatabase = null;

	public HashMap<Long, SQLDatabaseManager> databaseManagerMap = new HashMap<Long, SQLDatabaseManager>();
	private HashMap<Long, SQLFilterManager> filterManagerMap = new HashMap<Long, SQLFilterManager>();
	private HashMap<Long, QueryManager> queryManagerMap = new HashMap<Long, QueryManager>();

	private MethodResultCache resultCache = null;
	private VisualizationManager visualizationManager = null;
	private VisualizationException visualizationException = null;
	private ModificationManager modificationManager = null;

	private static String stringfromJSON(JSONObject obj, String key) throws SQLException {
		String s = (String) obj.get(key);
		if (s == null) {
			throw new SQLException("Key " + key + " is missing in JSON");
		}
		return s;
	}

	private static String[] stringArrayfromJSON(JSONObject obj, String key) throws SQLException {
		JSONArray ja = (JSONArray) obj.get(key);
		if (ja == null) {
			throw new SQLException("Key " + key + " is missing in JSON");
		}
		String[] s = ja.toArray(new String[0]);
		return s;
	}

	private static int intfromJSON(JSONObject obj, String key) {
		return (int) obj.get(key);
	}

	private static boolean boolfromJSON(JSONObject obj, String key) {
		try {
			return (boolean) obj.get(key);
		} catch (Exception e) {
			String b = (String) obj.get(key);
			if (b.equals("1")) {
				return true;
			}
			return (boolean) Boolean.parseBoolean(b);
		}
	}

	private static HttpResponse setContentType(HttpResponse res, Integer vtypei) {
		switch (vtypei) {
		case 0:
			res.setHeader("Content-Type", MediaType.TEXT_CSV);
			break;
		case 1:
			res.setHeader("Content-Type", MediaType.APPLICATION_JSON);
			break;
		case 2:
			res.setHeader("Content-Type", MediaType.TEXT_HTML);
			break;
		case 3:
			res.setHeader("Content-Type", MediaType.TEXT_XML);
			break;
		default:
			res.setHeader("Content-Type", "text/plain");
		}
		return res;
	}

	public QueryVisualizationService() {
		// read and set properties values
		// IF THE SERVICE CLASS NAME IS CHANGED, THE PROPERTIES FILE NAME NEED
		// TO BE CHANGED TOO!
		setFieldValues();
	}

	public void initializeDBConnection() {
		long user = getContext().getMainAgent().getId();
		if (databaseManagerMap.get(user) != null) {
			return;
		}
		try {
			if (stDbHost == null || stDbPort < 1 || stDbDatabase == null || stDbUser == null || stDbPassword == null) {
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Provided invalid parameters (default database) for the service! Please check you service config file!");
				throw new Exception("No Database Connection values from config file available!");
			}
			if (resultTimeout == null) {
				resultTimeout = "90";
			}

			databaseSettings = new SQLDatabaseSettings(stDbKey, SQLDatabaseType.MYSQL, stDbUser, stDbPassword, stDbDatabase, stDbHost, stDbPort);

			// setup the database manager
			storageDatabase = new SQLDatabase(databaseSettings);

			SQLDatabaseManager databaseManager = new SQLDatabaseManager(this, storageDatabase);
			databaseManagerMap.put(user, databaseManager);
			QueryManager queryManager = new QueryManager(this, storageDatabase);
			queryManagerMap.put(user, queryManager);
			SQLFilterManager filterManager = new SQLFilterManager(storageDatabase);
			filterManagerMap.put(user, filterManager);

			visualizationManager = VisualizationManager.getInstance();
			visualizationException = VisualizationException.getInstance();
			modificationManager = ModificationManager.getInstance();

			// add visualizations and modifications
			visualizationManager.registerVisualization(new VisualizationCSV());
			visualizationManager.registerVisualization(new VisualizationJSON());
			visualizationManager.registerVisualization(new VisualizationHTMLTable());
			visualizationManager.registerVisualization(new VisualizationXML());
			visualizationManager.registerVisualization(new VisualizationGoogleTable());
			visualizationManager.registerVisualization(new VisualizationPieChart());
			visualizationManager.registerVisualization(new VisualizationBarChart());
			visualizationManager.registerVisualization(new VisualizationLineChart());
			visualizationManager.registerVisualization(new VisualizationTimeline());
			visualizationManager.registerVisualization(new VisualizationRadarChart());

			modificationManager.registerModification(new ModificationIdentity());
			modificationManager.registerModification(new ModificationLogarithmic());
			modificationManager.registerModification(new ModificationNormalization());

			// get the result cache
			this.resultCache = MethodResultCache.getInstance(Integer.parseInt(resultTimeout));

		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
		}
	}

	/**
	 * Adds a sql database to the users available/usable databases (via the
	 * sqldatabase manager).
	 * 
	 * @param databaseKey
	 *            key which is later used to identify the database
	 * @param content
	 *            Credentials for the database
	 * 
	 * @return success or error message, if possible in the requested
	 *         encoding/format
	 */
	@PUT
	@Path("database/{key}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = 201, message = "Database added successfully."), @ApiResponse(code = 400, message = "Database data invalid.") })
	public HttpResponse addDatabase(@PathParam("key") String databaseKey, @ContentParam String content) {
		JSONObject o;
		try {
			o = (JSONObject) JSONValue.parseWithException(content);
			String dbcode = stringfromJSON(o, "db_code");
			String username = stringfromJSON(o, "username");
			String password = stringfromJSON(o, "password");
			String database = stringfromJSON(o, "database");
			String dbhost = stringfromJSON(o, "dbhost");
			Integer port = intfromJSON(o, "port");
			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_1, getContext().getMainAgent(), "" + dbcode);
			return addDatabase(databaseKey, SQLDatabaseType.valueOf(dbcode.toUpperCase()), username, password, database, dbhost, port, VisualizationType.JSON);
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse(visualizationException.generate(e, "Received invalid JSON"));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Adds a sql database to the users available/usable databases (via the
	 * sqldatabase manager).
	 * 
	 * @param databaseKey
	 *            key which is later used to identify the database
	 * @param databaseTypeCode
	 *            type of the database (DB2, MySql, ...)
	 * @param username
	 *            the username for the database
	 * @param password
	 *            the password for the database
	 * @param database
	 *            the name of the database
	 * @param host
	 *            the database address
	 * @param port
	 *            the database port
	 * @param visualizationTypeIndex
	 *            encoding of the returned message
	 * 
	 * @return success or error message, if possible in the requested
	 *         encoding/format
	 */
	public HttpResponse addDatabase(String databaseKey, SQLDatabaseType databaseTypeCode, String username, String password, String database, String host, Integer port, VisualizationType visualizationTypeIndex) {
		try {
			if (databaseKey == null || databaseKey.length() < 2) {
				throw new Exception("Databasekey is too short (Use at least 2 characters).");
			}

			initializeDBConnection();

			long user = getContext().getMainAgent().getId();
			SQLDatabaseManager databaseManager = databaseManagerMap.get(user);

			SQLDatabaseType sqlDatabaseType = databaseTypeCode;
			if (!databaseManager.addDatabase(databaseKey, sqlDatabaseType, username, password, database, host, port)) {
				throw new Exception("Failed to add a database for the user!");
			}

			// verify that it works (that one can get an instance, probably its
			// going to be used later anyways)...
			try {
				if (databaseManager.getDatabaseInstance(databaseKey) == null) {
					throw new Exception("Failed to get a database instance for " + databaseKey);
				}
			} catch (Exception e) {
				databaseManager.removeDatabase(databaseKey);
				throw e;
			}

			MethodResult result = new MethodResult();
			result.setColumnName("AddedDatabase");
			result.setColumnDatatype(Types.VARCHAR);
			Object[] defaultDatabase = { databaseKey };
			result.addRow(defaultDatabase);

			HttpResponse res = new HttpResponse(visualizationManager.getVisualization(visualizationTypeIndex).generate(result, null));
			res.setStatus(201);
			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_2, getContext().getMainAgent(), "" + databaseKey);
			return res;
		} catch (Exception e) {
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Removes a database from the user's list of configured databases (so that
	 * the user can not access the database anymore). Other user's database
	 * settings are not changed.
	 * 
	 * @param databaseKey
	 *            the key of the database
	 * @return success or error message, if possible in the requested
	 *         encoding/format
	 */
	@DELETE
	@Path("database/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Database removed successfully."), @ApiResponse(code = 400, message = "Database removal failed.") })
	public HttpResponse removeDatabase(@PathParam("key") String databaseKey) {
		try {
			if (databaseKey.equalsIgnoreCase("MonitoringDefault")) {
				databaseKey = stDbKey;
			}

			initializeDBConnection();

			long user = getContext().getMainAgent().getId();
			SQLDatabaseManager databaseManager = databaseManagerMap.get(user);
			QueryManager queryManager = queryManagerMap.get(user);
			SQLFilterManager filterManager = filterManagerMap.get(user);

			try {
				queryManager.databaseDeleted(databaseKey);
				filterManager.databaseDeleted(databaseKey);
			} catch (Exception e) {
			}

			if (!databaseManager.removeDatabase(databaseKey)) {
				HttpResponse res = new HttpResponse("Database " + databaseKey + " does not exist!");
				res.setStatus(404);
				return res;
			}

			MethodResult result = new MethodResult();
			result.setColumnName("RemovedDatabase");
			result.setColumnDatatype(Types.VARCHAR);
			Object[] defaultDatabase = { databaseKey };
			result.addRow(defaultDatabase);

			HttpResponse res = new HttpResponse(visualizationManager.getVisualization(VisualizationType.JSON).generate(result, null));
			res.setStatus(200);
			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_3, getContext().getMainAgent(), "" + databaseKey);
			return res;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Returns a list/table of the keys of all available/configured databases of
	 * the user.
	 * 
	 * @param visualizationTypeIndex
	 *            encoding of the returned message
	 * @return success or error message, if possible in the requested
	 *         encoding/format
	 */
	@GET
	@Path("database")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Got Database keys."), @ApiResponse(code = 400, message = "Retrieving keys failed.") })
	public HttpResponse getDatabaseKeys(@QueryParam("format") @DefaultValue("JSON") String visualizationTypeIndex) {
		try {
			initializeDBConnection();
			long user = getContext().getMainAgent().getId();
			SQLDatabaseManager databaseManager = databaseManagerMap.get(user);
			List<String> keyList = databaseManager.getDatabaseKeyList();

			if (keyList == null) {
				throw new Exception("Failed to get the key list for the users' databases!");
			}

			MethodResult result = new MethodResult();
			result.setColumnName("DatabaseKeys");
			result.setColumnDatatype(Types.VARCHAR);

			Iterator<String> iterator = keyList.iterator();

			while (iterator.hasNext()) {
				Object[] currentDatabaseKey = { iterator.next() };

				if (((String) currentDatabaseKey[0]).equalsIgnoreCase("MonitoringDefault")) {
					currentDatabaseKey[0] = stDbKey;
				}

				result.addRow(currentDatabaseKey);
			}

			VisualizationType t = VisualizationType.valueOf(visualizationTypeIndex.toUpperCase());
			Visualization vis = visualizationManager.getVisualization(t);
			String visString = vis.generate(result, null);
			HttpResponse res = new HttpResponse(visString);
			res.setStatus(200);
			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_4, getContext().getMainAgent(), "Get Database Keys");
			return res;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Returns a list/table of all filters the user has configured.
	 * 
	 * @param visualizationTypeIndex
	 *            encoding of the returned message
	 * @return success or error message, if possible in the requested
	 *         encoding/format
	 */
	@GET
	@Path("filter")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Got filter keys."), @ApiResponse(code = 400, message = "Retrieving filter keys failed.") })
	public HttpResponse getFilterKeys(@QueryParam("format") @DefaultValue("JSON") String visualizationTypeIndex) {
		try {
			initializeDBConnection();

			long user = getContext().getMainAgent().getId();
			SQLFilterManager filterManager = filterManagerMap.get(user);
			if (filterManager == null) {
				// initialize filter manager
				filterManager = new SQLFilterManager(storageDatabase);
				filterManagerMap.put(user, filterManager);
			}

			List<StringPair> keyList = filterManager.getFilterKeyList();

			if (keyList == null) {
				throw new Exception("Failed to get the key list for the users' filters!");
			}

			VisualizationType vtypei = VisualizationType.valueOf(visualizationTypeIndex.toUpperCase());

			MethodResult result = new MethodResult();
			Integer[] datatypes = { Types.VARCHAR, Types.VARCHAR };
			result.setColumnDatatypes(datatypes);
			String[] names = { "FilterKeys", "DatabaseKeys" };
			result.setColumnNames(names);

			Iterator<StringPair> iterator = keyList.iterator();
			while (iterator.hasNext()) {
				StringPair p = iterator.next();
				String databaseKey = p.getKey1();
				String filterKey = p.getKey2();
				Object[] currentRow = { filterKey, databaseKey };
				result.addRow(currentRow);
			}

			HttpResponse res = new HttpResponse(visualizationManager.getVisualization(vtypei).generate(result, null));
			res.setStatus(200);
			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_5, getContext().getMainAgent(), "Get Filter.");
			return res;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Retrieves the values for a specific filter of the current user.
	 * 
	 * @param filterKey
	 *            Key for the filter
	 * @param visualizationTypeIndex
	 *            encoding of the returned message
	 * @param dbKey
	 *            Key for database
	 * @return success or error message, if possible in the requested
	 *         encoding/format
	 */
	@GET
	@Path("filter/{database}/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Got filter values."), @ApiResponse(code = 400, message = "Retrieving filter keys failed."), @ApiResponse(code = 204, message = "Filter does not exist.") })
	public HttpResponse getFilterValuesForCurrentUser(@PathParam("database") String dbKey, @PathParam("key") String filterKey, @QueryParam("format") @DefaultValue("JSON") String visualizationTypeIndex) {
		return getFilterValuesOfUser(dbKey, filterKey, getContext().getMainAgent().getId(), visualizationTypeIndex);
	}

	/**
	 * Retrieves the filter keys for a specific query by ID.
	 * 
	 * @param queryKey
	 *            Key of the query
	 * @return success or error message, if possible in the requested
	 *         encoding/format
	 */
	@GET
	@Path("query/{query}/filter")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Got filters."), @ApiResponse(code = 400, message = "Retrieving filter keys failed."), @ApiResponse(code = 204, message = "Filter does not exist.") })
	public HttpResponse getFilterValuesForQuery(@PathParam("key") String queryKey) {
		try {
			initializeDBConnection();
			QueryManager queryManager = queryManagerMap.get(getContext().getMainAgent().getId());
			Query q = queryManager.getQuery(queryKey);
			String[] params = q.getQueryParameters();
			String statement = q.getQueryStatement();

			ArrayList<String> filterNames = new ArrayList<String>();

			// go through the query, replace placeholders by the values from the
			// query parameters
			int parameterCount = params == null ? 0 : params.length;
			Pattern placeholderPattern = Pattern.compile("\\$.*?\\$");
			Matcher m = placeholderPattern.matcher(statement);
			for (int i = 0; i < parameterCount; i++) {
				m.find();
				String param = m.group();
				filterNames.add(param.substring(1, param.length() - 1));
			}
			String dbKey = q.getDatabaseKey();
			long user = q.getUser();

			SQLDatabaseManager dbManager = databaseManagerMap.get(user);
			if (dbManager == null) {
				dbManager = new SQLDatabaseManager(this, storageDatabase);
			}
			SQLDatabaseSettings db = dbManager.getDatabaseSettings(dbKey);

			ArrayList<StringPair> filters = new ArrayList<StringPair>();
			for (String filter : filterNames) {
				filters.add(new StringPair(db.getKey(), filter));
			}

			MethodResult result = new MethodResult();
			Integer[] datatypes = { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR };
			result.setColumnDatatypes(datatypes);
			String[] names = { "FilterKeys", "DatabaseKeys", "user" };
			result.setColumnNames(names);

			Iterator<StringPair> iterator = filters.iterator();
			while (iterator.hasNext()) {
				StringPair p = iterator.next();
				String databaseKey = p.getKey1();
				String filterKey = p.getKey2();
				Object[] currentRow = { filterKey, databaseKey, "" + user };
				result.addRow(currentRow);
			}

			HttpResponse res = new HttpResponse(visualizationManager.getVisualization(VisualizationType.JSON).generate(result, null));
			res.setStatus(200);
			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_6, getContext().getMainAgent(), "Get Filter for " + queryKey);
			return res;
		} catch (DoesNotExistException e) {
			HttpResponse res = new HttpResponse("Query " + queryKey + " does not exist.");
			res.setStatus(404);
			return res;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Retrieves the values for a specific filter of a chosen user.
	 * 
	 * @param dbKey
	 *            Key for database
	 * @param filterKey
	 *            Key for the filter
	 * @param visualizationTypeIndex
	 *            encoding of the returned message
	 * @param user
	 *            AgentID of the user
	 * @return success or error message, if possible in the requested
	 *         encoding/format
	 */
	@GET
	@Path("filter/{database}/{key}/{user}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Got filter values."), @ApiResponse(code = 400, message = "Retrieving filter keys failed."), @ApiResponse(code = 204, message = "Filter does not exist.") })
	public HttpResponse getFilterValuesOfUser(@PathParam("database") String dbKey, @PathParam("key") String filterKey, @PathParam("user") long user, @QueryParam("format") @DefaultValue("JSON") String visualizationTypeIndex) {
		try {
			VisualizationType vtypei = VisualizationType.valueOf(visualizationTypeIndex.toUpperCase());
			initializeDBConnection();
			SQLFilterManager filterManager = filterManagerMap.get(user);
			if (filterManager == null) {
				if (((UserAgent) getContext().getMainAgent()).getUserData() == null) {
					filterManager = new SQLFilterManager(storageDatabase, user);
					// throw new DoesNotExistException("Anonymous user requested
					// non-existant filter");
				} else {
					// initialize filter manager
					filterManager = new SQLFilterManager(storageDatabase);
					filterManagerMap.put(user, filterManager);
				}
			}

			HttpResponse res = new HttpResponse(filterManager.getFilterValues(dbKey, filterKey, vtypei, this));
			res.setStatus(200);
			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_7, getContext().getMainAgent(), "Get values for Filter " + filterKey + ", user " + user);
			return res;
		} catch (DoesNotExistException e) {
			HttpResponse res = new HttpResponse("Filter " + filterKey + " does not exist.");
			res.setStatus(404);
			return res;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Adds a filter to the user's settings/profile.
	 * 
	 * @param dbKey
	 *            key of the database for which the filter has been configured
	 * @param filterName
	 *            the Key that should be used for this filter
	 * @param query
	 *            encoding of the returned message
	 * 
	 * @return success or error message, if possible in the requested
	 *         encoding/format
	 */
	@PUT
	@Path("filter/{database}/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = 201, message = "Added filter."), @ApiResponse(code = 400, message = "Adding filter failed.") })
	public HttpResponse addFilter(@PathParam("database") String dbKey, @PathParam("key") String filterName, @ContentParam String query) {
		JSONObject o;
		try {
			VisualizationType vtypei = VisualizationType.JSON;
			o = (JSONObject) JSONValue.parseWithException(query);
			return addFilter(dbKey, filterName, stringfromJSON(o, "query"), vtypei);
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse(visualizationException.generate(e, "Received invalid JSON"));
			res.setStatus(400);
			return res;
		}
	}

	public HttpResponse addFilter(String databaseKey, String filterKey, String sqlQuery, VisualizationType visualizationTypeIndex) {
		try {
			initializeDBConnection();
			// TODO: parameter sanity checks
			long user = getContext().getMainAgent().getId();
			SQLFilterManager filterManager = filterManagerMap.get(user);

			if (filterManager == null) {
				// initialize filter manager
				filterManager = new SQLFilterManager(storageDatabase);
				filterManagerMap.put(user, filterManager);
			}

			if (!filterManager.addFilter(databaseKey, filterKey, sqlQuery)) {
				throw new Exception("Failed to add a database for the user!");
			}

			// verify that it works (that one can get an instance, probably its
			// going to be used later anyways)...
			try {
				if (filterManager.getFilterValues(databaseKey, filterKey, visualizationTypeIndex, this) == null) {
					throw new Exception("Failed to retrieve the filter values!");
				}
			} catch (Exception e) {
				filterManager.deleteFilter(databaseKey, filterKey);
				throw e;
			}

			MethodResult result = new MethodResult();
			result.setColumnName("AddedFilter");
			result.setColumnDatatype(Types.VARCHAR);
			Object[] addedFilter = { filterKey };
			result.addRow(addedFilter);

			HttpResponse res = new HttpResponse(visualizationManager.getVisualization(visualizationTypeIndex).generate(result, null));
			res.setStatus(201);

			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_8, getContext().getMainAgent(), "" + user);
			return res;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Deletes a filter from the user's settings/profile.
	 * 
	 * @param dbKey
	 *            the key of the database
	 * @param filterKey
	 *            the key of the filter
	 * @return success or error message, if possible in the requested
	 *         encoding/format
	 */
	@DELETE
	@Path("filter/{database}/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Deleted filter."), @ApiResponse(code = 400, message = "Deleting filter failed.") })
	public HttpResponse deleteFilter(@PathParam("database") String dbKey, @PathParam("key") String filterKey) {
		try {
			initializeDBConnection();
			long user = getContext().getMainAgent().getId();
			SQLFilterManager filterManager = filterManagerMap.get(user);
			if (filterManager == null) {
				// initialize filter manager
				filterManager = new SQLFilterManager(storageDatabase);
				filterManagerMap.put(user, filterManager);
			}

			if (!filterManager.deleteFilter(dbKey, filterKey)) {
				HttpResponse res = new HttpResponse("Filter " + filterKey + " does not exist!");
				res.setStatus(404);
				return res;
			}

			MethodResult result = new MethodResult();
			result.setColumnName("DeletedFilter");
			result.setColumnDatatype(Types.VARCHAR);
			Object[] deletedDatabase = { filterKey };
			result.addRow(deletedDatabase);

			HttpResponse res = new HttpResponse(visualizationManager.getVisualization(VisualizationType.JSON).generate(result, null));
			res.setStatus(200);

			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_9, getContext().getMainAgent(), "" + user);
			return res;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	@POST
	@Path("query/visualize")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Created query."), @ApiResponse(code = 400, message = "Creating Query failed.") })
	public HttpResponse visualizeQuery(@QueryParam("format") @DefaultValue("JSON") String vtypei, @ContentParam String content) {
		JSONObject o;
		try {
			VisualizationType v = VisualizationType.valueOf(vtypei.toUpperCase());
			o = (JSONObject) JSONValue.parseWithException(content);
			String query = stringfromJSON(o, "query");
			String dbKey = stringfromJSON(o, "dbkey");
			String[] queryParameters = stringArrayfromJSON(o, "queryparams");
			boolean useCache = boolfromJSON(o, "cache");
			Integer modificationTypeIndex = intfromJSON(o, "modtypei");
			String title = stringfromJSON(o, "title");
			String width = stringfromJSON(o, "width");
			String height = stringfromJSON(o, "height");
			HttpResponse res = createQuery(query, queryParameters, dbKey, useCache, modificationTypeIndex, v, title, width, height, false);
			setContentType(res, v.ordinal());

			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_10, getContext().getMainAgent(), "" + vtypei);
			return res;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse(visualizationException.generate(e, "Received invalid JSON"));
			res.setStatus(400);
			return res;
		}
	}

	@POST
	@Path("query")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = 201, message = "Created query."), @ApiResponse(code = 400, message = "Creating Query failed.") })
	public HttpResponse createQuery(@QueryParam("format") @DefaultValue("JSON") String vtypei, @ContentParam String content) {
		JSONObject o;
		try {
			VisualizationType v = VisualizationType.valueOf(vtypei.toUpperCase());
			o = (JSONObject) JSONValue.parseWithException(content);
			String query = stringfromJSON(o, "query");
			String dbKey = stringfromJSON(o, "dbkey");
			String[] queryParameters = stringArrayfromJSON(o, "queryparams");
			boolean useCache = boolfromJSON(o, "cache");
			Integer modificationTypeIndex = intfromJSON(o, "modtypei");
			String title = stringfromJSON(o, "title");
			String width = stringfromJSON(o, "width");
			String height = stringfromJSON(o, "height");
			HttpResponse res = createQuery(query, queryParameters, dbKey, useCache, modificationTypeIndex, v, title, width, height, true);
			setContentType(res, v.ordinal());

			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_11, getContext().getMainAgent(), "" + query);
			return res;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse(visualizationException.generate(e, "Received invalid JSON"));
			res.setStatus(400);
			return res;
		}
	}

	public HttpResponse createQuery(String query, String[] queryParameters, String databaseKey, boolean useCache, int modificationTypeIndex, VisualizationType visualizationTypeIndex, String title, String width, String height, boolean save) {
		String queryString = createQueryString(query, queryParameters, databaseKey, useCache, modificationTypeIndex, visualizationTypeIndex, new String[] { title, height, width }, save);
		HttpResponse res = new HttpResponse(queryString);
		if (queryString.startsWith("The Query has lead to an error.")) {
			res.setStatus(400);
		} else {
			res.setStatus(201);
		}
		return res;
	}

	/**
	 * Generates a query on the specified database. The query may contain
	 * placeholders that have to be replaced by the parameters. <br>
	 * This is the main services entry point that should be used to generate and
	 * save queries. A placeholder is set by $placeholder$ and will be replaced
	 * by the corresponding element in the query parameters. The query parameter
	 * array has to contain the elements in the same order as they appear in the
	 * query.
	 * 
	 * @param query
	 *            a String containing the query
	 * @param queryParameters
	 *            the query parameters as an array of Strings that contain the
	 *            content of the placeholders
	 * @param databaseKey
	 *            a String containing the database key
	 * @param useCache
	 *            if true, a cached result is returned (if available) instead of
	 *            performing the query again (does only affect stored queries!)
	 * @param modificationTypeIndex
	 *            the desired modification function
	 * @param visualizationTypeIndex
	 *            the desired visualization
	 * @param visualizationParameters
	 *            an array of additional parameters for the visualization,
	 *            including title, height and weight
	 * @param save
	 *            if set, the query will be saved and the return statement will
	 *            be the query id.
	 * 
	 * @return Result of the query in the requested output format or the id of
	 *         the saved query
	 */
	public String createQueryString(String query, String[] queryParameters, String databaseKey, boolean useCache, int modificationTypeIndex, VisualizationType visualizationTypeIndex, String[] visualizationParameters, boolean save) {
		initializeDBConnection();

		MethodResult methodResult = null;
		String cacheKey = ""; // If empty, the query is not cached (used in
								// "Non-Saving"-case, as creation always is)

		try {
			// Check for visualization parameters
			if (visualizationTypeIndex.ordinal() > 3 && visualizationTypeIndex.ordinal() < 10) {
				if (visualizationParameters == null || visualizationParameters.length != 3) {
					return visualizationException.generate(new Exception(), "Missing visualization-parameters!");
				}
			}

			if (save) {
				return saveQuery(query, queryParameters, databaseKey, useCache, modificationTypeIndex, visualizationTypeIndex, visualizationParameters);
			}
			methodResult = executeSQLQuery(query, queryParameters, databaseKey, cacheKey);
			Modification modification = modificationManager.getModification(ModificationType.fromInt(modificationTypeIndex));
			Visualization visualization = visualizationManager.getVisualization(visualizationTypeIndex);

			if (modification.check(methodResult))
				methodResult = modification.apply(methodResult);
			else
				return visualizationException.generate(new Exception(), "Can not modify result with " + modification.getType().name() + ".");

			if (visualization.check(methodResult, visualizationParameters))
				return visualization.generate(methodResult, visualizationParameters);
			else
				return visualizationException.generate(new Exception(), "Can not convert result into " + visualization.getType().name() + "-format.");
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			return visualizationException.generate(e, "An Error occured while trying to execute the query!");
		}
	}

	/**
	 * Returns a list/table of the keys of all available/configured databases of
	 * the user.
	 * 
	 * @param visualizationTypeIndex
	 *            encoding of the returned message
	 * @return success or error message, if possible in the requested
	 *         encoding/format
	 */
	@GET
	@Path("query")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Got Database queries."), @ApiResponse(code = 400, message = "Retrieving queries failed.") })
	public HttpResponse getQueryKeys(@QueryParam("format") @DefaultValue("JSON") String visualizationTypeIndex) {
		try {
			initializeDBConnection();
			long user = getContext().getMainAgent().getId();
			QueryManager queryManager = queryManagerMap.get(user);
			List<Query> keyList = queryManager.getQueryList();

			if (keyList == null) {
				throw new Exception("Failed to get the key list for the users' queries!");
			}

			MethodResult result = new MethodResult();
			Integer[] datatypes = { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR };
			result.setColumnDatatypes(datatypes);
			String[] names = { "QueryKeys", "DatabaseKeys", "DatabaseNames", "Title" };
			result.setColumnNames(names);
			Iterator<Query> iterator = keyList.iterator();

			while (iterator.hasNext()) {
				Query q = iterator.next();
				String queryKey = q.getKey();
				String databaseKey = q.getDatabaseKey();
				String databaseName = q.getDatabaseName();
				String title = q.getTitle();
				Object[] currentRow = { queryKey, databaseKey, databaseName, title };

				result.addRow(currentRow);
			}

			HttpResponse res = new HttpResponse(visualizationManager.getVisualization(VisualizationType.valueOf(visualizationTypeIndex.toUpperCase())).generate(result, null));
			res.setStatus(200);

			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_12, getContext().getMainAgent(), "" + user);
			return res;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Deletes a filter from the user's settings/profile.
	 * 
	 * @param queryKey
	 *            the key of the filter
	 * @return success or error message, if possible in the requested
	 *         encoding/format
	 */
	@DELETE
	@Path("query/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Deleted query."), @ApiResponse(code = 400, message = "Deleting query failed.") })
	public HttpResponse deleteQuery(@PathParam("key") String queryKey) {
		try {
			initializeDBConnection();
			long user = getContext().getMainAgent().getId();
			QueryManager queryManager = queryManagerMap.get(user);
			if (queryManager == null) {
				throw new Exception("Query Manager is null");
			}

			queryManager.removeQ(queryKey);

			MethodResult result = new MethodResult();
			result.setColumnName("DeletedQuery");
			result.setColumnDatatype(Types.VARCHAR);
			Object[] deletedQuery = { queryKey };
			result.addRow(deletedQuery);

			HttpResponse res = new HttpResponse(visualizationManager.getVisualization(VisualizationType.JSON).generate(result, null));
			res.setStatus(200);

			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_13, getContext().getMainAgent(), "" + user);
			return res;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Executes a stored query on the specified database. <br>
	 * This is the main services entry point that should be used to visualize
	 * saved queries.
	 * 
	 * @param key
	 *            a String that contains the id of the query
	 * @param format
	 *            currently not used, default JSON
	 * 
	 * @return Result of the query in the given output format
	 */
	@GET
	@Path("query/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Visualization created."), @ApiResponse(code = 400, message = "Creating visualization failed."), @ApiResponse(code = 404, message = "Didn't find requested query.") })
	public HttpResponse getQueryValues(@PathParam("key") String key, @QueryParam("format") @DefaultValue("JSON") String format) {
		initializeDBConnection();

		try {
			// VisualizationType vtypei =
			// VisualizationType.valueOf(format.toUpperCase());
			initializeDBConnection();
			long user = getContext().getMainAgent().getId();
			QueryManager queryManager = queryManagerMap.get(user);
			if (queryManager == null) {
				// initialize query manager
				queryManager = new QueryManager(this, storageDatabase);
			}
			Query q = queryManager.getQuery(key);
			if (q == null) {
				throw new DoesNotExistException("Query does not exist.");
			}
			JSONObject o = queryManager.toJSON(q);
			HttpResponse res = new HttpResponse(o.toJSONString());

			res.setStatus(200);

			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_14, getContext().getMainAgent(), "" + user);
			return res;
		} catch (DoesNotExistException e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse("Query " + key + " does not exist.");
			res.setStatus(400);
			return res;
		} catch (DBDoesNotExistException e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			long user = getContext().getMainAgent().getId();
			QueryManager queryManager = queryManagerMap.get(user);
			queryManager.removeQ(key);
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Executes a stored query on the specified database. <br>
	 * This is the main services entry point that should be used to visualize
	 * saved queries.
	 * 
	 * @param key
	 *            a String that contains the id of the query
	 * @param format
	 *            currently not used
	 * @param content
	 *            containing the query parameters
	 * 
	 * @return Result of the query in the given output format
	 */
	@POST
	@Path("query/{key}/visualize")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Visualization generated."), @ApiResponse(code = 400, message = "Creating visualization failed."), @ApiResponse(code = 404, message = "Didn't find requested query.") })
	public HttpResponse visualizeQueryByKeyWithValues(@PathParam("key") String key, @QueryParam("format") @DefaultValue("") String format, @ContentParam String content) {
		return visualizeQueryByKey(key, format, content);
	}

	/**
	 * Executes a stored query on the specified database. <br>
	 * This is the main services entry point that should be used to visualize
	 * saved queries.
	 * 
	 * @param key
	 *            a String that contains the id of the query
	 * @param format
	 *            currently not used
	 * @param content
	 *            containing the query parameters
	 * @return Result of the query in the given output format
	 */
	@GET
	@Path("query/{key}/visualize")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Visualization created."), @ApiResponse(code = 400, message = "Creating visualization failed."), @ApiResponse(code = 404, message = "Didn't find requested query.") })
	public HttpResponse visualizeQueryByKey(@PathParam("key") String key, @QueryParam("format") @DefaultValue("") String format, @ContentParam String content) {
		initializeDBConnection();

		Query query = null;
		String[] queryParameters = null;
		try {
			long user = getContext().getMainAgent().getId();
			try {
				JSONObject o = (JSONObject) JSONValue.parseWithException(content);
				queryParameters = stringArrayfromJSON(o, "queryparams");
			} catch (Exception e) {
				// Use default filters
				queryParameters = null;
			}

			QueryManager queryManager = queryManagerMap.get(user);
			query = queryManager.getQuery(key);
			if (query == null) {
				throw new DoesNotExistException("Query does not exist!");
			}
		} catch (DoesNotExistException e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse("Query " + key + " does not exist");
			res.setStatus(404);
			return res;
		} catch (Exception e) {
			HttpResponse res = new HttpResponse(visualizationException.generate(e, "Encountered a problem while trying to fetch stored query " + key));
			res.setStatus(400);
			return res;
		}

		try {
			HttpResponse res = new HttpResponse(visualizeQuery(query, queryParameters));

			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_15, getContext().getMainAgent(), "" + query);
			res.setStatus(200);
			return res;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse(visualizationException.generate(e, "Encountered a Problem while trying to visualize Query!"));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Saves a query. <br>
	 * 
	 * @param queryStatement
	 *            a String containing the query
	 * @param databaseKey
	 *            the key to the database
	 * @param useCache
	 *            if true, a cached result is returned (if available) instead of
	 *            performing the query again
	 * @param modificationTypeIndex
	 *            the desired modification function
	 * @param visualizationTypeIndex
	 *            the desired visualization
	 * @param visualizationParamaters
	 *            an array of additional parameters for the visualization,
	 *            including title, height and weight
	 * 
	 * @return The id of the saved query as a String
	 */
	private String saveQuery(String queryStatement, String[] queryParameters, String databaseKey, boolean useCache, int modificationTypeIndex, VisualizationType visualizationTypeIndex, String[] visualizationParamaters) {

		SQLDatabase database = null;
		Query query = null;
		String queryKey = ""; // If empty, the query generates a new one
		try {
			long user = getContext().getMainAgent().getId();
			SQLDatabaseManager databaseManager = databaseManagerMap.get(user);
			QueryManager queryManager = queryManagerMap.get(user);
			database = databaseManager.getDatabaseInstance(databaseKey);
			query = new Query(getContext().getMainAgent().getId(), database.getJdbcInfo(), database.getUser(), database.getPassword(), databaseKey, database.getDatabase(), database.getHost(), database.getPort(), queryStatement, queryParameters, useCache, modificationTypeIndex, visualizationTypeIndex, visualizationParamaters, queryKey);
			queryManager.storeQuery(query);
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			return VisualizationException.getInstance().generate(e, "An error occured while trying to save a Query!");
		}

		L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_17, getContext().getMainAgent(), "" + query.getQueryStatement());
		return query.getKey();
	}

	/**
	 * Executes a given query.
	 * 
	 * @param query
	 *            the query
	 * 
	 * @return Result a visualization of the query
	 */
	private String visualizeQuery(Query query, String[] queryParameters) throws Exception {
		MethodResult methodResult = null;
		if (query.usesCache() && queryParameters == null) {
			methodResult = resultCache.get(query.getKey());
		}
		if (methodResult == null) { // query was not cached or no cached result
									// desired
			SQLDatabase sqlDatabase = new SQLDatabase(query);

			sqlDatabase.connect();
			ResultSet resultSet = sqlDatabase.executeQuery(query.getInsertedQueryStatement(queryParameters));
			if (resultSet == null) {
				return visualizationException.generate(new Exception(), "Failed to get a result set from the desired database!");
			}
			if (query.usesCache()) {
				methodResult = transformToMethodResult(resultSet, query.getKey());
			} else {
				methodResult = transformToMethodResult(resultSet, ""); // No
																		// caching
																		// desired
																		// by
																		// this
																		// query
			}
			sqlDatabase.disconnect();
		}

		Modification modification = modificationManager.getModification(ModificationType.fromInt(query.getModificationTypeIndex()));
		Visualization visualization = visualizationManager.getVisualization(query.getVisualizationTypeIndex());

		if (modification.check(methodResult))
			methodResult = modification.apply(methodResult);
		else
			return visualizationException.generate(new Exception(), "Can not modify result with " + modification.getType().name() + ".");

		if (visualization.check(methodResult, query.getVisualizationParameters())) {
			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_16, getContext().getMainAgent(), "" + query);
			return visualization.generate(methodResult, query.getVisualizationParameters());
		} else
			return visualizationException.generate(new Exception(), "Can not convert result into " + visualization.getType().name() + "-format.");

	}

	/**
	 * 
	 * Executes a sql query on the specified database. Warning: only a very
	 * simple checking mechanism for escape characters is implemented. Only
	 * queries from trusted sources should be executed!
	 * 
	 * @param sqlQuery
	 *            the query (has to be "ready to execute")
	 * @param databaseKey
	 *            the key of the database which is to be queried
	 * @param cacheKey
	 *            the key which should be used to cache the query result (if
	 *            empty, result is not cached)
	 * 
	 * @return a Method Result
	 */
	private MethodResult executeSQLQuery(String sqlQuery, String[] queryParameters, String databaseKey, String cacheKey) throws L2pServiceException, SQLException {
		if (queryParameters != null && queryParameters.length > 0) {
			sqlQuery = Query.insertParameters(sqlQuery, queryParameters);
		}
		ResultSet resultSet = getResultSet(sqlQuery, databaseKey);
		L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_18, getContext().getMainAgent(), "" + sqlQuery);
		return transformToMethodResult(resultSet, cacheKey);
	}

	/**
	 * 
	 * Transforms a SQL-ResultSet to a MethodResult.
	 * 
	 * @param resultSet
	 *            the Result Set
	 * @param cacheKey
	 *            the key which should be used to cache the query result
	 * 
	 * @return a Method Result
	 * 
	 * @throws L2pServiceException
	 */
	private MethodResult transformToMethodResult(ResultSet resultSet, String cacheKey) throws L2pServiceException {
		try {
			MethodResult methodResult = new MethodResult();

			try {
				if (resultSet == null) {
					throw new Exception("Tried to transform an invalid sql result set!");
				}

				ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
				int columnCount = resultSetMetaData.getColumnCount();

				// first row contains the column names
				String[] columnNames = new String[columnCount];
				for (int i = 1; i <= columnCount; i++) {
					columnNames[i - 1] = resultSetMetaData.getColumnName(i);

					if (columnNames[i - 1] == null) {
						columnNames[i - 1] = "";
					}
				}
				methodResult.setColumnNames(columnNames);

				// the second row the data type
				Integer[] columnTypes = new Integer[columnCount];
				for (int i = 1; i <= columnCount; i++) {
					columnTypes[i - 1] = resultSetMetaData.getColumnType(i);

					if (columnNames[i - 1] == null) {
						// logMessage("Invalid SQL Datatype for column: " + i +
						// ". Fallback to Object...");
					}
				}
				methodResult.setColumnDatatypes(columnTypes);

				// now the result data is added
				while (resultSet.next()) {
					Object[] currentRow = new Object[columnCount];

					for (int i = 1; i <= columnCount; i++) {
						switch (columnTypes[i - 1]) {
						case Types.BOOLEAN:
							currentRow[i - 1] = resultSet.getBoolean(i);
							break;
						case Types.DATE:
							currentRow[i - 1] = resultSet.getDate(i);
							break;
						case Types.TIME:
						case Types.TIMESTAMP:
							currentRow[i - 1] = resultSet.getTime(i);
							break;
						case Types.BIGINT:
							currentRow[i - 1] = resultSet.getLong(i);
							break;
						case Types.DECIMAL:
						case Types.NUMERIC:
							currentRow[i - 1] = resultSet.getBigDecimal(i);
							break;
						case Types.DOUBLE:
							currentRow[i - 1] = resultSet.getDouble(i);
							break;
						case Types.REAL:
						case Types.FLOAT:
							currentRow[i - 1] = resultSet.getFloat(i);
							break;
						case Types.INTEGER:
							currentRow[i - 1] = resultSet.getInt(i);
							break;
						case Types.SMALLINT:
							currentRow[i - 1] = resultSet.getShort(i);
							break;
						case Types.VARCHAR:
							currentRow[i - 1] = resultSet.getString(i);
							break;
						default:
							L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Unknown SQL Datatype: " + columnTypes[i - 1].toString());
							try {
								currentRow[i - 1] = resultSet.getObject(i).toString();
							} catch (Exception e) {
								currentRow[i - 1] = null;
							}
							break;
						}
						;

						// Note: this is a little DANGEROUS because it does not
						// match the
						// column datatype. BUT: toString() works on it
						if (currentRow[i - 1] == null) {
							currentRow[i - 1] = "";
						}
					}
					methodResult.addRow(currentRow);
				}
			} catch (SQLException e) {
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, "SQL exception when trying to handle an SQL query result: " + e.getMessage());
			} catch (Exception e) {
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Exception when trying to handle an SQL query result: " + e.getMessage());
			}

			// since the values are now in the method result the result set can
			// be closed...
			resultSet.close();

			// Caching
			if (!cacheKey.equals("")) {
				resultCache.cache(cacheKey, methodResult);
			}

			return methodResult;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.getMessage().toString());
			throw new L2pServiceException("Exception in Transform to Method Result", e);
		}
	}

	/**
	 * Execute an sql-query and returns the corresponding result set. <br>
	 * The actual database access is done here (by calling
	 * sqlDatabase.executeQuery).
	 * 
	 * @param sqlQuery
	 *            the query
	 * @param databaseKey
	 *            the key of the database
	 * 
	 * @return ResultSet of the database query
	 * @throws LASException
	 */
	private ResultSet getResultSet(String sqlQuery, String databaseKey) throws L2pServiceException, SQLException {
		try {
			if (databaseKey == null || databaseKey.isEmpty() || databaseKey.equalsIgnoreCase("undefined") || databaseKey.equalsIgnoreCase("MonitoringDefault")) {
				databaseKey = stDbKey;
				// logMessage("No database key has been provided. Using default
				// DB: " + databaseKey);
			}
			long user = getContext().getMainAgent().getId();
			SQLDatabaseManager databaseManager = databaseManagerMap.get(user);

			if (databaseManager == null) {
				throw new Exception("Did not provide a valid databaseManager!");
			}

			SQLDatabase sqlDatabase = databaseManager.getDatabaseInstance(databaseKey);
			if (sqlDatabase == null) {
				throw new Exception("Failed to get an instance of the desired database!");
			}

			ResultSet resultSet = sqlDatabase.executeQuery(sqlQuery);
			if (resultSet == null) {
				throw new Exception("Failed to get an result set from the desired database!");
			}

			return resultSet;
		} catch (SQLException ex) {
			throw ex;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			System.out.println(e.getMessage());
			throw new L2pServiceException("Exception in getResultSet", e);
		}
	}

	/**
	 * Simple function to validate a user login. Basically it only serves as a
	 * "calling point" and does not really validate a user (since this is done
	 * previously by LAS2peer itself, the user does not reach this method if he
	 * or she is not authenticated).
	 * 
	 * @return status of login
	 */
	@GET
	@Path("validate")
	public HttpResponse validateLogin() {
		String returnString = "";
		returnString += "You are " + ((UserAgent) getContext().getMainAgent()).getUserData() + " and your login is valid!";

		HttpResponse res = new HttpResponse(returnString);
		res.setStatus(200);
		return res;
	}
}
