
package server;
import java.net.*;
import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class Generator {
    public int mTextSize;
    public long mRangeSize;
    private static long mEnd;
    private static int mBase;
  	public static Map<Long, Long> mRangeMap = new ConcurrentHashMap<Long, Long>();
    public Generator(int base, int maxChars, int rangeSize) {
        mEnd = (long) Math.pow(base, maxChars);
        mBase = base;
        mRangeSize = rangeSize;
        mTextSize = maxChars;
    }
    public long NextRange() {
        if(mTextSize == 0 && mRangeMap.isEmpty()) return -1;
      	
        if(mEnd == 0) {
          	if(!mRangeMap.isEmpty())
            {
              java.util.List<Long> keysArray = new java.util.ArrayList<Long>(mRangeMap.keySet());
              return mRangeMap.remove(keysArray.get(0));
            }
            mTextSize = mTextSize - 1;
            mEnd = (long) Math.pow(mBase, mTextSize);
        }
        long rngEnd = mEnd;
        mRangeMap.put(rngEnd, rngEnd);
        mEnd = Math.max(0, mEnd - mRangeSize);
        return rngEnd;
    }
}
 


public class Server {
    public static String hash;
    public static Socket userSocket;
    public static DataInputStream userInput;
    public static DataOutputStream userOutput;
    private static final int BASE = 36;
    private static final int PORT = 8000;
    private static final int MAX_SIZE = 5;
    private static final int RANGE_SIZE = 1000000;
    public static void main(String[] args) throws IOException {
        Generator generator = new Generator(BASE , MAX_SIZE , RANGE_SIZE);
        ServerSocket server = new ServerSocket(PORT);
      
      	userSocket = server.accept();
        
        userInput = new DataInputStream(userSocket.getInputStream());
        userOutput = new DataOutputStream(userSocket.getOutputStream());
        
        hash = userInput.readUTF();
      
        while(true) {
            System.out.println("[PENDING]: Waiting for Connection");
            Thread client = new ClientHandler(server, generator);
            client.start();
        }
    }

    public static void finish() throws IOException {
        userSocket.close();
      	userInput.close();
        userOutput.close();
        System.exit(0);
    }
}
 
class ClientHandler extends Thread {
    private Socket socket;
    private Generator generator;
    private DataInputStream serverInput;
    private DataOutputStream serverOutput;
 
    public ClientHandler(ServerSocket server , Generator generator) throws IOException {
        this.socket = server.accept();
        this.serverInput = new DataInputStream(socket.getInputStream());
        this.serverOutput = new DataOutputStream(socket.getOutputStream());
        this.generator = generator;
    }
 
    @Override
    public void run() {
        String received;
      	long prevRange = 0;
        try {
            serverOutput.writeUTF(Server.hash);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                received = serverInput.readUTF();
                String[] msg = received.split(",");
                if (msg[0].equals("REQUEST_RANGE")) {
                    long range = generator.NextRange();
                  	if(Generator.mRangeMap.containsKey(prevRange))
                      	Generator.mRangeMap.remove(prevRange);
                  	prevRange = range;
                    String message = Long.toString(Math.max(0, range - generator.mRangeSize)) + ',' + Long.toString(range) + ',' + Integer.toString(generator.mTextSize);
                    serverOutput.writeUTF(message);
                }
                else if (msg[0].equals("FOUND")) {
                    System.out.println("[SUCCESS]: Password Found! \nPassword:   " + msg[1]);
                  	Server.userOutput.writeUTF(msg[1]);
                    Server.finish();
                }
            } catch (SocketException | EOFException e) {
                try {
                    this.cancel();
                } catch (IOException err) {
                    err.printStackTrace();
                }
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
 
    public void cancel() throws IOException {
        this.socket.close();
        this.serverOutput.close();
        this.serverInput.close();
    }

    
}
