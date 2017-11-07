import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Request
{
    private String mHostName;
    private String mPath;
    private String mFlag;
    private String mErrorCode;
    private ArrayList<String> mHeaders;
    private HashMap<String, String> mHeaderMap;
    boolean mValid;
    private final String CRLF = "\r\n";

    public String getURL()
    {
        return mHostName + mPath;
    }

    public String getFlag()
    {
        return mFlag;
    }

    public String getHostName()
    {
        return mHostName;
    }

    public String getPath()
    {
        return mPath;
    }

    public boolean isValid()
    {
        return mValid;
    }

    public Request()
    {
        mHostName = null;
        mPath = null;
        mFlag = null;
        mValid = false;
    }

    public Request(List<String> headers)
    {
        mHeaders = new ArrayList<String>();
        mHeaderMap = new HashMap<String, String>();
        buildRequest(headers);
    }

    private void buildRequest(List<String> headers)
    {
        if(headers.size() > 0)
        {
            String[] initialHeader = headers.get(0).split(" ");
            if(initialHeader.length > 2)
            {
                try
                {
                    mFlag = initialHeader[2];
                    if(mFlag.equals("HTTP/1.1") || !mFlag.equals("HTTP/1.0"))
                    {
                        setError(" ");
                        return;
                    }

                    URI uri = new URI(initialHeader[1]);
                    mHostName = uri.getHost();
                    mPath = uri.getPath();
                }
                catch (URISyntaxException e)
                {
                    try
                    {
                        String[] hostHeader = headers.get(1).split(" ");
                        URI uri = new URI(hostHeader[1]);
                        mPath = initialHeader[1];
                        mHostName = uri.getHost();

                    }
                    catch (Exception e2)
                    {
                        setError(" ");
                        return;
                    }
                }

                mValid = true;
                populateHeaderData(headers);

                String url = mHeaderMap.get("GET");
                int index = url.indexOf(" ");
            }
            else
            {
                setError("");
                return;
            }
        }
        else
        {
            setError("");
            return;
        }
    }

    private void populateHeaderData(List<String> headers)
    {
        String beginningHeader = "GET " + mPath + " " + mFlag + CRLF;

        // Add the first line of the request
        mHeaders.add(0, beginningHeader);
        mHeaderMap.put("GET", beginningHeader.substring(4));

        for(int i = 1; i < headers.size(); i++)
        {
            String line = headers.get(i);

            // Replaces instances of connection: keep-alive with close
            if(line.contains("Connection:") && !line.contains("Proxy"))
            {
                if(!line.equals("Connection: close"))
                {
                    mHeaders.add("Connection: close");
                    mHeaderMap.put("Connection", "close");
                    continue;
                }
            }
            if(line.equals("Accept-Encoding: gzip, deflate"))
            {
                continue;
            }

            // Get indices of spaces so key/value pairs can be stored
            int indexOfSpace = line.indexOf(" ");
            String key = line.substring(0, indexOfSpace - 1);
            String value = line.substring(indexOfSpace + 1);

            mHeaderMap.put(key, value + CRLF);
            mHeaders.add(headers.get(i) + CRLF);
        }
        System.out.println("Array list: " + Arrays.toString(mHeaders.toArray()));
    }


    public String getHeaderField(String key)
    {
        return mHeaderMap.get(key);
    }

    public String generateRequestString()
    {
        if(mValid)
        {
            StringBuilder builder = new StringBuilder();
            for(String line : mHeaders)
                builder.append(line);

            builder.append(CRLF);
            return builder.toString();
        }
        return null;
    }

    public Byte[] generateRequest()
    {
        if(mValid)
        {
            ArrayList<Byte> byteList = new ArrayList<Byte>();
            for(int i = 0; i < mHeaders.size(); i++)
            {
                int length = mHeaders.get(i).length();
                for(int j = 0; j < length; j++)
                    byteList.add((byte) mHeaders.get(i).charAt(j));
            }

            Byte[] byteArray = new Byte[byteList.size()];
            for(int i = 0; i < byteList.size(); i++)
                byteArray[i] = byteList.get(i);

            return byteArray;
        }

        return null;
    }

    private void setError(String errorCode)
    {
        mHostName = null;
        mPath = null;
        mFlag = null;
        mValid = false;
        mErrorCode = errorCode;
    }
}