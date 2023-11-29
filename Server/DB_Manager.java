import java.sql.*;

public class DB_Manager implements Runnable
{
    String HOSTNAME = "localhost";
    String PORT = "3306";
    String USER = "root";
    String PWD = "";
    String DB_NAME = "";
    String url = "jdbc:mysql://" + HOSTNAME + ":" +  PORT + "/" + DB_NAME + "?autoreconnect=true";

    Server server;
    ClientHandler clientHandler;
    String[] commandString;

    // Constructor for simple commands e.g. PNG
    public DB_Manager(Server server, String command)
    {
        this.server = server;
        commandString = new String[] {command};
    }

    // Constructor for complex commands e.g. CON, DES etc
    public DB_Manager(Server server, ClientHandler clientHandler, String[] commandString)
    {
        this.server = server;
        this.clientHandler = clientHandler;
        this.commandString = commandString;
    }

    public synchronized void run()
    {
        if(commandString[0].equals("CON"))
        {
            addUser(commandString[1]);
        }
        else if(commandString[0].equals("DES"))
        {
            removeUser(commandString[1]);
        }
        else if(commandString[0].equals("PNG"))
        {
            pingDB();
        }
    }

    private synchronized void pingDB()
    {
        try 
        {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, USER, PWD);
            if(conn.isValid(0))
            {
                System.out.println("MySQL connection works...");
            }
            conn.close();
        } 
        catch (ClassNotFoundException | SQLException e) 
        {
            System.out.println("Error communicating with MySQL server...");
            //e.printStackTrace(); //Debug
            System.out.println("Server initialisation aborted!\n");
            System.exit(0);
        }
    }

    private synchronized void addUser(String id)
    {
        try 
        {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, USER, PWD);
            Statement stmt = conn.createStatement();
            
            stmt.executeUpdate("INSERT INTO tbl_users VALUES("+ id + ")");

            stmt.close();
            conn.close();
        } 
        catch (ClassNotFoundException | SQLException e) 
        {
            if(e.getMessage() != null)
            {
                server.log(e.getMessage());
            }
            else
            {
                e.printStackTrace();
            }
        }
    }

    private synchronized void removeUser(String id)
    {
        try 
        {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, USER, PWD);
            Statement stmt = conn.createStatement();
            
            stmt.executeUpdate("DELETE FROM tbl_users WHERE user_id =" + id);

            stmt.close();
            conn.close();
        } 
        catch (ClassNotFoundException | SQLException e) 
        {
            if(e.getMessage() != null)
            {
                server.log(e.getMessage());
            }
            else
            {
                e.printStackTrace();
            }
        }
    }
}
