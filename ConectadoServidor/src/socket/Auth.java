package socket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import utilities.DBConnectorFactory;

public class Auth {
	private String username;
	private String password;
	private static Connection conexion=null;

	public Auth(){
		conexion=DBConnectorFactory.getConnection();
	}
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public static void create(String uName, String pWord) {
		try {
			//java.sql.Connection conexion = null;
			conexion = DBConnectorFactory.getConnection();
			Statement state = conexion.createStatement();
			String query = "Insert into users values('" + uName + "','" + pWord + "')";
			boolean result = state.execute(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static boolean auth_login(String username1, String password1) {

		try {
			Class.forName("com.mysql.jdbc.Driver"); // MySQL database connection
			conexion=DBConnectorFactory.getConnection();
			//Connection conn = DriverManager.getConnection(url, user, pass);
			PreparedStatement pst = conexion.prepareStatement("Select * from users where Username=? and Password=?");
			pst.setString(1, username1);
			pst.setString(2, password1);
			ResultSet rs = pst.executeQuery();
			if (!rs.next()) {
				return false;
			}
			if (username1.equals(rs.getString("Username")) && password1.equals(rs.getString("Password"))) {
				return true;
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

}
