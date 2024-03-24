/**
 * Copyright (C) 2023 Iordan Iordanov
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

package com.iiordanov.util

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.Inet6Address
import java.net.InetAddress

object NetworkUtils {
    private const val tag: String = "NetworkUtils"

    fun isValidIpv6Address(address: String?): Boolean {
        return tryRunningCoroutineWithTimeout(
            {
                InetAddress.getByName(address) is Inet6Address
            },
            Dispatchers.IO
        )
    }

    fun tryRunningCoroutineWithTimeout(
        block: (CoroutineScope) -> Boolean?, dispatcher: CoroutineDispatcher
    ): Boolean {
        return runBlocking {
            this.tryRunningWithTimeoutAsync(block, false, 2000L, dispatcher)
        } ?: false
    }

    private suspend inline fun <T, R> T.tryRunningWithTimeoutAsync(
        crossinline block: T.() -> R, default: R, timeout: Long, dispatcher: CoroutineDispatcher
    ): R {
        return try {
            withTimeout(timeout) {
                withContext(dispatcher) {
                    block()
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "tryRunningWithTimeoutAsync: Exception caught, default $default returned. Exception: '$e'")
            default
        }
    }
}