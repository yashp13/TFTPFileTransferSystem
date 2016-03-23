//--------------------------------------------------// 
//SYSC 3303 Assignment 1							//
//TFTP Server Program: ReceivedPacketHandler.java	//
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

//Instead of extending Thread, implements Runnable
//Extending Thread screws up the thread name. implements Runnable provides the correct Thread name
public class ReceivedPacketHandler implements Runnable {

	static final int maxBytes = 516;			// Maximum TFTPpacket size to be used to create byte buffers
	static final int minBytes = 4;				// Minimum
	
	DatagramPacket responsePacket, receivedPacket;
	public DatagramSocket transferSocket;
	
	static final int RRQ = 1; 
	static final int WRQ = 2; 
	
	public Boolean continueRun = true;
	
	public ReceivedPacketHandler(DatagramPacket receivedPacket) {
		
		this.receivedPacket = receivedPacket;	
		
	}
	
	public void run(){

		byte[] pkgData = new byte[1024];
		
		try {
			
			
			pkgData = receivedPacket.getData();
			int opCode = getPkgOpCode(pkgData);
			
			//Instead of 1)randomly generate a TID, 2)check whether it is as a free port, and 3)assign it to UDP as source port as RFC
			//java net DatagramSocket gives a 'free port' as local port and assign it to the socket. This 'free port' is TID.
			transferSocket = new DatagramSocket();
			
			switch(opCode) {
				case RRQ:
					ReadRequestHandler rrqHld = new ReadRequestHandler(this);
					rrqHld.rrqResponseHandler();	
					break;
				
				case WRQ:
					WriteRequestHandler wrqHld = new WriteRequestHandler(this);
					wrqHld.wrqResponseHandler();					
					break;
					
				default:
					ErrorMessagesHandler invHld = new ErrorMessagesHandler(this);
					invHld.errorHandler(ErrorMessagesHandler.RQST_OPCODE, opCode);
					
					System.out.println("Invalid request op code '" + opCode + "' received.");
		    		//throw new InvalidRequestException("Invalid request op code " + opCode + ".");
			}	
			
		} catch (SocketException e) {
			
			e.printStackTrace();			
		}
		finally {
			transferSocket.close();
		}
		System.out.format("ReceivedPacketHandler. '%s' exited.\n\n", Thread.currentThread().getName());	
	}
	
	//opcode is the first two bytes in big endian
	public int getPkgOpCode(byte[] b) {
		return ((b[0] << 8) & 0xff00) + (b[1] & 0xff);
	}
	
	public Boolean isDataPackage(byte[] b) {
		return (getPkgOpCode(b) == 3);
	}	
	public Boolean isAckPackage(byte[] b) {
		return (getPkgOpCode(b) == 4);
	}	
	public Boolean isErrorPackage(byte[] b) {
		return (getPkgOpCode(b) == 5);
	}
	
	public int getErrorCode(byte[] b){
		return ((b[2] << 8) & 0xff00) + (b[3] & 0xff);
		
	}
	//opcode is the first two bytes in big endian
	public int getPkgBlock(byte[] b) {
		return ((b[2] << 8) & 0xff00) + (b[3] & 0xff);
	}
	
	protected void printContents(DatagramPacket p){

       
        byte[] data = new byte[p.getLength()];
        System.arraycopy(p.getData(), 0, data, 0, data.length);

        System.out.print("Containing (String): \n");
        String received = new String(p.getData(),0,p.getLength());   
        System.out.println(received + "\n");

	}
}
