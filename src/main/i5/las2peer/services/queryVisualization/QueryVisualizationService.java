package i5.las2peer.services.queryVisualization;

import i5.las2peer.api.Service;
import i5.las2peer.execution.L2pServiceException;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.Consumes;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.restMapper.annotations.DELETE;
import i5.las2peer.restMapper.annotations.GET;
import i5.las2peer.restMapper.annotations.POST;
import i5.las2peer.restMapper.annotations.PUT;
import i5.las2peer.restMapper.annotations.Path;
import i5.las2peer.restMapper.annotations.PathParam;
import i5.las2peer.restMapper.annotations.Produces;
import i5.las2peer.restMapper.annotations.QueryParam;
import i5.las2peer.restMapper.annotations.Version;
import i5.las2peer.restMapper.annotations.swagger.ApiInfo;
import i5.las2peer.restMapper.annotations.swagger.ApiResponse;
import i5.las2peer.restMapper.annotations.swagger.ApiResponses;
import i5.las2peer.restMapper.annotations.swagger.ResourceListApi;
import i5.las2peer.restMapper.annotations.swagger.Summary;
import i5.las2peer.restMapper.tools.ValidationResult;
import i5.las2peer.restMapper.tools.XMLCheck;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.queryVisualization.caching.MethodResultCache;
import i5.las2peer.services.queryVisualization.database.DBDoesNotExistException;
import i5.las2peer.services.queryVisualization.database.DoesNotExistException;
import i5.las2peer.services.queryVisualization.database.SQLDatabase;
import i5.las2peer.services.queryVisualization.database.SQLDatabaseManager;
import i5.las2peer.services.queryVisualization.database.SQLDatabaseSettings;
import i5.las2peer.services.queryVisualization.database.SQLDatabaseType;
import i5.las2peer.services.queryVisualization.database.SQLFilterManager;
import i5.las2peer.services.queryVisualization.encoding.MethodResult;
import i5.las2peer.services.queryVisualization.encoding.Modification;
import i5.las2peer.services.queryVisualization.encoding.ModificationIdentity;
import i5.las2peer.services.queryVisualization.encoding.ModificationLogarithmic;
import i5.las2peer.services.queryVisualization.encoding.ModificationManager;
import i5.las2peer.services.queryVisualization.encoding.ModificationNormalization;
import i5.las2peer.services.queryVisualization.encoding.ModificationType;
import i5.las2peer.services.queryVisualization.encoding.Visualization;
import i5.las2peer.services.queryVisualization.encoding.VisualizationBarChart;
import i5.las2peer.services.queryVisualization.encoding.VisualizationCSV;
import i5.las2peer.services.queryVisualization.encoding.VisualizationException;
import i5.las2peer.services.queryVisualization.encoding.VisualizationGoogleTable;
import i5.las2peer.services.queryVisualization.encoding.VisualizationHTMLTable;
import i5.las2peer.services.queryVisualization.encoding.VisualizationJSON;
import i5.las2peer.services.queryVisualization.encoding.VisualizationLineChart;
import i5.las2peer.services.queryVisualization.encoding.VisualizationManager;
import i5.las2peer.services.queryVisualization.encoding.VisualizationPieChart;
import i5.las2peer.services.queryVisualization.encoding.VisualizationRadarChart;
import i5.las2peer.services.queryVisualization.encoding.VisualizationTimeline;
import i5.las2peer.services.queryVisualization.encoding.VisualizationType;
import i5.las2peer.services.queryVisualization.encoding.VisualizationXML;
import i5.las2peer.services.queryVisualization.query.Query;
import i5.las2peer.services.queryVisualization.query.QueryManager;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 * LAS2peer Service
 * 
 * This is a template for a very basic LAS2peer service
 * that uses the LAS2peer Web-Connector for RESTful access to it.
 * 
 */
@Path("QVS")
@Version("0.1")
@ApiInfo(
		  title="Query Visualization Service",
		  description="This service can be used to visualize queries on RDB's",
		  termsOfServiceUrl="https://github.com/rwth-acis/LAS2peer-Query-Visualization-Service",
		  contact="renzel@dbis.rwth-aachen.de",
		  license="MIT",
		  licenseUrl="https://raw.githubusercontent.com/rwth-acis/LAS2peer-Query-Visualization-Service/master/LICENSE"
		)
public class QueryVisualizationService extends Service {

	/*** configuration ***/
	public static final String DEFAULT_DATABASE_KEY = "storage";
	public static final String DEFAULT_HOST = "This value should be replaced automatically by configuration file!"; 
	public static final int DEFAULT_PORT = 0; //This value should be replaced automatically by configuration file!
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
		JSONArray ja = (JSONArray)obj.get(key);
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
			return (boolean)Boolean.parseBoolean(b);
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
		// IF THE SERVICE CLASS NAME IS CHANGED, THE PROPERTIES FILE NAME NEED TO BE CHANGED TOO!
		setFieldValues();
	}		
	
	public void initializeDBConnection() {
		long user = getActiveAgent().getId();
		if (databaseManagerMap.get(user) != null) {
			return;
		}
		try {
			if(stDbHost == null || stDbPort < 1 || stDbDatabase == null || stDbUser == null || stDbPassword == null) {
				logError("Provided invalid parameters (default database) for the service! Please check you service config file!");
			}
			if(resultTimeout == null) {
				resultTimeout = "90";
			}

			databaseSettings = new SQLDatabaseSettings(stDbKey, SQLDatabaseType.MYSQL, stDbUser,
					stDbPassword, stDbDatabase, stDbHost, stDbPort);

			// setup the database manager
			storageDatabase = new SQLDatabase(databaseSettings);

			SQLDatabaseManager databaseManager = new SQLDatabaseManager(this, storageDatabase);
			databaseManagerMap.put(user, databaseManager);
			QueryManager queryManager = new QueryManager(this, storageDatabase);
			queryManagerMap.put(user, queryManager);

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

			if(databaseManager.getDatabaseCount() < 1) {

				// the user has no databases yet - add the example database
				addDatabase(exKey, SQLDatabaseType.valueOf(exType.toUpperCase()), exUser, exPassword, exDatabase, exHost, exPort, VisualizationType.JSON);
//				if(!this.databaseManager.addExampleDB()) {
//					// failed to add the database...
//					throw new Exception("Failed to add the default database for the user!");
//				}
			}

			// get the result cache
			this.resultCache = MethodResultCache.getInstance(Integer.parseInt(resultTimeout));

		} 
		catch(Exception e) {
			logError(e);
		}
	}

	/**
	 * Adds a sql database to the users available/usable databases (via the sqldatabase manager).
	 * 
	 * @param databaseKey key which is later used to identify the database
	 * @param vtypei Type of the response
	 * @param content Credentials for the database
	 * 
	 * @return success or error message, if possible in the requested encoding/format
	 */
	@PUT
	@Path("database/{key}")
	@Summary("Adds a database with a specified key")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value={
			  @ApiResponse(code = 201, message = "Database added successfully."),
			  @ApiResponse(code = 400, message = "Database data invalid.")})
	public HttpResponse addDatabase(
			@PathParam("key") String databaseKey,
			@ContentParam String content) {
		JSONObject o;
		try{	
			o = (JSONObject) JSONValue.parseWithException(content);
			String dbcode = stringfromJSON(o, "db_code");
			String username = stringfromJSON(o, "username");
			String password = stringfromJSON(o, "password");
			String database = stringfromJSON(o, "database");
			String dbhost = stringfromJSON(o, "dbhost");
			Integer port = intfromJSON(o, "port");
			return addDatabase(databaseKey, SQLDatabaseType.valueOf(dbcode.toUpperCase()),
					username, password, database, dbhost, port, VisualizationType.JSON);
		} catch (Exception e) {
			logError(e);
			HttpResponse res = new HttpResponse(visualizationException.generate(e, "Received invalid JSON"));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Adds a sql database to the users available/usable databases (via the sqldatabase manager).
	 * 
	 * @param databaseKey key which is later used to identify the database
	 * @param databaseTypeCode	type of the database (DB2, MySql, ...)
	 * @param username the username for the database
	 * @param password the password for the database
	 * @param database the name of the database
	 * @param host the database address
	 * @param port the database port
	 * @param visualizationTypeIndex encoding of the returned message
	 * 
	 * @return success or error message, if possible in the requested encoding/format
	 */
	public HttpResponse addDatabase( String databaseKey, SQLDatabaseType databaseTypeCode, String username, String password,
			String database, String host, Integer port, VisualizationType visualizationTypeIndex) {
		try {
			if(databaseKey == null || databaseKey.length() < 3) {
				throw new Exception("Databasekey is too short (Use at least 2 characters).");
			}
			
			initializeDBConnection();
			long user = getActiveAgent().getId();
			SQLDatabaseManager databaseManager = databaseManagerMap.get(user);

			SQLDatabaseType sqlDatabaseType = databaseTypeCode;
			if(!databaseManager.addDatabase(databaseKey,sqlDatabaseType, username, password, database, host, port)) {
				throw new Exception("Failed to add a database for the user!");
			}

			// verify that it works (that one can get an instance, probably its going to be used later anyways)...
			try {
				if(databaseManager.getDatabaseInstance(databaseKey) == null) {
					throw new Exception("Failed to get a database instance for " + databaseKey);
				} 
			}
			catch (Exception e) {
				databaseManager.removeDatabase(databaseKey);
				throw e;
			}

			MethodResult result = new MethodResult();
			result.setColumnName("AddedDatabase");
			result.setColumnDatatype(Types.VARCHAR);
			Object[] defaultDatabase = {databaseKey};
			result.addRow(defaultDatabase);

			HttpResponse res = new HttpResponse(visualizationManager
					.getVisualization(visualizationTypeIndex)
					.generate(result, null));
			res.setStatus(201);
			return res;
		}
		catch (Exception e) {
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}
	
	/**
	 * Removes a database from the user's list of configured databases (so that the user can not access the database anymore).
	 * Other user's database settings are not changed.
	 * 
	 * @param databaseKey the key of the database
	 * @param vtypei Visualization type of the output
	 * @return success or error message, if possible in the requested encoding/format
	 */
	@DELETE
	@Path("database/{key}")
	@Summary("Removes a database with a specified key")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value={
			  @ApiResponse(code = 200, message = "Database removed successfully."),
			  @ApiResponse(code = 400, message = "Database removal failed.")})
	public HttpResponse removeDatabase(@PathParam("key") String databaseKey) {
		try {			
			if(databaseKey.equalsIgnoreCase("MonitoringDefault")) {
				databaseKey = stDbKey;
			}
			
			initializeDBConnection();

			long user = getActiveAgent().getId();
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
			Object[] defaultDatabase = {databaseKey};
			result.addRow(defaultDatabase);

			HttpResponse res = new HttpResponse(
					visualizationManager.getVisualization(VisualizationType.JSON).generate(result, null));
			res.setStatus(200);
			return res;
		}
		catch (Exception e) {
			logError(e);
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Returns a list/table of the keys of all available/configured databases of the user.
	 * 
	 * @param visualizationTypeIndex encoding of the returned message
	 * @return success or error message, if possible in the requested encoding/format
	 */
	@GET
	@Path("database")
	@ResourceListApi(description = "Manage a users databases")
	@Summary("Gets all database keys for your user")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value={
			  @ApiResponse(code = 200, message = "Got Database keys."),
			  @ApiResponse(code = 400, message = "Retrieving keys failed.")})
	public HttpResponse getDatabaseKeys(
			@QueryParam(defaultValue = "JSON", name = "format") String visualizationTypeIndex) {
		try {
			initializeDBConnection();
			long user = getActiveAgent().getId();
			SQLDatabaseManager databaseManager = databaseManagerMap.get(user);
			List<String> keyList = databaseManager.getDatabaseKeyList();

			if(keyList == null) {
				throw new Exception("Failed to get the key list for the users' databases!");
			}

			MethodResult result = new MethodResult();
			result.setColumnName("DatabaseKeys");
			result.setColumnDatatype(Types.VARCHAR);

			Iterator<String> iterator = keyList.iterator();

			while(iterator.hasNext()) {
				Object[] currentDatabaseKey = {iterator.next()};

				if(((String)currentDatabaseKey[0]).equalsIgnoreCase("MonitoringDefault")) {
					currentDatabaseKey[0] = stDbKey;
				}

				result.addRow(currentDatabaseKey);
			}

			HttpResponse res = new HttpResponse(
					visualizationManager.getVisualization(VisualizationType.valueOf(visualizationTypeIndex.toUpperCase())).generate(result, null));
			res.setStatus(200);
			return res;
		} catch (Exception e) {
			logError(e);
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Returns a list/table of all filters the user has configured.
	 * 
	 * @param visualizationTypeIndex encoding of the returned message
	 * @return success or error message, if possible in the requested encoding/format
	 */
	@GET
	@Path("filter")
	@ResourceListApi(description = "Manage a users filters")
	@Summary("Gets all filter keys for your user")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value={
			  @ApiResponse(code = 200, message = "Got filter keys."),
			  @ApiResponse(code = 400, message = "Retrieving filter keys failed.")})
	public HttpResponse getFilterKeys(@QueryParam(defaultValue = "JSON", name = "format") String visualizationTypeIndex) {
		try {
			initializeDBConnection();

			long user = getActiveAgent().getId();
			SQLFilterManager filterManager = filterManagerMap.get(user);
			if(filterManager == null) {
				// initialize filter manager
				filterManager = new SQLFilterManager(storageDatabase);
			}

			List<String> keyList = filterManager.getFilterKeyList();

			if(keyList == null) {
				throw new Exception("Failed to get the key list for the users' filters!");
			}
			
			VisualizationType vtypei = VisualizationType.valueOf(visualizationTypeIndex.toUpperCase());

			if(keyList.isEmpty()) {
				// in order to encounter the cold-start...
				// add some examples for the default DB
				this.addFilter("Customers", "SELECT DISTINCT customerNumber FROM `customers`", exKey, vtypei);

				keyList = filterManager.getFilterKeyList();
			}

			MethodResult result = new MethodResult();
			Integer[] datatypes = {Types.VARCHAR,Types.VARCHAR};
			result.setColumnDatatypes(datatypes);
			String[] names = {"FilterKeys","DatabaseKeys"};
			result.setColumnNames(names);

			Iterator<String> iterator = keyList.iterator();
			while(iterator.hasNext()) {
				String filterKey = iterator.next();
				String databaseKey = filterManager.getDatabaseKey(filterKey);
				Object[] currentRow = {filterKey,databaseKey};
				result.addRow(currentRow);
			}


			HttpResponse res = new HttpResponse(
					visualizationManager.getVisualization(vtypei).generate(result, null));
			res.setStatus(200);
			return res;
		}
		catch (Exception e) {
			logError(e);
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Retrieves the values for a specific filter.
	 * 
	 * @param filterKey
	 * @param visualizationTypeIndex encoding of the returned message
	 * @return success or error message, if possible in the requested encoding/format
	 */
	@GET
	@Path("filter/{key}")
	@Summary("Gets the values for a filter")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value={
			  @ApiResponse(code = 200, message = "Got filter values."),
			  @ApiResponse(code = 400, message = "Retrieving filter keys failed."),
			  @ApiResponse(code = 204, message = "Filter does not exist.")})
	public HttpResponse getFilterValues(
			@PathParam("key") String filterKey,
			@QueryParam(name="format", defaultValue = "JSON") String visualizationTypeIndex) {
		try {
			VisualizationType vtypei = VisualizationType.valueOf(visualizationTypeIndex.toUpperCase());
			initializeDBConnection();
			long user = getActiveAgent().getId();
			SQLFilterManager filterManager = filterManagerMap.get(user);
			if(filterManager == null) {
				// initialize filter manager
				filterManager = new SQLFilterManager(storageDatabase);
			}

			HttpResponse res = new HttpResponse(
					filterManager.getFilterValues(filterKey, vtypei, this));
			res.setStatus(200);
			return res;
		} catch (DoesNotExistException e) {
			HttpResponse res = new HttpResponse("Filter " + filterKey + " does not exist.");
			res.setStatus(404);
			return res;
		} catch (Exception e) {
			logError(e);
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}		

	/**
	 * Adds a filter to the user's settings/profile.
	 * 
	 * @param filterKey the Key that should be used for this filter
	 * @param SQLQuery SQL  query which is used to retrieve the filter values
	 * @param stDbKey	key of the database for which the filter has been configured
	 * @param visualizationTypeIndex  encoding of the returned message
	 * 
	 * @return success or error message, if possible in the requested encoding/format
	 */
	@PUT
	@Path("filter/{key}")
	@Summary("Adds a filter with a specified key")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiResponses(value={
			  @ApiResponse(code = 201, message = "Added filter."),
			  @ApiResponse(code = 400, message = "Adding filter failed.")})
	public HttpResponse addFilter(@PathParam("key") String filterKey,
			@ContentParam String content) {
		JSONObject o;
		try{	
			VisualizationType vtypei = VisualizationType.JSON;
			o = (JSONObject) JSONValue.parseWithException(content);
			String query = stringfromJSON(o, "query");
			String dbkey = stringfromJSON(o, "dbkey");
			return addFilter(filterKey, query, dbkey, vtypei);
		} catch (Exception e) {
			logError(e);
			HttpResponse res = new HttpResponse(visualizationException.generate(e, "Received invalid JSON"));
			res.setStatus(400);
			return res;
		}
	}

	public HttpResponse addFilter(String filterKey, String SQLQuery, String databaseKey, VisualizationType visualizationTypeIndex) {
		try {
			initializeDBConnection();
			//TODO: parameter sanity checks
			long user = getActiveAgent().getId();
			SQLFilterManager filterManager = filterManagerMap.get(user);

			if(filterManager == null) {
				// initialize filter manager
				filterManager = new SQLFilterManager(storageDatabase);
			}

			if(!filterManager.addFilter(filterKey, SQLQuery, databaseKey)) {
				throw new Exception("Failed to add a database for the user!");
			}

			// verify that it works (that one can get an instance, probably its going to be used later anyways)...
			try {
				if(filterManager.getFilterValues(filterKey, visualizationTypeIndex, this) == null) {
					throw new Exception("Failed to retrieve the filter values!");
				}
			}
			catch (Exception e) {
				filterManager.deleteFilter(filterKey);
				throw e;
			}

			MethodResult result = new MethodResult();
			result.setColumnName("AddedFilter");
			result.setColumnDatatype(Types.VARCHAR);
			Object[] addedFilter = {filterKey};
			result.addRow(addedFilter);

			HttpResponse res = new HttpResponse(
					visualizationManager.getVisualization(visualizationTypeIndex).generate(result, null));
			res.setStatus(201);
			return res;
		}
		catch (Exception e) {
			logError(e);
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Deletes a filter from the user's settings/profile.
	 * 
	 * @param filterKey the key of the filter
	 * @param visualizationTypeIndex encoding of the returned message
	 * @return success or error message, if possible in the requested encoding/format
	 */
	@DELETE
	@Path("filter/{key}")
	@Summary("Deletes a filter with a specified key")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value={
			  @ApiResponse(code = 200, message = "Deleted filter."),
			  @ApiResponse(code = 400, message = "Deleting filter failed.")})
	public HttpResponse deleteFilter(@PathParam("key") String filterKey) {
		try {		
			initializeDBConnection();
			long user = getActiveAgent().getId();
			SQLFilterManager filterManager = filterManagerMap.get(user);
			if(filterManager == null) {
				// initialize filter manager
				filterManager = new SQLFilterManager(storageDatabase);
			}

			if(!filterManager.deleteFilter(filterKey)) {
				HttpResponse res = new HttpResponse("Filter " + filterKey + " does not exist!");
				res.setStatus(404);
				return res;
			}

			MethodResult result = new MethodResult();
			result.setColumnName("DeletedFilter");
			result.setColumnDatatype(Types.VARCHAR);
			Object[] deletedDatabase = {filterKey};
			result.addRow(deletedDatabase);

			HttpResponse res = new HttpResponse(
					visualizationManager.getVisualization(VisualizationType.JSON).generate(result, null));
			res.setStatus(200);
			return res;
		}
		catch (Exception e) {
			logError(e);
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	@POST
	@Path("query/visualize")
	@Summary("Visualizes a query without saving it")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiResponses(value={
			  @ApiResponse(code = 200, message = "Created query."),
			  @ApiResponse(code = 400, message = "Creating Query failed.")})
	public HttpResponse visualizeQuery(
			@QueryParam(defaultValue = "JSON", name = "format") String vtypei,
			@ContentParam String content) {
		JSONObject o;
		try{	
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
			return res;
		} catch (Exception e) {
			logError(e);
			HttpResponse res = new HttpResponse(visualizationException.generate(e, "Received invalid JSON"));
			res.setStatus(400);
			return res;
		}
	}

	@POST
	@Path("query")
	@Summary("Saves a query and returns the ID")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiResponses(value={
			  @ApiResponse(code = 201, message = "Created query."),
			  @ApiResponse(code = 400, message = "Creating Query failed.")})
	public HttpResponse createQuery(
			@QueryParam(defaultValue = "JSON", name = "format") String vtypei,
			@ContentParam String content) {
		JSONObject o;
		try{	
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
			return res;
		} catch (Exception e) {
			logError(e);
			HttpResponse res = new HttpResponse(visualizationException.generate(e, "Received invalid JSON"));
			res.setStatus(400);
			return res;
		}
	}

	public HttpResponse createQuery(String query, String[] queryParameters, String databaseKey, boolean useCache,
			int modificationTypeIndex, VisualizationType visualizationTypeIndex, String title, String width, String height,
			boolean save) {
		String queryString = createQueryString(query, queryParameters, databaseKey, useCache, modificationTypeIndex, visualizationTypeIndex, new String[]{title, height, width}, save);
		HttpResponse res = new HttpResponse(queryString);
		if (queryString.startsWith("The Query has lead to an error.")) {
			res.setStatus(400);
		} else {
			res.setStatus(201);
		}
		return res;
	}

	/**
	 * Generates a query on the specified database. The query may contain placeholders that have to be replaced by the parameters.
	 * <br>
	 * This is the main services entry point that should be used to generate and save queries.
	 * A placeholder is set by $placeholder$ and will be replaced by the corresponding element in the query parameters.
	 * The query parameter array has to contain the elements in the same order as they appear in the query.
	 * 
	 * @param query a String containing the query
	 * @param queryParameters the query parameters as an array of Strings that contain the content of the placeholders
	 * @param databaseKey a String containing the database key
	 * @param useCache if true, a cached result is returned (if available) instead of performing the query again (does only affect stored queries!)
	 * @param modificationTypeIndex the desired modification function
	 * @param visualizationTypeIndex the desired visualization
	 * @param visualizationParameters an array of additional parameters for the visualization, including title, height and weight
	 * @param save if set, the query will be saved and the return statement will be the query id.
	 * 
	 * @return Result of the query in the requested output format or the id of the saved query
	 */
	public String createQueryString(String query, String[] queryParameters, String databaseKey, boolean useCache,
			int modificationTypeIndex, VisualizationType visualizationTypeIndex, String[] visualizationParameters, boolean save) {
		initializeDBConnection();

		MethodResult methodResult = null;
		String cacheKey = ""; // If empty, the query is not cached (used in "Non-Saving"-case, as creation always is)

		try {
			//Check for visualization parameters
			if(visualizationTypeIndex.ordinal() > 3 && visualizationTypeIndex.ordinal() < 10){
				if(visualizationParameters == null || visualizationParameters.length != 3){
					return visualizationException.generate(new Exception(), "Missing visualization-parameters!");
				}
			}

			query = insertParameters(query, queryParameters);

			if(save){
				return saveQuery(query, databaseKey, useCache, modificationTypeIndex, visualizationTypeIndex, visualizationParameters);
			}
			methodResult = executeSQLQuery(query, databaseKey, cacheKey);
			Modification modification = modificationManager.getModification(ModificationType.fromInt(modificationTypeIndex));
			Visualization visualization = visualizationManager.getVisualization(visualizationTypeIndex);

			if(modification.check(methodResult))
				methodResult = modification.apply(methodResult);
			else
				return visualizationException.generate(new Exception(), "Can not modify result with " + modification.getType().name() + ".");

			if(visualization.check(methodResult, visualizationParameters))
				return visualization.generate(methodResult, visualizationParameters);
			else
				return visualizationException.generate(new Exception(), "Can not convert result into " + visualization.getType().name() + "-format.");
		}
		catch(Exception e) {
			logError(e);
			return visualizationException.generate(e, "An Error occured while trying to execute the query!");
		}
	}

	/**
	 * Returns a list/table of the keys of all available/configured databases of the user.
	 * 
	 * @param visualizationTypeIndex encoding of the returned message
	 * @return success or error message, if possible in the requested encoding/format
	 */
	@GET
	@Path("query")
	@ResourceListApi(description = "Manage a users queries")
	@Summary("Gets all queries for your user")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value={
			  @ApiResponse(code = 200, message = "Got Database queries."),
			  @ApiResponse(code = 400, message = "Retrieving queries failed.")})
	public HttpResponse getQueryKeys(
			@QueryParam(defaultValue = "JSON", name = "format") String visualizationTypeIndex) {
		try {
			initializeDBConnection();
			long user = getActiveAgent().getId();
			QueryManager queryManager = queryManagerMap.get(user);
			List<Query> keyList = queryManager.getQueryList();

			if(keyList == null) {
				throw new Exception("Failed to get the key list for the users' queries!");
			}

			MethodResult result = new MethodResult();
			Integer[] datatypes = {Types.VARCHAR,Types.VARCHAR,Types.VARCHAR};
			result.setColumnDatatypes(datatypes);
			String[] names = {"QueryKeys","DatabaseKeys","Title"};
			result.setColumnNames(names);
			Iterator<Query> iterator = keyList.iterator();

			while(iterator.hasNext()) {
				Query q = iterator.next();
				String queryKey = q.getKey();
				String databaseKey = q.getDatabase();
				String title = q.getTitle();
				Object[] currentRow = {queryKey,databaseKey,title};

				result.addRow(currentRow);
			}

			HttpResponse res = new HttpResponse(
					visualizationManager.getVisualization(VisualizationType.valueOf(visualizationTypeIndex.toUpperCase())).generate(result, null));
			res.setStatus(200);
			return res;
		} catch (Exception e) {
			logError(e);
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Deletes a filter from the user's settings/profile.
	 * 
	 * @param queryKey the key of the filter
	 * @param visualizationTypeIndex encoding of the returned message
	 * @return success or error message, if possible in the requested encoding/format
	 */
	@DELETE
	@Path("query/{key}")
	@Summary("Deletes a query with a specified key")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value={
			  @ApiResponse(code = 200, message = "Deleted query."),
			  @ApiResponse(code = 400, message = "Deleting query failed.")})
	public HttpResponse deleteQuery( @PathParam("key") String queryKey) {
		try {		
			initializeDBConnection();
			long user = getActiveAgent().getId();
			QueryManager queryManager = queryManagerMap.get(user);
			if(queryManager == null) {
				throw new Exception("Query Manager is null");
			}

			queryManager.removeQ(queryKey);

			MethodResult result = new MethodResult();
			result.setColumnName("DeletedQuery");
			result.setColumnDatatype(Types.VARCHAR);
			Object[] deletedQuery = {queryKey};
			result.addRow(deletedQuery);

			HttpResponse res = new HttpResponse(
					visualizationManager.getVisualization(VisualizationType.JSON).generate(result, null));
			res.setStatus(200);
			return res;
		}
		catch (Exception e) {
			logError(e);
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Executes a stored query on the specified database.
	 * <br>
	 * This is the main services entry point that should be used to visualize saved queries.
	 * 
	 * @param key a String that contains the id of the query
	 * 
	 * @return Result of the query in the given output format
	 */
	@GET
	@Path("query/{key}")
	@Summary("Visualizes a query with a given key")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value={
			  @ApiResponse(code = 200, message = "Visualization created."),
			  @ApiResponse(code = 400, message = "Creating visualization failed."),
			  @ApiResponse(code = 404, message = "Didn't find requested query.")})
	public HttpResponse getQueryValues(@PathParam("key") String key,
			@QueryParam(name="format", defaultValue = "JSON") String format) {
		initializeDBConnection();

		try {
//			VisualizationType vtypei = VisualizationType.valueOf(format.toUpperCase());
			initializeDBConnection();
			long user = getActiveAgent().getId();
			QueryManager queryManager = queryManagerMap.get(user);
			if(queryManager == null) {
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
			return res;
		} catch (DoesNotExistException e) {
			logError(e);
			HttpResponse res = new HttpResponse("Query " + key + " does not exist.");
			res.setStatus(400);
			return res;
		} catch (DBDoesNotExistException e) {
			logError(e);
			long user = getActiveAgent().getId();
			QueryManager queryManager = queryManagerMap.get(user);
			queryManager.removeQ(key);
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		} catch (Exception e) {
			logError(e);
			HttpResponse res = new HttpResponse(visualizationException.generate(e, null));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Executes a stored query on the specified database.
	 * <br>
	 * This is the main services entry point that should be used to visualize saved queries.
	 * 
	 * @param key a String that contains the id of the query
	 * 
	 * @return Result of the query in the given output format
	 */
	@GET
	@Path("query/{key}/visualize")
	@Summary("Visualizes a query with a given key")
	@ApiResponses(value={
			  @ApiResponse(code = 200, message = "Visualization created."),
			  @ApiResponse(code = 400, message = "Creating visualization failed."),
			  @ApiResponse(code = 404, message = "Didn't find requested query.")})
	public HttpResponse visualizeQueryByKey(@PathParam("key") String key,
			@QueryParam(name="format", defaultValue = "") String format) {
		initializeDBConnection();

		Query query = null;
		try {
			long user = getActiveAgent().getId();
			QueryManager queryManager = queryManagerMap.get(user);
			query = queryManager.getQuery(key);
			if(query == null) {
				throw new DoesNotExistException("Query does not exist!"); 
			}
		} catch(DoesNotExistException e) {
			logError(e.getMessage());
			HttpResponse res = new HttpResponse(
					"Query " + key + " does not exist");
			res.setStatus(404);
			return res;
		} catch(Exception e) {
			HttpResponse res = new HttpResponse(
					visualizationException.generate(e, "Encountered a problem while trying to fetch stored query " + key));
			res.setStatus(400);
			return res;
		}

		try {
			HttpResponse res = new HttpResponse(visualizeQuery(query));
			res.setStatus(200);
			return res;
		} catch(Exception e) {
			logError(e.getMessage());
			HttpResponse res = new HttpResponse(
					visualizationException.generate(e, "Encountered a Problem while trying to visualize Query!"));
			res.setStatus(400);
			return res;
		}
	}

	/**
	 * Saves a query.
	 * <br>
	 * 
	 * @param queryStatement a String containing the query
	 * @param databaseKey the key to the database
	 * @param useCache if true, a cached result is returned (if available) instead of performing the query again
	 * @param modificationTypeIndex the desired modification function
	 * @param visualizationTypeIndex the desired visualization
	 * @param visualizationParamaters an array of additional parameters for the visualization, including title, height and weight
	 * 
	 * @return The id of the saved query as a String
	 */
	private String saveQuery(String queryStatement, String databaseKey,
			boolean useCache, int modificationTypeIndex, VisualizationType visualizationTypeIndex, String[] visualizationParamaters) {

		SQLDatabase database = null;
		Query query = null;
		String queryKey = ""; //If empty, the query generates a new one
		try {
			long user = getActiveAgent().getId();
			SQLDatabaseManager databaseManager = databaseManagerMap.get(user);
			QueryManager queryManager = queryManagerMap.get(user);
			database = databaseManager.getDatabaseInstance(databaseKey);
			query = new Query(getL2pThread().getContext().getMainAgent().getId(), database.getJdbcInfo(),
					database.getUser(), database.getPassword(), database.getDatabase(), database.getHost(),
					database.getPort(), queryStatement, useCache, modificationTypeIndex, visualizationTypeIndex,
					visualizationParamaters, queryKey);
			queryManager.storeQuery(query);
		} catch (Exception e) {
			logError(e.getMessage());
			return VisualizationException.getInstance().generate(e, "An error occured while trying to save a Query!");
		}

		return query.getKey();
	}

	/**
	 * Executes a given query.
	 * 
	 * @param query the query
	 * 
	 * @return Result a visualization of the query
	 */
	private String visualizeQuery(Query query) throws Exception{
		MethodResult methodResult = null;
		if(query.usesCache()){
			methodResult = resultCache.get(query.getKey());
		}

		if(methodResult == null) { //query was not cached or no cached result desired
			SQLDatabase sqlDatabase = new SQLDatabase(query);

			sqlDatabase.connect();
			ResultSet resultSet = sqlDatabase.executeQuery(query.getQueryStatement());
			if(resultSet == null) {
				return visualizationException.generate(new Exception(), "Failed to get a result set from the desired database!");
			}
			if(query.usesCache()){
				methodResult = transformToMethodResult(resultSet, query.getKey());
			}
			else{
				methodResult = transformToMethodResult(resultSet, ""); //No caching desired by this query
			}
			sqlDatabase.disconnect();
		}

		Modification modification = modificationManager.getModification(ModificationType.fromInt(query.getModificationTypeIndex()));
		Visualization visualization = visualizationManager.getVisualization(query.getVisualizationTypeIndex());

		if(modification.check(methodResult))
			methodResult = modification.apply(methodResult);
		else
			return visualizationException.generate(new Exception(), "Can not modify result with " + modification.getType().name() + ".");

		if(visualization.check(methodResult, query.getVisualizationParameters()))
			return visualization.generate(methodResult, query.getVisualizationParameters());
		else
			return visualizationException.generate(new Exception(), "Can not convert result into " + visualization.getType().name() + "-format.");

	}

	/**
	 * Inserts the query parameters and returns the "ready to use" query.
	 * 
	 * @param query a query with placeholders
	 * @param queryParameters the corresponding query parameters
	 * 
	 * @return the query with the inserted query parameters
	 * @throws L2pServiceException 
	 * 
	 */
	private String insertParameters(String query, String[] queryParameters) throws L2pSecurityException, L2pServiceException {
		try {
			//Check, if the Array is empty, then no parameters have to be inserted
			if(queryParameters == null) return query;

			// go through the query, replace placeholders by the values from the query parameters
			int parameterCount = queryParameters.length;
			Pattern placeholderPattern = Pattern.compile("\\$.*?\\$");
			for(int i=0; i<parameterCount; i++) {
				query = placeholderPattern.matcher(query).replaceFirst(queryParameters[i]);
			}
			return query;
		}
		catch(Exception e) {
			logError(e);
			throw new L2pServiceException("exception in insertParameters", e);
		}
	}

	/**
	 * 
	 * Executes a sql query on the specified database.
	 * Warning: only a very simple checking mechanism for escape characters is implemented. Only queries from trusted sources should be executed!
	 * 
	 * @param sqlQuery the query (has to be "ready to execute")
	 * @param databaseKey the key of the database which is to be queried
	 * @param cacheKey the key which should be used to cache the query result (if empty, result is not cached)
	 * 
	 * @return a Method Result
	 */
	private MethodResult executeSQLQuery(String sqlQuery, String databaseKey, String cacheKey) throws L2pServiceException {	
		ResultSet resultSet = getResultSet(sqlQuery, databaseKey);
		return transformToMethodResult(resultSet, cacheKey);
	}

	/**
	 * 
	 * Transforms a SQL-ResultSet to a MethodResult.
	 * 
	 * @param resultSet the Result Set
	 * @param cacheKey the key which should be used to cache the query result
	 * 
	 * @return a Method Result
	 * 
	 * @throws L2pServiceException 
	 */
	private MethodResult transformToMethodResult(ResultSet resultSet, String cacheKey) throws L2pServiceException {	
		try{
			MethodResult methodResult = new MethodResult();

			try {
				if(resultSet == null) {
					throw new Exception("Tried to transform an invalid sql result set!");
				}

				ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
				int columnCount = resultSetMetaData.getColumnCount();

				// first row contains the column names
				String[] columnNames = new String[columnCount];
				for (int i = 1; i <= columnCount; i++) {
					columnNames[i-1] = resultSetMetaData.getColumnName(i);

					if(columnNames[i-1] == null) {
						columnNames[i-1] = "";
					}
				}
				methodResult.setColumnNames(columnNames);

				// the second row the data type
				Integer[] columnTypes = new Integer[columnCount];
				for (int i = 1; i <= columnCount; i++) {
					columnTypes[i-1] = resultSetMetaData.getColumnType(i);

					if(columnNames[i-1] == null) {
						logMessage("Invalid SQL Datatype for column: " + i + ". Fallback to Object...");
					}
				}
				methodResult.setColumnDatatypes(columnTypes);

				// now the result data is added
				while(resultSet.next()) {
					Object[] currentRow = new Object[columnCount];

					for(int i = 1; i<=columnCount; i++) {
						switch(columnTypes[i-1]) {
						case Types.BOOLEAN:
							currentRow[i-1] = resultSet.getBoolean(i);
							break;
						case Types.DATE:
							currentRow[i-1] = resultSet.getDate(i);
							break;
						case Types.TIME:
						case Types.TIMESTAMP:
							currentRow[i-1] = resultSet.getTime(i);
							break;
						case Types.BIGINT:
							currentRow[i-1] = resultSet.getLong(i);
							break;
						case Types.DECIMAL:
						case Types.NUMERIC:
							currentRow[i-1] = resultSet.getBigDecimal(i);
							break;
						case Types.DOUBLE:
							currentRow[i-1] = resultSet.getDouble(i);
							break;
						case Types.REAL:
						case Types.FLOAT:
							currentRow[i-1] = resultSet.getFloat(i);
							break;
						case Types.INTEGER:
							currentRow[i-1] = resultSet.getInt(i);
							break;
						case Types.SMALLINT:
							currentRow[i-1] = resultSet.getShort(i);
							break;
						case Types.VARCHAR:
							currentRow[i-1] = resultSet.getString(i);
							break;
						default:
							logMessage("Unknown SQL Datatype: " + columnTypes[i-1].toString());
							try {
								currentRow[i-1] = resultSet.getObject(i).toString();
							} catch (Exception e) {
								currentRow[i-1] = null;
							}
							break;
						};

						// Note: this is a little DANGEROUS because it does not match the
						// column datatype. BUT: toString() works on it
						if(currentRow[i-1] == null) {
							currentRow[i-1] = "";
						}
					}
					methodResult.addRow(currentRow);
				}
			}
			catch (SQLException e) {
				logMessage("SQL exception when trying to handle an SQL query result: " + e.getMessage());
			}
			catch(Exception e) {
				logMessage("Exception when trying to handle an SQL query result: " + e.getMessage());
			}

			// since the values are now in the method result the result set can be closed...
			resultSet.close();

			//Caching
			if(!cacheKey.equals("")){
				resultCache.cache(cacheKey, methodResult);
			}

			return methodResult;
		} 
		catch (Exception e) {
			logError(e.getMessage());
			throw new L2pServiceException("Exception in Transform to Method Result", e);
		}
	}

	/**
	 * Execute an sql-query and returns the corresponding result set.
	 * <br>
	 * The actual database access is done here (by calling sqlDatabase.executeQuery).
	 * 
	 * @param sqlQuery the query
	 * @param databaseKey the key of the database
	 * 
	 * @return ResultSet of the database query
	 * @throws LASException
	 */
	private ResultSet getResultSet(String sqlQuery, String databaseKey) throws L2pServiceException {
		try{
			if(databaseKey == null || databaseKey.isEmpty() || databaseKey.equalsIgnoreCase("undefined") || databaseKey.equalsIgnoreCase("MonitoringDefault")) {
				databaseKey = stDbKey;
				logMessage("No database key has been provided. Using default DB: " + databaseKey);
			}
			long user = getActiveAgent().getId();
			SQLDatabaseManager databaseManager = databaseManagerMap.get(user);

			if(databaseManager == null) {
				throw new Exception("Did not provide a valid databaseManager!");
			}

			SQLDatabase sqlDatabase = databaseManager.getDatabaseInstance(databaseKey);
			if(sqlDatabase == null) {
				throw new Exception("Failed to get an instance of the desired database!");
			}

			ResultSet resultSet = sqlDatabase.executeQuery(sqlQuery);
			if(resultSet == null) {
				throw new Exception("Failed to get an result set from the desired database!");
			}

			return resultSet;
		} 
		catch (Exception e) {
			logError(e);
			System.out.println(e.getMessage());
			throw new L2pServiceException("Exception in getResultSet", e);
		}
	}

	@GET
	@Path("api-docs")
	@Produces(MediaType.APPLICATION_JSON)
	@Summary("retrieve Swagger 1.2 resource listing.")
	@ApiResponses(value={
			@ApiResponse(code = 200, message = "Swagger 1.2 compliant resource listing"),
			@ApiResponse(code = 404, message = "Swagger resource listing not available due to missing annotations."),
	})
	public HttpResponse getSwaggerResourceListing(){
		return RESTMapper.getSwaggerResourceListing(this.getClass());
	}

	@GET
	@Path("api-docs/{tlr}")
	@Produces(MediaType.APPLICATION_JSON)
	@Summary("retrieve Swagger 1.2 API declaration for given top-level resource.")
	@ApiResponses(value={
			@ApiResponse(code = 200, message = "Swagger 1.2 compliant API declaration"),
			@ApiResponse(code = 404, message = "Swagger API declaration not available due to missing annotations."),
	})
	public HttpResponse getSwaggerApiDeclaration(@PathParam("tlr") String tlr){
		return RESTMapper.getSwaggerApiDeclaration(this.getClass(),tlr, "http://localhost:8080/QVS");
	}

	/**
	 * Simple function to validate a user login.
	 * Basically it only serves as a "calling point" and does not really validate a user
	 * (since this is done previously by LAS2peer itself, the user does not reach this method
	 * if he or she is not authenticated).
	 * 
	 */
	@GET
	@Path("validate")
	public HttpResponse validateLogin() {
		String returnString = "";
		returnString += "You are " + ((UserAgent) getActiveAgent()).getUserData() + " and your login is valid!";
		UserAgent u = (UserAgent) getActiveAgent();

		HttpResponse res = new HttpResponse(returnString);
		res.setStatus(200);
		return res;
	}

	/**
	 * Method for debugging purposes.
	 * Here the concept of restMapping validation is shown.
	 * It is important to check, if all annotations are correct and consistent.
	 * Otherwise the service will not be accessible by the WebConnector.
	 * Best to do it in the unit tests.
	 * To avoid being overlooked/ignored the method is implemented here and not in the test section.
	 * @return  true, if mapping correct
	 */
	public boolean debugMapping() {
		String XML_LOCATION = "./restMapping.xml";
		String xml = getRESTMapping();

		try {
			RESTMapper.writeFile(XML_LOCATION, xml);
		} catch (IOException e) {
			e.printStackTrace();
		}

		XMLCheck validator = new XMLCheck();
		ValidationResult result = validator.validate(xml);

		if (result.isValid())
			return true;
		return false;
	}

	/**
	 * This method is needed for every RESTful application in LAS2peer. There is no need to change!
	 * 
	 * @return the mapping
	 */
	public String getRESTMapping() {
		String result = "";
		try {
			result = RESTMapper.getMethodsAsXML(this.getClass());
		} catch (Exception e) {

			e.printStackTrace();
		}
		return result;
	}

}
