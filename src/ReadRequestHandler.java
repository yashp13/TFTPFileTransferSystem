//--------------------------------------------------// 
//SYSC 3303 Iteration 4 							//
//TFTP Server Program: ReadRequestHandler.java		//
//													//
//Author: Jonathan Chan								//
//Student Number: 100936881							//
//													//
//Carleton University								//
//Department of Systems and Computer Engineering	//
//SYSC 3303 RealTime								//
//Concurrent Systems Winter 2016					//
//--------------------------------------------------//

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class ReadRequestHandler {
	
	int receivedPackageDataLen;
    int opCode;

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
	
	static final int maxByte = 516;
	
	int fileNameLen, modeLen;
	
	//For faster switch's casing
	int nMode;	
	static final int UNKNOWN_MODE = 0;
	static final int NETASCII_MODE = 1;
	static final int OCTET_MODE = 2;
	
	int expectedBlock;
	
	InetAddress remoteIpAddress;
	int remotePort;
	
    BufferedInputStream bufferedInputStream = null;
    FileInputStream  fileInputStream= null;
    
	public ReadRequestHandler(ReceivedPacketHandler rcvHdl) {	
		rcvHanlder = rcvHdl;	
		bytePrinter = new printByteArray(true);
		
		byteFileName = new byte[maxByte];
		byteMode = new byte[maxByte];
		dataBuf = new byte[maxByte];
		sendBuf = new byte[maxByte];	
		
		extraBuf = new byte[518];			// extra bytes to test too long packet
		
		dataLen = sendLen = 0;
		
		pkg = rcvHanlder.receivedPacket;
		expectedBlock = 1;
	}
	
	
	
	public void rrqResponseHandler() {
		try {
			if(processFirstPackage()) {
						
				Boolean timeToBreak = false;
				
				while(rcvHanlder.continueRun) {
					
					DatagramPacket packet = null;
					
					//It_3. Network error starts.
					//Data package has been sent. 
					//Now, wait 5 seconds for the ACK package to arrive.
					//
					//Timed out: 
					//		No ACK comes in, either data package to the client is lost OR client's ACK package to server is lost.
					//		Thus, re-send the data package to client. go back to wait ACK
					//
					//Package came in within 5 seconds:
					//		if it is ACK and has correct block #, 
					//			proceed to send another data block
					//		otherwise, re-send data. Hopefully, client will straight it out. go back to wait ACK
					//
					//After go back and wait ACK 3 times, then quit
					//
					int retry = 0;
					Boolean ErrorPack = false;
					Boolean timedOutOrIOError = true;
					while(retry < 3) {
						
						retry++;
						
						try {
							packet = new DatagramPacket(extraBuf, extraBuf.length);
							rcvHanlder.transferSocket.setSoTimeout(5000);
							rcvHanlder.transferSocket.receive(packet);
							
							if(packet.getLength() > maxByte){
								
								//error, illegal TFTP operation
								
								ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
								invHld.errorHandler(ErrorMessagesHandler.RFC_ILLEGAL_OP, ErrorMessagesHandler.PACKET_LONGER_THAN_516);
								
								System.out.printf("Expected Packet to be 516 bytes, but received greater than 516 bytes", packet.getLength());
								
								ErrorPack = true;
								break;
								
								//continue;
							}
							
							System.out.println("RRQ: Reveived package. Not Timedout!");
							timedOutOrIOError = false;
						}
						catch(SocketTimeoutException ex) {
							timedOutOrIOError = true;
							System.out.println("RRQ: Timedout " + retry + " times! (Represents Time out due to actual time out or error)" + ex.getMessage());
						}
						

						
						if((timedOutOrIOError)) {				
							//handle lost ACK. re-transmit							
							packet = new DatagramPacket(sendBuf, sendLen + 4, remoteIpAddress, remotePort);
				        	rcvHanlder.transferSocket.send(packet);		
				        	System.out.println("RRQ: Timedout, resend Block:" + rcvHanlder.getPkgBlock(sendBuf) + ", Data Length:" + (sendLen + 4));
				        	
						} else {
							
							dataBuf = packet.getData();	
							
							InetAddress incomingIpAddress = packet.getAddress();
				        	int incomingPort = packet.getPort();
				        	if((remotePort != incomingPort) || !(remoteIpAddress.equals(incomingIpAddress))){
				        		// Send error message back to the remote wrong port using information for the wrong packet
				        		ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder, packet);
				    			invHld.errorHandler(ErrorMessagesHandler.RFC_UNKNOWN_TID, ErrorMessagesHandler.UNKNOWNTID_INDEX);
				        		continue;
				        	}
							
				        	//Check opcode and block number
							if(packet.getLength() >= 4) {							
								int blk = rcvHanlder.getPkgBlock(dataBuf);
								int opCode = rcvHanlder.getPkgOpCode(dataBuf);
								if( rcvHanlder.isAckPackage(dataBuf) && (blk==expectedBlock) ) {
									break; // correct packet
								
								} else {							//handle wrong block#. re-transmit
									
									if(rcvHanlder.isErrorPackage(dataBuf)){
										int errorCode = rcvHanlder.getErrorCode(dataBuf);
										ErrorPack = true;
										if(errorCode == 5){
											rcvHanlder.printContents(packet);
											System.out.println("\n Server has received Error Packet, Unkown TID, and will now shutdown. See messaage above.");
										}
										if(errorCode == 4){
											rcvHanlder.printContents(packet);
											System.out.println("\n Server has received Error Packet, Illegal TFTP operation, and will now shutdown. See messaage above.");
											
										}
										
										
										break;
									}
									
									if(!rcvHanlder.isAckPackage(dataBuf)) {
										ErrorPack = true;
						    			ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
						    			invHld.errorHandler(ErrorMessagesHandler.RFC_ILLEGAL_OP, ErrorMessagesHandler.INVALID_OPCODE, 4, opCode);
						    			//closeBufferedInputStream();
						    			//closeFileInputStream();
						    			System.out.println("RRQ: Not ACK, Block:" + rcvHanlder.getPkgBlock(sendBuf) + ", Received OpCode:" + opCode + ", Data Length:" + (sendLen + 4));
							        	
						    			break;
						        	}
									
									// Can't be explained as a duplicate, send error
									if (blk > expectedBlock){
										timedOutOrIOError = true;
	
						    			ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
						    			invHld.errorHandler(ErrorMessagesHandler.RFC_ILLEGAL_OP, ErrorMessagesHandler.INVLIAD_RD_BLOCK, expectedBlock, blk);
						    			
										packet = new DatagramPacket(sendBuf, sendLen + 4, remoteIpAddress, remotePort);
							        	rcvHanlder.transferSocket.send(packet);	
							        	System.out.println("RRQ: Wrong block, Block:" + rcvHanlder.getPkgBlock(sendBuf) + ", Received block:" + blk + ", Data Length:" + (sendLen + 4));
									
							        	break;
							        	
									} else {
										
										System.out.println("Duplicate packet received, ignored.");
										// blk < expectedBlock, ignore since it can be explained as a duplicate
										
									}
								}
							} else {
								
								timedOutOrIOError = true;
							}
						}
					}
					if(timedOutOrIOError) {
						System.out.println("RRQ: Timedout or wrong block(sequence). Gave up and quit!");
						closeBufferedInputStream();
						closeFileInputStream();	
						break;
					}
					//It_3. Network error end
					
					if(ErrorPack){
						
						closeBufferedInputStream();
						closeFileInputStream();	
						break;
					}
					
					/*
		        	int receivedPackageDataLen = packet.getLength();
		        	
		        	dataBuf = packet.getData();	 
					
		        	int opCode = rcvHanlder.getPkgOpCode(dataBuf);
		        	if(opCode != 4) {
		    			ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
		    			invHld.errorHandler(ErrorMessagesHandler.RFC_ILLEGAL_OP, ErrorMessagesHandler.INVALID_OPCODE, 4, opCode);
		    			closeBufferedInputStream();
		    			closeFileInputStream();
			        	break;
		        	}
		        	*/
		        	
		        	int curBlock = rcvHanlder.getPkgBlock(dataBuf);
		        	//if(curBlock != expectedBlock) {
		    		//	ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
		    		//	invHld.errorHandler(ErrorMessagesHandler.ILLEGAL_OP, ErrorMessagesHandler.INVLIAD_RD_BLOCK, expectedBlock, curBlock);
	        		//	  closeBufferedInputStream();
	        		//    closeFileInputStream();
			        //	break;
		        	//}
		        	receivedPackageDataLen = packet.getLength();
		        	System.out.println("RRQ: Received ACK OpCode:" + opCode + ", Block:" + curBlock + ", Packet Length:" + receivedPackageDataLen);
			        					        	
			        System.out.println("WRR:          Local port(Host TID):" + rcvHanlder.transferSocket.getLocalPort() + " local IP: " + rcvHanlder.transferSocket.getLocalAddress());						        	
			        System.out.println("WRR:          remote port(remote TID):" + packet.getPort() + ", remote IP: " + packet.getAddress());
			        
			        //transfer completed
			        if(timeToBreak) {
		        		
	        			try {
							TimeUnit.SECONDS.sleep(2);
							
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

	        			closeBufferedInputStream();
	        			closeFileInputStream();		        		
	        		    break;
			        }
			        
			        
			        //read from file		        	        			        
				    sendLen = readfromFile(sendBuf);	//read into sendBuf starting from index 4
			        
			        if(sendLen != -1) {
				        
			        	expectedBlock = curBlock + 1;	
				        sendBuf[0] = 0;		//data
				        sendBuf[1] = 3;		//data
				        sendBuf[2] = (byte)((expectedBlock >> 8) & 0xff);
				        sendBuf[3] = (byte)(expectedBlock & 0xff);
				        
				        System.out.println("RRQ: Send Block:" + expectedBlock + ", Data Length:" + sendLen);
				        
			        	packet = new DatagramPacket(sendBuf, sendLen + 4, remoteIpAddress, remotePort);
			        	rcvHanlder.printContents(packet);
			        	rcvHanlder.transferSocket.send(packet);	
			        	
			        } else {
	        			try {
							TimeUnit.SECONDS.sleep(1);
							
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
	        			closeBufferedInputStream();
	        			closeFileInputStream();
			        	break;
			        }
			        			        
		        	if(sendLen < 512) {
		        		//done
		        		
		        		timeToBreak = true;
		        	}	 
		        	
				}	//while		
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
		finally {
			System.out.println("RRQ: ----------Read Request, completed. Socket closed.");	
			rcvHanlder.transferSocket.close();
		}
	}	
	
	Boolean processFirstPackage() throws IOException {
		Boolean result = false;
		
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
				
			if(openFileStream()) {
				
				sendLen = readfromFile(sendBuf);	//read into sendBuf starting from index 4
				
	        	if(sendLen != -1) {
	        		
					sendBuf[0] = 0;	//data high byte
					sendBuf[1] = 3;	//data low byte
					sendBuf[2] = (byte)(expectedBlock >> 8);	//block #1 high byte
					sendBuf[3] = (byte)(expectedBlock);			//block #1 low byte
	        		
	        		//sends the first data packet back to client. destination port is 'remotePort'.
					DatagramPacket packet = new DatagramPacket(sendBuf, sendLen + 4, remoteIpAddress, remotePort);	
					System.out.println("RRQ: Send Block:" + (((sendBuf[2] << 8) & 0xff00) + (sendBuf[3] & 0xff)) + ", Data Length:" + sendLen + 4);
					rcvHanlder.transferSocket.send(packet);				
					System.out.println("RRQ: local port(Host TID):" + rcvHanlder.transferSocket.getLocalPort() + ", remote port(remote TID):" + packet.getPort());
	
					result = true;					
	        	} 
			}
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
	




	Boolean openFileStream() {
		Boolean result = false;
	
		try {
			filePath = Paths.get(strgFileName);			
			File file = new File(strgFileName);
  
			fileInputStream = new FileInputStream(file);
			bufferedInputStream = new BufferedInputStream(fileInputStream);
			
			result = true;
		}
		catch(InvalidPathException | NullPointerException ex) {	//Paths.get() and new File()
			//Iteration #4
			System.out.println("RRQ: openFileStream. " + ex.getMessage());		
			ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
			invHld.errorHandler(ErrorMessagesHandler.RFC_UNDEFINED_SEE_MSG, ErrorMessagesHandler.INVALID_PATH, strgFileName);
		}
		catch(FileNotFoundException ex) {	//FileInputStream
			//Iteration #4
			System.out.println("RRQ: openFileStream. " + ex.getMessage());			
			ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
			invHld.errorHandler(ErrorMessagesHandler.RFC_FILE_NOT_FOUND, ErrorMessagesHandler.FILE_NOT_FOUND, strgFileName);
		}
		catch(Exception ex) {
			//Iteration #4
			ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
			invHld.errorHandler(ErrorMessagesHandler.RFC_UNDEFINED_SEE_MSG, ErrorMessagesHandler.READ_OPEN_FAILED, (strgFileName + ": " + ex.getMessage()) );
			ex.printStackTrace();
		}

		System.out.format("Created an inpur (read) file %s result: %s\n", strgFileName, result.toString());
		return result;
	}
	
    
    

	int readfromFile(byte[] buffer) {
		
		int read = -1;
		
		try {
			read = bufferedInputStream.read(buffer, 4, 512);
			
		} catch (IOException ex) {
    		//Iteration #4
			ErrorMessagesHandler invHld = new ErrorMessagesHandler(rcvHanlder);
			invHld.errorHandler(ErrorMessagesHandler.RFC_UNDEFINED_SEE_MSG, ErrorMessagesHandler.READ_FAILED, (strgFileName + ": " + ex.getMessage()) );
			read = -1;
		}

		return read;
	}
	

	void closeBufferedInputStream() {
		try {
			if(bufferedInputStream!=null) {
				bufferedInputStream.close();
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	void closeFileInputStream() {
		try {
			if(fileInputStream!=null) {
				fileInputStream.close();
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	

}
	

