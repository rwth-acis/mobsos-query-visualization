package i5.las2peer.services.queryVisualization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.restMapper.data.Pair;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.queryVisualization.database.SQLDatabaseType;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.webConnector.WebConnector;
import i5.las2peer.webConnector.client.ClientResponse;
import i5.las2peer.webConnector.client.MiniClient;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Example Test Class demonstrating a basic JUnit test structure.
 * 
 * 
 *
 */
public class ServiceTest {
	
	private static final String HTTP_ADDRESS = "http://127.0.0.1";
	private static final int HTTP_PORT = WebConnector.DEFAULT_HTTP_PORT;
	
	private static LocalNode node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;
	
	private static UserAgent testAgent;
	private static final String testPass = "adamspass";
	
	private static final String testServiceClass = QueryVisualizationService.class.getCanonicalName();
	
	private static final String mainPath = "QVS/";

	static JSONObject testQuery = new JSONObject();
	private static final String queryPath = "query";

	static JSONObject testDB = new JSONObject();
	private static final String testDBName = "newTestDB";
	private static final String dbPath = "database/";

	static JSONObject testFilter = new JSONObject();
	private static final String testFilterName = "newTestFilter";
	private static final String filterPath = "filter/";

	@SuppressWarnings("unchecked")
	Pair<String>[] emptyPairs = (Pair<String>[])new Pair[0];
	
	
	/**
	 * Called before the tests start.
	 * 
	 * Sets up the node and initializes connector and users that can be used throughout the tests.
	 * 
	 * @throws Exception
	 */
	@BeforeClass
	public static void startServer() throws Exception {
		
		//start node
		node = LocalNode.newNode();
		node.storeAgent(MockAgentFactory.getAdam());
		node.launch();
		
		ServiceAgent testService = ServiceAgent.generateNewAgent(testServiceClass, "a pass");
		testService.unlockPrivateKey("a pass");
		
		node.registerReceiver(testService);
		
		//start connector
		logStream = new ByteArrayOutputStream ();
		
		connector = new WebConnector(true,HTTP_PORT,false,1000);
		connector.setSocketTimeout(10000);
		connector.setLogStream(new PrintStream (logStream));
		connector.start ( node );
        Thread.sleep(1000); //wait a second for the connector to become ready
		testAgent = MockAgentFactory.getAdam();
		
        connector.updateServiceList();
        //avoid timing errors: wait for the repository manager to get all services before continuing
        testDB.put("db_code", SQLDatabaseType.MYSQL.toString().toLowerCase());
        testDB.put("username", "qv_user");
        testDB.put("password", "qv_password");
        testDB.put("database", "QVS");
        testDB.put("dbhost", "localhost");
        testDB.put("port", 3306);

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

        try
        {
            System.out.println("waiting..");
            Thread.sleep(10000);
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
		
	}
	
	
	/**
	 * Called after the tests have finished.
	 * Shuts down the server and prints out the connector log file for reference.
	 * 
	 * @throws Exception
	 */
	@AfterClass
	public static void shutDownServer () throws Exception {
		
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			cleanUp(c);
		} catch (Exception e) {
			e.printStackTrace();
			fail ( "Exception: " + e );
		}

		connector.stop();
		node.shutDown();
		
        connector = null;
        node = null;
        
        LocalNode.reset();
		
		System.out.println("Connector-Log:");
		System.out.println("--------------");
		
		System.out.println(logStream.toString());
		
    }
	
	private static void cleanUp(MiniClient c) {
		c.sendRequest("DELETE", mainPath + filterPath + testDBName, "");
		c.sendRequest("DELETE", mainPath + filterPath + testFilterName, "");
		c.sendRequest("DELETE", mainPath + dbPath + testDBName, "");
		c.sendRequest("DELETE", mainPath + dbPath + testFilterName, "");
	}
	
	
	/**
	 * 
	 * Tests the validation method.
	 * 
	 */
	public void testValidateLogin()
	{
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
            ClientResponse result=c.sendRequest("GET", mainPath +"validation", "");
            assertEquals(200, result.getHttpCode());
            assertTrue(result.getResponse().trim().contains("adam")); //login name is part of response
			System.out.println("Result of 'testValidateLogin': " + result.getResponse().trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
		
    }
	
	
	/**
	 * 
	 * Test the example method that consumes one path parameter
	 * which we give the value "testInput" in this test.
	 * 
	 */
	public void testExampleMethod()
	{
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
            ClientResponse result=c.sendRequest("POST", mainPath +"myResourcePath/testInput", ""); //testInput is the pathParam
            assertEquals(200, result.getHttpCode());
            assertTrue(result.getResponse().trim().contains("testInput")); //"testInput" name is part of response
			System.out.println("Result of 'testExampleMethod': " + result.getResponse().trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
		
    }

	/**
	 * Tests Listing of a database keys
	 */
	@Test
	public void testGetDatabases() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
            c.sendRequest("PUT", mainPath + dbPath + testDBName, testDB.toJSONString(), "application/json", "application/json", emptyPairs);
            ClientResponse result = c.sendRequest("GET", mainPath + dbPath, ""); // Remove DB first
            assertEquals(200, result.getHttpCode());
            JSONArray expected = new JSONArray();
            JSONArray actual = (JSONArray)JSONValue.parse(result.getResponse());
            for (String s : new String[]{"DatabaseKeys", "string", testDBName}) {
            	JSONArray inner = new JSONArray();
            	inner.add(s);
            	expected.add(inner);
            	assertTrue(inner.toString() + " should be in the data. Actual data: " + actual, actual.contains(inner));
			}
            
			System.out.println("Result of 'GetDatabases': " + result.getResponse().trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
	}

	/**
	 * Tests creation of a database 
	 */
	@Test
	public void testAddDatabase()
	{
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			cleanUp(c);

            ClientResponse result = c.sendRequest("PUT", mainPath + dbPath + testDBName, testFilter.toJSONString(), "application/json", "application/json", emptyPairs);
            assertEquals(400, result.getHttpCode());

            result = c.sendRequest("PUT", mainPath + dbPath + testDBName, testDB.toJSONString(), "application/json", "application/json", emptyPairs);
			cleanUp(c);
            assertEquals(201, result.getHttpCode());
            JSONArray expected = new JSONArray();
            for (String s : new String[]{"AddedDatabase", "string", testDBName}) {
            	JSONArray inner = new JSONArray();
            	inner.add(s);
            	expected.add(inner);
			}
            assertEquals(expected, (JSONArray)JSONValue.parse(result.getResponse()));
            
			System.out.println("Result of 'addDatabase': " + result.getResponse().trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
    }

	/**
	 * Tests removal of a database 
	 */
	@Test
	public void testRemoveDatabase() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			cleanUp(c);

            ClientResponse result = c.sendRequest("DELETE", mainPath + dbPath + "asohusnaoue", "");
            assertEquals(404, result.getHttpCode());

            c.sendRequest("PUT", mainPath + dbPath + testDBName, testDB.toJSONString(), "application/json", "application/json", emptyPairs);
            result = c.sendRequest("DELETE", mainPath + dbPath + testDBName, "");
            assertEquals(200, result.getHttpCode());
            JSONArray expected = new JSONArray();
            for (String s : new String[]{"RemovedDatabase", "string", testDBName}) {
            	JSONArray inner = new JSONArray();
            	inner.add(s);
            	expected.add(inner);
			}
            assertEquals(expected, (JSONArray)JSONValue.parse(result.getResponse()));
            
			System.out.println("Result of 'removeDatabase': " + result.getResponse().trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
		
    }

	/**
	 * Tests Listing of a database keys
	 */
	@Test
	public void testGetFilters() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);

			// Test Empty filter list
            ClientResponse result = c.sendRequest("GET", mainPath + filterPath, "");
            assertEquals(200, result.getHttpCode());

            // Add a filter
            result = c.sendRequest("PUT", mainPath + dbPath + testDBName, testDB.toJSONString(), "application/json", "application/json", emptyPairs);
            result = c.sendRequest("PUT", mainPath + filterPath + testDBName + "/" + testFilterName, testFilter.toJSONString(), "application/json", "application/json", emptyPairs);

            result = c.sendRequest("GET", mainPath + filterPath, "");
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
            
            // JSONArray actual = (JSONArray)JSONValue.parse(result.getResponse());
            assertTrue(expected.contains(names));
            assertTrue(expected.contains(types));
            assertTrue(expected.contains(content));
            
			System.out.println("Result of 'GetFilters': " + result.getResponse().trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
	}

	/**
	 * Tests Creation of Filter
	 */
	@Test
	public void testAddFilter() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			cleanUp(c);

            ClientResponse result = c.sendRequest("PUT", mainPath + filterPath + testDBName + "/" + testFilterName, testDB.toJSONString(), "application/json", "application/json", emptyPairs);
            assertEquals(400, result.getHttpCode());

            result = c.sendRequest("PUT", mainPath + filterPath + testDBName + "/" + testFilterName, testFilter.toJSONString(), "application/json", "application/json", emptyPairs);
            assertEquals(400, result.getHttpCode()); // testDB does not exist yet!

            result = c.sendRequest("PUT", mainPath + dbPath + testDBName, testDB.toJSONString(), "application/json", "application/json", emptyPairs);
            assertEquals(201, result.getHttpCode());
            result = c.sendRequest("PUT", mainPath + filterPath + testDBName + "/" + testFilterName, testFilter.toJSONString(), "application/json", "application/json", emptyPairs);
            assertEquals(201, result.getHttpCode()); // testDB exists now!
            JSONArray expected = new JSONArray();
            for (String s : new String[]{"AddedFilter", "string", testFilterName}) {
            	JSONArray inner = new JSONArray();
            	inner.add(s);
            	expected.add(inner);
			}
            assertEquals(expected, (JSONArray)JSONValue.parse(result.getResponse()));
            
			System.out.println("Result of 'AddFilter': " + result.getResponse().trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
	}

	/**
	 * Tests removal of a filter 
	 */
	@Test
	public void testRemoveFilter() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			cleanUp(c);

            ClientResponse result = c.sendRequest("DELETE", mainPath + filterPath + "/asohusnaoue", "");
            assertEquals(404, result.getHttpCode());

            result = c.sendRequest("PUT", mainPath + dbPath + testDBName, testDB.toJSONString(), "application/json", "application/json", emptyPairs);
            result = c.sendRequest("PUT", mainPath + filterPath + testDBName + "/" + testFilterName, testFilter.toJSONString(), "application/json", "application/json", emptyPairs);
            result = c.sendRequest("DELETE", mainPath + filterPath + testDBName + "/" + testFilterName, "");
            assertEquals(200, result.getHttpCode());
            JSONArray expected = new JSONArray();
            for (String s : new String[]{"DeletedFilter", "string", testFilterName}) {
            	JSONArray inner = new JSONArray();
            	inner.add(s);
            	expected.add(inner);
			}
            assertEquals(expected, (JSONArray)JSONValue.parse(result.getResponse()));
            
			System.out.println("Result of 'removeFilter': " + result.getResponse().trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
    }

	/**
	 * Tests Creation of Queries
	 */
	@Test
	public void testQuery() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			cleanUp(c);

			// Not a valid Query
            ClientResponse result = c.sendRequest("POST", mainPath + queryPath , testDB.toJSONString(), "application/json", "application/json", emptyPairs);
            assertEquals(400, result.getHttpCode());

            // Database does not exist yet
            result = c.sendRequest("POST", mainPath + queryPath, testQuery.toJSONString(), "application/json", "*/*", emptyPairs);
            assertEquals(400, result.getHttpCode());

            // Now create database first
            result = c.sendRequest("PUT", mainPath + dbPath + testDBName, testDB.toJSONString(), "application/json", "application/json", emptyPairs);
            assertEquals(201, result.getHttpCode());
            result = c.sendRequest("POST", mainPath + queryPath, testQuery.toJSONString(), "application/json", "*/*", emptyPairs);
            assertEquals(201, result.getHttpCode());
            Integer key = 0;
            try {
            	key = Integer.parseInt(JSONValue.parse(result.getResponse()).toString());
			} catch (Exception e) {
				fail("Received data is not a query id: " + JSONValue.parse(result.getResponse()).toString());
			}
            
            result = c.sendRequest("GET", mainPath + queryPath + "/" + key, "");
            assertEquals(200, result.getHttpCode());
            
            
			System.out.println("Result of 'AddFilter': " + result.getResponse().trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
	}

	/**
	 * Test the ServiceClass for valid rest mapping.
	 * Important for development.
	 */
	@Test
	public void testDebugMapping()
	{
		QueryVisualizationService cl = new QueryVisualizationService();
		assertTrue(cl.debugMapping());
	}
}
