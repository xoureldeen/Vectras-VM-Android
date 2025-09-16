package com.vectras.vm.home.vms;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.transition.MaterialFadeThrough;
import com.vectras.vm.AppConfig;
import com.vectras.vm.MainRoms.DataMainRoms;
import com.vectras.vm.R;
import com.vectras.vm.VMManager;
import com.vectras.vm.databinding.FragmentHomeVmsBinding;
import com.vectras.vm.home.HomeActivity;
import com.vectras.vm.home.core.CallbackInterface;
import com.vectras.vm.utils.DeviceUtils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.PermissionUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VmsFragment extends Fragment implements CallbackInterface.HomeCallToVmsListener {
    private final String TAG = "VmsFragment";
    FragmentHomeVmsBinding binding;
    private JSONArray jArray;
    private final List<DataMainRoms> data = new ArrayList<>();
    private VmsHomeAdapter vmsHomeAdapter;
    private int spanCount = 0;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    public static VmsCallToHomeListener vmsCallToHomeListener;
    public interface VmsCallToHomeListener {
        void openRomStore();
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
    public void onResume() {
        super.onResume();
        checkAndLoad();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentHomeVmsBinding.inflate(inflater, container, false);
        spanCount = requireActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 3 : 2;
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        HomeActivity.homeCallToVmsListener = this;

        binding.rvRomlist.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        vmsHomeAdapter = new VmsHomeAdapter(requireActivity(), data);
        binding.rvRomlist.setAdapter(vmsHomeAdapter);

        binding.bnRomstore.setOnClickListener(v -> {
            vmsCallToHomeListener.openRomStore();
        });

        binding.bnRepair.setOnClickListener(V -> {
            VMManager.startFixRomsDataJson();
            VMManager.fixRomsDataJsonResult(requireActivity());
        });

        binding.wrlRomlist.setOnRefreshListener(() -> {
            checkAndLoad();
            binding.wrlRomlist.setRefreshing(false);
        });

        checkAndLoad();
    }

    private void loadDataVbi() {

        if (!FileUtils.isFileExists(AppConfig.romsdatajson))
            FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");

        executor.execute(() -> {
            List<DataMainRoms> tempdata = new ArrayList<>();

            try {
                jArray = new JSONArray(FileUtils.readFromFile(requireActivity(), new File(AppConfig.maindirpath
                        + "roms-data.json")));

                // Extract data from json and store into ArrayList as class objects
                for (int i = 0; i < jArray.length(); i++) {
                    JSONObject json_data = jArray.getJSONObject(i);
                    DataMainRoms romsMainData = new DataMainRoms();
                    romsMainData.itemName = json_data.getString("imgName");
                    romsMainData.itemIcon = json_data.getString("imgIcon");
                    try {
                        romsMainData.itemArch = json_data.getString("imgArch");
                    } catch (JSONException ignored) {
                        romsMainData.itemArch = "unknown";
                    }
                    romsMainData.itemPath = json_data.getString("imgPath");
                    try {
                        romsMainData.imgCdrom = json_data.getString("imgCdrom");
                    } catch (JSONException ignored) {
                        romsMainData.imgCdrom = "";
                    }
                    try {
                        romsMainData.vmID = json_data.getString("vmID");
                    } catch (JSONException ignored) {
                        romsMainData.vmID = "";
                    }
                    try {
                        romsMainData.qmpPort = json_data.getInt("qmpPort");
                    } catch (JSONException ignored) {
                        romsMainData.qmpPort = 0;
                    }
                    try {
                        romsMainData.itemDrv1 = json_data.getString("imgDrv1");
                    } catch (JSONException ignored) {
                        romsMainData.itemDrv1 = "";
                    }
                    romsMainData.itemExtra = json_data.getString("imgExtra");
                    tempdata.add(romsMainData);
                }
                requireActivity().runOnUiThread(() -> binding.lnError.setVisibility(View.GONE));
            } catch (JSONException e) {
                requireActivity().runOnUiThread(() -> binding.lnError.setVisibility(View.VISIBLE));
                Log.e(TAG, "loadDataVbi: ", e);
            }

            requireActivity().runOnUiThread(() -> {
                binding.lnLoad.setVisibility(View.GONE);
                if (tempdata.isEmpty()) {
                    binding.lnNothinghere.setVisibility(View.VISIBLE);
                } else {
                    binding.lnNothinghere.setVisibility(View.GONE);
                    vmsHomeAdapter.updateData(tempdata);
                }
            });
        });
    }

    private void checkAndLoad() {
        if (PermissionUtils.storagepermission(requireActivity(), true)) {
            loadDataVbi();
            if (DeviceUtils.isStorageLow(requireActivity())) {
                DialogUtils.oneDialog(requireActivity(),
                        getResources().getString(R.string.oops),
                        getResources().getString(R.string.very_low_available_storage_space_content),
                        getResources().getString(R.string.ok),
                        true,
                        R.drawable.warning_48px,
                        true,
                        null,
                        () -> {
                            if (DeviceUtils.isStorageLow(requireActivity()))
                                requireActivity().finish();
                        });
            }
        }
    }

    @Override
    public void refeshVMList() {
        requireActivity().runOnUiThread(this::checkAndLoad);
    }

    @Override
    public void configurationChanged(boolean isLandscape) {
        spanCount = isLandscape ? 3 : 2;
        binding.rvRomlist.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
    }
}