package com.vectras.vm;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.MainVNCActivity;
import com.vectras.vm.AppConfig;
import com.vectras.vm.Fragment.HomeFragment;
import com.vectras.vm.MainRoms.AdapterMainRoms;
import com.vectras.vm.MainRoms.DataMainRoms;
import com.vectras.vm.Roms.AdapterRoms;
import com.vectras.vm.Roms.DataRoms;
import com.vectras.vm.logger.VectrasStatus;
import com.vectras.qemu.utils.FileInstaller;
import com.vectras.vm.utils.FileUtils;

import java.io.BufferedInputStream;

import com.vectras.vm.utils.UIUtils;
import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RomsManagerActivity extends AppCompatActivity {

    private RequestNetwork net;
    private RequestNetwork.RequestListener _net_request_listener;
    private String contentJSON = "";

    public static RomsManagerActivity activity;

    public static MaterialButton goBtn;

    public static AlertDialog ad;

    public static String license;
    public static RecyclerView mRVRoms;
    public static AdapterRoms mAdapter;
    public static List<DataRoms> data;
    public static Boolean selected = false;
    public static String selectedPath = null;
    public static String selectedExtra = null;
    public static String selectedLink = null;
    public static String selectedName = null;
    public static String selectedIcon = null;
    public static String selectedArch = null;
    public static String selectedFinalRomFileName = null;

    public MaterialButtonToggleGroup filterToggle;
    public MaterialButton windowsToggle;
    public MaterialButton linuxToggle;
    public MaterialButton appleToggle;
    public MaterialButton androidToggle;
    public MaterialButton otherToggle;

    public ProgressBar loadingPb;

    private LinearLayout linearload;
    private LinearLayout linearnothinghere;
    private Button buttontryagain;

    public static String sAvailable = "";
    public static String sUnavailable = "";
    public static String sInstalled = "";
    public static boolean isFinishNow = false;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * CHECK WHETHER INTERNET CONNECTION IS AVAILABLE OR NOT
     */
    public boolean checkConnection(Context context) {
        final ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connMgr != null) {
            NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();

            if (activeNetworkInfo != null) { // connected to the internet
                // connected to the mobile provider's data plan
                if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    // connected to wifi
                    return true;
                } else
                    return activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
            }
        }
        return false;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        activity = this;
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        VectrasApp.prepareDataForAppConfig(activity);
        sAvailable = getResources().getString(R.string.available);
        sUnavailable = getResources().getString(R.string.unavailable);
        sInstalled = getResources().getString(R.string.installed);
        SharedPreferences prefs = getSharedPreferences(CREDENTIAL_SHARED_PREF, Context.MODE_PRIVATE);
        boolean isAccessed = prefs.getBoolean("isFirstLaunch", false);
        //if (!isAccessed && !checkConnection(activity))
            //UIUtils.UIAlert(activity, "for first time you need internet connection to load app data!", "No internet connection!");
        setContentView(R.layout.activity_roms_manager);
        linearload = findViewById(R.id.linearload);
        linearnothinghere = findViewById(R.id.linearnothinghere);
        buttontryagain = findViewById(R.id.buttontryagain);
        loadingPb = findViewById(R.id.loadingPb);
        filterToggle = findViewById(R.id.filterToggle);
        windowsToggle = findViewById(R.id.windowsToggle);
        linuxToggle = findViewById(R.id.linuxToggle);
        appleToggle = findViewById(R.id.appleToggle);
        androidToggle = findViewById(R.id.androidToggle);
        otherToggle = findViewById(R.id.otherToggle);
        mRVRoms = findViewById(R.id.romsRv);
        filterToggle.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (checkedId == R.id.windowsToggle) {
                    if (isChecked)
                        filter = "windows";
                    else
                        filter = null;
                } else if (checkedId == R.id.linuxToggle) {
                    if (isChecked)
                        filter = "linux";
                    else
                        filter = null;
                } else if (checkedId == R.id.appleToggle) {
                    if (isChecked)
                        filter = "apple";
                    else
                        filter = null;
                } else if (checkedId == R.id.androidToggle) {
                    if (isChecked)
                        filter = "android";
                    else
                        filter = null;
                } else if (checkedId == R.id.otherToggle) {
                    if (isChecked)
                        filter = "other";
                    else
                        filter = null;
                }
                loadData();
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle(getResources().getString(R.string.roms_store));

        goBtn = (MaterialButton) findViewById(R.id.goBtn);

        goBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                onFirstStartup();
            }
        });

        CardView custom = (CardView) findViewById(R.id.cdCustom);

        custom.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startActivity(new Intent(activity, CustomRomActivity.class));
            }
        });

        net = new RequestNetwork(this);
        _net_request_listener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                contentJSON = response;
                if (contentJSON.length() == 0)
                        contentJSON ="[]";
                loadData();
                linearload.setVisibility(View.GONE);
            }

            @Override
            public void onErrorResponse(String tag, String message) {
                linearload.setVisibility(View.GONE);
                linearnothinghere.setVisibility(View.VISIBLE);
            }
        };

        buttontryagain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linearload.setVisibility(View.VISIBLE);
                net.startRequestNetwork(RequestNetworkController.GET,AppConfig.vectrasRaw + "vroms-store.json","anbui",_net_request_listener);
            }
        });

        net.startRequestNetwork(RequestNetworkController.GET,AppConfig.vectrasRaw + "vroms-store.json","anbui",_net_request_listener);
    }

    public void onResume() {
        super.onResume();
        if (isFinishNow)
            finish();
        isFinishNow = false;
    }

    private void loadData() {
        data = new ArrayList<>();
        try {
            JSONArray jArray = new JSONArray(contentJSON);

            // Extract data from json and store into ArrayList as class objects
            for (int i = 0; i < jArray.length(); i++) {
                JSONObject json_data = jArray.getJSONObject(i);
                DataRoms romsData = new DataRoms();
                romsData.itemName = json_data.getString("rom_name");
                romsData.itemIcon = json_data.getString("rom_icon");
                romsData.itemUrl = json_data.getString("rom_url");
                romsData.itemPath = json_data.getString("rom_path");
                romsData.itemFinalRomFileName = json_data.getString("final_rom_file_name");
                romsData.itemAvail = json_data.getBoolean("rom_avail");
                romsData.itemSize = json_data.getString("rom_size");
                romsData.itemArch = json_data.getString("rom_arch");
                romsData.itemKernel = json_data.getString("rom_kernel");
                romsData.itemExtra = json_data.getString("rom_extra");
                romsData.itemDesc = json_data.getString("desc");
                if (filter != null) {
                    if (romsData.itemKernel.toLowerCase().contains(filter.toLowerCase())) {
                        data.add(romsData);
                    }
                } else {
                    data.add(romsData);
                }
            }

            // Setup and Handover data to recyclerview

        } catch (JSONException e) {
            UIUtils.UIAlert(activity, "ERROR", e.toString());
        }
        mRVRoms = (RecyclerView) activity.findViewById(R.id.romsRv);
        mAdapter = new AdapterRoms(activity, data);
        mRVRoms.setAdapter(mAdapter);
        mRVRoms.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
    }


    public static String filter = null;

    public static class RomsJso extends JSONObject {

        public JSONObject makeJSONObject(String imgName, String imgIcon, String imgArch, String imgPath, String imgExtra) {

            JSONObject obj = new JSONObject();

            try {
                obj.put("imgName", imgName);
                obj.put("imgIcon", imgIcon);
                obj.put("imgArch", imgArch);
                obj.put("imgPath", imgPath);
                obj.put("imgExtra", imgExtra);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return obj;
        }
    }

    public static final String CREDENTIAL_SHARED_PREF = "settings_prefs";

    private void startIconDownload() {

    }

    public void onFirstStartup() {
        if (selected) {
            if (FileUtils.fileValid(activity, AppConfig.maindirpath + selectedPath)) {
                SharedPreferences credentials = activity.getSharedPreferences(CREDENTIAL_SHARED_PREF, Context.MODE_PRIVATE);
                ProgressDialog mProgressDialog = new ProgressDialog(this, R.style.MainDialogTheme);
                mProgressDialog.setTitle("Data Setup");
                mProgressDialog.setMessage("Please Wait...");
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
                //FileInstaller.installFiles(activity, false);
                SharedPreferences.Editor editor = credentials.edit();
                editor.putBoolean("isFirstLaunch", Boolean.TRUE);
                editor.apply();
                RomsJso obj = new RomsJso();
                try {
                    startIconDownload();
                } catch (Exception e) {
                    File file = new File(selectedPath);
                    try {
                        file.delete();
                    } catch (Exception er) {
                        throw new RuntimeException(er);
                    }
                    throw new RuntimeException(e);
                } finally {
                    mProgressDialog.dismiss();
                    final File jsonFile = new File(AppConfig.maindirpath + "roms-data.json");

                    if (jsonFile.exists()) {
                        try {
                            List<DataMainRoms> data = new ArrayList<>();
                            JSONArray jArray = new JSONArray(FileUtils.readFromFile(MainActivity.activity, jsonFile));

                            try {
                                // Extract data from json and store into ArrayList as class objects
                                for (int i = 0; i < jArray.length(); i++) {
                                    JSONObject json_data = jArray.getJSONObject(i);
                                    DataMainRoms romsMainData = new DataMainRoms();
                                    romsMainData.itemName = json_data.getString("imgName");
                                    romsMainData.itemIcon = json_data.getString("imgIcon");
                                    romsMainData.itemPath = json_data.getString("imgPath");
                                    romsMainData.itemExtra = json_data.getString("imgExtra");
                                    data.add(romsMainData);
                                }

                            } catch (JSONException e) {
                                Toast.makeText(MainActivity.activity, e.toString(), Toast.LENGTH_LONG).show();
                            }

                            JSONObject jsonObject = obj.makeJSONObject(selectedName, AppConfig.maindirpath + "icons/" + selectedPath.replace(".IMG", ".jpg"), MainSettingsManager.getArch(activity), AppConfig.maindirpath + selectedPath, selectedExtra);
                            jArray.put(jsonObject);
                            try {
                                Writer output = null;
                                output = new BufferedWriter(new FileWriter(jsonFile));
                                output.write(jArray.toString().replace("\\", "").replace("//", "/"));
                                output.close();
                            } catch (Exception e) {
                                UIUtils.toastLong(activity, e.toString());
                            }
                        } catch (JSONException e) {
                            UIUtils.toastLong(activity, e.toString());
                        }
                        //MainActivity.activity.finish();
                    } else {
                        JSONObject jsonObject = obj.makeJSONObject(selectedName, AppConfig.maindirpath + "icons/" + selectedPath.replace(".IMG", ".jpg"), MainSettingsManager.getArch(activity), AppConfig.maindirpath + selectedPath, selectedExtra);
                        JSONArray jsonArray = new JSONArray();
                        jsonArray.put(jsonObject);
                        try {
                            Writer output = null;
                            output = new BufferedWriter(new FileWriter(jsonFile));
                            output.write(jsonArray.toString().replace("\\", "").replace("//", "/"));
                            output.close();
                        } catch (Exception e) {
                            UIUtils.toastLong(activity, e.toString());
                        }
                        VectrasStatus.logInfo("Welcome to Vectras ♡");
                    }

				/*new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						mProgressDialog.dismiss();					}
				}, 3000);*/
                    finish();
                    startActivity(new Intent(activity, SplashActivity.class));
                }
            } else {
                AlertDialog ad;
                ad = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
                ad.setTitle(selectedPath);
                ad.setMessage("Have you downloaded this file yet? If you have, you will need to select it to continue. If not, you can get it.");
                ad.setButton(Dialog.BUTTON_POSITIVE, "Select that file now", (dialog, which) -> {
                    Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");

                    // Optionally, specify a URI for the file that should appear in the
                    // system file picker when it loads.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                    }

                    startActivityForResult(intent, 0);
                });
                ad.setButton(Dialog.BUTTON_NEGATIVE, "Get " + selectedPath.replace(".IMG", ".vbi"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (selectedLink != null) {
                            String gt = selectedLink;
                            Intent g = new Intent(Intent.ACTION_VIEW);
                            g.setData(Uri.parse(gt));
                            startActivity(g);
                        }
                    }
                });
                ad.show();
            }
        } else {
            AlertDialog ad;
            ad = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
            ad.setTitle("Please Select");
            ad.setMessage("Select the os (operating system) you need.");
            ad.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    return;
                }
            });
            ad.show();
        }
    }

    public String getPath(Uri uri) {
        return FileUtils.getPath(activity, uri);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK) {
            Uri content_describer = data.getData();
            File selectedFilePath = new File(getPath(content_describer));
            if (selectedFilePath.getName().equals(selectedPath) || (selectedFilePath.getName().endsWith(".cvbi.zip") && selectedFilePath.getName().equals(selectedPath + ".zip"))) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), CustomRomActivity.class);
                intent.putExtra("addromnow", "");
                intent.putExtra("romname", selectedName);
                intent.putExtra("romfilename", selectedPath);
                intent.putExtra("finalromfilename", selectedFinalRomFileName);
                intent.putExtra("rompath", selectedFilePath.getPath());
                if (selectedExtra.contains(selectedFilePath.getName())) {
                    intent.putExtra("addtodrive", "");
                    intent.putExtra("romextra", selectedExtra);
                } else {
                    intent.putExtra("addtodrive", "1");
                    intent.putExtra("romextra", selectedExtra);
                }
                intent.putExtra("romicon", AppConfig.maindirpath + "icons/" + selectedPath + ".png");
                switch (selectedArch) {
                    case "X86_64":
                        MainSettingsManager.setArch(this, "X86_64");
                        break;
                    case "i386":
                        MainSettingsManager.setArch(this, "I386");
                        break;
                    case "ARM64":
                        MainSettingsManager.setArch(this, "ARM64");
                        break;
                    case "PowerPC":
                        MainSettingsManager.setArch(this, "PPC");
                        break;
                }
                startActivity(intent);
            } else {
                UIUtils.UIAlert(activity, "Please select " + selectedPath.replace(".IMG", ".vbi") + " file to continue.", "File not supported");
            }

        }
    }

    public static class DownloadsImage extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            URL url = null;
            try {
                url = new URL(strings[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            Bitmap bm = null;
            try {
                bm = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Create Path to save Image
            File path = new File(AppConfig.maindirpath + "icons"); //Creates app specific folder

            if (!path.exists()) {
                path.mkdirs();
            }

            File imageFile = new File(path, selectedPath.replace(".IMG", ".jpg")); // Imagename.png
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(imageFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                bm.compress(Bitmap.CompressFormat.PNG, 100, out); // Compress Image
                out.flush();
                out.close();
                // Tell the media scanner about the new file so that it is
                // immediately available to the user.
                MediaScannerConnection.scanFile(activity, new String[]{imageFile.getAbsolutePath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        // Log.i("ExternalStorage", "Scanned " + path + ":");
                        //    Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
            } catch (Exception ignored) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (getParentActivityIntent() == MainActivity.activity.getIntent())
            finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (MainSettingsManager.getArch(activity) == null) {
            startActivity(new Intent(this, SetArchActivity.class));
        }
        activity = this;
    }

    private String getDataFromUrl(String _url) {
        Log.d("RomsManagerActivity", _url);
        try {
            StringBuilder sb = new StringBuilder();
            URL url = new URL(_url);
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
        } catch (ExceptionInInitializerError ex) {
            ex.printStackTrace();
            return "[]";

        } catch (Exception e) {
            e.printStackTrace();
            return "[]";

        }
    }

}
