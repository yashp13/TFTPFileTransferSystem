//--------------------------------------------------// 
//SYSC 3303 Iteration 4 							//
//TFTP Server Program: WriteRequestHandler.java		//
//													//
//Author: Jonathan Chan								//
//Student Number: 100936881							//
//													//
//Carleton University								//
//Department of Systems and Computer Engineering	//
//SYSC 3303 RealTime								//
//Concurrent Systems Winter 2016					//
//--------------------------------------------------//

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class WriteRequestHandler {

	ReceivedPacketHandler rcvHanlder;
	printByteArray bytePrinter;
	
	byte[] dataBuf;
	int dataLen;
	byte[] sendBuf;
	int sendLen;
	
	DatagramPacket pkg;
	
	byte[] byteFileName;
	String strgFileName;
	Path filePath;
	
	byte[] byteMode;
	String strgMode;
	
	byte[] extraBuf;
	
	int fileNameLen, modeLen;
	
	//For faster switch's casing
	int nMode;	
	static final int UNKNOWN_MODE = 0;
	static final int NETASCII_MODE = 1;
	static final int OCTET_MODE = 2;
	
	static final int maxByte = 516;
	
	int receivedPackageDataLen;
	
	int expectedBlock;
	int opCode;
	InetAddress remoteIpAddress;
	int remotePort;
	

	FileOutputStream fileOutputStream;
	BufferedOutputStream bufferedOutputStream;
	
	public WriteRequestHandler(ReceivedPacketHandler rcvHdl) {	
		rcvHanlder = rcvHdl;	
		bytePrinter = new printByteArray(true);
		
		byteFileName = new byte[maxByte];
		byteMode = new byte[maxByte];
		dataBuf = new byte[maxByte];
		sendBuf = new byte[maxByte];	
		dataLen = sendLen = 0;
		
		extraBuf = new byte[518];		// extra bytes to test too long packet
		
		pkg = rcvHanlder.receivedPacket;
		expectedBlock = 0;
	}
	
	
	
	public void wrqResponseHandler() {
		try {
			if(processFirstPackage()) {
					
				int numPrevBlockReceived = 0;
				
				while(rcvHanlder.continueRun) {
					DatagramPacket packet = new DatagramPacket(extraBuf, extraBuf.length);
					
					//It_3. Network error starts
					try {
						rcvHanlder.transferSocket.setSoTimeout(5000);
						rcvHanlder.transferSocket.receive(packet);
						
						if(packet.getLength() > maxByte){
							
							//error, illegal TFTP operation
							
							ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
							invHld.errorHandler(ErrorMessagesHandler.RFC_ILLEGAL_OP, ErrorMessagesHandler.PACKET_LONGER_THAN_516);
							
							System.out.printf("Expected Packet to be 516 bytes, but received greater than 516 bytes", packet.getLength());
							
							continue;
						}
					}
					catch (SocketTimeoutException ex) {
						
						//Wait for the package for 5 second. Long enough, quit.
						System.out.println("WRQ: Timedout! " + ex.getMessage());
						
						closeBufferedOutputStream();
						closeFileOutputStream();
			        	break;
					}
					
					InetAddress incomingIpAddress = packet.getAddress();
		        	int incomingPort = packet.getPort();
		        	if((remotePort != incomingPort) || !(remoteIpAddress.equals(incomingIpAddress))){
		        		// Send error message back to the remote wrong port using information for the wrong packet
		        		ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder, packet);
		    			invHld.errorHandler(ErrorMessagesHandler.RFC_UNKNOWN_TID, ErrorMessagesHandler.UNKNOWNTID_INDEX);
		        		continue;
		        	}
		        	
					//It_3. Network error end
					
		        	receivedPackageDataLen = packet.getLength();
		        	
		        	dataBuf = packet.getData();
		        	
		        	if(rcvHanlder.isErrorPackage(dataBuf)){
						int errorCode = rcvHanlder.getErrorCode(dataBuf);
						
						if(errorCode == 5){
							rcvHanlder.printContents(packet);
							System.out.println("\n Server has received Error Packet, Unkown TID, and will now shutdown this thread. See messaage above.");
						}
						else if(errorCode == 4){
							rcvHanlder.printContents(packet);
							System.out.println("\n Server has received Error Packet, Illegal TFTP operation, and will now shutdown this thread. See messaage above.");
							
						} else {
							rcvHanlder.printContents(packet);
							System.out.println("\n Server has received Error Packet and will now shutdown this thread. See messaage above.");
						}
						closeBufferedOutputStream();
		    			closeFileOutputStream();
		    			break;
		        	}
		        	
		        	if( ! rcvHanlder.isDataPackage(dataBuf) ) {
		    			ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
		    			invHld.errorHandler(ErrorMessagesHandler.RFC_ILLEGAL_OP, ErrorMessagesHandler.INVALID_OPCODE, 3, rcvHanlder.getPkgOpCode(dataBuf));
		    			closeBufferedOutputStream();
		    			closeFileOutputStream();
			        	break;
		        	}
		        	
		        	
		        	int curBlock = rcvHanlder.getPkgBlock(dataBuf);
		        	
		        	//It_3. Network error starts
		        	if(curBlock < expectedBlock) {	
		        		//Network error. This package block # is the same as a previous block #. Client must be re-sending. 
		        		//Server has already received the package because server block # has increased. Don't need to save data into file.
		        		//Re-send ACK to client, then go back to beginning of while loop.
		        		//If this happens up to 3 times consecutively, time to quit?.
		        		//This is checked by incrementing numPrevBlockReceived every time get in here. Clear numPrevBlockReceived after every good package.
		        		
		        		sendBuf[0] = 0;		//ack
			        	sendBuf[1] = 4;		//ack
			        	sendBuf[2] = dataBuf[2];
			        	sendBuf[3] = dataBuf[3]; 
			        	sendLen = 4;
		        		
		        		packet = new DatagramPacket(sendBuf, sendLen, remoteIpAddress, remotePort);
			        	rcvHanlder.transferSocket.send(packet);	
			        	
			        	numPrevBlockReceived++;
			        	System.out.println("WRR:          Previous block # received " + numPrevBlockReceived + " times. Re-sent ACK back to client.");			        	
			        	if(numPrevBlockReceived >= 3) {
			        		//consecutively received previous block number 3 times. Give up and quit.
			    			closeBufferedOutputStream();
			    			closeFileOutputStream();
			        		break;
			        	}			        	
		        	} 
		        	//It_3. Network error end. Go back to beginning of while loop
		        	
		        	else {
		        		
			        	if(curBlock > expectedBlock) {
			    			ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
			    			invHld.errorHandler(ErrorMessagesHandler.RFC_ILLEGAL_OP, ErrorMessagesHandler.INVLIAD_WR_BLOCK, expectedBlock, curBlock);
			    			closeBufferedOutputStream();
			    			closeFileOutputStream();
				        	break;
			        	}
			        	
			        	numPrevBlockReceived = 0;
			        	receivedPackageDataLen = packet.getLength();
			        	System.out.println("WRR: Received OpCode:" + rcvHanlder.getPkgOpCode(dataBuf) + ", Block:" + curBlock + ", Packet Length:" + receivedPackageDataLen);
				        System.out.println("WRR:          Local port(Host TID):" + rcvHanlder.transferSocket.getLocalPort() + " local IP: " + rcvHanlder.transferSocket.getLocalAddress());						        	
				        System.out.println("WRR:          remote port(remote TID):" + packet.getPort() + ", remote IP: " + packet.getAddress());
				        
				        rcvHanlder.printContents(packet);
				        
			        	//write to file
				        byte[] wrBuf = Arrays.copyOfRange(dataBuf, 4, receivedPackageDataLen);		        
			        	Boolean wrResultOk = writeToFile(wrBuf, receivedPackageDataLen - 4);
			        	if(!wrResultOk) {
			    			closeBufferedOutputStream();
			    			closeFileOutputStream();
				        	break;
			        	}
			        	
			        	
			        	//ACK cmd
			        	sendBuf[0] = 0;		//ack
			        	sendBuf[1] = 4;		//ack
			        	sendBuf[2] = dataBuf[2];
			        	sendBuf[3] = dataBuf[3]; 
			        	sendLen = 4;
		        	
			        	packet = new DatagramPacket(sendBuf, sendLen, remoteIpAddress, remotePort);
			        	rcvHanlder.transferSocket.send(packet);	
			        	System.out.println("\nWRR:          Sent ACK back to client. OpCode: 04, Block: " + rcvHanlder.getPkgBlock(sendBuf)+"\n");
			        	
				        expectedBlock = curBlock + 1;
				        
			        	if(receivedPackageDataLen < 516) {
			        		//done
	
			        		bufferedOutputStream.flush();
			    			closeBufferedOutputStream();
			    			closeFileOutputStream();		        			
			        		try {
								TimeUnit.SECONDS.sleep(1);
								
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
	
			        		break;
			        	}	 
		        	}
		        	
				}	//while		
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
		finally {
			System.out.println("WRR: ----------Write Request, completed. Socket closed.");	
			rcvHanlder.transferSocket.close();
		}
	}	
	
	Boolean processFirstPackage() throws IOException {
		Boolean result = false;
		
		try {
			//the IP address and port number on the remote host from which the datagram was received.
	        remoteIpAddress = pkg.getAddress();
			remotePort = pkg.getPort(); 	
			
			byte[] dataBuf = pkg.getData();
			int requestDataLen = pkg.getLength();
			
			if(getInputFileAndMode(byteFileName, byteMode, dataBuf, requestDataLen)) {
			
				strgFileName = (new String(byteFileName, 0, fileNameLen, StandardCharsets.UTF_8)).toLowerCase();		
				strgMode = (new String(byteMode, 0, modeLen, StandardCharsets.UTF_8)).toLowerCase();
				
				switch(strgMode) {
					case "netascii":
						nMode = NETASCII_MODE;
						break;
					case "octet":
						nMode = OCTET_MODE;
						break;
					default:
						nMode = UNKNOWN_MODE;	
						ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
						invHld.errorHandler(ErrorMessagesHandler.RFC_ILLEGAL_OP, ErrorMessagesHandler.INVALID_MODE, strgMode);
						return result;
				}
				
				
				if(createFile()) {
					
					//sendBuf is kept for retransmit if needed
					sendBuf[0] = 0;	//ACK high byte
					sendBuf[1] = 4;	//ACK low byte
					sendBuf[2] = (byte)(expectedBlock >> 8);	//block high byte
					sendBuf[3] = (byte)(expectedBlock);			//block 0 low byte
					sendLen = 4;
					
		        	//sends the ACK packet.	
					DatagramPacket packet = new DatagramPacket(sendBuf, sendLen, remoteIpAddress, remotePort);			
					rcvHanlder.transferSocket.send(packet);				
					System.out.println("WRR: local port(Host TID):" + rcvHanlder.transferSocket.getLocalPort() + ", remote port(remote TID):" + packet.getPort());
	
					expectedBlock += 1;
					
					result = true;
				}
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return result;
	}
	

	Boolean getInputFileAndMode(byte[] byteFileName, byte[] byteMode, byte[] requestDataBuf, int requestDataLen) {
		Boolean result = false;
		
		try {
			int index;
			int fileNameIndex = 0;	
			for(index=2; index<requestDataLen; index++) {
				byte b = requestDataBuf[index];
				byteFileName[fileNameIndex++] = b;
				
				if(b==0) {
					fileNameLen = fileNameIndex - 1;
					break;
				}
			}	//Now, index pints at \0 after file name
			
			//error checking: file name
			if(index == requestDataLen) {
				ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
				invHld.errorHandler(ErrorMessagesHandler.RFC_ILLEGAL_OP, ErrorMessagesHandler.FILENAME_NOT_TERMINATED);
				return result;
			}
			if(fileNameLen == 0) {
				ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
				invHld.errorHandler(ErrorMessagesHandler.RFC_ILLEGAL_OP, ErrorMessagesHandler.NO_FILENAME);
				return result;
			}
			
			
			//(at least two byte left) AND (file name at least 1 byte + \0)
			if( (index < (requestDataLen-2)) && (fileNameLen > 0) ) {
				
				int i;
				int modeIndex = 0;
				for(i=(index+1); i<requestDataLen; i++) {
					byte b = requestDataBuf[i];
					byteMode[modeIndex++] = b;
					
					if(b==0) {
						modeLen = modeIndex - 1;
						break;
					}
				}
				
	
				if(i == requestDataLen) {
					ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
					invHld.errorHandler(ErrorMessagesHandler.RFC_ILLEGAL_OP, ErrorMessagesHandler.MODE_NOT_TERMINATED);
					return result;
				}
				if(modeLen == 0) {
					ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
					invHld.errorHandler(ErrorMessagesHandler.RFC_ILLEGAL_OP, ErrorMessagesHandler.NO_MODE);	
					return result;
				}			
				
				
				if( (i < requestDataLen) && (modeIndex > 1) ) {
					result = true;
				}
			}
			else {
				ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
				invHld.errorHandler(ErrorMessagesHandler.RFC_ILLEGAL_OP, ErrorMessagesHandler.NO_MODE);
				return result;
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}

	Boolean createFile() {
		Boolean result = false;		

		try {	

			File file = new File(strgFileName);
			
			if (!file.exists()) {	
				result = file.createNewFile();
				fileOutputStream = new FileOutputStream(file, true);	//true means append
				bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
				
				result = true;
			} 
			else {
				ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
				invHld.errorHandler(ErrorMessagesHandler.RFC_FILE_EXIST, ErrorMessagesHandler.FILE_EXIST, strgFileName);
			}
		}
		catch (IOException ex) {	//create
			//Iteration #4			
			ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
			invHld.errorHandler(ErrorMessagesHandler.RFC_ACCESS_VIOLATION, ErrorMessagesHandler.FILE_CREATE_FAILED, strgFileName);
		} 
		
		if(!result) {
			closeBufferedOutputStream();
			closeFileOutputStream();			
		}
		
		System.out.format("Created an output (write) file %s result: %s\n", strgFileName, result.toString());
		return result;	
	}
	
	Boolean writeToFile(byte[] wrBuf, int len) {
		Boolean result = false;
		
		switch(nMode) {
		case OCTET_MODE:
			try {
				bufferedOutputStream.write(wrBuf, 0, len); 
				result = true;
				
			} catch (IOException e) {
				//Iteration #4
				ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
				invHld.errorHandler(ErrorMessagesHandler.RFC_DISK_FULL, ErrorMessagesHandler.WRITE_FAILED, strgFileName);
			}
			break;
		
		case NETASCII_MODE:
			try {
				//A host which receives netascii mode data must translate the data to its own format. ???
				bufferedOutputStream.write(wrBuf, 0, len); 
				result = true;
				
			} catch (IOException e) {
				//Iteration #4
				ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
				invHld.errorHandler(ErrorMessagesHandler.RFC_DISK_FULL, ErrorMessagesHandler.WRITE_FAILED, strgFileName);
			}
			break;	
			
		default:
			//error
			break;
		}
		
		return result;
	}
	
	void closeBufferedOutputStream() {
		if(bufferedOutputStream!=null) {
			try {
				bufferedOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	void closeFileOutputStream() {
		if(fileOutputStream!=null) {
			try {
				fileOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}
}
