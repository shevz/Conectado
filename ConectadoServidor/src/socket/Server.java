//Advanced Programming April 2016
//University of Technology
//Shevaun Mckenzie, C'Lloyd Walker, Dwayne Bryan,Andrew Gray,Romone Rose

package socket;

import java.io.*;
import java.net.*;
import java.util.Properties;

import utilities.Logging;


public class Server implements Runnable {

	public ServerClientThread clients[];
	public ServerSocket server = null;
	public Thread thread = null;
	private utilities.Props prop = new utilities.Props();
	private Properties props = prop.getProps(); //
	public int port = Integer.parseInt(props.getProperty("portNumber"));
	public int clientCount = 0;
	public Database db;
	private String filePath = props.getProperty("dbPath");

	public Server() {

		clients = new ServerClientThread[50];
		db = new Database(filePath);

		try {
			server = new ServerSocket(port);
			System.out.println("Server started on IP : " + InetAddress.getLocalHost() + ", Port : "
					+ server.getLocalPort() + "\n");
			Logging.getLogger()
					.info("Server started on IP : " + InetAddress.getLocalHost() + ", Port : " + server.getLocalPort());
			start();
		} catch (IOException ioe) {
			System.out.println("Can not bind to port : " + port + "\nRetrying\n");
			Logging.getLogger().error("Can not bind to port : " + port + "\nRetrying\n");
			RetryStart(0);// retries but this time selects a random open port
							// and uses it
		} catch (Exception ex) {
			ex.printStackTrace();
			Logging.getLogger().error(ex);
		}
	}

	public Server(int Port) {

		clients = new ServerClientThread[50];
		port = Port;
		db = new Database(filePath);

		try {
			server = new ServerSocket(port);
			System.out.println(
					"Server startet. IP : " + InetAddress.getLocalHost() + ", Port : " + server.getLocalPort());
			Logging.getLogger()
					.info("Server startet. IP : " + InetAddress.getLocalHost() + ", Port : " + server.getLocalPort());
			start();
		} catch (IOException ioe) {
			System.out.println("\nCan not bind to port " + port + ": " + ioe.getMessage());
			Logging.getLogger().error("\nCan not bind to port " + port + ": " + ioe.getMessage());
		} catch (Exception ex) {
			ex.printStackTrace();
			Logging.getLogger().error(ex);
		}
	}

	public void run() {
		while (thread != null) {
			try {
				System.out.println("\nWaiting for a client ...");
				addThread(server.accept());
			} catch (Exception ioe) {
				System.out.println("\nServer accept error: \n");
				Logging.getLogger().error("\nServer accept error: \n");
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
			// thread.stop();
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
			} else if (msg.type.equals("group")) {
				SendGroupList(msg.type, msg.sender, msg.content, msg.group);
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
	
	//used to send broadcast messages
	public void Announce(String type, String sender, String content) {
		Message msg = new Message(type, sender, content, "All");
		for (int i = 0; i < clientCount; i++) {
			clients[i].send(msg);
		}
	}
	
	//
	public void SendUserList(String toWhom) {
		for (int i = 0; i < clientCount; i++) {
			findUserThread(toWhom).send(new Message("newuser", "SERVER", clients[i].username, toWhom));
		}
	}

	// used to send messages to groups by looping through the individuals in the
	// group
	public void SendGroupList(String type, String sender, String content, String[] toWhom) {
		for (int i = 0; i < toWhom.length; i++) {
			for (int j = 0; j < clientCount; j++) {
				findUserThread(toWhom[i]).send(new Message(type, sender, clients[j].username, toWhom[i]));
			}
		}
	}

	public ServerClientThread findUserThread(String usr) {
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
			ServerClientThread toTerminate = clients[pos];
			System.out.println("\nRemoving client thread " + ID + " at " + pos + "\n");
			Logging.getLogger().info("\nRemoving client thread " + ID + " at " + pos);
			if (pos < clientCount - 1) {
				for (int i = pos + 1; i < clientCount; i++) {
					clients[i - 1] = clients[i];
				}
			}
			clientCount--;
			try {
				toTerminate.close();
			} catch (IOException ioe) {
				System.out.println("\nError closing thread: " + ioe + "\n");
				Logging.getLogger().error("\nError closing thread: " + ioe);
			} catch (Exception ex) {
				ex.printStackTrace();
				Logging.getLogger().error(ex);
			}
			toTerminate.stop();
			// toTerminate=null;
		}
	}

	private void addThread(Socket socket) {
		if (clientCount < clients.length) {
			System.out.println("\nClient accepted: " + socket + "\n");
			Logging.getLogger().info("\nClient accepted: " + socket);
			clients[clientCount] = new ServerClientThread(this, socket);
			try {
				clients[clientCount].open();
				clients[clientCount].start();
				clientCount++;
			} catch (IOException ioe) {
				System.out.println("\nError opening thread: " + ioe + "\n");
				Logging.getLogger().error("\nError opening thread: " + ioe);
			} catch (Exception e) {
				e.printStackTrace();
				Logging.getLogger().error(e);
			}
		} else {
			System.out.println("\nClient refused: maximum " + clients.length + " reached.");
			Logging.getLogger().info("\nClient refused: maximum " + clients.length + " reached.");
		}
	}

	public void RetryStart(int port) {
		if (this != null) {
			this.stop();
		}
		new Server(port);
	}

	public static void main(String args[]) {
		new Server();
	}
}
