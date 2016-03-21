import java.net.InetAddress;

public class printByteArray {

	Boolean enablePrint = true;
	
	public printByteArray(Boolean b) {
		enablePrint = b;
	}
	
	public void print(int sendORreceive, byte[] byteArray, int length, InetAddress Addr, int port) {

		if(!enablePrint) {
			return;
		}
		
    	// int sendORreceive = 1 is send. 0 is receive
    	
    	if(sendORreceive == 1){
            System.out.println("\nClient: Packet sending to: ");
            
          
        } else {
            System.out.println("\nClient: Packet received from: ");
            
        }
    	
    	System.out.println("Host: " + Addr);
        System.out.println("Port: " + port);
        System.out.println("Length: " + length);
    	
		try {
			
			//print the packet data in string which is framed with "" to show all bytes including \0
			String bufInString = new String(byteArray, 0, length, "UTF-8");
			System.out.format("Contents in String: \"%s\"\n", bufInString);
			
			//print the packet data in byte in hex format
			System.out.format("Contents in Byte: ");
			for(int i=0; i<length; i++) {
				System.out.format("0x%02x ", byteArray[i]);
				if( ((i+1) & 0xf) == 0)  {
					System.out.println();
				}
			}
			System.out.println();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
}
