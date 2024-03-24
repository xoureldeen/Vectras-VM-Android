/**
 * Copyright (c) 2013 Iordan Iordanov
 * Copyright (c) 2011 Michael A. MacDonald
 */
package com.iiordanov.android.bc;

import android.os.Environment;

import com.iiordanov.bVNC.MainConfiguration;
import com.iiordanov.bVNC.Utils;

import java.io.File;

/**
 * @author Michael A. MacDonald
 */
public class BCStorageContext7 implements IBCStorageContext {

    /* (non-Javadoc)
     * @see com.iiordanov.android.bc.IBCStorageContext#getExternalStorageDir(android.content.Context, java.lang.String)
     */
    @Override
    public File getExternalStorageDir(MainConfiguration context, String type) {
        File f = Environment.getExternalStorageDirectory();
        String packageName = Utils.pName(context);
        f = new File(f, "Android/data/" + packageName + "/files");
        if (type != null)
            f = new File(f, type);
        f.mkdirs();
        return f;
    }

}
