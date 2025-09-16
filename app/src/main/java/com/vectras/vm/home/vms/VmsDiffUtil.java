package com.vectras.vm.home.vms;

import androidx.recyclerview.widget.DiffUtil;

import com.vectras.vm.MainRoms.DataMainRoms;

import java.util.List;

public class VmsDiffUtil extends DiffUtil.Callback {
    private final List<DataMainRoms> oldList;
    private final List<DataMainRoms> newList;

    public VmsDiffUtil(List<DataMainRoms> oldList, List<DataMainRoms> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        // So sánh bằng khóa duy nhất (vd: vmID)
        return oldList.get(oldItemPosition).vmID.equals(newList.get(newItemPosition).vmID);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // So sánh nội dung, nếu khác thì update item đó
        DataMainRoms oldItem = oldList.get(oldItemPosition);
        DataMainRoms newItem = newList.get(newItemPosition);
        return oldItem.equals(newItem); // Nếu bạn override equals trong DataMainRoms thì dùng cách này
    }
}

