/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Cosmin
 */
public class KDC implements Runnable {

    public final int PORT = 8100;
    private ServerSocket serverSocket = null;
    private PrintWriter out = null;
    private BufferedReader in = null;
    private Socket clientSocket = null;
    private Database database;
    private Login login;

    @Override
    public void run() {

        database = new Database();

        try {
            in = new BufferedReader(new InputStreamReader(
                    clientSocket.getInputStream()));

            out = new PrintWriter(clientSocket.getOutputStream());

            String uORr = in.readLine();

            if (uORr.compareTo("user") == 0) {
                System.out.println("An user connected to the server");
                login(database.getConnection());
                login.sendInfos();
                getRequest();
            } else {
                if (uORr.compareTo("resource") == 0) {
                    System.out.println("A resource connected to the server");
                    sendKeyToResource();
                }
            }
        } catch (IOException e) {
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Socket cannot be closed \n" + e);
            }
        }
    }

    public KDC() throws IOException {
        while (true) {
            try {
                serverSocket = new ServerSocket(PORT);
                clientSocket = serverSocket.accept();
                new Thread(this).start();

            } finally {
                serverSocket.close();
            }
        }
    }

    public void login(Connection connection) throws IOException {
        boolean result = false;
        while (!result) {
            login = new Login(out, in, connection);
            result = login.getResult();
            if (result) {
                out.println("succes");
                out.flush();
                break;
            } else {
                out.println("fail");
                out.flush();
            }
        }
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public PrintWriter getOut() {
        return out;
    }

    public void setOut(PrintWriter out) {
        this.out = out;
    }

    public BufferedReader getIn() {
        return in;
    }

    public void setIn(BufferedReader in) {
        this.in = in;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public void setClientSocket(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void getRequest() {
        String username = null, resourceName = null, right = null;
        int n1 = -1;
        try {
            username = in.readLine();
            resourceName = in.readLine();
            n1 = Integer.parseInt(in.readLine());
            right = in.readLine();
            System.out.println("Getting a request from user: " + username + " for resource: " + resourceName + ", session: " + n1 + " to " + right);
        } catch (IOException ex) {
            Logger.getLogger(KDC.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        respondToRequest(username, resourceName, n1, right);

    }

    public void respondToRequest(String username, String resourceName, int n1, String right) {
        if (database.firstCheck(username, resourceName, right)) {
            System.out.println("Sending first part of the message to user");
            System.out.println("Generating triple des key K: ");
            String K = generateK();
            System.out.println(K);
            System.out.println("Generating key life length L");
            String L = generateL();
            System.out.println(L);
            String messageU = K + "," + n1 + "," + L + "," + resourceName;
            System.out.println("Message before encryption: ");
            System.out.println(messageU);
            System.out.println("");
            SecretKey aesKey = getUserAesKey(username);
            String encryptedUMessage = aesEncryption(messageU, aesKey);
            System.out.println("Message after encryption: ");
            System.out.println(encryptedUMessage);
            System.out.println("");
            out.write(encryptedUMessage);
            out.flush();
            System.out.println("Sending second part of the message");
            String messageS = K + "," + username + "," + L;
            System.out.println("Message before encryption: ");
            System.out.println(messageS);
            System.out.println("");
            SecretKey secretKey = getResourceAesKey(resourceName);
            String encryptedSMessage = aesEncryption(messageS, secretKey);
            System.out.println("Message after encryption: ");
            System.out.println(encryptedSMessage);
            System.out.println("");
            System.out.println("Message after decryption: ");
            System.out.println(aesDecryption(secretKey, encryptedSMessage));
            out.write(encryptedSMessage);
            out.flush();
        } else {
            out.write("");
            out.flush();
        }
    }

    public SecretKey getUserAesKey(String username) {
        try {
            String key = database.getKut(username);
            byte[] encodedKey = Base64.decode(key);
            SecretKey originalKey = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
            return originalKey;
        } catch (Base64DecodingException ex) {
            Logger.getLogger(KDC.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public SecretKey getResourceAesKey(String resourceName) {
        try {
            String key = database.getKst(resourceName);
            byte[] encodedKey = Base64.decode(key);
            SecretKey originalKey = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
            return originalKey;
        } catch (Base64DecodingException ex) {
            Logger.getLogger(KDC.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public String generateL() {
        int x = (int) (1 + Math.random() * 5);
        return x + "";
    }

    public String aesEncryption(String strToEncrypt, SecretKey aesKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            String encryptedString = Base64.encode(cipher.doFinal(strToEncrypt.getBytes()));
            return encryptedString;
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(KDC.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(KDC.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(KDC.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(KDC.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(KDC.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public String aesDecryption(SecretKey aesKey, String strToDecrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            String decryptedString = new String(cipher.doFinal(Base64.decode(strToDecrypt)));
            return decryptedString;
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(KDC.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(KDC.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(KDC.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(KDC.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(KDC.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Base64DecodingException ex) {
            Logger.getLogger(KDC.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public String generateK() {
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("DESede");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(KDC.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        keyGen.init(128); // key length 112 bits
        SecretKey secretKey = keyGen.generateKey();
        String stringKey = null;
        if (secretKey != null) {
            byte[] lastEncByte = secretKey.getEncoded();
            stringKey = Base64.encode(lastEncByte);
        }
        return stringKey;
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

    public void sendKeyToResource() {
        System.out.println("Generating key for resource");
        String kst = generateAESkey();
        System.out.println("Sending key to resource: " + generateAESkey());
        out.println(kst);
        out.flush();
    }
}