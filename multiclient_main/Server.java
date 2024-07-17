import java.io.*;
import java.text.*;
import java.util.*; 
import java.net.*; 
import java.util.concurrent.*; //Wasnt working without this...

public class Server{
    public static void main(String[] args) throws IOException{
        
        int clientcur_id = 0;

        HashMap<String,TreeMap<Integer,Integer>> buyorders = new HashMap<>();
        HashMap<String,TreeMap<Integer,Integer>> sellorders = new HashMap<>();
        ArrayList<DataOutputStream> client_out = new ArrayList<>();
        ConcurrentLinkedQueue<String> requests = new ConcurrentLinkedQueue<>();
        Thread engine = new requestengine(requests,buyorders,sellorders,client_out);
        engine.start();

        ServerSocket s1 = new ServerSocket(6666);
        while(true){
            Socket s = null;
            try{
                s = s1.accept();
                System.out.println("Connected to" + s);
                
                DataInputStream din = new DataInputStream(s.getInputStream());
                DataOutputStream dout = new DataOutputStream(s.getOutputStream());
                client_out.add(dout);

                Thread t = new ClientHandler(clientcur_id,s,din,dout,buyorders,sellorders,requests);
                clientcur_id++;
                t.start();
            }
            catch(Exception e){
                s.close();
                e.printStackTrace();
            }
        }
    }
}

class requestengine extends Thread{
    final ConcurrentLinkedQueue<String> requests;
    final HashMap<String,TreeMap<Integer,Integer>> buyorders;
    final HashMap<String,TreeMap<Integer,Integer>> sellorders;
    final ArrayList<DataOutputStream> client_out;
    public requestengine(ConcurrentLinkedQueue<String> requests,HashMap<String,TreeMap<Integer,Integer>> buyorders,HashMap<String,TreeMap<Integer,Integer>> sellorders,ArrayList<DataOutputStream> client_out){
        this.requests = requests;
        this.sellorders = sellorders;
        this.buyorders = buyorders;
        this.client_out = client_out;
    }

    @Override
    public void run(){
        while(true){
            if(!requests.isEmpty()){
                order_request(requests.poll());
            }
        }
    }

    public void order_request(String request){
        String parts[] = request.split("\\s+"); //note that .split(" ") wont split properly when multiple spaces present
        String operation = parts[0];
        String symbol = parts[1];   
        Integer price = Integer.parseInt(parts[2]);
        Integer client_id = Integer.parseInt(parts[3]);
        System.out.println(operation);
        
        //NOTE in java the == operator compares if the reference to both strings is to same place in memory
        if(operation.equalsIgnoreCase("bid")){
            if(sellorders.containsKey(symbol)){
                if(price >= sellorders.get(symbol).firstKey()){
                    System.out.println("Transaction");
                    Map.Entry<Integer,Integer> matched_entry = sellorders.get(symbol).pollFirstEntry();
                    Integer sellprice2 = matched_entry.getKey();
                    Integer client_id2 = matched_entry.getValue();
                    sellorders.get(symbol).remove(sellprice2);
                    try{
                        client_out.get(client_id2).writeUTF("Sold your symbol "+ symbol + " at "+ Integer.toString(sellprice2));
                        client_out.get(client_id).writeUTF("Bought symbol "+ symbol + " at "+ Integer.toString(price));
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    //idk about market but im assuming that the difference in price is to broker ?
                    return;
                }
            }
            //if the least sell order was more than the big price then add bid price to buyorders
            if(!buyorders.containsKey(symbol)){
                buyorders.put(symbol,new TreeMap<Integer,Integer>());
            }
            TreeMap<Integer,Integer> temptreemap = buyorders.get(symbol);
            temptreemap.put(price,client_id);
            System.out.printf("There are %d buy orders for symbol %s.\n",temptreemap.size(),symbol);
        }else if(operation.equalsIgnoreCase("ask")){
            if(buyorders.containsKey(symbol)){
                if(price <= buyorders.get(symbol).lastKey()){
                    System.out.println("Transaction");
                    Map.Entry<Integer,Integer> matched_entry = buyorders.get(symbol).pollLastEntry();
                    Integer buyprice2 = matched_entry.getKey();
                    Integer client_id2 = matched_entry.getValue();
                    buyorders.get(symbol).remove(buyprice2);
                    try{
                        client_out.get(client_id).writeUTF("Bought symbol " + symbol + " at " + Integer.toString(buyprice2));
                        client_out.get(client_id).writeUTF("Sold your symbol " + symbol + " at " + Integer.toString(price));
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    return;
                }
            }
            //
            if(!sellorders.containsKey(symbol)){
                sellorders.put(symbol,new TreeMap<Integer,Integer>());
            }
            TreeMap<Integer,Integer> temptreemap = sellorders.get(symbol);
            temptreemap.put(price,client_id);
            System.out.printf("There are %d sell orders for symbol %s.\n",temptreemap.size(),symbol);
        }
        else{
            try{
                client_out.get(client_id).writeUTF("Invalid Request.");
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }

}


class ClientHandler extends Thread  
{ 
    DateFormat fordate = new SimpleDateFormat("yyyy/MM/dd"); 
    DateFormat fortime = new SimpleDateFormat("hh:mm:ss"); 
    final DataInputStream dis; 
    final DataOutputStream dos; 
    final HashMap<String,TreeMap<Integer,Integer>> buyorders;
    final HashMap<String,TreeMap<Integer,Integer>> sellorders;
    final ConcurrentLinkedQueue<String> requests;
    final int id;
    final Socket s; 
      
  
    // Constructor 
    public ClientHandler(int ID,Socket s, DataInputStream dis, DataOutputStream dos,HashMap<String,TreeMap<Integer,Integer>> buyorders,HashMap<String,TreeMap<Integer,Integer>> sellorders,ConcurrentLinkedQueue<String> requests)  
    { 
        this.id = ID;
        this.s = s; 
        this.dis = dis; 
        this.dos = dos;
        this.buyorders = buyorders;
        this.sellorders = sellorders;
        this.requests = requests;
    } 
  
    @Override
    public void run()  
    { 
        String request; 
        String toreturn; 
        while (true)  
        { 
            try { 
  
                // Ask user what he wants 
                dos.writeUTF("Type your order."); 
                  
                // receive the answer from client 
                request = dis.readUTF(); 
                System.out.println(request);

                if(!request.equalsIgnoreCase("Exit")){
                    String parts[] = request.split("\\s+");
                    try{
                        if(parts.length < 3 || parts.length > 3){
                            dos.writeUTF("Invalid Request.");
                        }else if(!isInteger(parts[2])){
                            dos.writeUTF("Invalid Request.");
                        }else if(!parts[0].equalsIgnoreCase("ask") && !parts[0].equalsIgnoreCase("bid")){
                            dos.writeUTF("Invalid Request.");
                        }else{
                            request = request + " " + Integer.toString(id);
                            requests.add(request);
                        }
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                    //Design Consideration : When a new order request arrives we can just check if there is already an order which it can match with. 
                    //However this is not synchronous
                    //If a sell and buy order that can match arrive at same time they will both be added !!!
                    //Hence it is better to create a separate thread for order matching ? Although the time complexity of this would be worse and we would have to keep running this...
                    //Another consideration would be to queue requests and do the matching 100 ms after the order requests arrive to avoid errors due to non sync
                }
                else
                {  
                    System.out.println("Client " + this.s + " sends exit..."); 
                    System.out.println("Closing this connection."); 
                    this.s.close(); 
                    System.out.println("Connection closed"); 
                    break; 
                } 

            } catch (IOException e) { 
                e.printStackTrace(); 
            } 
        } 
          
        try
        { 
            // closing resources 
            this.dis.close(); 
            this.dos.close(); 
              
        }catch(IOException e){ 
            e.printStackTrace(); 
        } 
    }

    public static boolean isInteger(String s) {
        try { 
            Integer.parseInt(s); 
        } catch(NumberFormatException e) { 
            return false; 
        } catch(NullPointerException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }

     
} 