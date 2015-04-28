package i5.las2peer.services.queryVisualization.query;

import i5.las2peer.execution.L2pServiceException;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.XmlAble;
import i5.las2peer.security.Context;
import i5.las2peer.services.queryVisualization.database.SQLDatabaseType;
import i5.las2peer.services.queryVisualization.encoding.VisualizationType;
import i5.simpleXML.Element;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.regex.Pattern;


/**
 * Query.java
 *<br>
 *A stored Query. It provides all data needed to to execute it again, including credentials to the database and visualization information.
 *Since this is a dataclass, no functionality is implemented here. 
 */
public class Query implements XmlAble, Serializable {
	

	private static final long serialVersionUID = -4423072045695775785L;
	private static final String qpDelim = "$-$.$";
	
	private String key = null;
	private long user = 0;
	private String username = null;
	private String password = null;
	private SQLDatabaseType jdbcInfo;
	private String databaseName = null;
	private String host = null;
	private int port = -1;
	private boolean useCache = false;
	private String queryStatement = null;
	private String[] queryParameters = null;
	private int modificationTypeIndex = -1;
	private VisualizationType visualizationTypeIndex = VisualizationType.JSON;
	private String title = null;
	private int width = 0;
	private int height = 0;
	
	
	public Query(long user, SQLDatabaseType jdbcInfo, String username, String password, String database, String host, int port,
			String queryStatement, String[] queryParameters, boolean useCache, int modificationTypeIndex, VisualizationType visualizationTypeIndex, String[] visualizationParameters, String key) {
				
		this.user = user;
		this.jdbcInfo = jdbcInfo;
		this.username = username;
		this.password = password;
		this.host = host;
		this.port = port;
		this.databaseName = database;
		this.queryStatement = queryStatement;
		this.queryParameters = queryParameters;
		this.useCache = useCache;
		this.visualizationTypeIndex = visualizationTypeIndex;
		this.modificationTypeIndex = modificationTypeIndex;
		this.title = visualizationParameters[0];
		this.height = Integer.parseInt(visualizationParameters[1]);
		this.width = Integer.parseInt(visualizationParameters[2]);
		this.key = key;
		if(this.key.equals("")){
			this.key = this.hashCode()+"";
		}
		
	}
	
	public long getUser() {
		return user;
	}
	public SQLDatabaseType getJdbcInfo() {
		return jdbcInfo;
	}
	public String getUsername() {
		return username;
	}
	public String getPassword() {
		return password;
	}
	public String getDatabase() {
		return databaseName;
	}
	public String getHost() {
		return host;
	}
	public int getPort() {
		return port;
	}
	public boolean usesCache() {
		return useCache;
	}
	public String getQueryStatement() {
		return queryStatement;
	}
	public String[] getQueryParameters() {
		return queryParameters;
	}
	public String getInsertedQueryStatement(String[] queryParameters) {
		try {
			if (queryParameters == null) {
				return insertParameters(queryStatement, getQueryParameters());
			}
			return insertParameters(queryStatement, queryParameters);
		} catch (L2pServiceException e) {
			// TODO Auto-generated catch block
			return null;
		}

	}
	public int getModificationTypeIndex() {
		return modificationTypeIndex;
	}
	public VisualizationType getVisualizationTypeIndex() {
		return visualizationTypeIndex;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String[] getVisualizationParameters() {
		return new String[]{title, ""+height,""+width};
	}
	public String getTitle() {
		return title;
	}
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	


	public void setStateFromXml(String arg0) throws MalformedXMLException {
		try {
			Element elm = new Element(arg0);
	
			if(elm.getChildNodeCount() != 5) {
				throw new Exception("Wrong number of childs! " + elm.getChildNodeCount() + " instead of 5!");
			}
			
			this.key = elm.getAttribute("key");
			
			this.username = elm.getChild(0).getAttribute("username");
			this.password = elm.getChild(0).getAttribute("password");
			
			
			this.databaseName = elm.getChild(1).getAttribute("name");
			this.jdbcInfo = SQLDatabaseType.getSQLDatabaseType(Integer.parseInt(elm.getChild(1).getAttribute("type")));
			this.host = elm.getChild(1).getAttribute("host");
			this.port = Integer.parseInt(elm.getChild(1).getAttribute("port"));
			
			this.visualizationTypeIndex = VisualizationType.valueOf(elm.getChild(2).getAttribute("visualizationTypeIndex"));
			this.modificationTypeIndex = Integer.parseInt(elm.getChild(2).getAttribute("modificationTypeIndex"));
			this.useCache = Boolean.parseBoolean(elm.getChild(2).getAttribute("useCache"));
			
			this.queryStatement = elm.getChild(3).getAttribute("statement");
			
			//Google Visualizations -> Hardcoded TODO
			if(this.visualizationTypeIndex.ordinal() > 3){
				this.title = elm.getChild(4).getAttribute("title");
				this.height = Integer.parseInt(elm.getChild(4).getAttribute("height"));
				this.width = Integer.parseInt(elm.getChild(4).getAttribute("width"));
			}
		
		}
		catch(Exception e) {
			Context.logMessage(this, "Query, setStateFromXML: " + e.getMessage());
		}
	}

	@Override
	public String toXmlString() {
		String xmlString = "";
		xmlString += "<Query key=\""+this.getKey()+"\">\n";
		
		xmlString += "\t<Credentials username=\""+this.getUsername()+"\" password=\""+this.getPassword()+"\" ></Credentials>\n";
		xmlString += "\t<Database name=\""+this.getDatabase()+"\" type=\""+this.getJdbcInfo().getCode()+"\" host=\""+this.getHost()+"\" port=\""+this.getPort()+"\"></Database>\n";
		xmlString += "\t<VisualizationAndModification visualizationTypeIndex=\""+this.getVisualizationTypeIndex()+"\" modificationTypeIndex=\""+this.getModificationTypeIndex()+"\" useCache=\""+this.usesCache()+"\"></VisualizationAndModification>\n";
		xmlString += "\t<QueryStatement statement=\""+this.getQueryStatement()+"\" ></QueryStatement>\n";
		//Google Visualizations -> Hardcoded TODO
		if(this.visualizationTypeIndex.ordinal() > 3){
			xmlString += "\t<VisualizationParameters title=\""+this.title+"\" height=\""+this.height+"\" width=\""+this.width+"\"></VisualizationParameters>\n";
		}
		
		xmlString += "</Query>";
		
		return xmlString;
	}
	
	public PreparedStatement prepareStatement(PreparedStatement s) throws SQLException {
		StringBuilder qp = new StringBuilder();
		for (String param : queryParameters) {
			qp.append(param);
			qp.append(qpDelim);
		}
		if (qp.length() > 0) {
			qp.delete(qp.length() - 5, qp.length());
		}
		s.setString(1, key);
		s.setLong(2, user);
		s.setString(3, username);
		s.setString(4, password);
		s.setInt(5, jdbcInfo.getCode());
		s.setString(6, databaseName);
		s.setString(7, host);
		s.setInt(8, port);
		s.setInt(9, useCache ? 1 : 0);
		s.setString(10, queryStatement);
		s.setString(11, qp.toString());
		s.setInt(12, modificationTypeIndex);
		s.setString(13, visualizationTypeIndex.toString());
		s.setString(14, title);
		s.setInt(15, height);
		s.setInt(16, width);
	return s;
	}
	
	public static String getReplace() {
		return "REPLACE INTO `QUERIES` (`KEY`, `USER`, `USERNAME`, `PASSWORD`, `JDBCINFO`,"
				+ "`DATABASE_NAME`, `HOST`, `PORT`, `USE_CACHE`, `QUERY_STATEMENT`,"
				+ "`FILTER_DEFAULTS`,"
				+ "`MODIFICATION_TYPE`, `VISUALIZATION_TYPE`, `VISUALIZATION_TITLE`,"
				+ "`VISUALIZATION_HEIGHT`, `VISUALIZATION_WIDTH`) VALUES"
				+ "(?,	?,	?,	?,	?,	?,	?,	?,	?,	?,	?,	?,	?,	?,	?,	?);";
	}
	
	public static Query[] fromResultSet(ResultSet set) {
		LinkedList<Query> qs = new LinkedList<Query>();
		try {
			while (set.next()) {
				String key = set.getString("KEY");
				long user = set.getLong("USER");
				String username = set.getString("USERNAME");
				String password = set.getString("PASSWORD");
				SQLDatabaseType jdbcInfo = SQLDatabaseType.getSQLDatabaseType(set.getInt("JDBCINFO"));
				String databaseName = set.getString("DATABASE_NAME");
				String host = set.getString("HOST");
				int port = set.getInt("PORT");
				boolean useCache = set.getInt("USE_CACHE") == 1;
				String queryStatement = set.getString("QUERY_STATEMENT");
				String qp = set.getString("FILTER_DEFAULTS");
				String[] queryParameters = null;
				if (qp != null) {
					queryParameters = qp.split(qpDelim);
				}
				int modificationTypeIndex = set.getInt("MODIFICATION_TYPE");
				VisualizationType visualizationTypeIndex = VisualizationType.valueOf(set.getString("VISUALIZATION_TYPE"));
				String visualizationTitle = set.getString("VISUALIZATION_TITLE");
				int visualizationHeight = set.getInt("VISUALIZATION_HEIGHT");
				int visualizationWidth = set.getInt("VISUALIZATION_WIDTH");
				String[] s = new String[] {visualizationTitle, ""+visualizationHeight, ""+visualizationWidth};
				Query q = new Query(user, jdbcInfo, username, password, databaseName, host, port,
						queryStatement, queryParameters, useCache, modificationTypeIndex, visualizationTypeIndex, s, key);
				qs.add(q);
			}
			return qs.toArray(new Query[]{});
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
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
	public static String insertParameters(String query, String[] queryParameters) throws L2pServiceException {
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
			throw new L2pServiceException("exception in insertParameters", e);
		}
	}

}
