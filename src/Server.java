//--------------------------------------------------// 
//SYSC 3303 Assignment 1							//
//TFTP Server Program: Server.java					//
//													//
//Author: Jonathan Chan								//
//Student Number: 100936881							//
//													//
//Carleton University								//
//Department of Systems and Computer Engineering	//
//SYSC 3303 RealTime								//
//Concurrent Systems Winter 2016					//
//--------------------------------------------------//

//----------------
//Project Overview
//----------------
//1. Teams will design and implement a file transfer system based on the TFTP specification (RFC 1350).
//2. The system will consist of TFTP client(s) running on one or several computers, an error simulator, 
//   and THIS multiple threaded TFTP Server that runs on a different computer.
//3. This Server will run as a separate Win32 process, and the programs will communicate via DatagramSocket
//   objects. 
//4. In “normal” mode, only the client and server programs will run. In “test” mode, all three programs will be used.
//
//
//--------------------
//Server Specification
//--------------------
//1. The server will be implemented as a Java program that consists of multiple Java threads. 
//2. The server will be capable of supporting multiple concurrent read and write connections with different clients.
//3. No command line input arguments are required.
//
//-----------------------------------------
//Server Detail Design, Iteration #0 and #1
//-----------------------------------------
//1. Main process will start a listening thread that will wait on port 69 for UDP datagrams containing WRQ and RRQ packets to come.
//2. Main process then waits for the “shutdown”, case insensitive, command from the server operator to shut down the Server program.
//3. The listening thread 
//	 a. Verify that the received TFTP packet is a valid WRQ packet or RRQ packet
//	 b. Create another thread (call it the client connection thread), and pass it the TFTP packet; and
//	 c. Go back to waiting on port 69 for another transfer request.
//4. The newly created 'client connection thread' will perform for communicating with the client to transfer a file 
//   from client to server (Write Request), or from server to client (Read Request).
//
//----------------------------------
//Server Detail Design, Iteration #2
//----------------------------------
//1. Errors can occur in the TFTP packets received, so TFTP ERROR packets dealing with this Error Code, 4 and 5, is prepared, 
//   transmitted, received, and handled.
//2. Opcode 5, Error code 4. Illegal TFTP operation.
//   a. Opcode number neither 1 (RRQ) nor 2 (WRQ), but %n received.
//   b. File name was not terminated with 0.
//   c. Mode was not terminated with 0.
//	 d. Invalid mode, but %s received.
//	 e. Expected data package with opcode 3, but  %n received.
//   f. Write data starting block number is not 1, but %n received.
//   g. Write data block number neither previous block number n nor n+1, but %n received.
//   h. Read ACK block number %n expected, but %n received.
//
//3. Opcode 5, Error code 5. Unknown transfer ID.
//	 a. TID as local port at which server is listening. Wrong TIB package will never be received.
//
//----------------------------------
//Server Detail Design, Iteration #3
//----------------------------------
//1. handle network errors. 
//	a. Loss packets,	
//		On RRQ, after sending DATA package, wait for an ACK. 
//				At timed out, retransmit the last data package. 		
//		On WRQ, after sending ACK package, wait for DATA. 
//				At timed out, assume client has been stopped. Actions: 1) Terminate the transfer, 2) Close the file and 3) Delete it
// 	b. Delayed, and 
//		ACK delayed but received before socket receive is being timed out. No action is required.
//	c. Duplicated. 
//		On RRQ, duplication of ACK. No action required.
//		On WRQ, duplication of DATA is caused by not receiving the ACK by the client.
//				Drop the duplicated package and ACK it.
//2. Use TFTP's “wait for acknowledgment/timeout/retransmit” protocol
//3. Duplicate ACKs is not be acknowledged, and 
//4. Only the side that is currently sending DATA packets is required to retransmit after a timeout.
//
//----------------------------------
//Server Detail Design, Iteration #4
//----------------------------------
//1. I/O errors can occur, so TFTP ERROR packets dealing with this (Error Code 1, 2, 3, 6) is prepared, transmitted, received, or handled.
//2. Write Operation errors handled are
//		a. File exist
//		b. File create error
//		c. Write failed.
//3. Read Operation errors handled are
//		a. Invalid file path
//		b. File not found
//		c. Open for read fail
//		d. Read failed.
//
//
//======================================
//Assumption in this Iteration #0 and #1
//--------------------------------------
//1. No errors, relating to socket, package and file etc, will occur.
//2. No timing control on socket receiving, waiting on the last ACK etc.
//3. Computer has required resources.
//4. Client and error simulator are designed according to RFC1350.
//
//--------------------------------------
//Assumption in this Iteration #2
//--------------------------------------
//1. No packets will be lost, delayed, or duplicated (see Iteration #3), and 
//2. There will be no File Input/Output errors (see Iteration #4).
//
//
//--------------------------------------
//Assumption in this Iteration #3
//--------------------------------------
//1. Assume that there is no File Input/Output errors
//
//
//--------------------------------------
//Assumption in this Iteration #4
//--------------------------------------

import java.util.Scanner;


public class Server{

	static final int ReceiveServerPort = 69;	// Server receiver port
	private ServerListener serverRequest;		// listener thread

	public static Boolean continueScan = true;
	public static Scanner scan;
	
	private Server() {
		
		serverRequest = new ServerListener(this, ReceiveServerPort);
		serverRequest.start();					// start thread	
	}
	

	
	public static void main(String[] args){
		
		Server server = new Server();			// initialize constructor to start thread
			
		// Take user input for shutdown.
		scan = new Scanner(System.in);
			
		// Polls for user input
		while (true) {
			
			System.out.print("To shutdown, type 'shutdown' : ");
			String UserInput = scan.nextLine().toUpperCase();
			String[] sArray = UserInput.split("\\s+");			// Rid of whitespace and put into location 0
			
			if (sArray[0].length() == 0){
				
				// If user didn't type anything and pressed return, keeps polling.
				continue;
				
			}
			
			if (sArray[0].equals("SHUTDOWN")){
				
				scan.close();
				server.shutdown();
				
				break;
			}
		}
		
		
	}

		
	public void shutdown() {
		
		serverRequest.closeSocket();			// unbind socket, blocks additional incoming requests
		System.out.println("Server receiver port closed, server will shutdown after current threads finish.");
		System.out.println("Threads finishing . . . ");
		

		try {
			System.out.println("Exiting on either 10s timed out or all server threads exited (which ever comes first).. .");	//--
			serverRequest.join(10000);	//--set max wait to 10s.
		}
		catch (Exception ex) {
			
		}
		
		System.out.println("Exiting successfully.");
		System.exit(0);
		
	}
}


