import javax.xml.ws.http.HTTPException;

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
