package com.vectras.vm.main.softwarestore;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vectras.vm.R;
import com.vectras.vm.RomInfo;
import com.vectras.vm.main.romstore.DataRoms;
import com.vectras.vm.utils.UIUtils;

import java.util.List;

public class SoftwareStoreHomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context context;
    private final LayoutInflater inflater;
    private final List<DataRoms> dataRom;
    private final boolean isBrighterItemBackground;

    public SoftwareStoreHomeAdapter(Context context, List<DataRoms> data, boolean isBrighterItemBackground) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        dataRom = data;
        this.isBrighterItemBackground = isBrighterItemBackground;
    }

    // Inflate the layout when viewholder created
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.container_roms, parent, false);
        return new MyHolder(view);
    }

    // Bind data
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {

        // Get current position of item in recyclerview to bind data and assign values from list
        final MyHolder myHolder = (MyHolder) holder;
        final DataRoms current = dataRom.get(position);
        Glide.with(context).load(current.romIcon).override(180, 180).placeholder(R.drawable.ic_computer_180dp_with_padding).error(R.drawable.ic_computer_180dp_with_padding).into(myHolder.ivIcon);
        myHolder.textName.setText(current.romName);
        myHolder.textSize.setText(current.romSize);
        if (current.romAvail) {
            myHolder.linearItem.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.setClass(context, RomInfo.class);
                intent.putExtra("title", current.romName);
                intent.putExtra("shortdesc", current.romSize);
                intent.putExtra("getrom", current.romUrl);
                intent.putExtra("desc", current.desc);
                intent.putExtra("icon", current.romIcon);
                intent.putExtra("filename", current.romPath);
                intent.putExtra("finalromfilename", current.finalromfilename);
                intent.putExtra("extra", current.romExtra);
                intent.putExtra("arch", current.romArch);
                intent.putExtra("verified", current.verified);
                intent.putExtra("creator", current.creator);
                intent.putExtra("size", current.fileSize);
                intent.putExtra("id", current.id);
                intent.putExtra("vecid", current.vecid);
                intent.putExtra("isRomInfo", false);
                context.startActivity(intent);
            });
        } else {
            myHolder.textAvail.setText(context.getString(R.string.unavailable));
            myHolder.textAvail.setTextColor(Color.RED);
        }

        UIUtils.setBackgroundItemInList(myHolder.linearItem, position, dataRom.size(), isBrighterItemBackground);
    }

    // return total item from List
    @Override
    public int getItemCount() {
        return dataRom == null ? 0 : dataRom.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder {

        TextView textName, textAvail, textSize;
        ImageView ivIcon;
        LinearLayout linearItem;

        // create constructor to get widget reference
        public MyHolder(View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            textSize = itemView.findViewById(R.id.textSize);
            textAvail = itemView.findViewById(R.id.textAvail);

            linearItem = itemView.findViewById(R.id.linearItem);
        }

    }
}
