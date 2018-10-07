package nesmid.exception;
/**
 * 
 * @author cbonar
 *
 */
public class BusinessException extends Exception 
{
	public BusinessException()
	{
		
	}
	
	public BusinessException(String error)
	{
		super(error);
		
	}

}
