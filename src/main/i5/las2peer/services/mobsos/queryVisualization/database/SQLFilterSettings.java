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
 * SQLFilterSettings.java
 * <br>
 * Stores the settings of a filter (its key, the query associated with it and its database).
 */
public class SQLFilterSettings implements XmlAble, Serializable {

	private static final long serialVersionUID = 7160232780166466973L;
	private String key = null;
	private String query = null;
	private String databaseKey = null;
	
	public SQLFilterSettings() {
	}
	
	public SQLFilterSettings(String databaseKey, String name, String query) {
		//TODO: sanity checks
		this.key = name;
		this.query = query;
		this.databaseKey = databaseKey;
	}

	public String getName() {
		return key;
	}
	public StringPair getKey() {
		return new StringPair (databaseKey, key);
	}
	public String getQuery() {
		return query;
	}
	public String getDatabaseKey() {
		return databaseKey;
	}
	
	public void setStateFromXml(String arg0) throws MalformedXMLException {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(arg0);
			int childrenCount = doc.getChildNodes().item(0).getChildNodes().getLength();
			doc.getDocumentElement().normalize();
			Element elm = doc.getDocumentElement();
			
			
			if(childrenCount != 1) {
				throw new Exception("Wrong number of children! " + childrenCount + " instead of 1!");
			}
			
			this.key = elm.getAttribute("key");
			
			this.query = elm.getAttribute("query");
			this.databaseKey = elm.getAttribute("databaseKey");
			
		}
		catch(Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, "SQLFilterSettings, setStateFromXML: " + e.getMessage());
		}
	}

	@Override
	public String toXmlString() {
		String xmlString = "";
		xmlString += "<SQLFilterSettings key=\""+this.getKey()+"\">\n";
		
		xmlString += "\t<SQLQuery query=\""+this.getQuery()+"\" databaseKey=\""+this.getDatabaseKey()+"\">";
		
		xmlString += "\t</SQLQuery>\n";
		xmlString += "</SQLFilterSettings>";
		
		return xmlString;
	}

	public static SQLFilterSettings[] fromResultSet(ResultSet set) throws SQLException {
		LinkedList<SQLFilterSettings> settings = new LinkedList<SQLFilterSettings>();
		try {
			while (set.next()) {
				SQLFilterSettings setting = new SQLFilterSettings(set.getString("DB_KEY"), set.getString("KEY"), set.getString("QUERY") );
				settings.add(setting);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return settings.toArray(new SQLFilterSettings[]{});
	}

	@Override
	public String toString() {
		return "SQLFilterSettings [databaseKey=" + databaseKey + ", name = " + key + ", query=" + query + "]";
	}
}
