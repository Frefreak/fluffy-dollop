package io.cliper;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;


public class ChatActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private EditText messageET;
    private ListView messagesContainer;
    private Button sendBtn;
    private ChatAdapter adapter;
    private ArrayList<ChatMessage> chatHistory;

    public CliperDbOpenHelper dbHelper;

    private MyReceiver receiver=null;
    public String globaltoken = "";
    private final WebSocketConnection mConnection = new WebSocketConnection();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(io.cliper.R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(io.cliper.R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        if (drawer != null)
            drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(io.cliper.R.id.nav_view);
        if (navigationView != null)
            navigationView.setNavigationItemSelectedListener(this);

        /* Beginning in Android 6.0 (API level 23), users grant permissions to apps while the app is running,
            not when they install the app. Our app uses WRITE_EXTERNAL_STORAGE and INTERNET. The following checks
            if WRITE_EXTERNAL_STORAGE is given and prompt for enabling if not.
         */
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constant.REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            initialize();
            initControls();
        }
        /* end of permission request */

    }

    public void initialize() {
        dbHelper = new CliperDbOpenHelper(getApplicationContext());
        initializeToken(); // this must comes first
        initializeServices();
        initializeClipboardListener();
        registerBroadcastReceiver();
    }

    public void initializeToken() {
        try {
            Scanner in = new Scanner(new FileReader(Constant.tokenFileAbsPath));
            globaltoken = in.nextLine();
        } catch (FileNotFoundException e) {
            Toast.makeText(getApplication(), "NEED LOGIN!", Toast.LENGTH_LONG).show();
        }
    }

    public void initializeServices() {
        if (globaltoken  == null || globaltoken.equals("")) {
            Toast.makeText(getApplication(), "NEED LOGIN!", Toast.LENGTH_LONG).show();
            this.stopService(new Intent(this, PingService.class));
            this.stopService(new Intent(this, SyncService.class));
        } else {
            this.startService(new Intent(this, SyncService.class));
            this.startService(new Intent(this, PingService.class));
            Toast.makeText(getApplication(), "Sync start. Ping start.", Toast.LENGTH_LONG).show();
        }
    }

    public void postAndDisplayMessage(final String message) {

        try {
            mConnection.connect(Constant.postUrl, new WebSocketHandler() {

                @Override
                public void onOpen() {
                    JSONObject js = new JSONObject();
                    try {
                        js.put("token", globaltoken);
                        js.put("data", message);
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
                        CliperDbOpenHelper.insertMsg(true, message, dbHelper);
                    } catch (JSONException e) {
                        Toast.makeText(getApplication(), "Errors in sending msg." , Toast.LENGTH_LONG).show();
                    }
                }


                @Override
                public void onClose(int code, String reason) {
                    Toast.makeText(getApplication(),"message send closed", Toast.LENGTH_LONG).show();
                }
            });
        } catch (WebSocketException e) {
            Toast.makeText(getApplication(), "Errors in buliding connection.", Toast.LENGTH_LONG).show();
        }

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessage(message);
        chatMessage.setDate(DateFormat.getDateTimeInstance().format(new Date()));
        chatMessage.setMe(true);

        messageET.setText("");

        displayMessage(chatMessage);
    }

    public void initializeClipboardListener() {
        final ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cb.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                //Toast.makeText(getApplicationContext(), cb.getPrimaryClip().toString(), Toast.LENGTH_LONG).show();
                postAndDisplayMessage(cb.getPrimaryClip().getItemAt(0).getText().toString());
            }
        });
    }

    public void registerBroadcastReceiver() {
        //Registing the broadcast receiver.
        receiver=new MyReceiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction("SyncService");
        ChatActivity.this.registerReceiver(receiver, filter);
//Checking if there is a file saving token in SD card. If there is no file or the token is empty than print "need login"
        //If the token is not empty than start syncService.
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constant.REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults[0] == 0) {
                    initialize();   // from ljt
                    initControls(); // from zxz
                } else {    // user denied our permission request
                    finish();
                }
            }

        }
    }

    /*  This function receives and decodes Json messages from SyncService,
        The message have been decoded in SyncService.
        But I am trying to put the decoding part in SyncService for more convenient message transportation. 16/5/5.*/
    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ChatMessage cm = (ChatMessage) intent.getSerializableExtra("syncmsg");
            Log.i("syncmsg", cm.getMessage());
            displayMessage(cm);
//            String syncmsg = bundle.getString("syncmsg");
//            Toast.makeText(getApplication(),syncmsg , Toast.LENGTH_LONG).show();
            //tsync.setText(syncmsg);
            /*try {
            JSONObject c = new JSONObject(count);
            String msg = c.getString("msg");
            String msgId = c.optString("msgid");
        }catch (JSONException e){;}*/
        }
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
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initControls() {
        messagesContainer = (ListView) findViewById(R.id.messagesContainer);
        messageET = (EditText) findViewById(R.id.messageEdit);
        sendBtn = (Button) findViewById(R.id.chatSendButton);

        RelativeLayout container = (RelativeLayout) findViewById(R.id.container);

        loadHistory();

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = messageET.getText().toString();
                if (TextUtils.isEmpty(messageText)) {
                    return;
                }

                postAndDisplayMessage(messageText);
            }
        });


    }

    public void displayMessage(ChatMessage message) {
        adapter.add(message);
        adapter.notifyDataSetChanged();
        scroll();
    }

    private void scroll() {
        messagesContainer.setSelection(messagesContainer.getCount() - 1);
    }

    private void loadHistory(){

        chatHistory = CliperDbOpenHelper.getAllMessages(dbHelper);

        adapter = new ChatAdapter(ChatActivity.this, new ArrayList<ChatMessage>());
        messagesContainer.setAdapter(adapter);

        for(int i=0; i<chatHistory.size(); i++) {
            ChatMessage message = chatHistory.get(i);
            displayMessage(message);
        }

    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
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
