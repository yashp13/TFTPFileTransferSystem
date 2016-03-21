     _______.____    ____  _______.  ______     ____    ____     ___    ____   
    /       |\   \  /   / /       | /      |   |___ \  |___ \   / _ \  |___ \  
   |   (----` \   \/   / |   (----`|  ,----'     __) |   __) | | | | |   __) | 
    \   \      \_    _/   \   \    |  |         |__ <   |__ <  | | | |  |__ <  
.----)   |       |  | .----)   |   |  `----.    ___) |  ___) | | |_| |  ___) | 
|_______/        |__| |_______/     \______|   |____/  |____/   \___/  |____/  
                                                                               
.___________. _______     ___      .___  ___.       _  _    ___                
|           ||   ____|   /   \     |   \/   |     _| || |_ / _ \               
`---|  |----`|  |__     /  ^  \    |  \  /  |    |_  __  _| (_) |              
    |  |     |   __|   /  /_\  \   |  |\/|  |     _| || |_ > _ <               
    |  |     |  |____ /  _____  \  |  |  |  |    |_  __  _| (_) |              
    |__|     |_______/__/     \__\ |__|  |__|      |_||_|  \___/               
                                                                                     
                                                                                                                                              
___________________________________________________
#
#	TEAM 8
#
#	Members:
#		Jeremy Sawh	100940268
#		Jonathan Chan	100936881
#		Obinna Elobi	100953254
#		Yan Liao	100940287
#		Yash Patel	100943654
#
#	Iteration #: 4
#	Date: March 18, 2016
#__________________________________________________


							SETUP INSTRUCTIONS
---------------------------------------------------------------------------------------
							Adding project to eclipse
---------------------------------------------------------------------------------------

	1. From the main menu select FILE>IMPORT the wizard will open
	2. Collapse the general tab and select add "Existing Project into workspace"
	3. Select either root directory or archive file and browse to the directory
	   containing those the project. Make sure to check copy projects into workspace
	4. Click finish to import the files
	5. You should have the following files in your eclipse workspace:
		Server.java
		ErrorSim.java
		Client.java
		PrintByteArray.java
		ErrorMessagesHandler.java
		WriteRequestHandler.java
		ReadRequestHandler.java
		ReceivedPacketHandler.java
		InvalidRequestException.java
		ErrorType.java
		ServerListener.java
		
------------------------------------------------------------------------------------------
							Run Instructions
------------------------------------------------------------------------------------------
	Run the following files through eclipse Respectively:
		1. Run Server.java
		2. Run ErrorSim.java
		3. Run Client.java

------------------------------------------------------------------------------------------
							ERRORSIM INSTRUCTIONS
------------------------------------------------------------------------------------------
The error simulator provides a number of options for errors to generate, to test how the client and server
will handle these errors.

The options include
	0. Do nothing
	1. Invalid Operation(Change first 2 bytes to 99)
	2. Wrong block number (change 3rd and 4th byte)
	3. Remove Zero (removes the last byte)
	4. Change Mode (Change ASCII or octet)
	5. Missing file name
	6. Invaild Packet size (Change size to 1024)
	7. Invalid TID
	8. Duplicate packet 
	9. Packet lost
	10.Packet Delay

Once you have selected your desired operation type 999 to finish.  Multiple errors can be selected before
the file transfer begins.  They will be queued up. 

------------------------------------------------------------------------------------------
							CLIENT INSTRUCTIONS
------------------------------------------------------------------------------------------	
The client prompts the user to select a mode.  There are two different modes: 
	Mode 1 is normal mode sending packets directly to the server.  
	Mode 2 is the Testing mode that directs all packets to the error simulator for testing purposes.


Client run instructions: 
	1.Select a mode for operation
		1-->Normal mode
		2-->Test Mode
		3-->Shutdown

	2.If selected mode is "1"
		A)Choose either RRQ or WRQ
		B)Type in filename to read from
		C)Type in file name to write to
		D)Transfer files

	3.If selected mode is "2"
		A)Choose errors from menu in error simulator
		B)then go back to client and preform steps in 2.
		
		
		
///////////////////////////////////////////////////////////////////////////////////////////		
\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\		
			
		
								TROUBLESHOOTING
------------------------------------------------------------------------------------------
							ERRORCODE 01 - FILE NOT FOUND
------------------------------------------------------------------------------------------	
	WRQ: The Client does not make a request packet if the file does not exist. 
		 It displays the error and asks the user for the file-name again.
	RRQ: The Client sends a read request packet to Server and if the Server cannot find the file, 
		 it sends an error packet (error code 1) and the transfer stops.	
		
------------------------------------------------------------------------------------------
							ERRORCODE 02 - ACCESS VIOLATION
------------------------------------------------------------------------------------------
	WRQ: The Client sends a write request and if the Server cannot access the file, 
		 it will send an error packet (error code 2) back and the transfer will stop.
	RRQ: The Client sends a read request and then if it cannot access the file, 
		 it displays "ACCESS VIOLATION" and quits the transfer. 
		 The Server times out since it does not receive any packets.
	
	One way to simulate this is - when the client asks for the file to write to (second filename),
		enter a path to a read-only folder. Then, the Server(for WRQ) or the Client(for RRQ) 
		won't be able to write to it, causing an ACCESS VIOLATION error.

------------------------------------------------------------------------------------------
							ERRORCODE 03 - DISK FULL
------------------------------------------------------------------------------------------
	WRQ: The transfer goes on as normal until the Server cannot write because the disk got full. 
		 It sends an error packet (error code 3) to the Client and the transfer stops.
	RRQ: The transfer goes on as normal until the Client cannot write because the disk got full. 
		 The Client prints an error message saying the disk is full and quits the transfer. 
		 The Server times out and quits on its end too.	
		 
	One way to simulate this is - to write to a USB that has less space than the file being written.
		This will cause an error when the disk gets full mid-transfer.		 
		 
------------------------------------------------------------------------------------------
							ERRORCODE 04 - ILLEGAL TFTP OPERATION
------------------------------------------------------------------------------------------
Invalid TFTP Operation
	WRQ and RRQ: When the server receives a packet, either a request packet, data packet
				 ACK in which the op-code does not correspond to the current TFTP operation, 
				 the server will send an error packet containing details about the operation
				 and the transfer will end.  
	
Incorrect Block Number
	WRQ and RRQ: When the client receives an ACK or DATA packet with a block number less than
				 what is expected the packet is ignored and the previous packet is retransmitted.
				 When the server receives a DATA packet with a block number less than what is expected
				 the server sends a error packet to the client and the transfer is shut-down.
				 When the server receives an ACK packet with a block number less that what is expected
				 the packet is ignored the server retransmits the previous packet.		
				 
				 If the client or server receives an ACK or DATA packet with a block number
				 greater than what is expected the server sends an error packet containing 
				 details about the operation and the client then shuts down the transfer.  
			
Missing file name:
	WRQ and RRQ: When the server receives a request packet with no file-name it sends and 
				 error packet to the client and the transfer stops. 
				 
Incorrect Request Packet Format
	WRQ and RRQ: When the server receives a request packet with missing zero's, invalid packet size, 
				 or incorrect mode the server will send an error packet to the client and the 
				 transfer will stop. 
------------------------------------------------------------------------------------------
							ERRORCODE 05 - UNKNOWN TRANSFER ID
------------------------------------------------------------------------------------------
	WRQ and RRQ:  When a an unknown TID is received the recipient will send an error packet
				  and the transfer will stop. The sender's socket will close. 


------------------------------------------------------------------------------------------
							ERRORCODE 06 - FILE ALREADY EXISTS
------------------------------------------------------------------------------------------
	WRQ: The Client sends a write request and once the Server sees that the file it is supposed 
		 to write to already exists, it sends an error packet (error code 6) and the the transfer stops.
	RRQ: If the file the Client is supposed to write to exists, it simply overwrites it. 
		 So the transfer continues at normal.
		
	To simulate this is - to enter an already existing file's name for the file to be written to(second 
		filename in the Client prompt). 
		
\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\		
	
Note:  Duplicate request packets do not have desired results for outputs. The transfers
	   stop due to other errors not because of the duplicate packet. 


\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\


	
		
Files included:
________________________________________________________________________________________________________________

->Client.java:
Client handles a read request or write request for file transfers to the server.
________________________________________________________________________________________________________________

->ErrorSim.java:
When client enter ErrorSim, ErrorSim will generate different error case for each data pass through, and will send all error back client. Error simulator sends data to client via a new port created. 

________________________________________________________________________________________________________________

->ErrorSimHolder.java:
Creates a new thread in order to send duplicate packets while the rest of the transfer continues.
________________________________________________________________________________________________________________

->ErrorType.java:
A separate class for the errors that the users select and add to the list. It contains methods that can be called from the ErrorSim to find out more information about the error like the packet number, kind of error or if it has to occur on a DATA packet or an ACK packet.
________________________________________________________________________________________________________________

->Server.java:
Starts ServerListener thread and then polls for 'shutdown' command.

________________________________________________________________________________________________________________

->InvalidRequestException.java:
Not Used. Inherit exception message
________________________________________________________________________________________________________________

->printByteArray.java:
Not Used. To print packet information in Hex
________________________________________________________________________________________________________________

->ReadRequestHandler.java:
Handles Read Requests (RRQ).
If first packet, verfies packet fields (mode and filename). If valid, opens and holds the file to read from and fill a DATA packet then sends.
After first packet, verifies the current address, port, opcode of ACK, and block number.
If all valid, create DATA packet and fill with appropriate information, then send.
End condition, when there is less than 512 bytes of data left in the open file to read.
________________________________________________________________________________________________________________

->ReceivedPacketHandler.java:
Opens new socket to receive following packets. Verifies opcode and passes the first packet to the appropriate handler. WriteRequestHandler or ReadRequestHandler.
________________________________________________________________________________________________________________

->WriteRequestHandler:
Handles Write Requests (WRQ).
If first packet, verfies packet fields (mode and filename). 
If valid, creates a file with the name (will overwrite if a file is there with the same name) and if there is a directory in the filename, it will create it there (overwrite still applies).
Holds the created file open to write the data to.
After first packet, verifies the current address, port, opcode of DATA, and block number.
If all valid, write to the open file and sends an ACK.
End condition, received packet is less than 516 bytes.
________________________________________________________________________________________________________________

->ServerListener.java:
Has port 69 to receive packets, once it receives a packet start a ReceivedPacketHandler thread to deal with it.
________________________________________________________________________________________________________________
->ErrorMessagesHandler:
Collection of Errors and creates the Error packet to be sent when there is one.
________________________________________________________________________________________________________________




________________________________________________
Member Resposibilites - Iteration #1
================================================
Jeremy: |Implementation of the client
------------------------------------------------
Yash:	|ErrorSim, Diagrams and test cases
------------------------------------------------
Obinna:	|Implementation of the client
------------------------------------------------
Jon:	|Implementation of the Server
------------------------------------------------
Yan:	|ErrorSim, Diagrams and test cases
________________________________________________

________________________________________________
Member Resposibilites - Iteration #2
================================================
Jeremy: |Client, Debugging, Readme
------------------------------------------------
Yash:	|ErrorSim, Client, Diagrams
------------------------------------------------
Obinna:	|Client, Debugging, Diagrams
------------------------------------------------
Jon:	|Server, Client, Diagrams
------------------------------------------------
Yan:	|ErrorSim, Diagrams, Testing
________________________________________________

________________________________________________
Member Resposibilites - Iteration #3
================================================
Jeremy: |Diagrams, Readme, testing
------------------------------------------------
Yash:	|Error simulator, debugging
------------------------------------------------
Obinna:	|Client, debugging, testing 
------------------------------------------------
Jon:	|Server, debugging, testing
------------------------------------------------
Yan:	|ErrorSim, debugging, Testing
________________________________________________

________________________________________________
Member Resposibilites - Iteration #4
================================================
Jeremy: |Readme, Client
------------------------------------------------
Yash:	|Client, debugging
------------------------------------------------
Obinna:	|Diagrams, debugging
------------------------------------------------
Jon:	|Server, debugging
------------------------------------------------
Yan:	|ErrorSim, debugging
________________________________________________


