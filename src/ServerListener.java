//--------------------------------------------------// 
//SYSC 3303 Assignment 1							//
//TFTP Server Program: ServerListener.java			//
//													//
//Author: Jonathan Chan								//
//Student Number: 100936881							//
//													//
//Carleton University								//
//Department of Systems and Computer Engineering	//
//SYSC 3303 RealTime								//
//Concurrent Systems Winter 2016					//
//--------------------------------------------------//

import java.net.*;
import java.util.ArrayList;
import java.util.ListIterator;

/*
 * 
 * Multithreaded listener for receiving packets and creating appropriate responses, initiated from Server class.
 * 
 */

public class ServerListener extends Thread {

	public DatagramSocket serverSocket;			// Receive socket for server
	private int ServerPort;						// Receive Port for server
	DatagramPacket ReceivePacket;				// Receive packet
	
	static final int maxBytes = 516;			// Maximum TFTPpacket size to be used to create byte buffers
	static final int minBytes = 4;				// Minimum
	
	static final int extraLength = 518;			// Test for longer packet
	
	// constructor, values from class Server
	public ServerListener(Server TftpServer, int ServerPort){
		this.ServerPort = ServerPort;	
	}
	
	public void run(){
		
		try{
			//--listening on 
			//local port = 69 and one of the local IP addresses. 
			//remote port = ANY and remote IP = ANY
			serverSocket = new DatagramSocket(ServerPort);		
			
		} catch (SocketException se){							// if can't make a connection
			
			System.out.println(se.toString());
			se.printStackTrace();
			System.exit(1);			
		}
		
		System.out.format("\nConnection to port %d succesful and listening for requests.\n", ServerPort);	//-- use format instead 
		
		
		ArrayList<Thread> theadArrayList = new ArrayList<Thread>();											//--for checking thread status
		ArrayList<ReceivedPacketHandler> objArrayList = new ArrayList<ReceivedPacketHandler>();				//--for accessing object
		int threadNameNumber = 1;
		
		try{
			
			// While port is open, continue to start threads.
			while (!(serverSocket.isClosed())) {
				
				byte dataReceive[] = new byte[extraLength];
				ReceivePacket = new DatagramPacket(dataReceive, dataReceive.length);		// Construct a DatagramPacket to receive
				
				try {				
					serverSocket.receive(ReceivePacket);		
					
					if(ReceivePacket.getLength() > maxBytes){
						
						//error, illegal TFTP operation
						
						ErrorMessagesHandler invHld = new ErrorMessagesHandler(this);
						invHld.errorHandler(ErrorMessagesHandler.RFC_ILLEGAL_OP, ErrorMessagesHandler.PACKET_LONGER_THAN_516);
						
						System.out.printf("Expected Packet to be 516 bytes, but received greater than 516 bytes.");
						
						continue;
					}
				} 				
				catch (SocketException se) {			//socket closed by shutdown				
					System.out.println(se.toString());	
					break;
				} 
				catch (Exception e) {					
					e.printStackTrace();				
					break;
				}
				
				if(ReceivePacket.getLength() == 0) {
					//receive package no data. unknown error.
					break;
				}
				
				try {
					ReceivedPacketHandler obj = new ReceivedPacketHandler(ReceivePacket);	//--create object first
					Thread t = new Thread(obj);												//--from object, create thread
					String threadName = "ReceivedPacketHandler " + threadNameNumber++;		//-- for easier to debug
					t.setName(threadName);													//-- for easier to debug
					
					t.start();	
					
					theadArrayList.add(t);			//--save the thread in array list
					objArrayList.add(obj);			//--save the instance object in array list

					
					System.out.println("New thread '" + t.getName() + "' started. Number of threads created:" + theadArrayList.size());	
					
					
					

				
					
				
				} catch (Exception e){
					
					System.out.println(e.toString());
					e.printStackTrace();
				}
			}
			
		} catch (Exception e){
			
			// Socket will be waiting, this exception is thrown when it is closed.
			System.out.println(e.toString());
			e.printStackTrace();
		
		}
		
		int joinSecond = 30;
		ListIterator<Thread> litr = theadArrayList.listIterator();
		while(litr.hasNext()) {	
			try {
				Thread nextThread = litr.next();
				
				if(nextThread != null) {
					System.out.format("Waiting up %d seconds for %s to join.. .\n", joinSecond, nextThread.getName());
					nextThread.join(joinSecond * 1000);	//--max wait 30s.	
					System.out.format("Thread %s joined.\n", nextThread.getName());
				}
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		System.out.println("----------Exited join's while.");
		
		serverSocket.disconnect();										// Don't unbind socket with close(), use disconnect for current thread.
		System.out.println("----------Server Listener thread has been closed");
		
	}

	public void closeSocket() {
		serverSocket.close();
	}
}
