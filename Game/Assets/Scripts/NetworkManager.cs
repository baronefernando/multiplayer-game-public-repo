using UnityEngine;
using System;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Collections.Generic;

public class NetworkManager : MonoBehaviour
{
    private UdpClient client;
    private bool isConnected = false;
    private bool hasNewMessage = false;
    private string messageToSend = string.Empty;
    private SceneManager sM;
    private Thread receiveThread;
    private int sessionID;

    private MainThreadTaskQueue taskQueue;

    public int GetSessionID()
    {
        return sessionID;
    }

    public int SetNewSession()
    {
        sessionID = UnityEngine.Random.Range(1, 10000);
        return sessionID;
    }

    public bool GetConnectionState()
    {
        return isConnected;
    }

    private void Start()
    {
        taskQueue = new MainThreadTaskQueue();
        sM = GameObject.Find("SceneManager").GetComponent<SceneManager>();
        sessionID = UnityEngine.Random.Range(1, 10000);
        ConnectToServer();
        SendMessageToServer("CON:" + sessionID.ToString());
    }

    public void ConnectToServer()
    {
        try
        {
            // Connect to server
            client = new UdpClient();
            client.Connect("localhost", 1234);
            Debug.Log("Connected to server.");

            isConnected = true;

            // Start a separate thread to receive messages from the server
            receiveThread = new Thread(ReceiveMessages);
            receiveThread.Start();

        }
        catch (Exception e)
        {
            Debug.LogError(e.Message);
        }
    }

    private void ReceiveMessages()
    {
        try
        {
            IPEndPoint serverEndpoint = new IPEndPoint(IPAddress.Any, 0);
            while (isConnected)
            {
                byte[] serverMessageBytes = client.Receive(ref serverEndpoint);
                string serverMessage = System.Text.Encoding.ASCII.GetString(serverMessageBytes);
                if (serverMessage != null)
                {
                    // Receive message from server
                    Debug.Log("Received from server: " + serverMessage);
                    taskQueue.Enqueue(() =>
                    {
                        sM.DataHandler(serverMessage);
                    });
                }
                else
                {
                    // Server disconnected
                    isConnected = false;
                    break;
                }
            }
        }
        catch (Exception e)
        {
            Debug.LogError(e.Message);
        }
    }

    public void SendMessageToServer(string message)
    {
        if (!isConnected)
        {
            Debug.LogError("Not connected to the server.");
            return;
        }

        messageToSend = message;
        hasNewMessage = true;
    }

    private void Update()
    {
        if (hasNewMessage && isConnected)
        {
            try
            {
                // Send message to server
                byte[] messageBytes = System.Text.Encoding.ASCII.GetBytes(messageToSend);
                client.Send(messageBytes, messageBytes.Length);
                Debug.Log("Sent to server: " + messageToSend);
                hasNewMessage = false;
            }
            catch (Exception e)
            {
                Debug.LogError(e.Message);
            }
        }

        // Process tasks in the main thread task queue
        taskQueue.ExecuteTasks();
    }

    private void OnDestroy()
    {
        SendMessageToServer("DES:" + GetSessionID());
        DisconnectFromServer();
    }

    public void DisconnectFromServer()
    {
        try
        {
            isConnected = false;
            taskQueue.taskQueue.Clear();

            // Close connections
            client.Close();

            // Interrupt the receive thread to exit gracefully
            if (receiveThread != null && receiveThread.IsAlive)
            {
                receiveThread.Interrupt();
                receiveThread.Join();
            }

            Debug.Log("Disconnected from server.");
        }
        catch (Exception e)
        {
            Debug.LogError(e.Message);
        }
    }
}

// Class to execute code on the main thread
public class MainThreadTaskQueue
{
    private readonly object lockObject = new object();
    public Queue<Action> taskQueue = new Queue<Action>();

    public void Enqueue(Action action)
    {
        lock (lockObject)
        {
            taskQueue.Enqueue(action);
        }
    }

    public void ExecuteTasks()
    {
        while (true)
        {
            Action task;
            lock (lockObject)
            {
                if (taskQueue.Count == 0)
                {
                    return;
                }

                task = taskQueue.Dequeue();
            }

            task.Invoke();
        }
    }
}
