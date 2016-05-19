package io.cliper;

import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.util.Scanner;
import android.widget.Toast;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private MyReceiver receiver=null;
    public String globaltoken = "";
    private final WebSocketConnection mConnection = new WebSocketConnection();

    /*This function receives and decodes Json messages from SyncService,
    The message have been decoded in SyncService.
     BUt I am trying to put the decoding part in SyncService for more comvience message transportation. 16/5/5.*/
    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            String syncmsg = bundle.getString("syncmsg");
            Toast.makeText(getApplication(),syncmsg , Toast.LENGTH_LONG).show();
            //tsync.setText(syncmsg);
            /*try {
            JSONObject c = new JSONObject(count);
            String msg = c.getString("msg");
            String msgId = c.optString("msgid");
        }catch (JSONException e){;}*/
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(io.cliper.R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(io.cliper.R.id.toolbar);
        setSupportActionBar(toolbar);
        final ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        cb.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                //Toast.makeText(getApplicationContext(), cb.getPrimaryClip().toString(), Toast.LENGTH_LONG).show();

                try {
                    mConnection.connect(Constant.postUrl, new WebSocketHandler() {

                        @Override
                        public void onOpen() {
                            JSONObject js = new JSONObject();
                            try {
                                js.put("token", globaltoken);
                                js.put("data", cb.getPrimaryClip().getItemAt(0).getText());
                                //Toast.makeText(getApplicationContext(), js.get("data").toString(), Toast.LENGTH_LONG).show();

                            } catch (JSONException e) {
                                Toast.makeText(getApplicationContext(), "error in posting clipboard contents (pre)", Toast.LENGTH_LONG).show();
                            }
                            mConnection.sendTextMessage(js.toString());
                        }

                        @Override
                        public void onTextMessage(String payload) {
                            try {
                                JSONObject a = new JSONObject(payload);
                                String msg = a.getString("msg");
                                int code = a.getInt("code");
                                Toast.makeText(getApplication(), "msg sent." +msg + code , Toast.LENGTH_LONG).show();
                            } catch (JSONException e) {
                                Toast.makeText(getApplication(), "Errors in sending msg." , Toast.LENGTH_LONG).show();
                            }
                        }


                        @Override
                        public void onClose(int code, String reason) {
                            ;//Toast.makeText(getApplication(), "Connection lost.", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (WebSocketException e) {
                    Toast.makeText(getApplication(), "Errors in buliding connection.", Toast.LENGTH_LONG).show();
                }

            }
        });

        //Registing the broadcast receiver.
        receiver=new MyReceiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction("SyncService");
        MainActivity.this.registerReceiver(receiver, filter);


        //Checking if there is a file saving token in SD card. If there is no file or the token is empty than print "need login"
        //If the token is not empty than start syncService.
        try {
            Scanner in = new Scanner(new FileReader(Constant.tokenFileAbsPath));
            globaltoken = in.nextLine();
        } catch (FileNotFoundException e) {
            Toast.makeText(getApplication(), "NEED LOGIN!", Toast.LENGTH_LONG).show();
        }

        if (globaltoken  == null || globaltoken.equals("")) {
            Toast.makeText(getApplication(), "NEED LOGIN!", Toast.LENGTH_LONG).show();
            this.stopService(new Intent(this, PingService.class));
            this.stopService(new Intent(this, SyncService.class));
        } else {
            this.startService(new Intent(this, SyncService.class));
            this.startService(new Intent(this, PingService.class));
            Toast.makeText(getApplication(), "Sync start. Ping start.", Toast.LENGTH_LONG).show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(io.cliper.R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, io.cliper.R.string.navigation_drawer_open, io.cliper.R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(io.cliper.R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(io.cliper.R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(io.cliper.R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == io.cliper.R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();


        /*
        * All following codes finishing logout function.
        * when logout successed, the token file will be emptied.
        * when logout failed, throw a failed msg.
        *
        * */
        if (id == R.id.nav_REGISTER) {
             Intent intent = new Intent (this,RegisterActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_LOGOUT) {
            try {
                mConnection.connect(Constant.logoutUrl, new WebSocketHandler() {
                    @Override
                    public void onOpen() {
                        JSONObject jslogout = new JSONObject();
                        try {
                            jslogout.put("token", globaltoken);
                        } catch (JSONException e) {
                            ;
                        }
                        mConnection.sendTextMessage(jslogout.toString());
                    }

                    @Override
                    public void onTextMessage(String payload) {

                        try {
                            JSONObject a = new JSONObject(payload);
                            String msg = a.getString("msg");
                            int code = a.getInt("code");
                            if(code == 200){
                            Toast.makeText(getApplication(), "Logout successed" +" " +msg +" " + code, Toast.LENGTH_LONG).show();
                            try{
                            PrintWriter writer = new PrintWriter(Constant.tokenFileAbsPath);
                            writer.println("");
                            writer.close();}catch (FileNotFoundException e){;}}
                            else if(code == 422){
                                Toast.makeText(getApplication(), "You have not login yet.", Toast.LENGTH_LONG).show();
                            }
                            else Toast.makeText(getApplication(), "Logout failed!", Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Toast.makeText(getApplication(), "Logout failed!", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onClose(int code, String reason) {
                        //Log.d(TAG, "Connection lost.");
                    }
                });
            } catch (WebSocketException e) {
                //Log.d(TAG, e.toString());
            }
        } else if (id == R.id.nav_LOGIN) {
            Intent intent = new Intent(this,LoginActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_Send) {
            Intent intent = new Intent(this, ChatActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_Receive) {
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
