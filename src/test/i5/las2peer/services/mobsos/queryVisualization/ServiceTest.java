package i5.las2peer.services.mobsos.queryVisualization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.connectors.webConnector.client.ClientResponse;
import i5.las2peer.connectors.webConnector.client.MiniClient;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.LocalNodeManager;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.services.mobsos.queryVisualization.database.SQLDatabaseType;
import i5.las2peer.testing.MockAgentFactory;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 * Example Test Class demonstrating a basic JUnit test structure.
 * 
 * 
 *
 */
public class ServiceTest {

	private static LocalNode node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;
	private MiniClient client;

	private static UserAgentImpl testAgent;
	private static final String testPass = "adamspass";

	private final ServiceNameVersion testServiceClass = new ServiceNameVersion(
			QueryVisualizationService.class.getCanonicalName(), "1.0");

	private static final String mainPath = "QVS/";

	static JSONObject testQuery = new JSONObject();
	private static final String queryPath = "query";

	static JSONObject testDB = new JSONObject();
	private static final String testDBName = "newTestDB";
	private static final String dbPath = "database/";

	static JSONObject testFilter = new JSONObject();
	private static final String testFilterName = "newTestFilter";
	private static final String filterPath = "filter/";

	HashMap<String, String> emptyPairs = new HashMap<>();

	private static final String TEST_FILTER_URI = mainPath + filterPath + testDBName + "/" + testFilterName;
	private static final String TEST_DB_URI = mainPath + dbPath + testDBName;

	@BeforeClass
	public static void setUpDB() {

		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream(
					"etc/i5.las2peer.services.mobsos.queryVisualization.QueryVisualizationService.properties");

			// load a properties file
			prop.load(input);
			// avoid timing errors: wait for the repository manager to get all
			// services before continuing
			testDB.put("db_code", SQLDatabaseType.MYSQL.toString().toLowerCase());
			testDB.put("username", prop.getProperty("stDbUser"));
			testDB.put("password", prop.getProperty("stDbPassword"));
			testDB.put("database", prop.getProperty("stDbDatabase"));
			testDB.put("dbhost", prop.getProperty("stDbHost"));
			testDB.put("port", Integer.parseInt(prop.getProperty("stDbPort")));

			testFilter.put("query", "SELECT * FROM USERS");

			testQuery.put("query", "SELECT * FROM `DATABASE_CONNECTIONS` WHERE `KEY` = ?");
			testQuery.put("dbkey", testDBName);
			JSONArray qp = new JSONArray();
			qp.add(testDBName);
			testQuery.put("queryparams", qp);
			testQuery.put("cache", true);
			testQuery.put("modtypei", 0);
			testQuery.put("title", "testQuery");
			testQuery.put("width", "100");
			testQuery.put("height", "100");
			testQuery.put("save", true);

		} catch (IOException ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
			fail();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Called before the tests start.
	 * 
	 * Sets up the node and initializes connector and users that can be used throughout the tests.
	 * 
	 * @throws Exception
	 */
	@Before
	public void startServer() throws Exception {
		// start node
		node = new LocalNodeManager().newNode();
		testAgent = MockAgentFactory.getAdam();
		testAgent.unlock(testPass);
		node.storeAgent(testAgent);
		node.launch();

		ServiceAgentImpl testService = ServiceAgentImpl.createServiceAgent(testServiceClass, "a pass");
		testService.unlock("a pass");

		node.registerReceiver(testService);

		// start connector
		logStream = new ByteArrayOutputStream();

		connector = new WebConnector(true, 0, false, 0);
		connector.setLogStream(new PrintStream(logStream));
		connector.start(node);

		client = new MiniClient();
		client.setConnectorEndpoint(connector.getHttpEndpoint());
		client.setLogin(testAgent.getIdentifier(), testPass);
	}

	/**
	 * Called after the tests have finished. Shuts down the server and prints out the connector log file for reference.
	 * 
	 * @throws Exception
	 */
	@After
	public void shutDownServer() throws Exception {
		if (connector != null) {
			try {
				cleanUp(client);
			} catch (Exception e) {
				e.printStackTrace();
				fail("Exception: " + e);
			}
			connector.stop();
			connector = null;
		}
		if (node != null) {
			node.shutDown();
			node = null;
		}
		System.out.println("Connector-Log:");
		System.out.println("--------------");
		System.out.println(logStream.toString());
	}

	private static void cleanUp(MiniClient c) {
		c.sendRequest("DELETE", TEST_FILTER_URI, "");
		c.sendRequest("DELETE", TEST_DB_URI, "");
	}

	/**
	 * Tests Listing of a database keys
	 */
	@Test
	public void testGetDatabases() {
		try {
			client.sendRequest("PUT", TEST_DB_URI, testDB.toJSONString(), "application/json", "application/json",
					emptyPairs);
			ClientResponse result = client.sendRequest("GET", mainPath + dbPath, "");
			assertEquals(200, result.getHttpCode());
			JSONArray expected = new JSONArray();
			JSONArray actual = (JSONArray) JSONValue.parse(result.getResponse());
			for (String s : new String[] { "DatabaseKeys", "string", testDBName }) {
				JSONArray inner = new JSONArray();
				inner.add(s);
				expected.add(inner);
				assertTrue(inner.toString() + " should be in the data. Actual data: " + actual, actual.contains(inner));
			}

			System.out.println("Result of 'GetDatabases': " + result.getResponse().trim());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	/**
	 * Tests creation of a database
	 */
	@Test
	public void testAddDatabase() {
		try {
			cleanUp(client);

			ClientResponse result = client.sendRequest("PUT", TEST_DB_URI, testFilter.toJSONString(),
					"application/json", "application/json", emptyPairs);
			assertEquals(400, result.getHttpCode());

			result = client.sendRequest("PUT", TEST_DB_URI, testDB.toJSONString(), "application/json",
					"application/json", emptyPairs);
			assertEquals(201, result.getHttpCode());
			JSONArray expected = new JSONArray();
			for (String s : new String[] { "AddedDatabase", "string", testDBName }) {
				JSONArray inner = new JSONArray();
				inner.add(s);
				expected.add(inner);
			}
			assertEquals(expected, JSONValue.parse(result.getResponse()));

			System.out.println("Result of 'addDatabase': " + result.getResponse().trim());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	/**
	 * Tests removal of a database
	 */
	@Test
	public void testRemoveDatabase() {
		try {
			cleanUp(client);

			ClientResponse result = client.sendRequest("DELETE", mainPath + dbPath + "asohusnaoue", "");
			assertEquals(404, result.getHttpCode());

			client.sendRequest("PUT", TEST_DB_URI, testDB.toJSONString(), "application/json", "application/json",
					emptyPairs);
			result = client.sendRequest("DELETE", TEST_DB_URI, "");
			assertEquals(200, result.getHttpCode());
			JSONArray expected = new JSONArray();
			for (String s : new String[] { "RemovedDatabase", "string", testDBName }) {
				JSONArray inner = new JSONArray();
				inner.add(s);
				expected.add(inner);
			}
			assertEquals(expected, JSONValue.parse(result.getResponse()));

			System.out.println("Result of 'removeDatabase': " + result.getResponse().trim());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}

	}

	/**
	 * Tests Listing of a database keys
	 */
	@Test
	public void testGetFilters() {
		try {
			// Test Empty filter list
			ClientResponse result = client.sendRequest("GET", mainPath + filterPath, "");
			assertEquals(200, result.getHttpCode());

			// Add a filter
			result = client.sendRequest("PUT", TEST_DB_URI, testDB.toJSONString(), "application/json",
					"application/json", emptyPairs);
			result = client.sendRequest("PUT", TEST_FILTER_URI, testFilter.toJSONString(), "application/json",
					"application/json", emptyPairs);

			result = client.sendRequest("GET", mainPath + filterPath, "");
			assertEquals(200, result.getHttpCode());
			JSONArray expected = new JSONArray();
			JSONArray names = new JSONArray();
			names.add("FilterKeys");
			names.add("DatabaseKeys");
			expected.add(names);

			JSONArray types = new JSONArray();
			types.add("string");
			types.add("string");
			expected.add(types);

			JSONArray content = new JSONArray();
			content.add(testFilterName);
			content.add(testDBName);
			expected.add(content);

			// JSONArray actual =
			// (JSONArray)JSONValue.parse(result.getResponse());
			assertTrue(expected.contains(names));
			assertTrue(expected.contains(types));
			assertTrue(expected.contains(content));

			System.out.println("Result of 'GetFilters': " + result.getResponse().trim());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	/**
	 * Tests Creation of Filter
	 */
	@Test
	public void testAddFilter() {
		try {
			cleanUp(client);

			ClientResponse result = client.sendRequest("PUT", TEST_FILTER_URI, testDB.toJSONString(),
					"application/json", "application/json", emptyPairs);
			assertEquals(400, result.getHttpCode());

			result = client.sendRequest("PUT", TEST_FILTER_URI, testFilter.toJSONString(), "application/json",
					"application/json", emptyPairs);
			assertEquals(400, result.getHttpCode()); // testDB does not exist
														// yet!

			result = client.sendRequest("PUT", TEST_DB_URI, testDB.toJSONString(), "application/json",
					"application/json", emptyPairs);
			assertEquals(201, result.getHttpCode());
			result = client.sendRequest("PUT", TEST_FILTER_URI, testFilter.toJSONString(), "application/json",
					"application/json", emptyPairs);
			assertEquals(201, result.getHttpCode()); // testDB exists now!
			JSONArray expected = new JSONArray();
			for (String s : new String[] { "AddedFilter", "string", testFilterName }) {
				JSONArray inner = new JSONArray();
				inner.add(s);
				expected.add(inner);
			}
			assertEquals(expected, JSONValue.parse(result.getResponse()));

			System.out.println("Result of 'AddFilter': " + result.getResponse().trim());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	/**
	 * Tests removal of a filter
	 */
	@Test
	public void testRemoveFilter() {
		try {
			cleanUp(client);

			ClientResponse result = client.sendRequest("DELETE", mainPath + filterPath + testDBName + "/asohusnaoue",
					"");
			assertEquals(404, result.getHttpCode());

			result = client.sendRequest("PUT", TEST_DB_URI, testDB.toJSONString(), "application/json",
					"application/json", emptyPairs);
			assertEquals(201, result.getHttpCode());
			result = client.sendRequest("PUT", TEST_FILTER_URI, testFilter.toJSONString(), "application/json",
					"application/json", emptyPairs);
			assertEquals(201, result.getHttpCode());
			result = client.sendRequest("DELETE", TEST_FILTER_URI, "");
			assertEquals(200, result.getHttpCode());
			JSONArray expected = new JSONArray();
			for (String s : new String[] { "DeletedFilter", "string", testFilterName }) {
				JSONArray inner = new JSONArray();
				inner.add(s);
				expected.add(inner);
			}
			assertEquals(expected, JSONValue.parse(result.getResponse()));

			System.out.println("Result of 'removeFilter': " + result.getResponse().trim());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	/**
	 * Tests Creation of Queries
	 */
	@Test
	public void testQuery() {
		try {
			cleanUp(client);

			// Not a valid Query
			ClientResponse result = client.sendRequest("POST", mainPath + queryPath, testDB.toJSONString(),
					"application/json", "application/json", emptyPairs);
			assertEquals(400, result.getHttpCode());

			// Database does not exist yet
			result = client.sendRequest("POST", mainPath + queryPath, testQuery.toJSONString(), "application/json",
					"*/*", emptyPairs);
			assertEquals(400, result.getHttpCode());

			// Now create database first
			result = client.sendRequest("PUT", TEST_DB_URI, testDB.toJSONString(), "application/json",
					"application/json", emptyPairs);
			assertEquals(201, result.getHttpCode());
			result = client.sendRequest("POST", mainPath + queryPath, testQuery.toJSONString(), "application/json",
					"*/*", emptyPairs);
			assertEquals(201, result.getHttpCode());
			Integer key = 0;
			try {
				key = Integer.parseInt(JSONValue.parse(result.getResponse()).toString());
			} catch (Exception e) {
				fail("Received data is not a query id: " + JSONValue.parse(result.getResponse()).toString());
			}

			result = client.sendRequest("GET", mainPath + queryPath + "/" + key, "");
			assertEquals(200, result.getHttpCode());

			System.out.println("Result of 'AddFilter': " + result.getResponse().trim());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}
}
