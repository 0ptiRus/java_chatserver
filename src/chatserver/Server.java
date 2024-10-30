package chatserver;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.io.*;

public class Server 
{
	private List<Socket> clients = new ArrayList<>();
	private List<DataOutputStream> clientOutputStreams = new ArrayList<>();
	private LinkedList<String> messages = new LinkedList<>();
	private int max_messages = 10;
	private ServerSocket listener = null;
	
	public Server(int port) throws IOException
	{
		listener = new ServerSocket(port);
		System.out.println("Server instantiated.");
	}
	
	boolean isAlive() { return listener != null; }
	
	public List<Socket> getClients() { return clients; }
	
	public LinkedList<String> getMessages() { return messages; }
	
	public void listen()
	{
		while(!listener.isClosed())
		{
			try 
			{
				System.out.println("Waiting for connections...");
				Socket client = listener.accept();
				System.out.println("Client connected: " + client.getRemoteSocketAddress());
				synchronized(clients)
				{
					if (!clients.contains(client)) 
					{
						clients.add(client);
						clientOutputStreams.add(new DataOutputStream(client.getOutputStream()));
					} 
					else 
					{
						System.out.println("Client already exists, ignoring duplicate connection attempt.");
					}
				}
				serveAsync(client);
			} 
			catch (IOException e) 
			{
				System.out.println("Error while listening for clients!");
				e.printStackTrace();
			}	
		}
	}
	
	public CompletableFuture<Void> listenAsync()
	{
		return CompletableFuture.runAsync(this::listen);
	}
	
	private void validate()
	{
		if(messages.size() >= max_messages)
		{
			synchronized(messages)
			{
				messages.removeFirst();
			}
		}		
	}
	
	public void addMessage(String message)
	{
		validate();
		
		synchronized(messages)
		{
			messages.addFirst(message);			
		}					
	}
	
	public void serve(Socket client) {
	    if (client != null) {
	        DataOutputStream dos = null;
	        DataInputStream dis = null;
	        try {
	            dos = new DataOutputStream(client.getOutputStream());
	            dis = new DataInputStream(client.getInputStream());
	        } catch (IOException e) {
	            System.out.println("Couldn't instantiate streams: " + e.getMessage());
	            return;
	        }

	        while (!client.isClosed()) 
	        {
	            try 
	            {
	            	boolean validMessage = false;
	                String message = dis.readUTF();
	                if(message.getClass() == String.class && !message.isEmpty())
	                {
	                	validMessage = true;	                	
	                }
	                if(validMessage)
	                {
	                	System.out.println("Received message: " + message);
	                	addMessage(message);
	                	
	                	synchronized(clientOutputStreams) 
	                	{
	                		for (int i = 0; i < clients.size(); i++) 
	                		{
	                			if (!clients.get(i).equals(client)) 
	                			{
	                				clientOutputStreams.get(i).writeUTF(message);
	                				clientOutputStreams.get(i).flush();
	                			}
	                		}
	                	}
	                	System.out.println("Sent message to other clients");	                	
	                }
	            } 
	            
	            catch(UTFDataFormatException ex)
	            {
	            	System.out.println("Expected string, but received invalid data!");
	            }
	         
	            catch (EOFException e) 
	            {
	                System.out.println("Client disconnected: " + client.getRemoteSocketAddress());
	                break;  
	            }
	            catch (IOException e) 
	            {
	                e.printStackTrace();
	                break;  
	            }
	        }

	        synchronized(clientOutputStreams)
	        {
	        	try 
	        	{
					clientOutputStreams.remove(client.getOutputStream());
				}
	        	catch (IOException e) 
	        	{
					e.printStackTrace();
				}
	        }
	        synchronized (clients) 
	        {
	            clients.remove(client);
	        }
	        
	        System.out.println("Cleaned up after client " + client.getRemoteSocketAddress());
	        
	        try 
	        {
	            client.close();
	            System.out.println("Closed connection to client: " + client.getRemoteSocketAddress());
	        } 
	        catch (IOException e) 
	        {
	            System.err.println("Error closing client socket: " + e.getMessage());
	        }
	    } else {
	        System.out.println("Invalid client given!");
	    }
	}
	
	public boolean closeServer()
	{
		if(listener.isClosed())
		{
			return false;
		}
		
		try
		{
			listener.close();
			return true;
		}
		catch(SocketException soe)
		{
			return true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public CompletableFuture<Void> serveAsync(Socket client)
	{
		return CompletableFuture.runAsync(() -> serve(client));
	}
}
