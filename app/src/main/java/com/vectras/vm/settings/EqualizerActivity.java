package com.vectras.vm.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.vectras.vm.R;
import com.vectras.vm.databinding.ActivityEqualizerBinding;
import com.vectras.vm.sound.AudioSettingsData;
import com.vectras.vm.sound.SoundEffect;

import java.text.DecimalFormat;
import java.util.Objects;

public class EqualizerActivity extends AppCompatActivity {

    ActivityEqualizerBinding binding;
    AudioSettingsData audioSettingsData;
    float[] lastValues = new float[5];

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.undo) {
            binding.sliderUpperTreble.setValue(lastValues[0]);
            binding.sliderTreble.setValue(lastValues[1]);
            binding.sliderMid.setValue(lastValues[2]);
            binding.sliderBass.setValue(lastValues[3]);
            binding.sliderLowBass.setValue(lastValues[4]);
            return true;
        } else if (id == R.id.reset) {
            binding.sliderUpperTreble.setValue(0);
            binding.sliderTreble.setValue(0);
            binding.sliderMid.setValue(0);
            binding.sliderBass.setValue(0);
            binding.sliderLowBass.setValue(0);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.equalizer_menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityEqualizerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        initialize();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (audioSettingsData != null) {
            audioSettingsData.setEqualizerEnabled(binding.swEnabled.isChecked());
            audioSettingsData.setUpperTreble(binding.sliderUpperTreble.getValue());
            audioSettingsData.setTreble(binding.sliderTreble.getValue());
            audioSettingsData.setMid(binding.sliderMid.getValue());
            audioSettingsData.setBass(binding.sliderBass.getValue());
            audioSettingsData.setLowBass(binding.sliderLowBass.getValue());
        }
    }

    private void initialize() {
        SoundEffect soundEffect = new SoundEffect(this, 0);
        short bands = soundEffect.equalizer.getNumberOfBands();

        if (bands < 2) {
            binding.lnUnsupport.setVisibility(View.VISIBLE);
            binding.appbar.setVisibility(View.GONE);
            binding.lnEnabled.setVisibility(View.GONE);
            binding.lnAllOptions.setVisibility(View.GONE);
            binding.btnExit.setOnClickListener(v -> finish());
            return;
        }

        audioSettingsData = new AudioSettingsData(this);

        lastValues[0] = audioSettingsData.getUpperTreble();
        lastValues[1] = audioSettingsData.getTreble();
        lastValues[2] = audioSettingsData.getMid();
        lastValues[3] = audioSettingsData.getBass();
        lastValues[4] = audioSettingsData.getLowBass();


        // 1dB = 100mB
        int minBandLevelRange = soundEffect.getMinBandLevelRange() / 100;
        int maxBandLevelRange = soundEffect.getMaxBandLevelRange() / 100;

        if (bands > 4) {
            binding.sliderLowBass.setValueFrom(minBandLevelRange);
            binding.sliderLowBass.setValueTo(maxBandLevelRange);

            binding.sliderLowBass.setValue(audioSettingsData.getLowBass());

            binding.tvLowBassValue.setText(getFormattedValue(binding.sliderLowBass.getValue()));

            binding.sliderLowBass.addOnChangeListener((slider, value, fromUser) -> binding.tvLowBassValue.setText(getFormattedValue(value)));
        } else {
            binding.lnLowBass.setVisibility(View.GONE);
            binding.tvUpperTreble.setText(R.string.upper_treble);
            binding.tvTreble.setText(R.string.treble);
            binding.tvMid.setText(R.string.bass);
            binding.tvBass.setText(R.string.low_bass);
        }

        if (bands > 3) {
            binding.sliderBass.setValueFrom(minBandLevelRange);
            binding.sliderBass.setValueTo(maxBandLevelRange);

            binding.sliderBass.setValue(audioSettingsData.getBass());

            binding.tvBassValue.setText(getFormattedValue(binding.sliderBass.getValue()));

            binding.sliderBass.addOnChangeListener((slider, value, fromUser) -> binding.tvBassValue.setText(getFormattedValue(value)));
        } else {
            binding.lnBass.setVisibility(View.GONE);
            binding.tvUpperTreble.setText(R.string.treble);
            binding.tvTreble.setText(R.string.mid);
            binding.tvMid.setText(R.string.bass);
        }

        if (bands > 2) {
            binding.sliderMid.setValueFrom(minBandLevelRange);
            binding.sliderMid.setValueTo(maxBandLevelRange);

            binding.sliderMid.setValue(audioSettingsData.getMid());

            binding.tvMidValue.setText(getFormattedValue(binding.sliderMid.getValue()));

            binding.sliderMid.addOnChangeListener((slider, value, fromUser) -> binding.tvMidValue.setText(getFormattedValue(value)));
        } else {
            binding.lnMid.setVisibility(View.GONE);
            binding.tvUpperTreble.setText(R.string.treble);
            binding.tvTreble.setText(R.string.bass);
        }

        binding.sliderUpperTreble.setValueFrom(minBandLevelRange);
        binding.sliderUpperTreble.setValueTo(maxBandLevelRange);

        binding.sliderTreble.setValueFrom(minBandLevelRange);
        binding.sliderTreble.setValueTo(maxBandLevelRange);


        binding.sliderUpperTreble.setValue(audioSettingsData.getUpperTreble());
        binding.sliderTreble.setValue(audioSettingsData.getTreble());


        binding.tvUpperTrebleValue.setText(getFormattedValue(binding.sliderUpperTreble.getValue()));
        binding.tvTrebleValue.setText(getFormattedValue(binding.sliderTreble.getValue()));


        binding.sliderUpperTreble.addOnChangeListener((slider, value, fromUser) -> binding.tvUpperTrebleValue.setText(getFormattedValue(value)));
        binding.sliderTreble.addOnChangeListener((slider, value, fromUser) -> binding.tvTrebleValue.setText(getFormattedValue(value)));


        binding.lnEnabled.setOnClickListener(v -> binding.swEnabled.toggle());
        binding.swEnabled.setChecked(audioSettingsData.isEqualizerEnabled());
        binding.swEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> setEnabled(isChecked));


        setEnabled(binding.swEnabled.isChecked());
    }

    private void setEnabled(boolean isEnabled) {
        binding.lnAllOptions.setAlpha(isEnabled ? 1 : 0.5f);
        binding.sliderUpperTreble.setEnabled(isEnabled);
        binding.sliderTreble.setEnabled(isEnabled);
        binding.sliderMid.setEnabled(isEnabled);
        binding.sliderBass.setEnabled(isEnabled);
        binding.sliderLowBass.setEnabled(isEnabled);
    }

    DecimalFormat decimalFormat = new DecimalFormat("#.#");

    private String getFormattedValue(float value) {
        return decimalFormat.format(value) + " dB";
    }
}