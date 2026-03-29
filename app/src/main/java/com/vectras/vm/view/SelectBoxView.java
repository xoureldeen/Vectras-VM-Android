package com.vectras.vm.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vectras.vm.R;

public class SelectBoxView extends LinearLayout {

    private TextView tvTitle, tvSubtitle;

    public SelectBoxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.select_box_view, this, true);

        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SelectBoxView);

            String title = typedArray.getString(R.styleable.SelectBoxView_title);
            String subtitle = typedArray.getString(R.styleable.SelectBoxView_subtitle);

            if (title != null) tvTitle.setText(title);
            if (subtitle != null) tvSubtitle.setText(subtitle);

            typedArray.recycle();
        }
    }

    public void setTitle(String title) {
        tvTitle.setText(title);
    }

    public void setSubtitle(String subtitle) {
        tvSubtitle.setText(subtitle);
    }

    public void setEnabled(boolean isEnabled) {
        findViewById(R.id.root).setEnabled(isEnabled);
        findViewById(R.id.root).setAlpha(0.5f);
    }
}