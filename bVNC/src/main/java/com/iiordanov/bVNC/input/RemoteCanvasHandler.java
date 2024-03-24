package com.iiordanov.bVNC.input;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.iiordanov.bVNC.CredentialsObtainer;
import com.iiordanov.bVNC.RemoteCanvas;
import com.iiordanov.bVNC.RemoteCanvasActivity;
import com.iiordanov.bVNC.Utils;
import com.iiordanov.bVNC.dialogs.GetTextFragment;
import com.iiordanov.bVNC.protocol.RemoteConnection;
import com.undatech.opaque.Connection;
import com.undatech.opaque.MessageDialogs;
import com.undatech.opaque.RemoteClientLibConstants;
import com.undatech.opaque.dialogs.ChoiceFragment;
import com.undatech.opaque.dialogs.MessageFragment;
import com.undatech.opaque.dialogs.SelectTextElementFragment;
import com.undatech.opaque.proxmox.OvirtClient;
import com.undatech.opaque.util.HttpsFileDownloader;
import com.undatech.opaque.util.SslUtils;
import com.undatech.remoteClientUi.R;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

public class RemoteCanvasHandler extends Handler implements HttpsFileDownloader.OnDownloadFinishedListener {
    private static final String TAG = "RemoteCanvasHandler";
    private final Context context;
    private final RemoteCanvas c;
    RemoteConnection remoteConnection;
    private final Connection connection;
    private FragmentManager fm;
    CredentialsObtainer obtainer;
    Runnable setModes;

    public RemoteCanvasHandler(
            Context context,
            RemoteCanvas c,
            RemoteConnection remoteConnection,
            Connection connection,
            Runnable setModes
    ) {
        super();
        this.context = context;
        this.c = c;
        this.remoteConnection = remoteConnection;
        this.remoteConnection.setHandler(this);
        this.connection = connection;
        this.setModes = setModes;
        Activity activity = Utils.getActivity(context);
        if (activity instanceof FragmentActivity) {
            this.fm = ((FragmentActivity) Utils.getActivity(context)).getSupportFragmentManager();
        }
        obtainer = new CredentialsObtainer(connection, this, context);
    }

    /**
     * Convenience function to create a message with a single key and String value pair in it.
     */
    public static Message getMessageString(int what, String key, String value) {
        Message m = new Message();
        m.what = what;
        Bundle d = new Bundle();
        d.putString(key, value);
        m.setData(d);
        return m;
    }

    /**
     * Convenience function to create a message with a single key and String value pair in it.
     */
    public static Message getMessageStringList(int what, String key, ArrayList<String> value) {
        Message m = new Message();
        m.what = what;
        Bundle d = new Bundle();
        d.putStringArrayList(key, value);
        m.setData(d);
        return m;
    }

    public Connection getConnection() {
        return connection;
    }

    private void showGetTextFragment(
            String tag,
            String dialogId,
            String title,
            GetTextFragment.OnFragmentDismissedListener dismissalListener,
            int dialogType,
            int messageNum,
            int errorNum,
            String t1,
            String t2,
            String t3,
            boolean keep
    ) {
        if (remoteConnection.pd != null && remoteConnection.pd.isShowing()) {
            remoteConnection.pd.dismiss();
        }
        GetTextFragment frag = GetTextFragment.newInstance(dialogId, title, dismissalListener,
                dialogType, messageNum, errorNum, t1, t2, t3, keep);
        frag.setCancelable(false);
        frag.show(fm, tag);
    }

    private void displayMessageAndFinish(int titleTextId, int messageTextId, int buttonTextId) {
        MessageFragment message = MessageFragment.newInstance(
                context.getString(titleTextId),
                context.getString(messageTextId),
                context.getString(buttonTextId),
                () -> Utils.justFinish(context));
        message.show(fm, "endingDialog");
        remoteConnection.closeConnection();
    }


    /**
     * If there is a saved cert, checks the one given against it. If a signature was passed in
     * and no saved cert, then check that signature. Otherwise, presents the
     * given certificate signature to the user for approval.
     * <p>
     * The saved data must always win over any passed-in URI data
     *
     * @param cert the given cert.
     */
    @SuppressLint("StringFormatInvalid")
    private void validateX509Cert(final X509Certificate cert) {
        byte[] certDataTemp;
        try {
            certDataTemp = cert.getEncoded();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        String md5Hash = "";
        String sha1Hash = "";
        String sha256Hash = "";
        try {
            md5Hash = SslUtils.signature("MD5", certDataTemp);
            sha1Hash = SslUtils.signature("SHA-1", certDataTemp);
            sha256Hash = SslUtils.signature("SHA-256", certDataTemp);
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String certInfo;
        certInfo = String.format(
                context.getString(R.string.info_cert),
                md5Hash, sha1Hash, sha256Hash,
                cert.getSubjectX500Principal().getName(),
                cert.getIssuerX500Principal().getName(),
                cert.getNotBefore(),
                cert.getNotAfter()
        );

        // TODO: Create title string
        ChoiceFragment certDialog = ChoiceFragment.newInstance(
                context.getString(R.string.ca_new_or_changed),
                certInfo,
                "Accept",
                "Reject",
                result -> {
                    if (result) {
                        Log.e(TAG, "We were told to continue");
                        saveAndAcceptCert(cert);
                    } else {
                        Log.e(TAG, "We were told not to continue");
                        Utils.justFinish(context);
                    }
                });

        FragmentManager fm = ((FragmentActivity) context).getSupportFragmentManager();
        certDialog.setCancelable(false);
        certDialog.show(fm, "certDialog");
    }


    /**
     * Saves and accepts a x509 certificate.
     */
    private void saveAndAcceptCert(X509Certificate cert) {
        android.util.Log.d(TAG, "Saving X509 cert fingerprint.");
        String certificate = null;
        try {
            certificate = Base64.encodeToString(cert.getEncoded(), Base64.DEFAULT);
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
            remoteConnection.showFatalMessageAndQuit(context.getString(R.string.error_x509_could_not_generate_encoding));
        }
        connection.setX509KeySignature(certificate);
        handleSettingOvirtCaCert(certificate);
        connection.save(context);
        setCertificateAcceptedAndNotifyListeners();
    }

    private void handleSettingOvirtCaCert(String certificate) {
        if (isOvirtConnection()) {
            new OvirtClient(connection, this).downloadFromServer(this);
        } else if (isProxmoxConnection()) {
            connection.setOvirtCaData(certificate);
            connection.setUsingCustomOvirtCa(true);
        }
    }

    private boolean isProxmoxConnection() {
        return connection.getConnectionTypeString().equals(context.getResources().getString(R.string.connection_type_pve));
    }

    private boolean isOvirtConnection() {
        return connection.getConnectionTypeString().equals(context.getResources().getString(R.string.connection_type_ovirt));
    }

    /**
     * Permits the user to validate a certificate with subject, issuer and fingerprint.
     *
     * @param subject     optional subject
     * @param issuer      optional issuer
     * @param fingerprint non-optional fingerprint that may be saved
     * @param save        whether to save the fingerprint and verify it if it is saved
     */
    public void validateCert(String subject, String issuer, final String fingerprint, boolean save) {
        boolean certMismatch = false;
        if (save) {
            if (connection.getX509KeySignature().equals(fingerprint)) {
                setCertificateAcceptedAndNotifyListeners();
                return;
            } else if (!connection.getX509KeySignature().equals("")) {
                certMismatch = true;
            }
        }
        // Show a dialog with the key signature for approval.
        DialogInterface.OnClickListener signatureNo = (dialog, which) -> {
            // We were told not to continue, so stop the activity
            remoteConnection.closeConnection();
            Utils.justFinish(context);
        };
        DialogInterface.OnClickListener signatureYes = (dialog, which) -> {
            // Indicate the certificate was accepted.
            if (save) {
                connection.setX509KeySignature(fingerprint);
                connection.save(context);
            }
            setCertificateAcceptedAndNotifyListeners();
        };

        String message = context.getString(R.string.info_cert_signatures);
        if (!"".equals(subject)) {
            message += "\n" + context.getString(R.string.cert_subject) + ":     " + subject;
        }
        if (!"".equals(issuer)) {
            message += "\n" + context.getString(R.string.cert_issuer) + ":      " + issuer;
        }
        message += "\n" + context.getString(R.string.cert_fingerprint) + ": " + fingerprint +
                context.getString(R.string.info_cert_signatures_identical);
        if (certMismatch) {
            message += "\n\n" + context.getString(R.string.warning_cert_does_not_match);
        }
        Utils.showYesNoPrompt(context,
                context.getString(R.string.info_continue_connecting) + connection.getAddress() + "?",
                message,
                signatureYes,
                signatureNo
        );
    }

    private void setCertificateAcceptedAndNotifyListeners() {
        remoteConnection.getRfbConn().setCertificateAccepted(true);
        synchronized (remoteConnection.getRfbConn()) {
            remoteConnection.getRfbConn().notifyAll();
        }
        synchronized (RemoteCanvasHandler.this) {
            RemoteCanvasHandler.this.notifyAll();
        }
    }

    /**
     * Function used to initialize an empty SSH HostKey for a new VNC over SSH connection.
     */
    public void initializeSshHostKey(
            boolean keyChangeDetected,
            String idHash,
            String serverHostKey,
            String hostKeySignature
    ) {
        // If the SSH HostKey is empty, then we need to grab the HostKey from the server and save it.
        Log.d(TAG, "Attempting to initialize SSH HostKey.");

        c.displayShortToastMessage(context.getString(R.string.info_ssh_initializing_hostkey));
        // Show a dialog with the key signature.
        DialogInterface.OnClickListener signatureNo = (dialog, which) -> {
            // We were told to not continue, so stop the activity
            remoteConnection.pd.dismiss();
            Utils.justFinish(context);
        };
        DialogInterface.OnClickListener signatureYes = (dialog, which) -> {
            // We were told to go ahead with the connection.
            connection.setIdHash(idHash); // could prompt based on algorithm
            connection.setSshHostKey(serverHostKey);
            connection.save(context);
            synchronized (RemoteCanvasHandler.this) {
                RemoteCanvasHandler.this.notify();
            }
        };

        String warning = keyChangeDetected ?
                context.getString(R.string.error_ssh_hostkey_changed) + "\n" : "\n";
        Utils.showYesNoPrompt(
                context,
                context.getString(R.string.info_continue_connecting) + connection.getSshServer() + "?",
                warning +
                        context.getString(R.string.info_ssh_key_fingerprint) +
                        hostKeySignature +
                        context.getString(R.string.info_ssh_key_fingerprint_identical),
                signatureYes,
                signatureNo
        );
    }


    @Override
    public void handleMessage(final Message msg) {
        Bundle messageData;
        android.util.Log.d(TAG, "Handling message, msg.what: " + msg.what);
        final String messageText = Utils.getStringFromMessage(msg, "message");
        switch (msg.what) {
            case RemoteClientLibConstants.PRO_FEATURE:
                if (remoteConnection.pd != null && remoteConnection.pd.isShowing()) {
                    remoteConnection.pd.dismiss();
                }
                remoteConnection.showFatalMessageAndQuit(context.getString(R.string.pro_feature_mfa));
                break;
            case RemoteClientLibConstants.GET_VERIFICATIONCODE:
                showGetTextFragment(context.getString(R.string.verification_code),
                        GetTextFragment.DIALOG_ID_GET_VERIFICATION_CODE,
                        context.getString(R.string.verification_code),
                        remoteConnection.sshConnection, GetTextFragment.Plaintext,
                        R.string.verification_code_message, R.string.verification_code,
                        null, null, null, false);
                break;
            case RemoteClientLibConstants.GET_SSH_CREDENTIALS:
                showGetTextFragment(context.getString(R.string.enter_ssh_credentials),
                        GetTextFragment.DIALOG_ID_GET_SSH_CREDENTIALS,
                        context.getString(R.string.enter_ssh_credentials),
                        remoteConnection.sshConnection, GetTextFragment.CredentialsWithReadOnlyUser,
                        R.string.enter_ssh_credentials, R.string.enter_ssh_credentials,
                        connection.getSshUser(), connection.getSshPassword(), null,
                        connection.getKeepSshPassword());
                break;
            case RemoteClientLibConstants.GET_SSH_PASSPHRASE:
                showGetTextFragment(context.getString(R.string.ssh_passphrase_hint),
                        GetTextFragment.DIALOG_ID_GET_SSH_PASSPHRASE,
                        context.getString(R.string.enter_passphrase_title),
                        remoteConnection.sshConnection, GetTextFragment.Password,
                        R.string.enter_passphrase, R.string.ssh_passphrase_hint,
                        null, null, null, connection.getKeepSshPassword());
                break;
            case RemoteClientLibConstants.GET_VNC_PASSWORD:
                showGetTextFragment(context.getString(R.string.enter_vnc_password),
                        GetTextFragment.DIALOG_ID_GET_VNC_PASSWORD,
                        context.getString(R.string.enter_vnc_password),
                        obtainer, GetTextFragment.Password,
                        R.string.enter_vnc_password, R.string.enter_vnc_password,
                        connection.getPassword(), null, null, connection.getKeepPassword());
                break;
            case RemoteClientLibConstants.GET_VNC_CREDENTIALS:
                showGetTextFragment(context.getString(R.string.enter_vnc_credentials),
                        GetTextFragment.DIALOG_ID_GET_VNC_CREDENTIALS,
                        context.getString(R.string.enter_vnc_credentials),
                        obtainer, GetTextFragment.Credentials,
                        R.string.enter_vnc_credentials, R.string.enter_vnc_credentials,
                        connection.getUserName(), connection.getPassword(), null,
                        connection.getKeepPassword());
                break;
            case RemoteClientLibConstants.GET_RDP_CREDENTIALS:
                showGetTextFragment(context.getString(R.string.enter_rdp_credentials),
                        GetTextFragment.DIALOG_ID_GET_RDP_CREDENTIALS,
                        context.getString(R.string.enter_rdp_credentials),
                        obtainer, GetTextFragment.CredentialsWithDomain,
                        R.string.enter_rdp_credentials, R.string.enter_rdp_credentials,
                        connection.getUserName(), connection.getRdpDomain(), connection.getPassword(),
                        connection.getKeepPassword());
                break;
            case RemoteClientLibConstants.GET_RDP_GATEWAY_CREDENTIALS:
                showGetTextFragment(context.getString(R.string.enter_gateway_credentials),
                        GetTextFragment.DIALOG_ID_GET_RDP_GATEWAY_CREDENTIALS,
                        context.getString(R.string.enter_gateway_credentials),
                        obtainer, GetTextFragment.CredentialsWithDomain,
                        R.string.enter_gateway_credentials, R.string.enter_gateway_credentials,
                        connection.getRdpGatewayUsername(), connection.getRdpGatewayDomain(), connection.getRdpGatewayPassword(),
                        connection.getKeepRdpGatewayPassword());
                break;
            case RemoteClientLibConstants.GET_SPICE_PASSWORD:
                remoteConnection.maintainConnection = false;
                showGetTextFragment(context.getString(R.string.enter_spice_password),
                        GetTextFragment.DIALOG_ID_GET_SPICE_PASSWORD,
                        context.getString(R.string.enter_spice_password),
                        obtainer, GetTextFragment.Password,
                        R.string.enter_spice_password, R.string.enter_spice_password,
                        connection.getPassword(), null, null, connection.getKeepPassword());
                break;
            case RemoteClientLibConstants.DIALOG_SSH_CERT:
                android.util.Log.d(TAG, "DIALOG_SSH_CERT");
                messageData = (Bundle) msg.obj;
                boolean keyChangeDetected = messageData.getBoolean("keyChangeDetected");
                String idHash = messageData.getString("idHash");
                String serverHostKey = messageData.getString("serverHostKey");
                String hostKeySignature = messageData.getString("hostKeySignature");
                initializeSshHostKey(keyChangeDetected, idHash, serverHostKey, hostKeySignature);
                break;
            case RemoteClientLibConstants.DIALOG_RDP_CERT:
                android.util.Log.d(TAG, "DIALOG_RDP_CERT");
                messageData = (Bundle) msg.obj;
                validateCert(
                        messageData.getString("subject"),
                        messageData.getString("issuer"),
                        messageData.getString("fingerprint"),
                        messageData.getBoolean("save", false)
                );
                break;
            case RemoteClientLibConstants.DISCONNECT_WITH_MESSAGE:
                remoteConnection.showFatalMessageAndQuit(messageText);
                break;
            case RemoteClientLibConstants.SPICE_CONNECT_FAILURE_IF_MAINTAINING_CONNECTION:
                if (remoteConnection.maintainConnection) {
                    remoteConnection.showFatalMessageAndQuit(messageText);
                }
                break;
            case RemoteClientLibConstants.SPICE_TLS_ERROR:
                remoteConnection.showFatalMessageAndQuit(context.getString(R.string.error_ovirt_ssl_handshake_failure));
                break;
            case RemoteClientLibConstants.RDP_CONNECT_FAILURE:
                if (remoteConnection.maintainConnection) {
                    remoteConnection.showFatalMessageAndQuit(context.getString(R.string.error_rdp_connection_failed));
                }
                break;
            case RemoteClientLibConstants.RDP_UNABLE_TO_CONNECT:
                if (remoteConnection.maintainConnection) {
                    remoteConnection.showFatalMessageAndQuit(context.getString(R.string.error_rdp_unable_to_connect));
                }
                break;
            case RemoteClientLibConstants.RDP_AUTH_FAILED:
                if (remoteConnection.maintainConnection) {
                    remoteConnection.showFatalMessageAndQuit(context.getString(R.string.error_rdp_authentication_failed));
                }
                break;
            case RemoteClientLibConstants.SERVER_CUT_TEXT:
                messageData = (Bundle) msg.obj;
                remoteConnection.serverJustCutText = true;
                remoteConnection.setClipboardText(messageData.getString("text"));
                break;
            case RemoteClientLibConstants.REINIT_SESSION:
                remoteConnection.initializeConnection();
                c.setParameters(remoteConnection.getRfbConn(), connection, this, remoteConnection.getPointer(), setModes);
                break;
            case RemoteClientLibConstants.REPORT_TOOLBAR_POSITION:
                android.util.Log.d(TAG, "Handling message, REPORT_TOOLBAR_POSITION");
                if (connection.getUseLastPositionToolbar()) {
                    int useLastPositionToolbarX = Utils.getIntFromMessage(msg, "useLastPositionToolbarX");
                    int useLastPositionToolbarY = Utils.getIntFromMessage(msg, "useLastPositionToolbarY");
                    boolean useLastPositionToolbarMoved = Utils.getBooleanFromMessage(msg, "useLastPositionToolbarMoved");
                    android.util.Log.d(TAG, "Handling message, REPORT_TOOLBAR_POSITION, X Coordinate" + useLastPositionToolbarX);
                    android.util.Log.d(TAG, "Handling message, REPORT_TOOLBAR_POSITION, Y Coordinate" + useLastPositionToolbarY);
                    connection.setUseLastPositionToolbarX(useLastPositionToolbarX);
                    connection.setUseLastPositionToolbarY(useLastPositionToolbarY);
                    connection.setUseLastPositionToolbarMoved(useLastPositionToolbarMoved);
                }
                break;
            case RemoteClientLibConstants.VV_OVER_HTTP_FAILURE:
                MessageDialogs.displayMessageAndFinish(context, R.string.error_failed_to_download_vv_http,
                        R.string.error_dialog_title);
                break;
            case RemoteClientLibConstants.VV_OVER_HTTPS_FAILURE:
                MessageDialogs.displayMessageAndFinish(context, R.string.error_failed_to_download_vv_https,
                        R.string.error_dialog_title);
            case RemoteClientLibConstants.VV_DOWNLOAD_TIMEOUT:
                MessageDialogs.displayMessageAndFinish(context, R.string.error_vv_download_timeout,
                        R.string.error_dialog_title);
            case RemoteClientLibConstants.PVE_FAILED_TO_PARSE_JSON:
                MessageDialogs.displayMessageAndFinish(context, R.string.error_pve_failed_to_parse_json,
                        R.string.error_dialog_title);
                break;
            case RemoteClientLibConstants.PVE_VMID_NOT_NUMERIC:
                MessageDialogs.displayMessageAndFinish(context, R.string.error_pve_vmid_not_numeric,
                        R.string.error_dialog_title);
                break;
            case RemoteClientLibConstants.PVE_API_UNEXPECTED_CODE:
                MessageDialogs.displayMessageAndFinish(context, R.string.error_pve_api_unexpected_code,
                        R.string.error_dialog_title, msg.getData().getString("error"));
                break;
            case RemoteClientLibConstants.PVE_API_IO_ERROR:
                MessageDialogs.displayMessageAndFinish(context, R.string.error_pve_api_io_error,
                        R.string.error_dialog_title, msg.getData().getString("error"));
                break;
            case RemoteClientLibConstants.PVE_TIMEOUT_COMMUNICATING:
                MessageDialogs.displayMessageAndFinish(context, R.string.error_pve_timeout_communicating,
                        R.string.error_dialog_title);
                break;
            case RemoteClientLibConstants.PVE_NULL_DATA:
                MessageDialogs.displayMessageAndFinish(context, R.string.error_pve_null_data,
                        R.string.error_dialog_title);
                break;
            case RemoteClientLibConstants.VV_FILE_ERROR:
                remoteConnection.disconnectAndShowMessage(R.string.vv_file_error, R.string.error_dialog_title);
                break;
            case RemoteClientLibConstants.NO_VM_FOUND_FOR_USER:
                remoteConnection.disconnectAndShowMessage(R.string.error_no_vm_found_for_user, R.string.error_dialog_title);
                break;
            case RemoteClientLibConstants.OVIRT_SSL_HANDSHAKE_FAILURE:
                remoteConnection.disconnectAndShowMessage(R.string.error_ovirt_ssl_handshake_failure, R.string.error_dialog_title);
                break;
            case RemoteClientLibConstants.VM_LOOKUP_FAILED:
                remoteConnection.disconnectAndShowMessage(R.string.error_vm_lookup_failed, R.string.error_dialog_title);
                break;
            case RemoteClientLibConstants.VM_LAUNCHED:
                remoteConnection.disconnectAndShowMessage(R.string.info_vm_launched_on_stand_by, R.string.info_dialog_title);
                break;
            case RemoteClientLibConstants.LAUNCH_VNC_VIEWER:
                Log.d(TAG, "Trying to launch VNC viewer");
                try {
                    String address = msg.getData().getString("address");
                    String port = msg.getData().getString("port");
                    String password = msg.getData().getString("password");
                    String uriString = "vnc://" + address + ":" + port + "?VncPassword=" + password;
                    Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uriString), "application/vnd.vnc");
                    context.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to launch VNC viewer: + " + e.getLocalizedMessage());
                    displayMessageAndFinish(R.string.info_dialog_title, R.string.message_please_install_bvnc, R.string.ok);
                    return;
                }
                remoteConnection.disconnectAndShowMessage(R.string.vnc_viewer_started, R.string.info_dialog_title);
                break;
            case RemoteClientLibConstants.GET_PASSWORD:
                showGetTextFragment(context.getString(R.string.enter_password),
                        GetTextFragment.DIALOG_ID_GET_OPAQUE_PASSWORD,
                        context.getString(R.string.enter_password),
                        obtainer, GetTextFragment.Password,
                        R.string.enter_password, R.string.enter_password,
                        connection.getPassword(), null, null,
                        connection.getKeepPassword());
                break;
            case RemoteClientLibConstants.GET_OTP_CODE:
                remoteConnection.pd.dismiss();
                showGetTextFragment(context.getString(R.string.enter_otp_code),
                        GetTextFragment.DIALOG_ID_GET_OPAQUE_OTP_CODE,
                        context.getString(R.string.enter_otp_code),
                        obtainer, GetTextFragment.Plaintext,
                        R.string.enter_otp_code, R.string.enter_otp_code,
                        null, null, null,
                        false);
                break;
            case RemoteClientLibConstants.PVE_FAILED_TO_AUTHENTICATE:
                if (remoteConnection.getVvFileName() != null) {
                    MessageDialogs.displayMessageAndFinish(context, R.string.error_pve_failed_to_authenticate,
                            R.string.error_dialog_title);
                } else {
                    remoteConnection.maintainConnection = false;
                    showGetTextFragment(context.getString(R.string.enter_password_auth_failed),
                            GetTextFragment.DIALOG_ID_GET_OPAQUE_CREDENTIALS,
                            context.getString(R.string.enter_password_auth_failed),
                            obtainer, GetTextFragment.Credentials,
                            R.string.enter_password_auth_failed, R.string.enter_password_auth_failed,
                            connection.getUserName(), connection.getPassword(), null,
                            connection.getKeepPassword());
                }
                break;
            case RemoteClientLibConstants.OVIRT_AUTH_FAILURE:
                if (remoteConnection.getVvFileName() != null) {
                    remoteConnection.disconnectAndShowMessage(R.string.error_ovirt_auth_failure, R.string.error_dialog_title);
                } else {
                    remoteConnection.maintainConnection = false;
                    showGetTextFragment(context.getString(R.string.enter_password_auth_failed),
                            GetTextFragment.DIALOG_ID_GET_OPAQUE_CREDENTIALS,
                            context.getString(R.string.enter_password_auth_failed),
                            obtainer, GetTextFragment.Credentials,
                            R.string.enter_password_auth_failed, R.string.enter_password_auth_failed,
                            connection.getUserName(), connection.getPassword(), null,
                            connection.getKeepPassword());
                }
                break;
            case RemoteClientLibConstants.DIALOG_DISPLAY_VMS:
                remoteConnection.pd.dismiss();
                ArrayList<String> vms = msg.getData().getStringArrayList("vms");

                if (vms != null && vms.size() > 0) {
                    fm = ((FragmentActivity) context).getSupportFragmentManager();
                    SelectTextElementFragment displayVms = SelectTextElementFragment.newInstance(
                            context.getString(R.string.select_vm_title),
                            vms, (RemoteCanvasActivity) context);
                    displayVms.setCancelable(false);
                    displayVms.show(fm, "selectVm");
                }
                break;
            case RemoteClientLibConstants.SPICE_CONNECT_SUCCESS:
                if (remoteConnection.pd != null && remoteConnection.pd.isShowing()) {
                    remoteConnection.pd.dismiss();
                }
                synchronized (remoteConnection.getRfbConn()) {
                    remoteConnection.spiceUpdateReceived = true;
                    remoteConnection.getRfbConn().notifyAll();
                }
                break;
            case RemoteClientLibConstants.SPICE_CONNECT_FAILURE:
                String e = msg.getData().getString("message");
                if (remoteConnection.maintainConnection) {
                    remoteConnection.maintainConnection = false;
                    if (remoteConnection.pd != null && remoteConnection.pd.isShowing()) {
                        remoteConnection.pd.dismiss();
                    }
                    // Only if we were intending to stay connected, and the connection failed, show an error message.
                    if (!remoteConnection.spiceUpdateReceived) {
                        remoteConnection.disconnectAndShowMessage(R.string.error_ovirt_unable_to_connect, R.string.error_dialog_title, e);
                    } else {
                        remoteConnection.disconnectAndShowMessage(R.string.error_connection_interrupted, R.string.error_dialog_title, e);
                    }
                }
                break;
            case RemoteClientLibConstants.OVIRT_TIMEOUT:
                remoteConnection.pd.dismiss();
                remoteConnection.disconnectAndShowMessage(R.string.error_ovirt_timeout, R.string.error_dialog_title);
                break;
            case RemoteClientLibConstants.DIALOG_X509_CERT:
                android.util.Log.d(TAG, "DIALOG_X509_CERT");
                X509Certificate cert = (X509Certificate) msg.obj;
                validateX509Cert(cert);
                break;
            case RemoteClientLibConstants.DISCONNECT_NO_MESSAGE:
                Log.i(TAG, "DISCONNECT_NO_MESSAGE");
                remoteConnection.closeConnection();
                Utils.justFinish(context);
                break;
            case RemoteClientLibConstants.SHOW_TOAST:
                this.post(() -> Toast.makeText(context, Utils.getStringResourceByName(context, messageText),
                        Toast.LENGTH_LONG).show());
                break;
            default:
                android.util.Log.e(TAG, "Not handling unknown messageId: " + msg.what);
                break;
        }
    }

    @Override
    public void onDownload(String contents) {
        Log.d(TAG, "onDownload completed with contents: " + contents);
        connection.setOvirtCaData(contents);
        connection.setUsingCustomOvirtCa(true);
        connection.save(context);
        setCertificateAcceptedAndNotifyListeners();
    }

    @Override
    public void onDownloadFailure() {
        Log.e(TAG, "onDownloadFailure");
    }
}
