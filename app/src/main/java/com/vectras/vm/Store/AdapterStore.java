package com.vectras.vm.Store;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.vectras.vm.R;
import com.vectras.vm.Fragment.HomeFragment;
import com.vectras.vm.PostActivity;
import java.util.Collections;
import java.util.List;
import com.vectras.vm.MainActivity;
import com.vectras.vm.StoreActivity;
import com.vectras.vm.StoreItemActivity;

public class AdapterStore extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private Context context;
	private LayoutInflater inflater;
	List<DataStore> data = Collections.emptyList();
	DataStore current;
	int currentPos = 0;
	
	// create constructor to innitilize context and data sent from MainActivity
	public AdapterStore(Context context, List<DataStore> data) {
		this.context = context;
		inflater = LayoutInflater.from(context);
		this.data = data;
	}

	// Inflate the layout when viewholder created
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = inflater.inflate(R.layout.container_store, parent, false);
		MyHolder holder = new MyHolder(view);
		return holder;
	}

	// Bind data
	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

		// Get current position of item in recyclerview to bind data and assign values from list
		MyHolder myHolder = (MyHolder) holder;
		final DataStore current = data.get(position);
		myHolder.textName.setText(current.itemName);
		myHolder.textSize.setText("Size: " + current.itemSize);
		Glide.with(MainActivity.activity).load(current.itemIcon).into(myHolder.ivIcon);
		Animation animation;
		animation = AnimationUtils.loadAnimation(MainActivity.activity, android.R.anim.slide_in_left);
		animation.setDuration(300);

		myHolder.cdItem.startAnimation(animation);
		animation = null;
		myHolder.cdItem.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				StoreItemActivity.name = current.itemName;
				StoreItemActivity.icon = current.itemIcon;
				StoreItemActivity.size = current.itemSize;
				StoreItemActivity.desc = current.itemData;
				StoreItemActivity.link = current.itemLink;
				StoreItemActivity.prvMain = current.itemPreviewMain;
				StoreItemActivity.prv1 = current.itemPreview1;
				StoreItemActivity.prv2 = current.itemPreview2;
				StoreActivity.activity.startActivity(new Intent(StoreActivity.activity, StoreItemActivity.class));
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
		TextView textName;
		ImageView ivIcon;
		TextView textSize;

		// create constructor to get widget reference
		public MyHolder(View itemView) {
			super(itemView);
			cdItem = (CardView) itemView.findViewById(R.id.cdItem);
			textName = (TextView) itemView.findViewById(R.id.textName);
			ivIcon = (ImageView) itemView.findViewById(R.id.ivIcon);
			textSize = (TextView) itemView.findViewById(R.id.textSize);
		}

	}

}
