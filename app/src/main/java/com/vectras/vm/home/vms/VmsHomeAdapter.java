package com.vectras.vm.home.vms;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.vectras.qemu.Config;
import com.vectras.vm.MainRoms.DataMainRoms;
import com.vectras.vm.R;
import com.vectras.vm.StartVM;
import com.vectras.vm.VMManager;
import com.vectras.vm.home.core.HomeStartVM;
import com.vectras.vm.home.core.RomOptionsDialog;

import java.util.Collections;
import java.util.List;

public class VmsHomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private final Activity activity;
    private final LayoutInflater inflater;
    public List<DataMainRoms> data = Collections.emptyList();

    public VmsHomeAdapter(Activity activity, List<DataMainRoms> data) {
        this.activity = activity;
        inflater = LayoutInflater.from(activity);
        this.data = data;
    }

    // Inflate the layout when viewholder created
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.container_main_roms, parent, false);
        return new MyHolder(view);
    }

    // Bind data
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int _position) {

        int position = holder.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) return;

        // Get current position of item in recyclerview to bind data and assign values from list
        final MyHolder myHolder = (MyHolder) holder;
        final DataMainRoms current = data.get(position);
        myHolder.textName.setText(current.itemName);
        myHolder.textArch.setText(current.itemArch);
        if (current.itemIcon.isEmpty()){
            VMManager.setIconWithName(myHolder.ivIcon, current.itemName);
        } else {
            Bitmap bmImg = BitmapFactory.decodeFile(current.itemIcon);
            myHolder.ivIcon.setImageBitmap(bmImg);
        }
        myHolder.optionsBtn.setOnClickListener(view -> RomOptionsDialog.showNow(activity, data, position, current.vmID, current.itemName, current.itemArch));

        myHolder.cdRoms.setOnClickListener(view -> {
            VMManager.setArch(current.itemArch, activity);
            StartVM.cdrompath = current.imgCdrom;
            if (current.qmpPort == 0) {
                Config.setDefault();
            } else {
                Config.QMPPort = current.qmpPort;
            }
            Config.vmID = current.vmID;
            String env = StartVM.env(activity, current.itemExtra, current.itemPath, "");
            HomeStartVM.startNow(activity, current.itemName, env, current.itemExtra, current.itemPath, null, null);
        });

        myHolder.cdRoms.setOnLongClickListener(v -> {
            VMManager.deleteVMDialog(current.itemName, position, activity);
            return false;
        });
    }

    // return total item from List
    @Override
    public int getItemCount() {
        return data.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder {

        CardView cdRoms;
        TextView textName, textArch;
        ImageView ivIcon;
        ImageButton optionsBtn;

        // create constructor to get widget reference
        public MyHolder(View itemView) {
            super(itemView);
            cdRoms = itemView.findViewById(R.id.cdItem);
            textName = itemView.findViewById(R.id.textName);
            textArch = itemView.findViewById(R.id.textArch);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            optionsBtn = itemView.findViewById(R.id.optionsButton);
        }

    }

    public void updateData(List<DataMainRoms> newData) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new VmsDiffUtil(this.data, newData));
        this.data.clear();
        this.data.addAll(newData);
        diffResult.dispatchUpdatesTo(this);
    }

}
