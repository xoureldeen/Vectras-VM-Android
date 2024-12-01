package com.vectras.vm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.UIUtils;

import org.checkerframework.checker.guieffect.qual.UI;
import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ExportRomActivity extends AppCompatActivity {

    private LinearLayout linearone;
    private LinearLayout linearload;
    private LinearLayout lineardone;
    private LinearLayout linearerror;
    private TextView textviewfilename;
    private TextView textviewerrorcontent;
    private EditText editauthor;
    private EditText editdesc;
    public static int pendingPosition = 0;
    public static HashMap<String, Object> mapForGetData = new HashMap<>();
    public static ArrayList<HashMap<String, Object>> listmapForGetData = new ArrayList<>();
    private SharedPreferences data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_export_rom);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        linearone = findViewById(R.id.linearall);
        linearload = findViewById(R.id.linearload);
        lineardone = findViewById(R.id.lineardone);
        linearerror = findViewById(R.id.linearerror);
        textviewfilename = findViewById(R.id.textviewfilename);
        textviewerrorcontent = findViewById(R.id.textviewerrorcontent);
        editauthor = findViewById(R.id.edittext1);
        editdesc = findViewById(R.id.edittext2);

        Button buttondone;
        buttondone = findViewById(R.id.materialbutton1);
        buttondone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editauthor.setEnabled(false);
                editdesc.setEnabled(false);
                editauthor.setEnabled(true);
                editdesc.setEnabled(true);
                startCreateCVBI();
            }
        });

        Button buttonexit;
        buttonexit = findViewById(R.id.buttonexit);
        buttonexit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button buttonexit2;
        buttonexit2 = findViewById(R.id.buttonexit2);
        buttonexit2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        data= getSharedPreferences("data", Activity.MODE_PRIVATE);

        editauthor.setText(data.getString("author", ""));
        editdesc.setText(data.getString("desc", ""));
    }

    @Override
    public void onPause() {
        super.onPause();
        data.edit().putString("author", editauthor.getText().toString()).commit();
        data.edit().putString("desc", editdesc.getText().toString()).commit();
    }

    private void UIControler(int _status, String _content) {
        if (_status == 0) {
            linearone.setVisibility(View.GONE);
            linearload.setVisibility(View.VISIBLE);
        } else if (_status == 1) {
            linearone.setVisibility(View.GONE);
            linearload.setVisibility(View.GONE);
            lineardone.setVisibility(View.VISIBLE);
            textviewfilename.setText(getResources().getString(R.string.saved_in) + " " +_content);
        } else if (_status == 2) {
            linearone.setVisibility(View.GONE);
            linearload.setVisibility(View.GONE);
            lineardone.setVisibility(View.GONE);
            linearerror.setVisibility(View.VISIBLE);
            textviewerrorcontent.setText(_content);
        }
    }

    private void startCreateCVBI() {
        UIControler(0, "");

        File vDir = new File(AppConfig.maindirpath + "cvbi");
        if (!vDir.exists()) {
            vDir.mkdirs();
        }

        listmapForGetData.clear();
        mapForGetData.clear();

        listmapForGetData = new Gson().fromJson(VectrasApp.readFile(AppConfig.romsdatajson), new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
        if (listmapForGetData.get(pendingPosition).containsKey("imgName")) {
            mapForGetData.put("imgName", Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgName")).toString());
        } else {
            mapForGetData.put("imgName", "");
        }
        if (listmapForGetData.get(pendingPosition).containsKey("imgIcon")) {
            mapForGetData.put("imgIcon", Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgIcon")).toString());
        } else {
            mapForGetData.put("imgIcon", "");
        }
        if (listmapForGetData.get(pendingPosition).containsKey("imgPath")) {
            mapForGetData.put("imgPath", Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgPath")).toString());
        } else {
            mapForGetData.put("imgPath", "");
        }
        if (listmapForGetData.get(pendingPosition).containsKey("imgCdrom")) {
            mapForGetData.put("imgCdrom", Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgCdrom")).toString());
        } else {
            mapForGetData.put("imgCdrom", "");
        }
        if (listmapForGetData.get(pendingPosition).containsKey("imgExtra")) {
            mapForGetData.put("imgExtra", Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgExtra")).toString());
        } else {
            mapForGetData.put("imgExtra", "");
        }
        if (listmapForGetData.get(pendingPosition).containsKey("imgArch")) {
            mapForGetData.put("imgArch", Objects.requireNonNull(listmapForGetData.get(pendingPosition).get("imgArch")).toString());
        } else {
            mapForGetData.put("imgArch", "");
        }
        if (editauthor.getText().toString().isEmpty()) {
            mapForGetData.put("author", "Unknow");
        } else {
            mapForGetData.put("author", editauthor.getText().toString());
        }
        if (editdesc.getText().toString().isEmpty()) {
            mapForGetData.put("desc", "Empty.");
        } else {
            mapForGetData.put("desc", editdesc.getText().toString());
        }


        VectrasApp.writeToFile(new File(String.valueOf(getApplicationContext().getExternalFilesDir("data"))).getPath(), "rom-data.json", new Gson().toJson(mapForGetData));

        Thread t = new Thread() {
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if ((!Objects.requireNonNull(mapForGetData.get("imgIcon")).toString().isEmpty()) && (!Objects.requireNonNull(mapForGetData.get("imgPath")).toString().isEmpty()) && (!Objects.requireNonNull(mapForGetData.get("imgCdrom")).toString().isEmpty())) {
                                ZipEntrySource[] addedEntries = new ZipEntrySource[]{
                                        new FileSource("/" + new File(Objects.requireNonNull(mapForGetData.get("imgPath")).toString()).getName(), new File(Objects.requireNonNull(mapForGetData.get("imgPath")).toString())),
                                        new FileSource("/" + new File(Objects.requireNonNull(mapForGetData.get("imgIcon")).toString()).getName(), new File(Objects.requireNonNull(mapForGetData.get("imgIcon")).toString())),
                                        new FileSource("/" + new File(Objects.requireNonNull(mapForGetData.get("imgCdrom")).toString()).getName(), new File(Objects.requireNonNull(mapForGetData.get("imgCdrom")).toString())),
                                        new FileSource("/" + new File(getApplicationContext().getExternalFilesDir("data") + "/rom-data.json").getName(), new File(getApplicationContext().getExternalFilesDir("data") + "/rom-data.json"))
                                };
                                ZipUtil.pack(addedEntries, new File(FileUtils.getExternalFilesDirectory(getApplicationContext()).getPath() + "/cvbi/" + Objects.requireNonNull(mapForGetData.get("imgName")).toString() + ".cvbi"));
                            } else if ((!Objects.requireNonNull(mapForGetData.get("imgIcon")).toString().isEmpty()) && (!Objects.requireNonNull(mapForGetData.get("imgPath")).toString().isEmpty())) {
                                ZipEntrySource[] addedEntries = new ZipEntrySource[]{
                                        new FileSource("/" + new File(Objects.requireNonNull(mapForGetData.get("imgPath")).toString()).getName(), new File(Objects.requireNonNull(mapForGetData.get("imgPath")).toString())),
                                        new FileSource("/" + new File(Objects.requireNonNull(mapForGetData.get("imgIcon")).toString()).getName(), new File(Objects.requireNonNull(mapForGetData.get("imgIcon")).toString())),
                                        new FileSource("/" + new File(getApplicationContext().getExternalFilesDir("data") + "/rom-data.json").getName(), new File(getApplicationContext().getExternalFilesDir("data") + "/rom-data.json"))
                                };
                                ZipUtil.pack(addedEntries, new File(FileUtils.getExternalFilesDirectory(getApplicationContext()).getPath() + "/cvbi/" + Objects.requireNonNull(mapForGetData.get("imgName")).toString() + ".cvbi"));
                            } else if ((!Objects.requireNonNull(mapForGetData.get("imgIcon")).toString().isEmpty()) && (!Objects.requireNonNull(mapForGetData.get("imgCdrom")).toString().isEmpty())) {
                                ZipEntrySource[] addedEntries = new ZipEntrySource[]{
                                        new FileSource("/" + new File(Objects.requireNonNull(mapForGetData.get("imgIcon")).toString()).getName(), new File(Objects.requireNonNull(mapForGetData.get("imgIcon")).toString())),
                                        new FileSource("/" + new File(Objects.requireNonNull(mapForGetData.get("imgCdrom")).toString()).getName(), new File(Objects.requireNonNull(mapForGetData.get("imgCdrom")).toString())),
                                        new FileSource("/" + new File(getApplicationContext().getExternalFilesDir("data") + "/rom-data.json").getName(), new File(getApplicationContext().getExternalFilesDir("data") + "/rom-data.json"))
                                };
                                ZipUtil.pack(addedEntries, new File(FileUtils.getExternalFilesDirectory(getApplicationContext()).getPath() + "/cvbi/" + Objects.requireNonNull(mapForGetData.get("imgName")).toString() + ".cvbi"));
                            } else if ((!Objects.requireNonNull(mapForGetData.get("imgPath")).toString().isEmpty()) && (!Objects.requireNonNull(mapForGetData.get("imgCdrom")).toString().isEmpty())) {
                                ZipEntrySource[] addedEntries = new ZipEntrySource[]{
                                        new FileSource("/" + new File(Objects.requireNonNull(mapForGetData.get("imgPath")).toString()).getName(), new File(Objects.requireNonNull(mapForGetData.get("imgPath")).toString())),
                                        new FileSource("/" + new File(Objects.requireNonNull(mapForGetData.get("imgCdrom")).toString()).getName(), new File(Objects.requireNonNull(mapForGetData.get("imgCdrom")).toString())),
                                        new FileSource("/" + new File(getApplicationContext().getExternalFilesDir("data") + "/rom-data.json").getName(), new File(getApplicationContext().getExternalFilesDir("data") + "/rom-data.json"))
                                };
                                ZipUtil.pack(addedEntries, new File(FileUtils.getExternalFilesDirectory(getApplicationContext()).getPath() + "/cvbi/" + Objects.requireNonNull(mapForGetData.get("imgName")).toString() + ".cvbi"));
                            } else if (!Objects.requireNonNull(mapForGetData.get("imgIcon")).toString().isEmpty()) {
                                ZipEntrySource[] addedEntries = new ZipEntrySource[]{
                                        new FileSource("/" + new File(Objects.requireNonNull(mapForGetData.get("imgIcon")).toString()).getName(), new File(Objects.requireNonNull(mapForGetData.get("imgIcon")).toString())),
                                        new FileSource("/" + new File(getApplicationContext().getExternalFilesDir("data") + "/rom-data.json").getName(), new File(getApplicationContext().getExternalFilesDir("data") + "/rom-data.json"))
                                };
                                ZipUtil.pack(addedEntries, new File(FileUtils.getExternalFilesDirectory(getApplicationContext()).getPath() + "/cvbi/" + Objects.requireNonNull(mapForGetData.get("imgName")).toString() + ".cvbi"));
                            } else if (!Objects.requireNonNull(mapForGetData.get("imgPath")).toString().isEmpty()) {
                                ZipEntrySource[] addedEntries = new ZipEntrySource[]{
                                        new FileSource("/" + new File(Objects.requireNonNull(mapForGetData.get("imgPath")).toString()).getName(), new File(Objects.requireNonNull(mapForGetData.get("imgPath")).toString())),
                                        new FileSource("/" + new File(getApplicationContext().getExternalFilesDir("data") + "/rom-data.json").getName(), new File(getApplicationContext().getExternalFilesDir("data") + "/rom-data.json"))
                                };
                                ZipUtil.pack(addedEntries, new File(FileUtils.getExternalFilesDirectory(getApplicationContext()).getPath() + "/cvbi/" + Objects.requireNonNull(mapForGetData.get("imgName")).toString() + ".cvbi"));
                            } else if (!Objects.requireNonNull(mapForGetData.get("imgCdrom")).toString().isEmpty()) {
                                ZipEntrySource[] addedEntries = new ZipEntrySource[]{
                                        new FileSource("/" + new File(Objects.requireNonNull(mapForGetData.get("imgCdrom")).toString()).getName(), new File(Objects.requireNonNull(mapForGetData.get("imgCdrom")).toString())),
                                        new FileSource("/" + new File(getApplicationContext().getExternalFilesDir("data") + "/rom-data.json").getName(), new File(getApplicationContext().getExternalFilesDir("data") + "/rom-data.json"))
                                };
                                ZipUtil.pack(addedEntries, new File(FileUtils.getExternalFilesDirectory(getApplicationContext()).getPath() + "/cvbi/" + Objects.requireNonNull(mapForGetData.get("imgName")).toString() + ".cvbi"));
                            } else {
                                ZipEntrySource[] addedEntries = new ZipEntrySource[]{
                                        new FileSource("/" + new File(getApplicationContext().getExternalFilesDir("data") + "/rom-data.json").getName(), new File(getApplicationContext().getExternalFilesDir("data") + "/rom-data.json"))
                                };
                                ZipUtil.pack(addedEntries, new File(FileUtils.getExternalFilesDirectory(getApplicationContext()).getPath() + "/cvbi/" + Objects.requireNonNull(mapForGetData.get("imgName")).toString() + ".cvbi"));
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    UIControler(1, FileUtils.getExternalFilesDirectory(getApplicationContext()).getPath() + "/cvbi/" + Objects.requireNonNull(mapForGetData.get("imgName")).toString() + ".cvbi");
                                }
                            });
                        } catch (Exception e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    UIControler(2, e.toString());
                                }
                            });
                        }
                    }
                });

            }
        };
        t.start();
        return;
    }
}