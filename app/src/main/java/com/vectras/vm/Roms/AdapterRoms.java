package com.vectras.vm.Roms;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.graphics.Color;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.URLUtil;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vectras.vm.AppConfig;
import com.bumptech.glide.Glide;
import com.vectras.vm.RomsManagerActivity;
import com.vectras.vm.MainActivity;
import com.vectras.vm.R;
import com.vectras.vm.utils.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AlertDialog;

import android.content.DialogInterface;
import android.app.Dialog;
import android.widget.Toast;

public class AdapterRoms extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private LayoutInflater inflater;
    static List<DataRoms> data = Collections.emptyList();
    static List<DataRoms> filteredData = Collections.emptyList();
    DataRoms current;
    int currentPos = 0;
    private int mSelectedItem = -1;

    // create constructor to innitilize context and data sent from MainActivity
    public AdapterRoms(Context context, List<DataRoms> data) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        AdapterRoms.data = data;
        AdapterRoms.filteredData = data;
    }

    // Inflate the layout when viewholder created
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.container_roms, parent, false);
        MyHolder holder = new MyHolder(view);
        return holder;
    }

    // Bind data
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

        // Get current position of item in recyclerview to bind data and assign values from list
        final MyHolder myHolder = (MyHolder) holder;
        final DataRoms current = data.get(position);
        Glide.with(RomsManagerActivity.activity).load(current.itemIcon).placeholder(R.drawable.no_machine_image).error(R.drawable.no_machine_image).into(myHolder.ivIcon);
        myHolder.textName.setText(current.itemName + " " + current.itemArch);
        myHolder.textSize.setText(current.itemSize);
        myHolder.checkBox.setChecked(position == mSelectedItem);
        if (current.itemAvail) {
            if (FileUtils.fileValid(RomsManagerActivity.activity, AppConfig.maindirpath + current.itemPath)) {
                myHolder.checkBox.setEnabled(false);
                myHolder.textAvail.setTextColor(Color.BLUE);
                myHolder.textAvail.setText("(installed)");
            } else {
                myHolder.checkBox.setEnabled(true);
                myHolder.textAvail.setTextColor(Color.GREEN);
                myHolder.textAvail.setText("available");
            }
            myHolder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSelectedItem = position;
                    notifyItemRangeChanged(0, data.size());
                    RomsManagerActivity.selected = true;
                    RomsManagerActivity.selectedPath = current.itemPath;
                    RomsManagerActivity.selectedExtra = current.itemExtra;
                    RomsManagerActivity.selectedName = current.itemName + " " + current.itemArch;
                    RomsManagerActivity.selectedLink = current.itemUrl;
                    RomsManagerActivity.selectedIcon = current.itemIcon;
                    myHolder.ivIcon.buildDrawingCache();
                    Bitmap bm = myHolder.ivIcon.getDrawingCache();
                    OutputStream fOut = null;
                    Uri outputFileUri;
                    try {
                        File root = new File(AppConfig.maindirpath + "/icons/");
                        root.mkdirs();
                        File sdImageMainDirectory = new File(root, current.itemPath.replace(".IMG", ".jpg"));
                        outputFileUri = Uri.fromFile(sdImageMainDirectory);
                        fOut = new FileOutputStream(sdImageMainDirectory);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        bm.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                        fOut.flush();
                        fOut.close();
                    } catch (Exception e) {
                    }
                }
            });
        } else {
            myHolder.textAvail.setText("unavailable");
            myHolder.textAvail.setTextColor(Color.RED);
            myHolder.checkBox.setEnabled(false);
        }

    }

    // return total item from List
    @Override
    public int getItemCount() {
        return data.size();
    }

    class MyHolder extends RecyclerView.ViewHolder {

        TextView textName, textAvail, textSize;
        ImageView ivIcon;

        RadioButton checkBox;

        // create constructor to get widget reference
        public MyHolder(View itemView) {
            super(itemView);
            textName = (TextView) itemView.findViewById(R.id.textName);
            ivIcon = (ImageView) itemView.findViewById(R.id.ivIcon);
            textSize = (TextView) itemView.findViewById(R.id.textSize);
            textAvail = (TextView) itemView.findViewById(R.id.textAvail);

            checkBox = (RadioButton) itemView.findViewById(R.id.checkBox);
        }

    }

}
