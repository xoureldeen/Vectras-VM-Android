package com.iiordanov.bVNC.protocol

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.iiordanov.bVNC.input.RemoteCanvasHandler
import com.undatech.opaque.Connection
import com.undatech.opaque.RemoteClientLibConstants
import com.undatech.opaque.Viewable
import com.undatech.opaque.proxmox.ProxmoxClient
import com.undatech.opaque.proxmox.pojo.VmStatus
import com.undatech.opaque.util.FileUtils
import com.undatech.remoteClientUi.R
import org.apache.http.HttpException
import org.json.JSONException
import java.io.File
import java.io.IOException
import javax.security.auth.login.LoginException

class RemoteProxmoxConnection(
    context: Context,
    connection: Connection?,
    canvas: Viewable,
    vvFileName: String?,
    hideKeyboardAndExtraKeys: Runnable,
) : RemoteOpaqueConnection(context, connection, canvas, vvFileName, hideKeyboardAndExtraKeys) {
    private val tag: String = "RemoteProxmoxConnection"

    fun retrieveVvFileFromPve(
        api: ProxmoxClient, vmId: String,
        node: String?, virt: String?
    ): String? {
        Log.i(tag, String.format("Trying to connect to PVE host: " + api.host))
        val tempVvFile = context.filesDir.toString() + "/tempfile.vv"
        FileUtils.deleteFile(tempVvFile)
        val cThread: Thread = object : Thread() {
            override fun run() {
                try {
                    var status = api.getCurrentStatus(node, virt, vmId.toInt())
                    if (status.status == VmStatus.STOPPED) {
                        api.startVm(node, virt, vmId.toInt())
                        while (status.status != VmStatus.RUNNING) {
                            status = api.getCurrentStatus(node, virt, vmId.toInt())
                            SystemClock.sleep(500)
                        }
                    }
                    val spiceData = api.spiceVm(node, virt, vmId.toInt())
                    if (spiceData != null) {
                        spiceData.outputToFile(tempVvFile, api.host)
                    } else {
                        Log.e(tag, "PVE returned null data for display.")
                        handler.sendEmptyMessage(RemoteClientLibConstants.PVE_NULL_DATA)
                    }
                } catch (e: LoginException) {
                    Log.e(tag, "Failed to login to PVE.")
                    handler.sendEmptyMessage(RemoteClientLibConstants.PVE_FAILED_TO_AUTHENTICATE)
                } catch (e: JSONException) {
                    Log.e(tag, "Failed to parse json from PVE.")
                    handler.sendEmptyMessage(RemoteClientLibConstants.PVE_FAILED_TO_PARSE_JSON)
                } catch (e: NumberFormatException) {
                    Log.e(tag, "Error converting PVE ID to integer.")
                    handler.sendEmptyMessage(RemoteClientLibConstants.PVE_VMID_NOT_NUMERIC)
                } catch (e: IOException) {
                    Log.e(tag, "IO Error communicating with PVE API: " + e.message)
                    handler.sendMessage(
                        RemoteCanvasHandler.getMessageString(
                            RemoteClientLibConstants.PVE_API_IO_ERROR,
                            "error", e.message
                        )
                    )
                    e.printStackTrace()
                } catch (e: HttpException) {
                    Log.e(tag, "PVE API returned error code: " + e.message)
                    handler.sendMessage(
                        RemoteCanvasHandler.getMessageString(
                            RemoteClientLibConstants.PVE_API_UNEXPECTED_CODE,
                            "error", e.message
                        )
                    )
                }
                // At this stage we have either retrieved display data or failed, so permit the UI thread to continue.
                synchronized(tempVvFile) { castAsObject(tempVvFile).notify() }
            }
        }
        cThread.start()

        // Wait until a timeout or until we are notified the worker thread trying to retrieve display data is done.
        synchronized(tempVvFile) {
            try {
                castAsObject(tempVvFile).wait()
            } catch (e: InterruptedException) {
                handler.sendEmptyMessage(RemoteClientLibConstants.PVE_TIMEOUT_COMMUNICATING)
                e.printStackTrace()
            }
        }
        val checkFile = File(tempVvFile)
        return if (!checkFile.exists() || checkFile.length() == 0L) {
            null
        } else tempVvFile
    }

    override fun startConnection() {
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
                    var user = connection.userName
                    var realm = RemoteClientLibConstants.PVE_DEFAULT_REALM

                    // Try to parse realm from user entered
                    val indexOfAt = connection.userName.indexOf('@')
                    if (indexOfAt != -1) {
                        realm = user.substring(indexOfAt + 1)
                        user = user.substring(0, indexOfAt)
                    }

                    // Connect to the API and obtain available realms
                    val api = ProxmoxClient(connection, handler)
                    val realms = api.availableRealms

                    // If selected realm has TFA enabled, then ask for the code
                    val pveRealm = realms[realm]
                    if (pveRealm != null && pveRealm.tfa != null) {
                        Log.i(tag, "Displaying a dialog to obtain OTP/TFA.")
                        handler.sendEmptyMessage(RemoteClientLibConstants.GET_OTP_CODE)
                        synchronized(handler) { castAsObject(handler).wait() }
                    }

                    // Login with provided credentials
                    api.login(user, realm, connection.password, connection.otpCode)

                    // Get map of user parsable names to resources
                    val nameToResources = api.resources
                    if (nameToResources.isEmpty()) {
                        Log.e(tag, "No available VMs found for user in PVE cluster")
                        disconnectAndShowMessage(R.string.error_no_vm_found_for_user, R.string.error_dialog_title)
                        return
                    }
                    var vmId = connection.vmname
                    if (vmId.matches("/".toRegex())) {
                        vmId = connection.vmname.replace(".*/".toRegex(), "")
                        connection.vmname = vmId
                        connection.save(context)
                    }
                    var node = ""
                    var virt = ""

                    // If there is just one VM, pick it and ignore what is saved in settings.
                    if (nameToResources.size == 1) {
                        Log.e(tag, "A single VM was found, so picking it.")
                        val key = nameToResources.keys.toTypedArray()[0] as String
                        val pveResource = nameToResources[key]
                        if (pveResource != null) {
                            node = pveResource.node
                            virt = pveResource.type
                            connection.vmname = pveResource.vmid
                            connection.save(context)
                        }
                    } else {
                        while (connection.vmname.isEmpty()) {
                            Log.i(tag, "PVE: Displaying a dialog with VMs to the user.")
                            // Populate the data structure that is used to convert VM names to IDs.
                            for (s in nameToResources.keys) {
                                val pveResource = nameToResources[s]
                                if (pveResource != null) {
                                    vmNameToId[pveResource.name + " (" + s + ")"] = s
                                }
                            }
                            // Get the user parsable names and display them
                            val vms = ArrayList(vmNameToId.keys)
                            handler.sendMessage(
                                RemoteCanvasHandler.getMessageStringList(
                                    RemoteClientLibConstants.DIALOG_DISPLAY_VMS, "vms", vms
                                )
                            )
                            synchronized(rfbConn) { castAsObject(rfbConn).wait() }
                        }

                        // At this point, either the user selected a VM or there was an ID saved.
                        if (nameToResources[connection.vmname] != null) {
                            val pveResource = nameToResources[connection.vmname]
                            if (pveResource != null) {
                                node = pveResource.node
                                virt = pveResource.type
                            }
                        } else {
                            Log.e(tag, "No VM with the following ID was found: " + connection.vmname)
                            disconnectAndShowMessage(
                                R.string.error_no_such_vm_found_for_user,
                                R.string.error_dialog_title
                            )
                            return
                        }
                    }
                    vmId = connection.vmname
                    // Only if we managed to obtain a VM name we try to get a .vv file for the display.
                    if (vmId.isNotEmpty()) {
                        val vvFileName = retrieveVvFileFromPve(api, vmId, node, virt)
                        vvFileName?.let { startFromVvFile(it) }
                    }
                } catch (e: LoginException) {
                    Log.e(tag, "Failed to login to PVE.")
                    handler.sendEmptyMessage(RemoteClientLibConstants.PVE_FAILED_TO_AUTHENTICATE)
                } catch (e: JSONException) {
                    Log.e(tag, "Failed to parse json from PVE.")
                    handler.sendEmptyMessage(RemoteClientLibConstants.PVE_FAILED_TO_PARSE_JSON)
                } catch (e: IOException) {
                    Log.e(tag, "IO Error communicating with PVE API: " + e.message)
                    handler.sendMessage(
                        RemoteCanvasHandler.getMessageString(
                            RemoteClientLibConstants.PVE_API_IO_ERROR,
                            "error", e.message
                        )
                    )
                    e.printStackTrace()
                } catch (e: HttpException) {
                    Log.e(tag, "PVE API returned error code: " + e.message)
                    handler.sendMessage(
                        RemoteCanvasHandler.getMessageString(
                            RemoteClientLibConstants.PVE_API_UNEXPECTED_CODE,
                            "error", e.message
                        )
                    )
                } catch (e: Throwable) {
                    handleUncaughtException(e, R.string.error_spice_unable_to_connect)
                }
            }
        }
        cThread.start()
    }

}