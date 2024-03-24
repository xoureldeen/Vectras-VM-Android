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

#include <android/bitmap.h>

#include "android-spice-widget.h"
#include "android-spice-widget-priv.h"
#include "win32keymap.h"
#include "android-io.h"
#include "android-service.h"
#include "spice/vd_agent.h"

JNIEXPORT void JNICALL
Java_com_undatech_opaque_SpiceCommunicator_UpdateBitmap (JNIEnv* env, jobject obj, jobject bitmap, gint x, gint y, gint width, gint height) {
    uchar* pixels;
    SpiceDisplayPrivate *d = SPICE_DISPLAY_GET_PRIVATE(global_display);

    if (AndroidBitmap_lockPixels(env, bitmap, (void**)&pixels) < 0) {
        __android_log_write(ANDROID_LOG_ERROR, "android-io", "AndroidBitmap_lockPixels() failed!");
        return;
    }

    int slen = d->width * 4;
    int offset = (slen * y) + (x * 4);
    uchar *source = d->data;
    uchar *sourcepix = (uchar*) &source[offset];
    uchar *destpix   = (uchar*) &pixels[offset];

    for (int i = 0; i < height; i++) {
        for (int j = 0; j < width * 4; j += 4) {
            destpix[j + 0] = sourcepix[j + 2];
            destpix[j + 1] = sourcepix[j + 1];
            destpix[j + 2] = sourcepix[j + 0];
            destpix[j + 3] = 0xff;
        }
        sourcepix = sourcepix + slen;
        destpix   = destpix + slen;
    }

    AndroidBitmap_unlockPixels(env, bitmap);
}

int win32key2spice (int keycode)
{
    int newKeyCode = keymap_win322xtkbd[keycode];
    /*
    char buf[100];
    snprintf (buf, 100, "Converted win32 key: %d to linux key: %d", keycode, newKeyCode);
    __android_log_write(ANDROID_LOG_DEBUG, "android-io", buf);
    */
    return newKeyCode;
}

static int update_mask (int button, gboolean down) {
    static int mask;
    int update = 0;
    if (button == SPICE_MOUSE_BUTTON_LEFT)
        update = SPICE_MOUSE_BUTTON_MASK_LEFT;
    else if (button == SPICE_MOUSE_BUTTON_MIDDLE)
        update = SPICE_MOUSE_BUTTON_MASK_MIDDLE;
    else if (button == SPICE_MOUSE_BUTTON_RIGHT)
        update = SPICE_MOUSE_BUTTON_MASK_RIGHT;
    if (down) {
        mask |= update;
    } else {
        mask &= ~update;
    }
    return mask;
}


/* JNI functions related to input (keyboard, mouse), and output (display). */
/***************************************************************************/


JNIEXPORT void JNICALL
Java_com_undatech_opaque_SpiceCommunicator_SpiceRequestResolution(JNIEnv* env, jobject obj, jint x, jint y) {
    __android_log_write(ANDROID_LOG_INFO, "android-io", "SpiceRequestResolution");
    SpiceDisplay* display = global_display;
    SpiceDisplayPrivate *d = SPICE_DISPLAY_GET_PRIVATE(display);

    spice_main_channel_update_display_enabled(d->main, get_display_id(display), TRUE, FALSE);
    spice_main_channel_update_display(d->main, get_display_id(display), 0, 0, x, y, TRUE);
    spice_main_channel_send_monitor_config(d->main);
}


JNIEXPORT void JNICALL
Java_com_undatech_opaque_SpiceCommunicator_SpiceKeyEvent(JNIEnv * env, jobject  obj, jboolean down, jint hardware_keycode) {
    SpiceDisplay* display = global_display;
    SpiceDisplayPrivate* d = SPICE_DISPLAY_GET_PRIVATE(display);

    SPICE_DEBUG("%s %s: keycode: %d", __FUNCTION__, "Key", hardware_keycode);

    if (!d->inputs)
        return;

    if (down) {
        send_key(display, hardware_keycode, 1);
    } else {
        send_key(display, hardware_keycode, 0);
    }
}


JNIEXPORT void JNICALL
Java_com_undatech_opaque_SpiceCommunicator_SpiceButtonEvent(JNIEnv * env, jobject  obj, jint x,
                                                            jint y, jint metaState, jint type,
                                                            jboolean relative) {
    SpiceDisplay* display = global_display;
    SpiceDisplayPrivate *d = SPICE_DISPLAY_GET_PRIVATE(display);
    //char buf[60];
    //snprintf (buf, 60, "Pointer event: %d at x: %d, y: %d, guestX %d, guestY %d", type, x, y,
    //          d->mouse_guest_x, d->mouse_guest_y);
    //__android_log_write(ANDROID_LOG_DEBUG, "android-io", buf);

    if (d->inputs) {
        gboolean down = (type & PTRFLAGS_DOWN) != 0;
        int mouseButton = type &~ PTRFLAGS_DOWN;
        int newMask = update_mask (mouseButton, down);

        switch (d->mouse_mode) {
        case SPICE_MOUSE_MODE_CLIENT:
            //__android_log_write(ANDROID_LOG_DEBUG, "android-io", "spice mouse mode client");
            if (relative) {
                __android_log_write(ANDROID_LOG_ERROR, "android-io",
                                            "Relative mouse events sent in mouse mode client.");
                uiCallbackMouseMode(env, false);
            } else if (x >= 0 && x < d->width && y >= 0 && y < d->height) {
                spice_inputs_position(d->inputs, x, y, d->channel_id, newMask);
            } else {
                __android_log_write(ANDROID_LOG_ERROR, "android-io",
                                            "Absolute coordinates outside server resolution.");
            }
            break;
        case SPICE_MOUSE_MODE_SERVER:
            //__android_log_write(ANDROID_LOG_DEBUG, "android-io", "spice mouse mode server");
            if (!relative) {
                __android_log_write(ANDROID_LOG_ERROR, "android-io",
                                            "Absolute mouse event sent in mouse mode server");
                uiCallbackMouseMode(env, true);
            } else {
                spice_inputs_motion(d->inputs, x, y, newMask);
                d->mouse_last_x = d->mouse_last_x == -1 ? 0 : d->mouse_last_x - x;
                d->mouse_last_y = d->mouse_last_y == -1 ? 0 : d->mouse_last_y - y;
            }
            break;
        default:
            g_warn_if_reached();
            break;
        }

        if (mouseButton != SPICE_MOUSE_BUTTON_INVALID) {
            if (down) {
                //__android_log_write(ANDROID_LOG_DEBUG, "android-io", "Button press");
                spice_inputs_button_press(d->inputs, mouseButton, newMask);
            } else {
                //__android_log_write(ANDROID_LOG_DEBUG, "android-io", "Button release");
                // This sleep is an ugly hack to prevent stuck buttons after a drag/drop gesture.
                usleep(50000);
                spice_inputs_button_release(d->inputs, mouseButton, newMask);
            }
        }
    }
}

JNIEXPORT void JNICALL
Java_com_undatech_opaque_SpiceCommunicator_SpiceClientCutText(JNIEnv *env, jobject obj, jstring text) {
    __android_log_write(ANDROID_LOG_INFO, "android-io", "SpiceClientCutText");
    SpiceDisplay* display = global_display;
    SpiceDisplayPrivate *d = SPICE_DISPLAY_GET_PRIVATE(display);
    g_return_if_fail(SPICE_IS_MAIN_CHANNEL(d->main));

    size_t size = (*env)->GetStringUTFLength(env, text);
    const guchar *convertedText = (const guchar*)(*env)->GetStringUTFChars(env, text, NULL);
    spice_clipboard_selection_grab(d->main, convertedText, size);
    g_free((gpointer)convertedText);
}

/* Callbacks to the UI layer to draw screen updates and invalidate part of the screen,
 * or to request a new bitmap. */

void uiCallbackInvalidate (SpiceDisplayPrivate *d, gint x, gint y, gint w, gint h) {
    JNIEnv* env;
    gboolean attached = attachThreadToJvm (&env);

    // Tell the UI that it needs to send in the bitmap to be updated and to redraw.
    (*env)->CallStaticVoidMethod(env, jni_connector_class, jni_graphics_update, 0, x, y, w, h);

    if (attached) {
        detachThreadFromJvm ();
    }
}

void uiCallbackSettingsChanged (gint instance, gint width, gint height, gint bpp) {
    JNIEnv* env;
    gboolean attached = attachThreadToJvm (&env);

    // Ask for a new bitmap from the UI.
    (*env)->CallStaticVoidMethod(env, jni_connector_class, jni_settings_changed, instance, width, height, bpp);

    if (attached) {
        detachThreadFromJvm ();
    }
}

void uiCallbackMouseMode(JNIEnv *env, gboolean relative) {
    (*env)->CallStaticVoidMethod(env, jni_connector_class, jni_mouse_mode, relative);
}

void uiCallbackShowMessage(const gchar *message_text) {
    __android_log_write(ANDROID_LOG_DEBUG, "android-io", "uiCallbackShowMessage");
    JNIEnv *env = NULL;
    gboolean attached = attachThreadToJvm (&env);
    jstring jmessage = (*env)->NewStringUTF(env, message_text);
    (*env)->CallStaticVoidMethod(env, jni_connector_class, jni_show_message, jmessage);

    if (attached) {
        detachThreadFromJvm ();
    }
}
