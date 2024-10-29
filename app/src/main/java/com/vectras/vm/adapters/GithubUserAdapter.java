package com.vectras.vm.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.vectras.vm.R;
import com.vectras.vm.view.GithubUserView;

public class GithubUserAdapter extends RecyclerView.Adapter<GithubUserAdapter.ViewHolder> {

    private final String[] usernames;
    private final Context context;

    public GithubUserAdapter(Context context, String[] usernames) {
        this.context = context;
        this.usernames = usernames;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.github_user_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String username = usernames[position];
        holder.githubUserView.setUsername(username);
    }

    @Override
    public int getItemCount() {
        return usernames.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        GithubUserView githubUserView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            githubUserView = itemView.findViewById(R.id.github_user_view);
        }
    }
}