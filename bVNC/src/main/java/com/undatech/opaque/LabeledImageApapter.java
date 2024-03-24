/**
 * Copyright (C) 2013- Iordan Iordanov
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */


package com.undatech.opaque;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;

import com.iiordanov.bVNC.Constants;
import com.iiordanov.bVNC.Utils;
import com.undatech.remoteClientUi.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LabeledImageApapter extends BaseAdapter {
    private static final String TAG = "LabeledImageApapter";
    List<Connection> filteredConnectionsByPosition = new ArrayList<>();
    String[] filter = null;
    private Context context;
    private int numCols = 2;
    private String defaultLabel = "Untitled";
    private boolean doNotShowDesktopThumbnails = false;

    public LabeledImageApapter(Context context, Map<String, Connection> connectionsByPosition, String[] filter, int maxNumCols) {
        this.context = context;
        this.numCols = maxNumCols;
        this.filter = filter;
        if (connectionsByPosition != null) {
            for (Connection c : connectionsByPosition.values()) {
                boolean include = true;
                for (String item : this.filter) {
                    if (!c.getLabel().toLowerCase().contains(item)) {
                        include = false;
                    }
                }
                if (include) {
                    this.filteredConnectionsByPosition.add(c);
                }
            }
        }
        doNotShowDesktopThumbnails = Utils.querySharedPreferenceBoolean(context, Constants.doNotShowDesktopThumbnails);
        if (doNotShowDesktopThumbnails) {
            numCols = 1;
        }
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Connection c = filteredConnectionsByPosition.get(position);

        String label = c.getLabel();
        android.util.Log.d(TAG, "Now setting label at position: " + position + " to: " + label);

        GridView gView = (GridView) ((Activity) context).findViewById(R.id.gridView);
        int height = gView.getWidth() / numCols;

        View gridView;
        if (convertView != null) {
            gridView = convertView;
        } else {
            gridView = inflater.inflate(R.layout.grid_item, null);
        }

        ViewGroup.LayoutParams lp = gridView.getLayoutParams();
        if (lp != null) {
            lp.height = height;
            lp.width = height;
        } else {
            lp = new GridView.LayoutParams(height, height);
        }

        TextView textView = (TextView) gridView.findViewById(R.id.grid_item_text);
        if ("".equals(label)) {
            textView.setText(defaultLabel);
        } else {
            textView.setText(label);
        }
        String screenshotFilePath = context.getFilesDir() + "/" + c.getScreenshotFilename();
        AppCompatImageView imageView = gridView.findViewById(R.id.grid_item_image);
        if (doNotShowDesktopThumbnails) {
            imageView.setVisibility(View.GONE);
            lp.height = GridView.LayoutParams.WRAP_CONTENT;
        } else {
            boolean screenshotExists = new File(screenshotFilePath).exists();
            if (screenshotExists) {
                Log.d(TAG, "Setting screenshot from file " + screenshotFilePath);
                Bitmap gridImage = BitmapFactory.decodeFile(screenshotFilePath);
                imageView.setImageBitmap(gridImage);
            } else {
                imageView.setImageResource(R.drawable.ic_screen_black_48dp);
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }
        }
        gridView.setLayoutParams(lp);


        // This ID in a hidden TextView is used to send the right connection for editing or connection establishment
        // when the item is long-tapped or tapped respectively.
        TextView gridItemId = (TextView) gridView.findViewById(R.id.grid_item_id);
        gridItemId.setText(c.getRuntimeId());

        return gridView;
    }

    @Override
    public int getCount() {
        int count = 0;
        if (filteredConnectionsByPosition != null) {
            count = filteredConnectionsByPosition.size();
        }
        return count;
    }

    @Override
    public Object getItem(int position) {
        return filteredConnectionsByPosition.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public int getNumCols() {
        return numCols;
    }
}
