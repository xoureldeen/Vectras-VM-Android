package com.vectras.vm.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.MainRoms.AdapterMainRoms;
import com.vectras.vm.MainRoms.DataMainRoms;
import com.vectras.vm.R;
import com.vectras.vm.AppConfig;
import com.vectras.vm.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HomeFragment extends Fragment {

    public static View view;
    public static RecyclerView mRVMainRoms;
    public static LinearLayout romsLayout;
    public static AdapterMainRoms mMainAdapter;
    public static JSONArray jArray;
    public static List<DataMainRoms> data;
    private SwipeRefreshLayout refreshRoms;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO Auto-generated method stub

        // TODO show the text view in @layout/home_fragment if list empty

        view = inflater.inflate(R.layout.home_fragment, container, false);

        romsLayout = view.findViewById(R.id.romsLayout);

        refreshRoms = view.findViewById(R.id.refreshRoms);

        refreshRoms.setOnRefreshListener(() -> {
            loadDataVbi();
            mMainAdapter.notifyItemRangeChanged(0, mMainAdapter.data.size());
            refreshRoms.setRefreshing(false);
        });
        loadDataVbi();

        return view;
    }

    private void loadDataVbi() {
        data = new ArrayList<>();

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
                    romsMainData.itemDrv1 = json_data.getString("imgDrv1");
                } catch (JSONException ignored) {
                    romsMainData.itemDrv1 = "";
                }
                romsMainData.itemExtra = json_data.getString("imgExtra");
                try {
                    if (json_data.getString("imgArch").equals(MainSettingsManager.getArch(requireActivity())))
                        data.add(romsMainData);
                } catch (JSONException ignored) {
                    data.add(romsMainData);
                }
            }

            // Setup and Handover data to recyclerview
            mRVMainRoms = HomeFragment.view.findViewById(R.id.mRVMainRoms);
            mMainAdapter = new AdapterMainRoms(requireActivity(), data);
            mRVMainRoms.setAdapter(mMainAdapter);
            mRVMainRoms.setLayoutManager(new GridLayoutManager(getContext(), 2));
        } catch (JSONException e) {
            Toast.makeText(requireActivity(), e.toString(), Toast.LENGTH_LONG).show();
        }
    }
}
