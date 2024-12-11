package com.vectras.vm;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.Fragment.CreateImageDialogFragment;
import com.vectras.vm.MainRoms.DataMainRoms;
import com.vectras.vm.logger.VectrasStatus;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.UIUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class CustomRomActivity extends AppCompatActivity {

    public static TextInputEditText title;
    public TextInputEditText icon;
    public static TextInputEditText drive;
    public TextInputEditText cdrom;
    public TextInputEditText qemu;
    public Button addRomBtn;
    boolean iseditparams = false;
    public String previousName = "";
    public String secondVMdirectory = "";
    public boolean addromnowdone = false;
    public String vmID = VMManager.idGenerator();
    private boolean created = false;

    public ProgressBar loadingPb;

    public static CustomRomActivity activity;

    private boolean isFilled(TextInputEditText TXT) {
        if (TXT.getText().toString().trim().length() > 0)
            return true;
        else
            return false;
    }

    boolean modify;

    public static DataMainRoms current;

    private AlertDialog alertDialog;

    public void onStart() {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        if (!modify)
            menu.add(1, 1, 1, "custom rom").setShortcut('3', 'c').setIcon(R.drawable.input_circle).setShowAsAction(1);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case 1:
                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                }

                startActivityForResult(intent, 0);
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_rom);
        activity = this;
        Toolbar toolbar = findViewById(R.id.toolbar);
        loadingPb = findViewById(R.id.loadingPb);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle(getString(R.string.rom_options));
        //AdView mAdView = findViewById(R.id.adView);
        //AdRequest adRequest = new AdRequest.Builder().build();
        //mAdView.loadAd(adRequest);
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {

            }
        });
        title = findViewById(R.id.title);
        icon = findViewById(R.id.icon);
        drive = findViewById(R.id.drive);
        cdrom = findViewById(R.id.cdrom);
        qemu = findViewById(R.id.qemu);
        TextInputLayout iconLayout = findViewById(R.id.iconField);
        TextInputLayout driveLayout = findViewById(R.id.driveField);
        TextInputLayout cdromLayout = findViewById(R.id.cdromField);
        TextInputLayout qemuLayout = findViewById(R.id.qemuField);
        TextView arch = findViewById(R.id.textArch);
        arch.setText(MainSettingsManager.getArch(this));
        ImageView ivIcon = findViewById(R.id.ivIcon);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                }

                startActivityForResult(intent, 1001);
            }
        });
        iconLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                }

                startActivityForResult(intent, 1001);
            }
        });
        drive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                }

                startActivityForResult(intent, 1002);
            }
        });
        driveLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                }

                startActivityForResult(intent, 1002);
            }
        });

        driveLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File vDir = new File(com.vectras.vm.AppConfig.maindirpath + "IMG");
                if (!vDir.exists()) {
                    vDir.mkdirs();
                }
                CreateImageDialogFragment dialogFragment = new CreateImageDialogFragment();
                dialogFragment.customRom = true;
                dialogFragment.show(getSupportFragmentManager(), "CreateImageDialogFragment");
            }
        });

        View.OnClickListener cdromClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Objects.requireNonNull(cdrom.getText()).length() > 0) {
                    AlertDialog ad;
                    ad = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
                    ad.setTitle("REPLACE ISO");
                    ad.setMessage("there is iso imported you want to replace it?");
                    ad.setButton(Dialog.BUTTON_POSITIVE, "REPLACE", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.setType("*/*");

                            // Optionally, specify a URI for the file that should appear in the
                            // system file picker when it loads.
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                            }

                            startActivityForResult(intent, 1003);
                            return;
                        }
                    });
                    ad.setButton(Dialog.BUTTON_NEGATIVE, "REMOVE", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            File isoFile = new File(cdrom.getText().toString());
                            if (isoFile.exists()) {
                                isoFile.delete();
                            }
                            cdrom.setText("");

                            //Pattern pattern = Pattern.compile(cdromPatternCompile());
                            //Matcher matcher = pattern.matcher(qemu.getText().toString());

                            //if (matcher.find()) {
                                // Replace the entire -drive ... file='...' parameter with an empty string
                                //String modifiedQemuText = qemu.getText().toString().replace(matcher.group(0), "");
                                //qemu.setText(modifiedQemuText);
                           //} else {
                                // Handle the case where the -drive parameter doesn't exist
                                // This could involve logging the issue, displaying a message to the user, etc.
                            //}
                        }
                    });
                    ad.show();
                } else {
                    Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");

                    // Optionally, specify a URI for the file that should appear in the
                    // system file picker when it loads.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                    }

                    startActivityForResult(intent, 1003);
                }
            }
        };

        cdrom.setOnClickListener(cdromClickListener);
        cdromLayout.setOnClickListener(cdromClickListener);

        addRomBtn = findViewById(R.id.addRomBtn);
        addRomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Objects.requireNonNull(title.getText()).toString().isEmpty()) {
                    VectrasApp.oneDialog(getResources().getString(R.string.oops), getResources().getString(R.string.need_set_name),true, false, activity);
                } else if ((!Objects.requireNonNull(drive.getText()).toString().isEmpty()) || (!Objects.requireNonNull(cdrom.getText()).toString().isEmpty())) {
                    checkJsonFile();
                } else {
                    if (VectrasApp.isHaveADisk(Objects.requireNonNull(qemu.getText()).toString())) {
                        checkJsonFile();
                    } else {
                        if (qemu.getText().toString().isEmpty()) {
                            AlertDialog alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
                            alertDialog.setTitle(getResources().getString(R.string.problem_has_been_detected));
                            alertDialog.setMessage(getResources().getString(R.string.qemu_params_is_empty));
                            alertDialog.setCancelable(true);
                            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.continuetext), (dialog, which) -> {
                                checkJsonFile();
                            });
                            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel), (dialog, which) -> {

                            });
                            alertDialog.show();
                        } else {
                            AlertDialog alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
                            alertDialog.setTitle(getResources().getString(R.string.problem_has_been_detected));
                            alertDialog.setMessage(getResources().getString(R.string.you_have_not_added_any_storage_devices));
                            alertDialog.setCancelable(true);
                            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.continuetext), (dialog, which) -> {
                                checkJsonFile();
                            });
                            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel), (dialog, which) -> {

                            });
                            alertDialog.show();
                        }
                    }
                }
            }
        });

        qemu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iseditparams = true;
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), EditActivity.class);
                intent.putExtra("content", Objects.requireNonNull(qemu.getText()).toString());
                startActivity(intent);
            }
        });

        qemuLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iseditparams = true;
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), EditActivity.class);
                intent.putExtra("content", Objects.requireNonNull(qemu.getText()).toString());
                startActivity(intent);
            }
        });

        TextView textName = findViewById(R.id.textName);
        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textName.setText(Objects.requireNonNull(title.getText()).toString());

                if (!Objects.requireNonNull(icon.getText()).toString().isEmpty())
                    return;

                VectrasApp.setIconWithName(ivIcon, title.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        title.addTextChangedListener(afterTextChangedListener);
        icon.addTextChangedListener(afterTextChangedListener);
        drive.addTextChangedListener(afterTextChangedListener);
        qemu.addTextChangedListener(afterTextChangedListener);


        TextInputLayout tIQemu = findViewById(R.id.qemuField);
        tIQemu.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String qcc = "android-app://com.anbui.cqcm.app";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(qcc));
                startActivity(i);
            }
        });

        ivIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                }

                startActivityForResult(intent, 1001);
            }
        });

        modify = getIntent().getBooleanExtra("MODIFY", false);
        if (modify) {
            created = true;
            addRomBtn.setText(R.string.save_changes);
            title.setText(current.itemName);
            icon.setText(current.itemIcon);
            drive.setText(current.itemPath);
            cdrom.setText(current.imgCdrom);
            vmID = getIntent().getStringExtra("VMID");

            if (vmID.isEmpty()) {
                vmID = VMManager.idGenerator();
            }

            //Pattern pattern = Pattern.compile(cdromPatternCompile());
            //Matcher matcher = pattern.matcher(current.itemExtra);

            //if (matcher.find()) {
                //String cdromPath = matcher.group(1);
                //cdrom.setText(cdromPath);
            //}

            qemu.setText(current.itemExtra);

            File imgFile = new  File(Objects.requireNonNull(icon.getText()).toString());

            if(imgFile.exists()){
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                ivIcon.setImageBitmap(myBitmap);
            } else {
                VectrasApp.setIconWithName(ivIcon, current.itemName);
            }
            previousName = current.itemName;
        } else {
            if (getIntent().hasExtra("addromnow")) {
                title.setText(getIntent().getStringExtra("romname"));
                if (Objects.requireNonNull(getIntent().getStringExtra("romextra")).isEmpty()) {
                    setDefault();
                } else {
                    qemu.setText(Objects.requireNonNull(getIntent().getStringExtra("romextra")).replaceAll("OhnoIjustrealizeditsmidnightandIstillhavetodothis", AppConfig.vmFolder + vmID + "/"));
                }
                icon.setText(getIntent().getStringExtra("romicon"));
                if (!Objects.requireNonNull(getIntent().getStringExtra("romicon")).isEmpty()) {
                    File imgFile = new File(getIntent().getStringExtra("romicon"));
                    if (imgFile.exists()) {
                        Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        ivIcon.setImageBitmap(myBitmap);
                    }
                }
                if (Objects.requireNonNull(getIntent().getStringExtra("romfilename")).endsWith(".cvbi")) {
                    importCVBI(Objects.requireNonNull(getIntent().getStringExtra("rompath")), getIntent().getStringExtra("romfilename"));
                } else {
                    addromnowdone = true;
                    if (!Objects.requireNonNull(getIntent().getStringExtra("rompath")).isEmpty()) {
                        selectedDiskFile(Uri.fromFile(new File((Objects.requireNonNull(getIntent().getStringExtra("rompath"))))), false);
                    }
                    if (!Objects.requireNonNull(getIntent().getStringExtra("addtodrive")).isEmpty()) {
                        drive.setText(AppConfig.vmFolder + vmID + "/" + getIntent().getStringExtra("romfilename"));
                    }
                }

            } else if (getIntent().hasExtra("importcvbinow")) {
                title.setText("New VM");
                setDefault();

                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                }

                startActivityForResult(intent, 0);
            } else {
                title.setText("New VM");
                setDefault();
            }
        }
        VectrasApp.prepareDataForAppConfig(activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkpermissions();
        if (iseditparams) {
            iseditparams = false;
            qemu.setText(EditActivity.result);
        }
    }

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

    byte[] data;

    public String getPath(Uri uri) {
        return FileUtils.getPath(this, uri);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent ReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, ReturnedIntent);

        File romDir = new File(AppConfig.vmFolder + vmID);
        romDir.mkdirs();

        LinearLayout custom = findViewById(R.id.custom);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            Uri content_describer = ReturnedIntent.getData();
            TextInputEditText icon = findViewById(R.id.icon);
            File selectedFilePath = new File(getPath(content_describer));
            ImageView ivIcon = findViewById(R.id.ivIcon);
            loadingPb.setVisibility(View.VISIBLE);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FileInputStream File = null;
                    Bitmap selectedImage = null;
                    try {
                        File = (FileInputStream) getContentResolver().openInputStream(content_describer);
                        selectedImage = BitmapFactory.decodeStream(File);
                        Bitmap finalSelectedImage = selectedImage;
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                ivIcon.setImageBitmap(finalSelectedImage);
                            }
                        };
                        activity.runOnUiThread(runnable);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            try {
                                SaveImage(selectedImage, new File(AppConfig.vmFolder + vmID), vmID + "-" + selectedFilePath.getName());
                            } finally {
                                Runnable runnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        loadingPb.setVisibility(View.GONE);
                                        icon.setText(AppConfig.vmFolder + vmID + "/" + vmID + "-" + selectedFilePath.getName());
                                    }
                                };
                                activity.runOnUiThread(runnable);
                                File.close();
                            }
                        } catch (IOException e) {
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    loadingPb.setVisibility(View.GONE);
                                }
                            };
                            activity.runOnUiThread(runnable);
                            UIUtils.UIAlert(activity, "error", e.toString());
                        }

                    }
                }
            }).start();
        } else if (requestCode == 1002 && resultCode == RESULT_OK) {
            selectedDiskFile(ReturnedIntent.getData(), true);
        } else if (requestCode == 1003 && resultCode == RESULT_OK) {
            Uri content_describer = ReturnedIntent.getData();
            File selectedFilePath = new File(getPath(content_describer));
            if (selectedFilePath.getName().endsWith(".iso")) {
                String cdromPath = AppConfig.vmFolder + vmID + "/" + selectedFilePath.getName();
                cdrom.setText(cdromPath);

                //String qemuText = qemu.getText().toString();
                //String cdromParam = "-drive index=1,media=cdrom,file='" + cdromPath + "'";

                //if (MainSettingsManager.getArch(activity).equals("ARM64")) {
                    //if (!qemu.getText().toString().contains("-device nec-usb-xhci")) {
                        //qemu.setText(qemu.getText().toString() + " -device nec-usb-xhci");
                    //}
                    //cdromParam = "-device usb-storage,drive=cdrom -drive if=none,id=cdrom,format=raw,media=cdrom,file='" + cdromPath + "'";
                //} else {
                    //if (MainSettingsManager.getIfType(activity).isEmpty()) {
                        //cdromParam = "-cdrom '" + cdromPath + "'";
                    //}
                //}

                //Pattern pattern = Pattern.compile(cdromPatternCompile2());
                //Matcher matcher = pattern.matcher(qemuText);

                //if (!qemuText.contains("-drive index=1,media=cdrom,file=") || !qemuText.contains("-cdrom") || !qemuText.contains("-device usb-storage,drive=cdrom -drive if=none,id=cdrom,format=raw,media=cdrom,file=")) {
                    //qemu.append(" " + cdromParam);
                //} else {
                    //if (matcher.find()) {
                        //String cdromPath1 = matcher.group(1);
                        //qemu.setText(qemuText.replace(cdromPath1, cdromPath)); // Fixed this line to actually change the text of `qemu`
                    //}
                //}
                loadingPb.setVisibility(View.VISIBLE);
                custom.setVisibility(View.GONE);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FileInputStream File = null;
                        try {
                            File = (FileInputStream) getContentResolver().openInputStream(content_describer);
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            try {
                                OutputStream out = new FileOutputStream(new File(AppConfig.vmFolder + vmID + "/" + selectedFilePath.getName()));
                                try {
                                    // Transfer bytes from in to out
                                    byte[] buf = new byte[1024];
                                    int len;
                                    while ((len = File.read(buf)) > 0) {
                                        out.write(buf, 0, len);
                                    }
                                } finally {
                                    out.close();
                                }
                            } finally {
                                Runnable runnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        loadingPb.setVisibility(View.GONE);
                                        custom.setVisibility(View.VISIBLE);
                                    }
                                };
                                activity.runOnUiThread(runnable);
                                File.close();
                            }
                        } catch (IOException e) {
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    loadingPb.setVisibility(View.GONE);
                                    custom.setVisibility(View.VISIBLE);
                                    UIUtils.UIAlert(activity, "error", e.toString());
                                }
                            };
                            activity.runOnUiThread(runnable);
                        }
                    }
                }).start();
            } else
                UIUtils.UIAlert(activity, "please select iso file to continue.", "File not supported");
        } else if (requestCode == 0 && resultCode == RESULT_OK) {
            Uri content_describer = ReturnedIntent.getData();
            File selectedFilePath = new File(getPath(content_describer));
            importCVBI(selectedFilePath.getPath(), selectedFilePath.getName());
        } else if (requestCode == 1000 && resultCode == RESULT_CANCELED) {
            finish();
        }
    }

    private static void SaveImage(Bitmap finalBitmap, File imgDir, String name) {
        File myDir = imgDir;
        myDir.mkdirs();

        String fname = name;
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkJsonFile() {
        if (isFileExists(AppConfig.romsdatajson)) {
            if (!VectrasApp.checkJSONIsNormal(AppConfig.romsdatajson)) {
                alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
                alertDialog.setTitle(getResources().getString(R.string.oops));
                alertDialog.setMessage(getResources().getString(R.string.need_fix_json_before_create));
                alertDialog.setCancelable(true);
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.delete_all), (dialog, which) -> {
                    VectrasApp.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");
                });
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel), (dialog, which) -> {

                });
                alertDialog.show();
            } else {
                startCreateNewVM();
            }

        } else {
            VectrasApp.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");
            startCreateNewVM();
        }
    }

    private String readFile(String filePath) {
        StringBuilder content = new StringBuilder();
        try (FileInputStream inputStream = new FileInputStream(filePath);
             BufferedReader reader = new BufferedReader(new
                     InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return content.toString();

    }

    private boolean isFileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    private void checkpermissions() {
        boolean result = VectrasApp.checkpermissionsgranted(activity, true);
    }

    private void startCreateVM() {
        //errorjsondialog();

        File isoFile = new File(Objects.requireNonNull(cdrom.getText()).toString());
        if (isoFile.exists() && !Objects.requireNonNull(qemu.getText()).toString().contains(cdrom.getText().toString())) {
            isoFile.delete();
        }

        if (modify) {

            int position = getIntent().getIntExtra("POS", 0);
            final File jsonFile = new File(AppConfig.romsdatajson);
            current.itemDrv1 = Objects.requireNonNull(drive.getText()).toString();
            current.itemExtra = Objects.requireNonNull(qemu.getText()).toString();
            try {
                JSONObject jObj = MainActivity.jArray.getJSONObject(position);
                jObj.put("imgName", Objects.requireNonNull(title.getText()).toString());
                jObj.put("imgIcon", Objects.requireNonNull(icon.getText()).toString());
                jObj.put("imgPath", drive.getText().toString());
                jObj.put("imgExtra", qemu.getText().toString());

                MainActivity.jArray.put(position, jObj);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            try {
                Writer output = null;
                output = new BufferedWriter(new FileWriter(jsonFile));
                output.write(MainActivity.jArray.toString());
                output.close();
            } catch (Exception e) {
                UIUtils.toastLong(MainActivity.activity, e.toString());
            } finally {
                MainActivity.loadDataVbi();
                finish();
                //activity.startActivity(new Intent(activity, SplashActivity.class));
            }
        } else {
            String CREDENTIAL_SHARED_PREF = "settings_prefs";
            SharedPreferences credentials = activity.getSharedPreferences(CREDENTIAL_SHARED_PREF, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = credentials.edit();
            editor.putBoolean("isFirstLaunch", Boolean.TRUE);
            editor.apply();
            loadingPb.setVisibility(View.VISIBLE);
            final File jsonFile = new File(AppConfig.romsdatajson);
            RomsJso obj = new RomsJso();
            if (jsonFile.exists()) {
                try {
                    List<DataMainRoms> data = new ArrayList<>();
                    JSONArray jArray = null;
                    jArray = new JSONArray(FileUtils.readFromFile(MainActivity.activity, jsonFile));

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

                    JSONObject jsonObject = obj.makeJSONObject(Objects.requireNonNull(title.getText()).toString(), Objects.requireNonNull(icon.getText()).toString(), MainSettingsManager.getArch(activity), drive.getText().toString(), qemu.getText().toString());
                    jArray.put(jsonObject);
                    try {
                        Writer output = null;
                        output = new BufferedWriter(new FileWriter(jsonFile));
                        //output.write(jArray.toString().replace("\\", "").replace("//", "/"));
                        output.write(jArray.toString());
                        output.close();
                    } catch (Exception e) {
                        UIUtils.toastLong(activity, e.toString());
                        loadingPb.setVisibility(View.GONE);
                    }
                } catch (JSONException e) {
                    loadingPb.setVisibility(View.GONE);
                    throw new RuntimeException(e);
                }
            } else {
                JSONObject jsonObject = obj.makeJSONObject(Objects.requireNonNull(title.getText()).toString(), Objects.requireNonNull(icon.getText()).toString(), MainSettingsManager.getArch(activity), drive.getText().toString(), qemu.getText().toString());
                JSONArray jsonArray = new JSONArray();
                jsonArray.put(jsonObject);
                try {
                    Writer output = null;
                    output = new BufferedWriter(new FileWriter(jsonFile));
                    //output.write(jsonArray.toString().replace("\\", "").replace("//", "/"));
                    output.write(jsonArray.toString());
                    output.close();
                } catch (Exception e) {
                    UIUtils.toastLong(activity, e.toString());
                }
                VectrasStatus.logInfo("Welcome to Vectras ♡");
            }
            if (getIntent().hasExtra("addromnow")) {
                RomsManagerActivity.isFinishNow = true;
            }

            finish();
            //activity.startActivity(new Intent(activity, SplashActivity.class));
        }
        if (!previousName.isEmpty() && !title.getText().toString().equals(previousName)) {
            if (VectrasApp.isFileExists(AppConfig.vmFolder + previousName + "/vmID.txt")) {
                VectrasApp.copyAFile(AppConfig.vmFolder + previousName + "/vmID.txt", AppConfig.vmFolder + vmID + "/vmID.txt");
            } else {
                VectrasApp.writeToFile(AppConfig.vmFolder + title.getText().toString(), "/vmID.txt", VMManager.idGenerator());
                VectrasApp.copyAFile(AppConfig.vmFolder + vmID + "/vmID.txt", AppConfig.vmFolder + previousName + "/vmID.txt");
            }
        } else {
            VectrasApp.writeToFile(AppConfig.vmFolder + title.getText().toString(), "/vmID.txt", VMManager.idGenerator());
        }
        if ((!secondVMdirectory.isEmpty()) && VectrasApp.isFileExists(AppConfig.vmFolder + vmID + "/vmID.txt")) {
            if (!(AppConfig.vmFolder + title.getText().toString()).equals(secondVMdirectory)) {
                VectrasApp.copyAFile(AppConfig.vmFolder + vmID + "/vmID.txt", secondVMdirectory + "/vmID.txt");
            }
       }
    }

    private void startCreateNewVM() {
        if (modify) {
            VMManager.editVM(Objects.requireNonNull(title.getText()).toString(), Objects.requireNonNull(icon.getText()).toString(), Objects.requireNonNull(drive.getText()).toString(), MainSettingsManager.getArch(activity), Objects.requireNonNull(cdrom.getText()).toString(), Objects.requireNonNull(qemu.getText()).toString(), getIntent().getIntExtra("POS", 0));
        } else {
            VMManager.createNewVM(Objects.requireNonNull(title.getText()).toString(), Objects.requireNonNull(icon.getText()).toString(), Objects.requireNonNull(drive.getText()).toString(), MainSettingsManager.getArch(activity), Objects.requireNonNull(cdrom.getText()).toString(), Objects.requireNonNull(qemu.getText()).toString(), vmID);
        }

        created = true;

        if (getIntent().hasExtra("addromnow")) {
            RomsManagerActivity.isFinishNow = true;
            RomInfo.isFinishNow = true;
        }

        modify = false;
        finish();
    }

    private void setDefault() {
        String defQemuParams;
        if (AppConfig.getSetupFiles().contains("arm64-v8a") || AppConfig.getSetupFiles().contains("x86_64")) {
            switch (MainSettingsManager.getArch(MainActivity.activity)) {
                case "ARM64":
                    defQemuParams = "-M virt,virtualization=true -cpu cortex-a76 -accel tcg,thread=multi -net nic,model=e1000 -net user -device nec-usb-xhci -device usb-kbd -device usb-mouse -device VGA";
                    break;
                case "PPC":
                    defQemuParams = "-M mac99 -cpu g4 -accel tcg,thread=multi -smp 1";
                    break;
                case "I386":
                    defQemuParams = "-M pc -cpu qemu32,+avx -accel tcg,thread=multi -smp 4 -vga std -netdev user,id=usernet -device e1000,netdev=usernet";
                    break;
                default:
                    defQemuParams = "-M pc -cpu qemu64,+avx -accel tcg,thread=multi -smp 4 -vga std -netdev user,id=usernet -device e1000,netdev=usernet";
                    break;
            }
        } else {
            switch (MainSettingsManager.getArch(MainActivity.activity)) {
                case "ARM64":
                    defQemuParams = "-M virt -cpu cortex-a76 -net nic,model=e1000 -net user -device nec-usb-xhci -device usb-kbd -device usb-mouse -device VGA";
                    break;
                case "PPC":
                    defQemuParams = "-M mac99 -cpu g4 -smp 1";
                    break;
                case "I386":
                    defQemuParams = "-M pc -cpu qemu32,+avx -smp 4 -vga std -netdev user,id=usernet -device e1000,netdev=usernet";
                    break;
                default:
                    defQemuParams = "-M pc -cpu qemu64,+avx -smp 4 -vga std -netdev user,id=usernet -device e1000,netdev=usernet";
                    break;
            }
        }
        qemu.setText(defQemuParams);
    }

    private void importCVBI(String _filepath, String _filename) {
        LinearLayout custom = findViewById(R.id.custom);
        ImageView ivIcon = findViewById(R.id.ivIcon);
        if (_filepath.endsWith(".cvbi") || _filepath.endsWith(".cvbi.zip")) {
            //Error code: CR_CVBI1
            if (!VectrasApp.isFileExists(_filepath)) {
                if (getIntent().hasExtra("addromnow")) {
                    VectrasApp.oneDialog(getResources().getString(R.string.oops), getResources().getString(R.string.error_CR_CVBI1), false, true, this);
                } else {
                    VectrasApp.oneDialog(getResources().getString(R.string.oops), getResources().getString(R.string.error_CR_CVBI1), true, false, this);
                }
                return;
            }
            loadingPb.setVisibility(View.VISIBLE);
            custom.setVisibility(View.GONE);
            ivIcon.setEnabled(false);
            Thread t = new Thread() {
                public void run() {
                    FileInputStream zipFile = null;
                    try {
                        zipFile = (FileInputStream) getContentResolver().openInputStream((Uri.fromFile(new File(_filepath))));
                        File targetDirectory = new File(AppConfig.vmFolder + vmID);
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
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    UIUtils.toastLong(activity, e.toString());
                                }
                            };
                            activity.runOnUiThread(runnable);
                        } finally {
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    loadingPb.setVisibility(View.GONE);
                                    custom.setVisibility(View.VISIBLE);
                                    ivIcon.setEnabled(true);
                                    try {
                                        if (!VectrasApp.isFileExists(AppConfig.vmFolder + vmID + "/rom-data.json")) {
                                            String _getDiskFile = VMManager.quickScanDiskFileInFolder(AppConfig.vmFolder + vmID);
                                            if (!_getDiskFile.isEmpty()) {
                                                //Error code: CR_CVBI2
                                                if (getIntent().hasExtra("addromnow") && !addromnowdone) {
                                                    addromnowdone = true;
                                                    title.setText(getIntent().getStringExtra("romname"));
                                                    if (Objects.requireNonNull(getIntent().getStringExtra("romextra")).isEmpty()) {
                                                        setDefault();
                                                        drive.setText(_getDiskFile);
                                                    } else {
                                                        if (Objects.requireNonNull(getIntent().getStringExtra("romextra")).contains(getIntent().getStringExtra("finalromfilename"))) {
                                                            qemu.setText(Objects.requireNonNull(getIntent().getStringExtra("romextra")).replaceAll(getIntent().getStringExtra("finalromfilename"), "\"" + _getDiskFile + "\""));
                                                        } else {
                                                            drive.setText(_getDiskFile);
                                                            qemu.setText(getIntent().getStringExtra("romextra").replaceAll("OhnoIjustrealizeditsmidnightandIstillhavetodothis", AppConfig.vmFolder + vmID + "/"));
                                                        }
                                                    }
                                                    icon.setText(getIntent().getStringExtra("romicon"));
                                                    if (!Objects.requireNonNull(getIntent().getStringExtra("romicon")).isEmpty()) {
                                                        File imgFile = new File(getIntent().getStringExtra("romicon"));
                                                        if (imgFile.exists()) {
                                                            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                                                            ivIcon.setImageBitmap(myBitmap);
                                                        }
                                                    }
                                                } else {
                                                    if (Objects.requireNonNull(title.getText()).toString().isEmpty() || title.getText().toString().equals("New VM")) {
                                                        title.setText(_filename.replace(".cvbi", ""));
                                                    }
                                                    if (Objects.requireNonNull(qemu.getText()).toString().isEmpty()) {
                                                        setDefault();
                                                    }
                                                    drive.setText(_getDiskFile);
                                                }
                                                VMManager.setArch("X86_64", getApplicationContext());
                                                VectrasApp.oneDialog(getResources().getString(R.string.oops), getResources().getString(R.string.error_CR_CVBI2), true, false, CustomRomActivity.this);
                                            } else {
                                                //Error code: CR_CVBI3
                                                if (getIntent().hasExtra("addromnow")) {
                                                    VectrasApp.oneDialog(getResources().getString(R.string.oops), getResources().getString(R.string.error_CR_CVBI3), false, true, CustomRomActivity.this);
                                                } else {
                                                    VectrasApp.oneDialog(getResources().getString(R.string.oops), getResources().getString(R.string.error_CR_CVBI3), true, false, CustomRomActivity.this);
                                                }
                                            }
                                        } else {
                                            JSONObject jObj = new JSONObject(FileUtils.readFromFile(MainActivity.activity, new File(AppConfig.vmFolder + vmID + "/rom-data.json")));

                                            if (jObj.has("title") && !jObj.isNull("title")) {
                                                title.setText(jObj.getString("title"));
                                            }

                                            if (jObj.has("drive") && !jObj.isNull("drive")) {
                                                if (!jObj.getString("drive").isEmpty()) {
                                                    drive.setText(AppConfig.vmFolder + vmID + "/" + jObj.getString("drive"));
                                                }

                                            }

                                            if (jObj.has("qemu") && !jObj.isNull("qemu")) {
                                                if (!jObj.getString("qemu").isEmpty()) {
                                                    qemu.setText(jObj.getString("qemu").replaceAll("OhnoIjustrealizeditsmidnightandIstillhavetodothis", AppConfig.vmFolder + vmID + "/"));
                                                }
                                            }

                                            if (jObj.has("icon") && !jObj.isNull("icon")) {
                                                icon.setText(AppConfig.vmFolder + vmID + "/" + jObj.getString("icon"));
                                                ImageView ivIcon = findViewById(R.id.ivIcon);
                                                Bitmap bmImg = BitmapFactory.decodeFile(AppConfig.vmFolder
                                                        + vmID + "/" + jObj.getString("icon"));
                                                ivIcon.setImageBitmap(bmImg);
                                            }

                                            if (jObj.has("cdrom") && !jObj.isNull("cdrom")) {
                                                if (!jObj.getString("cdrom").isEmpty()) {
                                                    cdrom.setText(AppConfig.vmFolder + vmID + "/" + jObj.getString("cdrom"));
                                                }
                                            }

                                            if (jObj.has("arch") && !jObj.isNull("arch")) {
                                                VMManager.setArch(jObj.getString("arch"), getApplicationContext());
                                            } else {
                                                VMManager.setArch("x86_64", getApplicationContext());
                                            }

                                            VectrasApp.moveAFile(AppConfig.vmFolder + _filename.replace(".cvbi", ""), AppConfig.vmFolder + vmID);

                                            if (!jObj.has("drive") && !jObj.has("cdrom") && !jObj.has("qemu")) {
                                                VectrasApp.oneDialog(getResources().getString(R.string.problem_has_been_detected), getResources().getString(R.string.this_rom_is_missing_too_much_information), true, false, CustomRomActivity.this);
                                            }

                                            if (!jObj.has("versioncode")) {
                                                VectrasApp.oneDialog(getResources().getString(R.string.problem_has_been_detected), getResources().getString(R.string.this_rom_may_not_be_compatible), true, false, CustomRomActivity.this);
                                            }

                                            if (jObj.has("author") && !jObj.isNull("author") && jObj.has("desc") && !jObj.isNull("desc")) {
                                                UIUtils.UIAlert(activity, getResources().getString(R.string.from) + ": " + jObj.getString("author"), getResources().getString(R.string.description) + ":\n\n" + Html.fromHtml(jObj.getString("desc")));
                                            }
                                        }
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            };
                            activity.runOnUiThread(runnable);
                            try {
                                zis.close();
                            } catch (IOException e) {
                                UIUtils.toastLong(activity, e.toString());
                                throw new RuntimeException(e);
                            }
                        }
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            t.start();
        } else {
            if (getIntent().hasExtra("addromnow")) {
                VectrasApp.oneDialog(getResources().getString(R.string.problem_has_been_detected), getResources().getString(R.string.format_not_supported_please_select_file_with_format_cvbi), false, true, this);
            } else {
                VectrasApp.oneDialog(getResources().getString(R.string.problem_has_been_detected), getResources().getString(R.string.format_not_supported_please_select_file_with_format_cvbi), true, false, this);
            }
        }
    }

    private void selectedDiskFile(Uri _content_describer, boolean _addtodrive) {
        File selectedFilePath = new File(getPath(_content_describer));
        if (VMManager.isADiskFile(selectedFilePath.getPath())) {
            startProcessingHardDriveFile(_content_describer, _addtodrive);
        } else {
            alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
            alertDialog.setTitle(getResources().getString(R.string.problem_has_been_detected));
            alertDialog.setMessage(getResources().getString(R.string.file_format_is_not_supported));
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.continuetext), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    startProcessingHardDriveFile(_content_describer, _addtodrive);
                }
            });
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    alertDialog.dismiss();
                }
            });
            alertDialog.show();
        }
    }

    private void startProcessingHardDriveFile (Uri _content_describer, boolean _addtodrive) {
        LinearLayout custom = findViewById(R.id.custom);
        File selectedFilePath = new File(getPath(_content_describer));
        if (_addtodrive) {
            drive.setText(AppConfig.vmFolder + vmID + "/" + selectedFilePath.getName());
        }
        loadingPb.setVisibility(View.VISIBLE);
        custom.setVisibility(View.GONE);
        File romDir = new File(AppConfig.vmFolder + vmID);
        romDir.mkdirs();
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileInputStream File = null;
                try {
                    File = (FileInputStream) getContentResolver().openInputStream(_content_describer);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                try {
                    try {
                        OutputStream out = new FileOutputStream(new File(AppConfig.vmFolder + vmID + "/" + selectedFilePath.getName()));
                        try {
                            // Transfer bytes from in to out
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = File.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                        } finally {
                            out.close();
                        }
                    } finally {
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                loadingPb.setVisibility(View.GONE);
                                custom.setVisibility(View.VISIBLE);
                            }
                        };
                        activity.runOnUiThread(runnable);
                        File.close();
                    }
                } catch (IOException e) {
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            loadingPb.setVisibility(View.GONE);
                            custom.setVisibility(View.VISIBLE);
                            UIUtils.UIAlert(activity, "error", e.toString());
                        }
                    };
                    activity.runOnUiThread(runnable);
                }
            }
        }).start();
    }

    private String cdromPatternCompile() {
        //Matches any string of characters that does not contain single quotes
        if (MainSettingsManager.getArch(activity).equals("ARM64")) {
            return  "-device usb-storage,drive=cdrom -drive if=none,id=cdrom,format=raw,media=cdrom,file='([^']*)'";
        } else if (MainSettingsManager.getIfType(activity).isEmpty()) {
            return  "-cdrom '([^']*)'";
        } else {
            return "-drive index=1,media=cdrom,file='([^']*)'";
        }
    }

    private String cdromPatternCompile2() {
        //Matches any string of characters, but will try to match the shortest string possible
        if (MainSettingsManager.getArch(activity).equals("ARM64")) {
            return  "-device usb-storage,drive=cdrom -drive if=none,id=cdrom,format=raw,media=cdrom,file='(.*?)'";
        } else if (MainSettingsManager.getIfType(activity).isEmpty()) {
            return  "-cdrom '(.*?)'";
        } else {
            return "-drive index=1,media=cdrom,file='(.*?)'";
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!created && !modify) {
            VectrasApp.deleteDirectory(AppConfig.vmFolder + vmID);
        }
        modify = false;
    }

    public void onDestroy() {
        if (!created && !modify) {
            VectrasApp.deleteDirectory(AppConfig.vmFolder + vmID);
        }
        modify = false;
        super.onDestroy();
    }
}
