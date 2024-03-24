package com.iiordanov.bVNC

import android.content.Context
import android.os.Handler
import android.util.Log
import com.iiordanov.bVNC.dialogs.GetTextFragment
import com.undatech.opaque.Connection
import com.undatech.opaque.RemoteClientLibConstants
import com.undatech.opaque.RfbConnectable

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class CredentialsObtainer(
    val connection: Connection,
    val handler: Handler,
    val context: Context,
) : GetTextFragment.OnFragmentDismissedListener {
    private val tag: String = "CredentialsObtainer"

    override fun onTextObtained(dialogId: String?, obtainedString: Array<String?>, dialogCancelled: Boolean, save: Boolean) {
        if (dialogCancelled) {
            handler.sendEmptyMessage(RemoteClientLibConstants.DISCONNECT_NO_MESSAGE)
            return
        }
        when (dialogId) {
            GetTextFragment.DIALOG_ID_GET_VNC_CREDENTIALS -> {
                Log.i(tag, "Text obtained from DIALOG_ID_GET_VNC_CREDENTIALS.")
                connection.userName = obtainedString[0]
                connection.password = obtainedString[1]
                connection.keepPassword = save
                connection.save(context)
                handler.sendEmptyMessage(RemoteClientLibConstants.REINIT_SESSION)
            }

            GetTextFragment.DIALOG_ID_GET_VNC_PASSWORD -> {
                Log.i(tag, "Text obtained from DIALOG_ID_GET_VNC_PASSWORD.")
                connection.password = obtainedString[0]
                connection.keepPassword = save
                connection.save(context)
                handler.sendEmptyMessage(RemoteClientLibConstants.REINIT_SESSION)
            }

            GetTextFragment.DIALOG_ID_GET_RDP_CREDENTIALS -> {
                Log.i(tag, "Text obtained from DIALOG_ID_GET_RDP_CREDENTIALS.")
                connection.userName = obtainedString[0]
                connection.rdpDomain = obtainedString[1]
                connection.password = obtainedString[2]
                connection.keepPassword = save
                connection.save(context)
                handler.sendEmptyMessage(RemoteClientLibConstants.REINIT_SESSION)
            }

            GetTextFragment.DIALOG_ID_GET_RDP_GATEWAY_CREDENTIALS -> {
                Log.i(tag, "Text obtained from DIALOG_ID_GET_RDP_GATEWAY_CREDENTIALS.")
                connection.rdpGatewayUsername = obtainedString[0]
                connection.rdpGatewayDomain = obtainedString[1]
                connection.rdpGatewayPassword = obtainedString[2]
                connection.keepRdpGatewayPassword = save
                connection.save(context)
                handler.sendEmptyMessage(RemoteClientLibConstants.REINIT_SESSION)
            }

            GetTextFragment.DIALOG_ID_GET_SPICE_PASSWORD -> {
                Log.i(tag, "Text obtained from DIALOG_ID_GET_SPICE_PASSWORD.")
                connection.password = obtainedString[0]
                connection.keepPassword = save
                connection.save(context)
                handler.sendEmptyMessage(RemoteClientLibConstants.REINIT_SESSION)
            }

            GetTextFragment.DIALOG_ID_GET_OPAQUE_CREDENTIALS -> {
                Log.i(tag, "Text obtained from DIALOG_ID_GET_OPAQUE_CREDENTIALS")
                connection.userName = obtainedString[0]
                connection.password = obtainedString[1]
                connection.keepPassword = save
                connection.save(context)
                handler.sendEmptyMessage(RemoteClientLibConstants.REINIT_SESSION)
            }

            GetTextFragment.DIALOG_ID_GET_OPAQUE_PASSWORD -> {
                Log.i(tag, "Text obtained from DIALOG_ID_GET_OPAQUE_PASSWORD")
                connection.password = obtainedString[0]
                connection.keepPassword = save
                connection.save(context)
                synchronized(handler) {
                    (handler as Object).notify()
                }
            }

            GetTextFragment.DIALOG_ID_GET_OPAQUE_OTP_CODE -> {
                Log.i(tag, "Text obtained from DIALOG_ID_GET_OPAQUE_OTP_CODE")
                connection.otpCode = obtainedString[0]
                synchronized(handler) {
                    (handler as Object).notify()
                }
            }

            else -> Log.e(tag, "Unknown dialog type.")
        }
    }
}