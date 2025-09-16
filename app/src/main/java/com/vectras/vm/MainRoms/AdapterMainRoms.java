package com.vectras.vm.MainRoms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.vectras.qemu.Config;
import com.vectras.vm.CustomRomActivity;
import com.vectras.vm.ExportRomActivity;
import com.vectras.vm.MainActivity;
import com.vectras.vm.R;
import com.vectras.vm.StartVM;
import com.vectras.vm.VMManager;
import java.util.Collections;
import java.util.List;

public class AdapterMainRoms extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Activity activity;
    private final Context context;
    private final LayoutInflater inflater;
    public List<DataMainRoms> data = Collections.emptyList();
    int currentPos = 0;

    public AdapterMainRoms(Activity activity, List<DataMainRoms> data) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        inflater = LayoutInflater.from(context);
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
        myHolder.optionsBtn.setOnClickListener(view -> {

            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
            View v = activity.getLayoutInflater().inflate(R.layout.rom_options_dialog, null);
            bottomSheetDialog.setContentView(v);

            Button modifyRomBtn = v.findViewById(R.id.modifyRomBtn);
            modifyRomBtn.setOnClickListener(v3 -> {
                CustomRomActivity.current = data.get(position);
                VMManager.setArch(current.itemArch, activity);
                context.startActivity(new Intent(context, CustomRomActivity.class).putExtra("POS", position).putExtra("MODIFY", true).putExtra("VMID", current.vmID));
                bottomSheetDialog.cancel();
            });

            Button exportRomBtn = v.findViewById(R.id.exportRomBtn);
            exportRomBtn.setOnClickListener(v2 -> {
                ExportRomActivity.pendingPosition = position;
                Intent intent = new Intent();
                intent.setClass(context.getApplicationContext(), ExportRomActivity.class);
                context.startActivity(intent);
                bottomSheetDialog.cancel();
            });

            Button removeRomBtn = v.findViewById(R.id.removeRomBtn);
            removeRomBtn.setOnClickListener(v1 -> {
                VMManager.deleteVMDialog(current.itemName, position, activity);
                bottomSheetDialog.cancel();
            });
            bottomSheetDialog.show();
        });

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
            MainActivity.startVM(current.itemName, env, current.itemExtra, current.itemPath);
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

}
