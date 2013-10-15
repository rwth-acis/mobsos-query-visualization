package i5.las2peer.services.queryVisualization.query;

import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.XmlAble;
import i5.las2peer.security.Context;
import i5.las2peer.services.queryVisualization.database.SQLDatabaseType;
import i5.simpleXML.Element;

import java.io.Serializable;


/**
 * Query.java
 *<br>
 *A stored Query. It provides all data needed to to execute it again, including credentials to the database and visualization information.
 *Since this is a dataclass, no functionality is implemented here. 
 */
public class Query implements XmlAble, Serializable {
	

	private static final long serialVersionUID = -4423072045695775785L;
	
	private String key = null;
	private String username = null;
	private String password = null;
	private SQLDatabaseType jdbcInfo;
	private String databaseName = null;
	private String host = null;
	private int port = -1;
	private boolean useCache = false;
	private String queryStatement = null;
	private int modificationTypeIndex = -1;
	private int visualizationTypeIndex = -1;
	private String[] visualizationParameters = null;
	
	public Query(){
	}
	
	public Query(SQLDatabaseType jdbcInfo, String username, String password, String database, String host, int port,
			String queryStatement, boolean useCache, int modificationTypeIndex, int visualizationTypeIndex, String[] visualizationParameters, String key) {
				
		this.jdbcInfo = jdbcInfo;
		this.username = username;
		this.password = password;
		this.host = host;
		this.port = port;
		this.databaseName = database;
		this.queryStatement = queryStatement;
		this.useCache = useCache;
		this.visualizationTypeIndex = visualizationTypeIndex;
		this.modificationTypeIndex = modificationTypeIndex;
		this.visualizationParameters = visualizationParameters;
		this.key = key;
		if(this.key.equals("")){
			this.key = this.hashCode()+"";
		}
		
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
	public int getModificationTypeIndex() {
		return modificationTypeIndex;
	}
	public int getVisualizationTypeIndex() {
		return visualizationTypeIndex;
	}
	public String getKey() {
		return key;
	}
	public String[] getVisualizationParameters() {
		return visualizationParameters;
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
			
			this.visualizationTypeIndex = Integer.parseInt(elm.getChild(2).getAttribute("visualizationTypeIndex"));
			this.modificationTypeIndex = Integer.parseInt(elm.getChild(2).getAttribute("modificationTypeIndex"));
			this.useCache = Boolean.parseBoolean(elm.getChild(2).getAttribute("useCache"));
			
			this.queryStatement = elm.getChild(3).getAttribute("statement");
			
			//Google Visualizations -> Hardcoded TODO
			if(this.visualizationTypeIndex > 3){
				this.visualizationParameters = new String[3];
				this.visualizationParameters[0] = elm.getChild(4).getAttribute("title");
				this.visualizationParameters[1] = elm.getChild(4).getAttribute("width");
				this.visualizationParameters[2] = elm.getChild(4).getAttribute("height");
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
		if(this.visualizationTypeIndex > 3){
			xmlString += "\t<VisualizationParameters title=\""+this.visualizationParameters[0]+"\" height=\""+this.visualizationParameters[1]+"\" width=\""+this.visualizationParameters[2]+"\"></VisualizationParameters>\n";
		}
		
		xmlString += "</Query>";
		
		return xmlString;
	}
	
}
