package chatserver;

import java.io.IOException;

public class Main 
{
	public static void main(String[] args)
	{
		Server server = null;
		
		try 
		{
			server = new Server(12345);
		} 
		catch (IOException e) 
		{
			System.out.println("Couldnt instantiate server: " + e.getMessage());
		}
		
		if(server != null)
		{
			while(true)
			{
				server.listen();				
			}
		}
	}
}
