/**
 * Copyright (C) 2012 Iordan Iordanov
 *
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */
package com.iiordanov.bVNC

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.iiordanov.bVNC.dialogs.GetTextFragment
import com.iiordanov.pubkeygenerator.PubkeyUtils
import com.trilead.ssh2.ConnectionInfo
import com.trilead.ssh2.InteractiveCallback
import com.trilead.ssh2.KnownHosts
import com.trilead.ssh2.Session
import com.undatech.opaque.Connection
import com.undatech.opaque.MessageDialogs
import com.undatech.opaque.RemoteClientLibConstants
import com.undatech.remoteClientUi.R
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Arrays
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/**
 * @author Iordan K Iordanov
 */
class SSHConnection(
    targetAddress: String, conn: Connection, cntxt: Context, handler: Handler
) : InteractiveCallback,
    GetTextFragment.OnFragmentDismissedListener {
    private val numPortTries = 1000
    private val connection: com.trilead.ssh2.Connection
    private var connectionInfo: ConnectionInfo? = null
    var serverHostKey: String? = null
        private set
    private var session: Session? = null
    private val passwordAuth = false
    private val keyboardInteractiveAuth = false
    private val pubKeyAuth = false
    private var kp: KeyPair? = null
    private var privateKey: PrivateKey? = null
    private var publicKey: PublicKey? = null

    // Connection parameters
    private val host: String
    private var user: String
    private var password: String
    private val vncpassword: String
    private var passphrase: String
    private val savedServerHostKey: String
    private val idHashAlg: Int
    val idHash // URI alternative to key
            : String? = null
    private val savedIdHash // alternative to key
            : String?
    private val targetAddress: String
    private val sshPort: Int
    private val usePubKey: Boolean
    private val sshPrivKey: String
    private val useSshRemoteCommand: Boolean
    private val sshRemoteCommandType: Int
    private val sshRemoteCommandTimeout = 0
    private val sshRemoteCommand: String
    private var remoteStdout: BufferedInputStream? = null
    private var remoteStderr: BufferedInputStream? = null
    private var remoteStdin: BufferedOutputStream? = null
    private val autoXEnabled: Boolean
    private val autoXType: Int
    private val autoXCommand: String
    private val autoXUnixpw: Boolean
    private val autoXRandFileNm: String
    private val context: Context
    private val handler: Handler
    private var sshPasswordAuthAttempts = 0
    private var sshKeyDecryptionAttempts = 0
    private val conn: Connection

    // Used to communicate the MFA verification code obtained.
    private var verificationCode: String
    private var userInputLatch: CountDownLatch
    private val executor = Executors.newSingleThreadExecutor()

    init {
        host = conn.sshServer
        sshPort = conn.sshPort
        user = conn.sshUser
        password = conn.sshPassword
        vncpassword = conn.password
        passphrase = conn.sshPassPhrase
        savedServerHostKey = conn.sshHostKey
        idHashAlg = conn.idHashAlgorithm
        savedIdHash = conn.idHash
        usePubKey = conn.useSshPubKey
        sshPrivKey = conn.sshPrivKey
        useSshRemoteCommand = conn.useSshRemoteCommand
        sshRemoteCommandType = conn.sshRemoteCommandType
        sshRemoteCommand = conn.sshRemoteCommand
        autoXEnabled = conn.autoXEnabled
        autoXType = conn.autoXType
        autoXCommand = conn.autoXCommand
        autoXUnixpw = conn.autoXUnixpw
        connection = com.trilead.ssh2.Connection(host, sshPort)
        autoXRandFileNm = conn.autoXRandFileNm
        context = cntxt
        userInputLatch = CountDownLatch(1)
        verificationCode = String()
        this.handler = handler
        this.conn = conn
        this.targetAddress = targetAddress
    }

    fun setVerificationCode(verificationCode: String) {
        this.verificationCode = verificationCode
        userInputLatch.countDown()
    }

    fun setUserAndPassword(sshUser: String, sshPassword: String, keep: Boolean) {
        user = sshUser
        password = sshPassword
        conn.sshUser = sshUser
        conn.sshPassword = sshPassword
        conn.keepSshPassword = keep
        conn.save(context);
        userInputLatch.countDown()
    }

    fun setPassphrase(sshPassphrase: String, keep: Boolean) {
        passphrase = sshPassphrase
        conn.sshPassPhrase = sshPassphrase
        conn.keepSshPassword = keep
        conn.save(context);
        userInputLatch.countDown()
    }

    @Throws(Exception::class)
    private fun attemptSshPasswordAuthentication() {
        Log.i(TAG, "attemptSshPasswordAuthentication")
        while (!authenticateWithPassword() && canAuthWithPass()) {
            sshPasswordAuthAttempts++
            if (sshPasswordAuthAttempts > MAX_AUTH_RETRIES) {
                throw Exception(context.getString(R.string.error_ssh_pwd_auth_fail))
            }
            userInputLatch = CountDownLatch(1)
            Log.i(TAG, "Requesting SSH password from user")
            handler.sendEmptyMessage(RemoteClientLibConstants.GET_SSH_CREDENTIALS)
            while (true) {
                try {
                    userInputLatch.await()
                    break
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun attemptSshKeyDecryption() {
        // Try to decrypt and recover keypair, and failing that, report error.
        kp = PubkeyUtils.decryptAndRecoverKeyPair(sshPrivKey, passphrase)
        while (kp == null) {
            sshKeyDecryptionAttempts++
            if (sshKeyDecryptionAttempts > MAX_DECRYPTION_ATTEMPTS) {
                throw Exception(context.getString(R.string.error_ssh_keypair_decryption_failure))
            }
            userInputLatch = CountDownLatch(1)
            Log.i(TAG, "Requesting SSH passphrase from user")
            handler.sendEmptyMessage(RemoteClientLibConstants.GET_SSH_PASSPHRASE)
            while (true) {
                try {
                    userInputLatch.await()
                    break
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            kp = PubkeyUtils.decryptAndRecoverKeyPair(sshPrivKey, passphrase)
        }
    }

    /**
     * Initializes the SSH Tunnel
     *
     * @return -1 if the target port was not determined, and the port obtained from x11vnc if it was
     * determined with AutoX.
     * @throws Exception
     */
    @Throws(Exception::class)
    fun initializeSSHTunnel(): Unit {
        // Attempt to connect.
        if (!connect()) throw Exception(context.getString(R.string.error_ssh_unable_to_connect))

        // Verify host key against saved one.
        if (!verifyHostKey()) {
            changeOrInitializeSshHostKey(conn.sshHostKey != "")
        }

        // Authenticate and set up port forwarding.
        if (!usePubKey) {
            authenticateViaPasswordIfSupported()
        } else {
            authenticateViaPubKey()
        }
    }

    /**
     * Sets up AutoX
     *
     * @return -1 if the target port was not determined, and the port obtained from x11vnc if it was
     * determined with AutoX.
     * @throws Exception
     */
    fun setupAutoX(): Int {
        var port1 = -1
        var tries = 0
        while (port1 < 0 && tries < MAXTRIES) {
            // If we're not using unix credentials, protect access with a temporary password file.
            if (!autoXUnixpw) {
                writeStringToRemoteCommand(
                    vncpassword, Constants.AUTO_X_CREATE_PASSWDFILE +
                            Constants.AUTO_X_PWFILEBASENAME + autoXRandFileNm +
                            Constants.AUTO_X_SYNC
                )
            }
            // Execute AutoX command.
            execRemoteCommand(autoXCommand, 1)

            // If we are looking for the greeter, we give the password to sudo's stdin.
            if (autoXType == Constants.AUTOX_SELECT_SUDO_FIND) writeStringToStdin(
                """
        $password
        
        """.trimIndent()
            )

            // Try to find PORT=
            port1 = parseRemoteStdoutForPort()
            if (port1 < 0) {
                session!!.close()
                tries++
                // Wait a little for x11vnc to recover.
                if (tries < MAXTRIES) try {
                    Thread.sleep((tries * 3500).toLong())
                } catch (e1: InterruptedException) {
                }
            }
        }
        if (port1 < 0) {
            throw Exception(
                """${context.getString(R.string.error_ssh_x11vnc_no_port_failure)}  
    
    ${context.getString(R.string.error)}:  
    
    ${bufferedInputStreamToString(remoteStderr)}"""
            )
        }
        return port1
    }

    private fun authenticateViaPubKey() {
        Log.i(TAG, "SSH tunnel is configured to use public key, will attempt")
        if (canAuthWithPubKey()) {
            Log.i(TAG, "SSH server supports pubkey authentication, continuing")
            // Pubkey auth method is allowed so try it.
            if (!authenticateWithPubKey()) {
                if (!canAuthWithPubKey()) {
                    // If pubkey authentication is now no longer available, we know pubkey
                    // authentication succeeded but the server wants further authentication.
                    Log.i(
                        TAG,
                        "SSH server needs more than key auth, trying password auth in addition"
                    )
                    MessageDialogs.displayToast(
                        context, handler,
                        context.getString(R.string.ssh_server_needs_password_in_addition_to_key),
                        Toast.LENGTH_LONG
                    )
                    attemptSshPasswordAuthentication()
                } else {
                    Log.e(TAG, "Failed to authenticate to SSH server with key")
                    throw Exception(context.getString(R.string.error_ssh_key_auth_fail))
                }
            }
        } else {
            // Pubkey authentication is not available, so try password if one was supplied.
            if (canAuthWithPass()) {
                Log.i(
                    TAG,
                    "Key auth enabled, but server is asking for password, trying password auth"
                )
                MessageDialogs.displayToast(
                    context, handler,
                    context.getString(R.string.ssh_server_needs_password_in_addition_to_key),
                    Toast.LENGTH_LONG
                )
                attemptSshPasswordAuthentication()
                Log.d(TAG, "isAuthenticationComplete: " + connection.isAuthenticationComplete)
                Log.d(
                    TAG,
                    "isAuthenticationPartialSuccess: " + connection.isAuthenticationPartialSuccess
                )
                if (!connection.isAuthenticationComplete) {
                    Log.i(
                        TAG,
                        "Key auth enabled, password authenticated succeeded, and server is asking for key auth"
                    )
                    // If password authentication is now no longer available, we know password
                    // authentication succeeded but the server wants further authentication.
                    if (!authenticateWithPubKey()) {
                        Log.e(TAG, "Key authentication failed")
                        throw Exception(context.getString(R.string.error_ssh_key_auth_fail))
                    }
                } else if (!connection.isAuthenticationComplete) {
                    Log.e(TAG, "Password authentication failed")
                    throw Exception(context.getString(R.string.error_ssh_pwd_auth_fail))
                }
            } else {
                Log.e(
                    TAG,
                    "SSH server does not support key auth, but SSH tunnel is configured to use it"
                )
                val authMethods = Arrays.toString(connection.getRemainingAuthMethods(user))
                throw Exception(
                    context.getString(R.string.error_ssh_pubkey_auth_method_unavail)
                            + " " + authMethods
                )
            }
        }
    }

    private fun authenticateViaPasswordIfSupported() {
        Log.i(TAG, "SSH tunnel not configured to use public key, trying password auth")
        if (!canAuthWithPass()) {
            Log.e(TAG, "SSH server does not support password authentication so throw an error")
            val authMethods = Arrays.toString(connection.getRemainingAuthMethods(user))
            throw Exception(context.getString(R.string.error_ssh_kbd_auth_method_unavail) + " " + authMethods)
        }
        attemptSshPasswordAuthentication()
    }

    /**
     * Creates a port forward to the given port and returns the local port forwarded.
     *
     * @return the local port forwarded to the given remote port
     * @throws Exception
     */
    @Throws(Exception::class)
    fun createLocalPortForward(port: Int): Int {
        val localForwardedPort: Int

        // At this point we know we are authenticated.
        localForwardedPort = createPortForward(port, targetAddress, port)
        // If we got back a negative number, port forwarding failed.
        if (localForwardedPort < 0) {
            throw Exception(context.getString(R.string.error_ssh_port_forwarding_failure))
        }
        return localForwardedPort
    }

    /**
     * Connects to remote server.
     *
     * @return
     */
    fun connect(): Boolean {
        return try {
            connection.setCompression(false)

            // TODO: Try using the provided KeyVerifier instead of verifying keys myself.
            connectionInfo = connection.connect(null, 6000, 24000)

            // Store a base64 encoded string representing the HostKey
            serverHostKey = Base64.encodeToString(connectionInfo?.serverHostKey, Base64.DEFAULT)

            // Get information on supported authentication methods we're interested in.
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Return a string holding a Hex representation of the signature of the remote host's key.
     */
    val hostKeySignature: String
        get() = KnownHosts.createHexFingerprint(
            connectionInfo!!.serverHostKeyAlgorithm,
            connectionInfo!!.serverHostKey
        )

    /**
     * Disconnects from remote server.
     */
    fun terminateSSHTunnel() {
        connection.close()
    }

    private fun verifyHostKey(): Boolean {
        // first check data against URI hash
        try {
            val rawKey = connectionInfo!!.serverHostKey
            val isValid = SecureTunnel.isSignatureEqual(idHashAlg, savedIdHash, rawKey)
            if (isValid) {
                Log.i(TAG, "Validated against provided hash.")
                return true
            }
        } catch (ex: Exception) {
        }
        // Because JSch returns the host key base64 encoded, and trilead ssh returns it not base64 encoded,
        // we compare savedHostKey to serverHostKey both base64 encoded and not.
        return savedServerHostKey == serverHostKey || savedServerHostKey == String(
            Base64.decode(
                serverHostKey,
                Base64.DEFAULT
            )
        )
    }

    /**
     * Returns whether the server can authenticate either with pass or keyboard-interactive methods.
     */
    private fun canAuthWithPass(): Boolean {
        return hasPasswordAuth() || hasKeyboardInteractiveAuth()
    }

    /**
     * Returns whether the server supports passworde
     *
     * @return
     */
    private fun hasPasswordAuth(): Boolean {
        var passwordAuth = false
        try {
            passwordAuth = connection.isAuthMethodAvailable(user, "password")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return passwordAuth
    }

    /**
     * Returns whether the server supports passworde
     *
     * @return
     */
    private fun hasKeyboardInteractiveAuth(): Boolean {
        var keyboardInteractiveAuth = false
        try {
            keyboardInteractiveAuth = connection.isAuthMethodAvailable(user, "keyboard-interactive")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return keyboardInteractiveAuth
    }

    /**
     * Returns whether the server can authenticate with a key.
     */
    private fun canAuthWithPubKey(): Boolean {
        var pubKeyAuth = false
        try {
            pubKeyAuth = connection.isAuthMethodAvailable(user, "publickey")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return pubKeyAuth
    }

    /**
     * Authenticates with a password.
     */
    private fun authenticateWithPassword(): Boolean {
        var isAuthenticated = false
        return try {
            if (hasKeyboardInteractiveAuth()) {
                Log.i(TAG, "Trying SSH keyboard-interactive authentication.")
                isAuthenticated = connection.authenticateWithKeyboardInteractive(user, this)
            }
            if (!isAuthenticated && hasPasswordAuth()) {
                Log.i(TAG, "Trying SSH password authentication. $user $password")
                isAuthenticated = connection.authenticateWithPassword(user, password)
            }
            isAuthenticated
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Decrypts and recovers the key pair.
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun decryptAndRecoverKey() {
        Log.i(TAG, "decryptAndRecoverKey")

        // Detect an empty key (not generated).
        if (sshPrivKey.length == 0) {
            Log.e(TAG, "SSH key not generated yet")
            throw Exception(context.getString(R.string.error_ssh_keypair_missing))
        }

        // Detect passphrase entered when key unencrypted and report error.
        if (passphrase.length != 0 && !PubkeyUtils.isEncrypted(sshPrivKey)) {
            Log.e(TAG, "SSH key not encrypted but passphrase was entered")
            throw Exception(context.getString(R.string.error_ssh_passphrase_but_keypair_unencrypted))
        }
        attemptSshKeyDecryption()
        privateKey = kp!!.private
        publicKey = kp!!.public
    }

    /**
     * Authenticates with a public/private key-pair.
     */
    @Throws(Exception::class)
    private fun authenticateWithPubKey(): Boolean {
        decryptAndRecoverKey()
        Log.i(TAG, "Trying SSH pubkey authentication.")
        return connection.authenticateWithPublicKey(user, kp)
    }

    private fun createPortForward(localPortStart: Int, remoteHost: String, remotePort: Int): Int {
        var portsTried = 0
        while (portsTried < numPortTries) {
            try {
                connection.createLocalPortForwarder(
                    InetSocketAddress("127.0.0.1", localPortStart + portsTried),
                    remoteHost, remotePort
                )
                return localPortStart + portsTried
            } catch (e: IOException) {
                portsTried++
            }
        }
        return -1
    }

    /**
     * Executes a remote command, and waits a certain amount of time.
     *
     * @param command    - the command to execute.
     * @param secTimeout - amount of time in seconds to wait afterward.
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun execRemoteCommand(command: String, secTimeout: Int) {
        Log.i(TAG, "Executing remote command: $command")
        try {
            session = connection.openSession()
            session?.execCommand(command)
            remoteStdout = BufferedInputStream(session?.stdout)
            remoteStderr = BufferedInputStream(session?.stderr)
            remoteStdin = BufferedOutputStream(session?.stdin)
            Thread.sleep((secTimeout * 1000).toLong())
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception(
                """${context.getString(R.string.error_ssh_could_not_exec_command)}  

${context.getString(R.string.error)}:  

${bufferedInputStreamToString(remoteStderr)}"""
            )
        }
    }

    /**
     * Writes the specified string to a stdin of a remote command.
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun writeStringToRemoteCommand(s: String, cmd: String) {
        Log.i(TAG, "Writing string to stdin of remote command: $cmd")
        execRemoteCommand(cmd, 0)
        remoteStdin!!.write(s.toByteArray())
        remoteStdin!!.flush()
        remoteStdin!!.close()
        session!!.close()
    }

    /**
     * Writes the specified string to a stdin of open session.
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun writeStringToStdin(s: String) {
        Log.i(TAG, "Writing string to remote stdin.")
        remoteStdin!!.write(s.toByteArray())
        remoteStdin!!.flush()
    }

    // TODO: This doesn't work at the moment.
    @Throws(Exception::class)
    private fun sendSudoPassword() {
        Log.i(TAG, "Sending sudo password.")
        try {
            remoteStdin?.write(
                """
    $password
    
    """.trimIndent().toByteArray()
            )
        } catch (e: IOException) {
            e.printStackTrace()
            throw Exception(
                """${context.getString(R.string.error_ssh_could_not_send_sudo_pwd)}  

${context.getString(R.string.error)}:  

${bufferedInputStreamToString(remoteStderr)}"""
            )
        }
    }

    /**
     * Parses the remote stdout for PORT=
     */
    private fun parseRemoteStdoutForPort(): Int {
        Log.i(TAG, "Parsing remote stdout for PORT=")
        val sought = "PORT="
        val soughtLength = sought.length
        var port = -1
        try {
            var data = 0
            var i = 0
            while (data != -1 && i < soughtLength) {
                data = remoteStdout!!.read()
                i = if (data == sought[i].code) {
                    i + 1
                } else {
                    0
                }
            }
            if (i == soughtLength) {
                // Read in 5 bytes after PORT=
                var buffer = ByteArray(5)
                remoteStdout!!.read(buffer)
                // Get rid of any whitespace (e.g. if the port is less than 5 digits).
                buffer = String(buffer).replace("\\s".toRegex(), "").toByteArray()
                port = String(buffer).toInt()
                Log.i(TAG, "Found PORT=, set to: $port")
            } else {
                Log.e(TAG, "Failed to find PORT= in remote stdout.")
                port = -1
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read from remote stdout.")
            e.printStackTrace()
            port = -1
        } catch (e: NumberFormatException) {
            Log.e(TAG, "Failed to parse integer.")
            e.printStackTrace()
            port = -1
        }
        return port
    }

    fun bufferedInputStreamToString(remote: BufferedInputStream?): String {
        var nRead: Int
        val dataBuf = ByteArray(1024)
        val bufferOs = ByteArrayOutputStream()
        try {
            while (remote!!.read(dataBuf, 0, dataBuf.size).also { nRead = it } != -1) {
                bufferOs.write(dataBuf, 0, nRead)
            }
            val output = bufferOs.toString("UTF-8")
            Log.d(TAG, "bufferedInputStreamToString:")
            Log.d(TAG, output)
            return output
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read from remote stdout.")
            e.printStackTrace()
        }
        return ""
    }

    /**
     * Used for keyboard-interactive authentication.
     */
    @Throws(Exception::class)
    override fun replyToChallenge(
        name: String, instruction: String,
        numPrompts: Int, prompt: Array<String>,
        echo: BooleanArray
    ): Array<String?> {
        val responses = arrayOfNulls<String>(numPrompts)
        for (x in 0 until numPrompts) {
            if (serverRequestsOtpCode(prompt)) {
                Log.i(TAG, prompt[x] + "  Will request verification code from user")
                if (Utils.isFree(context)) {
                    handler.sendEmptyMessage(RemoteClientLibConstants.PRO_FEATURE)
                    responses[x] = ""
                } else {
                    userInputLatch = CountDownLatch(1)
                    Log.i(TAG, "Requesting verification code from user")
                    handler.sendEmptyMessage(RemoteClientLibConstants.GET_VERIFICATIONCODE)
                    while (true) {
                        try {
                            userInputLatch.await()
                            break
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                    Log.i(TAG, prompt[0] + "  Sending verification code: " + verificationCode)
                    responses[x] = verificationCode
                }
            } else {
                Log.i(TAG, prompt[0] + "  Sending SSH password")
                responses[x] = password
            }
        }
        return responses
    }

    private fun serverRequestsOtpCode(prompt: Array<String>): Boolean {
        val line = prompt[0];
        return line.indexOf("Verification code:") != -1 || line.contains("OTP")
    }

    override fun onTextObtained(
        dialogId: String,
        obtainedStrings: Array<String>,
        dialogCancelled: Boolean,
        keep: Boolean
    ) {
        if (dialogCancelled) {
            userInputLatch.countDown()
            handler.sendEmptyMessage(RemoteClientLibConstants.DISCONNECT_NO_MESSAGE)
            return
        }
        when (dialogId) {
            GetTextFragment.DIALOG_ID_GET_VERIFICATION_CODE -> {
                Log.i(TAG, "Text obtained from DIALOG_ID_GET_VERIFICATION_CODE.")
                setVerificationCode(obtainedStrings[0])
            }

            GetTextFragment.DIALOG_ID_GET_SSH_CREDENTIALS -> {
                Log.i(TAG, "Text obtained from DIALOG_ID_GET_SSH_CREDENTIALS.")
                setUserAndPassword(obtainedStrings[0], obtainedStrings[1], keep)
            }

            GetTextFragment.DIALOG_ID_GET_SSH_PASSPHRASE -> {
                Log.i(TAG, "Text obtained from DIALOG_ID_GET_SSH_PASSPHRASE.")
                setPassphrase(obtainedStrings[0], keep)
            }

            else -> Log.e(TAG, "Unknown dialog type.")
        }
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun Any.wait() = (this as Object).wait()

    fun changeOrInitializeSshHostKey(keyChangeDetected: Boolean) {
        if (keyChangeDetected) {
            conn.sshHostKey = ""
            conn.idHash = ""
        }

        val sshTunneled = conn.connectionType == Constants.CONN_TYPE_SSH
        val emptySshHostKey = conn.sshHostKey == ""
        val emptySshHostKeyHash = Utils.isNullOrEmptry(conn.idHash)
        val keyUninitialized = emptySshHostKey && emptySshHostKeyHash
        if (sshTunneled && (keyChangeDetected || keyUninitialized)) {
            val message = Message()
            val messageData = Bundle()
            messageData.putBoolean("keyChangeDetected", keyChangeDetected)
            messageData.putString("idHash", idHash)
            messageData.putString("serverHostKey", serverHostKey)
            messageData.putString("hostKeySignature", hostKeySignature)
            message.what = RemoteClientLibConstants.DIALOG_SSH_CERT
            message.obj = messageData
            handler.sendMessage(message)

            // Block while user decides whether to accept certificate or not.
            // The activity ends if the user taps "No", so we block indefinitely here.
            synchronized(handler) {
                while (conn.sshHostKey == "") {
                    try {
                        handler.wait()
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "SSHConnection"
        private const val MAXTRIES = 3
        private const val MAX_AUTH_RETRIES = 3
        private const val MAX_DECRYPTION_ATTEMPTS = 3
    }
}