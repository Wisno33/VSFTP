//Imports Socket object input and output for files and data streams and terminal input.
import java.net.Socket;
import java.io.*;
import java.util.Scanner;

/*Class for the client application. The client attempts to connect to a server and once 
 *connected the client can send commands to the server. The server provides file transfer 
 *service. The client sends commands in the form of <CMD> <ARGS> <NULL> the commands are all 
 *4 ASCII characters.*/
public class Client{

	private Socket VSFTPClient;
	private String requestedFileName;
	private boolean run;

	/*Creates a client object. This client is a socket with the host address host 
	 *connected to port number port.(1: Socket, 5: Connect)*/
	Client(String host, int port) throws Exception{
		try{

			this.VSFTPClient = new Socket(host, port);
			this.run = true;

		}catch(IOException ioE){

			//Throws an exception if the host cannot be reached (such as invalid IP).
			System.out.println("Invalid Host\nterminating...");
			throw new Exception();
		}
	}

	/*Receives message from the server in the form of <response-code> <message> <NULL>.
	 *The message is then output if an error occurs in connection then the socket is closed.
	 *(8: Receive)*/
	public boolean receiveMessage(){
		try{

			InputStreamReader input = new InputStreamReader(this.VSFTPClient.getInputStream());
			BufferedReader buffer = new BufferedReader(input);

			//Checks for the null at the end of the message.
			boolean validMessage = true;
			String message = "";

			while(validMessage){

				message = buffer.readLine();
				//If the message contains a null the null is removed and this method will end.
				if(message.contains("NULL")){

					message = message.substring(0,message.length()-4);
					validMessage = false;
				}

				System.out.println(message);
				//Checks the message for a out to lunch error and will terminate if true.
				checkServerStatus(message);
			}

			/*Checks if the response message contains a # denoting the request for a file 
			 *transfer. If true the client is then to send a second request if that request
			 *is a SEND then the file is sent.*/
			if(message.contains("#")){

				Scanner terminalInput = new Scanner(System.in);
				String secondaryMessage = terminalInput.next();

				sendRequest(secondaryMessage);
				receiveMessage();

				/*If a send command is sent then the file will be retrieved. If a send
				 *command is not send then the client receives a message from the server.*/
				if(secondaryMessage.equals("SEND") || secondaryMessage.equals("send")){

					receiveFile(Integer.parseInt(message.substring(1,message.length())));

					receiveMessage();

					this.requestedFileName = null;
				}

				else this.requestedFileName = null;
			}

			return true;

		}catch(IOException ioE){
			return false;
		}
	}

	//Sends a request to the server in the form <Command> <Args> (7: Send)
	public void sendRequest(String request) throws IOException{

		PrintWriter output = new PrintWriter(this.VSFTPClient.getOutputStream());

		//Checks if a file was requested if so that file name is stored to be saved upon transfer.
		if((request.contains("RETR") || request.contains("retr")) && request.length() > 4){

			this.requestedFileName = request.substring(5,request.length());
		}

		output.println(request);
		output.flush();
	}

	/*Receives a file from the server. If the method does not receive a file 
	 *after a set time the connection is closed.*/
	private boolean receiveFile(int fileSize){
		try{

			InputStream input = this.VSFTPClient.getInputStream();
			FileOutputStream fileOut = new FileOutputStream(this.requestedFileName);

			//Uses a set size byte array so buffers do not overflow on large files.
			byte[] byteArr = new byte[1000];

			/*Continues to write incoming byte stream to the file wile current number of 
			 *bytes written is less than the file size.*/
			int count = 0;
			int countSoFar = 0;
			while ((countSoFar < fileSize) && (count = input.read(byteArr)) > 0)
			{
			    fileOut.write(byteArr, 0, count);
			    countSoFar += count;
			}

			return true;

		}catch(Exception e){
			return false;
		}
	}

	/*Checks if the message received from the server is a out for lunch. If true the client
	 *application terminates.*/
	private void checkServerStatus(String message) throws IOException{

		if(message.equals("-VSFTP Server is out for lunch")){

			System.out.println("cannot reach server\nterminating...");
			this.VSFTPClient.close();

			throw new IOException();
		}
		//Closes the socket and ends the application. (10: Close)
		else if(message.equals("+GoodBye")){

			System.out.println("logout\nconnection ended\nprocess ended");
			this.VSFTPClient.close();

			this.run = false;
		}
	}

	/*Creates a client socket on port 50001 with a user specified server address.
	 *Once a connection is established the client is notified and the client 
	 *can request from the server continuously.*/
	public static void main(String[] args) {
		try{
			
			System.out.println("Enter the IP address for the remote server:");
			Scanner input = new Scanner(System.in);

			Client client = new Client((String)input.nextLine(), 50001);

			client.receiveMessage();

			while(client.run)
			{
				client.sendRequest(input.nextLine());
				client.receiveMessage();
			}

		}catch(Exception e){
		}
	}
}