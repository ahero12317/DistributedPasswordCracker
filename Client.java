
 
package client;
import java.io.*;
import java.net.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
 
class Cracker {
    private final static byte BASE = 36;
    private final static char[] digitToChar = new char[BASE];
 
    public Cracker() {
        for (int i = 0; i < 26; ++i) {
            digitToChar[i] = (char) (i + 'a');
        }
        for (int i = 0; i < 10; ++i) {
            digitToChar[i + 26] = (char) (i + '0');
        }
    }
 
    private static String maskToString(long mask, int size) {
        StringBuilder ret = new StringBuilder();
        while (size-- > 0) {
            int digit = (int) (mask % BASE);
            ret.append(digitToChar[digit]);
            mask /= (long) BASE;
        }
        return ret.toString();
    }
 
    private static String getHash(String password) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("MD5");
        mDigest.reset();
        mDigest.update(password.getBytes());
        byte[] digest = mDigest.digest();
        BigInteger bigInt = new BigInteger(1,digest);
        String hash = bigInt.toString(16);
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < 32 - hash.length(); ++i) {
            ret.append("0");
        }
        ret.append(hash);
        return ret.toString();
    }
 
    public String bruteForce(long start, long end, int size, String key) throws NoSuchAlgorithmException {
        for (long i = start; i <= end; ++i) {
            String password = maskToString(i, size);
            String hashText = getHash(password);
            if (key.equals(hashText))  {
                return password;
            }
        }
        return null;
    }
}


public class Client {
        private final static int PORT = 8000;
    private final static String HOST = "127.0.0.1";
    public static void main(String[] arg) {
        try {
            Cracker passCrack = new Cracker();
 
            Socket socket = new Socket(HOST , PORT);
            System.out.println("[SUCCESS]: Connected to Server");
 
            DataInputStream clientInput = new DataInputStream(socket.getInputStream());
            DataOutputStream clientOutput = new DataOutputStream(socket.getOutputStream());
 
            String hash = clientInput.readUTF();
            System.out.println("hash:  " + hash);
           
            while(true) {
                clientOutput.writeUTF("REQUEST_RANGE");
 
                String received = clientInput.readUTF();
                String[] msg = received.split(",");
                long start = Long.parseLong(msg[0]);
                long end = Long.parseLong(msg[1]);
                int textSize = Integer.parseInt(msg[2]);
 
                System.out.println("start: "  + Long.toString(start));
                System.out.println("end:   "  + Long.toString(end));
                System.out.println("size: " + Long.toString(textSize));
 
                String pass = passCrack.bruteForce(start, end, textSize, hash);
                if(pass != null) {
                  	System.out.println("[SUCCESS]: Password cracked successfully.\nDisconnecting...");
                    String message = "FOUND," + pass;
                    clientOutput.writeUTF(message);
                    break;
                }
            }
 
            socket.close();
            clientInput.close();
            clientOutput.close();
        } catch (SocketException | EOFException e) {
            System.out.println("[FAILED]: Could not connect to server (Reason: Server is closed)");
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }


}
