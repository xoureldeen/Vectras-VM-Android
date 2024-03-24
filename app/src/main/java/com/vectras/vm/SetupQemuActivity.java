package com.vectras.vm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.vectras.vterm.view.ZoomableTextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class SetupQemuActivity extends AppCompatActivity implements View.OnClickListener {
    SetupQemuActivity activity;
    private final String TAG = "SetupQemuActivity";
    ZoomableTextView vterm;
    MaterialButton dlBtn, hpBtn, slBtn, inBtn;
    TextView tvSelectedPath;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_qemu);
        activity = this;

        progressBar = findViewById(R.id.progressBar);

        vterm = findViewById(R.id.tvTerminalOutput);

        dlBtn = findViewById(R.id.btnDownload);
        hpBtn = findViewById(R.id.btnHelp);
        slBtn = findViewById(R.id.btnSelect);
        inBtn = findViewById(R.id.btnInstall);

        dlBtn.setOnClickListener(activity);
        hpBtn.setOnClickListener(activity);
        slBtn.setOnClickListener(activity);
        inBtn.setOnClickListener(activity);

        tvSelectedPath = findViewById(R.id.tarPath);
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnDownload) {
            String qe = AppConfig.vectrasPkg;
            Intent q = new Intent(Intent.ACTION_VIEW);
            q.setData(Uri.parse(qe));
            startActivity(q);
        } else if (id == R.id.btnHelp) {
            String qe = AppConfig.vectrasHelp;
            Intent q = new Intent(Intent.ACTION_VIEW);
            q.setData(Uri.parse(qe));
            startActivity(q);
        } else if (id == R.id.btnSelect) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");  // Allow the user to select any file type

            // Optionally, specify a URI for the file that should appear in the system file picker when it loads
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Use the Downloads folder as the starting path
                Uri downloadsUri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, downloadsUri);
            }

            startActivityForResult(intent, 0);
        } else if (id == R.id.btnInstall) {
            if (tarPath != null) {
                inBtn.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);

                String setupFilesUrl = AppConfig.getSetupFiles();

                tvSelectedPath.setText(setupFilesUrl);

                String filesDir = activity.getFilesDir().getAbsolutePath();
                if (!com.vectras.vm.utils.FileUtils.readFromFile(activity, new File(filesDir + "/distro/etc/apk/repositories")).contains("http://dl-cdn.alpinelinux.org/alpine/edge/testing"))
                    executeShellCommand("echo \"http://dl-cdn.alpinelinux.org/alpine/edge/testing\" >> /etc/apk/repositories");
                executeShellCommand("set -e;" +
                        " apk update;" +
                        " apk add libslirp libslirp-dev pulseaudio-dev glib-dev pixman-dev zlib-dev spice-dev libusbredirparser usbredir-dev libiscsi-dev  sdl2 sdl2-dev libepoxy-dev virglrenderer-dev tar;" +
                        " tar -xvzf " + tarPath.getAbsolutePath() + " -C " + filesDir + "/distro;" +
                        " rm " + tarPath.getAbsolutePath() + ";" +
                        " echo \"installation done! xssFjnj58Id\"");
            }
        }
    }

    File tarPath;

    public String getPath(Uri uri) {
        return com.vectras.vm.utils.FileUtils.getPath(this, uri);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent ReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, ReturnedIntent);

        if (requestCode == 0 && resultCode == RESULT_OK) {
            Uri content_describer = ReturnedIntent.getData();

            // Get the file extension from the URI
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(content_describer.toString());

            File selectedFilePath = new File(getPath(content_describer));
            progressBar.setVisibility(View.VISIBLE);
            String abi = Build.SUPPORTED_ABIS[0];
            if (selectedFilePath.getName().endsWith("tar.gz")) {
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
                                OutputStream out = new FileOutputStream(new File(com.vectras.vm.AppConfig.maindirpath + selectedFilePath.getName()));
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
                                        progressBar.setVisibility(View.GONE);
                                        inBtn.setEnabled(selectedFilePath.exists());
                                        tarPath = new File(com.vectras.vm.AppConfig.maindirpath + selectedFilePath.getName());

                                        tvSelectedPath.setText(tarPath.getAbsolutePath());

                                        tarPath.deleteOnExit();
                                    }
                                };
                                activity.runOnUiThread(runnable);
                                File.close();
                            }
                        } catch (IOException e) {
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                    inBtn.setEnabled(true);
                                    com.vectras.vm.utils.UIUtils.UIAlert(activity, "error", e.toString());
                                }
                            };
                            activity.runOnUiThread(runnable);
                        }
                    }
                }).start();
            } else {
                com.vectras.vm.utils.UIUtils.UIAlert(activity, "File not supported", "please select supported tar.gz to continue.<br><br>required files:<br>vectras.tar<br><br>please download the required files from our official website.");
                progressBar.setVisibility(View.GONE);
            }
        } else if (requestCode == 1000 && resultCode == RESULT_CANCELED) {
            finish();
        }
    }

    // Function to append text and automatically scroll to bottom
    private void appendTextAndScroll(String textToAdd) {
        ScrollView scrollView = findViewById(R.id.scrollView);

        // Update the text
        vterm.append(textToAdd);

        if (textToAdd.contains("installation done! xssFjnj58Id")) {
            //finish();
            startActivity(new Intent(this, SplashActivity.class));
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

                // Setup environment for the PRoot process
                processBuilder.environment().put("PROOT_TMP_DIR", filesDir + "/tmp");
                processBuilder.environment().put("PROOT_LOADER", nativeLibDir + "/libproot-loader.so");
                processBuilder.environment().put("PROOT_LOADER_32", nativeLibDir + "/libproot-loader32.so");

                processBuilder.environment().put("HOME", "/root");
                processBuilder.environment().put("USER", "root");
                processBuilder.environment().put("PATH", "/bin:/usr/bin:/sbin:/usr/sbin");
                processBuilder.environment().put("TERM", "xterm-256color");
                processBuilder.environment().put("TMPDIR", "/tmp");
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
                    activity.runOnUiThread(() -> Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG).show());
                }
            } catch (IOException | InterruptedException e) {
                // Handle exceptions by printing the stack trace in the terminal output
                final String errorMessage = e.getMessage();
                activity.runOnUiThread(() -> {
                    appendTextAndScroll("Error: " + errorMessage + "n");
                    Toast.makeText(activity, "Error executing command: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        }).start(); // Execute the command in a separate thread to prevent blocking the UI thread
    }
}