//Imports Socket object input and output for files and data streams and terminal input.
import java.net.Socket;
import java.net.ServerSocket;
import java.io.*;
import java.util.Scanner;

/*Class for the server application. Looks for a client and once a connection is established
 *commands from the client can be sent to be executed by the sever. The main purpose of this 
 *server is file transfer but support for other commands are provided. This server is also
 *secured via a username and password.*/
public class Server{

	//Nested user class contains information for user login.
	private static class User{

		private boolean validUser;
		private String password;
		private boolean loggedIn;

		//Always created by an empty constructor as no data is needed before a login attempt.
		User(){}
	}

	private ServerSocket serverS;
	private Socket VSFTPServer;
	private User userData;
	private File requestedFile;
	private boolean run;


	/*Constructs a server object where a user is not logged in. This server is a socket 
	 *bound to port number port that is listening for a connection.
	 *(1: Socket, 2: Bind, 3: Listen)*/ 
	Server(int port) throws IOException{
		this.serverS = new ServerSocket(port);
		this.run = true;
	}

	/*Server attempts to establish a connection if successful a message is sent to greet the 
	 *client and the method returns true. If an error occurs and a connection was established
	 *error message is sent to the client and the method returns false. If no connection is
	 *established the server terminates execution.(4: Accept)*/
	public boolean establishConnection() throws IOException{
		try{
			//Looking to establish a connection.
			System.out.println("looking for connection...");

			//Accepts and establishes a connection.
			this.VSFTPServer = this.serverS.accept();
			System.out.println("connection established\nwaiting for command...");

			//Garbage collects the no longer needed serverSocket.
			this.serverS = null;

			//Creates a user object for the client.
			this.userData = new User();

			//Sends a response code and message to the client.
			sendMessage('+', "Hello from the VSFTP Service");
			return true;

		}catch(IOException ioE){

			if(this.VSFTPServer != null && this.VSFTPServer.isConnected()){

				System.out.println("error encountered\nterminating...");
				//Sends a response code and message to the client.
				sendMessage('-', "VSFTP Server is out for lunch");

				this.VSFTPServer.close();
			}
			//Error and no connection.
			else{

				System.out.println("error in connection\nterminating...");
				this.VSFTPServer.close();
			}

			return false;
		}
	}

	/*The server receives a request from the client in the form of a command 
	 *<CMD> <ARGS <NULL>. (6: Receive).*/
	public boolean receiveRequest(){
		try{

			InputStreamReader input = new InputStreamReader(this.VSFTPServer.getInputStream());
			BufferedReader buffer = new BufferedReader(input);

			//Splits the command from the argument.
			String[] cmdArgs = buffer.readLine().split(" ");
			String cmd = cmdArgs[0];
			String arg = "";
			if(cmdArgs.length > 1) arg = cmdArgs[1];

			//Sends the request to be processed.
			processRequest(cmd, arg);
			return true;

		}catch(IOException ioE){

			return false;
		}
	}

	/*Processes a request consisting of a one of 6 commands (refer to documentation) and 
	 *its args. (9: Send).*/
	private void processRequest(String cmd, String arg) throws IOException{

		/*Checks if a file was requested if not any command can be processed if a file
		 *was requested only a send command is allowed next otherwise a time out occurs.*/
		if(this.requestedFile != null){

			if(cmd.equals("SEND") == false && cmd.equals("send") == false){
				
				this.requestedFile = null;

				System.out.println("client did not issue send, retrieval canceled\nwaiting for command...");
				//Sends a response code and message to the client.
				sendMessage('-', "Send request not received retrieval terminated");

				return;
			}
		}


		/*Username command client sends the USER command followed by a username. The users.txt
		 *file is checked for the username (the user name is case sensitive). If valid the username
		 *flag is set to true and the client will be prompted for a password. Otherwise the user will 
		 *be notified of the invalid Username.*/
		if(cmd.equals("USER") || cmd.equals("user")){
			
			if(this.userData.loggedIn){
				
				System.out.println("client already logged-in\nwaiting for command...");
				//Sends a response code and message to the client.
				sendMessage('-', "You are already logged-in");

				return;
			}

			File users = new File("data/users.txt");
			Scanner usersData =  new Scanner(users);
			
			while(usersData.hasNext()){
				if(usersData.next().equals(arg)){

					//Sets the username field to true and stores the password.
					this.userData.validUser = true;
					this.userData.password = usersData.next();

					System.out.println("valid username\nrequesting password...");
					//Sends a response code and message to the client.
					sendMessage('+', "User-id valid, send password");
					usersData.close();

					return;
				}
			}

			System.out.println("client sent invalid user-id\nwaiting for command...");
			//Sends a response code and message to the client.
			sendMessage('-', "Invalid user-id, try again");

			usersData.close();
		}

		/* Password command client sends the server a PASS command followed by a password arg.
		 *Checks for a matching password to the given username, if a correct password is given 
		 *the user us logged in. If not the user is notified of an incorrect password. If the 
		 *client attempts to use this command before a valid username is provided the client will
		 *be notified. Passwords are case sensitive*/
		else if(cmd.equals("PASS") || cmd.equals("pass")){
			
			if(this.userData.loggedIn){

				System.out.println("client already logged-in\nwaiting for command...");
				//Sends a response code and message to the client.
				sendMessage('-', "You are already logged-in");

				return;
			}
			if(this.userData.validUser == false){

				System.out.println("client attempted password without user-id\nwaiting for command...");
				//Sends a response code and message to the client.
				sendMessage('-', "No user-id provided, enter a valid user-id");

				return;
			}
			else{

				if(arg.equals(userData.password)){

					this.userData.loggedIn = true;

					System.out.println("password accepted\nwaiting for command...");
					//Sends a response code and message to the client.
					sendMessage('!', "Logged in\nPassword is ok and you can begin file transfers.");

					return;
				}

				System.out.println("client sent invalid password\nrequesting password...");
				//Sends a response code and message to the client.
				sendMessage('-', "Wrong password, try again");
			}
		}

		/*List command client sends the LIST command if there are any arguments sent they 
		 *are discarded. Lists all the content in the directory of the server. Only allowed 
		 *if the user is logged-in.*/
		else if(cmd.equals("LIST") || cmd.equals("list")){
			
			if(!isloggedIn()) return;

			else{

				//Pulls all file names from the current directory.
				String cwd = new File("root/").getAbsolutePath();
				File directory = new File(cwd);
				File[] files = directory.listFiles();

				//Creates a string of the path and files to be sent to the client.
				String listResponse;
				listResponse = String.format("+Files Available:");
				for(int x=0;x<files.length;x++){

					//Only sends file names not directories.
					if(files[x].isFile()){
						listResponse = String.format(listResponse + "\n" + files[x].getName());
					} 
				}

				System.out.println("client requested and received file list of current directory\nwaiting for command...");
				//Sends a response code and message to the client.
				sendMessage('\0', listResponse);
			}
		}

		/*Kill command client sends the KILL command followed by the file the client wishes to
		 *delete as an arg. If the user is not logged-in the client is notified. If the file does
		 *not exist the user is notified. Otherwise the file is deleted.*/
		else if(cmd.equals("KILL") || cmd.equals("kill")){
			
			if(!isloggedIn()) return;
			
			else{

				//Pulls all file names from the current directory.
				String cwd = new File("root/").getAbsolutePath();
				File directory = new File(cwd);
				File[] files = directory.listFiles();

				for(int x=0;x<files.length;x++){

					if(files[x].getName().equals(arg)){

						//Prevents the deletion of the files the server depends on.
						if(files[x].getName().equals("Server.java") || files[x].getName().equals("Server.class") || files[x].getName().equals("users.txt")){

							System.out.println("client attempted to delete file (protected file)\nwaiting for command...");
							//Sends a response code and message to the client.
							sendMessage('-', "Not deleted because users cannot delete this file");
							
							return;
						}

						//Deletes the file from the directory.
						files[x].delete();

						System.out.println("file " + files[x].getName() + " deleted\nwaiting for command...");
						//Sends a response code and message to the client.
						sendMessage('+', String.format(files[x].getName() + " deleted"));

						return;
					}
				}

				System.out.println("client attempted to delete file (file does not exist)\nwaiting for command...");
				//Sends a response code and message to the client.
				sendMessage('-',"Not deleted because file does not exist");
			}	
		}

		/*Command for file transfer request RETR. If the user is not logged in the client is
		 *notified. If the file does not exist the client is notified. Otherwise the file size
		 *in bytes is send the server then awaits a SEND command before sending the file.*/
		else if(cmd.equals("RETR") || cmd.equals("retr")){
			
			if(!isloggedIn()) return;
			
			else{

				//Pulls all file names from the current directory.
				String cwd = new File("root/").getAbsolutePath();
				File directory = new File(cwd);
				File[] files = directory.listFiles();

				for(int x=0;x<files.length;x++){

					if(files[x].getName().equals(arg)){
						byte[] byteArr = new byte[(int)files[x].length()];

						System.out.println("client requested file " + files[x].getName() + " of size " + byteArr.length + "\nwaiting for send...");
						//Sends a response code and message to the client.
						sendMessage('#',Integer.toString(byteArr.length));

						this.requestedFile = files[x];
						return;
					}
				}

				System.out.println("client requested a file that does not exist\nwaiting for command...");
				//Sends a response code and message to the client.
				sendMessage('-', "File doesn't exist");
			}
		}

		/*Command to send a file to the client SEND if the user is not logged in the client
		 *is notified. If there was no or an unsuccessful RETR command before this the client
		 *is told to issue a RETR before this command. Else the file is sent.*/
		//Ends the connection and closes the socket, ends the program.
		else if(cmd.equals("SEND") || cmd.equals("send")){
			
			if(!isloggedIn()) return;
			
			else if(this.requestedFile == null){

				//Sends a response code and message to the client.
				sendMessage('-', "Issue a retrieval request first");
			}

			else{

				System.out.println("send received, sending file...");
				//Sends a response code and message to the client.
				sendMessage('+', "Sending file");

				if(sendFile()){

					System.out.println("file sent\nwaiting for command...");

					try{
						Thread.sleep(100);
					}catch(Exception e){
						return;
					}
					//Sends a response code and message to the client.
					sendMessage('+', "File sent");

					this.requestedFile = null;
				}

				else{

					System.out.println("file send error transfer ended\nwaiting for command...");
					//Sends a response code and message to the client.
					sendMessage('-', "Error on send");

					this.requestedFile = null;
				}
			}
		}

		//Ends communication and closes the sockets. (10: Close)
		else if(cmd.equals("DONE") || cmd.equals("done")){

			System.out.println("connection ended\nclosing application");
			//Sends a response code and message to the client.
			sendMessage('+', "GoodBye");

			this.VSFTPServer.close();
			this.run = false;
		}

		else{

			System.out.println("invalid command\nwaiting for command...");
			//Sends a response code and message to the client.
			sendMessage('-', "Invalid command");
		}
	}

	/*Sends messages to the client. The message consists of one of 4 response codes (refer to
	 *documentation) and a message, with a terminating null (9: Send)*/
	private void sendMessage(char responseCode, String message) throws IOException{

		PrintWriter output = new PrintWriter(this.VSFTPServer.getOutputStream());

		output.println(responseCode + message + "NULL");
		output.flush();
	}

	//Sends a file to the client after a successful RETR request.
	private boolean sendFile(){
		try{

			FileInputStream fileIn = new FileInputStream(this.requestedFile);

			//byte[] byteArr = new byte[(int)this.requestedFile.length()];
			//fileIn.read(byteArr, 0, byteArr.length);

			byte[] byteArr = new byte[1000];
			OutputStream output = this.VSFTPServer.getOutputStream();

			//output.write(byteArr, 0, byteArr.length);
			//output.flush();

			int count;
			while ((count = fileIn.read(byteArr)) > 0)
			{
			    output.write(byteArr, 0, count);
			}

			output.flush();

			return true;

		}catch(Exception e){

			return false;
		}
	}

	//Checks if user is logged in. Sends a message if the client is not logged in.
	private boolean isloggedIn() throws IOException{

		if(this.userData.loggedIn) return true;

		//Sends a message that the user needs to login.
		System.out.println("client attempted to access restricted information\nwaiting for command...");
		//Sends a response code and message to the client.
		sendMessage('-', "Request denied please login");

		return false;

	}

	/*Creates the server on port 50001 and establishes a connection (accept).
	 *Then the server continuously runs waiting for requests.*/
	public static void main(String[] args) {
		try{
			Server server = new Server(50001);

			server.establishConnection();

			while(server.run){
				server.receiveRequest();
			}

		}catch(Exception e){
		}
	}
}