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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.vectras.vm.AppConfig;
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
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RomsManagerActivity extends AppCompatActivity {
    public static RomsManagerActivity activity;

    public static MaterialButton goBtn;

    public static CheckBox acceptLiceneseChkBox;
    public static AlertDialog ad;

    public static String license;
    public static RecyclerView mRVRoms;
    public static AdapterRoms mAdapter;
    public static String Data;
    public static List<DataRoms> data;
    public static Boolean selected = false;
    public static String selectedPath = null;
    public static String selectedExtra = null;
    public static String selectedLink = null;
    public static String selectedName = null;
    public static String selectedIcon = null;

    public MaterialButtonToggleGroup filterToggle;
    public MaterialButton windowsToggle;
    public MaterialButton linuxToggle;
    public MaterialButton appleToggle;
    public MaterialButton androidToggle;
    public MaterialButton otherToggle;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_roms_manager);
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
                new RomsManagerActivity.AsyncLogin().execute();
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        new RomsManagerActivity.AsyncLogin().execute();
        new Thread(new Runnable() {

            public void run() {

                BufferedReader reader = null;
                final StringBuilder builder = new StringBuilder();

                try {
                    // Create a URL for the desired page
                    URL url = new URL(AppConfig.vectrasTerms); //My text file location
                    //First open the connection
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(60000); // timing out in a minute

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    //t=(TextView)findViewById(R.id.TextView1); // ideally do this in onCreate()
                    String str;
                    while ((str = in.readLine()) != null) {
                        builder.append(str);
                    }
                    in.close();
                } catch (Exception e) {
                    acceptLiceneseChkBox.setEnabled(false);
                    UIUtils.toastLong(activity, "no internet connection "+e.toString());
                }

                //since we are in background thread, to post results we have to go back to ui thread. do the following for that

                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        license = builder.toString(); // My TextFile has 3 lines
                        acceptLiceneseChkBox.setEnabled(true);
                    }
                });

            }
        }).start();

        acceptLiceneseChkBox = findViewById(R.id.acceptLiceneseChkBox);

        acceptLiceneseChkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    UIAlertLicense("Terms&Conditions", license, activity);
                } else {
                    goBtn.setEnabled(false);
                }
            }
        });
        goBtn = (MaterialButton) findViewById(R.id.goBtn);

        goBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                onFirstStartup();
            }
        });

    }

    public static void UIAlertLicense(String title, String html, final Activity activity) {
        AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
        alertDialog.setTitle(title);
        alertDialog.setCancelable(true);

        alertDialog.setMessage(Html.fromHtml(html));

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "I Acknowledge", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                acceptLiceneseChkBox.setChecked(true);
                goBtn.setEnabled(true);
                return;
            }
        });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                acceptLiceneseChkBox.setChecked(false);
                goBtn.setEnabled(false);
            }
        });
        alertDialog.show();
    }

    public static String filter = null;

    public static class AsyncLogin extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread

        }

        @Override
        protected String doInBackground(String... params) {
            HttpsURLConnection con = null;
            try {
                URL u = new URL(AppConfig.romsJson);
                con = (HttpsURLConnection) u.openConnection();

                con.connect();

                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();
                Data = sb.toString();

                return (Data);

            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                if (con != null) {
                    try {
                        con.disconnect();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                return ("unsuccessful!");
            }

        }

        @Override
        protected void onPostExecute(String result) {

            //this method will be running on UI thread
            data = new ArrayList<>();

            try {

                JSONArray jArray = new JSONArray(Data);

                // Extract data from json and store into ArrayList as class objects
                for (int i = 0; i < jArray.length(); i++) {
                    JSONObject json_data = jArray.getJSONObject(i);
                    DataRoms romsData = new DataRoms();
                    romsData.itemName = json_data.getString("rom_name");
                    romsData.itemIcon = json_data.getString("rom_icon");
                    romsData.itemUrl = json_data.getString("rom_url");
                    romsData.itemPath = json_data.getString("rom_path");
                    romsData.itemAvail = json_data.getBoolean("rom_avail");
                    romsData.itemSize = json_data.getString("rom_size");
                    romsData.itemArch = json_data.getString("rom_arch");
                    romsData.itemKernel = json_data.getString("rom_kernel");
                    romsData.itemExtra = json_data.getString("rom_extra");
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
                UIUtils.toastLong(activity, e.toString());
            }
            mRVRoms = (RecyclerView) activity.findViewById(R.id.romsRv);
            mAdapter = new AdapterRoms(activity, data);
            mRVRoms.setAdapter(mAdapter);
            mRVRoms.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));

        }

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public class RomsJso extends JSONObject {

        public JSONObject makeJSONObject(String imgName, String imgIcon, String imgPath, String imgExtra) {

            JSONObject obj = new JSONObject();

            try {
                obj.put("imgName", imgName);
                obj.put("imgIcon", imgIcon);
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
        new DownloadsImage().execute(selectedIcon);
    }

    public void onFirstStartup() {
        if (selected) {
            if (FileUtils.fileValid(activity, AppConfig.maindirpath + selectedPath) && !FileUtils.fileValid(activity, AppConfig.maindirpath + "icons/" + selectedPath.replace(".IMG", ".jpg"))) {
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
                startIconDownload();
                final File jsonFile = new File(AppConfig.maindirpath + "roms-data" + ".json");

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

                        JSONObject jsonObject = obj.makeJSONObject(selectedName, AppConfig.maindirpath + "icons/" + selectedPath.replace(".IMG", ".jpg"), AppConfig.maindirpath + selectedPath, selectedExtra);
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
                } else {
                    JSONObject jsonObject = obj.makeJSONObject(selectedName, AppConfig.maindirpath + "icons/" + selectedPath.replace(".IMG", ".jpg"), AppConfig.maindirpath + selectedPath, selectedExtra);
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
                activity.startActivity(new Intent(activity, MainActivity.class));

				/*new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						mProgressDialog.dismiss();					}
				}, 3000);*/
            } else {
                AlertDialog ad;
                ad = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
                ad.setTitle(selectedPath.replace(".IMG", ".vbi") + " Needs to import");
                ad.setMessage("press import button and select " + selectedPath.replace(".IMG", ".vbi") + " file.");
                ad.setButton(Dialog.BUTTON_POSITIVE, "IMPORT", (dialog, which) -> {
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
                ad.setButton(Dialog.BUTTON_NEGATIVE, "DOWNLAOD " + selectedPath.replace(".IMG", ".vbi"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String gt = selectedLink;
                        Intent g = new Intent(Intent.ACTION_VIEW);
                        g.setData(Uri.parse(gt));
                        RomsManagerActivity.activity.startActivity(g);
                        RomsManagerActivity.activity.finish();
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

    public ProgressDialog progressDialog = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK) {
            Uri content_describer = data.getData();
            File selectedFilePath = new File(getPath(content_describer));
            if (selectedFilePath.getName().equals(selectedPath.replace(".IMG", ".vbi"))) {

                try {
                    progressDialog = new ProgressDialog(activity,
                            R.style.MainDialogTheme);
                    progressDialog.setTitle("Extracting");
                    progressDialog.setMessage("Please wait...");
                    progressDialog.setCancelable(false);
                    progressDialog.show(); // Showing Progress Dialog
                    Thread t = new Thread() {
                        public void run() {
                            FileInputStream zipFile = null;
                            try {
                                zipFile = (FileInputStream) getContentResolver().openInputStream(content_describer);
                            } catch (FileNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                            File targetDirectory = new File(AppConfig.maindirpath);
                            ZipInputStream zis = null;
                            zis = new ZipInputStream(
                                    new BufferedInputStream(zipFile));
                            try {
                                ZipEntry ze;
                                int count;
                                byte[] buffer = new byte[8192];
                                while ((ze = zis.getNextEntry()) != null) {
                                    File file = new File(targetDirectory, ze.getName());
                                    File dir = ze.isDirectory() ? file : file.getParentFile();
                                    if (!dir.isDirectory() && !dir.mkdirs())
                                        throw new FileNotFoundException("Failed to ensure directory: " +
                                                dir.getAbsolutePath());
                                    if (ze.isDirectory())
                                        continue;
                                    try (FileOutputStream fout = new FileOutputStream(file)) {
                                        while ((count = zis.read(buffer)) != -1)
                                            fout.write(buffer, 0, count);
                                    }
                        /* if time should be restored as well
                        long time = ze.getTime();
                        if (time > 0)
                            file.setLastModified(time);
                        */
                                }
                            } catch (IOException e) {
                                UIUtils.toastLong(activity, e.toString());
                                throw new RuntimeException(e);
                            } finally {
                                progressDialog.cancel(); // cancelling Dialog.
                                try {
                                    zis.close();
                                } catch (IOException e) {
                                    UIUtils.toastLong(activity, e.toString());
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    };
                    t.start();
                } catch (Exception e) {
                    progressDialog.dismiss(); // Close Progress Dialog
                    UIUtils.toastLong(activity, e.toString());
                    throw new RuntimeException(e);
                }

            } else {
                MainActivity.UIAlert("File not supported", "please select " + selectedPath.replace(".IMG", ".vbi") + " file to continue.", activity);
            }

        }
    }

    public static final int DIALOG_DOWNLOAD_PROGRESS = 0;

    static class DownloadsImage extends AsyncTask<String, Void, Void> {

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

}
