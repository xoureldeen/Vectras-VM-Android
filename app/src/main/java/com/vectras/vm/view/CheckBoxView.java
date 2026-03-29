package com.vectras.vm.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vectras.vm.R;

public class CheckBoxView extends LinearLayout {
    private TextView tvTitle;
    private CheckBox checkBox;

    public interface OnCheckedChangeListener {
        void onCheckedChanged(CheckBoxView view, boolean isChecked);
    }

    private OnCheckedChangeListener listener;

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    public CheckBoxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.checkbox_view, this, true);

        tvTitle = findViewById(R.id.tv_title);
        checkBox = findViewById(R.id.checkbox);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CheckBoxView);

            String title = typedArray.getString(R.styleable.CheckBoxView_setText);
            boolean isChecked = typedArray.getBoolean(R.styleable.CheckBoxView_setChecked, false);

            if (title != null) tvTitle.setText(title);
            checkBox.setChecked(isChecked);

            typedArray.recycle();
        }

        findViewById(R.id.root).setOnClickListener(v -> checkBox.setChecked(!checkBox.isChecked()));

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onCheckedChanged(this, isChecked);
            }
        });
    }

    public void setText(String title) {
        tvTitle.setText(title);
    }

    public void setChecked(boolean isCheck) {
        checkBox.setChecked(isCheck);
    }

    public boolean isChecked() {
        return checkBox.isChecked();
    }

    public void setEnabled(boolean isEnabled) {
        findViewById(R.id.root).setEnabled(isEnabled);
        findViewById(R.id.root).setAlpha(0.5f);
        checkBox.setEnabled(isEnabled);
    }
}
