import java.io.IOException;
import java.net.*;
import java.util.*;
import java.io.IOException;
/**
 * @author Yash Patel, Yan Liao
 * 
 * This is the error simulator but since it is the first iteration, it does not check for errors yet.
 * Therefore, for now, it simply acts as an intermediate host.
 */
public class ErrorSim extends Thread{
    private final int error_PORT = 61; 
    protected DatagramPacket sendPacket, receivePacket;
    protected DatagramSocket receiveSocket, ClientSocket, ServerSocket, ErrorSocket, testSocket;
    private boolean initial_packet = true;
    private boolean invaild_tid = false;
    private boolean packet_lost = false;
    private boolean read = false;
    private boolean write = false;
    private boolean is_ack = false;
    private boolean is_data = false;
    private InetAddress server_ad;
    private int server_PORT;
    private InetAddress client_ad;
    protected int client_PORT;
    protected static ErrorSim host;
    private List<ErrorType> error_list;
    private int error_packet_number;
    private int error_code_number;
    private int total_error;
    private boolean first_time_error = true;
    private int delay;
    //private int clientTID, serverTID;
    
    public ErrorSim(){
        error_list = new ArrayList<ErrorType>();
         try {
            receiveSocket = new DatagramSocket(error_PORT);
            ClientSocket = new DatagramSocket();
            ServerSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    protected void displayPacket(DatagramPacket p, String s){
        byte buffer[] = new byte[516];
        byte[] data = new byte[p.getLength()];
        System.arraycopy(p.getData(), 0, data, 0, data.length);
        if(s.equals("receive")) System.out.println("Error Simulator: Packet received:");
        else System.out.println("Error Simulator: Packet sent: ");
        System.out.println("From host: " + p.getAddress());
        System.out.println("Host port: " + p.getPort());
        System.out.println("Length: " + p.getLength());
        System.out.print("Containing (bytes): " );
        System.out.println(Arrays.toString(data));
        System.out.print("Containing (String): ");
        String received = new String(p.getData(),0,p.getLength());   
        System.out.println(received + "\n");
    }
    public void run(){
        /*  Receive the packet from Client  */
        boolean packet_ready = false;
        boolean client_error_applied = false; 
        boolean server_error_applied = false; 
        boolean read_last_packet = false;
        boolean write_last_packet = false;
        boolean error_packet = false;
        boolean finished = false;
        boolean packet_lost_lock = false;
        is_ack = false;
        is_data = false;
        do{
            byte buffer[] = new byte[516];
            byte[] data;
            int error_code = 0;
            invaild_tid = false;
            receivePacket = new DatagramPacket(buffer, buffer.length);
           if((write_last_packet || read_last_packet)){if(!client_error_applied)System.out.println("CLIENT ERROR APPLIED FAILED"); else if(!server_error_applied)System.out.println("SERVER ERROR APPLIED FAILED");}
           if(error_packet){initial_packet = true;error_list.remove(0);}
           if((write_last_packet || error_packet) &&error_list.isEmpty()){System.out.println("------------------------Error List is Over Please ReEnter------------------------");break;}
           System.out.println("Error Simulator: Waiting for the client...");
           //!packet_lost||write
           if(!packet_lost){
               try{
                   if(initial_packet){
                       receiveSocket.receive(receivePacket);
                       ErrorType e = error_list.get(0);
                       error_code_number = e.getOp();
                       error_packet_number = e.getLine();
                       System.out.println(" -------------------Error Applied-------------------------");
                       System.out.printf("|code = %-5d | op = %-20s | pack = %-5d |\n",e.getOp(), e.getOperation(),e.getLine());
                       System.out.println(" ---------------------------------------------------------");
                       is_ack = e.isAck(); 
                       is_data = e.isData();
                       delay = e.getDelay();
                       first_time_error = true;
                    }
                   else{
                       ClientSocket.receive(receivePacket);
                    }
                   client_ad = receivePacket.getAddress(); 
                   client_PORT = receivePacket.getPort();
                }
                catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                }
                data = new byte[receivePacket.getLength()];
                System.arraycopy(receivePacket.getData(), 0, data, 0, data.length);
                initial_packet = isInitial(data);
                //getByName("198.")
                if(initial_packet){try{server_ad = InetAddress.getLocalHost();}catch(UnknownHostException e){};server_PORT = 69;read_last_packet = false; write_last_packet = false; error_packet = false;}
                displayPacket(receivePacket, "receive");
                if((((write && is_data)||( read && is_ack)) && getPacketNumber(receivePacket.getData()) == error_packet_number && first_time_error) || (!is_data && !is_ack && !client_error_applied && first_time_error)){
                    System.out.println("\n--------Packet found. TO Server---------");
                    client_error_applied = true;
                    packet_lost = false;
                    first_time_error = false; 
                    error_code = error_code_number;
                }
                else {error_code = 0;client_error_applied = false;}
                sendPacket = errorCode(error_code,data, receivePacket.getLength(), server_ad, server_PORT, ServerSocket);
                write_last_packet = isLast(data,write);
                if(write_last_packet){
                    initial_packet = true;error_list.remove(0);
                }
               displayPacket(sendPacket, "send");
               packet_lost_lock=false;
          }

          if(packet_lost&&server_error_applied && is_data)packet_ready = true;//For reading request, Data lost
          if(server_error_applied && packet_ready && read){packet_lost = false;packet_ready = false;packet_lost_lock=true;}//For reading request, Data lost
          
          if(packet_lost&&client_error_applied && is_ack)packet_ready = true; //For reading request, ACK lost
          if(client_error_applied && packet_ready && read){packet_lost = false;packet_ready = false;packet_lost_lock=true;}//For reading request, ACK lost
          
          if(!packet_lost){
               if(!invaild_tid){
                   if(!packet_lost_lock){
                        try {
                            //ServerSocket.close();
                            //ServerSocket = new DatagramSocket(clientTID);
                            ServerSocket.send(sendPacket);
                        } 
                        catch (IOException e) {
                             e.printStackTrace();
                             System.exit(1);
                        }
                        if(!write_last_packet)initial_packet = false;
                        System.out.println("Error Simulator: packet sent");
                        System.out.println("-----------------------------------------------------------------------\n");
                    }
                   packet_lost_lock=false;
                        if(read_last_packet){initial_packet = true;error_list.remove(0);}
                        else{
                                    buffer = new byte[516];
                                    receivePacket = new DatagramPacket(buffer, buffer.length);
                                    System.out.println("Error Simulator: Waiting for server...");
                                    try {
                                        ServerSocket.receive(receivePacket);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        System.exit(1);
                                    }
                        }
                   
               }
               else{     
                        try {
                            ErrorSocket = new DatagramSocket();
                        } catch (SocketException e) {
                            e.printStackTrace();
                            System.exit(1);
                        }
                        try {
                                    ErrorSocket.send(sendPacket);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    System.exit(1);
                        }
                        buffer = new byte[516];
                        receivePacket = new DatagramPacket(buffer, buffer.length);
                        System.out.println("Error Simulator: Waiting for server...");
                        try {
                                    ErrorSocket.receive(receivePacket);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    System.exit(1);
                        }
                        invaild_tid = false;
                } 
               if(read_last_packet &&error_list.isEmpty()){System.out.println("------------------------Error List is Over Please ReEnter------------------------");break;}
               if(read_last_packet)break;
               server_ad = receivePacket.getAddress();
               server_PORT =  receivePacket.getPort();
               data = new byte[receivePacket.getLength()];
               System.arraycopy(receivePacket.getData(), 0, data, 0, data.length);
               error_packet = isErrorPacket(data);
               read_last_packet = isLast(data,read);
               displayPacket(receivePacket, "receive");
               if(((read && is_data)||( write && is_ack)) && (getPacketNumber(receivePacket.getData()) == error_packet_number) && (first_time_error)) {
                   System.out.println("\n--------Packet found. TO Client---------");
                   error_code = error_code_number;
                   server_error_applied = true;
                   packet_lost = false;
                   first_time_error = false;
               }
               else {error_code = 0;server_error_applied = false;}
               sendPacket = errorCode(error_code,data, receivePacket.getLength(), client_ad, client_PORT,ClientSocket);
               /*  Sending the packet to Client    */
               if(!packet_lost){     
                    displayPacket(sendPacket, "send");
                        try {
                         //ClientSocket = new DatagramSocket(serverTID);
                         if(invaild_tid){ErrorSocket = new DatagramSocket(); ErrorSocket.send(sendPacket);invaild_tid = false;}
                         else{ClientSocket.send(sendPacket);}
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.exit(1);
                        }
                    System.out.println("Error Simulator: packet sent");
                    System.out.println("_______________________________________________________________________\n");
                }
           }
          if(packet_lost && is_ack && server_error_applied){packet_ready = true;}//For writing request, ACK lost
          if(server_error_applied && packet_ready && write){packet_lost = false;packet_ready = false;packet_lost_lock=true;}//For writing request, ACK lost
          
          if(packet_lost&&client_error_applied && is_data ){packet_ready = true;}////For writing request, Data lost
          if(client_error_applied && packet_ready && write){packet_lost = false;packet_ready = false;packet_lost_lock=true;}////For writing request, ACK lost
          
          if(packet_lost&&client_error_applied && !is_data && !is_ack)packet_lost = false;// for initial packet Lost
        }while(!write_last_packet|| error_packet);//!write_last_packet || !read_last_packet || !error_packet
    }
    private void disconnect(DatagramSocket ds){
        ds.close();
    }
    private void selectErrorCode(){
        int op = 0;
        int line = 0;
        boolean ack,data;
        String operation = "";
        //System.out.println("Select Error: [0.Do Nothing, 1.Invaild Operation(Change First 2 byte), 2.Wrong Block Number(Change 3r and 4th byte)\n3.Remove Zero(Remove Last Byte), 4.Change Mode(Change ACSII or Octect), 5.Missing File Name, 6.Invail Packet Size(Change Size to 1024)\n7.Invaild TID, 8.Duplicate Packet, 9.Packet Lost 10.Packet Delay ):] ");
        System.out.println("Select Error:");
        System.out.println("0.Do Nothing,");
        System.out.println("1.Invalid Operation(Changes First 2 byte)");
        System.out.println("2.Wrong Block Number(Changes 3rd and 4th byte)");
        System.out.println("3.Remove Zero(Removes Last Byte)");
        System.out.println("4.Change Mode(Change ACSII or Octect)");
        System.out.println("5.Missing File Name");
        System.out.println("6.Invalid Packet Size(Changes Size to 1024)");
        System.out.println("7.Invalid TID");
        System.out.println("8.Duplicate Packet");
        System.out.println("9.Packet Lost");
        System.out.println("10.Packet Delay (timeout is set at 3s)");
        while(true){
            int count = 0;
            int delay = 0;
            boolean for_initial_only = false;
            boolean for_data_and_ack = false;
            boolean for_delay = false;
            System.out.print("Enter 888 to remove previous error or 999 if you're done: ");
            Scanner scanner = new Scanner(System.in);
            op = scanner.nextInt();
            if((op>=0 && op <=10)){
                if (op == 0) {System.out.println("Do Nothing");for_initial_only = true;operation = "Do Nothing";}
                else if(op == 1 ){/*System.out.println("Error Code 4 has selected(Illegal TFTP operation.)");*/operation = "Invaild Operation";for_data_and_ack = true;}
                else if(op == 2 ){/*System.out.println("Error Code 4 has selected(Illegal TFTP operation.)");*/operation = "Wrong Block Number";for_data_and_ack = true;}
                else if(op == 3 ){/*System.out.println("Error Code 4 has selected(Illegal TFTP operation.)");*/for_initial_only = true;operation = "Remove Zero";}
                else if(op == 4 ){/*System.out.println("Error Code 4 has selected(Illegal TFTP operation.)");*/for_initial_only = true;operation = "Change Mode";}
                else if(op == 5 ){/*System.out.println("Error Code 4 has selected(Illegal TFTP operation.)");*/for_initial_only = true;operation = "Missing File Name";}
                else if(op == 6 ){/*System.out.println("Error Code 4 has selected(Illegal TFTP operation.)");*/operation = "Invalid Packet Size";for_data_and_ack = true;}
                else if(op == 7 ){/*System.out.println("Error Code 5 has selected(Duplicate Packet.)");*/operation = "Invaild TID";for_data_and_ack = true;}
                else if(op == 8 ){/*System.out.println("Error Code 5 has selected(Duplicate Packet.)");*/operation = "Duplicate Packet";for_data_and_ack = true;for_delay=true;}
                else if(op == 9 ){/*System.out.println("Error Code 5 has selected(Packet Lost.)");*/operation = "Packet Lost";for_data_and_ack = true;}
                else if(op == 10 ){/*System.out.println("Error Code 5 has selected(Packet Delay.)");*/operation = "Packet Delay";for_data_and_ack = true;for_delay=true;}
                if(for_data_and_ack){
                    String s;
                    scanner.nextLine();
                    while(true){   
                        System.out.print("For Ack or Data or Request: ");
                        s = scanner.nextLine();
                        if(s.equalsIgnoreCase("ack")){ack = true; data = false; break;}
                        else if(s.equalsIgnoreCase("data")){ack = false; data = true; break;}
                        else if(s.equalsIgnoreCase("request")){ack = false; data = false; break;}
                    }
                } 
                else{ack = false; data = false;}
                if(for_delay){
                        System.out.print("Input Delay(ms): ");
                        delay = scanner.nextInt();
                }else{delay = 0;}
                if(!for_initial_only&& (ack || data)){
                     System.out.print("Enter the packet Please: ");
                     line = scanner.nextInt();
                } else{line = 0;}
                error_list.add(new ErrorType(operation,op ,line,delay,ack,data));
            }
            else if(op == 888){
                error_list.remove(error_list.size()-1);
            }
            else if(op == 999){
                //error_list.add(new ErrorType("Finished",99 ,0));
                break;
            }
            else{
                 System.out.println("Invaild Number Entered, Please Enter Again: ");
            }
            printList();
        }
      
    }
    private void printList(){
                int count = 0;
                String type = "";
                System.out.println(" -------------------------------------------------------------");
                System.out.printf("|%-5s |%-26s | %-12s | %-10s|\n","ID","Operation","Pack","Target");
                for(ErrorType e: error_list){
                    count++;
                    if(e.isAck())type = "ACK";
                    else if(e.isData())type = "DATA";
                    else type = "Neither";
                    System.out.println(" ------------------------------------------------------------");
                    System.out.printf("|%-5d | op = %-20s | pack = %-5d | %-10s|\n",count, e.getOperation(),e.getLine(),type);
                }
                total_error = count;
                System.out.println(" ------------------------------------------------------------");
    }
    public DatagramPacket errorCode(int error_code, byte[] data, int length, InetAddress address, int PORT, DatagramSocket src){
         Random rand = new Random();
         byte[] blk = new byte[2];
         byte[] blk1;
        if (error_code == 0){}
        else if(error_code == 1){
            data[0] = 0;
            data[1] = 9; 
        }
        else if(error_code == 2){
            if(!initial_packet){
                blk[0] =  data[2];
                blk[1] =  data[3];
                blk1 = blk;
                do{
                 rand.nextBytes(blk);
                }while(!Arrays.equals(blk,blk1));
                data[2] = blk[0];
                data[3] = blk[1];
            }
        }
        else if(error_code == 3){byte[] barray = Arrays.copyOf(data, data.length-1); data = barray;}
        else if(error_code == 4){
            byte[] barray = Arrays.copyOf(data, data.length-1);
            barray[barray.length-1] = 0;
            data = barray;
        
        }
        else if(error_code == 5){
            int counter = 1;
            int zero_enable = 0;
            byte[] error_data =new byte[data.length];
            error_data[0] = data[0];
            error_data[1] = data[1];
            for(int  i = 2; i < data.length;i++){
               if(zero_enable <=0){ if(data[i]==0)zero_enable++;}
               else{
                   counter++;
                   error_data[counter] = data[i-1];
                }
            }
            data = dataShrek(error_data);
        }
        else if(error_code == 6){
            byte[] error_data = Arrays.copyOf(data,data.length+516);
            data = error_data;
        }
        else if(error_code == 7){invaild_tid = true;}
        else if(error_code == 8){new ErrorSimHolder(new DatagramPacket(data,data.length,address,PORT),src,delay,read,write,is_ack,is_data).start();}
        else if(error_code == 9){packet_lost = true;}
        else if(error_code == 10){
            try {
                    sleep(delay);
                } catch (InterruptedException e) {
          
                    e.printStackTrace();
                }
            }
        return (new DatagramPacket(data,data.length,address,PORT));
    }
    
    private boolean isInitial(byte[] data){
        if(data[0]==0&&data[1]==1) {read = true; write = false;}
        else if(data[0]==0&&data[1]==2) {read = false; write = true;}
        return (data[0]==0 && (data[1]==2 || data[1]==1));
    }
    private boolean isErrorPacket(byte[] data){
        return (data[0]==0 && data[1]==5);
    }
    private boolean isLast(byte[] data, boolean type){
        return (!initial_packet && type && data.length<516);
    }
    private byte[] dataShrek(byte[] data){
        int newLength=0;
        int zero_counter=0;
        for(int i = 0; i < data.length; i++){
         if(data[i]==0 && data[i+1]==0){break;}
         newLength = i;
        }
        return Arrays.copyOf(data,newLength+2);

    }
    public int getPacketNumber(byte[] data){
        return ((data[2] << 8) & 0xff00) + (data[3] & 0xff);
    }
    public static void main(String[] args){
        host = new ErrorSim();
        //host.selectErrorCode();
        while(true){
            if(host.error_list.isEmpty())host.selectErrorCode();
            host.run();
        }
    }
}
