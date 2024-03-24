package com.undatech.opaque.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.iiordanov.bVNC.ConnectionBean;
import com.iiordanov.bVNC.Database;
import com.undatech.opaque.Connection;
import com.undatech.opaque.ConnectionSettings;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConnectionLoader {
    private static String TAG = "ConnectionLoader";
    private Context appContext;
    private boolean connectionsInSharedPrefs;
    private Map<String, Connection> connectionsById;
    private String[] connectionPreferenceFiles;
    private int numConnections = 0;
    private Activity activity;

    public ConnectionLoader(Context appContext, Activity activity, boolean connectionsInSharedPrefs) {
        this.appContext = appContext;
        this.connectionsInSharedPrefs = connectionsInSharedPrefs;
        this.activity = activity;
        this.connectionsById = new HashMap<>();
        loadConnectionsById();
    }

    public Map<String, Connection> loadConnectionsById() {
        if (connectionsInSharedPrefs) {
            loadFromSharedPrefs();
        } else {
            loadFromDatabase();
        }
        return connectionsById;
    }

    private void loadFromDatabase() {
        Database database = new Database(this.appContext);
        SQLiteDatabase db = database.getWritableDatabase();

        ArrayList<ConnectionBean> connections = new ArrayList<ConnectionBean>();
        ConnectionBean.getAll(db, ConnectionBean.GEN_TABLE_NAME, connections, ConnectionBean.newInstance);
        Collections.sort(connections);
        numConnections = connections.size();
        if (connections.size() == 0) {
            android.util.Log.i(TAG, "No connections in the database");
        } else {
            for (int i = 0; i < connections.size(); i++) {
                Connection connection = connections.get(i);
                connection.setRuntimeId(Integer.toString(i));
                connectionsById.put(Integer.toString(i), connection);
            }
        }
        database.close();
    }

    private void loadFromSharedPrefs() {
        SharedPreferences sp = appContext.getSharedPreferences("generalSettings", Context.MODE_PRIVATE);
        String connections = sp.getString("connections", null);
        android.util.Log.d(TAG, "Loading connections from this list: " + connections);
        if (connections != null && !connections.equals("")) {
            connectionPreferenceFiles = connections.split(" ");
            numConnections = connectionPreferenceFiles.length;
            for (int i = 0; i < numConnections; i++) {
                Connection cs = new ConnectionSettings(connectionPreferenceFiles[i]);
                cs.setRuntimeId(Integer.toString(i));
                cs.load(appContext);
                android.util.Log.d(TAG, "Adding label: " + cs.getLabel());
                connectionsById.put(Integer.toString(i), cs);
            }
        }
    }

    public boolean isConnectionsInSharedPrefs() {
        return connectionsInSharedPrefs;
    }

    public String[] getConnectionPreferenceFiles() {
        return connectionPreferenceFiles;
    }

    public int getNumConnections() {
        return numConnections;
    }

    public Map<String, Connection> getConnectionsById() {
        return connectionsById;
    }
}
