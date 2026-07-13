package com.vectras.vm.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.vectras.vm.R;

public class SwitchView extends LinearLayout {
    private TextView tvTitle, tvSubtitle;
    private MaterialSwitch switchWidget;

    public interface OnCheckedChangeListener {
        void onCheckedChanged(SwitchView view, boolean isChecked);
    }

    private SwitchView.OnCheckedChangeListener listener;

    public void setOnCheckedChangeListener(SwitchView.OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    public SwitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.switch_view, this, true);

        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        switchWidget = findViewById(R.id.switchWidget);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SwitchView);

            String title = typedArray.getString(R.styleable.SwitchView_title);
            String subTitle = typedArray.getString(R.styleable.SwitchView_subtitle);
            boolean isChecked = typedArray.getBoolean(R.styleable.SwitchView_setChecked, false);
            boolean isEnabled = typedArray.getBoolean(R.styleable.SwitchView_setEnable, true);

            if (title != null) tvTitle.setText(title);
            if (subTitle != null) tvSubtitle.setText(subTitle);
            switchWidget.setChecked(isChecked);
            setEnabled(isEnabled);

            typedArray.recycle();
        }

        setOnClickListener(v -> switchWidget.setChecked(!switchWidget.isChecked()));

        switchWidget.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onCheckedChanged(this, isChecked);
            }
        });
    }

    public void setTitle(String title) {
        tvTitle.setText(title);
    }

    public void setSubTitle(String title) {
        tvSubtitle.setText(title);
    }

    public void setChecked(boolean isCheck) {
        switchWidget.setChecked(isCheck);
    }

    public boolean isChecked() {
        return switchWidget.isChecked();
    }

    public void setEnabled(boolean isEnabled) {
        findViewById(R.id.root).setEnabled(isEnabled);
        findViewById(R.id.root).setAlpha(isEnabled ? 1f : 0.5f);
        switchWidget.setEnabled(isEnabled);
    }
}
