
import java.io.*;
import java.net.*;
import java.util.Properties;

class ServerThread extends Thread {

	public Server server = null;
	public Socket socket = null;
	public int ID = -1;
	public String username = "";
	public ObjectInputStream streamIn = null;
	public ObjectOutputStream streamOut = null;
	private volatile boolean running = true;	

	public ServerThread(Server _server, Socket _socket) {
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
			System.out.println("Exception [SocketClient : send(...)]");
		}
	}

	public int getID() {
		return ID;
	}

	public void run() {
		System.out.println("\nServer Thread " + ID + " running.");
		while (running) {
			try {
				Message msg = (Message) streamIn.readObject();
				server.handle(ID, msg);
			} catch (Exception ioe) {
				System.out.println(ID + " ERROR reading: " + ioe.getMessage());
				server.remove(ID);
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
		running = false;
	}
}

public class Server implements Runnable {

	public ServerThread clients[];
	public ServerSocket server = null;
	public Thread thread = null;
	private utilities.Props prop= new utilities.Props();
	private Properties props=prop.getProps();
	public int clientCount = 0, port=Integer.parseInt(props.getProperty("portNumber"));
	public Database db;
	private String filePath=props.getProperty("dbPath");	

	public Server() {

		clients = new ServerThread[50];
		db = new Database(filePath);

		try {
			server = new ServerSocket(port);
			port = server.getLocalPort();
			System.out.println(
					"Server startet. IP : " + InetAddress.getLocalHost() + ", Port : " + server.getLocalPort());
			start();
		} catch (IOException ioe) {
			System.out.println("Can not bind to port : " + port + "\nRetrying");
			RetryStart(0); //when 0 passes through it tell the program to take any available port
		}
	}

	public Server(int Port) {

		clients = new ServerThread[50];
		port = Port;
		db = new Database(filePath);

		try {
			server = new ServerSocket(port);
			port = server.getLocalPort();
			System.out.println(
					"Server startet. IP : " + InetAddress.getLocalHost() + ", Port : " + server.getLocalPort());
			start();
		} catch (IOException ioe) {
			System.out.println("\nCan not bind to port " + port + ": " + ioe.getMessage());
		}
	}

	public void run() {
		while (thread != null) {
			try {
				System.out.println("\nWaiting for a client ...");
				addThread(server.accept());
			} catch (Exception ioe) {
				System.out.println("\nServer accept error: \n");
				RetryStart(0);
			}
		}
	}

	public void start() {
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}

	public void stop() {
		if (thread != null) {
			thread = null;
		}
	}

	private int findClient(int ID) {
		for (int i = 0; i < clientCount; i++) {
			if (clients[i].getID() == ID) {
				return i;
			}
		}
		return -1;
	}

	public synchronized void handle(int ID, Message msg) {
		if (msg.content.equals(".bye")) {
			Announce("signout", "SERVER", msg.sender);
			remove(ID);
		} else {
			if (msg.type.equals("login")) {
				if (findUserThread(msg.sender) == null) {
					if (db.checkLogin(msg.sender, msg.content)) {
						clients[findClient(ID)].username = msg.sender;
						clients[findClient(ID)].send(new Message("login", "SERVER", "TRUE", msg.sender));
						Announce("newuser", "SERVER", msg.sender);
						SendUserList(msg.sender);
					} else {
						clients[findClient(ID)].send(new Message("login", "SERVER", "FALSE", msg.sender));
					}
				} else {
					clients[findClient(ID)].send(new Message("login", "SERVER", "FALSE", msg.sender));
				}
			} else if (msg.type.equals("message")) {
				if (msg.recipient.equals("All")) {
					Announce("message", msg.sender, msg.content);
				} else {
					findUserThread(msg.recipient).send(new Message(msg.type, msg.sender, msg.content, msg.recipient));
					clients[findClient(ID)].send(new Message(msg.type, msg.sender, msg.content, msg.recipient));
				}
			} else if (msg.type.equals("test")) {
				clients[findClient(ID)].send(new Message("test", "SERVER", "OK", msg.sender));
			} else if (msg.type.equals("signup")) {
				if (findUserThread(msg.sender) == null) {
					if (!db.userExists(msg.sender)) {
						db.addUser(msg.sender, msg.content);
						clients[findClient(ID)].username = msg.sender;
						clients[findClient(ID)].send(new Message("signup", "SERVER", "TRUE", msg.sender));
						clients[findClient(ID)].send(new Message("login", "SERVER", "TRUE", msg.sender));
						Announce("newuser", "SERVER", msg.sender);
						SendUserList(msg.sender);
					} else {
						clients[findClient(ID)].send(new Message("signup", "SERVER", "FALSE", msg.sender));
					}
				} else {
					clients[findClient(ID)].send(new Message("signup", "SERVER", "FALSE", msg.sender));
				}
			} else if (msg.type.equals("upload_req")) {
				if (msg.recipient.equals("All")) {
					clients[findClient(ID)]
							.send(new Message("message", "SERVER", "Uploading to 'All' forbidden", msg.sender));
				} else {
					findUserThread(msg.recipient)
							.send(new Message("upload_req", msg.sender, msg.content, msg.recipient));
				}
			} else if (msg.type.equals("upload_res")) {
				if (!msg.content.equals("NO")) {
					String IP = findUserThread(msg.sender).socket.getInetAddress().getHostAddress();
					findUserThread(msg.recipient).send(new Message("upload_res", IP, msg.content, msg.recipient));
				} else {
					findUserThread(msg.recipient)
							.send(new Message("upload_res", msg.sender, msg.content, msg.recipient));
				}
			}
		}
	}

	public void Announce(String type, String sender, String content) {
		Message msg = new Message(type, sender, content, "All");
		for (int i = 0; i < clientCount; i++) {
			clients[i].send(msg);
		}
	}

	public void SendUserList(String toWhom) {
		for (int i = 0; i < clientCount; i++) {
			findUserThread(toWhom).send(new Message("newuser", "SERVER", clients[i].username, toWhom));
		}
	}

	public ServerThread findUserThread(String usr) {
		for (int i = 0; i < clientCount; i++) {
			if (clients[i].username.equals(usr)) {
				return clients[i];
			}
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	public synchronized void remove(int ID) {
		int pos = findClient(ID);
		if (pos >= 0) {
			ServerThread toTerminate = clients[pos];
			System.out.println("\nRemoving client thread " + ID + " at " + pos);
			if (pos < clientCount - 1) {
				for (int i = pos + 1; i < clientCount; i++) {
					clients[i - 1] = clients[i];
				}
			}
			clientCount--;
			try {
				toTerminate.close();
			} catch (IOException ioe) {
				System.out.println("\nError closing thread: " + ioe);
			}
			toTerminate.stop();
		}
	}

	private void addThread(Socket socket) {
		if (clientCount < clients.length) {
			System.out.println("\nClient accepted: " + socket);
			clients[clientCount] = new ServerThread(this, socket);
			try {
				clients[clientCount].open();
				clients[clientCount].start();
				clientCount++;
			} catch (IOException ioe) {
				System.out.println("\nError opening thread: " + ioe);
			}
		} else {
			System.out.println("\nClient refused: maximum " + clients.length + " reached.");
		}
	}
	public void RetryStart(int port){
        if(this != null)
        { 
        	this.stop(); 
        }
       new Server(port);
    }
	 public static void main(String args[]) {
		 Server server = new Server();
	 }

}
