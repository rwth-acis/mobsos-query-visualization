package i5.las2peer.services.queryVisualization.database;

public enum SQLDatabaseType {
	// A DB2 database. Works with the "db2jcc-0.jar" +  "db2jcc_licence_cu-0.jar" archive.
	DB2 (1),
	// mysqlConnectorJava-5.1.16.jar (MySQL 5.1)
	MySQL (2),
	// jaybird-2.1.6.jar
	Firebird (3),
	// sqljdbc4.jar
	MSSQLServer (4),
	//postgresql-9.0-801.jdbc4.jar (PostgreSQL 9)
	PostgreSQL (5),
	// derbyclient.jar
	JavaDBDerby (6),
	// ojdbc14.jar (Oracle 10.2)
	Oracle (7);
	
	private final int code;
	
	SQLDatabaseType(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return this.code;
	}
	
	public static SQLDatabaseType getSQLDatabaseType(int code) {
		switch(code) {
			case 1:
				// DB2
				return SQLDatabaseType.DB2;
			case 2:
				// MySQL
				return SQLDatabaseType.MySQL;
			case 3:
				// Firebird
				return SQLDatabaseType.Firebird;
			case 4:
				// Microsoft SQL Server
				return SQLDatabaseType.MSSQLServer;
			case 5:
				// PostgreSQL
				return SQLDatabaseType.PostgreSQL;
			case 6:
				// JavaDB/Derby
				return SQLDatabaseType.JavaDBDerby;
			case 7:
				// Oracle
				return SQLDatabaseType.Oracle;
		}
	
		// not known...
		return null;
	}
	
	public String getDriverName() {
		switch(this.code) {
			case 1:
				// DB2
				return "com.ibm.db2.jcc.DB2Driver";
			case 2:
				// MySQL
				return "com.mysql.jdbc.Driver";
			case 3:
				// Firebird
				return "org.firebirdsql.jdbc.FBDriver";
			case 4:
				// Microsoft SQL Server
				return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
			case 5:
				// PostgreSQL
				return "org.postgresql.Driver";
			case 6:
				// JavaDB/Derby
				return "org.apache.derby.jdbc.ClientDriver";
			case 7:
				// Oracle
				return "oracle.jdbc.driver.OracleDriver";
		}
		// not found...
		return null;
	}
	
	public String getJDBCurl(String host, String database, int port) {
		String url = null;
		
		// add the url prefix
		switch(this.code) {
			case 1:
				// DB2
				url = "jdbc:db2://" + host + ":" + port + "/" + database;
				break;
			case 2:
				// MySQL
				url = "jdbc:mysql://" + host + ":" + port + "/" + database;
				break;
			case 3:
				// Firebird
				url = "jdbc:firebirdsql:"+ host + "/" + port + ":" + database;
				break;
			case 4:
				// Microsoft SQL Server
				// does a connect work? username and password...
				url = "jdbc:sqlserver://"+ host + ":" + port + ";databaseName=" + database+";";
				break;
			case 5:
				// PostgreSQL
				url = "jdbc:postgresql://"+ host + ":" + port + "/" + database;
				break;
			case 6:
				// JavaDB/Derby
				url = "jdbc:derby://"+ host + ":" + port + "/" + database;
				break;
			case 7:
				// Oracle
				url = "jdbc:oracle:thin:@"+ host + ":" + port + ":" + database;
				break;
			default:
				// not found...
				return null;
		}
		
		
		return url;
	}
}
