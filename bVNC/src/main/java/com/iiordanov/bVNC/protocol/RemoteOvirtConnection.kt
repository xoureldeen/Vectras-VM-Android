package com.iiordanov.bVNC.protocol

import android.content.Context
import android.util.Log
import com.iiordanov.bVNC.Utils
import com.iiordanov.bVNC.input.RemoteCanvasHandler
import com.undatech.opaque.Connection
import com.undatech.opaque.RemoteClientLibConstants
import com.undatech.opaque.Viewable
import com.undatech.opaque.proxmox.OvirtClient
import com.undatech.remoteClientUi.R
import java.io.File
import javax.security.auth.login.LoginException

class RemoteOvirtConnection(
    context: Context,
    connection: Connection?,
    canvas: Viewable,
    vvFileName: String?,
    hideKeyboardAndExtraKeys: Runnable,
) : RemoteOpaqueConnection(context, connection, canvas, vvFileName, hideKeyboardAndExtraKeys) {

    private val tag: String = "RemoteOvirtConnection"

    override fun startConnection() {
        connection.address = Utils.getHostFromUriString(connection.address)

        if (!pd.isShowing) pd.show()
        val cThread: Thread = object : Thread() {
            override fun run() {
                try {
                    // Obtain user's password if necessary.
                    if (connection.password == "") {
                        Log.i(tag, "Displaying a dialog to obtain user's password.")
                        handler.sendEmptyMessage(RemoteClientLibConstants.GET_PASSWORD)
                        synchronized(handler) { castAsObject(handler).wait() }
                    }
                    val ovirtClient = OvirtClient(connection, handler)
                    ovirtClient.trySsoLogin(connection.userName, connection.password)
                    val ssoToken = ovirtClient.accessToken
                    val ovirtCaFile: String = if (connection.isUsingCustomOvirtCa) {
                        connection.ovirtCaFile
                    } else {
                        File(context.filesDir, "ssl/certs/ca-certificates.crt").path
                    }

                    // If not VM name is specified, then get a list of VMs and let the user pick one.
                    if (connection.vmname == "") {
                        val success = spiceComm?.fetchOvirtVmNames(
                            connection.hostname,
                            connection.userName,
                            connection.password,
                            ovirtCaFile,
                            connection.isSslStrict,
                            ssoToken
                        )
                        // VM retrieval was unsuccessful we do not continue.
                        val vmNames = spiceComm?.vmNames ?: arrayListOf<String>()
                        if (success != 0 || vmNames.isEmpty()) {
                            return
                        } else {
                            // If there is just one VM, pick it and skip the dialog.
                            if (vmNames.size == 1) {
                                connection.vmname = vmNames[0]
                                connection.save(context)
                            } else {
                                while (connection.vmname == "") {
                                    Log.i(tag, "Displaying a dialog with VMs to the user.")
                                    // Populate the data structure that is used to convert VM names to IDs.
                                    for (s in vmNames) {
                                        vmNameToId[s] = s
                                    }
                                    handler.sendMessage(
                                        RemoteCanvasHandler.getMessageStringList(
                                            RemoteClientLibConstants.DIALOG_DISPLAY_VMS,
                                            "vms", vmNames
                                        )
                                    )
                                    synchronized(rfbConn) { castAsObject(rfbConn).wait() }
                                }
                            }
                        }
                    }
                    spiceComm?.handler = handler
                    spiceComm?.connectOvirt(
                        connection.hostname,
                        connection.vmname,
                        connection.userName,
                        connection.password,
                        ovirtCaFile,
                        connection.isAudioPlaybackEnabled,
                        connection.isSslStrict,
                        ssoToken
                    )
                    try {
                        synchronized(rfbConn) { castAsObject(rfbConn).wait(35000) }
                    } catch (e: InterruptedException) {
                        Log.w(tag, "Wait for SPICE connection interrupted.")
                    }
                    if (!spiceUpdateReceived && maintainConnection) {
                        handler.sendEmptyMessage(RemoteClientLibConstants.OVIRT_TIMEOUT)
                    }
                } catch (e: LoginException) {
                    Log.e(tag, "Failed to login to oVirt.")
                    handler.sendEmptyMessage(RemoteClientLibConstants.OVIRT_AUTH_FAILURE)
                } catch (e: Throwable) {
                    handleUncaughtException(e, R.string.error_spice_unable_to_connect)
                }
            }
        }
        cThread.start()
    }
}