package com.vectras.vm.Blog;

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

public class AdapterBlog extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private Context context;
	private LayoutInflater inflater;
	List<DataBlog> data = Collections.emptyList();
	DataBlog current;
	int currentPos = 0;
	
	// create constructor to innitilize context and data sent from MainActivity
	public AdapterBlog(Context context, List<DataBlog> data) {
		this.context = context;
		inflater = LayoutInflater.from(context);
		this.data = data;
	}

	// Inflate the layout when viewholder created
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = inflater.inflate(R.layout.container_post, parent, false);
		MyHolder holder = new MyHolder(view);
		return holder;
	}

	// Bind data
	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

		// Get current position of item in recyclerview to bind data and assign values from list
		MyHolder myHolder = (MyHolder) holder;
		final DataBlog current = data.get(position);
		myHolder.textTitle.setText(current.postTitle);
		myHolder.textDate.setText("Date: " + current.postDate);
		Glide.with(MainActivity.activity).load(current.postThumb).into(myHolder.ivThumb);
		Animation animation;
		animation = AnimationUtils.loadAnimation(MainActivity.activity, android.R.anim.slide_in_left);
		animation.setDuration(300);

		myHolder.cdPost.startAnimation(animation);
		animation = null;
		myHolder.cdPost.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {

				PostActivity.title = current.postTitle;
				PostActivity.content = current.postContent;
				PostActivity.date = current.postDate;
				PostActivity.thumb = current.postThumb;
				MainActivity.activity.startActivity(new Intent(MainActivity.activity, PostActivity.class));
			}
		});
	}

	// return total item from List
	@Override
	public int getItemCount() {
		return data.size();
	}

	class MyHolder extends RecyclerView.ViewHolder {

		CardView cdPost;
		TextView textTitle;
		ImageView ivThumb;
		TextView textDate;

		// create constructor to get widget reference
		public MyHolder(View itemView) {
			super(itemView);
			cdPost = (CardView) itemView.findViewById(R.id.cdPost);
			textTitle = (TextView) itemView.findViewById(R.id.textTitle);
			ivThumb = (ImageView) itemView.findViewById(R.id.ivThumb);
			textDate = (TextView) itemView.findViewById(R.id.textDate);
		}

	}

}
