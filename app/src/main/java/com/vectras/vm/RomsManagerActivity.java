package com.vectras.vm;

import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.vectras.vm.Roms.AdapterRomStoreSearch;
import com.vectras.vm.Roms.AdapterRoms;
import com.vectras.vm.Roms.DataRoms;
import com.vectras.vm.utils.UIUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class RomsManagerActivity extends AppCompatActivity {

    private RequestNetwork net;
    private RequestNetwork.RequestListener _net_request_listener;
    private String contentJSON = "[]";
    public static RomsManagerActivity activity;
    public static String license;
    private SearchView searchView;
    private RecyclerView romsSearch;
    private AdapterRoms mAdapter;
    private AdapterRomStoreSearch mAdapterSearch;
    private List<DataRoms> data = new ArrayList<>();
    private List<DataRoms> dataSearch = new ArrayList<>();
    private LinearLayout linearload;
    private LinearLayout linearnothinghere;
    public static String sAvailable = "";
    public static String sUnavailable = "";
    public static String sInstalled = "";
    public static boolean isFinishNow = false;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        activity = this;

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        sAvailable = getResources().getString(R.string.available);
        sUnavailable = getResources().getString(R.string.unavailable);
        sInstalled = getResources().getString(R.string.installed);

        UIUtils.edgeToEdge(this);
        setContentView(R.layout.activity_roms_manager);
        UIUtils.setOnApplyWindowInsetsListenerTop(findViewById(R.id.appbar));
        UIUtils.setOnApplyWindowInsetsListenerBottom(findViewById(R.id.romsRv));
        UIUtils.setOnApplyWindowInsetsListenerBottom(findViewById(R.id.romsSearch));
        UIUtils.setOnApplyWindowInsetsListenerBottom(findViewById(R.id.linear_search_emty));

        linearload = findViewById(R.id.linearload);
        linearnothinghere = findViewById(R.id.linearnothinghere);
        Button buttontryagain = findViewById(R.id.buttontryagain);
        RecyclerView mRVRoms = findViewById(R.id.romsRv);
        romsSearch = findViewById(R.id.romsSearch);

        mRVRoms = findViewById(R.id.romsRv);
        mAdapter = new AdapterRoms(this, data);
        mRVRoms.setAdapter(mAdapter);
        mRVRoms.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        romsSearch = findViewById(R.id.romsSearch);
        mAdapterSearch = new AdapterRomStoreSearch(this, dataSearch);
        romsSearch.setAdapter(mAdapterSearch);
        romsSearch.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        SearchBar searchBar = findViewById(R.id.search_bar);
        searchBar.setNavigationOnClickListener(v -> onBackPressed());

        searchView = findViewById(R.id.searchView);
        searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                search(s.toString());
            }

            @Override
            public void onTextChanged(CharSequence newText, int start, int before, int count) {
            }
        });

        net = new RequestNetwork(this);
        _net_request_listener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                if (!response.isEmpty())
                        contentJSON = response;
                loadData();
                loadDataSearch();
                linearload.setVisibility(View.GONE);
            }

            @Override
            public void onErrorResponse(String tag, String message) {
                linearload.setVisibility(View.GONE);
                linearnothinghere.setVisibility(View.VISIBLE);
            }
        };

        buttontryagain.setOnClickListener(v -> {
            linearload.setVisibility(View.VISIBLE);
            net.startRequestNetwork(RequestNetworkController.GET,AppConfig.vectrasRaw + "vroms-store.json","",_net_request_listener);
        });

        net.startRequestNetwork(RequestNetworkController.GET,AppConfig.vectrasRaw + "vroms-store.json","",_net_request_listener);
    }

    public void onResume() {
        super.onResume();
        if (isFinishNow)
            finish();
        isFinishNow = false;
    }

    private void loadData() {
        try {
            List<DataRoms> dataRoms;
            Gson gson = new Gson();
            Type listType = new TypeToken<List<DataRoms>>() {}.getType();
            dataRoms = gson.fromJson(contentJSON, listType);

            data.clear();
            data.addAll(dataRoms);
            mAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            linearload.setVisibility(View.GONE);
            linearnothinghere.setVisibility(View.VISIBLE);
        }
    }

    private void loadDataSearch() {
        List<DataRoms> dataRoms;
        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<DataRoms>>() {}.getType();
            dataRoms = gson.fromJson(contentJSON, listType);

            dataSearch.clear();
            dataSearch.addAll(dataRoms);
            mAdapterSearch.notifyDataSetChanged();
        } catch (Exception e) {
            linearload.setVisibility(View.GONE);
            linearnothinghere.setVisibility(View.VISIBLE);
        }
    }

    private void search(String keyword) {
        try {
            // Extract data from json and store into ArrayList as class objects
            Gson gson = new Gson();
            Type listType = new TypeToken<List<DataRoms>>() {}.getType();
            List<DataRoms> allData = gson.fromJson(contentJSON, listType);
            List<DataRoms> filteredData = new ArrayList<>();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                filteredData = allData.stream()
                        .filter(rom -> {
                            String romName = (rom.romName != null) ? rom.romName : "";
                            String romKernel = (rom.romKernel != null) ? rom.romKernel : "";

                            return romName.toLowerCase().contains(keyword.toLowerCase())
                                    || romKernel.toLowerCase().contains(keyword.toLowerCase());
                        })
                        .collect(Collectors.toList());
            } else {
                for (DataRoms rom : allData) {
                    if (rom.romName.toLowerCase().contains(keyword.toLowerCase()) ||
                            rom.romKernel.toLowerCase().contains(keyword.toLowerCase())) {
                        filteredData.add(rom);
                    }
                }
            }

            dataSearch.clear();
            dataSearch.addAll(filteredData);
        } catch (Exception e) {
            Log.e("RomManagerActivity", "Json parsing error: " + e.getMessage());
        }

        if (dataSearch.isEmpty())
            romsSearch.setVisibility(View.GONE);
        else
            romsSearch.setVisibility(View.VISIBLE);

        mAdapterSearch.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if (searchView.isShowing())
            searchView.hide();
        else
            super.onBackPressed();
    }
}
