package com.epicstudios.vectras.Roms;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.epicstudios.vectras.Config;
import com.bumptech.glide.Glide;
import com.epicstudios.vectras.FirstActivity;
import com.epicstudios.vectras.MainActivity;
import com.epicstudios.vectras.R;
import com.epicstudios.vectras.utils.FileUtils;
import java.util.Collections;
import java.util.List;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.app.Dialog;

public class AdapterRoms extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private Context context;
	private LayoutInflater inflater;
	List<DataRoms> data = Collections.emptyList();
	DataRoms current;
	int currentPos = 0;
	private int mSelectedItem = -1;

	// create constructor to innitilize context and data sent from MainActivity
	public AdapterRoms(Context context, List<DataRoms> data) {
		this.context = context;
		inflater = LayoutInflater.from(context);
		this.data = data;
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

		Glide.with(FirstActivity.activity).load(current.itemIcon).into(myHolder.ivIcon);
		myHolder.textName.setText(current.itemName + " " + current.itemArch);
		myHolder.textSize.setText(current.itemSize);
		myHolder.checkBox.setChecked(position == mSelectedItem);
		if (current.itemAvail) {
			myHolder.textAvail.setText("availability: available");
			myHolder.textAvail.setTextColor(Color.GREEN);
		} else if (!current.itemAvail) {
			myHolder.textAvail.setText("availability: unavailable");
			myHolder.textAvail.setTextColor(Color.RED);
			myHolder.checkBox.setEnabled(false);
			myHolder.cdItem.setEnabled(false);
		}
		if (current.itemAvail)
			myHolder.checkBox.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mSelectedItem = position;
						notifyItemRangeChanged(0, data.size());
						FirstActivity.selected = true;
						FirstActivity.selectedPath = current.itemPath;
						FirstActivity.selectedExtra = current.itemExtra;
						FirstActivity.selectedName = current.itemName+" "+current.itemArch;
						FirstActivity.selectedLink = current.itemUrl;
						FirstActivity.selectedIcon = current.itemIcon;
				}
			});
		//Glide.with(MainActivity.activity).load(current.itemIcon).into(myHolder.ivIcon);
		if (current.itemAvail)
			myHolder.cdItem.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (FileUtils.fileValid(FirstActivity.activity, Config.maindirpath + current.itemPath)) {
						mSelectedItem = position;
						notifyItemRangeChanged(0, data.size());
						FirstActivity.selected = true;
						FirstActivity.selectedPath = current.itemPath;
						FirstActivity.selectedExtra = current.itemExtra;
					} else {
						AlertDialog ad;
						ad = new AlertDialog.Builder(FirstActivity.activity, R.style.MainDialogTheme).create();
						ad.setTitle(current.itemName + " Not found");
						ad.setMessage(current.itemName + " Rom not found  please download from our official website");
						ad.setButton(Dialog.BUTTON_POSITIVE, "DOWNLAOD WEBSITE", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								String gt = current.itemUrl;
								Intent g = new Intent(Intent.ACTION_VIEW);
								g.setData(Uri.parse(gt));
								FirstActivity.activity.startActivity(g);
								FirstActivity.activity.finish();
							}
						});
					}
				}
			});

	}

	// return total item from List
	@Override
	public int getItemCount() {
		return data.size();
	}

	class MyHolder extends RecyclerView.ViewHolder {

		CardView cdItem;
		TextView textName, textAvail, textSize;
		ImageView ivIcon;

		RadioButton checkBox;

		// create constructor to get widget reference
		public MyHolder(View itemView) {
			super(itemView);
			cdItem = (CardView) itemView.findViewById(R.id.cdItem);
			textName = (TextView) itemView.findViewById(R.id.textName);
			ivIcon = (ImageView) itemView.findViewById(R.id.ivIcon);
			textSize = (TextView) itemView.findViewById(R.id.textSize);
			textAvail = (TextView) itemView.findViewById(R.id.textAvail);

			checkBox = (RadioButton) itemView.findViewById(R.id.checkBox);
		}

	}

}
