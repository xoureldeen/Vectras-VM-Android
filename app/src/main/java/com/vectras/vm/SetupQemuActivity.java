package com.vectras.vm;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;

import static com.vectras.vm.utils.UIUtils.UIAlert;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
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

public class SetupQemuActivity extends AppCompatActivity implements View.OnClickListener {
    Activity activity;
    private final String TAG = "SetupQemuActivity";
    ZoomableTextView vterm;
    MaterialButton inBtn;
    ProgressBar progressBar;
    AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_qemu);
        activity = this;

        progressBar = findViewById(R.id.progressBar);

        vterm = findViewById(R.id.tvTerminalOutput);

        inBtn = findViewById(R.id.btnInstall);
        inBtn.setOnClickListener(this);

        tarPath = getExternalFilesDir("data") + "/data.tar.gz";

        alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
        alertDialog.setTitle("BOOTSTRAP REQUIRED!");
        alertDialog.setMessage("U can choose between auto download and setup or manual setup by choosing bootstrap file.");
        alertDialog.setCancelable(false);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "AUTO SETUP", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                startDownload();
                return;
            }
        });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "MANUAL SETUP", new DialogInterface.OnClickListener() {
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

        File tarGZ = new File(tarPath);
        if (tarGZ.exists()) {
            setupVectras();
        } else {
            alertDialog.show();
        }
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnInstall) {
            File tarGZ = new File(tarPath);
            if (tarGZ.exists()) {
                tarGZ.delete();
            }
            alertDialog.show();
        }
    }

    String tarPath;

    // Function to append text and automatically scroll to bottom
    private void appendTextAndScroll(String textToAdd) {
        ScrollView scrollView = findViewById(R.id.scrollView);

        // Update the text
        vterm.append(textToAdd);

        if (textToAdd.contains("xssFjnj58Id")) {
            startActivity(new Intent(this, SplashActivity.class));
            finish();
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

                File tmpDir = new File(activity.getFilesDir(), "tmp");

                // Setup environment for the PRoot process
                processBuilder.environment().put("PROOT_TMP_DIR", tmpDir.getAbsolutePath());
                processBuilder.environment().put("PROOT_LOADER", nativeLibDir + "/libproot-loader.so");
                processBuilder.environment().put("PROOT_LOADER_32", nativeLibDir + "/libproot-loader32.so");

                processBuilder.environment().put("HOME", "/root");
                processBuilder.environment().put("USER", "root");
                processBuilder.environment().put("PATH", "/bin:/usr/bin:/sbin:/usr/sbin");
                processBuilder.environment().put("TERM", "xterm-256color");
                processBuilder.environment().put("TMPDIR", tmpDir.getAbsolutePath());
                processBuilder.environment().put("SHELL", "/bin/sh");

                // Example PRoot command; replace 'libproot.so' and other paths as needed
                String[] prootCommand = {
                        nativeLibDir + "/libproot.so", // PRoot binary path
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
                    });
                }
            } catch (IOException | InterruptedException e) {
                // Handle exceptions by printing the stack trace in the terminal output
                final String errorMessage = e.getMessage();
                activity.runOnUiThread(() -> {
                    appendTextAndScroll("Error: " + errorMessage + "\n");
                    Toast.makeText(activity, "Error executing command: " + errorMessage, Toast.LENGTH_LONG).show();
                    inBtn.setVisibility(View.VISIBLE);
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
            progressDialog.setTitle("Downloading \"data.tar.gz\"...");
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
            } else
                setupVectras();
        }

    }

    private void setupVectras() {
        inBtn.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        String filesDir = activity.getFilesDir().getAbsolutePath();
        String cmd = "";
        cmd += "echo \"http://dl-cdn.alpinelinux.org/alpine/edge/testing\" >> /etc/apk/repositories;";
        executeShellCommand(cmd);
        executeShellCommand("set -e;" +
                " echo 'Starting setup...';" +
                " apk update;" +
                " apk add tar tigervnc dwm libslirp libslirp-dev pulseaudio-dev glib-dev pixman-dev zlib-dev spice-dev" +
                " libusbredirparser usbredir-dev libiscsi-dev  sdl2 sdl2-dev libepoxy-dev virglrenderer-dev rdma-core" +
                " libusb ncurses-libs curl libnfs sdl2 gtk+3.0 fuse libpulse libseccomp jack pipewire liburing;" +
                " tar -xzvf " + tarPath + " -C /;" +
                " rm " + tarPath + ";" +
                " mkdir -p ~/.vnc && echo -e \"555555\\n555555\" | vncpasswd -f > ~/.vnc/passwd && chmod 0600 ~/.vnc/passwd" +
                " echo \"installation successful! xssFjnj58Id\"");
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
                alertDialog.show();
                UIAlert(activity, "NOT VAILED FILE", "please select vectras-vm-" + abi + ".tar.gz file");
            }
        } else
            alertDialog.show();
    }
}
