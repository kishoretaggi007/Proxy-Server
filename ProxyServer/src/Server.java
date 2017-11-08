import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//TODO: Close the connection when it is HTTP/1.1 and be able to get localhost type addresses

public class Server implements Runnable
{
    private final int CLIENTS_LIMIT = 2000;           // Set the number of concurrent clients
    private ServerSocket mProxy;                      // Starts a server socket
    private Socket mSocket;                           // Opens a socket
    private int mPort;                                // Port which the server accepts requests
    private int mCurrentConnections;                  // Number of concurrent connections
    private Thread mThread;                           // The thread the server will run on
    private ConcurrentHashMap<String, String> mCache; // Stores location of cache files

    public Server()
    {
        mProxy = null;
        mSocket = null;
        mPort = 2112;
        mCurrentConnections = 0;
        mCache = new ConcurrentHashMap<String, String>();
    }

    public Server(int port)
    {
        mProxy = null;
        mSocket = null;
        mPort = port;
        mCurrentConnections = 0;
        mCache = new ConcurrentHashMap<String, String>();
    }

    /**
     Start the proxy and open sockets for multiple clients to connect to.
     */
    public void initialize()
    {
        try
        {
            mProxy = new ServerSocket(mPort);
            System.out.println("Server started on port " + mPort);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        while(true)
        {
            try
            {
                // Accept new clients by starting a thread for each incoming connection
                mSocket = mProxy.accept();
                mCurrentConnections++;

                // Send message to client that the server is at it's limit for concurrent connections.
                
                if(mCurrentConnections > CLIENTS_LIMIT)
                {
                    PrintStream outMessage = new PrintStream(mSocket.getOutputStream());
                    outMessage.println("The server cannot accept anymore clients, please try again later.");
                    outMessage.close();
                    mSocket.close();
                }
                // Start a separate thread for each incoming client
                else
                {
                    ClientSocket client = new ClientSocket(this, mSocket, UUID.randomUUID());
                    Thread threadedClient = new Thread((Runnable) client);
                    threadedClient.start();
                    System.out.println(mCurrentConnections + " client(s) currently connected.");
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args)
    {
        Server server;

        // Check if a port number was provided and use that for the server.
        if (args.length > 0)
        {
            int port = Integer.parseInt(args[0]);
            server = new Server(port);
        }
        else
            server = new Server();

        Thread thread = new Thread(server);
        thread.start();

//        server.initialize();
        Scanner scanner = new Scanner(System.in);
        try
        {
            Thread.sleep(20 * 1000000);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
      Signal that a client has disconnected.
     */
    public void clientDisconnected(UUID clientId)
    {
        System.out.println(clientId + " has disconnected. There are now " + mCurrentConnections + " concurrent connections.");
        mCurrentConnections--;
    }

    /*
      Checks the cache for cached page, if it exists the Server will return
      the contents of the page, otherwise a null will be returned.
     */
    public String getCachedPage(String domain)
    {
        if(mCache.containsKey(domain))
        {
            try
            {
                byte[] bytesEncoded = Files.readAllBytes(Paths.get(mCache.get(domain)));
                return new String(bytesEncoded, StandardCharsets.UTF_8);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }

    /**
     * Adds a page for caching or updates an already cached page.
     */
    public void addPageToCache(String domain, String page)
    {
        if(!mCache.containsKey(domain))
        {
            FileOutputStream outputStream = null;
            try
            {
                
                String path = "files/file"+ domain.hashCode();
                File file = new File(path);
                outputStream = new FileOutputStream(file);
                byte[] content = page.getBytes(StandardCharsets.UTF_8);

                outputStream.write(content);
                //System.out.println("Hello 2");
                outputStream.flush();

                mCache.put(domain, path);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    if (outputStream != null)
                        outputStream.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run()
    {
        synchronized (this)
        {
            mThread = Thread.currentThread();
        }
        initialize();
    }

    /*String getCachedPage(String url) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    } */
}
