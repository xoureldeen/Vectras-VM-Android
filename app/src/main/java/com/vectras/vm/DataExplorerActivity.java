package com.vectras.vm;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;

import android.app.ListActivity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.vectras.qemu.Config;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.UIUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DataExplorerActivity extends AppCompatActivity {

    public AppCompatActivity activity;
    private List<String> item = null;

    private List<String> path = null;
    public String currentDirPath;

    public ListView lv;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, 0, 0, "import files").setShortcut('3', 'c').setIcon(R.drawable.input_circle).setShowAsAction(1);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case 0:
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

    /**
     * Called when the activity is first created.
     */

    @Override

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        activity = this;
        UIController.edgeToEdge(this);
        setContentView(R.layout.activity_data_explorer);
        UIController.setOnApplyWindowInsetsListener(findViewById(R.id.main));

        loadingPb = findViewById(R.id.loadingPb);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        lv = findViewById(R.id.listview1);

        getDir(AppConfig.maindirpath);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                File file = new File(path.get(position));


                if (file.isDirectory()) {

                    if (file.canRead())

                        getDir(path.get(position));

                    else {

                        new AlertDialog.Builder(activity)

                                .setIcon(R.drawable.round_folder_24)

                                .setTitle("[" + file.getName() + "] folder can't be read!")

                                .setPositiveButton("OK",

                                        new DialogInterface.OnClickListener() {


                                            @Override

                                            public void onClick(DialogInterface dialog, int which) {

                                                // TODO Auto-generated method stub

                                            }

                                        }).show();

                    }

                } else {

                    new AlertDialog.Builder(activity)

                            .setTitle(file.getName())

                            .setPositiveButton("DELETE",

                                    new DialogInterface.OnClickListener() {


                                        @Override

                                        public void onClick(DialogInterface dialog, int which) {

                                            file.delete();
                                            getDir(AppConfig.maindirpath);

                                        }

                                    }).setNegativeButton("OPEN",

                                    new DialogInterface.OnClickListener() {


                                        @Override

                                        public void onClick(DialogInterface dialog, int which) {

                                            open_file(file.getPath());

                                        }

                                    }).setNeutralButton("COPY PATH",

                                    new DialogInterface.OnClickListener() {


                                        @Override

                                        public void onClick(DialogInterface dialog, int which) {
                                            ClipboardManager clipboardManager = (ClipboardManager)
                                                    activity.getSystemService(Context.CLIPBOARD_SERVICE);
                                            ClipData clipData = ClipData.newPlainText("nonsense_data",
                                                    file.getPath());
                                            clipboardManager.setPrimaryClip(clipData);
                                            UIUtils.toastShort(activity, "Copied to clipboard successfully!");
                                        }

                                    }).show();

                }

            }
        });
    }

    public String getPath(Uri uri) {
        return FileUtils.getPath(activity, uri);
    }

    private void getDir(String dirPath) {

        item = new ArrayList<String>();

        path = new ArrayList<String>();

        currentDirPath = dirPath;

        File f = new File(dirPath);

        File[] files = f.listFiles();


        item.add("../");

        path.add(f.getParent());

        for (int i = 0; i < files.length; i++) {

            File file = files[i];

            path.add(file.getPath());

            if (file.isDirectory())

                item.add(file.getName() + "/");

            else

                item.add(file.getName());

        }


        ArrayAdapter<String> fileList =

                new ArrayAdapter<String>(activity, R.layout.row, item);

        lv.setAdapter(fileList);

    }

    public void open_file(String location) {
        // Create a new AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Set the dialog's title and message
        builder.setTitle(new File(location).getName())
                .setMessage("Edit");

        // Create an EditText for the user to input data
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        // Add the EditText to the dialog
        builder.setView(input);

        input.setText(FileUtils.readFromFile(activity, new File(location)));

        // Set a positive button and handle the button click event
        builder.setPositiveButton("SAVE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = String.valueOf(input.getText());
                FileUtils.writeToFile(text, new File(location), activity);
            }
        });

        // Set a positive button and handle the button click event
        builder.setNeutralButton("COPY CONTENT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = String.valueOf(input.getText());
                ClipboardManager clipboardManager = (ClipboardManager)
                        activity.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("nonsense_data",
                        input.getText());
                clipboardManager.setPrimaryClip(clipData);
                UIUtils.toastShort(activity, "Copied to clipboard successfully!");
            }
        });

        // Optionally, set a negative button
        builder.setNegativeButton("DISCARD", null);

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public ProgressBar loadingPb;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK) {
            Uri content_describer = data.getData();
            File selectedFilePath = new File(getPath(content_describer));
            loadingPb.setVisibility(View.VISIBLE);

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
                            OutputStream out = new FileOutputStream(new File(currentDirPath + "/" + selectedFilePath.getName()));
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
                                }
                            };
                            activity.runOnUiThread(runnable);
                            File.close();
                            getDir(currentDirPath);
                        }
                    } catch (IOException e) {
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                loadingPb.setVisibility(View.GONE);
                            }
                        };
                        activity.runOnUiThread(runnable);
                        UIUtils.UIAlert(activity, e.toString(), "error");
                    }
                }
            }).start();
        }
    }


}