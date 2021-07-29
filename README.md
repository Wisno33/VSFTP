# VSFTP
A Very Simple File Transfer Protocol.

This is a command line protocol that  utilizes the socket API and provides a client and server programs for transfer of file. Files are stored on the server end and the client can retrieve these files. The client cannot send files, but can delete them. The protocol uses a basic username and password authentication where the list of valid username and passwords are stored in plain text in a txt file.

This is not a file transfer protocol of any practical use. This is due to the  lack of two way send and receive along with poor security measures. However, it could be put to use for a home server where authentication security is not a major concern, and only retrieval is needed.

## Requirements
The client and server are written in Java work with JDK 8+, on Windows, Mac OSX, and Linux

## Documentation
Client commands include USER, PASS, LIST, RETR, SEND, KILL, DONE. These commands only work if the letters are in all upper or lower case. (For example DONE and done will work but Done will not)

Response Codes:

The server will send response codes to the client, they are listed below along with there meaning (symbol - meaning). The server will also log client commands and its response. 
		+  = Success
		-  = Fail
		!  = Logged in
		#  = File Size
	
  Usernames and Passwords:
  
  The users.txt file is stored in a data directory, and contains the usernames and passwords of the users in plain text. The username is first and password second and are separated with a space.
  User names and passwords are both case sensitive and should not include spaces.
  
  Command operation:
  
  1) USER takes a parameter username, only uses the first item given others are discarded.
  2) PASS takes a parameter password, only uses the first item given others are discarded.
  3) LIST takes no arg discards any given.
  4) RETR takes a file name only uses the first item given others are discarded.
  5) SEND takes no arg discards any given.
  6) KILL takes a file name only uses the first item given others are discarded.
  7) DONE takes no arg and ends the server and client.

  Files:
  
  Files are in a root directory where the server has full access.
