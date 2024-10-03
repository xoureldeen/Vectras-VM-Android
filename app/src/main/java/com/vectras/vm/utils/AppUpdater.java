package com.vectras.vm.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import com.vectras.vm.AppConfig;
import com.vectras.vm.R;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AppUpdater extends AsyncTask<String, String, String> {

    private Context context;
    private OnUpdateListener listener;
    private ProgressDialog progressDialog;
    private boolean isOnCreate;
	
    public AppUpdater(Context context, OnUpdateListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void start(boolean isOnCreate) {
        this.isOnCreate = isOnCreate;
        execute();
    }

    public interface OnUpdateListener {
        void onUpdateListener(String result);
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            StringBuilder sb = new StringBuilder();
			URL url = new URL(AppConfig.updateJson);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setRequestMethod("GET");
            conn.connect();

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String response;

            while ((response = br.readLine()) != null) {
                sb.append(response);
            }
			return sb.toString();
        } catch (ExceptionInInitializerError e) {
            e.printStackTrace();
            return "Error on getting data: " + e.getMessage();
        } catch (NoClassDefFoundError e) {
            e.printStackTrace();
            return "Error on getting data: " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error on getting data: " + e.getMessage();

        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (isOnCreate) {
            progressDialog = new ProgressDialog(context, R.style.MainDialogTheme);
            progressDialog.setMessage("Please wait for the check");
            progressDialog.setTitle("Looking for Update");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (isOnCreate && progressDialog != null) {
            progressDialog.dismiss();
        }
        if (listener != null) {
            listener.onUpdateListener(s);
        }
    }
}
