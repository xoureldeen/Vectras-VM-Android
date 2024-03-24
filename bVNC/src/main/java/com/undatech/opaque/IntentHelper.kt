package com.undatech.opaque

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.iiordanov.bVNC.ConnectionBean
import com.iiordanov.bVNC.Constants
import com.iiordanov.bVNC.Utils
import com.undatech.opaque.util.ConnectionLoader
import com.undatech.opaque.util.GeneralUtils

class IntentHelper {

    fun getIntent(
        connectionLoader: ConnectionLoader, runtimeId: String, appContext: Context, packageContext: Context
    ): Intent {
        var intent = getIntentForRemoteCanvasActivity(packageContext)
        if (Utils.isOpaque(packageContext)) {
            val cs = connectionLoader.connectionsById[runtimeId] as ConnectionSettings?
            cs!!.loadFromSharedPreferences(appContext)
            intent.putExtra(Constants.opaqueConnectionSettingsClassPath, cs)
        } else {
            val conn = connectionLoader.connectionsById[runtimeId] as ConnectionBean?
            if (conn!!.requiresVpn) {
                intent = getUriIntentForVpnClient(packageContext, conn)
            } else {
                intent.putExtra(Utils.getConnectionString(appContext), conn.Gen_getValues())
            }
        }
        return intent
    }

    private fun getIntentForRemoteCanvasActivity(packageContext: Context) =
        Intent(packageContext, GeneralUtils.getClassByName(Constants.remoteCanvasActivityClassPath))

    private fun getUriIntentForVpnClient(
        packageContext: Context, conn: ConnectionBean
    ): Intent {
        val tunneledProtocol = getProtocol(packageContext)
        return Intent(
            Intent.ACTION_VIEW
        ).setData(
            getUri(conn, tunneledProtocol)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }

    private fun getProtocol(packageContext: Context): String {
        var protocol = "VNC"
        if (Utils.isRdp(packageContext)) {
            protocol = "RDP"
        } else if (Utils.isSpice(packageContext)) {
            protocol = "SPICE"
        }
        return protocol
    }

    private fun getUri(conn: ConnectionBean, tunneledProtocol: String): Uri? = Uri.parse(
        String.format(
            "%s://%s:%s/tunnel?tunnelAction=start&remotePort=%s&tunneledProtocol=%s&externalId=%s",
            conn.vpnUriScheme,
            conn.address,
            conn.port,
            conn.port,
            tunneledProtocol,
            conn.externalId
        )
    )

}
