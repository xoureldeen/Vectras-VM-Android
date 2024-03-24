package com.iiordanov.bVNC.protocol

import android.content.Context
import android.util.Log
import com.iiordanov.bVNC.App
import com.iiordanov.bVNC.COLORMODEL
import com.iiordanov.bVNC.input.RemoteRdpKeyboard
import com.iiordanov.bVNC.input.RemoteRdpPointer
import com.undatech.opaque.Connection
import com.undatech.opaque.RdpCommunicator
import com.undatech.opaque.Viewable
import com.undatech.remoteClientUi.R

class RemoteRdpConnection(
    context: Context,
    connection: Connection?,
    canvas: Viewable,
    vvFileName: String?,
    hideKeyboardAndExtraKeys: Runnable,
) : RemoteConnection(context, connection, canvas, vvFileName, hideKeyboardAndExtraKeys) {
    private val tag: String = "RemoteRdpConnection"
    private var rdpComm: RdpCommunicator? = null

    /**
     * Initializes an RDP connection.
     */
    private fun initializeRdpConnection() {
        Log.i(tag, "initializeRdpConnection: Initializing RDP connection.")
        rdpComm = RdpCommunicator(
            connection, context, handler, canvas,
            connection.userName, connection.rdpDomain, connection.password,
            App.debugLog
        )
        rfbConn = rdpComm
        pointer = RemoteRdpPointer(rfbConn, context, this, canvas, handler, App.debugLog)
        keyboard = RemoteRdpKeyboard(
            rdpComm, canvas, this, handler, App.debugLog,
            connection.preferSendingUnicode
        )
    }

    /**
     * Starts an RDP connection using the FreeRDP library.
     */
    @Throws(Exception::class)
    private fun startRdpConnection() {
        Log.i(tag, "startRdpConnection: Starting RDP connection.")

        // Get the address and port (based on whether an SSH tunnel is being established or not).
        val address = address
        val gatewayAddress = getGatewayAddress()
        val rdpPort = getRemoteProtocolPort(connection.port)
        val gatewayPort = getGatewayPort(connection.rdpGatewayPort)
        canvas.waitUntilInflated()
        val remoteWidth = canvas.getRemoteWidth(canvas.width, canvas.height)
        val remoteHeight = canvas.getRemoteHeight(canvas.width, canvas.height)
        rdpComm!!.setConnectionParameters(
            address, rdpPort,
            connection.rdpGatewayEnabled, gatewayAddress, gatewayPort,
            connection.rdpGatewayUsername, connection.rdpGatewayDomain, connection.rdpGatewayPassword,
            connection.nickname, remoteWidth,
            remoteHeight, connection.desktopBackground, connection.fontSmoothing,
            connection.desktopComposition, connection.windowContents,
            connection.menuAnimation, connection.visualStyles,
            connection.redirectSdCard, connection.consoleMode,
            connection.remoteSoundType, connection.enableRecording,
            connection.remoteFx, connection.enableGfx, connection.enableGfxH264,
            connection.rdpColor
        )
        rdpComm!!.connect()
        pd.dismiss()
    }

    override fun initializeConnection() {
        super.initializeConnection()

        try {
            initializeRdpConnection()
        } catch (e: Throwable) {
            handleUncaughtException(e, R.string.error_rdp_unable_to_connect)
        }
        initializeClipboardMonitor()

        val t: Thread = object : Thread() {
            override fun run() {
                try {
                    startRdpConnection()
                } catch (e: Throwable) {
                    handleUncaughtException(e, R.string.error_rdp_unable_to_connect)
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
            if (sshTunneled && !connection.rdpGatewayEnabled) {
                constructSshConnectionIfNeeded()
                sshConnection.createLocalPortForward(port)
            } else {
                port
            }
        return result
    }


    /**
     * If necessary, initializes an SSH tunnel and returns local forwarded port, or
     * if SSH tunneling is not needed, returns the given port.
     */
    @Throws(java.lang.Exception::class)
    private fun getGatewayPort(port: Int): Int {
        var result = port
        if (sshTunneled) {
            constructSshConnectionIfNeeded()
            result = sshConnection.createLocalPortForward(port)
        }
        return result
    }

    /**
     * Returns localhost if using SSH tunnel or saved address.
     */
    override fun getAddress(): String? {
        return if (sshTunneled && !connection.rdpGatewayEnabled) {
            "127.0.0.1"
        } else {
            connection.address
        }
    }


    /**
     * Returns localhost if using SSH tunnel or saved address.
     */
    private fun getGatewayAddress(): String? {
        return if (sshTunneled) {
            "127.0.0.1"
        } else {
            connection.rdpGatewayHostname
        }
    }

    override fun getSshTunnelTargetAddress(): String? {
        var address = connection.address
        if (connection.rdpGatewayEnabled) {
            address = connection.rdpGatewayHostname
        }
        return address
    }

    override fun isColorModel(cm: COLORMODEL) = false
    override fun setColorModel(cm: COLORMODEL?) {}

    @Throws(java.lang.Exception::class)
    override fun correctAfterRotation() {
    }
}