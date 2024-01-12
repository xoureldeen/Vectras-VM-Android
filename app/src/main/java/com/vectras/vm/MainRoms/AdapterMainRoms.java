package com.vectras.vm.MainRoms;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vectras.qemu.Config;
import com.vectras.vm.AppConfig;
import com.vectras.vm.Fragment.HomeFragment;
import com.vectras.vm.MainActivity;
import com.vectras.vm.R;
import com.vectras.vm.utils.UIUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

public class AdapterMainRoms extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private LayoutInflater inflater;
    public List<DataMainRoms> data = Collections.emptyList();
    int currentPos = 0;
    private int mSelectedItem = -1;

    // create constructor to innitilize context and data sent from MainActivity
    public AdapterMainRoms(Context context, List<DataMainRoms> data) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.data = data;
    }

    // Inflate the layout when viewholder created
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.container_main_roms, parent, false);
        MyHolder holder = new MyHolder(view);
        return holder;
    }

    // Bind data
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

        // Get current position of item in recyclerview to bind data and assign values from list
        final MyHolder myHolder = (MyHolder) holder;
        final DataMainRoms current = data.get(position);
        myHolder.textName.setText(current.itemName);
        Bitmap bmImg = BitmapFactory.decodeFile(current.itemIcon);
        myHolder.ivIcon.setImageBitmap(bmImg);

        myHolder.cdRoms.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                MainActivity.setupNativeLibs();
                Config.hda_path = current.itemPath;
                Config.extra_params = current.itemExtra;
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        if (!Config.loadNativeLibsEarly && !Config.loadNativeLibsMainThread) {
                            MainActivity.setupNativeLibs();
                        }
                        MainActivity.onStartButton();
                    }
                });
                thread.setPriority(Thread.MIN_PRIORITY);
                thread.start();

            }
        });
        myHolder.cdRoms.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showDialog(current.itemName, current.itemPath, current.itemIcon);
                return false;
            }
        });
    }

    private void showDialog(String title, String path, String pathIcon) {

        final Dialog dialog = new Dialog(MainActivity.activity, R.style.MainDialogTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.rom_options_layout);

        LinearLayout removeLayout = dialog.findViewById(R.id.layoutRemove);

        removeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog ad;
                ad = new AlertDialog.Builder(MainActivity.activity, R.style.MainDialogTheme).create();
                ad.setTitle("Remove " + title);
                ad.setMessage("Are you sure?");
                ad.setButton(Dialog.BUTTON_NEGATIVE, "REMOVE " + title, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        File file = new File(path);
                        file.delete();
                        File fileIcon = new File(pathIcon);
                        fileIcon.delete();

                        HomeFragment.mMainAdapter = new AdapterMainRoms(MainActivity.activity, HomeFragment.data);
                        HomeFragment.data.remove(currentPos);
                        HomeFragment.mRVMainRoms.setAdapter(HomeFragment.mMainAdapter);
                        HomeFragment.mRVMainRoms.setLayoutManager(new GridLayoutManager(MainActivity.activity, 2));
                        HomeFragment.jArray.remove(currentPos);
                        try {
                            Writer output = null;
                            File jsonFile = new File(AppConfig.maindirpath + "roms-data" + ".json");
                            output = new BufferedWriter(new FileWriter(jsonFile));
                            output.write(HomeFragment.jArray.toString().replace("\\", "").replace("//", "/"));
                            output.close();
                        } catch (Exception e) {
                            UIUtils.toastLong(MainActivity.activity, e.toString());
                        }
                        UIUtils.toastLong(MainActivity.activity, title + " are removed successfully!");
                        return;
                    }
                });
                ad.setButton(Dialog.BUTTON_POSITIVE, "CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });
                ad.show();
                dialog.dismiss();
            }
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);

    }

    // return total item from List
    @Override
    public int getItemCount() {
        return data.size();
    }

    class MyHolder extends RecyclerView.ViewHolder {

        CardView cdRoms;
        TextView textName;
        ImageView ivIcon;

        // create constructor to get widget reference
        public MyHolder(View itemView) {
            super(itemView);
            cdRoms = (CardView) itemView.findViewById(R.id.cdItem);
            textName = (TextView) itemView.findViewById(R.id.textName);
            ivIcon = (ImageView) itemView.findViewById(R.id.ivIcon);
        }

    }

}
