package nesmid.util;

public abstract class  RunnableWithContext implements Runnable 
{
	
	public java.util.Hashtable props = null;
	public abstract boolean init();
}
