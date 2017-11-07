import javax.xml.ws.http.HTTPException;

/**
 * Created by mantis on 2/18/15.
 */
public class ErrorCode extends HTTPException
{
    private String mErrorMessage;

    public ErrorCode(int statusCode)
    {
        super(statusCode);
    }


    public String generateErrorCode()
    {
        StringBuilder errorMessage = new StringBuilder();
        return null;
    }
}