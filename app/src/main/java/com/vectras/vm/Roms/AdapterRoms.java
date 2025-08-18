package com.vectras.vm.Roms;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.vectras.vm.AppConfig;
import com.vectras.vm.RomInfo;
import com.vectras.vm.RomsManagerActivity;
import com.vectras.vm.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AdapterRoms extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final LayoutInflater inflater;
    static List<DataRoms> dataRom = Collections.emptyList();
    private final String TAG = "AdapterRoms";

    // create constructor to innitilize context and data sent from MainActivity
    public AdapterRoms(Context context, List<DataRoms> data) {
        inflater = LayoutInflater.from(context);
        dataRom = data;
    }

    // Inflate the layout when viewholder created
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.container_roms, parent, false);
        return new MyHolder(view);
    }

    // Bind data
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {

        // Get current position of item in recyclerview to bind data and assign values from list
        final MyHolder myHolder = (MyHolder) holder;
        final DataRoms current = dataRom.get(position);
        Glide.with(RomsManagerActivity.activity).load(current.romIcon).placeholder(R.drawable.ic_computer_180dp_with_padding).error(R.drawable.ic_computer_180dp_with_padding).into(myHolder.ivIcon);
        myHolder.textName.setText(current.romName);
        myHolder.textSize.setText(current.romArch + " - " + current.romSize);
        if (current.romAvail) {
            myHolder.linearItem.setOnClickListener(v -> {
                notifyItemRangeChanged(0, dataRom.size());

                Intent intent = new Intent();
                intent.setClass(RomsManagerActivity.activity, RomInfo.class);
                intent.putExtra("title", current.romName);
                intent.putExtra("shortdesc", current.romArch + " - " + current.romSize);
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
                RomsManagerActivity.activity.startActivity(intent);

                if (!current.romPath.endsWith(".cvbi")) {
                    //Save image to icon folder
                    myHolder.ivIcon.buildDrawingCache();
                    Bitmap bm = myHolder.ivIcon.getDrawingCache();
                    OutputStream fOut = null;
                    try {
                        File root = new File(AppConfig.maindirpath + "/icons/");
                        if(root.mkdirs()) {
                            File sdImageMainDirectory = new File(root, current.romPath + ".png");
                            fOut = new FileOutputStream(sdImageMainDirectory);
                        } else {
                            Log.e(TAG, "Directory: " + AppConfig.maindirpath + "/icons/");
                        }
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "File: " + Objects.requireNonNull(e.getMessage()));
                    }

                    try {
                        assert fOut != null;
                        bm.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                        fOut.flush();
                        fOut.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Bitmap: " + Objects.requireNonNull(e.getMessage()));
                    }
                }
            });
        } else {
            myHolder.textAvail.setText(RomsManagerActivity.sUnavailable);
            myHolder.textAvail.setTextColor(Color.RED);
        }

        if (position == 0) {
            myHolder.linearItem.setBackground(AppCompatResources.getDrawable(RomsManagerActivity.activity, R.drawable.object_shape_top));
        } else if (position == dataRom.size() - 1) {
            myHolder.linearItem.setBackground(AppCompatResources.getDrawable(RomsManagerActivity.activity, R.drawable.object_shape_bottom));
        } else {
            myHolder.linearItem.setBackground(AppCompatResources.getDrawable(RomsManagerActivity.activity, R.drawable.object_shape_middle));
        }
    }

    // return total item from List
    @Override
    public int getItemCount() {
        return dataRom.size();
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
