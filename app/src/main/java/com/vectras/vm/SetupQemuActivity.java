package com.vectras.vm;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static android.content.Intent.ACTION_VIEW;

import com.termux.app.TermuxService;
import static com.vectras.vm.utils.UIUtils.UIAlert;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.widget.BaseAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vterm.view.ZoomableTextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class SetupQemuActivity extends AppCompatActivity implements View.OnClickListener {
    Activity activity;
    private final String TAG = "SetupQemuActivity";
    ZoomableTextView vterm;
    MaterialButton inBtn;
    ProgressBar progressBar;
    TextView title;

    LinearLayout linearload;
    LinearLayout linearcannotconnecttoserver;
    Button buttontryconnectagain;
    LinearLayout linearsimplesetupui;
    LinearLayout linearstartsetup;
    MaterialButton buttonautosetup;
    MaterialButton buttonmanualsetup;
    LinearLayout linearsettingup;
    TextView textviewsettingup;
    LinearLayout linearsetupfailed;
    MaterialButton buttonsetuptryagain;
    MaterialButton buttonsetupshowlog;
    TextView textviewshowadvancedsetup;
    TextView textviewhideadvancedsetup;
    Spinner spinnerselectmirror;
    LinearLayout linearwelcome;
    Button buttonletstart;

    AlertDialog alertDialog;
    private boolean settingup = false;
    private boolean libprooterror = false;

    private RequestNetwork net;
    private RequestNetwork.RequestListener _net_request_listener;
    private String contentJSON = "";
    private HashMap<String, Object> mmap = new HashMap<>();
    private String bootstrapfilelink = "";
    private ArrayList<HashMap<String, String>> listmapForSelectMirrors = new ArrayList<>();
    private String selectedMirrorCommand = "";
    private String selectedMirrorLocation = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIController.edgeToEdge(this);
        setContentView(R.layout.activity_setup_qemu);
        UIController.setOnApplyWindowInsetsListener(findViewById(R.id.main));
        activity = this;

        linearload = findViewById(R.id.linearload);
        linearcannotconnecttoserver = findViewById(R.id.linearcannotconnecttoserver);
        buttontryconnectagain = findViewById(R.id.buttontryconnectagain);
        linearsimplesetupui = findViewById(R.id.linearsimplesetupui);
        linearstartsetup = findViewById(R.id.linearstartsetup);
        buttonautosetup = findViewById(R.id.buttonautosetup);
        buttonmanualsetup = findViewById(R.id.buttonmanualsetup);
        linearsettingup = findViewById(R.id.linearsettingup);
        textviewsettingup = findViewById(R.id.textviewsettingup);
        linearsetupfailed = findViewById(R.id.linearsetupfailed);
        buttonsetuptryagain = findViewById(R.id.buttonsetuptryagain);
        buttonsetupshowlog = findViewById(R.id.buttonsetupshowlog);
        textviewshowadvancedsetup = findViewById(R.id.textviewshowadvancedsetup);
        textviewhideadvancedsetup = findViewById(R.id.textviewhideadvancedsetup);
        spinnerselectmirror = findViewById(R.id.spinnerselectmirror);
        linearwelcome = findViewById(R.id.linearwelcome);
        buttonletstart = findViewById(R.id.buttonletstart);

        buttontryconnectagain.setOnClickListener(this);
        buttonautosetup.setOnClickListener(this);
        buttonmanualsetup.setOnClickListener(this);
        buttonsetuptryagain.setOnClickListener(this);
        buttonsetupshowlog.setOnClickListener(this);
        textviewshowadvancedsetup.setOnClickListener(this);
        textviewhideadvancedsetup.setOnClickListener(this);
        buttonletstart.setOnClickListener(this);

        progressBar = findViewById(R.id.progressBar);

        vterm = findViewById(R.id.tvTerminalOutput);

        inBtn = findViewById(R.id.btnInstall);
        title = findViewById(R.id.title);
        inBtn.setOnClickListener(this);
        setupSpiner();

        tarPath = getExternalFilesDir("data") + "/data.tar.gz";
        VectrasApp.prepareDataForAppConfig(activity);

        spinnerselectmirror.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMirrorCommand = Objects.requireNonNull(listmapForSelectMirrors.get(position).get("mirror"));
                selectedMirrorLocation = Objects.requireNonNull(listmapForSelectMirrors.get(position).get("location"));
                MainSettingsManager.setSelectedMirror(activity, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        net = new RequestNetwork(this);
        _net_request_listener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                linearload.setVisibility(View.GONE);
                contentJSON = response;
                if (VectrasApp.checkJSONMapIsNormalFromString(contentJSON)) {
                    mmap.clear();
                    mmap= new Gson().fromJson(contentJSON, new TypeToken<HashMap<String, Object>>(){}.getType());
                    if(mmap.containsKey("arm64") && mmap.containsKey("x86_64")) {
                        if (Build.SUPPORTED_ABIS[0].contains("arm64")) {
                            bootstrapfilelink = mmap.get("arm64").toString();
                        } else {
                            bootstrapfilelink = mmap.get("x86_64").toString();
                        }
                        linearcannotconnecttoserver.setVisibility(View.GONE);
                    }
                    if (AppConfig.needreinstallsystem) {
                        AppConfig.needreinstallsystem = false;
                        if (!MainSettingsManager.getsetUpWithManualSetupBefore(SetupQemuActivity.this)) {
                            if (AppConfig.getSetupFiles().contains("arm64-v8a") || AppConfig.getSetupFiles().contains("x86_64")) {
                                setupVectras64();
                            } else {
                                setupVectras32();
                            }
                            textviewsettingup.setText(getResources().getString(R.string.reinstalling));
                            simpleSetupUIControler(1);
                        }
                    }
                } else {
                    linearload.setVisibility(View.GONE);
                }
            }

            @Override
            public void onErrorResponse(String tag, String message) {
                linearload.setVisibility(View.GONE);
            }
        };
        
        String filesDir = activity.getFilesDir().getAbsolutePath();
        File distroDir = new File(filesDir + "/distro");
        File binDir = new File(distroDir + "/bin");
        if (!binDir.exists()) {
            extractBootstraps();

        }
    }

    public void extractBootstraps() {
        String filesDir = getFilesDir().getAbsolutePath();
        String abi = getDeviceAbi();
        String assetPath = "bootstrap/" + abi + ".tar";
        //String apkLoaderAssetPath = "bootstrap/loader.apk";
        String extractedFilePath = filesDir + "/" + abi + ".tar";
        //String apkLoaderextractedFilePath = TermuxService.PREFIX_PATH + "/libexec/termux-x11/loader.apk";

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Extracting data...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new AsyncTask<Void, Void, Boolean>() {
            String errorMessage = null;

            @Override
            protected Boolean doInBackground(Void... voids) {
                // Step 1: Copy asset to filesDir
                if (!copyAssetToFile(assetPath, extractedFilePath)) {
                    errorMessage = "Failed to copy asset file.";
                    return false;
                }

                // Step 2: Run tar extraction
                String[] cmdline = {"tar", "xf", extractedFilePath, "-C", filesDir};
                Process process = null;
                try {
                    process = Runtime.getRuntime().exec(cmdline);

                    // Capture standard error output (stderr)
                    BufferedReader errorReader =
                            new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    StringBuilder errorOutput = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }
                    errorReader.close();

                    // Wait for the process to complete
                    int exitCode = process.waitFor();

                    // If there was any output in stderr, treat it as an error
                    if (exitCode != 0 || errorOutput.length() > 0) {
                        errorMessage =
                                errorOutput.toString().isEmpty()
                                        ? "Unknown error"
                                        : errorOutput.toString();
                        return false;
                    }
                    return true;
                } catch (IOException | InterruptedException e) {
                    errorMessage = e.getMessage();
                    return false;
                } finally {
                    if (process != null) {
                        process.destroy();
                    }
                    //VectrasApp.deleteDirectory(apkLoaderextractedFilePath);
                    //if (!copyAssetToFile(apkLoaderAssetPath, apkLoaderextractedFilePath)) {
                        //errorMessage = "Failed to copy loader.apk file.";
                    //}
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                progressDialog.dismiss();
                if (success) {
                    Toast.makeText(
                                    getApplicationContext(),
                                    R.string.extraction_complete,
                                    Toast.LENGTH_SHORT)
                            .show();
                } else {
                    new AlertDialog.Builder(activity)
                            .setTitle("Extraction Failed")
                            .setMessage("Error: " + errorMessage)
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        }.execute();
    }

    /** Copies the specified asset to the given file path. */
    private boolean copyAssetToFile(String assetPath, String outputPath) {
        try (InputStream in = getAssets().open(assetPath);
                OutputStream out = new FileOutputStream(outputPath)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Determines the ABI of the device. */
    private String getDeviceAbi() {
        return Build.SUPPORTED_ABIS[0];
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkpermissions();
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnInstall) {
            File tarGZ = new File(tarPath);
            if (tarGZ.exists()) {
                tarGZ.delete();
            }
            alertDialog.show();
        } else if (id == R.id.buttonautosetup) {
            if (AppConfig.getSetupFiles().contains("arm64-v8a") || AppConfig.getSetupFiles().contains("x86_64")) {
                setupVectras64();
            } else {
                setupVectras32();
            }
            simpleSetupUIControler(1);
        } else if (id == R.id.buttonmanualsetup) {
            Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");

            // Optionally, specify a URI for the file that should appear in the
            // system file picker when it loads.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
            }

            startActivityForResult(intent, 1001);
        } else if (id == R.id.buttonsetuptryagain) {
            simpleSetupUIControler(0);
        } else if (id == R.id.buttonsetupshowlog) {
            linearsimplesetupui.setVisibility(View.GONE);
        } else if (id == R.id.textviewshowadvancedsetup) {
            linearsimplesetupui.setVisibility(View.GONE);
            if (linearstartsetup.getVisibility() == View.VISIBLE) {
                alertDialog.show();
            }
        } else if (id == R.id.buttontryconnectagain) {
            linearload.setVisibility(View.VISIBLE);
            net.startRequestNetwork(RequestNetworkController.GET,AppConfig.bootstrapfileslink,"anbui",_net_request_listener);
        } else if (id == R.id.textviewhideadvancedsetup) {
            linearsimplesetupui.setVisibility(View.VISIBLE);
            alertDialog.dismiss();
        } else if (id == R.id.buttonletstart) {
            linearwelcome.setVisibility(View.GONE);
            net.startRequestNetwork(RequestNetworkController.GET,AppConfig.bootstrapfileslink,"anbui",_net_request_listener);
        }
    }

    String tarPath;

    // Function to append text and automatically scroll to bottom
    @SuppressLint("SetTextI18n")
    private void appendTextAndScroll(String textToAdd) {
        ScrollView scrollView = findViewById(R.id.scrollView);

        // Update the text
        vterm.append(textToAdd);

        if (textToAdd.contains("xssFjnj58Id")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else if (textToAdd.contains("libproot.so --help")){
            libprooterror = true;
        }

        if (textToAdd.contains("Starting setup...")) {
            title.setText("Getting ready for you...");
            textviewsettingup.setText(R.string.getting_ready_for_you_please_don_t_disconnect_the_network);
        } else if (textToAdd.contains("fetch http")) {
            title.setText(getString(R.string.connecting_to_mirror_in) + "\n" + selectedMirrorLocation + "...");
            textviewsettingup.setText(getString(R.string.connecting_to_mirror_in) + "\n" + selectedMirrorLocation + "...");
        } else if (textToAdd.contains("Installing packages...")) {
            title.setText(R.string.it_won_t_take_long);
            textviewsettingup.setText(R.string.completed_10_it_won_t_take_long);
        } else if (textToAdd.contains("(50/")) {
            textviewsettingup.setText(R.string.completed_20_it_won_t_take_long);
        } else if (textToAdd.contains("100/")) {
            textviewsettingup.setText(R.string.completed_30_it_won_t_take_long);
        } else if (textToAdd.contains("150/")) {
            textviewsettingup.setText(R.string.completed_40_it_won_t_take_long);
        } else if (textToAdd.contains("200/")) {
            textviewsettingup.setText(R.string.completed_50_it_won_t_take_long);
        } else if (textToAdd.contains("Downloading Qemu...")) {
            title.setText(R.string.don_t_disconnect);
            textviewsettingup.setText(R.string.completed_75_don_t_disconnect);
        } else if (textToAdd.contains("Installing Qemu...")) {
            title.setText(R.string.keep_it_up);
            textviewsettingup.setText(R.string.completed_80_keep_it_up);
        } else if (textToAdd.contains("qemu-system")) {
            textviewsettingup.setText(R.string.completed_95_keep_it_up);
        } else if (textToAdd.contains("Just a sec...")) {
            title.setText(R.string.almost_there);
            textviewsettingup.setText(getString(R.string.almost_there));
        }

        // Scroll to the bottom
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    public void onBackPressed() {
        super.onBackPressed();
        return;
    }

    // Method to execute the shell command
    public void executeShellCommand(String userCommand) {
        new Thread(() -> {
            try {
                // Setup the process builder to start PRoot with environmental variables and commands
                ProcessBuilder processBuilder = new ProcessBuilder();

                // Adjust these environment variables as necessary for your app
                String filesDir = activity.getFilesDir().getAbsolutePath();
                String nativeLibDir = activity.getApplicationInfo().nativeLibraryDir;

                File tmpDir = new File(activity.getFilesDir(), "usr/tmp");

                // Setup environment for the PRoot process
                processBuilder.environment().put("PROOT_TMP_DIR", tmpDir.getAbsolutePath());
                
                processBuilder.environment().put("HOME", "/root");
                processBuilder.environment().put("USER", "root");
                processBuilder.environment().put("PATH", "/bin:/usr/bin:/sbin:/usr/sbin");
                processBuilder.environment().put("TERM", "xterm-256color");
                processBuilder.environment().put("TMPDIR", tmpDir.getAbsolutePath());
                processBuilder.environment().put("SHELL", "/bin/sh");

                String[] prootCommand = {
                        TermuxService.PREFIX_PATH + "/bin/proot", // PRoot binary path
                        "--kill-on-exit",
                        "--link2symlink",
                        "-0",
                        "-r", filesDir + "/distro", // Path to the rootfs
                        "-b", "/dev",
                        "-b", "/proc",
                        "-b", "/sys",
                        "-b", "/sdcard",
                        "-b", "/storage",
                        "-b", "/data",
                        "-w", "/root",
                        "/bin/sh",
                        "--login"// The shell to execute inside PRoot
                };

                processBuilder.command(prootCommand);
                Process process = processBuilder.start();
                // Get the input and output streams of the process
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                // Send user command to PRoot
                writer.write(userCommand);
                writer.newLine();
                writer.flush();
                writer.close();

                // Read the input stream for the output of the command
                String line;
                while ((line = reader.readLine()) != null) {
                    final String outputLine = line;
                    activity.runOnUiThread(() -> appendTextAndScroll(outputLine + "\n"));
                }

                // Read any errors from the error stream
                while ((line = errorReader.readLine()) != null) {
                    final String errorLine = line;
                    activity.runOnUiThread(() -> appendTextAndScroll(errorLine + "\n"));
                }

                // Clean up
                reader.close();
                errorReader.close();

                // Wait for the process to finish
                process.waitFor();

                // Wait for the process to finish
                int exitValue = process.waitFor();

                // Check if the exit value indicates an error
                if (exitValue != 0) {
                    // If exit value is not zero, display a toast message
                    String toastMessage = "Command failed with exit code: " + exitValue;
                    activity.runOnUiThread(() -> {
                        appendTextAndScroll("Error: " + toastMessage + "\n");
                        Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG).show();
                        inBtn.setVisibility(View.VISIBLE);
                        title.setText("Failed!");
                        simpleSetupUIControler(2);
                    });
                    if (libprooterror) {
                        AlertDialog alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
                        alertDialog.setTitle(getResources().getString(R.string.oops));
                        alertDialog.setMessage(getResources().getString(R.string.a_serious_problem_has_occurred));
                        alertDialog.setCancelable(false);
                        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.join_our_community), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.setAction(ACTION_VIEW);
                                intent.setData(Uri.parse(AppConfig.community));
                                startActivity(intent);
                            }
                        });
                        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.close), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });

                        alertDialog.show();
                    }
                }
            } catch (IOException | InterruptedException e) {
                // Handle exceptions by printing the stack trace in the terminal output
                final String errorMessage = e.getMessage();
                activity.runOnUiThread(() -> {
                    appendTextAndScroll("Error: " + errorMessage + "\n");
                    Toast.makeText(activity, "Error executing command: " + errorMessage, Toast.LENGTH_LONG).show();
                    inBtn.setVisibility(View.VISIBLE);
                    title.setText("Failed!");
                    simpleSetupUIControler(2);
                });
            }
        }).start(); // Execute the command in a separate thread to prevent blocking the UI thread
    }

    private void startDownload() {
        new DownloadFileTask(activity).execute(AppConfig.getSetupFiles());
    }

    private class DownloadFileTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private ProgressDialog progressDialog;
        private int fileLength;

        public DownloadFileTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context, R.style.MainDialogTheme);
            progressDialog.setTitle("Downloading... Please do not disconnect from the network.");
            progressDialog.setMessage(null);
            progressDialog.setIndeterminate(true);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(false); // Allow canceling with back button
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                fileLength = connection.getContentLength();
                input = connection.getInputStream();
                output = new FileOutputStream(tarPath);
                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    if (fileLength > 0) {
                        publishProgress((int) (total * 100 / fileLength));
                    }
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);

            // If you get here, the length of the file is known.
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(100);
            progressDialog.setProgress(progress[0]);

            // Convert the bytes downloaded to MB and update the dialog message accordingly.
            int progressMB = (int) ((progress[0] / 100.0) * fileLength / (1024 * 1024));
            progressDialog.setMessage(progressMB + " MB/" + fileLength / (1024 * 1024) + " MB");
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss(); // Dismiss the progress dialog

            if (result != null) {
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
                inBtn.setVisibility(View.VISIBLE);
                title.setText("Failed!");
            } else
                setupVectras();
        }

    }

    private void setupVectras() {
        simpleSetupUIControler(1);
        inBtn.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        String filesDir = activity.getFilesDir().getAbsolutePath();
        String neededPkgs = AppConfig.neededPkgs;
        if (!AppConfig.getSetupFiles().contains("64")) {
            neededPkgs.replaceAll(" mesa-vulkan-broadcom mesa-vulkan-freedreno mesa-vulkan-panfrost", "");
        }
        String cmd = "";
        cmd += selectedMirrorCommand + ";";
        executeShellCommand(cmd);
        executeShellCommand("set -e;" +
                " echo \"Starting setup...\";" +
                " apk update;" +
                " echo \"Installing packages...\";" +
                " apk add " + neededPkgs + ";" +
                " echo \"Installing Qemu...\";" +
                " tar -xzvf " + tarPath + " -C /;" +
                " rm " + tarPath + ";" +
                " echo \"Just a sec...\";" +
                " apk add qemu-audio-sdl pulseaudio;" +
                " echo export PULSE_SERVER=127.0.0.1 >> /etc/profile;" +
                " mkdir -p ~/.vnc && echo -e \"555555\\n555555\" | vncpasswd -f > ~/.vnc/passwd && chmod 0600 ~/.vnc/passwd;" +
                " echo \"installation successful! xssFjnj58Id\"");
    }

    private void setupVectras32() {
        inBtn.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        String filesDir = activity.getFilesDir().getAbsolutePath();
        String cmd = "";
        cmd += selectedMirrorCommand + ";";
        executeShellCommand(cmd);
        executeShellCommand("set -e;" +
                " echo \"Starting setup...\";" +
                " apk update;" +
                " echo \"Installing packages...\";" +
                " apk add " + AppConfig.neededPkgs.replaceAll(" mesa-vulkan-broadcom mesa-vulkan-freedreno mesa-vulkan-panfrost", "") + ";" +
                //" tar -xzvf " + tarPath + " -C /;" +
                " echo \"Installing Qemu...\";" +
                " apk add qemu-system-x86_64 qemu-system-ppc qemu-system-i386 qemu-system-aarch64 qemu-pr-helper qemu-img qemu-audio-sdl pulseaudio mesa-dri-gallium;" +
                " echo \"Just a sec...\";" +
                " echo export PULSE_SERVER=127.0.0.1 >> /etc/profile;" +
                //" rm " + tarPath + ";" +
                " mkdir -p ~/.vnc && echo -e \"555555\\n555555\" | vncpasswd -f > ~/.vnc/passwd && chmod 0600 ~/.vnc/passwd;" +
                " echo \"installation successful! xssFjnj58Id\"");
    }

    private void setupVectras64() {
        inBtn.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        String filesDir = activity.getFilesDir().getAbsolutePath();
        String cmd = "";
        cmd += selectedMirrorCommand + ";";
        executeShellCommand(cmd);
        executeShellCommand("set -e;" +
                " echo \"Starting setup...\";" +
                " apk update;" +
                " echo \"Installing packages...\";" +
                " apk add " + AppConfig.neededPkgs + ";" +
                " echo \"Downloading Qemu...\";" +
                " curl -o setup.tar.gz -L " + bootstrapfilelink + ";" +
                " echo \"Installing Qemu...\";" +
                " tar -xzvf setup.tar.gz -C /;" +
                " rm setup.tar.gz;" +
                " echo \"Just a sec...\";" +
                " apk add qemu-audio-sdl pulseaudio;" +
                " echo export PULSE_SERVER=127.0.0.1 >> /etc/profile;" +
                " mkdir -p ~/.vnc && echo -e \"555555\\n555555\" | vncpasswd -f > ~/.vnc/passwd && chmod 0600 ~/.vnc/passwd;" +
                " echo \"installation successful! xssFjnj58Id\"");
    }

    private void checkabi() {
        if (!AppConfig.getSetupFiles().contains("arm64-v8a")) {
            if (!AppConfig.getSetupFiles().contains("x86_64")) {
                VectrasApp.oneDialog(getResources().getString(R.string.warning), getResources().getString(R.string.cpu_not_support_64), true, false, activity);
            }
        }

        alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
        alertDialog.setTitle(getString(R.string.bootstrap_required));
        alertDialog.setMessage(getString(R.string.you_can_choose_between_auto_download_and_setup_or_manual_setup_by_choosing_bootstrap_file));
        alertDialog.setCancelable(false);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.auto_setup), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //startDownload();
                if (AppConfig.getSetupFiles().contains("arm64-v8a") || AppConfig.getSetupFiles().contains("x86_64")) {
                    setupVectras64();
                } else {
                    setupVectras32();
                }
                simpleSetupUIControler(1);
                return;
            }
        });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.manual_setup), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                }

                startActivityForResult(intent, 1001);
            }
        });
    }

    private void checkpermissions() {
        if (VectrasApp.checkpermissionsgranted(activity, true)) {
            if (!settingup) {
                settingup = true;
                checkabi();

                File tarGZ = new File(tarPath);
                if (tarGZ.exists()) {
                    setupVectras();
                } else {
                    if (linearsimplesetupui.getVisibility() == View.GONE) {
                        alertDialog.show();
                    }
                }
            }
        }
    }

    private void simpleSetupUIControler(int status) {
        if (status == 0) {
            linearstartsetup.setVisibility(View.VISIBLE);
            linearsettingup.setVisibility(View.GONE);
            linearsetupfailed.setVisibility(View.GONE);
        } else if (status == 1) {
            linearstartsetup.setVisibility(View.GONE);
            linearsettingup.setVisibility(View.VISIBLE);
            linearsetupfailed.setVisibility(View.GONE);
        } else if (status == 2) {
            linearstartsetup.setVisibility(View.GONE);
            linearsettingup.setVisibility(View.GONE);
            linearsetupfailed.setVisibility(View.VISIBLE);
        }
    }

    public String getPath(Uri uri) {
        return com.vectras.vm.utils.FileUtils.getPath(this, uri);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent ReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, ReturnedIntent);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            Uri content_describer = ReturnedIntent.getData();
            File selectedFilePath = new File(getPath(content_describer));
            ProgressBar loading = progressBar;
            String abi = Build.SUPPORTED_ABIS[0];
            if (selectedFilePath.toString().endsWith(abi+".tar.gz")) {
                loading.setVisibility(View.VISIBLE);
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
                                OutputStream out = new FileOutputStream(new File(tarPath));
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
                                        loading.setVisibility(View.GONE);
                                        alertDialog.dismiss();
                                        setupVectras();
                                        MainSettingsManager.setsetUpWithManualSetupBefore(SetupQemuActivity.this, true);
                                    }
                                };
                                activity.runOnUiThread(runnable);
                                File.close();
                            }
                        } catch (IOException e) {
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    loading.setVisibility(View.GONE);
                                    UIAlert(activity, e.toString(), "error");
                                }
                            };
                            activity.runOnUiThread(runnable);
                        }
                    }
                }).start();
            } else {
                if (linearsimplesetupui.getVisibility() == View.GONE) {
                    alertDialog.show();
                }
                UIAlert(activity, "INVALID FILE", "please select vectras-vm-" + abi + ".tar.gz file");
            }
        } else
        if (linearsimplesetupui.getVisibility() == View.GONE) {
            alertDialog.show();
        }
    }

    private void setupSpiner() {
        VectrasApp.setupMirrorListForListmap(listmapForSelectMirrors);

        spinnerselectmirror.setAdapter(new SpinnerSelectMirrorAdapter(listmapForSelectMirrors));
        spinnerselectmirror.setSelection(MainSettingsManager.getSelectedMirror(activity));
    }

    public class SpinnerSelectMirrorAdapter extends BaseAdapter {

        ArrayList<HashMap<String, String>> _data;

        public SpinnerSelectMirrorAdapter(ArrayList<HashMap<String, String>> _arr) {
            _data = _arr;
        }

        @Override
        public int getCount() {
            return _data.size();
        }

        @Override
        public HashMap<String, String> getItem(int _index) {
            return _data.get(_index);
        }

        @Override
        public long getItemId(int _index) {
            return _index;
        }

        @Override
        public View getView(final int _position, View _v, ViewGroup _container) {
            LayoutInflater _inflater = getLayoutInflater();
            View _view = _v;
            if (_view == null) {
                _view = _inflater.inflate(R.layout.simple_layout_for_spiner, null);
            }

            final TextView textViewLocation = _view.findViewById(R.id.textViewLocation);

            textViewLocation.setText(Objects.requireNonNull(_data.get((int) _position).get("location")));

            return _view;
        }
    }
}
