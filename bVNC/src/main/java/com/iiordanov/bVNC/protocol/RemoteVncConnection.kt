package com.iiordanov.bVNC.protocol

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import com.iiordanov.bVNC.App
import com.iiordanov.bVNC.COLORMODEL
import com.iiordanov.bVNC.Constants
import com.iiordanov.bVNC.Decoder
import com.iiordanov.bVNC.RfbProto
import com.iiordanov.bVNC.RfbProto.RfbPasswordAuthenticationException
import com.iiordanov.bVNC.RfbProto.RfbUltraVncColorMapException
import com.iiordanov.bVNC.RfbProto.RfbUserPassAuthFailedOrUsernameRequiredException
import com.iiordanov.bVNC.Utils
import com.iiordanov.bVNC.exceptions.AnonCipherUnsupportedException
import com.iiordanov.bVNC.input.RemoteVncKeyboard
import com.iiordanov.bVNC.input.RemoteVncPointer
import com.tigervnc.rfb.AuthFailureException
import com.undatech.opaque.Connection
import com.undatech.opaque.RemoteClientLibConstants
import com.undatech.opaque.Viewable
import com.undatech.remoteClientUi.R
import kotlin.math.max
import kotlin.math.min

class RemoteVncConnection(
    context: Context,
    connection: Connection?,
    canvas: Viewable,
    vvFileName: String?,
    hideKeyboardAndExtraKeys: Runnable,
) : RemoteConnection(context, connection, canvas, vvFileName, hideKeyboardAndExtraKeys) {
    private val tag: String = "RemoteVncConnection"
    private var rfb: RfbProto? = null

    /**
     * Determines the preferred remote width for VNC connections.
     */
    private fun getVncRemoteWidth(viewWidth: Int, viewHeight: Int): Int {
        var remoteWidth = 0
        val reqWidth = connection.rdpWidth
        val reqHeight = connection.rdpHeight
        if (connection.rdpResType == Constants.VNC_GEOM_SELECT_CUSTOM && reqWidth >= 2 && reqHeight >= 2) {
            remoteWidth = reqWidth
        } else if (connection.rdpResType == Constants.VNC_GEOM_SELECT_AUTOMATIC) {
            remoteWidth = viewWidth
        } else if (connection.rdpResType == Constants.VNC_GEOM_SELECT_NATIVE_PORTRAIT) {
            remoteWidth = min(viewWidth, viewHeight)
        } else if (connection.rdpResType == Constants.VNC_GEOM_SELECT_NATIVE_LANDSCAPE) {
            remoteWidth = max(viewWidth, viewHeight)
        }
        // We make the resolution even if it is odd.
        if (remoteWidth % 2 == 1) remoteWidth--
        return remoteWidth
    }

    /**
     * Determines the preferred remote height for VNC connections.
     */
    private fun getVncRemoteHeight(viewWidth: Int, viewHeight: Int): Int {
        var remoteHeight = 0
        val reqWidth = connection.rdpWidth
        val reqHeight = connection.rdpHeight
        if (connection.rdpResType == Constants.VNC_GEOM_SELECT_CUSTOM && reqWidth >= 2 && reqHeight >= 2) {
            remoteHeight = reqHeight
        } else if (connection.rdpResType == Constants.VNC_GEOM_SELECT_AUTOMATIC) {
            remoteHeight = viewHeight
        } else if (connection.rdpResType == Constants.VNC_GEOM_SELECT_NATIVE_PORTRAIT) {
            remoteHeight = max(viewWidth, viewHeight)
        } else if (connection.rdpResType == Constants.VNC_GEOM_SELECT_NATIVE_LANDSCAPE) {
            remoteHeight = min(viewWidth, viewHeight)
        }
        // We make the resolution even if it is odd.
        if (remoteHeight % 2 == 1) remoteHeight--
        return remoteHeight
    }

    /**
     * Initializes a VNC connection.
     */
    private fun initializeVncConnection() {
        Log.i(tag, "Initializing connection to: " + connection.address + ", port: " + connection.port)
        val sslTunneled = connection.connectionType == Constants.CONN_TYPE_STUNNEL
        decoder = Decoder(canvas, this, connection.useLocalCursor == Constants.CURSOR_FORCE_LOCAL)
        rfb = RfbProto(
            decoder, canvas, this, handler, connection.prefEncoding, connection.viewOnly,
            sslTunneled, connection.idHashAlgorithm, connection.idHash, connection.x509KeySignature,
            App.debugLog
        )
        rfbConn = rfb
        pointer = RemoteVncPointer(rfbConn, context, this, canvas, handler, App.debugLog)
        val rAltAsIsoL3Shift = Utils.querySharedPreferenceBoolean(
            this.context,
            Constants.rAltAsIsoL3ShiftTag
        )
        keyboard = RemoteVncKeyboard(
            rfbConn, this, context, handler,
            rAltAsIsoL3Shift, App.debugLog
        )
    }

    /**
     * Starts a VNC connection
     */
    @Throws(Exception::class)
    private fun startVncConnection() {
        try {
            val address = address
            val vncPort = getRemoteProtocolPort(connection.port)
            Log.i(
                tag,
                "Establishing VNC session to: $address, port: $vncPort"
            )
            val sslCert = connection.x509KeySignature
            rfb?.initializeAndAuthenticate(
                address, vncPort, connection.userName,
                connection.password, connection.useRepeater,
                connection.repeaterId, connection.connectionType,
                sslCert
            )
        } catch (e: AnonCipherUnsupportedException) {
            showFatalMessageAndQuit(context.getString(R.string.error_anon_dh_unsupported))
        } catch (e: RfbPasswordAuthenticationException) {
            Log.e(tag, "Authentication failed, will prompt user for password")
            handler.sendEmptyMessage(RemoteClientLibConstants.GET_VNC_PASSWORD)
            return
        } catch (e: RfbUserPassAuthFailedOrUsernameRequiredException) {
            Log.e(tag, "Auth failed or username required, prompting for username and password")
            handler.sendEmptyMessage(RemoteClientLibConstants.GET_VNC_CREDENTIALS)
            return
        } catch (e: AuthFailureException) {
            Log.e(tag, "TigerVNC AuthFailureException: " + e.localizedMessage)
            handler.sendEmptyMessage(RemoteClientLibConstants.GET_VNC_CREDENTIALS)
            return
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception(
                context.getString(R.string.error_vnc_unable_to_connect) +
                        Utils.messageAndStackTraceAsString(e)
            )
        }
        rfb?.writeClientInit()
        rfb?.readServerInit()

        // Is custom resolution enabled?
        if (connection.rdpResType != Constants.VNC_GEOM_SELECT_DISABLED) {
            canvas.waitUntilInflated()
            rfb?.setPreferredFramebufferSize(
                getVncRemoteWidth(canvas.width, canvas.height),
                getVncRemoteHeight(canvas.width, canvas.height)
            )
        }
        canvas.reallocateDrawable(rfb!!.framebufferWidth, rfb!!.framebufferHeight)
        decoder.setPixelFormat(rfb)
        handler.post { pd.setMessage(context.getString(R.string.info_progress_dialog_downloading)) }
        sendUnixAuth()
        canvas.postDrawableSetter()

        // Hide progress dialog
        if (pd.isShowing) pd.dismiss()
        try {
            rfb?.processProtocol()
        } catch (e: RfbUltraVncColorMapException) {
            Log.e(tag, "UltraVnc supports only 24bpp. Switching color mode and reconnecting.")
            connection.colorModel = COLORMODEL.C24bit.nameString()
            connection.save(context)
            handler.sendEmptyMessage(RemoteClientLibConstants.REINIT_SESSION)
        }
    }

    override fun initializeConnection() {
        super.initializeConnection()

        try {
            initializeVncConnection()
        } catch (e: Throwable) {
            handleUncaughtException(e, R.string.error_vnc_unable_to_connect)
        }
        initializeClipboardMonitor()

        val t: Thread = object : Thread() {
            override fun run() {
                try {
                    startVncConnection()
                } catch (e: Throwable) {
                    handleUncaughtException(e, R.string.error_vnc_unable_to_connect)
                }
            }
        }
        t.start()
    }


    /**
     * If necessary, initializes an SSH tunnel, attempts AutoX discovery, and returns local forwarded port, or
     * if SSH tunneling is not needed, returns the given port after converts VNC ports into real ports if needed.
     */
    @Throws(java.lang.Exception::class)
    fun getRemoteProtocolPort(port: Int): Int {
        val result: Int
        if (sshTunneled) {
            constructSshConnectionIfNeeded()
            var remotePort = port
            if (connection.autoXEnabled) {
                remotePort = sshConnection.setupAutoX()
            }
            result = sshConnection.createLocalPortForward(remotePort)
        } else {
            result = if (port <= 20) {
                Constants.DEFAULT_VNC_PORT + port
            } else {
                port
            }
        }
        return result
    }

    /**
     * Sends over the unix username and password if this is VNC over SSH connection and automatic sending of
     * UNIX credentials is enabled for AutoX (for the x11vnc "-unixpw" option).
     */
    private fun sendUnixAuth() {
        // If the type of connection is ssh-tunneled and we are told to send the unix credentials, then do so.
        if (sshTunneled && connection.autoXUnixAuth) {
            keyboard.keyEvent(
                KeyEvent.KEYCODE_UNKNOWN, KeyEvent(
                    SystemClock.uptimeMillis(),
                    connection.sshUser, 0, 0
                )
            )
            keyboard.keyEvent(KeyEvent.KEYCODE_ENTER, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            keyboard.keyEvent(KeyEvent.KEYCODE_ENTER, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            keyboard.keyEvent(
                KeyEvent.KEYCODE_UNKNOWN, KeyEvent(
                    SystemClock.uptimeMillis(),
                    connection.sshPassword, 0, 0
                )
            )
            keyboard.keyEvent(KeyEvent.KEYCODE_ENTER, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            keyboard.keyEvent(KeyEvent.KEYCODE_ENTER, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    override fun getSshTunnelTargetAddress(): String? {
        return connection.address
    }


    override fun isColorModel(cm: COLORMODEL): Boolean {
        return if (decoder != null) {
            decoder.colorModel != null && decoder.colorModel == cm
        } else {
            false
        }
    }

    override fun setColorModel(cm: COLORMODEL?) {
        if (decoder != null) {
            decoder.colorModel = cm
        }
    }

    @Throws(java.lang.Exception::class)
    override fun correctAfterRotation() {
        if (connection.rdpResType == Constants.VNC_GEOM_SELECT_AUTOMATIC) {
            rfbConn.requestResolution(canvas.width, canvas.height)
        }
    }

    override fun canUpdateColorModelConnected(): Boolean = true
}