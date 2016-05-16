package io.cliper;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;

/**
 * Created by Hpy on 2016/5/11.
 * Doing Ping to server every 4 minutes and return disconnect msg when ping failed.
 * This service transmit msg to mainactivity using a broadcast intent.
 */
public class PingService extends Service {

    private final String tokenFile = "/cliper.token";
    private final String sdcardPath = Environment.getExternalStorageDirectory().getPath();
    private final WebSocketConnection mConnection = new WebSocketConnection();
    final String Pingid = "ws://104.207.144.233:4564/ping";
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

    /*
        * Establishing connection and get msg form server.
        * To see transport formation, click:https://github.com/Frefreak/fluffy-dollop/blob/master/server/spec.md
    * */
    @Override
    public int onStartCommand(Intent intent,int flag,int srartid){
        gettoken();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!synctoken.equals(" ")) {
                    try {
                        mConnection.connect(Pingid, new WebSocketHandler() {
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
                            public void onTextMessage(String receive) {
                                try {
                                    JSONObject a = new JSONObject(receive);
                                    String msg = a.getString("msg");
                                    int code = a.optInt("code");
                                    if (code == 200) {
                                        ;
                                    } else {
                                        Toast.makeText(getApplication(), "Errors in pinging servicer" + code +msg, Toast.LENGTH_LONG).show();
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

                    /*
                    Ping to server every four minutes.
                    * */
                    try{
                        Thread.sleep(240000);
                    }catch (InterruptedException e) {;}
                }
                Toast.makeText(getApplication(), "Ping failed. Need login.", Toast.LENGTH_LONG).show();
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
