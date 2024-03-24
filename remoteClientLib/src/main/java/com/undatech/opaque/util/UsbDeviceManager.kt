package com.undatech.opaque.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.undatech.opaque.RemoteClientLibConstants

class UsbDeviceManager(val context: Context, val usbEnabled: Boolean) {
    private val TAG: String = this::javaClass.name
    var mUsbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    var requested: HashMap<UsbDevice, Pair<Boolean, Int>> = HashMap<UsbDevice, Pair<Boolean, Int>>()

    fun getUnrequested(): UsbDevice? {
        var attached: MutableCollection<UsbDevice> = this.mUsbManager.getDeviceList().values
        return (attached subtract requested.keys).firstOrNull()
    }

    fun getRemoved(): UsbDevice? {
        var attached: MutableCollection<UsbDevice> = this.mUsbManager.getDeviceList().values
        return (requested.keys subtract attached).firstOrNull()
    }

    fun getFileDescriptorForDevice(device: UsbDevice): Int {
        val res = requested[device]
        return res?.second ?: -1
    }

    fun isRequested(device: UsbDevice) = requested.containsKey(device)

    fun setPermission(device: UsbDevice, permission: Boolean, fDesc: Int) {
        requested[device] = Pair(permission, fDesc)
    }

    fun getPermission(device: UsbDevice): Boolean {
        var res = requested[device]?.first
        if (res != true) {
            res = false
        }
        return res
    }

    @Synchronized
    fun removeRequested(device: UsbDevice) {
        val temp = requested.clone() as HashMap<UsbDevice, Pair<Boolean, Int>>
        temp.remove(device)
        requested = temp
    }

    fun getFileDescriptorForUsbDevice(usbDevice: UsbDevice): Int {
        if (mUsbManager.hasPermission(usbDevice)) {
            val usbDeviceConnection = mUsbManager.openDevice(usbDevice)
            if (usbDeviceConnection != null) {
                return usbDeviceConnection.fileDescriptor
            } else {
                Log.w(TAG, "No permission for device: $usbDevice")
            }
        }
        return -1
    }

    fun getUsbDevicePermissions() {
        if (!this.usbEnabled) {
            Log.i(TAG, "Not requesting permissions for USB devices, because USB is disabled")
            return
        }
        Log.i(TAG, "Requesting permissions for all USB devices")
        val d = this.getUnrequested()
        if (d != null) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            };
            val mPermissionIntent = PendingIntent.getBroadcast(
                context, 0,
                Intent(RemoteClientLibConstants.ACTION_USB_PERMISSION), flags
            )
            Log.d(TAG, "Requesting permission to usbDevice: $d")
            mUsbManager.requestPermission(d, mPermissionIntent)
        } else {
            Log.d(TAG, "No unrequested devices left.")
        }
    }
}