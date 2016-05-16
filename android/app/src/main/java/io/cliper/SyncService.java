package io.cliper;

/**
 * Created by Ljt on 2016/5/3.
 *
 * This is a service doing sync in the client.
 * Including sending token to server to start sync the token will be display in mainActivity,
 * receiving messages form server then transport the message to mainActivity. The messages are in Json format.
 * returning messages to server for receiving message successfully.
 * The recived Json message have to be decoded in this service。
 * This service transmit msg to mainactivity using a broadcast intent.
 */

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import android.os.Environment;

public class SyncService extends Service {
    private final String tokenFile = "/cliper.token";
    private final String sdcardPath = Environment.getExternalStorageDirectory().getPath();
    private final WebSocketConnection mConnection = new WebSocketConnection();
    final String syncid = "ws://104.207.144.233:4564/sync";
    final String msgfile = "/clipermsg.txt";
    static String synctoken = "";

    //This function get token form tokenfilr to globaltoken1.
    void gettoken (){
        try {
            Scanner in = new Scanner(new FileReader(sdcardPath + tokenFile));
            synctoken = in.nextLine();
        } catch (FileNotFoundException e) {
            ;
        ;
        }
    }

    private void writefile(String fileinput){
        FileWriter fw =null;
        try{
            File f = new File (sdcardPath + msgfile);
            fw = new FileWriter(f,true);
        }catch(IOException e){
            e.printStackTrace();
        }
        PrintWriter pw = new PrintWriter(fw);
        pw.println(fileinput);
        pw.flush();;
        try{
            fw.flush();
            pw.close();
            fw.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }



    @Override
    public int onStartCommand(Intent intent,int flag,int srartid){
        gettoken();


        /*
        * Establishing connection and get msg form server.
        * To see transport formation, click:https://github.com/Frefreak/fluffy-dollop/blob/master/server/spec.md
        * */
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mConnection.connect(syncid, new WebSocketHandler() {
                        @Override
                        public void onOpen() {
                            JSONObject js = new JSONObject();
                            try {
                                js.put("token", synctoken);
                            } catch (JSONException e) {
                                ;
                            }
                            mConnection.sendTextMessage(js.toString());
                        }


                        @Override
                        public void onTextMessage (String receive) {
                            try {
                                JSONObject a = new JSONObject(receive);
                                String msg = a.getString("msg");
                                int code = a.optInt("code");
                                String msgId = a.optString("msgid");
                                writefile(msg);
                                if (code == 0 && msgId == "") {
                                    throw new JSONException("server return bad json");
                                } else {
                                    // First kind, the connect is established by sending tokne to server.
                                    // Then brocast the "connection established" message.
                                    if (msgId == "" && code ==200)
                                    {
                                        Intent intent=new Intent();
                                        intent.putExtra("syncmsg", "Connection established." +"\n"+ "Token: "+ synctoken);
                                        intent.setAction("SyncService");
                                        sendBroadcast(intent);
                                    }
                                    // Second kind, the connect has been established and the server send msg automatically.
                                    // Then brocast the recived message and messageID.
                                    else {
                                        JSONObject resp = new JSONObject();
                                        resp.put("msgid", msgId);
                                        resp.put("status", "ok");
                                        mConnection.sendTextMessage(resp.toString());
                                        Intent intent=new Intent();
                                        intent.putExtra("syncmsg","Data: " +msg + "\n" + "msgID: "+ msgId);
                                        intent.setAction("SyncService");
                                        sendBroadcast(intent);
                                    }
                                }
                            } catch (JSONException e) {
                            }
                        }


                        @Override
                        public void onClose(int code, String reason) {
                        }
                    });
                } catch (WebSocketException e) {

                }

            }
        }).start();







        return START_STICKY;
    }




    @Override
    public void onCreate() {
        super.onCreate();
    }




    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v("CountService", "on destroy");
    }


}