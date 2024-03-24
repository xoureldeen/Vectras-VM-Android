package com.iiordanov.util;

import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class CustomClientConfigFileReader {
    private final static String TAG = "ConfigFileReader";

    private Map<String, Map> configData;

    public CustomClientConfigFileReader(InputStream configFileInputStream) {
        BufferedReader reader = null;
        reader = new BufferedReader(new InputStreamReader(configFileInputStream));

        Yaml yaml = new Yaml();
        configData = yaml.load(reader);

        try {
            reader.close();
        } catch (IOException e) {
            android.util.Log.e(TAG, "Error closing config reader.");
            e.printStackTrace();
        }
    }

    public Map<String, Map> getConfigData() {
        return configData;
    }
}
