package com.vectras.vm.home.romstore;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.transition.MaterialFadeThrough;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.vectras.vm.AppConfig;
import com.vectras.vm.RequestNetwork;
import com.vectras.vm.RequestNetworkController;
import com.vectras.vm.Roms.AdapterRoms;
import com.vectras.vm.Roms.DataRoms;
import com.vectras.vm.databinding.FragmentHomeRomStoreBinding;
import com.vectras.vm.home.HomeActivity;
import com.vectras.vm.home.core.SharedData;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RomStoreFragment extends Fragment {

    FragmentHomeRomStoreBinding binding;
    private RequestNetwork net;
    private RequestNetwork.RequestListener _net_request_listener;
    private String contentJSON = "[]";
    HomeRomStoreViewModel homeRomStoreViewModel;
    RomStoreHomeAdpater mAdapter;
    List<DataRoms> data = new ArrayList<>();

    public static RomStoreCallToHomeListener romStoreCallToHomeListener;
    public interface RomStoreCallToHomeListener {
        void updateDataStatus(boolean isReady);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialFadeThrough());
        setReturnTransition(new MaterialFadeThrough());
        setExitTransition(new MaterialFadeThrough());
        setReenterTransition(new MaterialFadeThrough());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentHomeRomStoreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new RomStoreHomeAdpater(getContext(), data);
        binding.rvRomlist.setAdapter(mAdapter);
        binding.rvRomlist.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        homeRomStoreViewModel = new ViewModelProvider(requireActivity()).get(HomeRomStoreViewModel.class);
        homeRomStoreViewModel.getRomsList().observe(getViewLifecycleOwner(), roms -> {
            if (roms == null || roms.isEmpty()) {
                loadFromServer();
            } else {
                binding.linearload.setVisibility(View.GONE);
                data.clear();
                data.addAll(roms);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private void loadFromServer() {
        romStoreCallToHomeListener.updateDataStatus(false);

        net = new RequestNetwork(requireActivity());
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

        net.startRequestNetwork(RequestNetworkController.GET, AppConfig.vectrasRaw + "vroms-store.json","",_net_request_listener);
    }

    private void loadData() {
        List<DataRoms> dataRoms = new ArrayList<>();

        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<DataRoms>>() {}.getType();
            dataRoms = gson.fromJson(contentJSON, listType);
        } catch (Exception e) {
            binding.linearload.setVisibility(View.GONE);
            binding.linearnothinghere.setVisibility(View.VISIBLE);
        }

        homeRomStoreViewModel.setRomsList(dataRoms);
        data.clear();
        data.addAll(dataRoms);
        mAdapter.notifyDataSetChanged();
        SharedData.dataRomStore.addAll(dataRoms);
        romStoreCallToHomeListener.updateDataStatus(true);
    }
}