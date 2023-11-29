using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System.Globalization;
using TMPro;
public class SceneManager : MonoBehaviour
{
    public GameObject playerPrefab;
    public GameObject messagePrefab;
    private TMP_Text incomingData;
    private ArrayList playersData = new ArrayList();
    private ArrayList chatHistory = new ArrayList();
    private string[] playerData;
    private NetworkManager nM;
    private bool updateMovement = false;
 
    // Start is called before the first frame update
    void Start()
    {
        incomingData = GameObject.Find("incoming").GetComponent<TMP_Text>();
        nM = GameObject.Find("NetworkManager").GetComponent<NetworkManager>();
    }
    public void OnButtonDisconnect()
    {
        nM.SendMessageToServer("DES:" + nM.GetSessionID());
    }
    public void OnButtonConnect()
    {
        nM.ConnectToServer();
        nM.SendMessageToServer("CON:" + nM.GetSessionID());
    }

    public void OnButtonSend()
    {
        Debug.Log(GameObject.Find("textInput").GetComponent<TMP_InputField>().text);
        nM.SendMessageToServer("MSG:" + nM.GetSessionID() + ":" + GameObject.Find("textInput").GetComponent<TMP_InputField>().text);
    }

    public void DataHandler(string data)
    {
        incomingData.text = data;
        if(data.Contains("MOV")) //Prepares movement data and add it to pool
        {
            string[] tmp = data.Split(":");
            playersData.Add(tmp);
            updateMovement = true;
        }
        else if (data.Contains("DES"))
        {
            string[] tmp = data.Split(":");
            Destroy(GameObject.Find(tmp[1]));

            for (int i = 0; i < playersData.Count; i++)
            {
                playerData = (string[]) playersData[i];
                if (playerData[1].Equals(tmp[1]))
                {
                    playersData.Remove(playerData);
                }
            }

            // Prepares for disconnection
            if (tmp[1].Equals(nM.GetSessionID().ToString()))
            {
                updateMovement = false;

                for (int i = 0; i < playersData.Count; i++)
                {
                    tmp = (string[]) playersData[i];
                    if (GameObject.Find(tmp[1]) != null)
                    {
                        Destroy(GameObject.Find(tmp[1]));
                    }
                }
                
                playersData.Clear();
                playerData = null;
                nM.DisconnectFromServer();
            }
        }
        else if (data.Contains("USR"))
        {
            string[] tmp = data.Split(":");

            //Removes any previous data from joining player
            for (int i = 0; i < playersData.Count; i++)
            {
                playerData = (string[]) playersData[i];
                if(playerData[1].Equals(tmp[1]))
                {
                   playersData.Remove(i);
                }
            }

            //If player object does not exist, create one
            if (GameObject.Find(tmp[1]) == null)
            {
                var clone = Instantiate(playerPrefab);
                clone.name = tmp[1];
                clone.transform.parent = GameObject.Find("Canvas").transform;
                clone.transform.localPosition = new Vector2(0, 0);
            }
        }
        else if(data.Contains("MSG"))
        {
            string[] tmp = data.Split(":");
            string gameObjName = tmp[0] + "-" + tmp[1] + "-" + tmp[3] +":"+ tmp[4] + ":" + tmp[5];;
            
            if (GameObject.Find(gameObjName) == null)
            {
                var clone = Instantiate(messagePrefab,GameObject.Find("Canvas").transform);
                clone.name = gameObjName;
                clone.transform.localPosition = new Vector2(0, 0);
                clone.transform.GetChild(0).GetComponent<TMP_Text>().text = tmp[1] + ":";
                clone.transform.GetChild(1).GetComponent<TMP_Text>().text = tmp[2];
                clone.transform.GetChild(2).GetComponent<TMP_Text>().text = tmp[3] + ":" + tmp[4];
                chatHistory.Add(gameObjName);
                StartCoroutine(chatMovement(gameObjName));
            }
        }
    }

    // Update is called once per frame
    void Update()
    {
        if (updateMovement)
        {
            for (int i = 0; i < playersData.Count; i++)
            {
                playerData = (string[]) playersData[i];

                if (GameObject.Find(playerData[1]) != null)
                {
                    var t = GameObject.Find(playerData[1]).transform;
                    Vector2 pos = t.localPosition;

                    Vector2 newPos = new Vector2(float.Parse(playerData[2], CultureInfo.InvariantCulture.NumberFormat), float.Parse(playerData[3], CultureInfo.InvariantCulture.NumberFormat));
                    t.localPosition = Vector2.MoveTowards(t.localPosition, newPos, Time.deltaTime * 10000.0f);

                    if (pos.Equals(newPos))
                    {
                        updateMovement = false;
                        Debug.Log("Target reached!");
                        playersData.RemoveAt(i);
                    }
                }
            }
        }

        //Keyboard keys
        if (Input.GetKey(KeyCode.W))
        {
            if (GameObject.Find(nM.GetSessionID().ToString()) != null)
            {
                GameObject p = GameObject.Find(nM.GetSessionID().ToString());
                Vector2 v = p.transform.localPosition;
                v.y = v.y + 10.0f;
                nM.SendMessageToServer("MOV:" + nM.GetSessionID() + ":" + v.x + ":" + v.y);

            }
        }
        else if (Input.GetKey(KeyCode.A))
        {
            if (GameObject.Find(nM.GetSessionID().ToString()) != null)
            {
                GameObject p = GameObject.Find(nM.GetSessionID().ToString());
                Vector2 v = p.transform.localPosition;
                v.x = v.x - 10.0f;
                nM.SendMessageToServer("MOV:" + nM.GetSessionID() + ":" + v.x + ":" + v.y);

            }
        }
        else if (Input.GetKey(KeyCode.S))
        {
            if (GameObject.Find(nM.GetSessionID().ToString()) != null)
            {
                GameObject p = GameObject.Find(nM.GetSessionID().ToString());
                Vector2 v = p.transform.localPosition;
                v.y = v.y - 10.0f;
                nM.SendMessageToServer("MOV:" + nM.GetSessionID() + ":" + v.x + ":" + v.y);

            }
        }
        else if (Input.GetKey(KeyCode.D))
        {
            if (GameObject.Find(nM.GetSessionID().ToString()) != null)
            {
                GameObject p = GameObject.Find(nM.GetSessionID().ToString());
                Vector2 v = p.transform.localPosition;
                v.x = v.x + 10.0f;
                nM.SendMessageToServer("MOV:" + nM.GetSessionID() + ":" + v.x + ":" + v.y);
            }
        }
    }

    IEnumerator chatMovement(string chat)
    {
        yield return new WaitForSeconds(2);
        if(chatHistory.Count > 0)
        {
            if (GameObject.Find(chat) != null)
            {
                var t = GameObject.Find(chat).transform;
                Vector2 pos = t.localPosition;
                Vector2 newPos = new Vector2(t.localPosition.x,(t.localPosition.y + 100.0f));
                t.localPosition = Vector2.MoveTowards(t.localPosition, newPos, Time.deltaTime * 10000.0f);
                if(pos.y < 600)
                {
                    StartCoroutine(chatMovement(chat));
                }
                else if(pos.y >= 600)
                {
                    Destroy(GameObject.Find(chat));
                }
            }
            
        }
        
    }
}
