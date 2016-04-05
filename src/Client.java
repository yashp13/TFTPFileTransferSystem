import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.ArrayList;
public class Client {
	
	private static int  FILENAME 	 = 0;
	private static int  FILELOCATION = 1;
	private static int  INPUTS 		 = 2;
    private static byte ZEROBYTE 	 = 0;
    private static byte ONEBYTE 	 = 1;
    private static byte TWOBYTE 	 = 2;
    private static byte THREEBYTE	 = 3;
    private static byte FOURBYTE	 = 4;
    private static byte FIVEBYTE	 = 5;
    private static byte SIXBYTE	 = 6;
    private static int  MODE 		 = 0;
    private int TID;
    private int blockNumber = 1;
    private int shutdown;
    private int serverTID;
    private boolean initialPack = true;
    protected static InetAddress add;
    protected static InetAddress swag;
  
    private int errorCode;
    private DatagramSocket sendReceiveSocket;
    private DatagramPacket sendPacket, receivePacket, sendPacket2;


    
    
    public Client(){
        try {
            sendReceiveSocket = new DatagramSocket();
           // sendReceiveSocket.bind(new InetSocketAddress(InetAddress.getByName("134.117.59.51"),sendReceiveSocket.getLocalPort()));
            //initialPack = true;
        } catch (SocketException se){
            se.printStackTrace();
            System.exit(1);
        }       
    }
    
    //------------------------------------------------------------------------------------------------------------------------
    /**
     * Identify if the DatagramPacket received was an ACK.
     * ACK format 0 4 X X - that is a zero byte then a 4 byte then two other bytes.
     * 
     * @param packet of type DatagramPacket is packet to check
     * @return boolean
     */
    
    private boolean isACK(DatagramPacket packet){ 
        if(packet.getLength()== 4){						//First check that ack contains 4 bytes
            if(packet.getData()[0] == ZEROBYTE){		//Check if the first byte is zero
                if(packet.getData()[1] == FOURBYTE){	//Check second byte is a four
                	return true;
                }
            }   
        }
        return false;
    }

    //------------------------------------------------------------------------------------------------------------------------
    //to identify if the block received is a data block
    private boolean isDATA(byte[] t){
        byte threeByte = 3;
            //check if first byte is zero
            //DATA format 0 3 | X X | n bytes - that is a zero byte then a 3 byte then two other bytes then n bytes of data
        	//check if packet size is 516
        if(t.length <= 516){
            if(t[0] == ZEROBYTE){
                if(t[1] == threeByte){
                   return true;
                    
                }
            }
        }
            return false;
    }
        
        
        
    
    
    //checks if DATA packet received is last one
    //by checking if the contents of the data is less than 512 bytes.
    
    private boolean lastDataBlock(byte[] t){
        if( (t.length - 4) != 512)return false;
        return true;
    }
    
    //------------------------------------------------------------------------------------------------------------------------
    /**
     * Checks weather the packet received is an error packet
     * @param packet of type DatagramPacket
     * @return boolean 
     */
    
    private boolean isErrorPacket(DatagramPacket packet){    	
    	//Check if the Packet has the error format 0501<StringMessage>0
    	if(packet.getData()[0] == ZEROBYTE && packet.getData()[1] == FIVEBYTE){
    		
    		return true;
    	}
		return false;
    }
    
    
    //if error code is 4
    private boolean isErrorPacketCodeFour(DatagramPacket packet){    	
    	//Check if the Packet has the error format 0501<StringMessage>0
    	if(packet.getData()[0] == ZEROBYTE && packet.getData()[1] == FIVEBYTE && packet.getData()[2] == ZEROBYTE && packet.getData()[3] == FOURBYTE ){
    		
    		return true;
    	}
		return false;
    }
    
    //if error code is 5
    private boolean isErrorPacketCodeFive(DatagramPacket packet){    	
    	//Check if the Packet has the error format 0501<StringMessage>0
    	if(packet.getData()[0] == ZEROBYTE && packet.getData()[1] == FIVEBYTE && packet.getData()[2] == ZEROBYTE && packet.getData()[3] == FIVEBYTE){
    		
    		return true;
    	}
		return false;
    }
    
    //if error code is 3
    private boolean isErrorPacketCodeThree(DatagramPacket packet){
    	if(packet.getData()[0] == ZEROBYTE && packet.getData()[1] == FIVEBYTE && packet.getData()[2] == ZEROBYTE && packet.getData()[3] == THREEBYTE){
    		return true;
    	}
    	return false;
    }
    
    //if error code is 2
    private boolean isErrorPacketCodeTwo(DatagramPacket packet){
    	if(packet.getData()[0] == ZEROBYTE && packet.getData()[1] == FIVEBYTE && packet.getData()[2] == ZEROBYTE && packet.getData()[3] == TWOBYTE){
    		return true;
    	}
    	return false;
    }
    
    private boolean isErrorPacketCodeSix(DatagramPacket packet){
    	if(packet.getData()[0] == ZEROBYTE && packet.getData()[1] == FIVEBYTE && packet.getData()[2] == ZEROBYTE && packet.getData()[3] == SIXBYTE){
    		return true;
    	}
    	return false;
    }
    
    private boolean isErrorPacketCodeOne(DatagramPacket packet){
    	if(packet.getData()[0] == ZEROBYTE && packet.getData()[1] == FIVEBYTE && packet.getData()[2] == ZEROBYTE && packet.getData()[3] == ONEBYTE){
    		return true;
    	}
    	return false;
    }
    
    private int getErrorCode(DatagramPacket packet){
    	if(packet.getData()[0] == ZEROBYTE && packet.getData()[1] == FIVEBYTE && packet.getData()[2] == ZEROBYTE){
    		return packet.getData()[3] & 0xff;
    	}
    	return -1;
    }
    
   //----------------------------------------------------------------------------------------------------
    
    public void restartApplication()
    {
      //sendReceiveSocket.close();
      //Client C = new Client();
       blockNumber = 1;
       initialPack = true;
    
      blockNumber = 1;
      ClientAlgorithm();
      System.exit(0);
    }
    //close the socket and restart transfer
    
    //------------------------------------------------------------------------------------------------------------------------
    /**
     * Creates a request packet to send to the server.  The packet contains a filename the server will write to, the port
     * number the packet will be sent to and wether it is a read request (RRQ) packet or a write request (WRQ) packet.
     * The data is send in the byte format: (1 or 2) some_file 0 some_mode 0
     * 
     * @param Key of type String specifies RRQ or WRQ
     * @param filename of type String is the file server will write too
     * @param PORT of type in is where he packet will be sent
     * @return RequestPacket
     * @throws UnknownHostException
     */
    
    private DatagramPacket  CreateRequestPacket(String Key, String filename, int PORT) throws UnknownHostException{
    	byte request[] = new byte[100]; 	//Make a byte array to be the RRQ or WRQ;    
    	int lastArrayIndex = 0; 			//used to keep track of last unused index in array request & is also array size
    	DatagramPacket requestPacket = null;
             								
        //PREPARING BYTE ARRAY TO BE SENT 
        request[lastArrayIndex] = ZEROBYTE;	//Format: 0 (1 or 2) some_text 0 some_text 0
        lastArrayIndex++;					//First Byte in array always 0
        //if read request RRQ make second array element 1 if write request WRQ make second array element a 2 byte
        request[lastArrayIndex] = (Key.equals("r")) ? ONEBYTE : TWOBYTE; 
        lastArrayIndex++;  
        
        byte str[] = filename.getBytes(); 	//Convert String to byte array
        for(int j = 0; j < str.length;j++){
        	request[lastArrayIndex] = str[j];
        	lastArrayIndex++;
        }
        request[lastArrayIndex] = ZEROBYTE;	//0 byte after filename
        lastArrayIndex++;
        
        String mo = "OcTeT";				//convert string mode to bytes then,
        byte mode[] = mo.getBytes(); 		//append mode to byte array request
            
        for(int j = 0; j < mode.length;j++){
        	request[lastArrayIndex] = mode[j];
        	lastArrayIndex++;
        }
        request[lastArrayIndex] = ZEROBYTE; //0 byte after mode
        lastArrayIndex++;
            
        //CONSTRUCT DATAGRAM PACKET TO BE SENT
        request = Arrays.copyOfRange(request,0,lastArrayIndex);	//Trim relevant data int array
       
       	requestPacket = new DatagramPacket(request, request.length, add, PORT);
    	return requestPacket;
    }
    
    //--------------------------------------------------------------------------------------------------------------------------
    //134.117.59.52
    /**
     * Sends a DatagramPacket through the DatagramSocket created
     * in the Client Constructor
     * 
     * @param packet
     * @throws IOException
     */
    private void TransmitPacket(DatagramPacket packet) throws IOException{
    	sendReceiveSocket.send(packet);
        System.out.println("\nClient: Sending Packet");
        DisplayPacketInfo(packet, packet.getData(), "To");
    }
    
    ///--------------------------------------------------------------------------------------------------------------------------
    // Sends error for different TID
   private void differentTIDError(){
	   System.out.println("Different TID received. Error Code 5.");
   }
   
   //Sends error code 4 for wrong block number
   private void sendErrWrongBlockNumber(){
       System.out.println("Invalid TFTP Operation Wrong Block Number. ERROR CODE 4");
   }
   
   private void sendErrWrongPackSize(){
       System.out.println("Invalid TFTP Operation Wrong Packet Size. ERROR CODE 4");
   }
   
   private void sendErrWrongOPCode(){
       System.out.println("Invalid TFTP Operation Wrong Op Code. ERROR CODE 4");
   }
   
   
   private void AccessViolationError() throws UnknownHostException{
       System.out.println("ccess Violation. Error Code 2.");
   }
    
    private void ObtainPacketWithTimeoutGiveup(){
        byte data[] = new byte[518]; //518 to check for invalid larger packets
        receivePacket = new DatagramPacket(data, data.length);
        
        try {
			sendReceiveSocket.setSoTimeout(5000);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        for(int i = 0 ; i <= 4 ; i ++){
        	if(i<4){  
             try {//Wait for Packet from server, Block until packet is received
            	 System.out.println("CLIENT: ---------------Waiting For New Packet\n" );
                 sendReceiveSocket.receive(receivePacket);  
                 //setting the serverTID to the TID of the initial packet
                 if(initialPack) serverTID = receivePacket.getPort();
                 initialPack = false;
                 
                 System.out.println("server: " + serverTID + "  this TID: " + receivePacket.getPort());
                 if(receivePacket.getPort() != serverTID){
                	 i = 0; // restart number of waits 
                 	 System.out.println("CLIENT: --------------------------------Different TID received...Ignored\n" );
                   	 DisplayPacketInfo(receivePacket, receivePacket.getData(), "From");
                	 differentTIDError();
                 	continue; //ignored packet
                 }
                 
                 // make a be equal to the current received block number
                 int a = ((receivePacket.getData()[2] <<8) &0xff00) +( receivePacket.getData()[3] & 0xff);
                 
                 //If not error packet check block number
                 if(!isErrorPacket(receivePacket) && a > blockNumber){
                	 System.out.println("Client: Invalid Block Number-----------Error Code 4");
                	 System.out.println("Block Number Received: " + a); 
                	  System.out.println("Block Number Expected: " + (blockNumber)); 
                	 System.out.println("CLIENT: Shutting Down Transfer");
                	 sendErrWrongBlockNumber();
                	 restartApplication();
                 }
                 
                 //if block number is less than expected then resend ack with received block number
                 
                if(!isErrorPacket(receivePacket)&& a < blockNumber){ // block number recieved less than expected
               	  i = 0; // restart count even if incorrect packet was received
             	  System.out.println("Client: --------------------Duplicate Packet Received-----------------"); 
             	 DisplayPacketInfo(receivePacket, receivePacket.getData(), "From");
              	  System.out.println("Block Number Received: " + a); 
             	  System.out.println("Block Number Expected: " + (blockNumber)); 
             	 System.out.println("Resending ack with Block Number: " + a); 
             	 byte[] bno = new byte[4];
             	  bno[0]= ZEROBYTE;	
                  bno[1]= FOURBYTE;		//Block number
             	  bno[2]= (byte)(((a) >> 8) % 0xffff);//BlockNumber shift when 128	
                  bno[3]= (byte)((a) % 0xffff);		//Block number
             	 
             	sendPacket = new DatagramPacket(bno, bno.length, add, receivePacket.getPort());
             	 TransmitPacket(sendPacket);
              	  break;
               }
                 
                 //client sends block no equal to received block number every time
//                 if(a != blockNumber){
//                	 blockNumber =a;
//                 }
                 
                 if(receivePacket.getLength()<= 516){
                 //Display Packet if Appropriate Packet Received
                 DisplayPacketInfo(receivePacket, receivePacket.getData(), "From");
                 // break out of loop if appropriate packet is received
                 }
                 break;
             }
             catch(SocketTimeoutException e) {
             	System.out.println("Client: --------------------Packet Wait Timeout.-----------------");
             	continue;
             }
             catch(IOException e) {
            	 e.printStackTrace();
                 System.exit(1);
             }
        }else {
        	System.out.println("Client: --------------------Packet Time out Max Times. Restarting Client-----------------");
    		restartApplication();
        }
        }
      
    }
 

    
    ///------------------------------------------------------------------------------------------------------------------------
    private void ObtainPacketWithTimeoutRetransmit(){
    	byte data[] = new byte[518]; //518 to check for invalid larger packets
        receivePacket = new DatagramPacket(data, data.length);
        
          
        
        try {
			sendReceiveSocket.setSoTimeout(4000);
		} catch (SocketException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}   // Set socket to timeout after 1 second
        
        //retransmission loop, keep going until appropriate packet is received
        for(int i = 0 ; i <= 4 ; i++){
        	if(i <= 3 ){
        	try {//Wait for Packet from server, Block until packet is received or timed out
        		 System.out.println("CLIENT: ---------------Waiting For New Packet\n" );
                sendReceiveSocket.receive(receivePacket);
              //setting the serverTID to the TID of the initial packet
                if(initialPack) {
                	serverTID = receivePacket.getPort();
                }
                initialPack = false;
                
                if(receivePacket.getPort() != serverTID){
               	 //differentTIDError();
               	 System.out.println("CLIENT: --------------------------------Different TID received...Ignored\n" );
               	 DisplayPacketInfo(receivePacket, receivePacket.getData(), "From");
               	 differentTIDError();
                	continue;  //ignore and wait for next packet
                }
                
              
               
                //DisplayPacketInfo(receivePacket, receivePacket.getData(), "S");
                //Checking for error packets
                if(isErrorPacketCodeFour(receivePacket) || isErrorPacketCodeFive(receivePacket) || isErrorPacketCodeThree(receivePacket)
                		|| isErrorPacketCodeTwo(receivePacket) || isErrorPacketCodeSix(receivePacket) || isErrorPacketCodeOne(receivePacket))
                {
                	System.out.println("CLIENT: -----------------ERROR CODE " + getErrorCode(receivePacket) +" RECEIVED");
                	DisplayPacketInfo(receivePacket, receivePacket.getData(), "From");
                	System.out.println("CLIENT: -----------------SHUTTING DOWN and Restarting Client");
                	//in.close();

                	restartApplication();
                }
                
                
                if(!isACK(receivePacket)){
                	if(receivePacket.getLength() > 4){
                		System.out.println(" CLIENT: ----------Invalid Packet Size");
                    	DisplayPacketInfo(receivePacket, receivePacket.getData(), "S");
                    	sendErrWrongPackSize();
                    	restartApplication();
                	}else{
                	System.out.println(" CLIENT: ----------Invalid Opcode Received");
                	DisplayPacketInfo(receivePacket, receivePacket.getData(), "S");
                	restartApplication();
                	}
                }
                
                // if block number received is less than what is expected ignore and wait for the next one
              //  Byte b = receivePacket.getData()[3];
                int a = ((receivePacket.getData()[2] <<8) &0xff00) +( receivePacket.getData()[3] & 0xff);
                
                
              if( a < blockNumber -1){ // block number recieved less than expected
            	  i = 0; // restart count even if incorrect packet was received
            	  System.out.println("Client: --------------------Ignored A Packet-----------------"); 
            	  System.out.println("Block Number Received: " + a); 
            	  System.out.println("Block Number Expected: " + (blockNumber-1)); 
            	  continue;
              }
              if(a > blockNumber -1 || receivePacket.getLength() > 4)
              {// block number received greater than expected restart
            	  i = 0; // restart count even if incorrect packet was received
            	  System.out.println("Client: --------------------Error Code 4 received. Shutting Down-----------------"); 
            	  System.out.println("Block Number Received: " + a); 
            	  System.out.println("Block Number Expected: " + (blockNumber-1)); 
            	  sendErrWrongBlockNumber();
            	  restartApplication();
              }
              
                //Display Packet if Appropriate Packet Received
                DisplayPacketInfo(receivePacket, receivePacket.getData(), "From");
                // break out of loop if appropriate packet is received
                break;
            } catch(SocketTimeoutException e) {
            	System.out.println("Client: --------------------Packet Timeout. RESENDING PACKET-----------------");   
            	try {
					TransmitPacket(sendPacket);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        }
        	else {
        		System.out.println("Client: --------------------Packet Time out Max Times. Restarting Client-----------------");
        		restartApplication();
        	}
        }
       
        
        try {
			sendReceiveSocket.setSoTimeout(0);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   //remove timeout so it doesn't affect send
       
    }
 

    ///--------------------------------------------------------------------------------------------------------------------------
    /**
     * Displays information about a DatagramPacket
     * @param packet of type DatagramPakcet
     * @param buffer of type byte[] contains the packets data
     * @param location of type String specifies incoming or outgoing packet "To" or "From"
     */
    private void DisplayPacketInfo(DatagramPacket packet, byte[] buffer, String location){
        byte data[] = new byte[packet.getLength()];
        System.arraycopy(buffer, 0, data, 0, data.length);  
        System.out.println(location + ": " + packet.getAddress());
        System.out.println("Port: " + packet.getPort());
        System.out.println("Length: " + packet.getLength());
        System.out.print("Containing (bytes): " );
        System.out.println(Arrays.toString(data));
        System.out.print("Containing (String): ");  
        System.out.println((new String(data,0,packet.getLength())) + "\n");
    }
    ///--------------------------------------------------------------------------------------------------------------------------
    

    
    
    private void ClientAlgorithm(){
    	HashMap<String, String[]> input = getUserInput();
    	
    	if(input.containsKey("w")){
    		try { //Handles All Write Request
    			System.out.println("\nClient: WILL WRITE DATA TO SERVER!");
    			sendPacket = CreateRequestPacket("w", input.get("w")[FILENAME], TID);
    			TransmitPacket(sendPacket); //Send WRQ Request Packet
    			ObtainPacketWithTimeoutRetransmit();
                WriteRequest(input.get("w")[FILENAME],input.get("w")[FILELOCATION],receivePacket.getPort());
			} catch (UnknownHostException e) {
				System.out.println("ERROR: UNABLE TO CREATE WRQ REQUEST PACKET");
			} catch (IOException e) {
				System.out.println("ERROR: UNABLE TO SEND WRQ PACKET");
			}
    		
    		
    	}else if(input.containsKey("r")){
    		try {//Handlers All Read Requests
    			 System.out.println("\nClient: WILL READ DATA FROM SERVER!");
				sendPacket = CreateRequestPacket("r", input.get("r")[FILELOCATION], TID);
				/*//Makes output file with inputted filename
		         File outputFile = new File(input.get("r")[FILENAME]);
		         
		         //if file doesnt exist make it again
		         if (!outputFile.exists()) {
						try{
							outputFile.createNewFile();
						}catch(IOException e){
							System.out.println("-----------------ERROR CODE 2: ACCESS VIOLATION-----------------");
							restartApplication();
						}
					}*/
				TransmitPacket(sendPacket);
				//ObtainPacketWithTimeoutGiveup();
				readRequest(input.get("r")[FILENAME],input.get("r")[FILELOCATION]);
			} catch (UnknownHostException e) {
				System.out.println("ERROR: UNABLE TO CREATE RRQ REQUEST PACKET");			
			} catch (IOException e) {
				//e.printStackTrace();
				System.out.println("--------------------ERROR CODE 2: ACCESS VIOLATION--------------------");
				/*try {
					AccessViolationError();
				} catch (UnknownHostException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}*/
				restartApplication();
			}
    	} 
    	                 
    }
    
    ///--------------------------------------------------------------------------------------------------------------------------
  
    /**
     * Gets the name of the file to transfer from the user. 
     * Function searches current working directory for the file.
     * Gets the Mode for client to operate in
     * 1. For Normal mode, 2. For Testing Mode
     * 
     * @return a String containing the filename
     */
    
    private HashMap<String,String[]> getUserInput(){
    	Scanner sc;
    	HashMap<String,String[]> res;
        String operation;
        String filename;   
        String file_location;
        
        
        //Ask user for the mode
        sc = new Scanner(System.in);
        while(true){ //making sure mode is either 1 or 2
            System.out.print("Enter 1 for NORMAL MODE , 2 for TEST MODE or 3 to SHUTDOWN: ");
            if(!sc.hasNextInt()){//only accept integers
            // System.out.print("Enter 1 for NORMAL MODE , 2 for TEST MODE or 3 to SHUTDOWN: \n");
            	
             try{
            	sc.nextInt();
             }catch(InputMismatchException e){
            	
            	 sc = new Scanner(System.in);
            	 continue;
             }
                
            }//Update Mode once integer is received
           
            MODE = sc.nextInt();
          
            if(MODE == 1) { 
            	TID = 69;
            	//Get IP address of Server
            	if(initialPack && add == null)
            		getIP();
            	else{
            		Scanner newshit = new Scanner(System.in);
            		System.out.print("Do you want to change the Server's IP (y/n)? ");
                	if(newshit.nextLine().equalsIgnoreCase("y")) 
                		getIP();
            	}            	
            	break;//SetNormalMode
            }else if(MODE == 2) {
            	TID = 61;
            	//Get IP address of Errorsim
            	if(initialPack && add == null)
            		getIP();
            	else{
            		Scanner newshit2 = new Scanner(System.in);
            		System.out.print("Do you want to change the Server's IP (y/n)? ");
            		String x = newshit2.nextLine();
                	if(x.equalsIgnoreCase("y")) 
                		getIP();
            	}
            	break;//Set Testing Mode
            }else if(MODE == 3) {
            	sendReceiveSocket.close();
            	System.out.println("Shutting Down Succesfully");
            	System.exit(0);
            	break;//Set Testing Mode
            }
        }
        sc.nextLine();
        
        //Asks user for the desired operation
        System.out.print("\nPlease enter File Transfer Operation\n");
        System.out.print("INPUT 'r' for Read Operation OR 'w' for Write:");
        //sc = new Scanner(System.in); 
        operation = sc.nextLine();
        
        //Check if valid characters were provided
        while(!operation.equals("r")  && !operation.equals("w")){
            System.out.println("Operation not found try again\n");
            System.out.print("INPUT 'r' for Read Operation OR 'w' for Write:");
            operation = sc.nextLine();
        }
        String printss = operation.equals("w") ? "\nDATA WILL BE WRITTEN TO SERVER!\n" : "\nDATA WILL BE READ FROM SERVER!\n";
        System.out.print(printss);
        //Get location of file to read from
        System.out.print("Enter Filename OR Path: ");
        file_location = sc.nextLine();
        
        //Check if the file specified exists, if not prompt user for another
        if(operation.equals("w")){
        while(!new File(file_location).exists()){
        	System.out.print("\nFILE NOT FOUND");
        	System.out.print("\nPlease Enter the Path for the input file: ");
        	file_location = sc.nextLine();
        }
        }
        //Ask user for the file to 
        
        
        System.out.print("Please enter filename to write to: ");
        filename = sc.nextLine();
       // sc.close();

        //Create string array to hold information regarding the file
        String[] s = new String[INPUTS];
        s[FILENAME] = filename;
        s[FILELOCATION] = file_location;
        //Put all data in a HashMap and return 
        res = new HashMap<String,String[]>();
        res.put(operation, s);
        return res;
    }
    
    private void getIP(){
    	System.out.print("Enter Receiver's IP Address or 'local' for local host: ");
		String IP; 
		//IP = sc.nextLine();
		Scanner sc2 = new Scanner(System.in);
		while(true){
			IP = sc2.nextLine();
			if(IP.equalsIgnoreCase("local")){break;}
    	  
			String[] parts = IP.split("\\.");
			if( parts.length == 4 ) {
				break;
			}else{
				System.out.print("Invalid IP Format. Re-enter: ");
			}
		}
		//Make inetaddress with IP given if not throw exception 
		if(IP.equalsIgnoreCase("local")){
			try {
				add = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			try {
				add = InetAddress.getByName(IP);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.print("InetAddress creation Failed\n");
				System.out.print("Restarting Client\n");
				restartApplication();
			}
		}
    }
    
    //-------------------------------------------------------------------------------------------------------------------------
   
  
    ///--------------------------------------------------------------------------------------------------------------------------
    
    /**
     * This method handles write requests to the server.  It reads data from a file
     * specified by the user and sends data packets of 512Bytes 
     * @param input
     * @throws FileNotFoundException
     * @throws IOException
     */


    @SuppressWarnings("unused")
	private void WriteRequest(String filename, String fileLocation, int PORT) throws FileNotFoundException, IOException{
        
    	
        File file = new File(fileLocation);
        BufferedInputStream  in = new BufferedInputStream (new FileInputStream(file));
        byte[] data = new byte[512];
        byte[] message;
        int n;
        int file_size = (int)(file.length());
        boolean done =  true;
        int a;
        blockNumber = 1;
        
        /* Read the file in 512 byte chunks. */
        while (done){//(n = in.read(data)) != -1) {
        	
            if(file_size < 512){            	
            	done = false;
            	System.out.println("at stopping point");
            	//break;
            } 
        	in.read(data);
        	//Adjust the file size accordingly
        	if(file_size >512){
        		message = new byte[516]; 
            }else if (file_size > 0){
            	message = new byte[file_size+4]; 
            }else {
            		message = new byte [4];
            		 message[0]= ZEROBYTE;							//Setup Byte structure 
                     message[1]= THREEBYTE;							//03 Data opcode
                     message[2]= (byte)((blockNumber >> 8) % 0xffff);//BlockNumber shift when 128	
                     message[3]= (byte)(blockNumber % 0xffff);		//Block number
                     blockNumber ++;	
                     
                   
                     sendPacket = new DatagramPacket(message, message.length, add, PORT);
                     TransmitPacket(sendPacket); 					//Send the request data packet
                     
                     
                
                    
                     //retransmission loop, keep going until appropriate packet is received
                     ObtainPacketWithTimeoutRetransmit();
            		
                     System.out.println("WRQ: -----------Write Request, Completed, Starting new Transfer");
                     System.out.println("WRQ: New Transfer Started");
                     restartApplication();
                     
            }
        	file_size = file_size - 512;
            message[0]=	ZEROBYTE;							//Setup Byte structure 
            message[1]= THREEBYTE;							//03 Data opcode
            message[2]= (byte)((blockNumber >> 8) % 0xffff);//BlockNumber shift when 128	
            message[3]= (byte)(blockNumber % 0xffff);		//Block number
            blockNumber ++;									//Increment the block number each iteration
            for(int i = 0; i < message.length-4; i++ ){
                message[i+4] = data[i];
            }
             
       
            sendPacket = new DatagramPacket(message, message.length, add, PORT);
            TransmitPacket(sendPacket); 					//Send the data packet

           
            //retransmission loop, keep going until appropriate packet is received
            
            ObtainPacketWithTimeoutRetransmit();
            
            if(isErrorPacketCodeFour(receivePacket) || isErrorPacketCodeFive(receivePacket) || isErrorPacketCodeThree(receivePacket)
            		|| isErrorPacketCodeTwo(receivePacket) || isErrorPacketCodeSix(receivePacket)){
            	System.out.println("CLIENT: -----------------ERROR CODE RECEIVED");
            	DisplayPacketInfo(receivePacket, receivePacket.getData(), "From");
            	System.out.println("CLIENT: -----------------SHUTTING DOWN and Restarting Client");
            	in.close();
   
            	restartApplication();
            }   
            
          //retransmission loop, keep going until appropriate packet is received
            //ObtainPacketWithTimeoutRetransmit();
            
           // if(message.length < 516){            	
           // 	done = false;
           // 	System.out.println("at stopping point");
           // 	//break;
           // }
            //System.out.println(file_size); //TODO remove later
        } 
        in.close(); 
        System.out.println("WRQ: -----------Write Request, Completed, Starting new Transfer");
        System.out.println("WRQ: New Transfer Started");
        restartApplication();
    }
    
    /**
     * 
     * @param filename
     * @param fileLocation
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void readRequest(String filename, String fileLocation) throws FileNotFoundException, IOException
    {
         boolean stop = false;
         //make the ack
         
         blockNumber = 1;
         byte[] ack = new byte[4];
         
         
         //Makes output file with inputted filename
         File outputFile = new File(filename);
         
         //if file doesnt exist make it again
         if (!outputFile.exists()) {
		
					outputFile.createNewFile();
			
			}

         FileOutputStream fop = new FileOutputStream(outputFile);
         BufferedOutputStream out = new BufferedOutputStream(fop);
         
         //while still receiving packets
         while(stop == false){       
        	 ack[0] = ZEROBYTE;
             ack[1] = FOURBYTE;
             ack[2]= (byte)((blockNumber >> 8) % 0xffff);//BlockNumber shift when 128	
             ack[3]= (byte)(blockNumber % 0xffff);		//Block number
        	 int a;
        	 
        	 
        	 
        	 
             //wait for next data packet
             byte data[] = new byte[516];
             receivePacket = new DatagramPacket(data, data.length);
             
            
             
             //Wait for Packet from server
             ObtainPacketWithTimeoutGiveup();
             ack[2]= (byte)((blockNumber >> 8) % 0xffff);//BlockNumber shift when 128	
             ack[3]= (byte)(blockNumber % 0xffff);		//Block number
             
             if(isErrorPacketCodeFour(receivePacket) || isErrorPacketCodeFive(receivePacket) || isErrorPacketCodeThree(receivePacket)
             		|| isErrorPacketCodeTwo(receivePacket) || isErrorPacketCodeSix(receivePacket) || isErrorPacketCodeOne(receivePacket)){
            	System.out.println("CLIENT: -----------------ERROR RECEIVED");
             	System.out.println("CLIENT: -----------------SHUTTING DOWN TRANSFER");
             	fop.close();
             	System.out.println("CLIENT: -----------------Deleting: " + outputFile);
             	outputFile.delete();
             	System.out.println("STARTING NEW TRANSFER");
             	restartApplication();
             }
        
             
             // make a be equal to the current received block number
             int bno = ((receivePacket.getData()[2] <<8) &0xff00) +( receivePacket.getData()[3] & 0xff);
           
              sendPacket = new DatagramPacket(ack, ack.length, add, receivePacket.getPort());
             
              if(bno == blockNumber){
              TransmitPacket(sendPacket);
              }
             byte t[] = receivePacket.getData();
              
             
             //trim array to only include actual data
             t = Arrays.copyOfRange(t,0 ,receivePacket.getLength());
            		 
             //trim byte array to only include the last 512 bytes
             byte[] tw =  Arrays.copyOfRange(t,4 ,receivePacket.getLength());
            
         
             
             //Only write if block number received is current block number expected
             if(bno == blockNumber){
         //check if proper data blocks are received
             if(isDATA(t)){
            	 try{
            		 fop.write(tw);
            	 } catch (IOException e){
            		 System.out.println("---------- Disk full. Restarting client. ----------");
            		 restartApplication();
            	 }
             }else {
            	 if(t.length > 516){
            	 System.out.println("Error Found");
          	   	System.out.println("Error Code 4: Invalid Packet Size");
          	   	sendErrWrongPackSize();
            	 }
            	 else {
            		 System.out.println("Error Found");
               	   	System.out.println("Error Code 4: Invalid Op Code");
            		 sendErrWrongOPCode();
            	 }
          	   System.out.println("CLIENT: -----------------Deleting: " + outputFile);
          	 	outputFile.delete();
          	 	System.out.println("STARTING NEW TRANSFER");
          	 	restartApplication();
             }
             } else continue; // if not continue transfer without incrementing blockNumber
                
             
              System.out.println("==================================================================================");    
        	 
           
                
                   //check if its the last data packet
                   //break out of loop
                   
                     if(receivePacket.getLength() < 516 && !isErrorPacketCodeFour(receivePacket) && !isErrorPacketCodeFive(receivePacket)){
                         fop.close();
                         System.out.println("Client: Done Reading Starting new Transfer");
                         stop = true;
                         break;
                     }
            
                     blockNumber ++;									//Increment the block number each iteration  
         }
         
         restartApplication();     
    }

    
        
    /**
     * Main Running Argument
     * @param args
     */
    
    public static void main(String[] args) {
        Client C = new Client();
        C.ClientAlgorithm();

    }

}

   
