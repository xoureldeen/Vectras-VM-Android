package com.vectras.vm.manager;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.vectras.vm.R;
import com.vectras.vm.utils.ClipboardUltils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.ProgressDialog;
import com.vectras.vterm.Terminal;
import com.vectras.vterm.Terminal2;

public class HostSharedFolder {
    public static final String TAG = "HostSharedFolder";
    public static final String MAIN_URL = "http://10.0.2.2:19000";
    public static final String EX_UPLOAD_COMMAND = "curl -T myfile.txt http://10.0.2.2:19000/myfile.txt";
    public static final String SHARED_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

    public static void start(Context context) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setText(context.getString(R.string.just_a_moment));
        progressDialog.setFixTextColor(true);
        progressDialog.show();

        new Thread(() -> {
            boolean isRuning;

            Terminal2 terminal2 = new Terminal2(context);

            if (terminal2.executeOnThisThread("pgrep -f 'http.server 19000'").isEmpty()) {
                if (!terminal2.executeOnThisThread("which python3").contains("python3"))
                    terminal2.executeOnThisThread("apk add python3");

                new Terminal2(context).execute("python3 -m http.server 19000 --bind 127.0.0.1 --directory " + SHARED_DIR);

                isRuning = !terminal2.executeOnThisThread("sleep 1; pgrep -f 'http.server 19000'").isEmpty();
            } else {
                isRuning = true;
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                progressDialog.reset();

                if (isRuning) {
                    DialogUtils.threeDialog(
                            context,
                            context.getString(R.string.the_server_is_running),
                            String.format(context.getString(R.string.host_shared_folder_format_content), MAIN_URL, EX_UPLOAD_COMMAND),
                            context.getString(R.string.copy_link),
                            context.getString(R.string.copy_upload_command),
                            context.getString(R.string.close),
                            true,
                            R.drawable.folder_24px,
                            true,
                            () -> ClipboardUltils.copyToClipboard(context, MAIN_URL),
                            () -> ClipboardUltils.copyToClipboard(context, EX_UPLOAD_COMMAND),
                            null,
                            null
                    );
                } else {
                    DialogUtils.oopsDialog(context, context.getString(R.string.host_shared_folder_error_content));
                }
            });
        }).start();
    }
}
