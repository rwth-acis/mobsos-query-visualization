package i5.las2peer.services.mobsos.queryVisualization;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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

import javax.imageio.ImageIO;
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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import gui.ava.html.image.generator.HtmlImageGenerator;
import i5.las2peer.api.Context;
import i5.las2peer.execution.L2pServiceException;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.mobsos.queryVisualization.caching.MethodResultCache;
import i5.las2peer.services.mobsos.queryVisualization.dal.QVDatabase;
import i5.las2peer.services.mobsos.queryVisualization.dal.QVQuery;
import i5.las2peer.services.mobsos.queryVisualization.dal.QVQueryInformation;
import i5.las2peer.services.mobsos.queryVisualization.dal.QVQueryparameter;
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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import net.minidev.json.JSONObject;

/**
 * LAS2peer Service
 * 
 * This is a template for a very basic LAS2peer service that uses the LAS2peer Web-Connector for RESTful access to it.
 * 
 */
@ServicePath("QVS")
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

	public SQLDatabaseSettings databaseSettings = null;
	public SQLDatabase storageDatabase = null;

	public HashMap<Long, SQLDatabaseManager> databaseManagerMap = new HashMap<Long, SQLDatabaseManager>();
	public HashMap<Long, SQLFilterManager> filterManagerMap = new HashMap<Long, SQLFilterManager>();
	public HashMap<Long, QueryManager> queryManagerMap = new HashMap<Long, QueryManager>();

	public MethodResultCache resultCache = null;
	public VisualizationManager visualizationManager = null;
	public VisualizationException visualizationException = null;
	public ModificationManager modificationManager = null;

	public QueryVisualizationService() {
		// read and set properties values
		// IF THE SERVICE CLASS NAME IS CHANGED, THE PROPERTIES FILE NAME NEED
		// TO BE CHANGED TOO!
		setFieldValues();
	}

	public void initializeDBConnection() {
		long user = Context.getCurrent().getMainAgent().getId();
		if (databaseManagerMap.get(user) != null) {
			return;
		}
		try {
			if (stDbHost == null || stDbPort < 1 || stDbDatabase == null || stDbUser == null || stDbPassword == null) {
				L2pLogger.logEvent(this, Event.SERVICE_ERROR,
						"Provided invalid parameters (default database) for the service! Please check you service config file!");
				throw new Exception("No Database Connection values from config file available!");
			}
			if (resultTimeout == null) {
				resultTimeout = "90";
			}

			databaseSettings = new SQLDatabaseSettings(stDbKey, SQLDatabaseType.MYSQL, stDbUser, stDbPassword,
					stDbDatabase, stDbHost, stDbPort);

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
	 * Generates a query on the specified database. The query may contain placeholders that have to be replaced by the
	 * parameters. <br>
	 * This is the main services entry point that should be used to generate and save queries. A placeholder is set by
	 * $placeholder$ and will be replaced by the corresponding element in the query parameters. The query parameter
	 * array has to contain the elements in the same order as they appear in the query.
	 * 
	 * @param query a String containing the query
	 * @param queryParameters the query parameters as an array of Strings that contain the content of the placeholders
	 * @param databaseKey a String containing the database key
	 * @param useCache if true, a cached result is returned (if available) instead of performing the query again (does
	 *            only affect stored queries!)
	 * @param modificationTypeIndex the desired modification function
	 * @param visualizationTypeIndex the desired visualization
	 * @param visualizationParameters an array of additional parameters for the visualization, including title, height
	 *            and weight
	 * @param save if set, the query will be saved and the return statement will be the query id.
	 * 
	 * @return Result of the query in the requested output format or the id of the saved query
	 */
	public String createQueryString(String query, String[] queryParameters, String databaseKey, boolean useCache,
			int modificationTypeIndex, VisualizationType visualizationTypeIndex, String[] visualizationParameters,
			boolean save) {
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
				return saveQuery(query, queryParameters, databaseKey, useCache, modificationTypeIndex,
						visualizationTypeIndex, visualizationParameters);
			}
			methodResult = executeSQLQuery(query, queryParameters, databaseKey, cacheKey);
			Modification modification = modificationManager
					.getModification(ModificationType.fromInt(modificationTypeIndex));
			Visualization visualization = visualizationManager.getVisualization(visualizationTypeIndex);

			if (modification.check(methodResult))
				methodResult = modification.apply(methodResult);
			else
				return visualizationException.generate(new Exception(),
						"Can not modify result with " + modification.getType().name() + ".");

			if (visualization.check(methodResult, visualizationParameters))
				return visualization.generate(methodResult, visualizationParameters);
			else
				return visualizationException.generate(new Exception(),
						"Can not convert result into " + visualization.getType().name() + "-format.");
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			return visualizationException.generate(e, "An Error occured while trying to execute the query!");
		}
	}

	/**
	 * 
	 * Executes a sql query on the specified database. Warning: only a very simple checking mechanism for escape
	 * characters is implemented. Only queries from trusted sources should be executed!
	 * 
	 * @param sqlQuery the query (has to be "ready to execute")
	 * @param databaseKey the key of the database which is to be queried
	 * @param cacheKey the key which should be used to cache the query result (if empty, result is not cached)
	 * 
	 * @return a Method Result
	 */
	private MethodResult executeSQLQuery(String sqlQuery, String[] queryParameters, String databaseKey, String cacheKey)
			throws L2pServiceException, SQLException {
		if (queryParameters != null && queryParameters.length > 0) {
			sqlQuery = Query.insertParameters(sqlQuery, queryParameters);
		}
		ResultSet resultSet = getResultSet(sqlQuery, databaseKey);
		L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_18, Context.getCurrent().getMainAgent(), "" + sqlQuery);
		return transformToMethodResult(resultSet, cacheKey);
	}

	/**
	 * Execute an sql-query and returns the corresponding result set. <br>
	 * The actual database access is done here (by calling sqlDatabase.executeQuery).
	 * 
	 * @param sqlQuery the query
	 * @param databaseKey the key of the database
	 * 
	 * @return ResultSet of the database query
	 * @throws LASException
	 */
	private ResultSet getResultSet(String sqlQuery, String databaseKey) throws L2pServiceException, SQLException {
		try {
			if (databaseKey == null || databaseKey.isEmpty() || databaseKey.equalsIgnoreCase("undefined")
					|| databaseKey.equalsIgnoreCase("MonitoringDefault")) {
				databaseKey = stDbKey;
				// logMessage("No database key has been provided. Using
				// default
				// DB: " + databaseKey);
			}
			long user = Context.getCurrent().getMainAgent().getId();
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
						// logMessage("Invalid SQL Datatype for column: " +
						// i +
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
							L2pLogger.logEvent(this, Event.SERVICE_ERROR,
									"Unknown SQL Datatype: " + columnTypes[i - 1].toString());
							try {
								currentRow[i - 1] = resultSet.getObject(i).toString();
							} catch (Exception e) {
								currentRow[i - 1] = null;
							}
							break;
						}
						;

						// Note: this is a little DANGEROUS because it does
						// not
						// match the
						// column datatype. BUT: toString() works on it
						if (currentRow[i - 1] == null) {
							currentRow[i - 1] = "";
						}
					}
					methodResult.addRow(currentRow);
				}
			} catch (SQLException e) {
				L2pLogger.logEvent(this, Event.SERVICE_ERROR,
						"SQL exception when trying to handle an SQL query result: " + e.getMessage());
			} catch (Exception e) {
				L2pLogger.logEvent(this, Event.SERVICE_ERROR,
						"Exception when trying to handle an SQL query result: " + e.getMessage());
			}

			// since the values are now in the method result the result set
			// can
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
	 * Saves a query. <br>
	 * 
	 * @param queryStatement a String containing the query
	 * @param databaseKey the key to the database
	 * @param useCache if true, a cached result is returned (if available) instead of performing the query again
	 * @param modificationTypeIndex the desired modification function
	 * @param visualizationTypeIndex the desired visualization
	 * @param visualizationParamaters an array of additional parameters for the visualization, including title, height
	 *            and weight
	 * 
	 * @return The id of the saved query as a String
	 */
	private String saveQuery(String queryStatement, String[] queryParameters, String databaseKey, boolean useCache,
			int modificationTypeIndex, VisualizationType visualizationTypeIndex, String[] visualizationParamaters) {

		SQLDatabase database = null;
		Query query = null;
		String queryKey = ""; // If empty, the query generates a new one
		try {
			long user = Context.getCurrent().getMainAgent().getId();
			SQLDatabaseManager databaseManager = databaseManagerMap.get(user);
			QueryManager queryManager = queryManagerMap.get(user);
			database = databaseManager.getDatabaseInstance(databaseKey);
			query = new Query(Context.getCurrent().getMainAgent().getId(), database.getJdbcInfo(), database.getUser(),
					database.getPassword(), databaseKey, database.getDatabase(), database.getHost(), database.getPort(),
					queryStatement, queryParameters, useCache, modificationTypeIndex, visualizationTypeIndex,
					visualizationParamaters, queryKey);
			queryManager.storeQuery(query);
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			return VisualizationException.getInstance().generate(e, "An error occured while trying to save a Query!");
		}

		L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_17, Context.getCurrent().getMainAgent(),
				"" + query.getQueryStatement());
		return query.getKey();
	}

	/**
	 * Executes a given query.
	 * 
	 * @param query the query
	 * 
	 * @return Result a visualization of the query
	 */

	private String visualizeQuery(Query query, String[] queryParameters, String format) throws Exception {
		MethodResult methodResult = null;
		if (query.usesCache() && queryParameters == null) {
			methodResult = resultCache.get(query.getKey());
		}
		if (methodResult == null) { // query was not cached or no cached
									// result
									// desired
			SQLDatabase sqlDatabase = new SQLDatabase(query);

			sqlDatabase.connect();
			ResultSet resultSet = sqlDatabase.executeQuery(query.getInsertedQueryStatement(queryParameters));
			if (resultSet == null) {
				return visualizationException.generate(new Exception(),
						"Failed to get a result set from the desired database!");
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

		Modification modification = modificationManager
				.getModification(ModificationType.fromInt(query.getModificationTypeIndex()));

		Visualization visualization = visualizationManager.getVisualization(query.getVisualizationTypeIndex());
		if (format.length() > 0 && !format.equals("PNG")) {
			visualization = visualizationManager.getVisualization(VisualizationType.valueOf(format));
		}
		if (modification.check(methodResult))
			methodResult = modification.apply(methodResult);
		else
			return visualizationException.generate(new Exception(),
					"Can not modify result with " + modification.getType().name() + ".");

		if (visualization.check(methodResult, query.getVisualizationParameters())) {
			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_16, Context.getCurrent().getMainAgent(), "" + query);
			return visualization.generate(methodResult, query.getVisualizationParameters());
		} else
			return visualizationException.generate(new Exception(),
					"Can not convert result into " + visualization.getType().name() + "-format.");
	}

	@Override
	protected void initResources() {
		getResourceConfig().register(Resource.class);
	}

	// //////////////////////////////////////////////////////////////////////////////////////
	// Service methods.
	// //////////////////////////////////////////////////////////////////////////////////////
	@Api(
			value = "/")
	@SwaggerDefinition(
			info = @Info(
					title = "Query Visualization Service",
					version = "0.6.7",
					description = "This service can be used to visualize queries on RDB's",
					termsOfService = "http://las2peer.dbis.rwth-aachen.de/qv-service",
					contact = @Contact(
							name = "Alexander Neumann",
							url = "https://github.com/rwth-acis/mobsos-query-visualization",
							email = "neumann@dbis.rwth-aachen.de"),
					license = @License(
							name = "BSD",
							url = "https://raw.githubusercontent.com/rwth-acis/mobsos-query-visualization/master/LICENSE")))
	@Path("/") // this is the root resource
	public static class Resource {
		private QueryVisualizationService service = (QueryVisualizationService) Context.getCurrent().getService();

		/**
		 * Adds a sql database to the users available/usable databases (via the sqldatabase manager).
		 * 
		 * @param databaseKey key which is later used to identify the database
		 * @param db Credentials for the database
		 * 
		 * @return success or error message, if possible in the requested encoding/format
		 */
		@PUT
		@Path("database/{key}")
		@Consumes(MediaType.APPLICATION_JSON)
		@Produces(MediaType.APPLICATION_JSON)
		@ApiOperation(
				value = "This method adds a sql database to the users available/usable databases.")
		@ApiResponses(
				value = { @ApiResponse(
						code = 201,
						message = "Database added successfully."),
						@ApiResponse(
								code = 400,
								message = "Database data invalid.") })
		public Response addDatabase(@ApiParam(
				value = "Key of the database.",
				required = true) @PathParam("key") String databaseKey,
				@ApiParam(
						value = "Database information.",
						required = true) QVDatabase db) {
			try {
				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_1, Context.getCurrent().getMainAgent(),
						"" + db.getDb_code());
				return addDatabase(databaseKey, SQLDatabaseType.valueOf(db.getDb_code().toUpperCase()),
						db.getUsername(), db.getPassword(), db.getDatabase(), db.getDbhost(), db.getPort(),
						VisualizationType.JSON);
			} catch (Exception e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST)
						.entity(service.visualizationException.generate(e, "Received invalid JSON")).build();
			}
		}

		/**
		 * Adds a sql database to the users available/usable databases (via the sqldatabase manager).
		 * 
		 * @param databaseKey key which is later used to identify the database
		 * @param databaseTypeCode type of the database (DB2, MySql, ...)
		 * @param username the username for the database
		 * @param password the password for the database
		 * @param database the name of the database
		 * @param host the database address
		 * @param port the database port
		 * @param visualizationTypeIndex encoding of the returned message
		 * 
		 * @return success or error message, if possible in the requested encoding/format
		 */
		public Response addDatabase(String databaseKey, SQLDatabaseType databaseTypeCode, String username,
				String password, String database, String host, Integer port, VisualizationType visualizationTypeIndex) {
			try {
				if (databaseKey == null || databaseKey.length() < 2) {
					throw new Exception("Databasekey is too short (Use at least 2 characters).");
				}

				service.initializeDBConnection();
				long user = Context.getCurrent().getMainAgent().getId();
				SQLDatabaseManager databaseManager = service.databaseManagerMap.get(user);

				SQLDatabaseType sqlDatabaseType = databaseTypeCode;
				if (!databaseManager.addDatabase(databaseKey, sqlDatabaseType, username, password, database, host,
						port)) {
					throw new Exception("Failed to add a database for the user!");
				}

				// verify that it works (that one can get an instance, probably
				// its
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
				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_2, Context.getCurrent().getMainAgent(),
						"" + databaseKey);
				return Response.status(Status.CREATED).entity(
						service.visualizationManager.getVisualization(visualizationTypeIndex).generate(result, null))
						.build();
			} catch (Exception e) {
				return Response.status(Status.BAD_REQUEST).entity(service.visualizationException.generate(e, null))
						.build();
			}
		}

		/**
		 * Removes a database from the user's list of configured databases (so that the user can not access the database
		 * anymore). Other user's database settings are not changed.
		 * 
		 * @param databaseKey the key of the database
		 * @return success or error message, if possible in the requested encoding/format
		 */
		@DELETE
		@Path("database/{key}")
		@ApiOperation(
				value = "Removes a database from the user's list of configured databases.")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(
				value = { @ApiResponse(
						code = 200,
						message = "Database removed successfully."),
						@ApiResponse(
								code = 400,
								message = "Database removal failed."),
						@ApiResponse(
								code = 404,
								message = "Database not found.") })
		public Response removeDatabase(@ApiParam(
				value = "Key of the database.",
				required = true) @PathParam("key") String databaseKey) {
			try {
				if (databaseKey.equalsIgnoreCase("MonitoringDefault")) {
					databaseKey = service.stDbKey;
				}

				service.initializeDBConnection();

				long user = Context.getCurrent().getMainAgent().getId();
				SQLDatabaseManager databaseManager = service.databaseManagerMap.get(user);
				QueryManager queryManager = service.queryManagerMap.get(user);
				SQLFilterManager filterManager = service.filterManagerMap.get(user);

				try {
					queryManager.databaseDeleted(databaseKey);
					filterManager.databaseDeleted(databaseKey);
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}

				if (!databaseManager.removeDatabase(databaseKey)) {
					return Response.status(Status.NOT_FOUND).entity("Database " + databaseKey + " does not exist!")
							.build();
				}

				MethodResult result = new MethodResult();
				result.setColumnName("RemovedDatabase");
				result.setColumnDatatype(Types.VARCHAR);
				Object[] defaultDatabase = { databaseKey };
				result.addRow(defaultDatabase);

				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_3, Context.getCurrent().getMainAgent(),
						"" + databaseKey);
				return Response.status(Status.OK).entity(
						service.visualizationManager.getVisualization(VisualizationType.JSON).generate(result, null))
						.build();
			} catch (Exception e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST).entity(service.visualizationException.generate(e, null))
						.build();
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
		@Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
		@ApiOperation(
				value = "This method returns a list/table of the keys of all available/configured databases of the user.")
		@ApiResponses(
				value = { @ApiResponse(
						code = 200,
						message = "Got Database keys.",
						response = String[].class,
						responseContainer = "List"),
						@ApiResponse(
								code = 400,
								message = "Retrieving keys failed.") })

		public Response getDatabaseKeys(@ApiParam(
				value = "Visualization format index",
				required = false,
				allowableValues = "JSON,HTMLTABLE,CSV,XML") @QueryParam("format") @DefaultValue("JSON") String visualizationTypeIndex) {
			try {
				service.initializeDBConnection();
				long user = Context.getCurrent().getMainAgent().getId();
				SQLDatabaseManager databaseManager = service.databaseManagerMap.get(user);
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
						currentDatabaseKey[0] = service.stDbKey;
					}

					result.addRow(currentDatabaseKey);
				}

				VisualizationType t = VisualizationType.valueOf(visualizationTypeIndex.toUpperCase());
				Visualization vis = service.visualizationManager.getVisualization(t);
				String visString = vis.generate(result, null);
				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_4, Context.getCurrent().getMainAgent(),
						"Get Database Keys");
				return Response.status(Status.OK).entity(visString).build();
			} catch (Exception e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST).entity(service.visualizationException.generate(e, null))
						.build();
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
		@Produces(MediaType.APPLICATION_JSON)
		@ApiOperation(
				value = "This method returns a list/table of all filters the user has configured.",
				notes = "Array containing filter keys and database keys")
		@ApiResponses(
				value = { @ApiResponse(
						code = 200,
						message = "Got filter keys.",
						response = String[].class,
						responseContainer = "List"),
						@ApiResponse(
								code = 400,
								message = "Retrieving filter keys failed.") })
		public Response getFilterKeys(@ApiParam(
				value = "Visualization type.",
				required = true) @QueryParam("format") @DefaultValue("JSON") String visualizationTypeIndex) {
			try {
				service.initializeDBConnection();

				long user = Context.getCurrent().getMainAgent().getId();
				SQLFilterManager filterManager = service.filterManagerMap.get(user);
				if (filterManager == null) {
					// initialize filter manager
					filterManager = new SQLFilterManager(service.storageDatabase);
					service.filterManagerMap.put(user, filterManager);
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
				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_5, Context.getCurrent().getMainAgent(), "Get Filter.");
				return Response.status(Status.OK)
						.entity(service.visualizationManager.getVisualization(vtypei).generate(result, null)).build();
			} catch (Exception e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST).entity(service.visualizationException.generate(e, null))
						.build();
			}
		}

		/**
		 * Retrieves the values for a specific filter of the current user.
		 * 
		 * @param filterKey Key for the filter
		 * @param visualizationTypeIndex encoding of the returned message
		 * @param dbKey Key for database
		 * @return success or error message, if possible in the requested encoding/format
		 */
		@GET
		@Path("filter/{database}/{key}")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiOperation(
				value = "Retrieves the values for a specific filter of the current user.")
		@ApiResponses(
				value = { @ApiResponse(
						code = 200,
						message = "Got filter values.",
						response = String[].class,
						responseContainer = "List"),
						@ApiResponse(
								code = 400,
								message = "Retrieving filter keys failed."),
						@ApiResponse(
								code = 404,
								message = "Filter does not exist.") })
		public Response getFilterValuesForCurrentUser(@ApiParam(
				value = "Key of the database.",
				required = true) @PathParam("database") String dbKey,
				@ApiParam(
						value = "Key of the filter.",
						required = true) @PathParam("key") String filterKey,
				@ApiParam(
						value = "Visualization type.") @QueryParam("format") @DefaultValue("JSON") String visualizationTypeIndex) {
			return getFilterValuesOfUser(dbKey, filterKey, Context.getCurrent().getMainAgent().getId(),
					visualizationTypeIndex);
		}

		/**
		 * Retrieves the filter keys for a specific query by ID.
		 * 
		 * @param queryKey Key of the query
		 * @return success or error message, if possible in the requested encoding/format
		 */
		@GET
		@Path("query/{query}/filter")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiOperation(
				value = "Retrieves the filter keys for a specific query by ID.")
		@ApiResponses(
				value = { @ApiResponse(
						code = 200,
						message = "Got filters."),
						@ApiResponse(
								code = 400,
								message = "Retrieving filter keys failed.",
								response = String[].class,
								responseContainer = "List"),
						@ApiResponse(
								code = 204,
								message = "Filter does not exist.") })
		public Response getFilterValuesForQuery(@ApiParam(
				value = "Key of the query",
				required = true) @PathParam("query") String queryKey) {
			try {
				service.initializeDBConnection();
				QueryManager queryManager = service.queryManagerMap.get(Context.getCurrent().getMainAgent().getId());
				Query q = queryManager.getQuery(queryKey);
				String[] params = q.getQueryParameters();
				String statement = q.getQueryStatement();

				ArrayList<String> filterNames = new ArrayList<String>();

				// go through the query, replace placeholders by the values from
				// the
				// query parameters
				int parameterCount = params == null ? 0 : params.length;
				Pattern placeholderPattern = Pattern.compile("\\$\\$.*?\\$\\$}");
				Matcher m = placeholderPattern.matcher(statement);
				for (int i = 0; i < parameterCount; i++) {
					m.find();
					String param = m.group();
					filterNames.add(param.substring(1, param.length() - 1));
				}
				String dbKey = q.getDatabaseKey();
				long user = q.getUser();

				SQLDatabaseManager dbManager = service.databaseManagerMap.get(user);
				if (dbManager == null) {
					dbManager = new SQLDatabaseManager(service, service.storageDatabase);
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
				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_6, Context.getCurrent().getMainAgent(),
						"Get Filter for " + queryKey);
				return Response.status(Status.OK).entity(
						service.visualizationManager.getVisualization(VisualizationType.JSON).generate(result, null))
						.build();
			} catch (DoesNotExistException e) {
				return Response.status(Status.NOT_FOUND).entity("Query " + queryKey + " does not exist.").build();
			} catch (Exception e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST).entity(service.visualizationException.generate(e, null))
						.build();
			}
		}

		/**
		 * Retrieves the values for a specific filter of a chosen user.
		 * 
		 * @param dbKey Key for database
		 * @param filterKey Key for the filter
		 * @param visualizationTypeIndex encoding of the returned message
		 * @param user AgentID of the user
		 * @return success or error message, if possible in the requested encoding/format
		 */
		@GET
		@Path("filter/{database}/{key}/{user}")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiOperation(
				value = "Retrieves the values for a specific filter of a chosen user.")
		@ApiResponses(
				value = { @ApiResponse(
						code = 200,
						message = "Got filter values.",
						response = String[].class,
						responseContainer = "List"),
						@ApiResponse(
								code = 400,
								message = "Retrieving filter keys failed."),
						@ApiResponse(
								code = 204,
								message = "Filter does not exist.") })

		public Response getFilterValuesOfUser(@ApiParam(
				value = "Key of the database.",
				required = true) @PathParam("database") String dbKey,
				@ApiParam(
						value = "Key of the filter.",
						required = true) @PathParam("key") String filterKey,
				@ApiParam(
						value = "Agent id of the user.",
						required = true) @PathParam("user") long user,
				@ApiParam(
						value = "Visualization type.") @QueryParam("format") @DefaultValue("JSON") String visualizationTypeIndex) {
			try {
				VisualizationType vtypei = VisualizationType.valueOf(visualizationTypeIndex.toUpperCase());
				service.initializeDBConnection();
				SQLFilterManager filterManager = service.filterManagerMap.get(user);
				if (filterManager == null) {
					if (((UserAgent) Context.getCurrent().getMainAgent()).getUserData() == null) {
						filterManager = new SQLFilterManager(service.storageDatabase, user);
						// throw new DoesNotExistException("Anonymous user
						// requested
						// non-existant filter");
					} else {
						// initialize filter manager
						filterManager = new SQLFilterManager(service.storageDatabase);
						service.filterManagerMap.put(user, filterManager);
					}
				}
				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_7, Context.getCurrent().getMainAgent(),
						"Get values for Filter " + filterKey + ", user " + user);
				return Response.status(Status.OK)
						.entity(filterManager.getFilterValues(dbKey, filterKey, vtypei, service)).build();
			} catch (DoesNotExistException e) {
				return Response.status(Status.NOT_FOUND).entity("Filter " + filterKey + " does not exist.").build();
			} catch (Exception e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST).entity(service.visualizationException.generate(e, null))
						.build();

			}
		}

		/**
		 * Adds a filter to the user's settings/profile.
		 * 
		 * @param dbKey key of the database for which the filter has been configured
		 * @param filterName the Key that should be used for this filter
		 * @param query SQL query
		 * 
		 * @return success or error message, if possible in the requested encoding/format
		 */
		@PUT
		@Path("filter/{database}/{key}")
		@Produces(MediaType.APPLICATION_JSON)
		@Consumes(MediaType.APPLICATION_JSON)
		@ApiOperation(
				value = "Adds a filter to the user's settings/profile.")
		@ApiResponses(
				value = { @ApiResponse(
						code = 201,
						message = "Added filter.",
						response = String[].class,
						responseContainer = "List"),
						@ApiResponse(
								code = 400,
								message = "Adding filter failed.") })
		public Response addFilter(@ApiParam(
				value = "Key of the database.",
				required = true) @PathParam("database") String dbKey,
				@ApiParam(
						value = "Key that should be used for the filter",
						required = true) @PathParam("key") String filterName,
				@ApiParam(
						value = "JSON containing the query.",
						required = true) QVQuery query) {
			try {
				VisualizationType vtypei = VisualizationType.JSON;
				return addFilter(dbKey, filterName, query.getQuery(), vtypei);
			} catch (Exception e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST)
						.entity(service.visualizationException.generate(e, "Received invalid JSON")).build();
			}
		}

		public Response addFilter(String databaseKey, String filterKey, String sqlQuery,
				VisualizationType visualizationTypeIndex) {
			try {
				service.initializeDBConnection();
				// TODO: parameter sanity checks
				long user = Context.getCurrent().getMainAgent().getId();
				SQLFilterManager filterManager = service.filterManagerMap.get(user);

				if (filterManager == null) {
					// initialize filter manager
					filterManager = new SQLFilterManager(service.storageDatabase);
					service.filterManagerMap.put(user, filterManager);
				}

				if (!filterManager.addFilter(databaseKey, filterKey, sqlQuery)) {
					throw new Exception("Failed to add a database for the user!");
				}

				// verify that it works (that one can get an instance, probably
				// its
				// going to be used later anyways)...
				try {
					if (filterManager.getFilterValues(databaseKey, filterKey, visualizationTypeIndex,
							service) == null) {
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

				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_8, Context.getCurrent().getMainAgent(), "" + user);
				return Response.status(Status.CREATED).entity(
						service.visualizationManager.getVisualization(visualizationTypeIndex).generate(result, null))
						.build();
			} catch (Exception e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST).entity(service.visualizationException.generate(e, null))
						.build();
			}
		}

		/**
		 * Deletes a filter from the user's settings/profile.
		 * 
		 * @param dbKey the key of the database
		 * @param filterKey the key of the filter
		 * @return success or error message, if possible in the requested encoding/format
		 */
		@DELETE
		@Path("filter/{database}/{key}")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiOperation(
				value = "Deletes a filter from the user's settings/profile.")
		@ApiResponses(
				value = { @ApiResponse(
						code = 200,
						message = "Deleted filter.",
						response = String[].class,
						responseContainer = "List"),
						@ApiResponse(
								code = 400,
								message = "Deleting filter failed.") })
		public Response deleteFilter(@ApiParam(
				value = "Key of the database.",
				required = true) @PathParam("database") String dbKey,
				@ApiParam(
						value = "Key of the filter.",
						required = true) @PathParam("key") String filterKey) {
			try {
				service.initializeDBConnection();
				long user = Context.getCurrent().getMainAgent().getId();
				SQLFilterManager filterManager = service.filterManagerMap.get(user);
				if (filterManager == null) {
					// initialize filter manager
					filterManager = new SQLFilterManager(service.storageDatabase);
					service.filterManagerMap.put(user, filterManager);
				}

				if (!filterManager.deleteFilter(dbKey, filterKey)) {
					return Response.status(Status.NOT_FOUND).entity("Filter " + filterKey + " does not exist!").build();
				}

				MethodResult result = new MethodResult();
				result.setColumnName("DeletedFilter");
				result.setColumnDatatype(Types.VARCHAR);
				Object[] deletedDatabase = { filterKey };
				result.addRow(deletedDatabase);

				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_9, Context.getCurrent().getMainAgent(), "" + user);
				return Response.status(Status.OK).entity(
						service.visualizationManager.getVisualization(VisualizationType.JSON).generate(result, null))
						.build();
			} catch (Exception e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST).entity(service.visualizationException.generate(e, null))
						.build();
			}
		}

		@POST
		@Path("query/visualize")
		@Produces({ MediaType.TEXT_HTML, "image/png" })
		@Consumes(MediaType.APPLICATION_JSON)
		@ApiOperation(
				value = "Executes a query and returns the chosen visualization.")
		@ApiResponses(
				value = { @ApiResponse(
						code = 200,
						message = "Created query.",
						response = String.class),
						@ApiResponse(
								code = 400,
								message = "Creating Query failed.") })
		public Response visualizeQuery(@ApiParam(
				value = "Visualization type.") @QueryParam("format") @DefaultValue("JSON") String vtypei,
				@ApiParam(
						value = "Query information.",
						required = true) QVQueryInformation content) {
			try {
				VisualizationType v = VisualizationType.valueOf(vtypei.toUpperCase());
				Response res = createQuery(content.getQuery(), content.getQueryparams(), content.getDbkey(),
						content.isCache(), content.getModtypei(), v, content.getTitle(), content.getWidth(),
						content.getHeight(), false);

				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_10, Context.getCurrent().getMainAgent(), "" + vtypei);
				return res;
			} catch (Exception e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST)
						.entity(service.visualizationException.generate(e, "Received invalid JSON")).build();
			}
		}

		@POST
		@Path("query")
		@Produces(MediaType.APPLICATION_JSON)
		@Consumes(MediaType.APPLICATION_JSON)
		@ApiOperation(
				value = "Executes a query.")
		@ApiResponses(
				value = { @ApiResponse(
						code = 201,
						message = "Created query.",
						response = String[].class,
						responseContainer = "List"),
						@ApiResponse(
								code = 400,
								message = "Creating Query failed.") })
		public Response createQuery(@ApiParam(
				value = "Visualization type.") @QueryParam("format") @DefaultValue("JSON") String vtypei,
				@ApiParam(
						value = "Query information.") QVQueryInformation content) {
			try {
				VisualizationType v = VisualizationType.valueOf(vtypei.toUpperCase());
				Response res = createQuery(content.getQuery(), content.getQueryparams(), content.getDbkey(),
						content.isCache(), content.getModtypei(), v, content.getTitle(), content.getWidth(),
						content.getHeight(), true);

				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_11, Context.getCurrent().getMainAgent(),
						"" + content.getQuery());
				return res;
			} catch (Exception e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST)
						.entity(service.visualizationException.generate(e, "Received invalid JSON")).build();

			}
		}

		public Response createQuery(String query, String[] queryParameters, String databaseKey, boolean useCache,
				int modificationTypeIndex, VisualizationType visualizationTypeIndex, String title, String width,
				String height, boolean save) {
			String queryString = service.createQueryString(query, queryParameters, databaseKey, useCache,
					modificationTypeIndex, visualizationTypeIndex, new String[] { title, height, width }, save);
			if (queryString.startsWith("The Query has lead to an error.")) {
				return Response.status(Status.BAD_REQUEST).entity(queryString).build();
			} else {
				return Response.status(Status.CREATED).entity(queryString).build();
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
		@Produces(MediaType.APPLICATION_JSON)
		@ApiOperation(
				value = "Returns a list/table of the keys of all available/configured databases of the user.")
		@ApiResponses(
				value = { @ApiResponse(
						code = 200,
						message = "Got Database queries.",
						response = String[].class,
						responseContainer = "List"),
						@ApiResponse(
								code = 400,
								message = "Retrieving queries failed.") })
		public Response getQueryKeys(@ApiParam(
				value = "Visualization type.") @QueryParam("format") @DefaultValue("JSON") String visualizationTypeIndex) {
			try {
				service.initializeDBConnection();
				long user = Context.getCurrent().getMainAgent().getId();
				QueryManager queryManager = service.queryManagerMap.get(user);
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

				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_12, Context.getCurrent().getMainAgent(), "" + user);
				return Response.status(Status.OK)
						.entity(service.visualizationManager
								.getVisualization(VisualizationType.valueOf(visualizationTypeIndex.toUpperCase()))
								.generate(result, null))
						.build();
			} catch (Exception e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST).entity(service.visualizationException.generate(e, null))
						.build();
			}
		}

		/**
		 * Deletes a filter from the user's settings/profile.
		 * 
		 * @param queryKey the key of the query
		 * @return success or error message, if possible in the requested encoding/format
		 */
		@DELETE
		@Path("query/{key}")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiOperation(
				value = "Deletes a filter from the user's settings/profile.")
		@ApiResponses(
				value = { @ApiResponse(
						code = 200,
						message = "Deleted query.",
						response = String[].class,
						responseContainer = "List"),
						@ApiResponse(
								code = 400,
								message = "Deleting query failed.") })
		public Response deleteQuery(@ApiParam(
				value = "Key of the query.") @PathParam("key") String queryKey) {
			try {
				service.initializeDBConnection();
				long user = Context.getCurrent().getMainAgent().getId();
				QueryManager queryManager = service.queryManagerMap.get(user);
				if (queryManager == null) {
					throw new Exception("Query Manager is null");
				}

				queryManager.removeQ(queryKey);

				MethodResult result = new MethodResult();
				result.setColumnName("DeletedQuery");
				result.setColumnDatatype(Types.VARCHAR);
				Object[] deletedQuery = { queryKey };
				result.addRow(deletedQuery);

				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_13, Context.getCurrent().getMainAgent(), "" + user);
				return Response.status(Status.OK).entity(
						service.visualizationManager.getVisualization(VisualizationType.JSON).generate(result, null))
						.build();
			} catch (Exception e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST).entity(service.visualizationException.generate(e, null))
						.build();
			}
		}

		/**
		 * Executes a stored query on the specified database. <br>
		 * This is the main services entry point that should be used to visualize saved queries.
		 * 
		 * @param key a String that contains the id of the query
		 * @param format currently not used, default JSON
		 * 
		 * @return Result of the query in the given output format
		 */
		@GET
		@Path("query/{key}")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiOperation(
				value = "Executes a stored query on the specified database.")
		@ApiResponses(
				value = { @ApiResponse(
						code = 200,
						message = "Visualization created.",
						response = String[].class,
						responseContainer = "List"),
						@ApiResponse(
								code = 400,
								message = "Creating visualization failed."),
						@ApiResponse(
								code = 404,
								message = "Didn't find requested query.") })
		public Response getQueryValues(@ApiParam(
				value = "Key of the query.") @PathParam("key") String key,
				@ApiParam(
						value = "Visualization type.") @QueryParam("format") @DefaultValue("JSON") String format) {
			service.initializeDBConnection();

			try {
				// VisualizationType vtypei =
				// VisualizationType.valueOf(format.toUpperCase());
				service.initializeDBConnection();
				long user = Context.getCurrent().getMainAgent().getId();
				QueryManager queryManager = service.queryManagerMap.get(user);
				if (queryManager == null) {
					// initialize query manager
					queryManager = new QueryManager(service, service.storageDatabase);
				}
				Query q = queryManager.getQuery(key);
				if (q == null) {
					throw new DoesNotExistException("Query does not exist.");
				}
				JSONObject o = queryManager.toJSON(q);

				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_14, Context.getCurrent().getMainAgent(), "" + user);
				return Response.status(Status.OK).entity(o.toJSONString()).build();
			} catch (DoesNotExistException e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.NOT_FOUND).entity("Query " + key + " does not exist.").build();
			} catch (DBDoesNotExistException e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				long user = Context.getCurrent().getMainAgent().getId();
				QueryManager queryManager = service.queryManagerMap.get(user);
				queryManager.removeQ(key);
				return Response.status(Status.BAD_REQUEST).entity(service.visualizationException.generate(e, null))
						.build();
			} catch (Exception e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST).entity(service.visualizationException.generate(e, null))
						.build();
			}
		}

		/**
		 * Executes a stored query on the specified database. <br>
		 * This is the main services entry point that should be used to visualize saved queries.
		 * 
		 * @param key a String that contains the id of the query
		 * @param format currently not used
		 * @param content containing the query parameters
		 * 
		 * @return Result of the query in the given output format
		 */
		@POST
		@Consumes(MediaType.APPLICATION_JSON)
		@Path("query/{key}/visualize")
		@ApiOperation(
				value = "Executes a stored query on the specified database.")
		@ApiResponses(
				value = { @ApiResponse(
						code = 200,
						message = "Visualization generated."),
						@ApiResponse(
								code = 400,
								message = "Creating visualization failed."),
						@ApiResponse(
								code = 404,
								message = "Didn't find requested query.") })
		public Response visualizeQueryByKeyWithValues(@ApiParam(
				value = "Key of the query") @PathParam("key") String key,
				@ApiParam(
						value = "Visualization type.") @QueryParam("format") @DefaultValue("") String format,
				@ApiParam(
						value = "List of query parameters.") QVQueryparameter content) {
			return visualizeQueryByKey(key, format, content);
		}

		/**
		 * Executes a stored query on the specified database. <br>
		 * This is the main services entry point that should be used to visualize saved queries.
		 * 
		 * @param key a String that contains the id of the query
		 * @param format currently not used
		 * @param content containing the query parameters
		 * @return Result of the query in the given output format
		 */
		@GET
		@Consumes(MediaType.APPLICATION_JSON)
		@Path("query/{key}/visualize")
		@ApiOperation(
				value = "Executes a stored query on the specified database.")
		@ApiResponses(
				value = { @ApiResponse(
						code = 200,
						message = "Visualization created."),
						@ApiResponse(
								code = 400,
								message = "Creating visualization failed."),
						@ApiResponse(
								code = 404,
								message = "Didn't find requested query.") })
		public Response visualizeQueryByKey(@ApiParam(
				value = "Key of the query.") @PathParam("key") String key,
				@ApiParam(
						value = "Visualization type.") @QueryParam("format") @DefaultValue("") String format,
				@ApiParam(
						value = "List of the query parameters.") QVQueryparameter content) {
			service.initializeDBConnection();

			Query query = null;
			String[] queryParameters = null;
			try {
				long user = Context.getCurrent().getMainAgent().getId();
				try {
					queryParameters = content.getQueryparams();
				} catch (Exception e) {
					// Use default filters
					queryParameters = null;
				}

				QueryManager queryManager = service.queryManagerMap.get(user);
				query = queryManager.getQuery(key);
				if (query == null) {
					throw new DoesNotExistException("Query does not exist!");
				}
			} catch (DoesNotExistException e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.NOT_FOUND).entity("Query " + key + " does not exist").build();
			} catch (Exception e) {
				return Response.status(Status.BAD_REQUEST).entity(service.visualizationException.generate(e,
						"Encountered a problem while trying to fetch stored query " + key)).build();
			}

			try {
				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_15, Context.getCurrent().getMainAgent(), "" + query);
				String res = service.visualizeQuery(query, queryParameters, format.toUpperCase());
				if (format.equals("PNG")) {
					HtmlImageGenerator imageGenerator = new HtmlImageGenerator();
					imageGenerator.loadHtml(res);
					BufferedImage image = imageGenerator.getBufferedImage();
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ImageIO.write(image, "png", baos);
					byte[] imageData = baos.toByteArray();
					ResponseBuilder responseBuilder = Response.ok(imageData);
					responseBuilder.header(HttpHeaders.CONTENT_TYPE, "image/png");
					return responseBuilder.build();
				}
				return Response.status(Status.OK).entity(res).build();
			} catch (Exception e) {
				L2pLogger.logEvent(service, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST).entity(service.visualizationException.generate(e,
						"Encountered a Problem while trying to visualize Query!")).build();
			}
		}

		/**
		 * Simple function to validate a user login. Basically it only serves as a "calling point" and does not really
		 * validate a user (since this is done previously by LAS2peer itself, the user does not reach this method if he
		 * or she is not authenticated).
		 * 
		 * @return status of login
		 */
		@GET
		@Path("validate")
		@ApiOperation(
				value = "Validates a user login.")
		public Response validateLogin() {
			String returnString = "";
			returnString += "You are " + ((UserAgent) Context.getCurrent().getMainAgent()).getUserData()
					+ " and your login is valid!";

			return Response.status(Status.OK).entity(returnString).build();
		}
	}
}
