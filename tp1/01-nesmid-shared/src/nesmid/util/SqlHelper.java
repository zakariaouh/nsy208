package nesmid.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class SqlHelper {

	public static void executeUpdate(String driver, String connStr, String usr,String pwd, String command) throws Exception
	{
			Class.forName(driver); // driver jdbc
			
			Connection conn = DriverManager.getConnection(
					connStr, // url
					usr, 	// user
					pwd); 	// password

			Statement stmt = conn.createStatement();
			
			stmt.executeUpdate(command);
			
			conn.commit() ;
			
			stmt.close() ;
			
			conn.close();
	}
	
	public static ResultSet  executeQuery(String driver, String connStr, String usr,String pwd, String command) throws Exception
	{		
			Class.forName(driver); // driver jdbc
			
			Connection conn = DriverManager.getConnection(
					connStr, // url
					usr, 	// user
					pwd); 	// password

			Statement stmt = conn.createStatement();
			
			ResultSet rs = stmt.executeQuery(command);
			
			conn.commit() ;
			
			stmt.close() ;
			
			conn.close();
			
			return rs;
	}
 
	
	
}
