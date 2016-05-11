package com.example.zhe.zhe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
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
import java.io.PrintWriter;
import java.util.Scanner;
import android.os.Environment;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private MyReceiver receiver=null;
    public String globaltoken = "";
    private final String tokenFile = "/cliper.token";
    private final String sdcardPath = Environment.getExternalStorageDirectory().getPath();

    /*This function receives and decodes Json messages from SyncService,
    The message have been decoded in SyncService.
     BUt I am trying to put the decoding part in SyncService for more comvience message transportation. 16/5/5.*/
    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            String syncmsg = bundle.getString("syncmsg");
            Toast.makeText(getApplication(),"You received: " + syncmsg , Toast.LENGTH_LONG).show();
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
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        //Registing the broadcast receiver.
        receiver=new MyReceiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction("SyncService");
        MainActivity.this.registerReceiver(receiver, filter);

        //Checking if there is a file saving token in SD card. If there is no file or the token is empty than print "need login"
        //If the token is not empty than start syncService.
        try {
            Scanner in = new Scanner(new FileReader(sdcardPath + tokenFile));
            globaltoken = in.nextLine();
        } catch (FileNotFoundException e) {
            Toast.makeText(getApplication(), "NEED LOGIN!", Toast.LENGTH_LONG).show();
            //tv.setText("NEED LOGIN");
        }

        if (globaltoken  == null || globaltoken.equals("")) {
            Toast.makeText(getApplication(), "NEED LOGIN!", Toast.LENGTH_LONG).show();
            //tv.setText("Need login");
            //this.startService(new Intent(this, SyncService.class));
        } else {
            Toast.makeText(getApplication(), "You have logined.", Toast.LENGTH_LONG).show();
            //tv.setText("You have logined.");
            //tlog.setText("Token: " + globaltoken);
            this.startService(new Intent(this, SyncService.class));
        }


        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_register){
             Intent intent = new Intent (this,zhe.zhe.RegisterActivity.class);
            startActivity(intent);
        }

        else if (id == R.id.nav_login) {
            Intent intent = new Intent(this,zhe.zhe.LoginActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_send) {

        } else if (id == R.id.nav_receive) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
