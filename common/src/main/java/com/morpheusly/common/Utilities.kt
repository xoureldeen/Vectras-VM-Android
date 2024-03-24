package com.morpheusly.common

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class Utilities {
    companion object {
        private const val TAG = "Utilities"
        private const val BUFFER_SIZE = 3000

        /**
         * Outputs the given InputStream to a file.
         * @param toOutput
         * @param out
         * @throws IOException
         */
        fun outputToStream(toOutput: String, out: OutputStream) {
            val bos = BufferedOutputStream(out)
            try {
                bos.write(toOutput.toByteArray())
                bos.close()
                out.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun readFileToUtf8String(i: InputStream?): String? {
            val bis = BufferedInputStream(i)
            val buffer = ByteArrayOutputStream()
            try {
                val data = ByteArray(BUFFER_SIZE)
                var current: Int
                while (bis.read(data, 0, data.size).also { current = it } != -1) {
                    buffer.write(data, 0, current)
                }
                i?.close()
                return buffer.toString("UTF-8")
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return ""
        }

        fun getInputStreamFromUri(resolver: ContentResolver, uri: Uri?): InputStream? {
            var stream: InputStream? = null
            try {
                stream = resolver.openInputStream(uri!!)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return stream
        }

        fun getOutputStreamFromUri(resolver: ContentResolver, uri: Uri?): OutputStream? {
            var out: OutputStream? = null
            try {
                out = resolver.openOutputStream(uri!!, "wt")
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return out
        }

        fun getStringDataFromIntent(data: Intent, activity: Activity): String? {
            val resolver = activity.contentResolver
            val stream: InputStream? = getInputStreamFromUri(resolver, data.data)
            return readFileToUtf8String(stream)
        }

        fun importCaCertFromFile(activity: Activity, requestCode: Int) {
            Log.d(TAG, "importCaCertFromFile")
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            intent.putExtra(
                Intent.EXTRA_MIME_TYPES, arrayOf(
                    "*/*"
                )
            )
            activity.startActivityForResult(intent, requestCode)
        }

        fun toList(array: Array<String>): List<String> {
            return array.toList()
        }
    }
}