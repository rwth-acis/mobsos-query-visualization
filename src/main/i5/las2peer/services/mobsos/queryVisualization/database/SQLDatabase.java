package i5.las2peer.services.mobsos.queryVisualization.database;

import i5.las2peer.api.Context;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.services.mobsos.queryVisualization.query.Query;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.dbcp2.BasicDataSource;

/**
 * SQLDatabase.java <br>
 * Provides access to SQL Queries. Instances of this class are only loaded in the SQLDatabaseManager if they are
 * currently connected.
 *
 */
public class SQLDatabase {

  private BasicDataSource dataSource;

  private SQLDatabaseType jdbcInfo = null;
  private String username = null;
  private String password = null;
  private String database = null;
  private String host = null;
  private int port = -1;
  private String key = null;

  public SQLDatabase(
    SQLDatabaseType jdbcInfo,
    String username,
    String password,
    String database,
    String host,
    int port,
    String key
  ) {
    // TODO: check parameters

    this.jdbcInfo = jdbcInfo;
    this.username = username;
    this.password = password;
    this.host = host;
    this.port = port;
    this.database = database;
    this.key = key;

    BasicDataSource ds = new BasicDataSource();
    String urlPrefix =
      jdbcInfo.getJDBCurl(this.host, this.database, this.port) +
      "?autoReconnect=true&useSSL=false";
    ds.setUrl(urlPrefix);
    ds.setUsername(username);
    ds.setPassword(password);
    ds.setDriverClassName(jdbcInfo.getDriverName());
    ds.setMinIdle(5);
    ds.setMaxIdle(10);
    ds.setMaxOpenPreparedStatements(100);

    dataSource = ds;
    System.out.println(ds);
    setValidationQuery();
  }

  public SQLDatabase(SQLDatabaseSettings settings) {
    this.jdbcInfo = settings.getJdbcInfo();
    this.username = settings.getUsername();
    this.password = settings.getPassword();
    this.host = settings.getHost();
    this.port = settings.getPort();
    this.database = settings.getDatabase();
    this.key = settings.getKey();

    BasicDataSource ds = new BasicDataSource();
    String urlPrefix =
      jdbcInfo.getJDBCurl(this.host, this.database, this.port) +
      "?autoReconnect=true&useSSL=false";
    ds.setUrl(urlPrefix);
    ds.setUsername(username);
    ds.setPassword(password);
    ds.setDriverClassName(jdbcInfo.getDriverName());
    ds.setMinIdle(5);
    ds.setMaxIdle(10);
    ds.setMaxOpenPreparedStatements(100);

    dataSource = ds;
    setValidationQuery();
  }

  public SQLDatabase(Query query) {
    this.jdbcInfo = query.getJdbcInfo();
    this.username = query.getUsername();
    this.password = query.getPassword();
    this.host = query.getHost();
    this.port = query.getPort();
    this.database = query.getDatabaseName();
    this.key = query.getKey();

    BasicDataSource ds = new BasicDataSource();
    String urlPrefix =
      jdbcInfo.getJDBCurl(this.host, this.database, this.port) +
      "?autoReconnect=true&useSSL=false";
    ds.setUrl(urlPrefix);
    ds.setUsername(username);
    ds.setPassword(password);
    ds.setDriverClassName(jdbcInfo.getDriverName());
    ds.setMinIdle(5);
    ds.setMaxIdle(10);
    ds.setMaxOpenPreparedStatements(100);

    dataSource = ds;
    setValidationQuery();
  }

  public BasicDataSource getDataSource() {
    return dataSource;
  }

  public ResultSet executeQuery(Connection con, String sqlQuery)
    throws Exception {
    // I don't allow escape characters...
    // at least some very basic escape checking
    // (I don't think it is sufficient for a real attack though....)
    sqlQuery = sqlQuery.replace("\\", "\\\\");
    sqlQuery = sqlQuery.replace("\0", "\\0");
    sqlQuery = sqlQuery.replace(";", "");

    try {
      Statement statement = con.createStatement();
      ResultSet resultSet = statement.executeQuery(sqlQuery);
      return resultSet;
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
      throw ex;
    } catch (Exception e) {
      System.out.println(e.getMessage());
      Context
        .get()
        .monitorEvent(this, MonitoringEvent.SERVICE_ERROR, e.toString());
      throw e;
    }
  }

  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  public String getUser() {
    return this.username;
  }

  public String getPassword() {
    return this.password;
  }

  public String getDatabase() {
    return this.database;
  }

  public String getHost() {
    return this.host;
  }

  public int getPort() {
    return this.port;
  }

  public String getKey() {
    return this.key;
  }

  public SQLDatabaseType getJdbcInfo() {
    return jdbcInfo;
  }

  private void setValidationQuery() {
    switch (jdbcInfo.getCode()) {
      case 1:
        dataSource.setValidationQuery("LIST TABLES");
      case 2:
        dataSource.setValidationQuery("SELECT 1");
      default:
        dataSource.setValidationQuery("SELECT 1");
    }
  }
}
