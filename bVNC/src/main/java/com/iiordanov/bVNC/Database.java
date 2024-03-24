/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2010 Michael A. MacDonald
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

package com.iiordanov.bVNC;

import android.content.Context;
import android.util.Log;

import com.iiordanov.bVNC.input.AbstractMetaKeyBean;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.io.File;

/**
 * @author Michael A. MacDonald
 */
public class Database extends SQLiteOpenHelper {
    public final static String TAG = Database.class.toString();
    static final int DBV_0_5_0 = 12;
    static final int DBV_1_2_0 = 20;
    static final int DBV_1_5_0 = 22;
    static final int DBV_1_6_0 = 291;
    static final int DBV_1_7_0 = 292;
    static final int DBV_1_8_0 = 293;
    static final int DBV_1_9_0 = 308;
    static final int DBV_2_0_0 = 309;
    static final int DBV_2_1_0 = 329;
    static final int DBV_2_1_1 = 335;
    static final int DBV_2_1_2 = 336;
    static final int DBV_2_1_3 = 360;
    static final int DBV_2_1_4 = 367;
    static final int DBV_2_1_5 = 374;
    static final int DBV_2_1_6 = 431;
    static final int DBV_2_1_7 = 501;
    static final int DBV_2_1_8 = 507;
    static final int DBV_2_1_9 = 521;
    static final int DBV_2_2_0 = 525;
    static final int CURRVERS = DBV_2_2_0;
    private static final String dbName = "VncDatabase";
    private static String password = "";

    public Database(Context context) {
        super(context, dbName, null, CURRVERS);
        SQLiteDatabase.loadLibs(context);
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String newPassword) {
        Database.password = newPassword;
    }

    /* (non-Javadoc)
     * @see net.sqlcipher.database.SQLiteOpenHelper#onCreate(net.sqlcipher.database.SQLiteDatabase)
     */
    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(AbstractConnectionBean.GEN_CREATE);
        db.execSQL(MostRecentBean.GEN_CREATE);
        db.execSQL(MetaList.GEN_CREATE);
        db.execSQL(AbstractMetaKeyBean.GEN_CREATE);
        db.execSQL(SentTextBean.GEN_CREATE);

        db.execSQL("INSERT INTO " + MetaList.GEN_TABLE_NAME + " VALUES ( 1, 'DEFAULT')");
    }

    public SQLiteDatabase getWritableDatabase() {
        return getWritableDatabase(Database.password);
    }

    public SQLiteDatabase getReadableDatabase() {
        return getReadableDatabase(Database.password);
    }

    public boolean changeDatabasePassword(String newPassword) {
        // Get readable database with old password
        SQLiteDatabase db = getReadableDatabase(Database.password);
        int version = db.getVersion();
        String pathToDb = db.getPath();
        // Delete any temp db that may be in the way.
        deleteTempDatabase(pathToDb, "-temp");
        String newFormat = null;

        // If the previous key is an empty string, we must encrypt a plaintext DB.
        if (Database.password.equals("")) {
            Log.i(TAG, "Previous database unencrypted, encrypting.");
            newFormat = "encrypted";
            // If the previous key is not an empty string, then we must rekey an existing DB.
        } else if (newPassword.equals("")) {
            Log.i(TAG, "Previous database encrypted, decrypting.");
            newFormat = "plaintext";
            // If both the previous and new password are non-empty, we are rekeying the DB.
        } else {
            Log.i(TAG, "Previous database encrypted, rekeying.");
            db.rawExecSQL(String.format("PRAGMA key = '%s'", Database.password));
            Database.setPassword(newPassword);
            // Rekey the database
            db.rawExecSQL(String.format("PRAGMA rekey = '%s'", Database.password));
        }

        if (newFormat != null) {
            // Write out the database in the new format.
            db.rawExecSQL(String.format("ATTACH DATABASE '%s' AS %s KEY '%s'", db.getPath() + "-temp", newFormat, newPassword));
            db.rawExecSQL(String.format("select sqlcipher_export('%s')", newFormat));
            db.rawExecSQL(String.format("DETACH DATABASE '%s'", newFormat));
            db.close();
            this.close();
            Log.i(TAG, "Done exporting to: " + newFormat);

            try {
                // Set version of database
                SQLiteDatabase tempDb = SQLiteDatabase.openDatabase(db.getPath() + "-temp", newPassword, null, SQLiteDatabase.OPEN_READWRITE);
                tempDb.setVersion(version);
                tempDb.close();
            } catch (Exception e) {
                // If we could not open the temp database, delete it and return failure.
                deleteTempDatabase(pathToDb, "-temp");
                return false;
            }

            try {
                // Save away the old database
                moveFile(pathToDb, pathToDb + "-BAK");
                // Move the temp database file over the old database
                moveFile(pathToDb + "-temp", pathToDb);

                // Test-open the new database and read off the version.
                SQLiteDatabase newDb = SQLiteDatabase.openDatabase(db.getPath(), newPassword, null, SQLiteDatabase.OPEN_READWRITE);
                newDb.getVersion();
                newDb.close();
            } catch (Exception e) {
                // Having received an exception in testing the DB above, restore the old DB and return failure.
                moveFile(pathToDb + "-BAK", pathToDb);
                return false;
            } finally {
                // Having succeeded to open the database without an exception, delete the backup of the old DB.
                deleteTempDatabase(pathToDb, "-BAK");
                // And set the password to the new password.
                Database.setPassword(newPassword);
            }
        } else {
            db.close();
            this.close();
        }

        return true;
    }

    private void moveFile(String from, String to) {
        new File(from).renameTo(new File(to));
    }

    private void deleteTempDatabase(String pathToDb, String suffix) {
        String tempDbPath = pathToDb + suffix;
        File temp = new File(tempDbPath);
        if (temp.exists()) {
            temp.delete();
        }
    }

    /* (non-Javadoc)
     * @see net.sqlcipher.database.SQLiteOpenHelper#onUpgrade(net.sqlcipher.database.SQLiteDatabase, int, int)
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == DBV_0_5_0) {
            Log.i(TAG, "Doing upgrade from 12 to 20");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_CONNECTIONTYPE + " INTEGER");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_SSHSERVER + " TEXT");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_SSHPORT + " INTEGER");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_SSHUSER + " TEXT");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_SSHPASSWORD + " TEXT");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_KEEPSSHPASSWORD + " BOOLEAN DEFAULT FALSE");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_SSHPUBKEY + " TEXT");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_SSHPRIVKEY + " TEXT");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_SSHPASSPHRASE + " TEXT");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_USESSHPUBKEY + " BOOLEAN DEFAULT FALSE");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_SSHHOSTKEY + " TEXT");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_SSHREMOTECOMMANDOS + " INTEGER");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_SSHREMOTECOMMAND + " TEXT");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_SSHREMOTECOMMANDTIMEOUT + " INTEGER");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_USESSHREMOTECOMMAND + " BOOLEAN DEFAULT FALSE");
            oldVersion = DBV_1_2_0;
        }

        if (oldVersion == DBV_1_2_0) {
            Log.i(TAG, "Doing upgrade from 20 to 22");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_USEDPADASARROWS + " BOOLEAN DEFAULT FALSE");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_ROTATEDPAD + " BOOLEAN DEFAULT FALSE");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_USEPORTRAIT + " BOOLEAN DEFAULT FALSE");
            oldVersion = DBV_1_5_0;
        }

        if (oldVersion == DBV_1_5_0) {
            Log.i(TAG, "Doing upgrade from 22 to 291");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_SSHREMOTECOMMANDTYPE + " INTEGER");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_AUTOXENABLED + " BOOLEAN DEFAULT FALSE");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_AUTOXTYPE + " INTEGER");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_AUTOXCOMMAND + " TEXT");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_AUTOXRESTYPE + " INTEGER");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_AUTOXWIDTH + " INTEGER");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_AUTOXHEIGHT + " INTEGER");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_AUTOXSESSIONTYPE + " INTEGER");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_AUTOXSESSIONPROG + " TEXT");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_AUTOXRANDFILENM + " TEXT");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_AUTOXUNIXPW + " BOOLEAN DEFAULT FALSE");
            oldVersion = DBV_1_6_0;
        }

        if (oldVersion == DBV_1_6_0) {
            Log.i(TAG, "Doing upgrade from 291 to 292");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_AUTOXUNIXAUTH + " BOOLEAN DEFAULT FALSE");
            oldVersion = DBV_1_7_0;
        }

        if (oldVersion == DBV_1_7_0) {
            Log.i(TAG, "Doing upgrade from 292 to 293");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_EXTRAKEYSTOGGLETYPE + " INTEGER");
            oldVersion = DBV_1_8_0;
        }

        if (oldVersion == DBV_1_8_0) {
            Log.i(TAG, "Doing upgrade from 293 to 308");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_PREFENCODING + " INTEGER");
            oldVersion = DBV_1_9_0;
        }

        if (oldVersion == DBV_1_9_0) {
            Log.i(TAG, "Doing upgrade from 308 to 309");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_RDPDOMAIN + " TEXT");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_RDPRESTYPE + " INTEGER");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_RDPWIDTH + " INTEGER");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_RDPHEIGHT + " INTEGER");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_RDPCOLOR + " INTEGER");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_REMOTEFX + " BOOLEAN DEFAULT FALSE");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_DESKTOPBACKGROUND + " BOOLEAN DEFAULT FALSE");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_FONTSMOOTHING + " BOOLEAN DEFAULT FALSE");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_DESKTOPCOMPOSITION + " BOOLEAN DEFAULT FALSE");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_WINDOWCONTENTS + " BOOLEAN DEFAULT FALSE");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_MENUANIMATION + " BOOLEAN DEFAULT FALSE");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_VISUALSTYLES + " BOOLEAN DEFAULT FALSE");
            oldVersion = DBV_2_0_0;
        }

        if (oldVersion == DBV_2_0_0) {
            Log.i(TAG, "Doing upgrade from 309 to 329");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_CACERT + " TEXT");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_CACERTPATH + " TEXT");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_TLSPORT + " INTEGER");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_CERTSUBJECT + " TEXT");
            oldVersion = DBV_2_1_0;
        }

        if (oldVersion == DBV_2_1_0) {
            Log.i(TAG, "Doing upgrade from 329 to 335");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_ENABLESOUND + " BOOLEAN DEFAULT FALSE");
            oldVersion = DBV_2_1_1;
        }

        if (oldVersion == DBV_2_1_1) {
            Log.i(TAG, "Doing upgrade from 335 to 336");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_VIEWONLY + " BOOLEAN DEFAULT FALSE");
            oldVersion = DBV_2_1_2;
        }

        if (oldVersion == DBV_2_1_2) {
            Log.i(TAG, "Doing upgrade from 336 to 360");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_CONSOLEMODE + " BOOLEAN DEFAULT FALSE");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_REDIRECTSDCARD + " BOOLEAN DEFAULT FALSE");
            oldVersion = DBV_2_1_3;
        }

        if (oldVersion == DBV_2_1_3) {
            Log.i(TAG, "Doing upgrade from 360 to 367");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_LAYOUTMAP + " TEXT DEFAULT 'English (US)'");
            oldVersion = DBV_2_1_4;
        }

        if (oldVersion == DBV_2_1_4) {
            Log.i(TAG, "Doing upgrade from 367 to 374");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_ENABLERECORDING + " BOOLEAN DEFAULT FALSE");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_REMOTESOUNDTYPE + " INTEGER DEFAULT " + Constants.REMOTE_SOUND_DISABLED);
            oldVersion = DBV_2_1_5;
        }

        if (oldVersion == DBV_2_1_5) {
            Log.i(TAG, "Doing upgrade from 374 to 430");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_FILENAME + " TEXT");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_X509KEYSIGNATURE + " TEXT");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_SCREENSHOTFILENAME + " TEXT");
            oldVersion = DBV_2_1_6;
        }

        if (oldVersion == DBV_2_1_6) {
            Log.i(TAG, "Doing upgrade from 430 to 501");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_ENABLEGFX + " BOOLEAN DEFAULT FALSE");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_ENABLEGFXH264 + " BOOLEAN DEFAULT FALSE");
            oldVersion = DBV_2_1_7;
        }

        if (oldVersion == DBV_2_1_7) {
            Log.i(TAG, "Doing upgrade from 501 to 507");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_PREFERSENDINGUNICODE + " BOOLEAN DEFAULT FALSE");
            oldVersion = DBV_2_1_8;
        }

        if (oldVersion == DBV_2_1_8) {
            Log.i(TAG, "Doing upgrade from 507 to 521");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_EXTERNALID + " TEXT");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_REQUIRESVPN + " BOOLEAN DEFAULT FALSE");
            db.execSQL("ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                    + AbstractConnectionBean.GEN_FIELD_VPNURISCHEME + " TEXT");
            oldVersion = DBV_2_1_9;
        }

        if (oldVersion == DBV_2_1_9) {
            Log.i(TAG, "Doing upgrade from 521 to 525");
            db.execSQL(
                    "ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                            + AbstractConnectionBean.GEN_FIELD_RDPGATEWAYENABLED
                            + " BOOLEAN DEFAULT FALSE"
            );
            db.execSQL(
                    "ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                            + AbstractConnectionBean.GEN_FIELD_RDPGATEWAYHOSTNAME + " TEXT"
            );
            db.execSQL(
                    "ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                            + AbstractConnectionBean.GEN_FIELD_RDPGATEWAYPORT + " INTEGER DEFAULT "
                            + Constants.DEFAULT_RDP_GATEWAY_PORT
            );
            db.execSQL(
                    "ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                            + AbstractConnectionBean.GEN_FIELD_RDPGATEWAYUSERNAME + " TEXT"
            );
            db.execSQL(
                    "ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                            + AbstractConnectionBean.GEN_FIELD_RDPGATEWAYDOMAIN + " TEXT"
            );
            db.execSQL(
                    "ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                            + AbstractConnectionBean.GEN_FIELD_RDPGATEWAYPASSWORD + " TEXT"
            );
            db.execSQL(
                    "ALTER TABLE " + AbstractConnectionBean.GEN_TABLE_NAME + " ADD COLUMN "
                            + AbstractConnectionBean.GEN_FIELD_KEEPRDPGATEWAYPASSWORD
                            + " BOOLEAN DEFAULT FALSE"
            );
            oldVersion = DBV_2_2_0;
        }
    }

    public boolean checkMasterPassword(String password, Context context) {
        Log.i(TAG, "Checking master password.");
        boolean result = false;

        Database testPassword = new Database(context);
        testPassword.close();
        try {
            testPassword.getReadableDatabase(password);
            result = true;
        } catch (Exception e) {
            result = false;
        }
        testPassword.close();
        return result;
    }
}
