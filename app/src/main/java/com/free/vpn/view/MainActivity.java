package com.free.vpn.view;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.free.vpn.CheckInternetConnection;

import com.free.vpn.R;
import com.free.vpn.SharedPreference;
import com.free.vpn.databinding.ActivityMainBinding;
import com.free.vpn.model.Server;
import com.free.vpn.utils.Utils;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.blinkt.openvpn.OpenVpnApi;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.OpenVPNThread;
import de.blinkt.openvpn.core.VpnStatus;
import android.os.SystemClock;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ActivityMainBinding binding;
    private Server server;
    private CheckInternetConnection connection;

    private OpenVPNThread vpnThread = new OpenVPNThread();
    private OpenVPNService vpnService = new OpenVPNService();
    boolean vpnStart = false;
    private SharedPreference preference;
    String mIp = null;
    private long startTime = 0L;
    private Handler customHandlerTimer = new Handler();
    volatile static long timeInMillisecondsTimer = 0L;
    volatile static long offline = 0L;

    private Handler customHandlerNetworkChecker = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preference = new SharedPreference(this);
        server = preference.getServer();
        if("".equals(server.getOvpnUserName()) || "".equals(server.getOvpnUserPassword())){
            openSettingsActivity();
        } else {
            super.onCreate(savedInstanceState);
            customHandlerNetworkChecker.postDelayed(updateNetworkCheckerThread, 0);

            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            binding.settingsImgBtn.setOnClickListener(this);
            binding.btnConnectDisconnect.setOnClickListener(this);
            if(isConnected()){
                getIp();
            }
        }
    }

    private final Runnable updateNetworkCheckerThread = new Runnable() {
        public synchronized void run() {
            if(isConnected()){
                getIp();
                if(preference.getIsServerRunning() && preference.getIsServerLosingNetworkOnRunning()){
                    preference.setIsServerLosingNetworkOnRunning(false);
                    offline = ((SystemClock.uptimeMillis())-(preference.getServerRunningNoNetworkTimeInMilliseconds()));
                    customHandlerTimer.postDelayed(updateTimerThread, 0);
                } else if(preference.getIsServerRunning()){
                    binding.statusValueTextView.setText(R.string.status_connected_value_text_view_main);
                }
            } else if(preference.getIsServerRunning() && !preference.getIsServerLosingNetworkOnRunning()){
                preference.setIsServerLosingNetworkOnRunning(true);
                preference.setServerRunningNoNetworkTimeInMilliseconds(SystemClock.uptimeMillis());
                customHandlerTimer.removeCallbacks(updateTimerThread);
            } else {
                binding.yourIpValueTextView.setText(R.string.status_no_network_value_text_view_main);
                binding.statusValueTextView.setText(R.string.status_no_network_value_text_view_main);
            }
            customHandlerNetworkChecker.postDelayed(this, 1000);
        }
    };

    private final Runnable updateTimerThread = new Runnable() {
        public synchronized void run() {
            timeInMillisecondsTimer = (SystemClock.uptimeMillis() - startTime -offline);
            int seconds = (int) (timeInMillisecondsTimer / 1000) % 60 ;
            int minutes = (int) ((timeInMillisecondsTimer / (1000*60)) % 60);
            int hours   = (int) ((timeInMillisecondsTimer / (1000*60*60)) % 24);
            binding.connectionTimeValueTextView.setText(String.format("%02d", hours) +":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds));
            customHandlerTimer.postDelayed(this, 1000);
        }
    };


    private  boolean isConnected(){
        ConnectivityManager conManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] networkInfo = conManager.getAllNetworkInfo();
        for (NetworkInfo netInfo : networkInfo) {
            if (netInfo.getTypeName().equalsIgnoreCase("WIFI")) {
                if (netInfo.isConnected()) {
                    return true;
                }
            } else if (netInfo.getTypeName().equalsIgnoreCase("MOBILE")) {
                if (netInfo.isConnected()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void getIp(){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                URL url = new URL("https://api.ipify.org");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0"); // Set a User-Agent to avoid HTTP 403 Forbidden error
                try (Scanner s = new Scanner(connection.getInputStream(), "UTF-8").useDelimiter("\\A")) {
                    mIp = s.next();
                }
                connection.disconnect();
            } catch (Exception e) {
                mIp = null;
                e.printStackTrace();
            }
            handler.post(() -> {
                applyIpInView(mIp);
            });
        });
    }

    private void applyIpInView(String ip){

        if(ip != null){
            binding.yourIpValueTextView.setText(ip);
        } else {
            binding.yourIpValueTextView.setText(R.string.status_no_network_value_text_view_main);
        }
    }

    private void initializeAll() {
        preference = new SharedPreference(this);
        server = preference.getServer();

        connection = new CheckInternetConnection();
    }

    @Override
    public void onResume(){
        super.onResume();
        if(isConnected()){
            getIp();
        }
        // Checking is vpn already running or not
        isServiceRunning();
        VpnStatus.initLogCache(this.getCacheDir());
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("connectionState"));
    }
    @Override
    public void onClick(View v) {
        initializeAll();
        switch (v.getId()) {
            case R.id.settingsImgBtn:
                openSettingsActivity();
                break;
            case R.id.btnConnectDisconnect:
                // Vpn is running, user would like to disconnect current connection.
                if (vpnStart) {
                    confirmDisconnect();
                }else {
                    prepareVpn();
                }
                break;
        }
    }

    private void openSettingsActivity(){
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    //    Show show disconnect confirm dialog
    public void confirmDisconnect(){
        offline = 0L;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(this.getString(R.string.connection_close_confirm));

        builder.setPositiveButton(this.getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                stopVpn();
            }
        });
        builder.setNegativeButton(this.getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //    Prepare for vpn connect with required permission
    private void prepareVpn() {
        if (!vpnStart) {
            if (getInternetStatus()) {
                // Checking permission for network monitor
                Intent intent = VpnService.prepare(this);
                if (intent != null) {
                    startActivityForResult(intent, 1);
                } else startVpn();//have already permission
                // Update confection status
                status("connecting");
            } else {
                // No internet connection available
                showToast("you have no internet connection !!");
            }

        } else if (stopVpn()) {
            // VPN is stopped, show a Toast message.
            showToast("Disconnect Successfully");
        }
    }

    // Stop vpn and return boolean: VPN status
    public boolean stopVpn() {
        try {
            vpnThread.stop();
            status("connect");
            vpnStart = false;
            if(isConnected()){
                getIp();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            //Permission granted, start the VPN
            startVpn();
        } else {
            showToast("Permission Deny !! ");
        }
    }
    //    Internet connection status.
    public boolean getInternetStatus() {
        return connection.netCheck(this);
    }

    // Get service status
    public void isServiceRunning() {
        setStatus(vpnService.getStatus());
    }

    // Start the VPN
    private void startVpn() {
        try {
            // .ovpn file
            InputStream conf = this.getAssets().open(server.getOvpn());
            InputStreamReader isr = new InputStreamReader(conf);
            BufferedReader br = new BufferedReader(isr);
            String config = "";
            String line;
            while (true) {
                line = br.readLine();
                if (line == null) break;
                config += line + "\n";
            }
            br.readLine();
            OpenVpnApi.startVpn(this, config, server.getCountry(), server.getOvpnUserName(), server.getOvpnUserPassword());
            // Update log
            binding.statusValueTextView.setText("Connecting...");
            vpnStart = true;
        } catch (IOException | RemoteException e) {
            e.printStackTrace();
        }
    }

    // Status change with corresponding vpn connection status @param connectionState
    public void setStatus(String connectionState) {
        if (connectionState!= null){
            switch (connectionState) {
                case "DISCONNECTED": ;
                    status("connect");
                    vpnStart = false;
                    vpnService.setDefaultStatus();
                    binding.statusValueTextView.setText(R.string.status_disconected_value_text_view_main_default);
                    preference.setIsServerRunning(false);
                    customHandlerTimer.removeCallbacks(updateTimerThread);
                    binding.shieldNotConnectedImageView.setVisibility(View.INVISIBLE);
                    binding.shieldConnectedImageView.setVisibility(View.VISIBLE);
                    break;
                case "CONNECTED":
                    vpnStart = true;// it will use after restart this activity
                    status("connected");
                    binding.statusValueTextView.setText(R.string.status_connected_value_text_view_main);
                    binding.shieldConnectedImageView.setVisibility(View.INVISIBLE);
                    binding.shieldNotConnectedImageView.setVisibility(View.VISIBLE);
                    if(!preference.getIsServerRunning()){
                        // If Server is not running start timer with current timer from zero
                        preference.setIsServerRunning(true);
                        startTime = SystemClock.uptimeMillis();
                        customHandlerTimer.postDelayed(updateTimerThread, 0);
                    } else {
                        // If Server is running get running time from shared preferences
                        // and start timer with data from shared preferences
                        startTime = preference.getServerStartTimeRunning();
                        timeInMillisecondsTimer  = preference.getServerRunningTimeInMilliseconds();
                        customHandlerTimer.postDelayed(updateTimerThread, 0);
                    }
                    break;
                case "WAIT":
                    binding.statusValueTextView.setText(R.string.status_waiting_connection_value_text_view_main);
                    break;
                case "AUTH":
                    binding.statusValueTextView.setText(R.string.status_authenticating_value_text_view_main);
                    break;
                case "RECONNECTING":
                    status("connecting");
                    binding.statusValueTextView.setText(R.string.status_reconnecting_value_text_view_main);
                    break;
                case "NONETWORK":
                    applyIpInView(null);
                    binding.statusValueTextView.setText(R.string.status_no_network_value_text_view_main);
                    break;
            }
        }
    }

    // Change button background color and text @param status: VPN current status
    public void status(String status) {
        if (status.equals("connect")) {
            binding.btnConnectDisconnect.setText(this.getString(R.string.connect));
        } else if (status.equals("connecting")) {
            binding.btnConnectDisconnect.setText(this.getString(R.string.connecting));
        } else if (status.equals("connected")) {
            binding.btnConnectDisconnect.setText(this.getString(R.string.disconnect));
        } else if (status.equals("tryDifferentServer")) {
            binding.btnConnectDisconnect.setText("Try Different\nServer");
        } else if (status.equals("loading")) {
            binding.btnConnectDisconnect.setText("Loading Server..");
        } else if (status.equals("invalidDevice")) {
            binding.btnConnectDisconnect.setText("Invalid Device");
        } else if (status.equals("authenticationCheck")) {
            binding.btnConnectDisconnect.setText("Authentication \n Checking...");
        }

    }

    // Receive broadcast message
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                setStatus(intent.getStringExtra("state"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                String duration = intent.getStringExtra("duration");
                if (duration == null) duration = "00:00:00";
                updateConnectionStatus(duration);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    // Update status UI - duration: running time
    public void updateConnectionStatus(String duration) {
        if(!duration.equals("00:00:00")){
            binding.connectionTimeIdleTextView.setVisibility(View.INVISIBLE);
            binding.connectionTimeValueTextView.setVisibility(View.VISIBLE);
        } else if(duration.equals("00:00:00")){
            binding.connectionTimeValueTextView.setVisibility(View.INVISIBLE);
            binding.connectionTimeIdleTextView.setVisibility(View.VISIBLE);
        }
    }

    // Show toast message @param message: toast message
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    // Save current selected server on local shared preference
    @Override
    public void onStop() {
        if (server != null) {
            preference.saveServer(server);
        }
        // If Server is running save running time in shared preferences and stop timer
        if(preference.getIsServerRunning()){
            preference.setServerStartTimeRunning(startTime);
            preference.setServerRunningTimeInMilliseconds(timeInMillisecondsTimer);
            customHandlerTimer.removeCallbacks(updateTimerThread);
        }

        super.onStop();
    }
}

