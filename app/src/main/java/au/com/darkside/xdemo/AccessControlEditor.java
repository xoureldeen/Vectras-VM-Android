package au.com.darkside.xdemo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.vectras.vm.R;

import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Editor for the list of hosts allowed to access the X server.
 *
 * Written by Matthew Kwan - March 2012
 *
 * @author mkwan
 */
public class AccessControlEditor extends ListActivity implements OnItemClickListener {
    private ArrayAdapter<String> _adapter;
    private EditText _hostField;
    private int _deletePosition = -1;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_control_editor_x_server);

        _hostField = findViewById(R.id.host_field);

        Button button;

        button = findViewById(R.id.add_button);
        button.setOnClickListener(v -> addHost());

        button = findViewById(R.id.cancel_button);
        button.setOnClickListener(v -> {
            setResult(RESULT_CANCELED, null);
            finish();
        });

        button = findViewById(R.id.apply_button);
        button.setOnClickListener(v -> {
            saveAccessList();
            setResult(RESULT_OK, null);
            finish();
        });

        getListView().setOnItemClickListener(this);
        loadAccessList();
    }

    /**
     * Called when a list item is selected.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        _deletePosition = position;
        getDeleteHostDialog().show();
    }

    /**
     * @return The Dialog to delete a host.
     */
    private Dialog getDeleteHostDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Delete the IP address?").setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteSelectedHost();
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        return builder.create();
    }

    /**
     * Delete the host that was selected by the user.
     */
    private void deleteSelectedHost() {
        if (_deletePosition < 0) return;

        _adapter.remove(_adapter.getItem(_deletePosition));
    }

    /**
     * Convert a hexadecimal IP address into a human-readable dot-separated
     * format.
     *
     * @param host    The host IP address, in hexadecimal format.
     * @return The host IP address in dot-separated format.
     */
    private static String hostToString(String host) {
        int n;

        try {
            n = (int) Long.parseLong(host, 16);
        } catch (Exception e) {
            return "Error";
        }

        int b1 = (n >> 24) & 0xff;
        int b2 = (n >> 16) & 0xff;
        int b3 = (n >> 8) & 0xff;
        int b4 = n & 0xff;

        return b1 + "." + b2 + "." + b3 + "." + b4;
    }

    /**
     * Load the list of hosts that are allowed to access the X server.
     */
    private void loadAccessList() {
        SharedPreferences prefs = getSharedPreferences("AccessControlHosts", MODE_PRIVATE);
        Map<String, ?> map = prefs.getAll();
        Set<String> set = map.keySet();
        LinkedList<String> hosts = new LinkedList<String>();

        for (String s : set)
            hosts.add(hostToString(s));

        _adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, hosts);
        setListAdapter(_adapter);
    }

    /**
     * Convert a human-readable dot-separated IP address into hexadecimal.
     * Return null if there's a parse error.
     *
     * @param s    The host IP address, in dot-separated format.
     * @return The host IP address in hexadecimal format, or null.
     */
    private static String stringToHost(String s) {
        String[] sa = s.split("\\.");

        if (sa.length != 4) return null;

        int n = 0;

        for (int i = 0; i < 4; i++) {
            int b;

            try {
                b = Integer.parseInt(sa[i]);
            } catch (Exception e) {
                return null;
            }

            if (b < 0 || b > 255) return null;

            n = (n << 8) | b;
        }

        return Integer.toHexString(n);
    }

    /**
     * Save the list of hosts that are allowed to access the X server.
     */
    private void saveAccessList() {
        SharedPreferences prefs = getSharedPreferences("AccessControlHosts", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.clear();

        int n = _adapter.getCount();

        for (int i = 0; i < n; i++) {
            String host = stringToHost(Objects.requireNonNull(_adapter.getItem(i)));

            if (host != null) editor.putBoolean(host, true);
        }

        editor.commit();
    }

    /**
     * Parse the IP address in the host field and add it to the list.
     */
    private void addHost() {
        String s = _hostField.getText().toString();

        if (stringToHost(s) != null) _adapter.add(s);
        else Toast.makeText(this, "Bad IP address '" + s + "'", Toast.LENGTH_LONG).show();
    }
}