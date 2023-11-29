import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.*;

public class ClientHandler implements Runnable 
{
    private Server server;
    private DatagramSocket serverSocket;
    private DatagramPacket receivePacket;
   
    private String[] commandStrings;

    private DB_Manager db;
    private Thread dbThread;
    private ResultSet dbResult;

    public ClientHandler(DatagramSocket serverSocket, DatagramPacket receivePacket, Server server) 
    {
        this.serverSocket = serverSocket;
        this.receivePacket = receivePacket;
        this.server = server;
    }

    public synchronized void run() 
    {
        try 
        {
            byte[] receiveData = receivePacket.getData();
            String clientMessage = new String(receiveData, 0, receivePacket.getLength());
            commandStrings = clientMessage.split(":");
            server.log(clientMessage);

            if(clientMessage.contains("CON:"))
            {
                CON();
            }
            else if(clientMessage.contains("MOV"))
            {
                MOV();
            }
            else if(clientMessage.contains("DES"))
            {
                DES();
            }
            else if(clientMessage.contains("MSG"))
            {
                MSG();
            }
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    //Server commands
    private void CON()
    {
        try 
        {
            String[] tmp = {receivePacket.getAddress().toString(), Integer.toString(receivePacket.getPort()), commandStrings[1]};

            if(!server.playerIDs.contains(tmp))
            {
                server.playerIDs.add(tmp);
                updateDB(true);
            }

            //Log number of users currently connected
            server.log(server.playerIDs.size() + " users connected");

            // Propagate to all clients
            for (int i = 0; i < server.playerIDs.size(); i++) 
            {
                // Get client address and port
                InetAddress Address = server.clientAddresses.get(i);
                int Port = server.clientPorts.get(i);
                        
                // Send all connected players
                for(int j = 0; j < server.playerIDs.size(); j++)
                {
                    String[] tmpSearch = server.playerIDs.get(j);
                    String sendCommand = "USR:" + tmpSearch[2];
                    byte[] sendData = sendCommand.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, Address, Port);
                        
                    // Send the response to the client
                    serverSocket.send(sendPacket);
                    server.log("Sent \"" + sendCommand+"\"" + " to " + Address + ":" + Port);
                }
            }
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    private void MOV()
    {
        try 
        {
            String sendCommand = "MOV:" + commandStrings[1] + ":" + commandStrings[2] + ":" + commandStrings[3];

            // Propagate to all clients
            for (int i = 0; i < server.playerIDs.size(); i++) 
            {
                // Get client address and port
                InetAddress Address = server.clientAddresses.get(i);
                int Port = server.clientPorts.get(i);

                // Prepare response data
                byte[] sendData = sendCommand.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, Address, Port);

                // Send the response to the client
                serverSocket.send(sendPacket);
            }
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    private void DES()
    {
        try 
        {
            String sendCommand = "DES:" + commandStrings[1];
            int clientIndex = -1;
            int playerIndex = -1;

            for (int i = 0; i < server.playerIDs.size(); i++) 
            {
                // Get client address and port
                InetAddress Address = server.clientAddresses.get(i);
                int Port = server.clientPorts.get(i);
                String[] tmpSearch = server.playerIDs.get(i);

                byte[] sendData = sendCommand.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, Address, Port);
                        
                // Send the response to the client
                serverSocket.send(sendPacket);
                server.log("Sent \"" + sendCommand + "\" to " + Address + ":" + Port);

                if(tmpSearch[2].equals(commandStrings[1]))
                {
                    playerIndex = i;
                    clientIndex = i;
                }
            }  
    
            if (clientIndex != -1) 
            {
                server.clientAddresses.remove(clientIndex);
                server.clientPorts.remove(clientIndex);
                server.playerIDs.remove(playerIndex);
                server.log("Player " + commandStrings[1] + " disconnected!");
                updateDB(true);
            }
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
    
    private void MSG()
    {
        try 
        {
            String sendCommand = "MSG:" + commandStrings[1] + ":" +  commandStrings[2] + ":" + server.getTimeStamp();

            for (int i = 0; i < server.playerIDs.size(); i++) 
            {
                // Get client address and port
                InetAddress Address = server.clientAddresses.get(i);
                int Port = server.clientPorts.get(i);

                byte[] sendData = sendCommand.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, Address, Port);
                        
                // Send the response to the client
                serverSocket.send(sendPacket);
                server.log("Sent \"" + sendCommand + "\" to " + Address + ":" + Port);
            }  
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    //Private methods
    private void updateDB(boolean waitForCompletion)
    {
        //Adds to DB
        db = new DB_Manager(server, this, commandStrings);
        dbThread = new Thread(db);
        dbThread.start();

        //Wait till DB thread is done
        while(dbThread.isAlive() && waitForCompletion)
        {
            try 
            {
                Thread.sleep(1);
            } 
            catch (Exception e) 
            {
                e.printStackTrace();
            }
        }
    }
    //Public methods
    public void setResult(ResultSet dbResult)
    {
        this.dbResult = dbResult;
    }
}