/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import com.sun.org.apache.xml.internal.security.utils.Base64;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 *
 * @author Cosmin
 */
public class Login {

    private String username;
    private String password;
    private Connection connection;
    private boolean result;
    private BufferedReader in;
    private PrintWriter out;

    public Login(PrintWriter out, BufferedReader in, Connection connection)
            throws IOException {
        this.in = in;
        this.out = out;
        this.connection = connection;
        String option = in.readLine();
        if (option.compareTo("login") == 0) {
            checkLogin();
        } else {
            checkRegister();
        }
    }

    public void checkRegister() throws IOException {
        System.out.println("User try to register, verifying username and password");
        username = in.readLine();
        password = md5(in.readLine());
        if (username.equals(null) || password.equals(null)) {
            return;
        }
        this.connection = connection;

        if (checkUsername(username)) {
            if (checkPassword(password)) {
                if (checkExistence(username, password)) {
                    result = true;
                    System.out.println("Everything is ok, user is registered");
                } else {
                    System.out.println("Password");
                    result = false;
                }
            } else {
                result = false;
            }
        } else {
            result = false;
        }
    }

    public boolean checkExistence(String username, String password) {
        String query = "SELECT username FROM users WHERE UPPER(username) LIKE '"
                + username.toUpperCase() + "';";
        Statement s;
        try {
            s = connection.createStatement();

            ResultSet r = s.executeQuery(query);
            if (r.next()) {
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        addUser(username, password);

        return true;
    }

    public void addUser(String username, String password) {
        String sql = " INSERT INTO users VALUES (? , ?, ?, ?) ";
        PreparedStatement pstmt;
        try {
            pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            String level = generateLevel();
            pstmt.setString(3, level);
            String aesKey = generateAESkey();
            pstmt.setString(4, aesKey);
            pstmt.executeUpdate();

        } catch (SQLException ex) {
            Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String generateLevel() {
        String returnValue = "";
        int random = (int) (1 + (Math.random() * 4));
        switch (random) {
            case 1:
                returnValue = "top secret";
                break;
            case 2:
                returnValue = "secret";

                break;
            case 3:
                returnValue = "classified";
                break;
            case 4:
                returnValue = "unclassified";
                break;
        }
        return returnValue;
    }

    public void checkLogin() throws IOException {
        System.out.println("User trying to login, verifying username and password");
        username = in.readLine();
        password = md5(in.readLine());
        if (username.equals(null) || password.equals(null)) {
            return;
        }
        this.connection = connection;

        if (checkUsername(username)) {
            if (checkPassword(password)) {
                if (checkDatabase(username, password)) {
                    System.out.println("Everything is ok, user is logged in");
                    result = true;
                } else {
                    result = false;
                }
            } else {
                result = false;
            }
        } else {
            result = false;
        }
    }

    public boolean regexChecker(String theRegex, String string2Check) {
        Pattern checkRegex = Pattern.compile(theRegex);
        Matcher regexMatcher = checkRegex.matcher(string2Check);
        while (regexMatcher.matches()) {
            if (regexMatcher.group().length() != 0) {
                return true;
            }
        }
        return false;
    }

    public boolean checkUsername(String username) {
        String theRegex = "([a-zA-Z]|_)([a-zA-Z]|[0-9]*|_)*";
        return regexChecker(theRegex, username);
    }

    public boolean checkPassword(String password) {
        String theRegex = "[a-zA-Z0-9_]*";
        return regexChecker(theRegex, password);
    }

    public boolean checkDatabase(String username, String password) {
        String query = "SELECT username, password, level, kut FROM users WHERE UPPER(username) LIKE '"
                + username.toUpperCase()
                + "' AND UPPER(password) LIKE '"
                + password.toUpperCase() + "'";
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

    public boolean getResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public String md5(String input) {
        String md5 = null;

        if (null == input) {
            return null;
        }

        try {
            // Create MessageDigest object for MD5
            MessageDigest digest = MessageDigest.getInstance("MD5");

            // Update input string in message digest
            digest.update(input.getBytes(), 0, input.length());

            // Converts message digest value in base 16 (hex)
            md5 = new BigInteger(1, digest.digest()).toString(16);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return md5;
    }

    public String generateAESkey() {
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
        }
        keyGen.init(192); // key length 192 bits
        SecretKey secretKey = keyGen.generateKey();
        String stringKey = null;
        if (secretKey != null) {
            byte[] lastEncByte = secretKey.getEncoded();
            stringKey = Base64.encode(lastEncByte);
        }
        return stringKey;
    }

    public void sendInfos() {
        String query = "SELECT level, kut FROM users WHERE UPPER(username) LIKE '"
                + username.toUpperCase() + "';";
        Statement s;
        try {
            s = connection.createStatement();

            ResultSet r = s.executeQuery(query);
            if (r.next()) {
                String level = r.getString(1);
                String kut = r.getString(2);
                out.println(level);
                out.flush();
                out.println(kut);
                out.flush();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}