package com.vectras.vm;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.*;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.vectras.vm.R;
import com.vectras.vm.Blog.AdapterBlog;
import com.vectras.vm.Blog.DataBlog;
import com.vectras.vm.Fragment.HomeFragment;
import com.vectras.vm.Store.AdapterStore;
import com.vectras.vm.Store.DataStore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class StoreActivity extends AppCompatActivity{
    private RecyclerView mRVStore;
    private AdapterStore mAdapter;
    public static LinearLayout noConnectionLayout;
    public SwipeRefreshLayout pullToRefresh;
    public static StoreActivity activity;
    public String Data;
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_store);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setTitle(getString(R.string.app_name));
        
        activity = this;

        noConnectionLayout = findViewById(R.id.noConnectionLayout);
        mRVStore = findViewById(R.id.storeRv);

        if (checkConnection(activity)) {
            new StoreActivity.AsyncLogin().execute();
            noConnectionLayout.setVisibility(View.GONE);
            //mRVBlog.setVisibility(View.VISIBLE);
        } else {
            noConnectionLayout.setVisibility(View.VISIBLE);
            //mRVBlog.setVisibility(View.GONE);
        }

        pullToRefresh = findViewById(R.id.refreshLayout);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (checkConnection(activity)) {
                    new StoreActivity.AsyncLogin().execute();
                } else {
                    noConnectionLayout.setVisibility(View.VISIBLE);
                    pullToRefresh.setRefreshing(false);
                }
            }
        });
    }
    public boolean checkConnection(Context context) {
        final ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connMgr != null) {
            NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();

            if (activeNetworkInfo != null) { // connected to the internet
                // connected to the mobile provider's data plan
                if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    // connected to wifi
                    return true;
                } else
                    return activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
            }
        }
        return false;
    }

    private class AsyncLogin extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread

        }

        @Override
        protected String doInBackground(String... params) {
            HttpsURLConnection con = null;
            try {
                URL u = new URL(AppConfig.storeJson);
                con = (HttpsURLConnection) u.openConnection();

                con.connect();

                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();
                Data = sb.toString();

                return (Data);

            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                if (con != null) {
                    try {
                        con.disconnect();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                return ("unsuccessful!");
            }

        }

        @Override
        protected void onPostExecute(String result) {

            //this method will be running on UI thread
            pullToRefresh.setRefreshing(false);

            noConnectionLayout.setVisibility(View.GONE);

            List<DataStore> data = new ArrayList<>();

            try {

                JSONArray jArray = new JSONArray(Data);

                // Extract data from json and store into ArrayList as class objects
                for (int i = 0; i < jArray.length(); i++) {
                    JSONObject json_data = jArray.getJSONObject(i);
                    DataStore storeData = new DataStore();
                    storeData.itemName = json_data.getString("item_name");
                    storeData.itemIcon = json_data.getString("item_icon");
                    storeData.itemData = json_data.getString("item_data");
                    storeData.itemSize = json_data.getString("item_size");
                    storeData.itemLink = json_data.getString("item_link");
                    storeData.itemPreviewMain = json_data.getString("item_preview_main");
                    storeData.itemPreview1 = json_data.getString("item_preview_1");
                    storeData.itemPreview2 = json_data.getString("item_preview_2");
                    data.add(storeData);
                }

                // Setup and Handover data to recyclerview

            } catch (JSONException e) {
                Toast.makeText(activity, e.toString(), Toast.LENGTH_LONG).show();
            }
            mRVStore = (RecyclerView) findViewById(R.id.storeRv);
            mAdapter = new AdapterStore(activity, data);
            mRVStore.setAdapter(mAdapter);
            mRVStore.setLayoutManager(new LinearLayoutManager(activity));

        }

    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()== android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
