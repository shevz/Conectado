package utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

public class DBConnectorFactory {
	private static java.sql.Connection conexion = null;

	public static java.sql.Connection getConnection() {
		if (conexion == null) {
			MysqlDataSource mySQL = new MysqlDataSource();
			mySQL.setUser("root");
			mySQL.setPassword("");
			mySQL.setUrl("jdbc:mysql://localhost:3306/login");
			try {
				conexion = mySQL.getConnection();
			} catch (SQLException e) {
				System.out.println("Could not connect to database.");
				e.printStackTrace();
				checkForDB();
			}
		}
		return conexion;
	}
	
	public static void checkForDB(){
		Connection connection ;
	    Statement statement ;
	    
	    try {
	        Class.forName("com.mysql.jdbc.Driver");
	        connection = DriverManager.getConnection("jdbc:mysql://localhost/", "root", "");
	        statement = connection.createStatement();
	        int myResult = statement.executeUpdate("CREATE DATABASE login");   
	        
	        Logging.getLogger().info("Database created!");
	    } catch (SQLException sqlException) {
	        if (sqlException.getErrorCode() == 1007) {
	            // Database already exists error
	        	Logging.getLogger().info(sqlException.getMessage());
	        } else {
	            // Some other problems, e.g. Server down, no permission, etc
	            sqlException.printStackTrace();
	        }
	    } catch (ClassNotFoundException e) {
	        // No driver class found!
	    }
	}

}
