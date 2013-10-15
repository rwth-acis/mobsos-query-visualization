package i5.las2peer.services.queryVisualization;


import i5.las2peer.api.Service;
import i5.las2peer.execution.L2pServiceException;
import i5.las2peer.p2p.Node;
import i5.las2peer.security.Agent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.services.queryVisualization.caching.MethodResultCache;
import i5.las2peer.services.queryVisualization.database.SQLDatabase;
import i5.las2peer.services.queryVisualization.database.SQLDatabaseManager;
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
import i5.las2peer.tools.TimerThread;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * QueryVisualizationService.java
 *
 * LAS2peer version of the LAS "Query Visualization Service" which provides access to various databases, primarily in order to perform visualizations with the corresponding frontend.
 * It has the ability to save queries and export them.
 * Note that this version lacks the backend graph support of the LAS service.
 * It is based on the mobsosmonitoringstatiticsservice by Dominik Renzel and Malte Behrendt.
 *
 * @author Peter de Lange (lange@dbis.rwth-aachen.de)
 * @author Stephan Erdtmann (erdtmann@dbis.rwth-aachen.de)
 */
public class QueryVisualizationService extends Service {
	
	/*** configuration ***/
	public static final String DEFAULT_DATABASE_KEY = "Default Database";
	public static final String DEFAULT_DB2_HOST = "This value should be replaced automatically by configuration file!"; 
	public static final int DEFAULT_DB2_PORT = 0; //This value should be replaced automatically by configuration file!
	public static final String DEFAULT_DB2_DATABASE = "This value should be replaced automatically by configuration file!"; 
	public static final String DEFAULT_DB2_USER = "This value should be replaced automatically by configuration file!";
	public static final String DEFAULT_DB2_PASSWORD = "This value should be replaced automatically by configuration file!";
	
	public static final String DEFAULT_RESULT_TIMEOUT = "90";
	public static final long DEFAULT_SESSION_TIMEOUT = 3600000; // 1h
	public static final long DEFAULT_TIDY_SLEEPTIME_MS = 300000; // 5 min
	
	protected String databaseKey = DEFAULT_DATABASE_KEY;
	protected String db2Host = DEFAULT_DB2_HOST; 
	protected int db2Port = DEFAULT_DB2_PORT; 
	protected String db2Database = DEFAULT_DB2_DATABASE; 
	protected String db2User = DEFAULT_DB2_USER; 
	protected String db2Password = DEFAULT_DB2_PASSWORD; 
	
	protected String resultTimeout = DEFAULT_RESULT_TIMEOUT;

	protected String defaultDBKey = DEFAULT_DATABASE_KEY;
	
	
	/*** end of configuration ***/
	
	
	
	protected long sessionTimeout = DEFAULT_SESSION_TIMEOUT;
	protected long tidySleepTimeMs = DEFAULT_TIDY_SLEEPTIME_MS;
	
	
	private TimerThread tidyThread;
	
	
	
	/**
	 * Nested Class AgentSession
	 * 
	 * This class provides the session scope for the ported LAS service.
	 * 
	 */
	public class AgentSession {
		
		private SQLDatabaseManager databaseManager = null;
		private SQLFilterManager filterManager = null;
		private QueryManager queryManager = null;
		private MethodResultCache resultCache = null;
		private VisualizationManager visualizationManager = null;
		private VisualizationException visualizationException = null;
		private ModificationManager modificationManager = null;
		private long timeStamp;
		
		/**
		 * create a new session instance
		 * @param agent
		 */
		public AgentSession ( Agent agent) {
			initializeDBConnection();
			touch();
		}
		
		
		
		
		/**
		 * set the timestamp of this object
		 */
		public void touch() {
			timeStamp  = new Date().getTime();
		}
		
		/**
		 * get the age of the last access to this session in milliseconds
		 * 
		 * @return age in milliseconds
		 */
		public long getAgeInMs () {
			return new Date().getTime()-timeStamp;
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
		public String addDatabase(String databaseKey, Integer databaseTypeCode, String username, String password, String database, String host, Integer port, Integer visualizationTypeIndex) {
			try {
				if(databaseKey == null || databaseKey.length() < 3) {
					throw new Exception("Databasekey is too short (Use at least 2 characters).");
				}
				
				SQLDatabaseType sqlDatabaseType = SQLDatabaseType.getSQLDatabaseType(databaseTypeCode);
				if(!this.databaseManager.addDatabase(databaseKey,sqlDatabaseType, username, password, database, host, port)) {
					throw new Exception("Failed to add a database for the user!");
				}
				
				// verify that it works (that one can get an instance, probably its going to be used later anyways)...
				try {
					if(this.databaseManager.getDatabaseInstance(databaseKey) == null) {
						throw new Exception("Failed to get a database instance for " + databaseKey);
					} 
				}
				catch (Exception e) {
					this.databaseManager.removeDatabase(databaseKey);
					throw e;
				}
				
				MethodResult result = new MethodResult();
				result.setColumnName("AddedDatabase");
				result.setColumnDatatype(Types.VARCHAR);
				Object[] defaultDatabase = {databaseKey};
				result.addRow(defaultDatabase);
				
				return visualizationManager.getVisualization(VisualizationType.fromInt(visualizationTypeIndex)).generate(result, null);
			}
			catch (Exception e) {
				logError(e);
				return visualizationException.generate(e, null);
				
			}
		}		

		
		/**
		 * Removes a database from the user's list of configured databases (so that the user can not access the database anymore).
		 * Other user's database settings are not changed.
		 * 
		 * @param databaseKey the key of the database
		 * @param visualizationTypeIndex encoding of the returned message
		 * @return success or error message, if possible in the requested encoding/format
		 */
		public String removeDatabase(String databaseKey, Integer visualizationTypeIndex) {
			try {			
				if(databaseKey.equalsIgnoreCase("MonitoringDefault")) {
					databaseKey = defaultDBKey;
				}
				
				if(!databaseManager.removeDatabase(databaseKey)) {
					throw new Exception("A unknown error occurred!");
				}
				
				MethodResult result = new MethodResult();
				result.setColumnName("RemovedDatabase");
				result.setColumnDatatype(Types.VARCHAR);
				Object[] defaultDatabase = {databaseKey};
				result.addRow(defaultDatabase);

				return visualizationManager.getVisualization(VisualizationType.fromInt(visualizationTypeIndex)).generate(result, null);
			}
			catch (Exception e) {
				logError(e);
				return visualizationException.generate(e, null);
			}
		}
		
		/**
		 * Returns a list/table of the keys of all available/configured databases of the user.
		 * 
		 * @param visualizationTypeIndex encoding of the returned message
		 * @return success or error message, if possible in the requested encoding/format
		 */
		public String getDatabaseKeys(Integer visualizationTypeIndex) {
			try {
				List<String> keyList = this.databaseManager.getDatabaseKeyList();
				
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
						currentDatabaseKey[0] = defaultDBKey;
					}
					
					result.addRow(currentDatabaseKey);
				}
				
				return visualizationManager.getVisualization(VisualizationType.fromInt(visualizationTypeIndex)).generate(result, null);			
			}
			catch (Exception e) {
				logError(e);
				return visualizationException.generate(e, null);
			}
		}
		
		/**
		 * Returns a list/table of all filters the user has configured.
		 * 
		 * @param visualizationTypeIndex encoding of the returned message
		 * @return success or error message, if possible in the requested encoding/format
		 */
		public String getFilterKeys(Integer visualizationTypeIndex) {
			try {
				if(this.filterManager == null) {
					// initialize filter manager
					this.filterManager = new SQLFilterManager();
				}
				
				List<String> keyList = this.filterManager.getFilterKeyList();
				
				if(keyList == null) {
					throw new Exception("Failed to get the key list for the users' filters!");
				}
				
				if(keyList.isEmpty()) {
					// in order to encounter the cold-start...
					// add some examples for the default DB
					this.addFilter("User", "SELECT DISTINCT UID FROM MOBSOS.SESSION", defaultDBKey, visualizationTypeIndex);
					this.addFilter("Community", "SELECT DISTINCT CID FROM MOBSOS.MCONTEXT", defaultDBKey, visualizationTypeIndex);
					this.addFilter("Service", "SELECT DISTINCT SCODE FROM MOBSOS.INVOCATION", defaultDBKey, visualizationTypeIndex);
				
					keyList = this.filterManager.getFilterKeyList();
				}
				
				MethodResult result = new MethodResult();
				Integer[] datatypes = {Types.VARCHAR,Types.VARCHAR};
				result.setColumnDatatypes(datatypes);
				String[] names = {"FilterKeys","DatabaseKeys"};
				result.setColumnNames(names);
				
				Iterator<String> iterator = keyList.iterator();
				while(iterator.hasNext()) {
					String filterKey = iterator.next();
					String databaseKey = this.filterManager.getDatabaseKey(filterKey);
					Object[] currentRow = {filterKey,databaseKey};
					result.addRow(currentRow);
				}
				
				
				return visualizationManager.getVisualization(VisualizationType.fromInt(visualizationTypeIndex)).generate(result, null);
			}
			catch (Exception e) {
				logError(e);
				return visualizationException.generate(e, null);
			}
		}
		
		
		/**
		 * Retrieves the values for a specific filter.
		 * 
		 * @param filterKey
		 * @param visualizationTypeIndex encoding of the returned message
		 * @return success or error message, if possible in the requested encoding/format
		 */
		public String getFilterValues(String filterKey, Integer visualizationTypeIndex) {
			try {
				if(this.filterManager == null) {
					// initialize filter manager
					this.filterManager = new SQLFilterManager();
				}
				
				return this.filterManager.getFilterValues(filterKey, visualizationTypeIndex, this);
			}
			catch (Exception e) {
				logError(e);
				return visualizationException.generate(e, null);
			}
		}		
		
		
		
		
		/**
		 * Adds a filter to the user's settings/profile.
		 * 
		 * @param filterKey the Key that should be used for this filter
		 * @param SQLQuery SQL  query which is used to retrieve the filter values
		 * @param databaseKey	key of the database for which the filter has been configured
		 * @param visualizationTypeIndex  encoding of the returned message
		 * 
		 * @return success or error message, if possible in the requested encoding/format
		 */
		public String addFilter(String filterKey, String SQLQuery, String databaseKey, Integer visualizationTypeIndex) {
			try {
				//TODO: parameter sanity checks
				
				if(this.filterManager == null) {
					// initialize filter manager
					this.filterManager = new SQLFilterManager();
				}
				
				if(!this.filterManager.addFilter(filterKey, SQLQuery, databaseKey)) {
					throw new Exception("Failed to add a database for the user!");
				}
				 
				// verify that it works (that one can get an instance, probably its going to be used later anyways)...
				try {
					if(this.filterManager.getFilterValues(filterKey, visualizationTypeIndex, this) == null) {
						throw new Exception("Failed to retrieve the filter values!");
					}
				}
				catch (Exception e) {
					this.filterManager.deleteFilter(filterKey);
					throw e;
				}
				
				MethodResult result = new MethodResult();
				result.setColumnName("AddedFilter");
				result.setColumnDatatype(Types.VARCHAR);
				Object[] addedFilter = {filterKey};
				result.addRow(addedFilter);
				
				return visualizationManager.getVisualization(VisualizationType.fromInt(visualizationTypeIndex)).generate(result, null);
			}
			catch (Exception e) {
				logError(e);
				return visualizationException.generate(e, null);
			}
		}
		
		/**
		 * Deletes a filter from the user's settings/profile.
		 * 
		 * @param filterKey the key of the filter
		 * @param visualizationTypeIndex encoding of the returned message
		 * @return success or error message, if possible in the requested encoding/format
		 */
		public String deleteFilter(String filterKey, Integer visualizationTypeIndex) {
			try {		
				if(this.filterManager == null) {
					// initialize filter manager
					this.filterManager = new SQLFilterManager();
				}
				
				if(!this.filterManager.deleteFilter(filterKey)) {
					throw new Exception("A unknown error occurred!");
				}
				
				MethodResult result = new MethodResult();
				result.setColumnName("DeletedFilter");
				result.setColumnDatatype(Types.VARCHAR);
				Object[] deletedDatabase = {filterKey};
				result.addRow(deletedDatabase);
				
				return visualizationManager.getVisualization(VisualizationType.fromInt(visualizationTypeIndex)).generate(result, null);
			}
			catch (Exception e) {
				logError(e);
				return  visualizationException.generate(e, null);
			}
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
		public String createQuery(String query, String[] queryParameters, String databaseKey,
				boolean useCache, int modificationTypeIndex, int visualizationTypeIndex, String[] visualizationParameters, boolean save) {
			
			MethodResult methodResult = null;
			String cacheKey = ""; // If empty, the query is not cached (used in "Non-Saving"-case, as creation always is)
			
			try {
				//Check for visualization parameters
				if(visualizationTypeIndex > 3 && visualizationTypeIndex < 10){
					if(visualizationParameters == null || visualizationParameters.length != 3){
						return visualizationException.generate(new Exception(), "Missing visualization-parameters!");
					}
				}
				
				query = insertParameters(query, queryParameters);
				
				if(save){
					return saveQuery(query, databaseKey, useCache, modificationTypeIndex, visualizationTypeIndex, visualizationParameters);
				}
				else{
					methodResult = executeSQLQuery(query, databaseKey, cacheKey);
					Modification modification = modificationManager.getModification(ModificationType.fromInt(modificationTypeIndex));
					Visualization visualization = visualizationManager.getVisualization(VisualizationType.fromInt(visualizationTypeIndex));
					
					if(modification.check(methodResult))
						methodResult = modification.apply(methodResult);
					else
						return visualizationException.generate(new Exception(), "Can not modify result with " + modification.getType().name() + ".");
					
					if(visualization.check(methodResult, visualizationParameters))
						return visualization.generate(methodResult, visualizationParameters);
					else
						return visualizationException.generate(new Exception(), "Can not convert result into " + visualization.getType().name() + "-format.");
				}
			}
			catch(Exception e) {
				logError(e);
				return visualizationException.generate(e, "An Error occured while trying to execute the query!");
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
		public String visualizeQuery(String key) {
			
			Query query = null;
			try {
				query = queryManager.getQuery(key);
				if(query == null) {
					throw new Exception("Query does not exist!"); 
				}
			}
			catch(Exception e) {
				return visualizationException.generate(e, "Encountered a problem while trying to fetch stored query " + key);
			}
			
			try {
				return visualizeQuery(query);
			}
			catch(Exception e) {
				logError(e.getMessage());
				return visualizationException.generate(e, "Encountered a Problem while trying to visualize Query!");
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
				boolean useCache, int modificationTypeIndex, int visualizationTypeIndex, String[] visualizationParamaters) {
			
			SQLDatabase database = null;
			Query query = null;
			String queryKey = ""; //If empty, the query generates a new one
			try {
				database = databaseManager.getDatabaseInstance(databaseKey);
				query = new Query(database.getJdbcInfo(), database.getUser(), database.getPassword(), database.getDatabase(), database.getHost(), database.getPort(),
					queryStatement, useCache, modificationTypeIndex, visualizationTypeIndex, visualizationParamaters, queryKey);
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
				SQLDatabase sqlDatabase = new SQLDatabase(query.getJdbcInfo(), query.getUsername(), query.getPassword(), 
						query.getDatabase(), query.getHost(), query.getPort());

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
			}
			
			Modification modification = modificationManager.getModification(ModificationType.fromInt(query.getModificationTypeIndex()));
			Visualization visualization = visualizationManager.getVisualization(VisualizationType.fromInt(query.getVisualizationTypeIndex()));
			
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
									currentRow[i-1] = resultSet.getObject(i).toString();
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
					databaseKey = defaultDBKey;
					logMessage("No database key has been provided. Using default DB: " + databaseKey);
				}
				
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
		
		/**
		 * initialize the database connection
		 */
		private void initializeDBConnection () {
			try {
				if(db2Host == null || db2Port < 1 || db2Database == null || db2User == null || db2Password == null) {
					logError("Provided invalid parameters (default database) for the service! Please check you service config file!");
				}
				if(resultTimeout == null) {
					resultTimeout = "90";
				}
				
				// setup the database manager
				this.databaseManager = new SQLDatabaseManager();
				this.queryManager = new QueryManager();
				this.visualizationManager = VisualizationManager.getInstance();
				this.visualizationException = VisualizationException.getInstance();
				this.modificationManager = ModificationManager.getInstance();
				
				// add visualizations and modifications
				this.visualizationManager.registerVisualization(new VisualizationCSV());
				this.visualizationManager.registerVisualization(new VisualizationJSON());
				this.visualizationManager.registerVisualization(new VisualizationHTMLTable());
				this.visualizationManager.registerVisualization(new VisualizationXML());
				this.visualizationManager.registerVisualization(new VisualizationGoogleTable());
				this.visualizationManager.registerVisualization(new VisualizationPieChart());
				this.visualizationManager.registerVisualization(new VisualizationBarChart());
				this.visualizationManager.registerVisualization(new VisualizationLineChart());
				this.visualizationManager.registerVisualization(new VisualizationTimeline());
				this.visualizationManager.registerVisualization(new VisualizationRadarChart());

				this.modificationManager.registerModification(new ModificationIdentity());
				this.modificationManager.registerModification(new ModificationLogarithmic());
				this.modificationManager.registerModification(new ModificationNormalization());
				
				if(this.databaseManager.getDatabaseCount() < 1) {
					
					// the user has no databases yet - add the default database
					if(!this.databaseManager.addDatabase(defaultDBKey, SQLDatabaseType.DB2, db2User, db2Password, db2Database, db2Host, db2Port)) {
						// failed to add the database...
						throw new Exception("Failed to add the default database for the user!");
					}
				}
				
				// get the result cache
				this.resultCache = MethodResultCache.getInstance(Integer.parseInt(resultTimeout));
				
			} 
			catch (NoSuchMethodException e) {
				logError(e);
			} 
			catch(Exception e) {
				logError(e);
			}
		}		

	} // nested class AgentSession
	
	
	
	private Hashtable<Long, AgentSession> htAgentSessions = new Hashtable<Long, AgentSession> ();
	
	
	
	/**
	 * Get a session for the given agent.
	 * 
	 * If none exists, a new one will be created
	 * 
	 * @param agent
	 * 
	 * @return an AgentSession
	 */
	private AgentSession getAgentSession ( Agent agent ) {
		synchronized ( htAgentSessions ) {
			AgentSession result = htAgentSessions.get ( agent.getId());
			
			if ( result == null ) {
				result = new AgentSession ( agent ) ;
				htAgentSessions.put ( agent.getId(), result);
				
				logMessage("Created new Session for " + agent );
			}
			
			return result;
		}
	}
	
	/**
	 * get a session for the currently active agent
	 * 
	 * @return an AgentSession
	 */
	private AgentSession getCurrentAgentSession () {
		return getAgentSession ( getActiveAgent() );
	}
	
	
	/**
	 * just for testing
	 * 
	 * @return session age of the current user
	 */
	public long getSessionAge () {
		return getCurrentAgentSession().getAgeInMs();
	}
	
	
	
	/**
	 * Simple constructor for service generation
	 */
	public QueryVisualizationService()
	{
		setFieldValues();
	}
	
	
	@Override
	public void launchedAt ( Node node ) throws L2pServiceException {
		super.launchedAt ( node );
		
		tidyThread = new TimerThread( tidySleepTimeMs ) {
			@Override
			public void tick() {
				for ( long agentId : htAgentSessions.keySet() ){
					if ( htAgentSessions.get(agentId).getAgeInMs() >= sessionTimeout ) {
						htAgentSessions.remove ( agentId );
						logMessage ( "Session of agent " + agentId + " timed out");
					}
				}
			}};
		tidyThread.start();
	}
	

	@Override 
	public void close () {
		tidyThread.stopTimer();
		super.close();
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
	public String addDatabase(String databaseKey, Integer databaseTypeCode, String username, String password, String database, String host, Integer port, Integer visualizationTypeIndex) {
		AgentSession session = getCurrentAgentSession ();
		return session.addDatabase(databaseKey, databaseTypeCode, username, password, database, host, port, visualizationTypeIndex);		
	}
	
	
	/**
	 * Removes a database from the user's list of configured databases (so that the user can not access the database anymore).
	 * Other user's database settings are not changed.
	 * 
	 * @param databaseKey the key of the database
	 * @param visualizationTypeIndex encoding of the returned message
	 * @return success or error message, if possible in the requested encoding/format
	 */
	public String removeDatabase(String databaseKey, Integer visualizationTypeIndex) {
		return getCurrentAgentSession ().removeDatabase(databaseKey, visualizationTypeIndex);
	}
	
	
	/**
	 * Returns a list/table of the keys of all available/configured databases of the user.
	 * 
	 * @param visualizationTypeIndex encoding of the returned message
	 * @return success or error message, if possible in the requested encoding/format
	 */
	public String getDatabaseKeys(Integer visualizationTypeIndex) {
		return getCurrentAgentSession().getDatabaseKeys(visualizationTypeIndex);
	}
	
	
	/**
	 * Returns a list/table of all filters the user has configured.
	 * 
	 * @param visualizationTypeIndex encoding of the returned message
	 * @return success or error message, if possible in the requested encoding/format
	 */
	public String getFilterKeys(Integer visualizationTypeIndex) {
		return getCurrentAgentSession().getFilterKeys(visualizationTypeIndex);
	}
	
	
	/**
	 * Retrieves the values for a specific filter.
	 * 
	 * @param filterKey
	 * @param visualizationTypeIndex encoding of the returned message
	 * @return success or error message, if possible in the requested encoding/format
	 */
	public String getFilterValues(String filterKey, Integer visualizationTypeIndex) {
		return getCurrentAgentSession().getFilterValues(filterKey, visualizationTypeIndex);
	}
	
	
	/**
	 * Adds a filter to the user's settings/profile.
	 * 
	 * @param filterKey the Key that should be used for this filter
	 * @param SQLQuery SQL  query which is used to retrieve the filter values
	 * @param databaseKey	key of the database for which the filter has been configured
	 * @param visualizationTypeIndex  encoding of the returned message
	 * 
	 * @return success or error message, if possible in the requested encoding/format
	 */
	public String addFilter(String filterKey, String SQLQuery, String databaseKey, Integer visualizationTypeIndex) {
		return getCurrentAgentSession().addFilter(filterKey, SQLQuery, databaseKey, visualizationTypeIndex);
	}
	
	
	/**
	 * Deletes a filter from the user's settings/profile.
	 * 
	 * @param filterKey the key of the filter
	 * @param visualizationTypeIndex encoding of the returned message
	 * @return success or error message, if possible in the requested encoding/format
	 */
	public String deleteFilter(String filterKey, Integer visualizationTypeIndex) {
		return getCurrentAgentSession().deleteFilter(filterKey, visualizationTypeIndex);
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
	public String createQuery(String query, String[] queryParameters, String databaseKey,
			boolean useCache, int modificationTypeIndex, int visualizationTypeIndex, String[] visualizationParameters, boolean save) {
				return getCurrentAgentSession().createQuery(query, queryParameters, databaseKey, useCache, modificationTypeIndex, visualizationTypeIndex, visualizationParameters, save);
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
	public String visualizeQuery(String key) {
		return getCurrentAgentSession().visualizeQuery(key);
	}
}