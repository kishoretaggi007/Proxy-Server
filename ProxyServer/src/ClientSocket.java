import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class ClientSocket implements Runnable
{
    private final String CRLF = "\r\n";          // The end characters for a socket or server
    private BufferedReader mReader;             // Reads input from the server
    private PrintStream mPrintStream;           // Sends information to the client
    private Server mServer;                     // Used to communicate with the server
    private Socket mClient;                     // The socket for the client
    private UUID mClientID;                      // The id of the client
    private Request mRequest;

    public ClientSocket(Server server, Socket socket, UUID clientID)
    {
        mServer = server;
        mClient = socket;
        mClientID = clientID;
        try
        {
            mReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            mPrintStream = new PrintStream(socket.getOutputStream());

            System.out.println("Connection accepted with client " + clientID);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*ClientSocket(Server aThis, Socket mSocket, UUID randomUUID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }*/

    @Override
    public void run()
    {
        try
        {
            ArrayList<String> headers = new ArrayList<String>();
            String line = mReader.readLine();

            while(true)
            {
                if(line == null || line.equals(""))
                    break;

                System.out.println("Sent: " + line);
                headers.add(line);

                line = mReader.readLine();
            }

            if(headers.size() > 0)
            {
               // System.out.println("n hj");
                String[] initialHeaderContents = headers.get(0).split(" ");
                System.out.println(Arrays.toString(initialHeaderContents));
                if(initialHeaderContents[0].equals("GET"))
                {
                    //String s = initialHeaderContents[1];
                    Request request = new Request(headers);

                    if(request.isValid())
                    {
                        // Send request
                        //System.out.println("abcdefg");
                        sendRequest(request);
                    }
                    else
                    {
                        // Send error code
                        //sendRequest(request);
                        mPrintStream.println("Unable to make a request object");
                    }
                }
                // TODO other HTTP methods
                else if(initialHeaderContents[0].equals("POST"))
                {
                    // Send not implemented
                    mPrintStream.println("Reached the not implemented thing");
                }
                else
                {
                    // Send other error code
                    mPrintStream.println("Reached the other error code");
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                mReader.close();
                mPrintStream.close();
                mClient.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks if the input is a mValid URL, returns hostname if it is. Null otherwise.
     */
    private String validURL(String input)
    {
        try
        {
            URI uri = new URI(input);
            return uri.getHost();
        }
        catch(Exception e)
        {
            return null;
        }
    }

    /**
     * Parses the mValid message sent by the server.
     */
    private void handleGetRequestTypeOne(String[] messageContents, URI uri)
    {
        String hostname = uri.getHost();
        String path = uri.getPath();
        String flag = messageContents[2];

        sendGetRequest(hostname, path, flag);
    }

    private void handleGetRequestTypeTwo(String[] messageContents, String hostname)
    {
        String path = messageContents[1];
        String flag = messageContents[2];

        sendGetRequest(hostname, path, flag);
    }

    /**
     * Sends a request from the socket to the server based on the hostname and path
     */
    private void sendGetRequest(String hostname, String path, String flag)
    {
        try
        {
            //TODO: Port 80 needs to not be hard coded
            InetAddress address = InetAddress.getByName(hostname);
            Socket getRequest = new Socket(address, 80);

            //Send header information
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(getRequest.getOutputStream(), "UTF8"));
            writer.write("GET " + path + " " + flag + CRLF);
            writer.write("Content-Type: application/x-www-form-urlencoded" + CRLF);
            writer.write(CRLF + CRLF);

            writer.flush();

            //Get Request
            BufferedReader response = new BufferedReader(new InputStreamReader(getRequest.getInputStream()));
            String line;

            // Print response to the client
            while ((line = response.readLine()) != null)
            {
                mPrintStream.println(line);
            }

            writer.close();
            response.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void sendRequest(Request request)
    {
        try
        {
            String pageContents = mServer.getCachedPage(request.getURL());

            if(pageContents != null)
            {
                System.out.println("Pages contents: ");
                System.out.println(pageContents);
                mPrintStream.println(pageContents);
            }
            else
            {
                pageContents = "";
                InetAddress address = InetAddress.getByName(request.getHostName());
                Socket requestSocket = new Socket(address, 80);

                if(request.getHeaderField("Accept").contains("image/"))
                {
                }

                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(requestSocket.getOutputStream(), "UTF-8")
                );

                writer.write(request.generateRequestString());
                writer.flush();

                //Get Request
                BufferedReader response = new BufferedReader(new InputStreamReader(requestSocket.getInputStream(), "UTF-8"));
                String line;

                // Print response to the client
                while ((line = response.readLine()) != null)
                {
//                if(line.equals("\r\n"))
//                    continue;

                    mPrintStream.println(line);
                    pageContents += line + CRLF;
                    System.out.println("reader: " + line);
                }

                //mServer.addPageToCache(request.getURL(), pageContents);
                writer.close();
                response.close();
                requestSocket.close();
            }
            mServer.clientDisconnected(mClientID);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}