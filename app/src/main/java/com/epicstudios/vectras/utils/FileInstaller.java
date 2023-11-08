package com.epicstudios.vectras.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.epicstudios.vectras.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dev
 */
public class FileInstaller {

    public static void installFiles(Activity activity) {

        Log.v("Installer", "Installing files...");
        File tmpDir = new File(Config.basefiledir);
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }

        //Install base dir
        File dir = new File(Config.basefiledir);
        if (dir.exists() && dir.isDirectory()) {
            //don't create again
        } else if (dir.exists() && !dir.isDirectory()) {
            Log.v("Installer", "Could not create Dir, file found: " + Config.basefiledir);
            return;
        } else if (!dir.exists()) {
            dir.mkdir();
        }

        Log.v("Installer", "Getting Files: ");
        //Get each file in assets under ./roms/ and install in SDCARD
        AssetManager am = activity.getResources().getAssets();
        String[] files = null;
        try {
            files = am.list("roms");
        } catch (IOException ex) {
            Logger.getLogger(FileInstaller.class.getName()).log(Level.SEVERE, null, ex);
            Log.v("Installer", "Could not install files: " + ex.getMessage());
        }
        for (int i = 0; i < files.length; i++) {
            Log.v("Installer", "File: " + files[i]);
            String[] subfiles = null;
            try {
                subfiles = am.list("roms/" + files[i]);
            } catch (IOException ex) {
                Logger.getLogger(FileInstaller.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (subfiles != null && subfiles.length > 0) {
                //Install base dir
                File dir1 = new File(Config.basefiledir + files[i]);
                if (dir1.exists() && dir1.isDirectory()) {
                    //don't create again
                } else if (dir1.exists() && !dir1.isDirectory()) {
                    Log.v("Installer", "Could not create Dir, file found: " + Config.basefiledir + files[i]);
                    return;
                } else if (!dir1.exists()) {
                    dir1.mkdir();
                }
                for (int k = 0; k < subfiles.length; k++) {
                    Log.v("Installer", "File: " + files[i] + "/" + subfiles[k]);
                    installFile(activity, files[i] + "/" + subfiles[k], Config.basefiledir, "roms", null);
                }
            } else {
                installFile(activity, files[i], Config.basefiledir, "roms", null);
            }
        }
//        InputStream is = am.open(srcFile);

    }

    public static boolean installFile(Context activity, String srcFile, 
    		String destDir, String assetsDir, String destFile) {
        try {
            AssetManager am = activity.getResources().getAssets(); // get the local asset manager
            InputStream is = am.open(assetsDir + "/" + srcFile); // open the input stream for reading
            File destDirF = new File(destDir);
            if (!destDirF.exists()) {
                boolean res = destDirF.mkdirs();
                if(!res){
                	UIUtils.toastLong(activity, "Could not create directory for image");
                }
            }
            
            if(destFile==null)
            	destFile=srcFile;
            OutputStream os = new FileOutputStream(destDir + "/" + destFile);
            byte[] buf = new byte[8092];
            int n;
            while ((n = is.read(buf)) > 0) {
                os.write(buf, 0, n);
            }
            os.close();
            is.close();
            return true;
        } catch (Exception ex) {
            Log.e("Installer", "failed to install file: " + destFile + ", Error:" + ex.getMessage());
            return false;
        }
    }
}
