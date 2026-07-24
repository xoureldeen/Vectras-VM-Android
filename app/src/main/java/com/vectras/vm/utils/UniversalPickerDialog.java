package com.vectras.vm.utils;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.vectras.vm.R;
import com.vectras.vm.databinding.DialogListSelectorLayoutBinding;
import com.vectras.vm.databinding.SimpleLayoutListViewWithCheckBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class UniversalPickerDialog {
    public interface UniversalPickerDialogCallback {
        void onSelected(int position, String name, String value);
    }

    public static void show(Activity activity, ArrayList<HashMap<String, Object>> list, int position, UniversalPickerDialogCallback callback, String title) {
        show(activity, list, position, callback, title, true);
    }

    public static void show(Activity activity, ArrayList<HashMap<String, Object>> list, int position, UniversalPickerDialogCallback callback, String title, boolean markSelected) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        LinearLayoutManager layoutmanager = new LinearLayoutManager(activity);
        DialogListSelectorLayoutBinding binding = DialogListSelectorLayoutBinding.inflate(activity.getLayoutInflater());

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(binding.getRoot())
                .create();

        binding.tvTitle.setText(title);
        binding.btnClose.setOnClickListener(v -> dialog.dismiss());

        binding.list.setAdapter(new RecyclerviewAdapter(activity, dialog, list, position, callback, markSelected));
        binding.list.setLayoutManager(layoutmanager);

        if (activity.isFinishing() || activity.isDestroyed()) return;
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
        boolean markSelected;
        AlertDialog dialog;
        UniversalPickerDialogCallback callback;

        public RecyclerviewAdapter(Activity activity, AlertDialog alertDialog, ArrayList<HashMap<String, Object>> arr, int position, UniversalPickerDialogCallback callback, boolean markSelected) {
            this.activity = activity;
            data = arr;
            currentPosition = position;
            dialog = alertDialog;
            this.callback = callback;
            this.markSelected = markSelected;
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
            ImageView check = view.findViewById(R.id.iv_check);
            title.setText(Objects.requireNonNull(data.get(position).get("name")).toString());
            title.setTextColor(MaterialColors.getColor(title, markSelected && position == currentPosition ? androidx.appcompat.R.attr.colorPrimary : com.google.android.material.R.attr.colorOnSurface));
            view.setBackgroundResource(markSelected && position == currentPosition ? R.drawable.dialog_shape_single_button : R.drawable.dialog_shape_click_effect_button);
            check.setVisibility(markSelected && position == currentPosition ? View.VISIBLE : View.INVISIBLE);
            view.findViewById(R.id.main).setOnClickListener(v -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
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

    public static void putToList
            (
                    ArrayList<HashMap<String, Object>> listMap,
                    String name,
                    String value
            ) {
        HashMap<String, Object> thisItem = new HashMap<>();
        thisItem.put("name", name);
        thisItem.put("value", value);
        listMap.add(thisItem);
    }

    public static void putToList
            (
                    ArrayList<HashMap<String, Object>> listMap,
                    int value
            ) {
        HashMap<String, Object> thisItem = new HashMap<>();
        thisItem.put("name", value);
        thisItem.put("value", value);
        listMap.add(thisItem);
    }
}
