/*
   Copyright (C) 2022 Iordan Iordanov

   This library is free software; you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public
   License as published by the Free Software Foundation; either
   version 2.1 of the License, or (at your option) any later version.

   This library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public
   License along with this library; if not, see <http://www.gnu.org/licenses/>.
*/

#include <jni.h>
#include "android-service.h"
#include "android-clipboard.h"
#include <glib/gi18n.h>
#include "gobject/gsignal.h"

spice_clipboard* spice_clipboard_alloc() {
    __android_log_write(ANDROID_LOG_INFO, "spice_clipboard_alloc", "Allocating clipboard");
    spice_clipboard* clipboard = malloc(sizeof(spice_clipboard));
    clipboard->buffer = malloc(CLIPBOARD_MAX_LENGTH);
    clipboard->available = CLIPBOARD_MAX_LENGTH;
    clipboard->length = 0;
    return clipboard;
}

void spice_clipboard_connect_signals(SpiceChannel* channel, spice_connection *conn) {
        g_signal_connect(channel, "main-clipboard-selection",
                G_CALLBACK(spice_clipboard_selection_handler), conn);
        g_signal_connect(channel, "main-clipboard-selection-grab",
                G_CALLBACK(spice_clipboard_selection_grab_handler), conn);
        g_signal_connect(channel, "main-clipboard-selection-release",
                G_CALLBACK(spice_clipboard_selection_release_handler), conn);
        g_signal_connect(channel, "main-clipboard-selection-request",
                G_CALLBACK(spice_clipboard_selection_request_handler), conn);
}

void spice_clipboard_selection_handler(
    SpiceMainChannel* channel, guint selection, guint type, gpointer data, guint size, spice_connection *conn) {
    __android_log_write(ANDROID_LOG_INFO, "android-spice",
        "spice_clipboard_selection_handler: Appending clipboard to buffer and sending clipboard selection to UI");

    if (selection != VD_AGENT_CLIPBOARD_SELECTION_CLIPBOARD) {
        __android_log_write(ANDROID_LOG_ERROR, "android-spice", "Ignoring selection other than clipboard");
        return;
    }

    if (type != VD_AGENT_CLIPBOARD_UTF8_TEXT) {
        __android_log_write(ANDROID_LOG_ERROR, "android-spice", "Non-text clipboard selection not supported");
        return;
    }

    clipboard->length = MIN(size, clipboard->available - 1);
    strncpy(clipboard->buffer, data, clipboard->length);
    strncpy(clipboard->buffer + clipboard->length, "\0", 1);
    JNIEnv *env = NULL;
    gboolean attached = attachThreadToJvm(&env);
    jstring jdata = (*env)->NewStringUTF(env, clipboard->buffer);
    (*env)->CallStaticVoidMethod(env, jni_connector_class, jni_remote_clipboard_changed, jdata);
    if (attached) {
        detachThreadFromJvm ();
    }
}

void spice_clipboard_selection_grab_handler(
    SpiceMainChannel* channel, guint selection, guint32* types, guint ntypes, spice_connection *conn) {
    __android_log_write(ANDROID_LOG_INFO, "android-spice",
        "spice_clipboard_selection_grab_handler: Notifying client of clipboard grab in VM");

    if (selection != VD_AGENT_CLIPBOARD_SELECTION_CLIPBOARD) {
        __android_log_write(ANDROID_LOG_ERROR, "android-spice", "Unsupported clipboard selection type");
        return;
    }

    // Loop through the data types sent by the Spice server and process them.
    for (int i = 0; i < ntypes; i++) {
        if (types[i] != VD_AGENT_CLIPBOARD_UTF8_TEXT) {
            __android_log_write(ANDROID_LOG_ERROR, "android-spice", "Unsupported clipboard data type");
            continue;
        }

        spice_main_channel_clipboard_selection_request(channel, selection, types[i]);
    }
}

void spice_clipboard_selection_release_handler(
    SpiceMainChannel* channel, guint selection, spice_connection *conn) {
    __android_log_write(ANDROID_LOG_INFO, "android-spice",
        "spice_clipboard_selection_release_handler: Clipboard released in VM, sending buffer to UI");
    strncpy(clipboard->buffer, (const char *)"\0", 1);
    clipboard->length = 0;
}

void spice_clipboard_selection_request_handler(
    SpiceMainChannel* channel, guint selection, guint type, spice_connection *conn) {
    __android_log_write(ANDROID_LOG_INFO, "android-spice",
        "spice_clipboard_selection_request_handler: Getting clipboard data from UI");

    if (selection != VD_AGENT_CLIPBOARD_SELECTION_CLIPBOARD) {
        __android_log_write(ANDROID_LOG_ERROR, "android-spice", "Ignoring selection other than clipboard");
        return;
    }

    if (type != VD_AGENT_CLIPBOARD_UTF8_TEXT) {
        __android_log_write(ANDROID_LOG_ERROR, "android-spice", "Ignoring clipboard selection other than text");
        return;
    }

    spice_main_channel_clipboard_selection_notify(
        channel,
        selection,
        type,
        (const guchar *)clipboard->buffer,
        clipboard->length
    );
}


void spice_clipboard_selection_grab(SpiceMainChannel* channel, const guchar* text, size_t size) {
    clipboard->length = MIN(size, clipboard->available - 1);
    strncpy(clipboard->buffer, (const char *)text, clipboard->length);
    guint32 clipboard_types[] = { VD_AGENT_CLIPBOARD_UTF8_TEXT };
    spice_main_channel_clipboard_selection_grab(
            channel,
            VD_AGENT_CLIPBOARD_SELECTION_CLIPBOARD,
            clipboard_types,
            1
    );
}