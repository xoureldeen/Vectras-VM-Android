package com.vectras.vm.manager;

import android.app.Activity;

import com.vectras.vm.R;
import com.vectras.vm.utils.UniversalPickerDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class VmPicker {
    public boolean isRunningOnly;
    public String currentVmId = "";
    public String title;
    private Activity activity;
    public ArrayList<HashMap<String, Object>> listVm;

    public interface VMPickerCallback {
        void onSelected(int position, String name, String vmId);
    }

    public VmPicker(Activity activity) {
        this.activity = activity;
    }

    public void pick(VMPickerCallback callback) {
        ArrayList<HashMap<String, Object>> list = listVm != null ? listVm : (isRunningOnly ? VmListManager.getAllVmForPickRunningOnly(activity) : VmListManager.getAllVmForPick(activity));

        if (list.isEmpty()) callback.onSelected(-1, "", "");

        int position = -1;

        if (!currentVmId.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                if (Objects.equals(list.get(i).get("value"), currentVmId)) {
                    position = i;
                    break;
                }
            }
        }

        UniversalPickerDialog.show(
                activity,
                list,
                position,
                (callback::onSelected),
                title == null || title.isEmpty() ? activity.getString(R.string.vm_picker) : title);
    }
}
