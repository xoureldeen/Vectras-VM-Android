package com.vectras.vm.main.vms;

import androidx.recyclerview.widget.DiffUtil;

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
        // Compare using a unique key (e.g., vmID)
        return oldList.get(oldItemPosition).vmID.equals(newList.get(newItemPosition).vmID);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // Compare the content, if it's different, update that item.
        DataMainRoms oldItem = oldList.get(oldItemPosition);
        DataMainRoms newItem = newList.get(newItemPosition);
        return oldItem.equals(newItem); // If you override equals in DataMainRoms, use this method.
    }
}

