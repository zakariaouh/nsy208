package nesmid.util;
/**
 * 
 * @author djamel bellebia
 *
 */
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;

public class Func extends Logger
{
	public static byte[] resizeByteArray(byte[] _array, int _newSize)
	{
		byte[] newArray=new byte[_newSize];
		System.arraycopy(_array,0,newArray,0,(_array.length < _newSize ? _array.length : _newSize));
		return newArray;
	}
	 
	public static String[] stringToArray(String a,String delimeter) 
	{
		String c[] =null;
		if(a!=null)
		{
		c =new String[0];
		
		String b=a;
		
		while (true)
		{
			int i=b.indexOf(delimeter);
			
			String d=b;
			
			if (i>=0)
				d=b.substring(0,i);
			
			String e[]=new String[c.length+1];
			
			for (int k=0;k<c.length;k++)
				e[k]=c[k];
			
			e[e.length-1]=d;
			
			c=e;
			
			b=b.substring(i+delimeter.length(),b.length());
		
			if (b.length()<=0 || i<0 )
				break;
		}
		
		}
		return c;
		
	}
	
	public static Hashtable stringToHashtable(String a,String delimeter)
	{		
		Hashtable c = new Hashtable();
		try
		{
			String b=a;

			while (true)
			{
				int i=b.indexOf(delimeter);
		
				String d=b;
			
				if (i>=0)
					d=b.substring(0,i);
			
				c.put(new String(d), new String(d));
			
				b=b.substring(i+delimeter.length(),b.length());
	
				if (b.length()<=0 || i<0 )
					break;
			}
		}
		catch(Exception e)
		{
			System.out.println("Exception @ Functions.stringToHashtable" + e );
		}
		
		return c;
	}
	
	public static String getCurrentDate()
	{
		Calendar calendar = Calendar.getInstance();
		return calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH)+1) + "-" + calendar.get(Calendar.DAY_OF_MONTH) ;
	}
	
	public static String getCurrentTime()
	{
		StringBuffer valRet = new StringBuffer("");
		
		Calendar calendar = Calendar.getInstance();
	    int hour    = calendar.get(Calendar.HOUR_OF_DAY);
	    int minute  = calendar.get(Calendar.MINUTE);
	    int second  = calendar.get(Calendar.SECOND);
	    	    
	    valRet.append(hour + ":");

	    if(minute < 10)
	    	valRet.append("0");
      
	    valRet.append(Integer.toString(minute));
	    valRet.append(":");

      	if(second < 10)
      		valRet.append("0");

      	valRet.append(Integer.toString(second));
		
		return valRet.toString();
	}
	
	public static String arrayToString(Object[] array)
	{
		StringBuffer valRet = new StringBuffer("{");
		
		for(int i= 0; i < array.length; i++)
		{
			if(i>0)
				valRet.append("," + array[i].toString());
			else
				valRet.append(array[i].toString());
		}
		
		valRet.append("}");
		
		return valRet.toString();
	}
	
	public static String stackToString(Stack table)
	{
		StringBuffer valRet = new StringBuffer("");
		
		int i =0;
		
		for(Enumeration e = table.elements(); e.hasMoreElements();)
		{
			if(i>0)
				valRet.append( " ; " +  e.nextElement()  ) ;
			else
				valRet.append(  e.nextElement() ) ;
			
			i++;
		}
				
		valRet.append("");
		
		return valRet.toString();
	}
	
	public static String hastableToString(Hashtable table)
	{
		StringBuffer valRet = new StringBuffer("");
		
		int i =0;
		
		for(Enumeration e = table.elements(); e.hasMoreElements();)
		{
			if(i>0)
				valRet.append( " ; " +  e.nextElement()  ) ;
			else
				valRet.append(  e.nextElement() ) ;
			
			i++;
		}
				
		valRet.append("");
		
		return valRet.toString();
	}
	
	public static String hastableWithKeysToString(Hashtable table)
	{
		StringBuffer valRet = new StringBuffer("");
		
		int i =0;
		Enumeration keys = table.keys();
		for(Enumeration e = table.elements(); e.hasMoreElements();)
		{
			if(i>0)
				valRet.append( " ;\n" + keys.nextElement() +"=" + e.nextElement()  ) ;
			else
				valRet.append( keys.nextElement() +"=" + e.nextElement() ) ;
			
			i++;
		}
				
		valRet.append("");
		
		return valRet.toString();
	}
	
	public static String dump(byte[] data) throws Exception
	{
		StringBuffer valRet = new StringBuffer();
	
		for(int i=0; i < data.length; i++)
		{
			valRet.append((char)data[i]);
		}
		return valRet.toString();
			
	}
	
	public void println(String str) 
	{
		System.out.println(str);
	}
	
}
