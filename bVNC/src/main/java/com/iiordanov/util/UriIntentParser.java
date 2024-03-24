package com.iiordanov.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.iiordanov.bVNC.AbstractConnectionBean;
import com.iiordanov.bVNC.COLORMODEL;
import com.iiordanov.bVNC.ConnectionBean;
import com.iiordanov.bVNC.Constants;
import com.iiordanov.bVNC.Database;
import com.iiordanov.bVNC.MostRecentBean;
import com.iiordanov.bVNC.Utils;
import com.undatech.opaque.Connection;

import net.sqlcipher.database.SQLiteDatabase;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UriIntentParser {
    private static final String TAG = "UriIntentParser";

    public static ConnectionBean loadFromUriOrCreateNew(Uri dataUri, Context ctx) {
        android.util.Log.d(TAG, "loadFromUriOrCreateNew");
        ConnectionBean newConnection = new ConnectionBean(ctx);
        if (dataUri == null) {
            return newConnection;
        }

        ConnectionBean connection = tryFindingByWidgetId(dataUri, ctx, new ConnectionBean(ctx));
        if (connection != null) return connection;

        connection = tryFindingByHostnameAndPort(dataUri, ctx, new ConnectionBean(ctx));
        if (connection != null) return connection;

        return newConnection;
    }

    private static ConnectionBean tryFindingByHostnameAndPort(Uri dataUri, Context ctx, ConnectionBean connection) {
        String host = dataUri.getHost();
        String port = String.format(Locale.US, "%d", dataUri.getPort());
        return tryFindingByFields(
                ctx,
                connection,
                AbstractConnectionBean.GEN_FIELD_ADDRESS,
                host,
                AbstractConnectionBean.GEN_FIELD_PORT,
                port
        );
    }

    private static ConnectionBean tryFindingByFields(
            Context ctx,
            ConnectionBean connection,
            String field,
            String value,
            String field2,
            String value2
    ) {
        Database database = new Database(ctx);
        SQLiteDatabase queryDb = database.getReadableDatabase();
        ConnectionBean connectionByField = null;
        Cursor cursor = getCursor(field, value, field2, value2, queryDb);
        if (cursor != null && cursor.moveToFirst()) {
            Log.i(TAG, String.format(
                            Locale.US,
                            "Loading connection info from field: %s, value: %s, field2: %s, value2: %s",
                            field, value, field2, value2
                    )
            );
            connectionByField = new ConnectionBean(ctx);
            connectionByField.Gen_populate(cursor, connection.Gen_columnIndices(cursor));
            cursor.close();
        }
        database.close();
        return connectionByField;
    }

    private static Cursor getCursor(String field, String value, String field2, String value2, SQLiteDatabase queryDb) {
        Cursor cursor = null;
        if (field != null && value != null && field2 == null && value2 == null) {
            cursor = getCursorOneField(field, value, queryDb);
        } else if (field2 != null && value2 != null) {
            cursor = getCursorTwoFields(field, value, field2, value2, queryDb);
        }
        return cursor;
    }

    private static Cursor getCursorTwoFields(String field, String value, String field2, String value2, SQLiteDatabase queryDb) {
        Cursor cursor;
        cursor = queryDb.query(
                AbstractConnectionBean.GEN_TABLE_NAME,
                null,
                field + " = ? AND " + field2 + " = ?",
                new String[]{value, value2},
                null,
                null,
                null);
        return cursor;
    }

    private static Cursor getCursorOneField(String field, String value, SQLiteDatabase queryDb) {
        Cursor cursor = queryDb.query(
                AbstractConnectionBean.GEN_TABLE_NAME,
                null,
                field + " = ?",
                new String[]{value},
                null,
                null,
                null);
        return cursor;
    }

    @Nullable
    private static ConnectionBean tryFindingByWidgetId(Uri dataUri, Context ctx, ConnectionBean connection) {
        String host = dataUri.getHost();
        if (host != null && host.startsWith(Utils.getConnectionString(ctx))) {
            getConnectionForShortcutWidget(ctx, connection, host);
            return connection;
        }
        return null;
    }

    private static void getConnectionForShortcutWidget(
            Context context,
            ConnectionBean connection,
            String host
    ) {
        Database database = new Database(context);
        int connectionId = 0;
        int idx = host.indexOf(':');

        if (idx != -1) {
            try {
                connectionId = Integer.parseInt(host.substring(idx + 1));
            } catch (NumberFormatException nfe) {
            }
        }

        if (connection.Gen_read(database.getReadableDatabase(), connectionId)) {
            MostRecentBean bean = ConnectionBean.getMostRecent(database.getReadableDatabase());
            if (bean != null) {
                bean.setConnectionId(connection.get_Id());
                bean.Gen_update(database.getWritableDatabase());
            }
        }
        database.close();
    }

    public static void parseFromUri(Context context, Connection connection, Uri dataUri) {
        Log.i(TAG, "Parsing URI.");
        if (dataUri == null) {
            connection.setReadyForConnection(false);
            connection.setReadyToBeSaved(true);
            return;
        }
        parseHost(connection, dataUri);
        parsePort(connection, dataUri);
        parseLegacyColorAndPasswordPaths(connection, dataUri);
        // query based parameters
        parseConnectionName(connection, dataUri);
        parseUser(connection, dataUri);
        parsePassword(connection, dataUri);
        int secType = parseSecurityType(connection, dataUri);
        // ssh parameters
        parseSshHost(connection, dataUri);
        parseSshPort(connection, dataUri);
        parseSshUser(connection, dataUri);
        parseSshPassword(connection, dataUri);
        // security hashes
        parseIdHashAndIdHashAlgorithm(connection, dataUri);
        parseViewOnlyMode(connection, dataUri);
        parseScaleMode(connection, dataUri);
        parseExtraKeysVisibility(connection, dataUri);
        // color model
        parseColorMode(connection, dataUri);
        // Parse a passed-in TLS port number.
        parseTlsPort(connection, dataUri);
        // Parse a CA Cert path parameter
        parseCaCertPath(connection, dataUri);
        // Parse a Cert subject
        parseCertSubject(connection, dataUri);
        // Parse a keyboard layout parameter
        parseKeyboardLayout(connection, dataUri);
        parseExternalId(connection, dataUri);
        parseRequiresVpn(connection, dataUri);
        parseVpnScheme(connection, dataUri);

        connection.determineIfReadyForConnection(secType);

        boolean saveConnection = parseSaveConnection(dataUri);
        if (saveConnection && connection.isReadyToBeSaved()) {
            connection.saveAndWriteRecent(false, context);
        }
    }

    private static void parseVpnScheme(Connection connection, Uri dataUri) {
        String vpnUriScheme = dataUri.getQueryParameter(Constants.PARAM_VPN_URI_SCHEME);
        if (vpnUriScheme != null) {
            connection.setVpnUriScheme(vpnUriScheme);
        }
    }

    private static void parseRequiresVpn(Connection connection, Uri dataUri) {
        boolean requiresVpn = dataUri.getBooleanQueryParameter(Constants.PARAM_REQUIRES_VPN, false);
        connection.setRequiresVpn(requiresVpn);
    }

    private static void parseExternalId(Connection connection, Uri dataUri) {
        String externalId = dataUri.getQueryParameter(Constants.PARAM_EXTERNAL_ID);
        if (externalId != null) {
            connection.setExternalId(externalId);
        }
    }

    private static void parseKeyboardLayout(Connection connection, Uri dataUri) {
        String keyboardLayout = dataUri.getQueryParameter(Constants.PARAM_KEYBOARD_LAYOUT);
        if (keyboardLayout != null) {
            connection.setLayoutMap(keyboardLayout);
        }
    }

    private static void parseCertSubject(Connection connection, Uri dataUri) {
        String certSubject = dataUri.getQueryParameter(Constants.PARAM_CERT_SUBJECT);
        if (certSubject != null) {
            connection.setCertSubject(certSubject);
        }
    }

    private static void parseCaCertPath(Connection connection, Uri dataUri) {
        String caCertPath = dataUri.getQueryParameter(Constants.PARAM_CACERT_PATH);
        if (caCertPath != null) {
            connection.setCaCertPath(caCertPath);
        }
    }

    private static void parseTlsPort(Connection connection, Uri dataUri) {
        String tlsPortParam = dataUri.getQueryParameter(Constants.PARAM_TLS_PORT);
        if (tlsPortParam != null) {
            int tlsPort = Integer.parseInt(tlsPortParam);
            if (isInvalidPort(tlsPort)) {
                throw new IllegalArgumentException("The specified TLS port is not valid.");
            }
            connection.setTlsPort(tlsPort);
        }
    }

    private static boolean parseSaveConnection(Uri dataUri) {
        String saveConnectionParam = dataUri.getQueryParameter(Constants.PARAM_SAVE_CONN);
        boolean saveConnection = true;
        if (saveConnectionParam != null) {
            saveConnection = Boolean.parseBoolean(saveConnectionParam); // throw if invalid
        }
        return saveConnection;
    }

    private static void parseColorMode(Connection connection, Uri dataUri) {
        String colorModelParam = dataUri.getQueryParameter(Constants.PARAM_COLORMODEL);
        if (colorModelParam != null) {
            int colorModel = Integer.parseInt(colorModelParam); // throw if invalid
            switch (colorModel) {
                case Constants.COLORMODEL_BLACK_AND_WHITE:
                    connection.setColorModel(COLORMODEL.C2.nameString());
                    break;
                case Constants.COLORMODEL_GREYSCALE:
                    connection.setColorModel(COLORMODEL.C4.nameString());
                    break;
                case Constants.COLORMODEL_8_COLORS:
                    connection.setColorModel(COLORMODEL.C8.nameString());
                    break;
                case Constants.COLORMODEL_64_COLORS:
                    connection.setColorModel(COLORMODEL.C64.nameString());
                    break;
                case Constants.COLORMODEL_256_COLORS:
                    connection.setColorModel(COLORMODEL.C256.nameString());
                    break;
                // use the best currently available model
                case Constants.COLORMODEL_16BIT:
                    connection.setColorModel(COLORMODEL.C24bit.nameString());
                    break;
                case Constants.COLORMODEL_24BIT:
                    connection.setColorModel(COLORMODEL.C24bit.nameString());
                    break;
                case Constants.COLORMODEL_32BIT:
                    connection.setColorModel(COLORMODEL.C24bit.nameString());
                    break;
                default:
                    // we are given a bad parameter
                    throw new IllegalArgumentException("The specified color model is invalid or unsupported.");
            }
        }
    }

    private static void parseExtraKeysVisibility(Connection connection, Uri dataUri) {
        String extraKeysToggleParam = dataUri.getQueryParameter(Constants.PARAM_EXTRAKEYS_TOGGLE);
        if (extraKeysToggleParam != null)
            connection.setExtraKeysToggleType(Integer.parseInt(extraKeysToggleParam));
    }

    private static void parseScaleMode(Connection connection, Uri dataUri) {
        String scaleModeParam = dataUri.getQueryParameter(Constants.PARAM_SCALE_MODE);
        if (scaleModeParam != null)
            connection.setScaleMode(ImageView.ScaleType.valueOf(scaleModeParam));
    }

    private static void parseViewOnlyMode(Connection connection, Uri dataUri) {
        String viewOnlyParam = dataUri.getQueryParameter(Constants.PARAM_VIEW_ONLY);
        if (viewOnlyParam != null) connection.setViewOnly(Boolean.parseBoolean(viewOnlyParam));
    }

    private static void parseIdHashAndIdHashAlgorithm(Connection connection, Uri dataUri) {
        String idHashAlgParam = dataUri.getQueryParameter(Constants.PARAM_ID_HASH_ALG);
        if (idHashAlgParam != null) {
            int idHashAlg = Integer.parseInt(idHashAlgParam); // throw if invalid
            switch (idHashAlg) {
                case Constants.ID_HASH_MD5:
                case Constants.ID_HASH_SHA1:
                case Constants.ID_HASH_SHA256:
                    connection.setIdHashAlgorithm(idHashAlg);
                    break;
                default:
                    // we are given a bad parameter
                    throw new IllegalArgumentException("The specified hash algorithm is invalid or unsupported.");
            }
        }

        String idHash = dataUri.getQueryParameter(Constants.PARAM_ID_HASH);
        if (idHash != null) {
            connection.setIdHash(idHash);
        }
    }

    private static void parseSshPassword(Connection connection, Uri dataUri) {
        String sshPassword = dataUri.getQueryParameter(Constants.PARAM_SSH_PWD);
        if (sshPassword != null) {
            connection.setSshPassword(sshPassword);
        }
    }

    private static void parseSshUser(Connection connection, Uri dataUri) {
        String sshUser = dataUri.getQueryParameter(Constants.PARAM_SSH_USER);
        if (sshUser != null) {
            connection.setSshUser(sshUser);
        }
    }

    private static void parseSshPort(Connection connection, Uri dataUri) {
        String sshPortParam = dataUri.getQueryParameter(Constants.PARAM_SSH_PORT);
        if (sshPortParam != null) {
            int sshPort = Integer.parseInt(sshPortParam);
            if (isInvalidPort(sshPort)) {
                throw new IllegalArgumentException("The specified SSH port is not valid.");
            }
            connection.setSshPort(sshPort);
        }
    }

    private static void parseSshHost(Connection connection, Uri dataUri) {
        String sshHost = dataUri.getQueryParameter(Constants.PARAM_SSH_HOST);
        if (sshHost != null) {
            connection.setSshServer(sshHost);
        }
    }

    private static int parseSecurityType(Connection connection, Uri dataUri) {
        String securityTypeParam = dataUri.getQueryParameter(Constants.PARAM_SECTYPE);
        int secType = 0; //invalid
        if (securityTypeParam != null) {
            secType = Integer.parseInt(securityTypeParam); // throw if invalid
            switch (secType) {
                case Constants.SECTYPE_NONE:
                case Constants.SECTYPE_VNC:
                    connection.setConnectionType(Constants.CONN_TYPE_PLAIN);
                    break;
                case Constants.SECTYPE_INTEGRATED_SSH:
                    connection.setConnectionType(Constants.CONN_TYPE_SSH);
                    break;
                case Constants.SECTYPE_ULTRA:
                    connection.setConnectionType(Constants.CONN_TYPE_ULTRAVNC);
                    break;
                case Constants.SECTYPE_TLS:
                    connection.setConnectionType(Constants.CONN_TYPE_ANONTLS);
                    break;
                case Constants.SECTYPE_VENCRYPT:
                    connection.setConnectionType(Constants.CONN_TYPE_VENCRYPT);
                    break;
                case Constants.SECTYPE_TUNNEL:
                    connection.setConnectionType(Constants.CONN_TYPE_STUNNEL);
                    break;
                default:
                    throw new IllegalArgumentException("The specified security type is invalid or unsupported.");
            }
        }
        return secType;
    }

    private static void parsePassword(Connection connection, Uri dataUri) {
        ArrayList<String> supportedPwdParams = new ArrayList<String>() {{
            add(Constants.PARAM_RDP_PWD);
            add(Constants.PARAM_SPICE_PWD);
            add(Constants.PARAM_VNC_PWD);
        }};
        for (String pwdParam : supportedPwdParams) {
            String password = dataUri.getQueryParameter(pwdParam);
            if (password != null) {
                connection.setPassword(password);
                break;
            }
        }
    }

    private static void parseUser(Connection connection, Uri dataUri) {
        ArrayList<String> supportedUserParams = new ArrayList<String>() {{
            add(Constants.PARAM_RDP_USER);
            add(Constants.PARAM_SPICE_USER);
            add(Constants.PARAM_VNC_USER);
        }};
        for (String userParam : supportedUserParams) {
            String username = dataUri.getQueryParameter(userParam);
            if (username != null) {
                connection.setUserName(username);
                break;
            }
        }
    }

    private static void parseConnectionName(Connection connection, Uri dataUri) {
        String connectionName = dataUri.getQueryParameter(Constants.PARAM_CONN_NAME);

        if (connectionName != null) {
            connection.setNickname(connectionName);
        }
    }

    private static void parseLegacyColorAndPasswordPaths(Connection connection, Uri dataUri) {
        // handle legacy android-vnc-viewer parsing vnc://host:port/colormodel/password
        List<String> path = dataUri.getPathSegments();
        if (path.size() >= 1) {
            connection.setColorModel(path.get(0));
        }

        if (path.size() >= 2) {
            connection.setPassword(path.get(1));
        }
    }

    private static void parsePort(Connection connection, Uri dataUri) {
        final int PORT_NONE = 0;
        int port = dataUri.getPort();
        if (port > PORT_NONE && isInvalidPort(port)) {
            throw new IllegalArgumentException("The specified VNC port is not valid.");
        }
        connection.setPort(port);
    }

    private static void parseHost(Connection connection, Uri dataUri) {
        String host = dataUri.getHost();
        if (host != null) {
            connection.setAddress(host);

            // by default, the connection name is the host name
            String nickName = connection.getNickname();
            if (Utils.isNullOrEmptry(nickName)) {
                connection.setNickname(host);
            }
        }
    }

    static boolean isInvalidPort(int port) {
        final int PORT_MAX = 65535;
        return port <= 0 || port > PORT_MAX;
    }
}
