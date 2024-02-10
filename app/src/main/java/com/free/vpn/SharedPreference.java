package com.free.vpn;

import android.content.Context;
import android.content.SharedPreferences;

import com.free.vpn.model.Server;

public class SharedPreference {

    private static final String APP_PREFS_NAME = "ScytaleVPNPreference";

    private SharedPreferences mPreference;
    private SharedPreferences.Editor mPrefEditor;
    private Context context;

    private static final String SERVER_COUNTRY = "server_country";
    private static final String SERVER_OVPN = "server_ovpn";
    private static final String SERVER_OVPN_USER = "server_ovpn_user";
    private static final String SERVER_OVPN_PASSWORD = "server_ovpn_password";
    private static final String IS_SERVER_RUNNING = "is_server_running";
    private static final String SERVER_RUNNING_START_TIME = "server_running_start_time";
    private static final String SERVER_RUNNING_TIME_IN_MILLISECONDS = "server_running_time_in_milliseconds";
    private static final String IS_SERVER_LOSING_NETWORK_ON_RUNNING =  "is_server_losing_network_on_running";
    private static final String SERVER_RUNNING_NO_NETWORK_TIME_MILLISECONDS =  "server_running_no_network_time_in_milliseconds";

    public SharedPreference(Context context) {
        this.mPreference = context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE);
        this.mPrefEditor = mPreference.edit();
        this.context = context;
    }

    /**
     * Save server details
     * @param server details of ovpn server
     */
    public void saveServer(Server server){
        mPrefEditor.putString(SERVER_COUNTRY, server.getCountry());
        mPrefEditor.putString(SERVER_OVPN, server.getOvpn());
        mPrefEditor.putString(SERVER_OVPN_USER, server.getOvpnUserName());
        mPrefEditor.putString(SERVER_OVPN_PASSWORD, server.getOvpnUserPassword());
        mPrefEditor.commit();
    }

    /**
     * Get server data from shared preference
     * @return server model object
     */
    public Server getServer() {

        Server server = new Server(
                mPreference.getString(SERVER_COUNTRY,""),
                mPreference.getString(SERVER_OVPN, ""),
                mPreference.getString(SERVER_OVPN_USER,""),
                mPreference.getString(SERVER_OVPN_PASSWORD,"")
        );

        return server;
    }

    public void setIsServerRunning(Boolean isServerRunning){
        mPrefEditor.putBoolean(IS_SERVER_RUNNING, isServerRunning);
        mPrefEditor.commit();
    }

    public Boolean getIsServerRunning(){
        return mPreference.getBoolean(IS_SERVER_RUNNING, false);
    }

    public long getServerStartTimeRunning() {
        return mPreference.getLong(SERVER_RUNNING_START_TIME, 0);
    }

    public void setServerStartTimeRunning(long startTimeRunning){
        mPrefEditor.putLong(SERVER_RUNNING_START_TIME, startTimeRunning);
        mPrefEditor.commit();
    }

    public long getServerRunningTimeInMilliseconds() {
        return mPreference.getLong(SERVER_RUNNING_TIME_IN_MILLISECONDS, 0);
    }

    public void setServerRunningTimeInMilliseconds(long serverRunningTimeInMilliseconds){
        mPrefEditor.putLong(SERVER_RUNNING_TIME_IN_MILLISECONDS, serverRunningTimeInMilliseconds);
        mPrefEditor.commit();
    }

    public void setIsServerLosingNetworkOnRunning(Boolean isServerLosingNetworkOnRunning){
        mPrefEditor.putBoolean(IS_SERVER_LOSING_NETWORK_ON_RUNNING, isServerLosingNetworkOnRunning);
        mPrefEditor.commit();
    }

    public Boolean getIsServerLosingNetworkOnRunning(){
        return mPreference.getBoolean(IS_SERVER_LOSING_NETWORK_ON_RUNNING, false);
    }

    public long getServerRunningNoNetworkTimeInMilliseconds() {
        return mPreference.getLong(SERVER_RUNNING_NO_NETWORK_TIME_MILLISECONDS, 0);
    }

    public void setServerRunningNoNetworkTimeInMilliseconds(long serverRunningNoNetworkTimeInMilliseconds){
        mPrefEditor.putLong(SERVER_RUNNING_NO_NETWORK_TIME_MILLISECONDS, serverRunningNoNetworkTimeInMilliseconds);
        mPrefEditor.commit();
    }
}
