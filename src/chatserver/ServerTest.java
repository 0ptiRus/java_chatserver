package chatserver;

import static org.junit.jupiter.api.Assertions.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServerTest {
	private final static int basePort = 12345;
	private static Server server; 
	private static Socket client;
	private static DataOutputStream client_dos;
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		server = new Server(basePort);
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		
	}

	@BeforeEach
	void setUp() throws Exception {
		client = new Socket("localhost", basePort);
		client_dos = new DataOutputStream(client.getOutputStream());
		server.listenAsync();
	}

	@AfterEach
	void tearDown() throws Exception {
		client.close();
		server.closeServer();
		server = new Server(basePort);
	}
	

	@Test
	void testConstructorWithValidArguments() 
	{
		assertTrue(server.isAlive());
		
	}

	@Test
	void testConstructorWithInvalidArguemnts() 
	{
		Server server2 = null;
		try
		{
			server2 = new Server(basePort);
			fail("Server was created successfully!");
		}
		catch(IOException e)
		{
			assertNull(server2);
		}
	}
	
	@Test
	void testMessageOrder()
	{
		server.addMessage("hi");
		server.addMessage("hello");
		assertEquals("hi", server.getMessages().get(1));
		assertEquals("hello", server.getMessages().get(0));
	}
	
	@Test
	void testMessageOverflow()
	{
		for(int i = 0; i < 15; i++)
		{
			server.addMessage("lul");
		}
		assertEquals(10, server.getMessages().size());
	}

	@Test
	void testClientConnectionHandling() {
		try {
			Socket client2 = new Socket("localhost", basePort);
		} catch (IOException e) {
			fail("CLients couldn't connect: " + e.getMessage());
		}
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			fail("Test interrupted: " + e.getMessage());
		}
		assertEquals(2, server.getClients().size());
	}

	@Test
	void testServe() {
		try 
		{
			client_dos.writeUTF("Hello!");
		} catch (IOException e) {
			fail("Couldn't send message: " + e.getMessage());
		}
		
		boolean messageReceived = false;
		while(!messageReceived)
		{
			if(server.getMessages().size() == 1)
			{
				messageReceived = true;
				break;
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				fail("Test interrupted: " + e.getMessage());
			}
		}
		
		assertTrue(messageReceived);
		assertEquals(1, server.getMessages().size());
		assertEquals("Hello!", server.getMessages().get(0));
	}
	
	@Test
	void testServeGettingInvalidMessage()
	{
		try 
		{
			client_dos.writeLong(100000000);
		} catch (IOException e) {
			fail("Couldn't send object: " + e.getMessage());
		}
		
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			fail("Test interrupted: " + e.getMessage());
		}
		assertEquals(0, server.getMessages().size());
	}
	
	@Test
	void testHandlingClientDisconnection()
	{
		try {
			client.close();
			Thread.sleep(1);
		} catch (IOException e) {
			fail("Couldn't close client: " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Test interrupted: " + e.getMessage());
		}
		
		assertEquals(0, server.getClients().size());
	}

}
