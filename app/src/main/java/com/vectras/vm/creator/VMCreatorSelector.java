package com.vectras.vm.creator;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vectras.vm.R;
import com.vectras.vm.databinding.DialogListSelectorLayoutBinding;
import com.vectras.vm.databinding.SimpleLayoutListViewWithCheckBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class VMCreatorSelector {
    private static final String TAG = "VMCreatorSelector";

    public interface SelectorCallback {
        void onSelected(int position, String name, String value);
    }

    public static HashMap<String, Object> getBootFrom(Context context, int position) {
        return ListManager.bootFrom(context).get(position);
    }

    public static void bootFrom(Activity activity, int position, SelectorCallback callback) {
        showDialog(activity, ListManager.bootFrom(activity), position, callback, activity.getString(R.string.boot_from));
    }

    public static void showDialog(Activity activity, ArrayList<HashMap<String, Object>> list, int position,SelectorCallback callback, String title) {
        LinearLayoutManager layoutmanager = new LinearLayoutManager(activity);
        DialogListSelectorLayoutBinding binding = DialogListSelectorLayoutBinding.inflate(activity.getLayoutInflater());

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(binding.getRoot())
                .create();

        binding.tvTitle.setText(title);
        binding.btnClose.setOnClickListener(v -> dialog.dismiss());

        binding.list.setAdapter(new RecyclerviewAdapter(activity, dialog, list, position, callback));
        binding.list.setLayoutManager(layoutmanager);

        dialog.show();

        binding.list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                boolean canScrollUp = rv.canScrollVertically(-1);
                boolean canScrollDown = rv.canScrollVertically(1);

                binding.dvTop.setVisibility(canScrollUp ? View.VISIBLE : View.INVISIBLE);
                binding.dvBottom.setVisibility(canScrollDown ? View.VISIBLE : View.INVISIBLE);
            }
        });

        if (position > -1) binding.list.scrollToPosition(position);
    }

    private static class RecyclerviewAdapter extends RecyclerView.Adapter<RecyclerviewAdapter.ViewHolder> {

        Activity activity;
        ArrayList<HashMap<String, Object>> data;
        int currentPosition;
        AlertDialog dialog;
        SelectorCallback callback;

        public RecyclerviewAdapter(Activity activity, AlertDialog alertDialog, ArrayList<HashMap<String, Object>> arr, int position, SelectorCallback callback) {
            this.activity = activity;
            data = arr;
            currentPosition = position > -1 ? position : 0;
            dialog = alertDialog;
            this.callback = callback;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = activity.getLayoutInflater();
            SimpleLayoutListViewWithCheckBinding simpleLayoutListViewWithCheckBinding =
                    SimpleLayoutListViewWithCheckBinding.inflate(inflater, parent, false);
            return new ViewHolder(simpleLayoutListViewWithCheckBinding.getRoot());
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            View view = holder.itemView;
            TextView title = view.findViewById(R.id.textview);
            title.setText(Objects.requireNonNull(data.get(position).get("name")).toString());
            view.findViewById(R.id.iv_check).setVisibility(position == currentPosition ? View.VISIBLE : View.INVISIBLE);
            view.findViewById(R.id.main).setOnClickListener(v -> {
                callback.onSelected(
                        position,
                        Objects.requireNonNull(data.get(position).get("name")).toString(),
                        Objects.requireNonNull(data.get(position).get("value")).toString()
                );
                dialog.dismiss();
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View v) {
                super(v);
            }
        }
    }
}
