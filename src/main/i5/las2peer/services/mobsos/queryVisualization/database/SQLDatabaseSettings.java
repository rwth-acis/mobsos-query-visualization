package i5.las2peer.services.mobsos.queryVisualization.database;


import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.XmlAble;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * SQLDatabaseSettings.java
 *<br>
 *This class provides access to a stored database. This does not provide access to any database functions.
 *All instances of this class are always loaded in the SQLDatabaseManager.
 */
public class SQLDatabaseSettings implements XmlAble, Serializable {

	private static final long serialVersionUID = -3534465664376573595L;
	private SQLDatabaseType jdbcInfo = null;
	private String key = null;
	
	private String username = null;
	private String password = null;
	private String database = null;
	private String host = null;
	private int port = -1;
	
	public SQLDatabaseSettings() {
	}
	
	public SQLDatabaseSettings(String key, SQLDatabaseType jdbcInfo, String username, String password, String database, String host, int port) {
		//TODO: sanity checks
		
		this.key = key;
		this.jdbcInfo = jdbcInfo;
		this.username = username;
		this.password = password;
		this.host = host;
		this.port = port;
		this.database = database;
		
	}
	
	public String getKey() {
		return key;
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
		return database;
	}
	public String getHost() {
		return host;
	}
	public int getPort() {
		return port;
	}

	
	


	public void setStateFromXml(String arg0) throws MalformedXMLException {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(arg0);
			int childrenCount = doc.getChildNodes().item(0).getChildNodes().getLength();
			doc.getDocumentElement().normalize();
			Element elm = doc.getDocumentElement();
	
			if(childrenCount != 2) {
				throw new Exception("Wrong number of children! " + childrenCount + " instead of 2!");
			}
			
			this.key = elm.getAttribute("key");
			
			this.username = elm.getAttribute("username");
			this.password = elm.getAttribute("password");
			
			this.database = elm.getAttribute("name");
			this.jdbcInfo = SQLDatabaseType.getSQLDatabaseType(Integer.parseInt(elm.getAttribute("type")));
			this.host = elm.getAttribute("host");
			this.port = Integer.parseInt(elm.getAttribute("port"));
		}
		catch(Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, "SQLDatabaseSettings, setStateFromXML: " + e.getMessage());
		}
	}

	@Override
	public String toXmlString() {
		String xmlString = "";
		xmlString += "<SQLDatabaseSettings key=\""+this.getKey()+"\">\n";
		
		xmlString += "\t<Credentials username=\""+this.getUsername()+"\" password=\""+this.getPassword()+"\" ></Credentials>\n";
		xmlString += "\t<Database name=\""+this.getDatabase()+"\" type=\""+this.getJdbcInfo().getCode()+"\" host=\""+this.getHost()+"\" port=\""+this.getPort()+"\" ></Database>\n";
		
		xmlString += "</SQLDatabaseSettings>";
		
		return xmlString;
	}
	
	public static SQLDatabaseSettings[] fromResultSet(ResultSet set) throws SQLException {
		LinkedList<SQLDatabaseSettings> settings = new LinkedList<SQLDatabaseSettings>();
		try {
			while (set.next()) {
				SQLDatabaseSettings setting = new SQLDatabaseSettings(
						set.getString("KEY"), SQLDatabaseType.getSQLDatabaseType(set.getInt("JDBCINFO")),
						set.getString("USERNAME"), set.getString("PASSWORD"), set.getString("DATABASE"),
						set.getString("HOST"), set.getInt("PORT"));
				settings.add(setting);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return settings.toArray(new SQLDatabaseSettings[0]);
	}
}
