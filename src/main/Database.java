/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author Cosmin
 */
public class Database {

    private Connection connection = null;

    public Database() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Error, MySQL JDBC Driver?");
            e.printStackTrace();
            return;
        }

        try {
            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/kdc", "root", "August17th");

        } catch (SQLException e) {
            System.out.println("Connection Failed!");
            e.printStackTrace();
            return;
        }

        if (connection == null) {
            System.out.println("Failed to make connection!");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public boolean firstCheck(String username, String resourceName, String right) {
        System.out.println("Checking username");
        if (checkUser(username)) {
            System.out.println("Username ok, checking if user is in access list of the resource");
            if (checkAccestoResource(username, resourceName, right)) {
                System.out.println("User is in resource list, checking Bell-LaPadula access");
                if (checkBellLaPadula(username, resourceName, right)) {
                    System.out.println("-> User has access to the resource");
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkBellLaPadula(String username, String resourceName, String right) {
        String querry1 = "SELECT level  FROM resources WHERE UPPER(name) LIKE '"
                + resourceName.toUpperCase() + "';";
        String querry2 = "SELECT level  FROM users WHERE UPPER(username) LIKE '"
                + username.toUpperCase() + "';";
        Statement s;
        int resourceLevel = -1, userLevel = -1;
        try {
            s = connection.createStatement();

            ResultSet r1 = s.executeQuery(querry1);

            if (r1.next()) {
                resourceLevel = getIndexLevel(r1.getString(1));
            }

            ResultSet r2 = s.executeQuery(querry2);

            if (r2.next()) {
                userLevel = getIndexLevel(r2.getString(1));
            }
            
            if (userLevel >= resourceLevel && right.compareTo("write") == 0) {
                return true;
            }
            if (userLevel <= resourceLevel && right.compareTo("read") == 0) {
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getIndexLevel(String level) {
        switch (level) {
            case "unclassified":
                return 1;
            case "classified":
                return 2;
            case "secret":
                return 3;
            case "top secret":
                return 4;
        }
        return -1;
    }

    public boolean checkAccestoResource(String username, String resourceName, String right) {
        String query = "SELECT userlist  FROM resources WHERE UPPER(name) LIKE '"
                + resourceName.toUpperCase() + "';";
        Statement s;
        try {
            s = connection.createStatement();

            ResultSet r = s.executeQuery(query);
            if (r.next()) {
                if (r.getString(1).contains(username)) {
                    return true;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean checkUser(String username) {
        String query = "SELECT username FROM users WHERE UPPER(username) LIKE '"
                + username.toUpperCase() + "';";
        Statement s;
        try {
            s = connection.createStatement();

            ResultSet r = s.executeQuery(query);
            if (r.next()) {
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getKut(String username) {
        String query = "SELECT kut FROM users WHERE UPPER(username) LIKE '"
                + username.toUpperCase() + "';";
        Statement s;
        try {
            s = connection.createStatement();

            ResultSet r = s.executeQuery(query);
            if (r.next()) {
                return r.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String getKst(String resourceName) {
        String query = "SELECT kst FROM resources WHERE UPPER(name) LIKE '"
                + resourceName.toUpperCase() + "';";
        Statement s;
        try {
            s = connection.createStatement();

            ResultSet r = s.executeQuery(query);
            if (r.next()) {
                return r.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }
}