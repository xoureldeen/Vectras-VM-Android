/**
 * Copyright (C) 2013 Iordan Iordanov
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

#include <jni.h>
#include <android/bitmap.h>

#include "android-spicy.h"
#include "android-spice-widget.h"
#include "android-clipboard.h"
#include "virt-viewer-file.h"
#include "libusb.h"
#include <android/log.h>

#define PTRFLAGS_DOWN 0x8000

#ifdef ANDROID_SERVICE_C
SpiceDisplay*   global_display = NULL;
spice_connection*  global_conn = NULL;
JavaVM*                    jvm = NULL;
JNIEnv*                  g_env = NULL;
jclass    jni_connector_class  = NULL;
jmethodID jni_settings_changed = NULL;
jmethodID jni_graphics_update  = NULL;
jmethodID jni_mouse_update     = NULL;
jmethodID jni_mouse_mode       = NULL;
jmethodID jni_show_message     = NULL;
jmethodID jni_remote_clipboard_changed = NULL;
GMainLoop            *mainloop = NULL;
int                connections = 0;
gboolean          soundEnabled = FALSE;
gboolean          soundInitialized = FALSE;
const gchar*         oVirtUser = NULL;
const gchar*     oVirtPassword = NULL;
spice_clipboard* clipboard     = NULL;
#else
extern SpiceDisplay *global_display;
extern spice_connection *global_conn;
extern JavaVM *jvm;
extern JNIEnv *g_env;
extern jclass jni_connector_class;
extern jmethodID jni_settings_changed;
extern jmethodID jni_graphics_update;
extern jmethodID jni_mouse_update;
extern jmethodID jni_mouse_mode;
extern jmethodID jni_show_message;
extern jmethodID jni_remote_clipboard_changed;
extern GMainLoop *mainloop;
extern int connections;
extern gboolean soundEnabled;
extern gboolean soundInitialized;
extern gchar *oVirtUser;
extern gchar *oVirtPassword;
extern spice_clipboard *clipboard;
#endif

inline gboolean attachThreadToJvm(JNIEnv **env) {
    gboolean attached = FALSE;
    int rs2 = 0;
    int rs1 = (*jvm)->GetEnv(jvm, (void **) env, JNI_VERSION_1_6);
    switch (rs1) {
        case JNI_OK:
            break;
        case JNI_EDETACHED:
            rs2 = (*jvm)->AttachCurrentThread(jvm, env, NULL);
            if (rs2 != JNI_OK) {
                __android_log_write(ANDROID_LOG_ERROR, "android-io", "ERROR: Could not attach current thread to jvm.");
            } else {
                attached = TRUE;
            }
            break;
    }

    return attached;
}

inline void detachThreadFromJvm() {
    (*jvm)->DetachCurrentThread(jvm);
}

int spiceClientConnect(const gchar *h, const gchar *p, const gchar *tp,
                       const gchar *pw, const gchar *cf, GByteArray *cc,
                       const gchar *cs, const gboolean sound, const gchar *proxy);

int spiceClientConnectVv(VirtViewerFile *vv_file, const gboolean sound);

int connectSession(spice_connection *conn);

int SpiceClientConnect(JNIEnv *env, jobject obj, const gchar *h, const gchar *p,
                       const gchar *tp, const gchar *pw, const gchar *cf, GByteArray *cc, const gchar *cs,
                       const gboolean sound, VirtViewerFile *vv_file);

int CreateOvirtSession(JNIEnv *env, jobject obj, const gchar *uri, const gchar *user, const gchar *password,
                       const gchar *ovirt_ca_file, const gboolean sound, const gboolean sslStrict,
                       const gboolean didPowerOn, const gchar *sso_token, const gboolean onlyFetchVmNames);

int openUsbDevice(int vid, int pid);

int get_usb_device_fd(libusb_device *device);

void sendMessage(JNIEnv *env, const int messageID, const gchar *message_text);
