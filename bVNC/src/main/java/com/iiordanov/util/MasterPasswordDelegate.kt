package com.iiordanov.util

import android.content.Context
import android.util.Log
import com.iiordanov.bVNC.Constants
import com.iiordanov.bVNC.Database
import com.iiordanov.bVNC.Utils
import com.undatech.remoteClientUi.R

class MasterPasswordDelegate(val context: Context, val database: Database) {
    private val TAG: String? = "MasterPasswordDelegate"

    public fun checkMasterPasswordAndQuitIfWrong(
        providedPassword: String,
        dialogWasCancelled: Boolean
    ): Boolean {
        Log.i(TAG, "checkMasterPasswordAndQuitIfWrong: Just checking the password.")
        var result = false
        if (dialogWasCancelled) {
            Log.i(TAG, "Dialog cancelled, so quitting.")
            Utils.showFatalErrorMessage(
                context,
                context.getResources().getString(R.string.master_password_error_password_necessary)
            )
        } else if (database.checkMasterPassword(providedPassword, context)) {
            Log.i(TAG, "Entered password is correct, so proceeding.")
            Database.setPassword(providedPassword)
            result = true
        } else {
            // Finish the activity if the password was wrong.
            Log.i(TAG, "Entered password is wrong, so quitting.")
            Utils.showFatalErrorMessage(
                context,
                context.getResources().getString(R.string.master_password_error_wrong_password)
            )
        }
        return result
    }

    public fun toggleMasterPassword(
        providedPassword: String,
        dialogWasCancelled: Boolean
    ): Boolean {
        Log.i(
            TAG,
            "toggleMasterPassword: User asked to toggle master password."
        )
        return if (Utils.querySharedPreferenceBoolean(
                context,
                Constants.masterPasswordEnabledTag
            )
        ) {
            checkAndDisableMasterPassword(providedPassword, dialogWasCancelled)
        } else {
            enableMasterPassword(providedPassword, dialogWasCancelled)
        }
    }

    private fun enableMasterPassword(
        providedPassword: String,
        dialogWasCancelled: Boolean
    ): Boolean {
        Log.i(TAG, "enableMasterPassword: Master password was disabled.")
        var result = false
        if (!dialogWasCancelled) {
            Log.i(TAG, "Setting master password.")
            Database.setPassword("")
            if (database.changeDatabasePassword(providedPassword)) {
                Utils.toggleSharedPreferenceBoolean(context, Constants.masterPasswordEnabledTag)
                result = true
            } else {
                Database.setPassword("")
                Utils.showErrorMessage(
                    context, context.getResources()
                        .getString(R.string.master_password_error_failed_to_enable)
                )
            }
        } else {
            // No need to show error message because user cancelled consciously.
            Log.i(TAG, "Dialog cancelled, not setting master password.")
            Utils.showErrorMessage(
                context,
                context.getResources().getString(R.string.master_password_error_password_not_set)
            )
        }
        return result
    }

    private fun checkAndDisableMasterPassword(
        providedPassword: String,
        dialogWasCancelled: Boolean
    ): Boolean {
        Log.i(
            TAG,
            "checkAndDisableMasterPassword: User wants to disable master password"
        )
        var result = false
        // Master password is enabled
        if (dialogWasCancelled) {
            Log.i(TAG, "Dialog cancelled, so quitting.")
            Utils.showFatalErrorMessage(
                context,
                context.getResources().getString(R.string.master_password_error_password_necessary)
            )
        } else if (database.checkMasterPassword(providedPassword, context)) {
            Log.i(TAG, "Entered password was correct, disabling password.")
            result = disableMasterPassword(providedPassword)
        } else {
            Log.i(
                TAG,
                "Entered password is wrong or dialog cancelled, so quitting."
            )
            Utils.showFatalErrorMessage(
                context,
                context.getResources().getString(R.string.master_password_error_wrong_password)
            )
        }
        return result
    }

    private fun disableMasterPassword(providedPassword: String): Boolean {
        Log.i(TAG, "disableMasterPassword")
        var result = false
        Database.setPassword(providedPassword)
        if (database.changeDatabasePassword("")) {
            Utils.toggleSharedPreferenceBoolean(context, Constants.masterPasswordEnabledTag)
            result = true
        } else {
            Utils.showErrorMessage(
                context,
                context.getResources().getString(R.string.master_password_error_failed_to_disable)
            )
        }
        return result
    }

}