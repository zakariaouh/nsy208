package nesmid.util;

public abstract class Logger 
{
	public static void init(String loggerClassName) throws Exception
	{
		if(out==null)
		{
			Class cls = Class.forName(loggerClassName);
			
			out = (Logger)cls.newInstance();
		}
	}
	
	public  abstract void println(String str);
	
	public static Logger out;
}
