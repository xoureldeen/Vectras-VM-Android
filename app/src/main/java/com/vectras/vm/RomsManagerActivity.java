package com.vectras.vm;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.vectras.vm.Roms.AdapterRomStoreSearch;
import com.vectras.vm.Roms.AdapterRoms;
import com.vectras.vm.Roms.DataRoms;
import com.vectras.vm.databinding.ActivityRomsManagerBinding;
import com.vectras.vm.utils.UIUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class RomsManagerActivity extends AppCompatActivity {

    ActivityRomsManagerBinding binding;
    private RequestNetwork net;
    private RequestNetwork.RequestListener _net_request_listener;
    private String contentJSON = "[]";
    public static String license;
    private AdapterRomStoreSearch mAdapterSearch;
    private List<DataRoms> data = new ArrayList<>();
    private List<DataRoms> dataSearch = new ArrayList<>();
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

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        UIUtils.edgeToEdge(this);
        binding = ActivityRomsManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UIUtils.setOnApplyWindowInsetsListenerTop(findViewById(R.id.appbar));
        UIUtils.setOnApplyWindowInsetsListenerBottom(findViewById(R.id.romsRv));
        UIUtils.setOnApplyWindowInsetsListenerBottom(findViewById(R.id.romsSearch));
        UIUtils.setOnApplyWindowInsetsListenerBottom(findViewById(R.id.linear_search_emty));

        binding.searchBar.setNavigationOnClickListener(v -> onBackPressed());

        binding.searchView.getEditText().addTextChangedListener(new TextWatcher() {
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
                binding.linearload.setVisibility(View.GONE);
            }

            @Override
            public void onErrorResponse(String tag, String message) {
                binding.linearload.setVisibility(View.GONE);
                binding.linearnothinghere.setVisibility(View.VISIBLE);
            }
        };

        binding.buttontryagain.setOnClickListener(v -> {
            binding.linearload.setVisibility(View.VISIBLE);
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

    @Override
    public void onBackPressed() {
        if (binding.searchView.isShowing())
            binding.searchView.hide();
        else
            super.onBackPressed();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadData() {
        AdapterRoms mAdapter = new AdapterRoms(this, data);
        binding.romsRv.setAdapter(mAdapter);
        binding.romsRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        mAdapterSearch = new AdapterRomStoreSearch(this, dataSearch);
        binding.romsSearch.setAdapter(mAdapterSearch);
        binding.romsSearch.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        List<DataRoms> dataRoms = new ArrayList<>();

        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<DataRoms>>() {}.getType();
            dataRoms = gson.fromJson(contentJSON, listType);
        } catch (Exception e) {
            binding.linearload.setVisibility(View.GONE);
            binding.linearnothinghere.setVisibility(View.VISIBLE);
        }

        data.clear();
        data.addAll(dataRoms);
        mAdapter.notifyDataSetChanged();

        dataSearch.clear();
        dataSearch.addAll(dataRoms);
        mAdapterSearch.notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
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
            binding.romsSearch.setVisibility(View.GONE);
        else
            binding.romsSearch.setVisibility(View.VISIBLE);

        mAdapterSearch.notifyDataSetChanged();
    }
}
