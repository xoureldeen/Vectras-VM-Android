/*
 * Virt Viewer: A virtual machine console viewer
 *
 * Copyright (C) 2007-2012 Red Hat, Inc.
 * Copyright (C) 2009-2012 Daniel P. Berrange
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Author: Daniel P. Berrange <berrange@redhat.com>
 */

#include <config.h>

#include <glib.h>
#include <glib/gi18n.h>
#include <locale.h>

#ifdef G_OS_WIN32
#include <windows.h>
#include <io.h>
#endif

#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <string.h>
#include <libxml/xpath.h>
#include <libxml/uri.h>

#include "virt-viewer-util.h"

GQuark
virt_viewer_error_quark(void)
{
  return g_quark_from_static_string ("virt-viewer-error-quark");
}

int
virt_viewer_util_extract_host(const char *uristr,
                              char **scheme,
                              char **host,
                              char **transport,
                              char **user,
                              int *port)
{
    xmlURIPtr uri;
    char *offset = NULL;

    if (uristr == NULL ||
        !g_ascii_strcasecmp(uristr, "xen"))
        uristr = "xen:///";

    uri = xmlParseURI(uristr);
    g_return_val_if_fail(uri != NULL, 1);

    if (host) {
        if (!uri || !uri->server) {
            *host = g_strdup("localhost");
        } else {
            if (uri->server[0] == '[') {
                gchar *tmp;
                *host = g_strdup(uri->server + 1);
                if ((tmp = strchr(*host, ']')))
                    *tmp = '\0';
            } else {
                *host = g_strdup(uri->server);
            }
        }
    }

    if (user) {
        if (uri->user)
            *user = g_strdup(uri->user);
        else
            *user = NULL;
    }

    if (port)
        *port = uri->port;

    if (uri->scheme)
        offset = strchr(uri->scheme, '+');

    if (transport) {
        if (offset)
            *transport = g_strdup(offset + 1);
        else
            *transport = NULL;
    }

    if (scheme && uri->scheme) {
        if (offset)
            *scheme = g_strndup(uri->scheme, offset - uri->scheme);
        else
            *scheme = g_strdup(uri->scheme);
    }

    xmlFreeURI(uri);
    return 0;
}

typedef struct {
    GObject *instance;
    GObject *observer;
    GClosure *closure;
    gulong handler_id;
} WeakHandlerCtx;

static WeakHandlerCtx *
whc_new(GObject *instance,
        GObject *observer)
{
    WeakHandlerCtx *ctx = g_slice_new0(WeakHandlerCtx);

    ctx->instance = instance;
    ctx->observer = observer;

    return ctx;
}

static void
whc_free(WeakHandlerCtx *ctx)
{
    g_slice_free(WeakHandlerCtx, ctx);
}

static void observer_destroyed_cb(gpointer, GObject *);
static void closure_invalidated_cb(gpointer, GClosure *);

/*
 * If signal handlers are removed before the object is destroyed, this
 * callback will never get triggered.
 */
static void
instance_destroyed_cb(gpointer ctx_,
                      GObject *where_the_instance_was G_GNUC_UNUSED)
{
    WeakHandlerCtx *ctx = ctx_;

    /* No need to disconnect the signal here, the instance has gone away. */
    g_object_weak_unref(ctx->observer, observer_destroyed_cb, ctx);
    g_closure_remove_invalidate_notifier(ctx->closure, ctx,
                                         closure_invalidated_cb);
    whc_free(ctx);
}

/* Triggered when the observer is destroyed. */
static void
observer_destroyed_cb(gpointer ctx_,
                      GObject *where_the_observer_was G_GNUC_UNUSED)
{
    WeakHandlerCtx *ctx = ctx_;

    g_closure_remove_invalidate_notifier(ctx->closure, ctx,
                                         closure_invalidated_cb);
    g_signal_handler_disconnect(ctx->instance, ctx->handler_id);
    g_object_weak_unref(ctx->instance, instance_destroyed_cb, ctx);
    whc_free(ctx);
}

/* Triggered when either object is destroyed or the handler is disconnected. */
static void
closure_invalidated_cb(gpointer ctx_,
                       GClosure *where_the_closure_was G_GNUC_UNUSED)
{
    WeakHandlerCtx *ctx = ctx_;

    g_object_weak_unref(ctx->instance, instance_destroyed_cb, ctx);
    g_object_weak_unref(ctx->observer, observer_destroyed_cb, ctx);
    whc_free(ctx);
}

/* Copied from tp_g_signal_connect_object. */
/**
  * virt_viewer_signal_connect_object: (skip)
  * @instance: the instance to connect to.
  * @detailed_signal: a string of the form "signal-name::detail".
  * @c_handler: the #GCallback to connect.
  * @gobject: the object to pass as data to @c_handler.
  * @connect_flags: a combination of #GConnectFlags.
  *
  * Similar to g_signal_connect_object() but will delete connection
  * when any of the objects is destroyed.
  *
  * Returns: the handler id.
  */
gulong virt_viewer_signal_connect_object(gpointer instance,
                                         const gchar *detailed_signal,
                                         GCallback c_handler,
                                         gpointer gobject,
                                         GConnectFlags connect_flags)
{
    GObject *instance_obj = G_OBJECT(instance);
    WeakHandlerCtx *ctx = whc_new(instance_obj, gobject);

    g_return_val_if_fail(G_TYPE_CHECK_INSTANCE (instance), 0);
    g_return_val_if_fail(detailed_signal != NULL, 0);
    g_return_val_if_fail(c_handler != NULL, 0);
    g_return_val_if_fail(G_IS_OBJECT (gobject), 0);
    g_return_val_if_fail((connect_flags & ~(G_CONNECT_AFTER|G_CONNECT_SWAPPED)) == 0, 0);

    if (connect_flags & G_CONNECT_SWAPPED)
        ctx->closure = g_cclosure_new_object_swap(c_handler, gobject);
    else
        ctx->closure = g_cclosure_new_object(c_handler, gobject);

    ctx->handler_id = g_signal_connect_closure(instance, detailed_signal,
                                               ctx->closure, (connect_flags & G_CONNECT_AFTER) ? TRUE : FALSE);

    g_object_weak_ref(instance_obj, instance_destroyed_cb, ctx);
    g_object_weak_ref(gobject, observer_destroyed_cb, ctx);
    g_closure_add_invalidate_notifier(ctx->closure, ctx,
                                      closure_invalidated_cb);

    return ctx->handler_id;
}

void virt_viewer_util_init(const char *appname)
{
#ifdef G_OS_WIN32
    /*
     * This named mutex will be kept around by Windows until the
     * process terminates. This allows other instances to check if it
     * already exists, indicating already running instances. It is
     * used to warn the user that installer can't proceed in this
     * case.
     */
    CreateMutexA(0, 0, "VirtViewerMutex");

    if (AttachConsole(ATTACH_PARENT_PROCESS) != 0) {
        freopen("CONIN$", "r", stdin);
        freopen("CONOUT$", "w", stdout);
        freopen("CONOUT$", "w", stderr);
        dup2(fileno(stdin), STDIN_FILENO);
        dup2(fileno(stdout), STDOUT_FILENO);
        dup2(fileno(stderr), STDERR_FILENO);
    }
#endif

#if !GLIB_CHECK_VERSION(2,31,0)
    g_thread_init(NULL);
#endif

    setlocale(LC_ALL, "");
    bindtextdomain(GETTEXT_PACKAGE, SPICE_GTK_LOCALEDIR);
    bind_textdomain_codeset(GETTEXT_PACKAGE, "UTF-8");
    textdomain(GETTEXT_PACKAGE);

    g_set_application_name(appname);
}

static gchar *
ctrl_key_to_gtk_key(const gchar *key)
{
    int i;

    static const struct {
        const char *ctrl;
        const char *gtk;
    } keys[] = {
        /* FIXME: right alt, right ctrl, right shift, cmds */
        { "alt", "<Alt>" },
        { "ralt", "<Alt>" },
        { "rightalt", "<Alt>" },
        { "right-alt", "<Alt>" },
        { "lalt", "<Alt>" },
        { "leftalt", "<Alt>" },
        { "left-alt", "<Alt>" },

        { "ctrl", "<Ctrl>" },
        { "rctrl", "<Ctrl>" },
        { "rightctrl", "<Ctrl>" },
        { "right-ctrl", "<Ctrl>" },
        { "lctrl", "<Ctrl>" },
        { "leftctrl", "<Ctrl>" },
        { "left-ctrl", "<Ctrl>" },

        { "shift", "<Shift>" },
        { "rshift", "<Shift>" },
        { "rightshift", "<Shift>" },
        { "right-shift", "<Shift>" },
        { "lshift", "<Shift>" },
        { "leftshift", "<Shift>" },
        { "left-shift", "<Shift>" },

        { "cmd", "<Ctrl>" },
        { "rcmd", "<Ctrl>" },
        { "rightcmd", "<Ctrl>" },
        { "right-cmd", "<Ctrl>" },
        { "lcmd", "<Ctrl>" },
        { "leftcmd", "<Ctrl>" },
        { "left-cmd", "<Ctrl>" },

        { "win", "<Super>" },
        { "rwin", "<Super>" },
        { "rightwin", "<Super>" },
        { "right-win", "<Super>" },
        { "lwin", "<Super>" },
        { "leftwin", "<Super>" },
        { "left-win", "<Super>" },

        { "esc", "Escape" },
        /* { "escape", "Escape" }, */

        { "ins", "Insert" },
        /* { "insert", "Insert" }, */

        { "del", "Delete" },
        /* { "delete", "Delete" }, */

        { "pgup", "Page_Up" },
        { "pageup", "Page_Up" },
        { "pgdn", "Page_Down" },
        { "pagedown", "Page_Down" },

        /* { "home", "home" }, */
        { "end", "End" },
        /* { "space", "space" }, */

        { "enter", "Return" },

        /* { "tab", "tab" }, */
        /* { "f1", "F1" }, */
        /* { "f2", "F2" }, */
        /* { "f3", "F3" }, */
        /* { "f4", "F4" }, */
        /* { "f5", "F5" }, */
        /* { "f6", "F6" }, */
        /* { "f7", "F7" }, */
        /* { "f8", "F8" }, */
        /* { "f9", "F9" }, */
        /* { "f10", "F10" }, */
        /* { "f11", "F11" }, */
        /* { "f12", "F12" } */
    };

    for (i = 0; i < G_N_ELEMENTS(keys); ++i) {
        if (g_ascii_strcasecmp(keys[i].ctrl, key) == 0)
            return g_strdup(keys[i].gtk);
    }

    return g_ascii_strup(key, -1);
}

gchar*
spice_hotkey_to_gtk_accelerator(const gchar *key)
{
    gchar *accel, **k, **keyv;

    keyv = g_strsplit(key, "+", -1);
    g_return_val_if_fail(keyv != NULL, NULL);

    for (k = keyv; *k != NULL; k++) {
        gchar *tmp = *k;
        *k = ctrl_key_to_gtk_key(tmp);
        g_free(tmp);
    }

    accel = g_strjoinv(NULL, keyv);
    g_strfreev(keyv);

    return accel;
}

/**
 * virt_viewer_compare_version:
 * @s1: a version-like string
 * @s2: a version-like string
 *
 * Compare two version-like strings: 1.1 > 1.0, 1.0.1 > 1.0, 1.10 > 1.7...
 *
 * String suffix (1.0rc1 etc) are not accepted, and will return 0.
 *
 * Returns: negative value if s1 < s2; zero if s1 = s2; positive value if s1 > s2.
 **/
gint
virt_viewer_compare_version(const gchar *s1, const gchar *s2)
{
    gint i, retval = 0;
    gchar **v1, **v2;

    g_return_val_if_fail(s1 != NULL, 0);
    g_return_val_if_fail(s2 != NULL, 0);

    v1 = g_strsplit(s1, ".", -1);
    v2 = g_strsplit(s2, ".", -1);

    for (i = 0; v1[i] && v2[i]; ++i) {
        gchar *e1 = NULL, *e2 = NULL;
        guint64 m1 = g_ascii_strtoull(v1[i], &e1, 10);
        guint64 m2 = g_ascii_strtoull(v2[i], &e2, 10);

        retval = m1 - m2;
        if (retval != 0)
            goto end;

        g_return_val_if_fail(e1 && e2, 0);
        if (*e1 || *e2) {
            g_warning("the version string contains suffix");
            goto end;
        }
    }

    if (v1[i])
        retval = 1;
    else if (v2[i])
        retval = -1;

end:
    g_strfreev(v1);
    g_strfreev(v2);
    return retval;
}
/*
 * Local variables:
 *  c-indent-level: 4
 *  c-basic-offset: 4
 *  indent-tabs-mode: nil
 * End:
 */
