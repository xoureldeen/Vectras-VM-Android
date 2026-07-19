package com.vectras.vm.file;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vectras.vm.R;
import com.vectras.vm.databinding.DialogFilePickerBinding;
import com.vectras.vm.databinding.ListFileBinding;
import com.vectras.vm.manager.FormatManager;
import com.vectras.vm.utils.ClipboardUltils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.IntentUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class FilePickerDialog extends DialogFragment {
    
    DialogFilePickerBinding binding;
    LinearLayoutManager layoutmanager;
    RecyclerviewAdapter adapter;
    FilePickerDialogCallback callback;
    FilePickerSettings settings;

    String currentPath = "";
    String homeName;
    String homePath;
    boolean lockHome;
    int pickType = -1;
    boolean doNotSelectInSystemFolder;

    boolean isShowHiddenFile;
    boolean isShowHiddenDiviers;

    boolean fixTextColor;

    int textColor;
    
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        binding = DialogFilePickerBinding.inflate(getLayoutInflater());

        settings = new FilePickerSettings(requireActivity());

        isShowHiddenFile = settings.showHiddenFiles();
        isShowHiddenDiviers = settings.divider();

        if (fixTextColor) {
            textColor = binding.tvTitle.getCurrentTextColor();
            binding.tvFullPath.setTextColor(textColor);
            binding.tvNothingHere.setTextColor(textColor);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireActivity()).create();
        dialog.setView(binding.getRoot());

        layoutmanager = new LinearLayoutManager(requireActivity());
        adapter = new RecyclerviewAdapter(requireActivity(), dialog, new ArrayList<>(), callback);
        binding.list.setAdapter(adapter);
        binding.list.setLayoutManager(layoutmanager);

        binding.list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                boolean canScrollUp = rv.canScrollVertically(-1);
                boolean canScrollDown = rv.canScrollVertically(1);

                binding.dvTop.setVisibility(canScrollUp ? View.VISIBLE : View.INVISIBLE);
                binding.dvBottom.setVisibility(canScrollDown ? View.VISIBLE : View.INVISIBLE);
            }
        });

        String lastPath = new File(settings.lastPath()).exists() ? settings.lastPath() : Environment.getExternalStorageDirectory().getAbsolutePath();

        adapter.load((homePath != null && new File(homePath).exists()) ? homePath : lastPath);

        binding.btnHome.setOnClickListener(v -> adapter.load(homePath != null && new File(homePath).exists() ? homePath : Environment.getExternalStorageDirectory().getAbsolutePath()));
        binding.btnUp.setOnClickListener(v -> adapter.load(new File(currentPath).getParent()));

        if (pickType == BROWSE_MODE) {
            binding.btnOpenInOtherApp.setOnClickListener(v -> {
                if (!isAdded()) return;
                FileUtils.openFolder(requireContext(), currentPath);
                dismiss();
            });
        } else {
            binding.btnOpenInOtherApp.setVisibility(View.GONE);
        }

        binding.tvFullPath.setOnClickListener(v -> {
            if (!isAdded()) return;
            ClipboardUltils.copyToClipboard(requireContext(), currentPath, false);
            Toast.makeText(requireContext(), R.string.the_folder_path_has_been_copied_to_the_clipboard, Toast.LENGTH_SHORT).show();
        });

        if (pickType == TYPE_FOLDER) {
            binding.btnPick.setVisibility(View.VISIBLE);

            binding.btnPick.setOnClickListener(v -> {
                if (!isAdded()) return;

                if (!isAllowPick(currentPath)) {
                    Toast.makeText(requireContext(), R.string.you_are_not_allowed_to_use_this_folder, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (callback != null) callback.onPicked(currentPath);
                settings.lastPath(currentPath);
                dismiss();
            });
        }

        binding.btnClose.setOnClickListener(v -> dismiss());
        
        return dialog;
    }

    public void setFixTextColor(boolean isEnable) {
        fixTextColor = isEnable;
    }

    public void setHomeName(String name) {
        homeName = name;
    }

    public void setHomePath(String path) {
        homePath = new File(path).getAbsolutePath();
    }

    public void setLockHome(boolean lock) {
        lockHome = lock;
    }

    public void browse(Activity activity) {
        pickType = BROWSE_MODE;
        show(((FragmentActivity) activity).getSupportFragmentManager(), "file_picker");
    }

    public void browse(Activity activity, String path) {
        setHomePath(path);
        browse(activity);
    }

    public void setDoNotSelectInSystemFolder(boolean doNotSelect) {
        doNotSelectInSystemFolder = doNotSelect;
    }

    public void pick(Activity activity, int type, FilePickerDialogCallback callback) {
        this.callback = callback;
        pickType = type;

        show(((FragmentActivity) activity).getSupportFragmentManager(), "file_picker");
    }

    public boolean isAllowPick(String path) {
        return !(doNotSelectInSystemFolder && (path.equals(Environment.getExternalStorageDirectory().getAbsolutePath()) || path.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android")));
    }

    public void updateUpButton(String path) {
        if (path.equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            binding.tvTitle.setText(R.string.internal_storage);
            binding.btnHome.setVisibility(View.GONE);
            binding.btnUp.setVisibility(View.INVISIBLE);
            binding.btnUp.setEnabled(false);
        } else if (lockHome && homePath.equals(path)) {
            binding.tvTitle.setText(homeName == null || homeName.isEmpty() ? new File(path).getName() : homeName);
            binding.btnHome.setVisibility(View.GONE);
            binding.btnUp.setVisibility(View.INVISIBLE);
            binding.btnUp.setEnabled(false);
        } else {
            binding.tvTitle.setText(new File(path).getName());
            binding.btnHome.setVisibility(View.VISIBLE);
            binding.btnUp.setVisibility(View.VISIBLE);
            binding.btnUp.setEnabled(true);
        }

        binding.tvFullPath.setText(path);
    }

    boolean isLoading;

    private ArrayList<FilePickerData> loadFileList(String path) {
        currentPath = path;

        new Handler(Looper.getMainLooper()).post(() -> updateUpButton(path));

        ArrayList<FilePickerData> list = new ArrayList<>();

        File dir = new File(path);
        if (!dir.exists() || dir.isFile()) return list;

        File[] listFiles = dir.listFiles();
        if (listFiles == null || listFiles.length == 0) return list;

        list.clear();
        for (File file : listFiles) {
            if (!isShowHiddenFile && file.getName().startsWith(".")) continue;

            FilePickerData item = new FilePickerData();

            if (pickType == TYPE_FOLDER) {
                if (!file.isDirectory()) continue;
            }

            item.name = file.getName();
            item.path = file.getAbsolutePath();
            item.type = file.isDirectory() ? TYPE_FOLDER : getType(file.getName());
            item.icon = getIcon(item.type);
            item.enabled = pickType == TYPE_FILE || pickType == BROWSE_MODE || item.type == TYPE_FOLDER || (pickType >= FIT_PICK_DISK_FILE_MODE ? checkFitPickFileMode(item.name) : item.type == pickType);
            list.add(item);
        }

        Collections.sort(list, (o1, o2) -> {
            if (o1.name == null || o2.name == null) {
                return 0;
            }
            return o1.name.compareToIgnoreCase(o2.name);
        });

        return list;
    }

    public static final int BROWSE_MODE = -1;
    public static final int TYPE_FILE = 0;
    public static final int TYPE_FOLDER = 1;
    public static final int AUDIO_FILE = 2;
    public static final int VIDEO_FILE = 3;
    public static final int IMAGE_FILE = 4;
    public static final int DOCUMENT_FILE = 5;
    public static final int APPLICATION_FILE = 6;
    public static final int DISK_FILE = 7;
    public static final int ISO_FILE = 8;
    public static final int FLOPPY_FILE = 9;
    public static final int ROM_FILE = 10;
    public static final int SHELL_FILE = 11;
    public static final int DB_FILE = 12;
    public static final int LOG_FILE = 13;
    public static final int WINDOWS_FILE = 14;
    public static final int CONFIG_FILE = 15;
    public static final int ZIP_FILE = 16;
    public static final int DEV_FILE = 17;
    public static final int SYSTEM_FILE = 18;
    public static final int PACKAGE_FILE = 19;
    public static final int FONT_FILE = 20;
    public static final int KEY_FILE = 21;
    public static final int IOS_FILE = 22;

    private int getType(String fileName) {
        if (!fileName.contains(".")) return TYPE_FILE;

        String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        return switch (extension) {
            case ".mp3", ".wav", ".ogg", ".flac", ".m4a", ".aif", ".wma", ".smi" -> AUDIO_FILE;
            case ".mp4", ".webm", ".mov", ".mkv", ".avi", ".wmv" -> VIDEO_FILE;
            case ".jpg", ".jpeg", ".png", ".gif", ".webp", ".raw" -> IMAGE_FILE;
            case ".pdf", ".txt", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".md", ".csv" -> DOCUMENT_FILE;
            case ".apk", ".xapk", ".apkm", ".apks" -> APPLICATION_FILE;
            case ".img", ".qcow2", ".vhd", ".vhdx", ".vdi", ".qcow", ".vmdk", ".vpc" -> DISK_FILE;
            case ".iso", ".cdr", ".toast" -> ISO_FILE;
            case ".flp", ".ima" -> FLOPPY_FILE;
            case ".cvbi" -> ROM_FILE;
            case ".sh", ".cmd", ".bat", ".ps1" -> SHELL_FILE;
            case ".json", ".db", ".sqlite", ".sqlite3", ".dbf", ".accdb", ".mdf", ".ldf", ".ibd", ".frm", ".parquet", ".orc", ".kdbx" -> DB_FILE;
            case ".log" -> LOG_FILE;
            case ".exe", ".msi", ".msix", ".appx", ".msixbundle" -> WINDOWS_FILE;
            case ".conf", ".ini" -> CONFIG_FILE;
            case ".zip", ".7z", ".rar" -> ZIP_FILE;
            case ".java", ".kt", ".js", ".py", ".c", ".cpp", ".h", ".xml", ".html", ".css", ".lua", ".go", ".asl" -> DEV_FILE;
            case ".dll" -> SYSTEM_FILE;
            case ".deb", ".rpm" -> PACKAGE_FILE;
            case ".ttf", ".otf" -> FONT_FILE;
            case ".jks" -> KEY_FILE;
            case ".ipa", ".ipsw" -> IOS_FILE;
            default -> TYPE_FILE;
        };
    }

    private int getIcon(int type) {
        return switch (type) {
            case TYPE_FOLDER -> R.drawable.folder_24px;
            case AUDIO_FILE -> R.drawable.music_note_24px;
            case VIDEO_FILE -> R.drawable.movie_24px;
            case IMAGE_FILE -> R.drawable.image_24px;
            case DOCUMENT_FILE -> R.drawable.description_24px;
            case APPLICATION_FILE -> R.drawable.android_24px;
            case DISK_FILE -> R.drawable.hard_drive_24px;
            case ISO_FILE -> R.drawable.album_24px;
            case ROM_FILE -> R.drawable.vectras_vm_24px;
            case SHELL_FILE -> R.drawable.terminal_24px;
            case DB_FILE -> R.drawable.database_24px;
            case LOG_FILE -> R.drawable.monitor_heart_24px;
            case WINDOWS_FILE -> R.drawable.wysiwyg_24px;
            case CONFIG_FILE -> R.drawable.tune_24px;
            case ZIP_FILE -> R.drawable.folder_zip_24px;
            case DEV_FILE -> R.drawable.code_24px;
            case SYSTEM_FILE -> R.drawable.settings_24px;
            case PACKAGE_FILE -> R.drawable.package_24px;
            case FONT_FILE -> R.drawable.titlecase_24px;
            case KEY_FILE -> R.drawable.key_24px;
            case IOS_FILE -> R.drawable.ios_24px;
            default -> R.drawable.draft_24px;
        };
    }

    public static final int FIT_PICK_DISK_FILE_MODE = 20;
    public static final int FIT_PICK_OPTICAL_FILE_MODE = 21;
    public static final int FIT_PICK_FLOPPY_FILE_MODE = 22;

    public boolean checkFitPickFileMode(String fileName) {
        if (pickType == FIT_PICK_DISK_FILE_MODE) {
            return FormatManager.isHardDriveFileFormat(fileName);
        } else if (pickType == FIT_PICK_OPTICAL_FILE_MODE) {
            return FormatManager.isOpticalFileFormat(fileName);
        } else if (pickType == FIT_PICK_FLOPPY_FILE_MODE) {
            return FormatManager.isFloppyFileFormat(fileName);
        } else {
            return false;
        }
    }
    
    public interface FilePickerDialogCallback {
        void onPicked(String path);
    }

    private class RecyclerviewAdapter extends RecyclerView.Adapter<RecyclerviewAdapter.ViewHolder> {

        Activity activity;
        ArrayList<FilePickerData> data;
        AlertDialog dialog;
        FilePickerDialogCallback callback;

        public RecyclerviewAdapter(Activity activity, AlertDialog alertDialog, ArrayList<FilePickerData> arr, FilePickerDialogCallback callback) {
            this.activity = activity;
            data = arr;
            dialog = alertDialog;
            this.callback = callback;
        }

        @NonNull
        @Override
        public RecyclerviewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = activity.getLayoutInflater();
            ListFileBinding binding1 = ListFileBinding.inflate(inflater, parent, false);
            return new RecyclerviewAdapter.ViewHolder(binding1.getRoot());
        }

        @Override
        public void onBindViewHolder(RecyclerviewAdapter.ViewHolder holder, final int position) {
            View view = holder.itemView;
            TextView text = view.findViewById(R.id.text);
            ImageView icon = view.findViewById(R.id.icon);
            text.setText(data.get(position).name);
            icon.setImageResource(data.get(position).icon);

            if (fixTextColor) text.setTextColor(textColor);

            LinearLayout main = view.findViewById(R.id.main);

            if (data.get(position).enabled) {
                main.setEnabled(true);
                main.setAlpha(1f);
                main.setOnClickListener(v -> {
                    if (data.get(position).type == TYPE_FOLDER) {
                        load(data.get(position).path);
                    } else {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        if (pickType == BROWSE_MODE) {
                            openFile(data.get(position).path);
                        } else {
                            if (callback != null) callback.onPicked(data.get(position).path);

                            settings.lastPath(currentPath);
                            dialog.dismiss();
                        }
                    }
                });
            } else {
                main.setAlpha(0.5f);
                main.setOnClickListener(null);
                main.setEnabled(false);
            }

            if (!isShowHiddenDiviers || position == data.size() - 1) {
                view.findViewById(R.id.md_inset_bottom).setVisibility(View.GONE);
            } else {
                view.findViewById(R.id.md_inset_bottom).setVisibility(View.VISIBLE);
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public void load(String path) {
            isLoading = true;

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded() && isLoading) binding.lnLoading.setVisibility(View.VISIBLE);
            }, 200);

            new Thread(() -> {
                ArrayList<FilePickerData> data1 = loadFileList(path);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded()) return;

                    if (data1.isEmpty()) {
                        binding.lnEmpty.setVisibility(View.VISIBLE);
                        binding.lnBrowse.setVisibility(View.INVISIBLE);
                    } else {
                        binding.lnEmpty.setVisibility(View.INVISIBLE);
                        binding.lnBrowse.setVisibility(View.VISIBLE);
                        data.clear();
                        data.addAll(data1);
                        notifyDataSetChanged();
                    }

                    binding.lnLoading.setVisibility(View.GONE);
                    isLoading = false;

                    if (pickType == TYPE_FOLDER) binding.btnPick.setEnabled(isAllowPick(currentPath));
                });
            }).start();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View v) {
                super(v);
            }
        }
    }

    public void openFile(String path) {
        File file = new File(path);

        if (!file.exists()) {
            if (!isAdded()) return;
            IntentUtils.showErrorDialog(requireActivity());
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri fileUri;

        if (!isAdded()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String authority = requireActivity().getPackageName() + ".provider";
            fileUri = FileProvider.getUriForFile(requireContext(), authority, file);
        } else {
            fileUri = Uri.fromFile(file);
        }


        String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        if (mimeType == null) {
            mimeType = "*/*";
        }

        intent.setDataAndType(fileUri, mimeType);


        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


        Intent chooserIntent = Intent.createChooser(intent, getString(R.string.open_with));
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


        if (!isAdded()) return;
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            requireContext().startActivity(chooserIntent);
        } else {
            IntentUtils.showErrorDialog(requireActivity());
        }
    }
}
