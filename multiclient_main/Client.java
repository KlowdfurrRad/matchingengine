import java.io.*; 
import java.net.*; 
import java.util.Scanner; 

public class Client{
    public static void main(String [] args) throws IOException{
        try{
            InetAddress ip = InetAddress.getByName("localhost"); 
            Socket s = new Socket(ip, 6666); 

            DataInputStream din = new DataInputStream(s.getInputStream());
            DataOutputStream dout = new DataOutputStream(s.getOutputStream());
            Scanner scn = new Scanner(System.in);

            InputReceiver input_thread = new InputReceiver(din);
            input_thread.start();

            while(true){
                String tosend = scn.nextLine(); 
                dout.writeUTF(tosend);
                if(tosend.equals("Exit")) 
                { 
                    System.out.println("Closing this connection : " + s); 
                    s.close(); 
                    System.out.println("Connection closed"); 
                    break; 
                }
            }
            scn.close(); 
            din.close(); 
            dout.close(); 
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}

//below is purely my addition yes.
class InputReceiver extends Thread{
    final DataInputStream din;
    InputReceiver(DataInputStream din){
        this.din = din;
    }

    @Override
    public void run(){
        try{
            while(true){
                String received = din.readUTF();
                System.out.println(received);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}