import java.net.DatagramPacket;
import java.net.InetAddress;

//--------------------------------------------------// 
//SYSC 3303 Iteration 4 							//
//TFTP Server Program: InvalidRequestHandler.java	//
//													//
//Author: Jonathan Chan								//
//Student Number: 100936881							//
//													//
//Carleton University								//
//Department of Systems and Computer Engineering	//
//SYSC 3303 RealTime								//
//Concurrent Systems Winter 2016					//
//--------------------------------------------------//

public class ErrorMessagesHandler {
	
	ReceivedPacketHandler rcvHanlder;
	DatagramPacket pkg;
	InetAddress remoteIpAddress;
	int remotePort;
	ServerListener SvLHandler;
	
	
	public final static int RFC_UNDEFINED_SEE_MSG	= 0;	//It3.	Not defined, see error message (if any).
	public final static int RFC_FILE_NOT_FOUND		= 1;	//It4. 	File not found.
	public final static int RFC_ACCESS_VIOLATION	= 2;	//It4. 	Access violation.
	public final static int RFC_DISK_FULL			= 3;	//It4.	Disk full or allocation exceeded.
	public final static int RFC_ILLEGAL_OP			= 4;		//It2.	Illegal TFTP operation.
	public final static int RFC_UNKNOWN_TID			= 5;		//It2.	Unknown transfer ID.
	public final static int RFC_FILE_EXIST			= 6;	//It4.	File already exists.
	public final static int RFC_NO_SUCH_USER		= 7;	//		No such user.
	
	
	public final static int RQST_OPCODE = 0;
	public final static int FILENAME_NOT_TERMINATED = 1;
	public final static int MODE_NOT_TERMINATED = 2;
	public final static int INVALID_MODE = 3;
	public final static int INVALID_OPCODE = 4;
	public final static int WR_BLK_START_NOT_1 = 5;
	public final static int INVLIAD_WR_BLOCK = 6;
	public final static int INVLIAD_RD_BLOCK = 7;
	public final static int NO_FILENAME = 8;
	public final static int NO_MODE = 9;
	public final static int READ_OPEN_FAILED = 10;
	public final static int WRITE_OPEN_FAILED = 11;
	public final static int READ_FAILED = 12;
	public final static int WRITE_FAILED = 13;		//Disk full can cause this
	public final static int INVALID_PATH = 14;
	public final static int FILE_NOT_FOUND = 15;
	public final static int FILE_EXIST = 16;
	public final static int FILE_CREATE_FAILED = 17;
	public final static int PACKET_LONGER_THAN_516 = 18;
	public final static int UNKNOWNTID_INDEX = 19;
	
	public String[] errMsgs = {
	/* 0*/		"Opcode number neither 1 (RRQ) nor 2 (WRQ), but %d received.",						
	/* 1*/		"File name was not terminated with 0.",	
	/* 2*/		"Mode was not terminated with 0.",
	/* 3*/		"Invalid mode '%s' was received.",
	/* 4*/		"Expected package with opcode %d, but %d received.",
	/* 5*/		"Write data starting block number is not 1, but %d received.",
	/* 6*/		"Write data block number %d expected, but %d received.",
	/* 7*/		"Read ACK block number %d expected, but %d received.",
	/* 8*/		"File name was not found in the request package.",
	/* 9*/		"Mode was not found in the request package.",
	/*10*/		"Open file %s for reading failed.",
	/*11*/		"Open file %s for writing failed",
	/*12*/		"Read file %s failed, IO exception",
	/*13*/		"Write file %s failed, may be caused be disk full.",
	/*14*/		"Invalid file path, '%s'.",
	/*15*/		"File, %s, not found.",
	/*16*/		"File, %s, exist.",
	/*17*/		"Create file, %s, failed. Can be caused by Access Violation.",
	/*18*/		"Expected Packet to be 516 bytes, but received over 516 bytes",
	/*19*/		"Unknown transfer ID received"
	};
	
	
	public ErrorMessagesHandler(ReceivedPacketHandler rcvHdl) {	
		rcvHanlder = rcvHdl;
		pkg = rcvHanlder.receivedPacket;
	}
	
	public ErrorMessagesHandler(ReceivedPacketHandler rcvHdl, DatagramPacket wrongPacket) {	
		rcvHanlder = rcvHdl;
		pkg = wrongPacket;
	}
	
	public void errorHandler(int errorCode, int index) {
		sendErrorMessage(errorCode, errMsgs[index]);
	}
	
	public ErrorMessagesHandler(ServerListener SvL){
		
		SvLHandler = SvL;
		pkg = SvLHandler.ReceivePacket;
		
	}
	
	public void errorHandler(int errorCode, int index, int n) {
		sendErrorMessage(errorCode, String.format(errMsgs[index], n));
	}
		
	public void errorHandler(int errorCode, int index, int n, int m) {
		sendErrorMessage(errorCode, String.format(errMsgs[index], n, m));
	}
	
	public void errorHandler(int errorCode, int index, String s) {
		sendErrorMessage(errorCode, String.format(errMsgs[index], s));
	}
	
	private void sendErrorMessage(int errorCode, String errorString) {
		try {
			
			byte[] err = null;

			remoteIpAddress = pkg.getAddress();
			remotePort = pkg.getPort(); 
	
			err = errorString.getBytes();	
			byte[] dataBuf = new byte[err.length + 4];

			if(dataBuf.length > 4) {
				//Error opcode high byte
				dataBuf[0] = 0;	
				dataBuf[1] = 5;	
				
				//Error code
				dataBuf[2] = 0;
				dataBuf[3] = (byte)errorCode;		
				
				System.arraycopy(err, 0, dataBuf, 4, err.length);				
				DatagramPacket packet = new DatagramPacket(dataBuf, dataBuf.length, remoteIpAddress, remotePort);				
				rcvHanlder.transferSocket.send(packet);	//sends the error packet.	
				
				System.out.println("ERR: local port(Host TID):" + rcvHanlder.transferSocket.getLocalPort() + ", remote port(remote TID):" + packet.getPort());
				System.out.println("ERR: message> " + errorString);
			}
			
		}
		catch (Exception ex) {
			
		}
	}
}

