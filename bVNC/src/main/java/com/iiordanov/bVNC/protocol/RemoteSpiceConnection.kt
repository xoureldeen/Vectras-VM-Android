package com.iiordanov.bVNC.protocol

import android.content.Context
import android.util.Log
import com.iiordanov.bVNC.App
import com.iiordanov.bVNC.COLORMODEL
import com.iiordanov.bVNC.Constants
import com.iiordanov.bVNC.Utils
import com.iiordanov.bVNC.input.RemoteSpiceKeyboard
import com.iiordanov.bVNC.input.RemoteSpicePointer
import com.undatech.opaque.Connection
import com.undatech.opaque.RemoteClientLibConstants
import com.undatech.opaque.SpiceCommunicator
import com.undatech.opaque.Viewable
import com.undatech.remoteClientUi.R

class RemoteSpiceConnection(
    context: Context,
    connection: Connection?,
    canvas: Viewable,
    vvFileName: String?,
    hideKeyboardAndExtraKeys: Runnable,
) : RemoteConnection(context, connection, canvas, vvFileName, hideKeyboardAndExtraKeys) {
    private val tag: String = "RemoteVncConnection"
    private var spiceComm: SpiceCommunicator? = null

    /**
     * Initializes a SPICE connection.
     */
    @Throws(Exception::class)
    private fun initializeSpiceConnection() {
        Log.d(tag, "initializeSpiceConnection")
        spiceComm = SpiceCommunicator(
            context, handler, canvas, true,
            !Utils.isFree(context) && connection.isUsbEnabled,
            App.debugLog
        )
        rfbConn = spiceComm
        pointer = RemoteSpicePointer(spiceComm, context, this, canvas, handler, App.debugLog)
        keyboard = RemoteSpiceKeyboard(
            context.resources, spiceComm, canvas, this,
            handler, connection.layoutMap, App.debugLog
        )
    }

    /**
     * Starts a SPICE connection using libspice.
     */
    @Throws(Exception::class)
    private fun startSpiceConnection() {
        Log.d(tag, "startSpiceConnection")
        // Get the address and port (based on whether an SSH tunnel is being established or not).
        val address = address
        // To prevent an SSH tunnel being created when port or TLS port is not set, we only
        // getPort when port/tlsPort are positive.
        var port = connection.port
        if (port > 0) {
            port = getRemoteProtocolPort(port)
        }
        var tlsPort = connection.tlsPort
        if (tlsPort > 0) {
            tlsPort = getRemoteProtocolPort(tlsPort)
        }
        spiceComm?.connectSpice(
            address, port.toString(), tlsPort.toString(), connection.password,
            connection.caCertPath, null,  // TODO: Can send connection.getCaCert() here instead
            connection.certSubject, connection.enableSound
        )
    }

    override fun initializeConnection() {
        super.initializeConnection()

        try {
            initializeSpiceConnection()
        } catch (e: Throwable) {
            handleUncaughtException(e, R.string.error_spice_unable_to_connect)
        }
        initializeClipboardMonitor()

        val t: Thread = object : Thread() {
            override fun run() {
                try {
                    startSpiceConnection()
                } catch (e: Throwable) {
                    handleUncaughtException(e, R.string.error_spice_unable_to_connect)
                }
            }
        }
        t.start()
    }

    /**
     * If necessary, initializes an SSH tunnel and returns local forwarded port, or
     * if SSH tunneling is not needed, returns the given port.
     */
    @Throws(java.lang.Exception::class)
    fun getRemoteProtocolPort(port: Int): Int {
        val result =
            if (sshTunneled) {
                constructSshConnectionIfNeeded()
                sshConnection.createLocalPortForward(port)
            } else {
                port
            }
        return result
    }

    override fun getSshTunnelTargetAddress(): String? {
        return connection.address
    }

    override fun isColorModel(cm: COLORMODEL) = false
    override fun setColorModel(cm: COLORMODEL?) {}

    @Throws(java.lang.Exception::class)
    override fun correctAfterRotation() {
        if (connection.rdpResType == RemoteClientLibConstants.RDP_GEOM_SELECT_AUTO) {
            spiceComm?.requestResolution(canvas.width, canvas.height)
        }
    }
}