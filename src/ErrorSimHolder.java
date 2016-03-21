import java.net.*;
import java.io.IOException;
/**
 * Write a description of class ErrorSimHolder here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class ErrorSimHolder extends Thread
{   
         private DatagramSocket tempSocket;
         private DatagramPacket tempPacket;
         private int delay;
        public ErrorSimHolder(DatagramPacket dp, DatagramSocket ds, int delay ){
                tempSocket = ds;
                tempPacket = dp;
                this.delay = delay;
        }
        
        public void run(){
            ErrorSim sim = ErrorSim.host;
            byte[]buffer = new byte[516];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            System.out.println("My port:"+tempSocket.getLocalPort());
            //ErrorSimHolder a = new ErrorSimHolder();

            try {Thread.sleep(delay);}
            catch (Exception e) { System.out.println(e);}
            sim.displayPacket(tempPacket,"Thread");
            try{tempSocket.send(tempPacket);}
            catch (IOException e) { e.printStackTrace();System.exit(1);}
            try {
            tempSocket.receive(receivePacket);
             } catch (IOException e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
            receivePacket.setPort(sim.client_PORT);                    
                                try {
            sim.ClientSocket.send(receivePacket);
            } catch (IOException e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
            return;
        }
        
   
}
