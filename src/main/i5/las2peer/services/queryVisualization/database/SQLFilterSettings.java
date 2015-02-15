package i5.las2peer.services.queryVisualization.database;


import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.XmlAble;
import i5.las2peer.security.Context;
import i5.simpleXML.Element;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

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
	
	public SQLFilterSettings(String key, String query, String databaseKey) {
		//TODO: sanity checks
		this.key = key;
		this.query = query;
		this.databaseKey = databaseKey;
	}

	public String getKey() {
		return key;
	}
	public String getQuery() {
		return query;
	}
	public String getDatabaseKey() {
		return databaseKey;
	}
	
	public void setStateFromXml(String arg0) throws MalformedXMLException {
		try {
			Element elm = new Element(arg0);
			
			if(elm.getChildNodeCount() != 1) {
				throw new Exception("Wrong number of children! " + elm.getChildNodeCount() + " instead of 1!");
			}
			
			this.key = elm.getAttribute("key");
			
			Element sqlQueryElement = elm.getChild(0);
			this.query = sqlQueryElement.getAttribute("query");
			this.databaseKey = sqlQueryElement.getAttribute("databaseKey");
			
		}
		catch(Exception e) {
			Context.logMessage(this, "SQLFilterSettings, setStateFromXML: " + e.getMessage());
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
				SQLFilterSettings setting = new SQLFilterSettings(set.getString("KEY"), set.getString("QUERY"), set.getString("DB_KEY") );
				settings.add(setting);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return settings.toArray(new SQLFilterSettings[]{});
	}
}
