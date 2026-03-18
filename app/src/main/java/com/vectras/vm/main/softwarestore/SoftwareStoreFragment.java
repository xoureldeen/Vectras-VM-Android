package com.vectras.vm.main.softwarestore;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.anbui.elephant.retrofit2utils.Retrofit2Utils;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.vectras.vm.AppConfig;
import com.vectras.vm.main.romstore.DataRoms;
import com.vectras.vm.databinding.FragmentHomeSoftwareStoreBinding;
import com.vectras.vm.main.core.SharedData;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SoftwareStoreFragment extends Fragment {

    FragmentHomeSoftwareStoreBinding binding;
    private String contentJSON = "[]";
    SoftwareStoreViewModel homeSoftwareStoreViewModel;
    SoftwareStoreHomeAdapter mAdapter;
    List<DataRoms> data = new ArrayList<>();
    LinearLayoutManager layoutManager;

    public static SoftwareStoreFragment.SoftwareStoreCallToHomeListener softwareStoreCallToHomeListener;
    public interface SoftwareStoreCallToHomeListener {
        void updateSearchStatus(boolean isReady);
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
        binding = FragmentHomeSoftwareStoreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new SoftwareStoreHomeAdapter(getContext(), data, false);
        layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        binding.rvSoftwarelist.setAdapter(mAdapter);
        binding.rvSoftwarelist.setLayoutManager(layoutManager);

        binding.rvSoftwarelist.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();

                if (lastVisibleItem >= totalItemCount - 2) {
                    mAdapter.loadMore();
                }
            }
        });

        homeSoftwareStoreViewModel = new ViewModelProvider(requireActivity()).get(SoftwareStoreViewModel.class);
        homeSoftwareStoreViewModel.getSoftwareList().observe(getViewLifecycleOwner(), roms -> {
            if (roms == null || roms.isEmpty()) {
                loadFromServer();
            } else {
                binding.linearload.setVisibility(View.GONE);
                mAdapter.submitList(roms);
            }
        });

        binding.buttontryagain.setOnClickListener(v -> loadFromServer());
    }

    private void loadFromServer() {
        softwareStoreCallToHomeListener.updateSearchStatus(false);
        binding.linearload.setVisibility(View.VISIBLE);

        Retrofit2Utils.get(AppConfig.vectrasRaw + "software-store.json", ((isSuccess, body, status, error) -> {
            binding.linearload.setVisibility(View.GONE);
            if (isSuccess) {
                if (!body.isEmpty())
                    contentJSON = body;
                loadData();
            } else {
                binding.linearnothinghere.setVisibility(View.VISIBLE);
            }
        }));
    }

    private void loadData() {
        List<DataRoms> dataSoftware = new ArrayList<>();

        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<DataRoms>>() {}.getType();
            dataSoftware = gson.fromJson(contentJSON, listType);
        } catch (Exception e) {
            binding.linearload.setVisibility(View.GONE);
            binding.linearnothinghere.setVisibility(View.VISIBLE);
        }

        homeSoftwareStoreViewModel.setSoftwareList(dataSoftware);
        data.clear();
        data.addAll(dataSoftware);

        mAdapter.submitList(data);

        SharedData.dataSoftwareStore.clear();
        SharedData.dataSoftwareStore.addAll(dataSoftware);
        softwareStoreCallToHomeListener.updateSearchStatus(true);
    }
}