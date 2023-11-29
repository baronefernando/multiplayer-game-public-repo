import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Server 
{
    // Server lists
    public ArrayList<InetAddress> clientAddresses = new ArrayList<>();
    public ArrayList<Integer> clientPorts = new ArrayList<>();
    public ArrayList<String[]> playerIDs = new ArrayList<>();

    // Time and date for logs
    private Date date;
    private String timeStamp;

    public static void main(String[] args) 
    {
        try 
        {
            System.out.println(
                    "  ____    _____   ____   __     __  _____   ____  \n" + //
                    " / ___|  | ____| |  _ \\  \\ \\   / / | ____| |  _ \\ \n" + //
                    " \\___ \\  |  _|   | |_) |  \\ \\ / /  |  _|   | |_) |\n" + //
                    "  ___) | | |___  |  _ <    \\ V /   | |___  |  _ < \n" + //
                    " |____/  |_____| |_| \\_\\    \\_/    |_____| |_| \\_\\\n");

            System.out.println("MULTIPLAYER SERVER v0.1");
            System.out.println("Created By: Fernando Barone\n");

            // Create server socket
            Server server = new Server();
            DatagramSocket serverSocket = new DatagramSocket(1234);

            System.out.println("Server starting on port " + serverSocket.getLocalPort() + "...");
            System.out.println("Testing MySQL connection...");
            server.testDB();
            System.out.println("Server has started!");
            System.out.println("\nWaiting for players...");

            // Buffer to store incoming data
            byte[] receiveBuffer = new byte[1024];

            // Run continuously to listen to multiple client connections
            while (true) 
            {
                server.listen(serverSocket,receiveBuffer);
            }
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    //Private methods
    private void listen(DatagramSocket serverSocket, byte[] receiveBuffer)
    {
        try 
        {
            // Receive client request
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            serverSocket.receive(receivePacket);

            InetAddress clientAddress = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();

            if (!clientAddresses.contains(clientAddress) || !clientPorts.contains(clientPort)) 
            {
                clientAddresses.add(clientAddress);
                clientPorts.add(clientPort);
                System.out.println("\nPlayer " + clientAddress + ":" + clientPort + " is connected!\n");
            }

            // Start a new thread to handle the client
            ClientHandler clientHandler = new ClientHandler(serverSocket, receivePacket, this);
            new Thread(clientHandler).start();
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    private void testDB()
    {
        DB_Manager db = new DB_Manager(this,"PNG");
        Thread dbThread = new Thread(db);
        dbThread.start();

        while(dbThread.isAlive())
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
    public void log(String message)
    {
        date = new java.util.Date(System.currentTimeMillis());
        timeStamp = new SimpleDateFormat("[HH:mm:ss]").format(date);
        System.out.println(timeStamp + " Log: " + message);
    }

    public String getTimeStamp()
    {
        date = new java.util.Date(System.currentTimeMillis());
        timeStamp = new SimpleDateFormat("HH:mm:ss").format(date);
        return timeStamp;
    }
}
