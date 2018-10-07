package nesmid.exception;

public class ConcernRuntimeException extends RuntimeException 
{
	public ConcernRuntimeException (Throwable cause)
	{	
		super(cause.getMessage());
	}
}
