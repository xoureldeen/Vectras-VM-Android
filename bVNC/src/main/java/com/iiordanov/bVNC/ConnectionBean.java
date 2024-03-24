/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2009 Michael A. MacDonald
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

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.ImageView.ScaleType;

import com.antlersoft.android.dbimpl.NewInstance;
import com.iiordanov.bVNC.input.TouchInputHandlerDirectSwipePan;
import com.iiordanov.util.NetworkUtils;
import com.undatech.remoteClientUi.R;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.UUID;

/**
 * @author Iordan Iordanov
 * @author Michael A. MacDonald
 * @author David Warden
 */
public class ConnectionBean extends AbstractConnectionBean implements Comparable<ConnectionBean> {

    private static final String TAG = "ConnectionBean";
    static Context c = null;
    public static final NewInstance<ConnectionBean> newInstance = new NewInstance<ConnectionBean>() {
        public ConnectionBean get() {
            return new ConnectionBean(c);
        }
    };
    private boolean readyForConnection = true; // saved connections are OK
    private boolean readyToBeSaved = false;
    private int idHashAlgorithm;
    private String idHash;
    private String masterPassword;
    private String id;
    private boolean useLastPositionToolbar;
    private int useLastPositionToolbarX;
    private int useLastPositionToolbarY;
    private boolean useLastPositionToolbarMoved;
    private boolean showOnlyConnectionNicknames = false;

    public ConnectionBean(Context context) {
        String inputMode = TouchInputHandlerDirectSwipePan.ID;
        Boolean preferSendingUnicode = false;

        if (context == null) {
            context = App.getContext();
        }

        if (context != null) {
            inputMode = Utils.querySharedPreferenceString(context, Constants.defaultInputMethodTag,
                    TouchInputHandlerDirectSwipePan.ID);
            preferSendingUnicode = Utils.querySharedPreferenceBoolean(context, Constants.preferSendingUnicode);
        } else {
            android.util.Log.e(TAG, "Failed to query defaults from shared preferences, context is null.");
        }

        showOnlyConnectionNicknames = Utils.querySharedPreferenceBoolean(context, Constants.showOnlyConnectionNicknames);

        set_Id(0);
        setAddress("127.0.0.1:6999");
        setPassword("");
        setKeepPassword(true);
        setNickname("QEMU");
        setConnectionType(Constants.CONN_TYPE_PLAIN);
        setSshServer("");
        setSshPort(Constants.DEFAULT_SSH_PORT);
        setSshUser("");
        setSshPassword("");
        setKeepSshPassword(false);
        setSshPubKey("");
        setSshPrivKey("");
        setSshPassPhrase("");
        setUseSshPubKey(false);
        setSshHostKey("");
        setSshRemoteCommandOS(0);
        setSshRemoteCommandType(0);
        setSshRemoteCommand("");
        setSshRemoteCommandTimeout(5);
        setAutoXType(0);
        setAutoXCommand("");
        setAutoXEnabled(false);
        setAutoXResType(0);
        setAutoXWidth(0);
        setAutoXHeight(0);
        setAutoXSessionProg("");
        setAutoXSessionType(0);
        setAutoXUnixpw(false);
        setAutoXUnixAuth(false);
        setAutoXRandFileNm("");
        setUseSshRemoteCommand(false);
        setUserName("");
        setRdpDomain("");
        setPort(Constants.DEFAULT_PROTOCOL_PORT);
        setCaCert("");
        setCaCertPath("");
        setTlsPort(-1);
        setCertSubject("");
        setColorModel(COLORMODEL.C24bit.nameString());
        setPrefEncoding(RfbProto.EncodingTight);
        setScaleMode(ScaleType.MATRIX);
        setInputMode(inputMode);
        setUseDpadAsArrows(true);
        setRotateDpad(false);
        setUsePortrait(false);
        setUseLocalCursor(Constants.CURSOR_AUTO);
        setRepeaterId("");
        setExtraKeysToggleType(1);
        setMetaListId(1);
        setRdpResType(0);
        setRdpWidth(0);
        setRdpHeight(0);
        setRdpColor(Constants.DEFAULT_RDP_COLOR_MODE);
        setRemoteFx(false);
        setDesktopBackground(false);
        setFontSmoothing(false);
        setDesktopComposition(false);
        setWindowContents(false);
        setMenuAnimation(false);
        setVisualStyles(false);
        setConsoleMode(false);
        setRedirectSdCard(false);
        setEnableSound(false);
        setEnableRecording(false);
        setRemoteSoundType(Constants.REMOTE_SOUND_ON_DEVICE);
        setViewOnly(false);
        setLayoutMap("English (US)");
        setFilename(UUID.randomUUID().toString());
        setX509KeySignature("");
        setIdHash("");
        setScreenshotFilename(Utils.newScreenshotFileName());

        setEnableGfx(false);
        setEnableGfxH264(false);
        setPreferSendingUnicode(preferSendingUnicode);
        setExternalId("");
        setRequiresVpn(false);
        setVpnUriScheme(Constants.DEFAULT_VPN_URI_SCHEME);
        c = context;

        // These two are not saved in the database since we always save the cert data. 
        setIdHashAlgorithm(Constants.ID_HASH_SHA1);
        setIdHash("");

        // These settings are saved in SharedPrefs
        setUseLastPositionToolbar(true);
        setUseLastPositionToolbarX(0);
        setUseLastPositionToolbarY(0);
        setUseLastPositionToolbarMoved(false);

        setRdpGatewayPort(Constants.DEFAULT_RDP_GATEWAY_PORT);
    }

    /**
     * Return the object representing the app global state in the database, or null
     * if the object hasn't been set up yet
     *
     * @param db App's database -- only needs to be readable
     * @return Object representing the single persistent instance of MostRecentBean, which
     * is the app's global state
     */
    public static MostRecentBean getMostRecent(SQLiteDatabase db) {
        ArrayList<MostRecentBean> recents = new ArrayList<MostRecentBean>(1);
        MostRecentBean.getAll(db, MostRecentBean.GEN_TABLE_NAME, recents, MostRecentBean.GEN_NEW);
        if (recents.size() == 0)
            return null;
        return recents.get(0);
    }

    public int getIdHashAlgorithm() {
        return idHashAlgorithm;
    }

    public void setIdHashAlgorithm(int idHashAlgorithm) {
        this.idHashAlgorithm = idHashAlgorithm;
    }

    public String getIdHash() {
        return idHash;
    }

    public void setIdHash(String idHash) {
        this.idHash = idHash;
    }

    boolean isNew() {
        return get_Id() == 0;
    }

    @Override
    public String saveCaToFile(Context context, String caCertData) {
        return null;
    }

    @Override
    public void populateFromContentValues(ContentValues values) {
        Gen_populate(values);
    }

    @Override
    public String getLabel() {
        String nickname = "";
        if (!"".equals(this.getNickname())) {
            nickname = this.getNickname() + "\n";
        }
        String address = "";
        if (!showOnlyConnectionNicknames) {
            address = this.getAddress() + ":" + this.getPort();
            if (!"".equals(this.getUserName())) {
                address = this.getUserName() + "@" + address;
            }
            if (!"".equals(this.getSshServer())) {
                address = "SSH " + this.getSshUser() + "@" + this.getSshServer() + ":" +
                        this.getSshPort() + "\n" + address;
            }
        }
        return nickname + address;
    }

    @Override
    public String getRuntimeId() {
        return id;
    }

    @Override
    public void setRuntimeId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return Long.toString(get_Id());
    }

    @Override
    public String getConnectionTypeString() {
        return "";
    }

    @Override
    public void setConnectionTypeString(String connectionTypeString) {

    }

    @Override
    public String getHostname() {
        return null;
    }

    @Override
    public void setHostname(String hostname) {

    }

    @Override
    public String getVmname() {
        return null;
    }

    @Override
    public void setVmname(String vmname) {

    }

    @Override
    public String getOtpCode() {
        return null;
    }

    @Override
    public void setOtpCode(String otpCode) {
    }

    @Override
    public boolean isRotationEnabled() {
        return false;
    }

    @Override
    public void setRotationEnabled(boolean rotationEnabled) {

    }

    @Override
    public boolean isRequestingNewDisplayResolution() {
        return true;
    }

    @Override
    public void setRequestingNewDisplayResolution(boolean requestingNewDisplayResolution) {

    }

    @Override
    public boolean isAudioPlaybackEnabled() {
        return false;
    }

    @Override
    public void setAudioPlaybackEnabled(boolean audioPlaybackEnabled) {

    }

    @Override
    public boolean isUsingCustomOvirtCa() {
        return false;
    }

    @Override
    public void setUsingCustomOvirtCa(boolean useCustomCa) {

    }

    @Override
    public boolean isSslStrict() {
        return false;
    }

    @Override
    public void setSslStrict(boolean sslStrict) {

    }

    @Override
    public boolean isUsbEnabled() {
        return true;
    }

    @Override
    public void setUsbEnabled(boolean usbEnabled) {

    }

    @Override
    public String getOvirtCaFile() {
        return null;
    }

    @Override
    public void setOvirtCaFile(String ovirtCaFile) {

    }

    @Override
    public String getOvirtCaData() {
        return null;
    }

    @Override
    public void setOvirtCaData(String ovirtCaData) {

    }

    public synchronized void save(Context c) {
        android.util.Log.d(TAG, "save called");
        Database database = new Database(c);
        save(database.getWritableDatabase());
        database.close();
        saveToSharedPreferences(c);
        readyToBeSaved = true;
    }

    @Override
    public void load(Context context) {
        loadFromSharedPreferences(context);
        parsePortIfIpv4Address();
        setDefaultProtocolAndSshPorts();
    }

    public void loadFromSharedPreferences(Context context) {
        android.util.Log.d(TAG, "loadFromSharedPreferences called");
        SharedPreferences sp = context.getSharedPreferences(Long.toString(get_Id()), Context.MODE_PRIVATE);
        useLastPositionToolbar = sp.getBoolean("useLastPositionToolbar", true);
        useLastPositionToolbarX = sp.getInt("useLastPositionToolbarX", 0);
        useLastPositionToolbarY = sp.getInt("useLastPositionToolbarY", 0);
        useLastPositionToolbarMoved = sp.getBoolean("useLastPositionToolbarMoved", false);
    }

    public void saveToSharedPreferences(Context context) {
        android.util.Log.d(TAG, "saveToSharedPreferences called");
        SharedPreferences sp = context.getSharedPreferences(Long.toString(get_Id()), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("useLastPositionToolbar", useLastPositionToolbar);
        editor.putInt("useLastPositionToolbarX", useLastPositionToolbarX);
        editor.putInt("useLastPositionToolbarY", useLastPositionToolbarY);
        editor.putBoolean("useLastPositionToolbarMoved", useLastPositionToolbarMoved);
        editor.apply();
    }

    private synchronized void save(SQLiteDatabase database) {
        android.util.Log.d(TAG, "save called with database");
        ContentValues values = Gen_getValues();
        values.remove(GEN_FIELD__ID);
        if (!getKeepSshPassword()) {
            values.put(GEN_FIELD_SSHPASSWORD, "");
            values.put(GEN_FIELD_SSHPASSPHRASE, "");
        }
        if (!getKeepPassword()) {
            values.put(GEN_FIELD_PASSWORD, "");
        }
        if (isNew()) {
            set_Id(database.insert(GEN_TABLE_NAME, null, values));
        } else {
            database.update(GEN_TABLE_NAME, values, GEN_FIELD__ID + " = ?", new String[]{Long.toString(get_Id())});
        }
    }

    public boolean isReadyForConnection() {
        return readyForConnection;
    }

    public void setReadyForConnection(boolean readyForConnection) {
        this.readyForConnection = readyForConnection;
    }

    public boolean isReadyToBeSaved() {
        return readyToBeSaved;
    }

    public void setReadyToBeSaved(boolean readyToBeSaved) {
        this.readyToBeSaved = readyToBeSaved;
    }

    public ScaleType getScaleMode() {
        return ScaleType.valueOf(getScaleModeAsString());
    }

    public void setScaleMode(ScaleType value) {
        setScaleModeAsString(value.toString());
    }

    @Override
    public boolean getUseLastPositionToolbar() {
        return useLastPositionToolbar;
    }

    @Override
    public void setUseLastPositionToolbar(boolean useLastPositionToolbar) {
        this.useLastPositionToolbar = useLastPositionToolbar;
    }

    @Override
    public int getUseLastPositionToolbarX() {
        return useLastPositionToolbarX;
    }

    @Override
    public void setUseLastPositionToolbarX(int useLastPositionToolbarX) {
        this.useLastPositionToolbarX = useLastPositionToolbarX;
    }

    @Override
    public int getUseLastPositionToolbarY() {
        return useLastPositionToolbarY;
    }

    @Override
    public void setUseLastPositionToolbarY(int useLastPositionToolbarY) {
        this.useLastPositionToolbarY = useLastPositionToolbarY;
    }

    @Override
    public boolean getUseLastPositionToolbarMoved() {
        return useLastPositionToolbarMoved;
    }

    @Override
    public void setUseLastPositionToolbarMoved(boolean useLastPositionToolbarMoved) {
        this.useLastPositionToolbarMoved = useLastPositionToolbarMoved;
    }

    @Override
    public String toString() {
        if (isNew()) {
            return c.getString(R.string.new_connection);
        }
        String result = new String("");

        // Add the nickname if it has been set.
        if (!getNickname().equals("")) {
            result += getNickname() + ":";
        }

        // If this is an VNC over SSH connection, add the SSH server:port in parentheses
        if (getConnectionType() == Constants.CONN_TYPE_SSH) {
            result += "(" + getSshServer() + ":" + getSshPort() + ")" + ":";
        }

        // Add the VNC server and port.
        result += getAddress() + ":" + getPort();
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(ConnectionBean another) {
        int result = getNickname().compareTo(another.getNickname());
        if (result == 0) {
            result = getConnectionType() - another.getConnectionType();
        }
        if (result == 0) {
            result = getAddress().compareTo(another.getAddress());
        }
        if (result == 0) {
            result = getPort() - another.getPort();
        }
        if (result == 0) {
            result = getSshServer().compareTo(another.getSshServer());
        }
        if (result == 0) {
            result = getSshPort() - another.getSshPort();
        }
        return result;
    }

    public void saveAndWriteRecent(boolean saveEmpty, Context c) {
        android.util.Log.d(TAG, "saveAndWriteRecent called");
        Database database = new Database(c);
        if ((getConnectionType() == Constants.CONN_TYPE_SSH && getSshServer().equals("")
                || getAddress().equals("")) && !saveEmpty) {
            android.util.Log.d(TAG, "saveAndWriteRecent not saving due to missing data");
        } else {
            android.util.Log.d(TAG, "saveAndWriteRecent saving connection");
            saveAndWriteRecent(database);
            saveToSharedPreferences(c);
        }
    }

    private void saveAndWriteRecent(Database database) {
        android.util.Log.d(TAG, "saveAndWriteRecent called with database");

        SQLiteDatabase db = database.getWritableDatabase();
        db.beginTransaction();
        try {
            save(db);
            MostRecentBean mostRecent = getMostRecent(db);
            if (mostRecent == null) {
                mostRecent = new MostRecentBean();
                mostRecent.setConnectionId(get_Id());
                mostRecent.Gen_insert(db);
            } else {
                mostRecent.setConnectionId(get_Id());
                mostRecent.Gen_update(db);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
        if (db.isOpen()) {
            db.close();
        }
    }

    public void determineIfReadyForConnection(int secType) {
        setReadyForConnection(true);
        if (Utils.isNullOrEmptry(getAddress())) {
            Log.i(TAG, "URI missing remote address");
            setReadyForConnection(false);
        }

        int connType = getConnectionType();
        if (connType == Constants.CONN_TYPE_SSH) {
            if (Utils.isNullOrEmptry(getSshServer())) {
                Log.i(TAG, "URI SSH server when connection type is Constants.CONN_TYPE_SSH");
                setReadyForConnection(false);
            }
        }
    }

    private void setDefaultProtocolAndSshPorts() {
        if (this.getPort() == 0)
            this.setPort(Constants.DEFAULT_PROTOCOL_PORT);

        if (this.getSshPort() == 0)
            this.setSshPort(Constants.DEFAULT_SSH_PORT);
    }

    private void parsePortIfIpv4Address() {
        // Parse a HOST:PORT entry but only if not ipv6 address
        String host = this.getAddress();
        if (host.indexOf(':') > -1 && !NetworkUtils.INSTANCE.isValidIpv6Address(host)) {
            String p = host.substring(host.indexOf(':') + 1);
            try {
                int parsedPort = Integer.parseInt(p);
                this.setPort(parsedPort);
                this.setAddress(host.substring(0, host.indexOf(':')));
            } catch (Exception e) {
                Log.i(TAG, "Could not parse port from address, will use default");
            }
        }
    }
}
