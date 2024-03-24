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
import com.undatech.opaque.MessageDialogs
import com.undatech.opaque.RemoteClientLibConstants
import com.undatech.opaque.SpiceCommunicator
import com.undatech.opaque.Viewable
import com.undatech.remoteClientUi.R
import java.io.File

open class RemoteOpaqueConnection(
    context: Context,
    connection: Connection?,
    canvas: Viewable,
    vvFileName: String?,
    hideKeyboardAndExtraKeys: Runnable,
) : RemoteConnection(context, connection, canvas, vvFileName, hideKeyboardAndExtraKeys) {
    private val tag: String = "RemoteOpaqueConnection"
    protected var spiceComm: SpiceCommunicator? = null

    override fun initializeConnection() {
        super.initializeConnection()
        checkNetworkConnectivity()
        spiceComm = SpiceCommunicator(
            context, handler, canvas,
            connection.isRequestingNewDisplayResolution || connection.rdpResType == RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM,
            !Utils.isFree(context) && connection.isUsbEnabled, App.debugLog
        )
        rfbConn = spiceComm
        pointer = RemoteSpicePointer(spiceComm, context, this, canvas, handler, App.debugLog)
        try {
            keyboard = RemoteSpiceKeyboard(
                context.resources, spiceComm, canvas, this,
                handler, connection.layoutMap, App.debugLog
            )
        } catch (e: Throwable) {
            handleUncaughtException(e, R.string.error_spice_unable_to_connect)
        }
        maintainConnection = true
        if (vvFileName == null) {
            startConnection()
        } else {
            Log.d(tag, "Initializing session from vv file: $vvFileName")
            val f = File(vvFileName)
            if (!f.exists()) {
                // Quit with an error if the file does not exist.
                MessageDialogs.displayMessageAndFinish(context, R.string.vv_file_not_found, R.string.error_dialog_title)
            }
            startFromVvFile(vvFileName)
        }
        initializeClipboardMonitor()
    }

    open fun startConnection() {

    }

    open fun startFromVvFile(vvFileName: String?) {
        val cThread: Thread = object : Thread() {
            override fun run() {
                try {
                    spiceComm?.startSessionFromVvFile(vvFileName, connection.isAudioPlaybackEnabled)
                } catch (e: Throwable) {
                    handleUncaughtException(e, R.string.error_spice_unable_to_connect)
                }
            }
        }
        cThread.start()
    }

    override fun getSshTunnelTargetAddress(): String = ""

    override fun isColorModel(cm: COLORMODEL) = false
    override fun setColorModel(cm: COLORMODEL?) {}

    @Throws(java.lang.Exception::class)
    override fun correctAfterRotation() {
        if (connection.isRequestingNewDisplayResolution) {
            spiceComm?.requestResolution(canvas.width, canvas.height)
        }
    }

    protected fun <T> castAsObject(someObject: T) = (someObject as Object)
}