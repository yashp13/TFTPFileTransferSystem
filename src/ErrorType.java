
/**
 * Write a description of class ErrorType here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class ErrorType
{
   String operation ;
   int op;
   int line;
   int delay;
   boolean ack;
   boolean data;
   public ErrorType(){
       this("");
    }
   public ErrorType(String operation){
       this(operation,0,0);
    }
   public ErrorType(String operation, int op, int line){
       this(operation,0,0,0,false,false);
    }
   public ErrorType(String operation,int op, int line, int delay, boolean ack, boolean data){
        this.operation = operation;
        this.op = op;
        this.line = line;
        this.delay = delay;
        this.ack = ack;
        this.data = data;
   }
   public void setOperation(String operation){
       this.operation = operation;
    }
   public void setLine(int line){
       this.line = line;
    }
   public void setOp(int op){
       this.op = op;
    }
   public String getOperation(){
       return operation;
    }
   public int getLine(){
       return line;
    }
   public int getOp(){
       return op;
    }
     public int getDelay(){
       return delay;
    }
   public boolean isAck(){
       return ack;
    }
   public boolean isData(){
       return data;
    }
}
