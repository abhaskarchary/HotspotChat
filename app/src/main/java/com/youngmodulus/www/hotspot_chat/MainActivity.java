package com.youngmodulus.www.hotspot_chat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private final int port_number = 8080;
    private ServerSocket serverSocket;
    private boolean isClient = false;

    private String connectedIP="";

    private String serverName = "";
    TextView join_server_text,server_chatlog,server_ip_client,client_chatlog;
    EditText client_username,server_send_text,server_name_text,client_send_text;
    Button client_join,server_create,server_send_button,client_send_button,client_disconnect_button;

    RelativeLayout join_layout,server_layout,create_server_layout,client_layout;

    ClientThread clientThread = null;

    List<ChatClient> userList;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        join_layout = (RelativeLayout)findViewById(R.id.join_layout);
        server_layout = (RelativeLayout)findViewById(R.id.server_layout);
        create_server_layout = (RelativeLayout)findViewById(R.id.create_server_layout);
        client_layout = (RelativeLayout)findViewById(R.id.client_layout);

        ArrayList<String> devices = new ArrayList<String>();
        try{
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            String a;
            while ((a = br.readLine()) != null) {
                String[] all = a.split(" ");
                devices.add(all[0]);
            }
            connectedIP = devices.get(1);
            String[] temp = connectedIP.split("\\.");

            if(temp[3].equals("1")) {
                isClient = true;
            }
        }catch(Exception e){
            Log.d("Error in reading arp", e.toString());
        }

        //set the layout Client/Server

        if(isClient){
            join_layout.setVisibility(View.VISIBLE);
            startClientSequence();
        }else{
            create_server_layout.setVisibility(View.VISIBLE);
            startServerSequence();
        }

    }

    private void startClientSequence(){
        //Join layout stuff
        join_server_text = (TextView)findViewById(R.id.join_server);
        join_server_text.setText("Connect server at: "+connectedIP);
        client_username = (EditText)findViewById(R.id.uname);
        client_join = (Button)findViewById(R.id.join_button);
        client_join.setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View v){
                        String textUserName = client_username.getText().toString();
                        if (textUserName.equals("")) {
                            Toast.makeText(MainActivity.this, "Enter User Name",Toast.LENGTH_LONG).show();
                            return;
                        }
                        server_ip_client.setText(connectedIP);
                        join_layout.setVisibility(View.GONE);
                        client_layout.setVisibility(View.VISIBLE);

                        clientThread = new ClientThread(textUserName, connectedIP, port_number);
                        clientThread.start();
                    }
                }
        );

        //client layout stuff
        server_ip_client = (TextView)findViewById(R.id.serverip);
        client_send_text = (EditText)findViewById(R.id.message_client);
        client_chatlog = (TextView)findViewById(R.id.chat_log);
        client_send_button = (Button)findViewById(R.id.send_button);
        client_send_button.setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View v){
                        if (client_send_text.getText().toString().equals("")) {
                            return;
                        }

                        if(clientThread==null){
                            return;
                        }
                        if (!client_send_text.getText().toString().equals(""))
                            clientThread.sendMsg(client_send_text.getText().toString());
                    }
                }
        );
        client_disconnect_button = (Button)findViewById(R.id.disconnect_client);
        client_disconnect_button.setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View v){
                        if(clientThread==null){
                            return;
                        }
                        clientThread.disconn();
                    }
                }
        );

    }

    private void startServerSequence(){
        //create Server stuff
        server_name_text = (EditText)findViewById(R.id.server_name);
        server_create = (Button)findViewById(R.id.create_server_button);
        server_create.setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View v){
                        if(server_name_text.getText().toString().equals("")){
                            Toast.makeText(MainActivity.this, "Enter User Name",Toast.LENGTH_LONG).show();
                            return;

                        }
                        serverName = server_name_text.getText().toString();
                        userList = new ArrayList<ChatClient>();

                        ServerThread chatServerThread = new ServerThread();
                        chatServerThread.start();

                        server_layout.setVisibility(View.VISIBLE);
                        create_server_layout.setVisibility(View.GONE);
                    }
                }
        );

        //server layout stuff
        server_chatlog = (TextView)findViewById(R.id.server_chat_log);
        server_send_button =(Button)findViewById(R.id.server_send_button);
        server_send_text = (EditText)findViewById(R.id.server_send_text);

        server_send_button.setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View v){
                        if(!server_send_text.getText().toString().equals("")) {
                            ServerSenderThread serverSenderThread = new ServerSenderThread(server_send_text.getText().toString());
                            serverSenderThread.start();
                        }

                    }
                }
        );

    }


    protected void onDestroy() {
        super.onDestroy();

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }







    //outer classes
    class ChatClient {
        String name;
        Socket socket;
        ConnectThread chatThread;

    }





    private class ClientThread extends Thread{
        String name,destip;
        int port;
        private String message = "";

        boolean disconnect;

        public ClientThread(String n, String ip,int p){
            name = n;
            destip = ip;
            port = p;
            disconnect = false;
        }

        public void run(){
            Socket sock = null;
            DataOutputStream dataOutputStream = null;
            DataInputStream dataInputStream = null;

            try {
                sock = new Socket(destip, port);
                dataOutputStream = new DataOutputStream(sock.getOutputStream());
                dataInputStream = new DataInputStream(sock.getInputStream());
                dataOutputStream.writeUTF(name);
                dataOutputStream.flush();

                while(!disconnect){
                    if(dataInputStream.available()>0){
                        final String msgLog = dataInputStream.readUTF();


                        MainActivity.this.runOnUiThread(new Runnable(){
                            public void run(){

                                client_chatlog.setText(client_chatlog.getText().toString()+msgLog);
                            }
                        });
                    }

                    if(!message.equals("")){
                        dataOutputStream.writeUTF(message);
                        dataOutputStream.flush();
                        message = "";
                    }
                }


            }catch (UnknownHostException e) {
                e.printStackTrace();
                final String eString = e.toString();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, eString, Toast.LENGTH_LONG).show();
                    }

                });
            } catch (IOException e) {
                e.printStackTrace();
                final String eString = e.toString();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, eString, Toast.LENGTH_LONG).show();
                    }

                });
            } finally {
                if (sock != null) {
                    try {
                        sock.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

            if (dataOutputStream != null) {
                try {
                    dataOutputStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            if (dataInputStream != null) {
                try {
                    dataInputStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            MainActivity.this.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    join_layout.setVisibility(View.VISIBLE);
                    client_layout.setVisibility(View.GONE);
                }

            });
        }
        private void sendMsg(String msg){
            message = msg;
        }

        private void disconn(){
            disconnect = true;
            client_chatlog.setText("CHAT LOG:\n");
        }

    }

    private class ServerThread extends Thread{

        @Override
        public void run() {
            Socket socket = null;

            try {
                serverSocket = new ServerSocket(port_number);
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        server_chatlog.setText("I'm waiting here: " + serverSocket.getLocalPort());
                    }
                });

                while (true) {
                    socket = serverSocket.accept();
                    ChatClient client = new ChatClient();
                    userList.add(client);
                    ConnectThread connectThread = new ConnectThread(client, socket);
                    connectThread.start();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }


        }


    }


    private class ConnectThread extends Thread{

        Socket socket;
        ChatClient connectClient;
        String msgToSend = "";
        String msgLog = "";

        ConnectThread(ChatClient client, Socket socket){
            connectClient = client;
            this.socket= socket;
            client.socket = socket;
            client.chatThread = this;
        }

        @Override
        public void run() {
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                String n = dataInputStream.readUTF();

                connectClient.name = n;

                msgLog = connectClient.name + " connected@" +
                        connectClient.socket.getInetAddress() +
                        ":" + connectClient.socket.getPort() + "\n";
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        String temp = server_chatlog.getText().toString();
                        server_chatlog.setText("");
                        server_chatlog.setText(temp+"\n"+msgLog);
                    }
                });

                dataOutputStream.writeUTF("Welcome " + n + "\n");
                dataOutputStream.flush();

                broadcastMsg(n + " join our chat.\n");

                while (true) {
                    if (dataInputStream.available() > 0) {
                        String newMsg = dataInputStream.readUTF();


                        msgLog = n + ": " + newMsg;
                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                String temp = server_chatlog.getText().toString();
                                server_chatlog.setText("");
                                server_chatlog.setText(temp+"\n"+msgLog);
                            }
                        });

                        broadcastMsg(n + ": " + newMsg);
                    }

                    if (!msgToSend.equals("")) {
                        dataOutputStream.writeUTF(msgToSend);
                        dataOutputStream.flush();
                        msgToSend = "";
                    }


                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                userList.remove(connectClient);
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                connectClient.name + " removed.", Toast.LENGTH_LONG).show();

                        msgLog = "-- " + connectClient.name + " left\n";
                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                String temp = server_chatlog.getText().toString();
                                server_chatlog.setText("");
                                server_chatlog.setText(temp+"\n"+msgLog);
                            }
                        });

                        broadcastMsg("-- " + connectClient.name + " left\n");
                    }
                });

            }



        }

        private void sendMsg(String msg){
            msgToSend = msg;
        }

        private void broadcastMsg(String msg){
            for(int i=0; i<userList.size(); i++){
                userList.get(i).chatThread.sendMsg(msg);
                //msgLog += "- send to " + userList.get(i).name + "\n";
            }


        }


    }

    private class ServerSenderThread extends Thread{

        String msgToSend = "";

        public ServerSenderThread(String msg){
            msgToSend = msg;
        }


        @Override
        public void run() {
            try {




                    if (!msgToSend.equals("")) {
                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                String temp = server_chatlog.getText().toString();
                                server_chatlog.setText("");
                                server_chatlog.setText(temp+"Server:"+msgToSend+"\n");
                            }
                        });
                        broadcastMsg(msgToSend);
                        msgToSend = "";
                    }


            } catch (Exception e) {
                e.printStackTrace();
            }



        }


        private void broadcastMsg(String msg){
            for(int i=0; i<userList.size(); i++){
                userList.get(i).chatThread.sendMsg("Server:"+msg+"\n");
                //msgLog += "- send to " + userList.get(i).name + "\n";
            }


        }


    }




}



