package socket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import utilities.Logging;



class ServerClientThread extends Thread {

	public Server server = null;
	public Socket socket = null;
	public int ID = -1;
	public String username = "";
	public ObjectInputStream streamIn = null;
	public ObjectOutputStream streamOut = null;
	private volatile boolean running = true; // this boolean is used in efforts
												// to try end a thread
												// properly rather than using
												// deprecated code.

	public ServerClientThread(Server _server, Socket _socket) {
		super();
		server = _server;
		socket = _socket;
		ID = socket.getPort();
	}

	public void send(Message msg) {
		try {
			streamOut.writeObject(msg);
			streamOut.flush();
		} catch (IOException ex) {
			System.out.println("Exception [SocketClient : send(...)]\n");
			Logging.getLogger().error("Exception [SocketClient : send(...)]\n" + ex.toString());
		} catch (Exception e) {
			e.printStackTrace();
			Logging.getLogger().error(e);
		}
	}

	public int getID() {
		return ID;
	}

	public void run() {
		System.out.println("\nServer Thread " + ID + " running.\n");
		Logging.getLogger().info("\nServer Thread " + ID + " running.\n");
		while (running) {
			try {
				Message msg = (Message) streamIn.readObject();
				server.handle(ID, msg);
			} catch (Exception ioe) {
				System.out.println(ID + " ERROR reading: " + ioe.getMessage());
				Logging.getLogger().error(ioe);
				server.removeUser(ID);
				running = false;
			}
		}
	}

	public void open() throws IOException {
		streamOut = new ObjectOutputStream(socket.getOutputStream());
		streamOut.flush();
		streamIn = new ObjectInputStream(socket.getInputStream());
	}

	public void close() throws IOException {
		if (socket != null)
			socket.close();
		if (streamIn != null)
			streamIn.close();
		if (streamOut != null)
			streamOut.close();
	}
}