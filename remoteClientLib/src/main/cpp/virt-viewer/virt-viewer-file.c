/* -*- Mode: C; c-basic-offset: 4; indent-tabs-mode: nil -*- */
/*
 * Virt Viewer: A virtual machine console viewer
 *
 * Copyright (C) 2012 Red Hat, Inc.
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */

#include <config.h>

#include <glib/gi18n.h>
#include <glib/gstdio.h>

#include "virt-viewer-util.h"
#include "virt-viewer-file.h"

/*
 * VirtViewerFile can read files in the .ini file format, with a
 * mandatory [virt-viewer] group and "type" key:
 *
 * # this is a comment
 * [virt-viewer]
 * type=spice
 * host=localhost
 * port=5900
 *
 *  The current list of [virt-viewer] keys is:
 * - version: string
 * - type: string, mandatory, values: "spice" (later "vnc" etc..)
 * - host: string
 * - port: int
 * - tls-port: int
 * - username: string
 * - password: string
 * - disable-channels: string list
 * - tls-ciphers: string
 * - ca: string PEM data (use \n to seperate the lines)
 * - host-subject: string
 * - fullscreen: int (0 or 1 atm)
 * - title: string
 * - toggle-fullscreen: string in spice hotkey format
 * - release-cursor: string in spice hotkey format
 * - smartcard-insert: string in spice hotkey format
 * - smartcard-remove: string in spice hotkey format
 * - secure-attention: string in spice hotkey format
 * - enable-smartcard: int (0 or 1 atm)
 * - enable-usbredir: int (0 or 1 atm)
 * - color-depth: int
 * - disable-effects: string list
 * - enable-usb-autoshare: int
 * - usb-filter: string
 * - secure-channels: string list
 * - delete-this-file: int (0 or 1 atm)
 * - proxy: proxy URL, like http://user:pass@foobar:8080
 *
 * (the file can be extended with extra groups or keys, which should
 * be prefixed with x- to avoid later conflicts)
 */

struct _VirtViewerFilePrivate {
    GKeyFile* keyfile;
};

G_DEFINE_TYPE(VirtViewerFile, virt_viewer_file, G_TYPE_OBJECT);

#define VIRT_VIEWER_FILE_GET_PRIVATE(o) (G_TYPE_INSTANCE_GET_PRIVATE((o), VIRT_VIEWER_TYPE_FILE, VirtViewerFilePrivate))

#define GROUP "virt-viewer"

enum  {
    PROP_DUMMY_PROPERTY,
    PROP_TYPE,
    PROP_HOST,
    PROP_PORT,
    PROP_TLS_PORT,
    PROP_USERNAME,
    PROP_PASSWORD,
    PROP_DISABLE_CHANNELS,
    PROP_TLS_CIPHERS,
    PROP_CA,
    PROP_HOST_SUBJECT,
    PROP_FULLSCREEN,
    PROP_TITLE,
    PROP_TOGGLE_FULLSCREEN,
    PROP_RELEASE_CURSOR,
    PROP_ENABLE_SMARTCARD,
    PROP_ENABLE_USBREDIR,
    PROP_COLOR_DEPTH,
    PROP_DISABLE_EFFECTS,
    PROP_ENABLE_USB_AUTOSHARE,
    PROP_USB_FILTER,
    PROP_PROXY,
    PROP_VERSION,
    PROP_SECURE_CHANNELS,
    PROP_DELETE_THIS_FILE,
    PROP_SECURE_ATTENTION,
};

VirtViewerFile*
virt_viewer_file_new(const gchar* location, GError** error)
{
    GError* inner_error = NULL;

    g_return_val_if_fail (location != NULL, NULL);

    VirtViewerFile* self = VIRT_VIEWER_FILE(g_object_new(VIRT_VIEWER_TYPE_FILE, NULL));
    GKeyFile* keyfile = self->priv->keyfile;

    g_key_file_load_from_file(keyfile, location,
                              G_KEY_FILE_KEEP_COMMENTS | G_KEY_FILE_KEEP_TRANSLATIONS,
                              &inner_error);
    if (inner_error != NULL) {
        g_propagate_error(error, inner_error);
        g_object_unref(self);
        return NULL;
    }

    if (!g_key_file_has_group (keyfile, GROUP) ||
        !virt_viewer_file_is_set(self, "type")) {
        inner_error = g_error_new_literal(G_KEY_FILE_ERROR,
                                          G_KEY_FILE_ERROR_NOT_FOUND, "Invalid file");
        g_propagate_error(error, inner_error);
        g_object_unref(self);
        return NULL;
    }

    if (virt_viewer_file_get_delete_this_file(self) &&
        !g_getenv("VIRT_VIEWER_KEEP_FILE")) {
        if (g_unlink(location) != 0)
            g_warning("failed to remove %s", location);
    }

    return self;
}

gboolean
virt_viewer_file_is_set(VirtViewerFile* self, const gchar* key)
{
    GError *inner_error = NULL;
    gboolean set;

    g_return_val_if_fail(VIRT_VIEWER_IS_FILE(self), FALSE);
    g_return_val_if_fail(key != NULL, FALSE);

    set = g_key_file_has_key(self->priv->keyfile, GROUP, key, &inner_error);
    if (inner_error == NULL)
        return set;
    else {
        g_clear_error(&inner_error);
        return FALSE;
    }
}

static void
virt_viewer_file_set_string(VirtViewerFile* self, const gchar* key, const gchar* value)
{
    g_return_if_fail(VIRT_VIEWER_IS_FILE(self));
    g_return_if_fail(key != NULL);
    g_return_if_fail(value != NULL);

    g_key_file_set_string(self->priv->keyfile, GROUP, key, value);
}

static gchar*
virt_viewer_file_get_string(VirtViewerFile* self, const gchar* key)
{
    GError* inner_error = NULL;
    gchar* result = NULL;

    g_return_val_if_fail(VIRT_VIEWER_IS_FILE(self), NULL);
    g_return_val_if_fail(key != NULL, NULL);

    result = g_key_file_get_string(self->priv->keyfile, GROUP, key, &inner_error);
    if (inner_error && inner_error->domain != G_KEY_FILE_ERROR)
        g_critical("%s", inner_error->message);
    g_clear_error(&inner_error);

    return result;
}

static void
virt_viewer_file_set_string_list(VirtViewerFile* self, const gchar* key, const gchar* const* value, gsize length)
{
    g_return_if_fail(VIRT_VIEWER_IS_FILE(self));
    g_return_if_fail(key != NULL);

    g_key_file_set_string_list(self->priv->keyfile, GROUP, key, value, length);
}

static gchar**
virt_viewer_file_get_string_list(VirtViewerFile* self, const gchar* key, gsize* length)
{
    GError* inner_error = NULL;
    gchar** result = NULL;

    g_return_val_if_fail(VIRT_VIEWER_IS_FILE(self), NULL);
    g_return_val_if_fail(key != NULL, NULL);

    result = g_key_file_get_string_list(self->priv->keyfile, GROUP, key, length, &inner_error);
    if (inner_error && inner_error->domain != G_KEY_FILE_ERROR)
        g_critical("%s", inner_error->message);
    g_clear_error(&inner_error);

    return result;
}

static void
virt_viewer_file_set_int(VirtViewerFile* self, const gchar* key, gint value)
{
    g_return_if_fail(VIRT_VIEWER_IS_FILE(self));
    g_return_if_fail(key != NULL);

    g_key_file_set_integer(self->priv->keyfile, GROUP, key, value);
}

static gint
virt_viewer_file_get_int(VirtViewerFile* self, const gchar* key)
{
    GError* inner_error = NULL;
    gint result;

    g_return_val_if_fail(VIRT_VIEWER_IS_FILE(self), -1);
    g_return_val_if_fail(key != NULL, -1);

    result = g_key_file_get_integer(self->priv->keyfile, GROUP, key, &inner_error);
    if (inner_error && inner_error->domain != G_KEY_FILE_ERROR)
        g_critical("%s", inner_error->message);
    g_clear_error(&inner_error);

    return result;
}

gchar*
virt_viewer_file_get_ca(VirtViewerFile* self)
{
    return virt_viewer_file_get_string(self, "ca");
}

void
virt_viewer_file_set_ca(VirtViewerFile* self, const gchar* value)
{
    virt_viewer_file_set_string(self, "ca", value);
    g_object_notify(G_OBJECT(self), "ca");
}

gchar*
virt_viewer_file_get_host(VirtViewerFile* self)
{
    return virt_viewer_file_get_string(self, "host");
}

void
virt_viewer_file_set_host(VirtViewerFile* self, const gchar* value)
{
    virt_viewer_file_set_string(self, "host", value);
    g_object_notify(G_OBJECT(self), "host");
}

gchar*
virt_viewer_file_get_file_type(VirtViewerFile* self)
{
    return virt_viewer_file_get_string(self, "type");
}

void
virt_viewer_file_set_type(VirtViewerFile* self, const gchar* value)
{
    virt_viewer_file_set_string(self, "type", value);
    g_object_notify(G_OBJECT(self), "type");
}

gint
virt_viewer_file_get_port(VirtViewerFile* self)
{
    return virt_viewer_file_get_int(self, "port");
}

void
virt_viewer_file_set_port(VirtViewerFile* self, gint value)
{
    virt_viewer_file_set_int(self, "port", value);
    g_object_notify(G_OBJECT(self), "port");
}

gint
virt_viewer_file_get_tls_port(VirtViewerFile* self)
{
    return virt_viewer_file_get_int(self, "tls-port");
}

void
virt_viewer_file_set_tls_port(VirtViewerFile* self, gint value)
{
    virt_viewer_file_set_int(self, "tls-port", value);
    g_object_notify(G_OBJECT(self), "tls-port");
}

gchar*
virt_viewer_file_get_username(VirtViewerFile* self)
{
    return virt_viewer_file_get_string(self, "username");
}

gchar*
virt_viewer_file_get_password(VirtViewerFile* self)
{
    return virt_viewer_file_get_string(self, "password");
}

void
virt_viewer_file_set_username(VirtViewerFile* self, const gchar* value)
{
    virt_viewer_file_set_string(self, "username", value);
    g_object_notify(G_OBJECT(self), "username");
}

void
virt_viewer_file_set_password(VirtViewerFile* self, const gchar* value)
{
    virt_viewer_file_set_string(self, "password", value);
    g_object_notify(G_OBJECT(self), "password");
}

gchar**
virt_viewer_file_get_disable_channels(VirtViewerFile* self, gsize* length)
{
    return virt_viewer_file_get_string_list(self, "disable-channels", length);
}

void
virt_viewer_file_set_disable_channels(VirtViewerFile* self, const gchar* const* value, gsize length)
{
    virt_viewer_file_set_string_list(self, "disable-channels", value, length);
    g_object_notify(G_OBJECT(self), "disable-channels");
}

gchar**
virt_viewer_file_get_disable_effects(VirtViewerFile* self, gsize* length)
{
    return virt_viewer_file_get_string_list(self, "disable-effects", length);
}

void
virt_viewer_file_set_disable_effects(VirtViewerFile* self, const gchar* const* value, gsize length)
{
    virt_viewer_file_set_string_list(self, "disable-effects", value, length);
    g_object_notify(G_OBJECT(self), "disable-effects");
}

gchar*
virt_viewer_file_get_tls_ciphers(VirtViewerFile* self)
{
    return virt_viewer_file_get_string(self, "tls-ciphers");
}

void
virt_viewer_file_set_tls_ciphers(VirtViewerFile* self, const gchar* value)
{
    virt_viewer_file_set_string(self, "tls-ciphers", value);
    g_object_notify(G_OBJECT(self), "tls-ciphers");
}

gchar*
virt_viewer_file_get_host_subject(VirtViewerFile* self)
{
    return virt_viewer_file_get_string(self, "host-subject");
}

void
virt_viewer_file_set_host_subject(VirtViewerFile* self, const gchar* value)
{
    virt_viewer_file_set_string(self, "host-subject", value);
    g_object_notify(G_OBJECT(self), "host-subject");
}

gint
virt_viewer_file_get_fullscreen(VirtViewerFile* self)
{
    return virt_viewer_file_get_int(self, "fullscreen");
}

void
virt_viewer_file_set_fullscreen(VirtViewerFile* self, gint value)
{
    virt_viewer_file_set_int(self, "fullscreen", !!value);
    g_object_notify(G_OBJECT(self), "fullscreen");
}

gchar*
virt_viewer_file_get_title(VirtViewerFile* self)
{
    return virt_viewer_file_get_string(self, "title");
}

void
virt_viewer_file_set_title(VirtViewerFile* self, const gchar* value)
{
    virt_viewer_file_set_string(self, "title", value);
    g_object_notify(G_OBJECT(self), "title");
}

gchar*
virt_viewer_file_get_toggle_fullscreen(VirtViewerFile* self)
{
    return virt_viewer_file_get_string(self, "toggle-fullscreen");
}

void
virt_viewer_file_set_toggle_fullscreen(VirtViewerFile* self, const gchar* value)
{
    virt_viewer_file_set_string(self, "toggle-fullscreen", value);
    g_object_notify(G_OBJECT(self), "toggle-fullscreen");
}

gchar*
virt_viewer_file_get_release_cursor(VirtViewerFile* self)
{
    return virt_viewer_file_get_string(self, "release-cursor");
}

void
virt_viewer_file_set_release_cursor(VirtViewerFile* self, const gchar* value)
{
    virt_viewer_file_set_string(self, "release-cursor", value);
    g_object_notify(G_OBJECT(self), "release-cursor");
}

gchar*
virt_viewer_file_get_secure_attention(VirtViewerFile* self)
{
    return virt_viewer_file_get_string(self, "secure-attention");
}

void
virt_viewer_file_set_secure_attention(VirtViewerFile* self, const gchar* value)
{
    virt_viewer_file_set_string(self, "secure-attention", value);
    g_object_notify(G_OBJECT(self), "secure-attention");
}

gchar*
virt_viewer_file_get_smartcard_remove(VirtViewerFile* self)
{
    return virt_viewer_file_get_string(self, "smartcard-remove");
}

void
virt_viewer_file_set_smartcard_remove(VirtViewerFile* self, const gchar* value)
{
    virt_viewer_file_set_string(self, "smartcard-remove", value);
    g_object_notify(G_OBJECT(self), "smartcard-remove");
}

gchar*
virt_viewer_file_get_smartcard_insert(VirtViewerFile* self)
{
    return virt_viewer_file_get_string(self, "smartcard-insert");
}

void
virt_viewer_file_set_smartcard_insert(VirtViewerFile* self, const gchar* value)
{
    virt_viewer_file_set_string(self, "smartcard-insert", value);
    g_object_notify(G_OBJECT(self), "smartcard-insert");
}

gint
virt_viewer_file_get_enable_smartcard(VirtViewerFile* self)
{
    return virt_viewer_file_get_int(self, "enable-smartcard");
}

void
virt_viewer_file_set_enable_smartcard(VirtViewerFile* self, gint value)
{
    virt_viewer_file_set_int(self, "enable-smartcard", !!value);
    g_object_notify(G_OBJECT(self), "enable-smartcard");
}

gint
virt_viewer_file_get_enable_usbredir(VirtViewerFile* self)
{
    return virt_viewer_file_get_int(self, "enable-usbredir");
}

void
virt_viewer_file_set_enable_usbredir(VirtViewerFile* self, gint value)
{
    virt_viewer_file_set_int(self, "enable-usbredir", !!value);
    g_object_notify(G_OBJECT(self), "enable-usbredir");
}

gint
virt_viewer_file_get_delete_this_file(VirtViewerFile* self)
{
    return virt_viewer_file_get_int(self, "delete-this-file");
}

void
virt_viewer_file_set_delete_this_file(VirtViewerFile* self, gint value)
{
    virt_viewer_file_set_int(self, "delete-this-file", !!value);
    g_object_notify(G_OBJECT(self), "delete-this-file");
}

gint
virt_viewer_file_get_color_depth(VirtViewerFile* self)
{
    return virt_viewer_file_get_int(self, "color-depth");
}

void
virt_viewer_file_set_color_depth(VirtViewerFile* self, gint value)
{
    virt_viewer_file_set_int(self, "color-depth", value);
    g_object_notify(G_OBJECT(self), "color-depth");
}

gint
virt_viewer_file_get_enable_usb_autoshare(VirtViewerFile* self)
{
    return virt_viewer_file_get_int(self, "enable-usb-autoshare");
}

void
virt_viewer_file_set_enable_usb_autoshare(VirtViewerFile* self, gint value)
{
    virt_viewer_file_set_int(self, "enable-usb-autoshare", !!value);
    g_object_notify(G_OBJECT(self), "enable-usb-autoshare");
}

gchar*
virt_viewer_file_get_usb_filter(VirtViewerFile* self)
{
    return virt_viewer_file_get_string(self, "usb-filter");
}

void
virt_viewer_file_set_usb_filter(VirtViewerFile* self, const gchar* value)
{
    virt_viewer_file_set_string(self, "usb-filter", value);
    g_object_notify(G_OBJECT(self), "usb-filter");
}

gchar*
virt_viewer_file_get_proxy(VirtViewerFile* self)
{
    return virt_viewer_file_get_string(self, "proxy");
}

void
virt_viewer_file_set_proxy(VirtViewerFile* self, const gchar* value)
{
    virt_viewer_file_set_string(self, "proxy", value);
    g_object_notify(G_OBJECT(self), "proxy");
}

gchar*
virt_viewer_file_get_version(VirtViewerFile* self)
{
    return virt_viewer_file_get_string(self, "version");
}

void
virt_viewer_file_set_version(VirtViewerFile* self, const gchar* value)
{
    virt_viewer_file_set_string(self, "version", value);
    g_object_notify(G_OBJECT(self), "version");
}

gchar**
virt_viewer_file_get_secure_channels(VirtViewerFile* self, gsize* length)
{
    return virt_viewer_file_get_string_list(self, "secure-channels", length);
}

void
virt_viewer_file_set_secure_channels(VirtViewerFile* self, const gchar* const* value, gsize length)
{
    virt_viewer_file_set_string_list(self, "secure-channels", value, length);
    g_object_notify(G_OBJECT(self), "secure-channels");
}

static void
virt_viewer_file_set_property(GObject* object, guint property_id,
                        const GValue* value, GParamSpec* pspec)
{
    VirtViewerFile *self = VIRT_VIEWER_FILE(object);
    gchar **strv;

    switch (property_id) {
    case PROP_TYPE:
        virt_viewer_file_set_type(self, g_value_get_string(value));
        break;
    case PROP_HOST:
        virt_viewer_file_set_host(self, g_value_get_string(value));
        break;
    case PROP_PORT:
        virt_viewer_file_set_port(self, g_value_get_int(value));
        break;
    case PROP_TLS_PORT:
        virt_viewer_file_set_tls_port(self, g_value_get_int(value));
        break;
    case PROP_USERNAME:
        virt_viewer_file_set_username(self, g_value_get_string(value));
        break;
    case PROP_PASSWORD:
        virt_viewer_file_set_password(self, g_value_get_string(value));
        break;
    case PROP_DISABLE_CHANNELS:
        strv = g_value_get_boxed(value);
        virt_viewer_file_set_disable_channels(self, (const gchar* const*)strv, g_strv_length(strv));
        break;
    case PROP_TLS_CIPHERS:
        virt_viewer_file_set_tls_ciphers(self, g_value_get_string(value));
        break;
    case PROP_CA:
        virt_viewer_file_set_ca(self, g_value_get_string(value));
        break;
    case PROP_HOST_SUBJECT:
        virt_viewer_file_set_host_subject(self, g_value_get_string(value));
        break;
    case PROP_FULLSCREEN:
        virt_viewer_file_set_fullscreen(self, g_value_get_int(value));
        break;
    case PROP_TITLE:
        virt_viewer_file_set_title(self, g_value_get_string(value));
        break;
    case PROP_TOGGLE_FULLSCREEN:
        virt_viewer_file_set_toggle_fullscreen(self, g_value_get_string(value));
        break;
    case PROP_RELEASE_CURSOR:
        virt_viewer_file_set_release_cursor(self, g_value_get_string(value));
        break;
    case PROP_SECURE_ATTENTION:
        virt_viewer_file_set_secure_attention(self, g_value_get_string(value));
        break;
    case PROP_ENABLE_SMARTCARD:
        virt_viewer_file_set_enable_smartcard(self, g_value_get_int(value));
        break;
    case PROP_ENABLE_USBREDIR:
        virt_viewer_file_set_enable_usbredir(self, g_value_get_int(value));
        break;
    case PROP_COLOR_DEPTH:
        virt_viewer_file_set_color_depth(self, g_value_get_int(value));
        break;
    case PROP_DISABLE_EFFECTS:
        strv = g_value_get_boxed(value);
        virt_viewer_file_set_disable_effects(self, (const gchar* const*)strv, g_strv_length(strv));
        break;
    case PROP_ENABLE_USB_AUTOSHARE:
        virt_viewer_file_set_enable_usb_autoshare(self, g_value_get_int(value));
        break;
    case PROP_USB_FILTER:
        virt_viewer_file_set_usb_filter(self, g_value_get_string(value));
        break;
    case PROP_PROXY:
        virt_viewer_file_set_proxy(self, g_value_get_string(value));
        break;
    case PROP_VERSION:
        virt_viewer_file_set_version(self, g_value_get_string(value));
        break;
    case PROP_SECURE_CHANNELS:
        strv = g_value_get_boxed(value);
        virt_viewer_file_set_secure_channels(self, (const gchar* const*)strv, g_strv_length(strv));
        break;
    case PROP_DELETE_THIS_FILE:
        virt_viewer_file_set_delete_this_file(self, g_value_get_int(value));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, property_id, pspec);
        break;
    }
}

static void
virt_viewer_file_get_property(GObject* object, guint property_id,
                        GValue* value, GParamSpec* pspec)
{
    VirtViewerFile *self = VIRT_VIEWER_FILE(object);

    switch (property_id) {
    case PROP_TYPE:
        g_value_take_string(value, virt_viewer_file_get_file_type(self));
        break;
    case PROP_HOST:
        g_value_take_string(value, virt_viewer_file_get_host(self));
        break;
    case PROP_PORT:
        g_value_set_int(value, virt_viewer_file_get_port(self));
        break;
    case PROP_TLS_PORT:
        g_value_set_int(value, virt_viewer_file_get_tls_port(self));
        break;
    case PROP_USERNAME:
        g_value_take_string(value, virt_viewer_file_get_username(self));
        break;
    case PROP_PASSWORD:
        g_value_take_string(value, virt_viewer_file_get_password(self));
        break;
    case PROP_DISABLE_CHANNELS:
        g_value_take_boxed(value, virt_viewer_file_get_disable_channels(self, NULL));
        break;
    case PROP_TLS_CIPHERS:
        g_value_take_string(value, virt_viewer_file_get_tls_ciphers(self));
        break;
    case PROP_CA:
        g_value_take_string(value, virt_viewer_file_get_ca(self));
        break;
    case PROP_HOST_SUBJECT:
        g_value_take_string(value, virt_viewer_file_get_host_subject(self));
        break;
    case PROP_FULLSCREEN:
        g_value_set_int(value, virt_viewer_file_get_fullscreen(self));
        break;
    case PROP_TITLE:
        g_value_take_string(value, virt_viewer_file_get_title(self));
        break;
    case PROP_TOGGLE_FULLSCREEN:
        g_value_take_string(value, virt_viewer_file_get_toggle_fullscreen(self));
        break;
    case PROP_RELEASE_CURSOR:
        g_value_take_string(value, virt_viewer_file_get_release_cursor(self));
        break;
    case PROP_SECURE_ATTENTION:
        g_value_take_string(value, virt_viewer_file_get_secure_attention(self));
        break;
    case PROP_ENABLE_SMARTCARD:
        g_value_set_int(value, virt_viewer_file_get_enable_smartcard(self));
        break;
    case PROP_ENABLE_USBREDIR:
        g_value_set_int(value, virt_viewer_file_get_enable_usbredir(self));
        break;
    case PROP_COLOR_DEPTH:
        g_value_set_int(value, virt_viewer_file_get_color_depth(self));
        break;
    case PROP_DISABLE_EFFECTS:
        g_value_take_boxed(value, virt_viewer_file_get_disable_effects(self, NULL));
        break;
    case PROP_ENABLE_USB_AUTOSHARE:
        g_value_set_int(value, virt_viewer_file_get_enable_usb_autoshare(self));
        break;
    case PROP_USB_FILTER:
        g_value_take_string(value, virt_viewer_file_get_usb_filter(self));
        break;
    case PROP_PROXY:
        g_value_take_string(value, virt_viewer_file_get_proxy(self));
        break;
    case PROP_VERSION:
        g_value_take_string(value, virt_viewer_file_get_version(self));
        break;
    case PROP_SECURE_CHANNELS:
        g_value_take_boxed(value, virt_viewer_file_get_secure_channels(self, NULL));
        break;
    case PROP_DELETE_THIS_FILE:
        g_value_set_int(value, virt_viewer_file_get_delete_this_file(self));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, property_id, pspec);
        break;
    }
}


static void
virt_viewer_file_finalize(GObject* object)
{
    VirtViewerFile *self = VIRT_VIEWER_FILE(object);

    g_clear_pointer(&self->priv->keyfile, g_key_file_free);

    G_OBJECT_CLASS(virt_viewer_file_parent_class)->finalize(object);
}

static void
virt_viewer_file_init(VirtViewerFile* self)
{
    self->priv = VIRT_VIEWER_FILE_GET_PRIVATE(self);

    self->priv->keyfile = g_key_file_new();
}

static void
virt_viewer_file_class_init(VirtViewerFileClass* klass)
{
    virt_viewer_file_parent_class = g_type_class_peek_parent(klass);
    g_type_class_add_private(klass, sizeof(VirtViewerFilePrivate));

    G_OBJECT_CLASS(klass)->get_property = virt_viewer_file_get_property;
    G_OBJECT_CLASS(klass)->set_property = virt_viewer_file_set_property;
    G_OBJECT_CLASS(klass)->finalize = virt_viewer_file_finalize;

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_TYPE,
        g_param_spec_string("type", "type", "type", NULL,
                            G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_HOST,
        g_param_spec_string("host", "host", "host", NULL,
                            G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_PORT,
        g_param_spec_int("port", "port", "port", -1, 65535, -1,
                         G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_TLS_PORT,
        g_param_spec_int("tls-port", "tls-port", "tls-port", -1, 65535, -1,
                         G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_USERNAME,
        g_param_spec_string("username", "username", "username", NULL,
                            G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_PASSWORD,
        g_param_spec_string("password", "password", "password", NULL,
                            G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_TLS_CIPHERS,
        g_param_spec_string("tls-ciphers", "tls-ciphers", "tls-ciphers", NULL,
                            G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_CA,
        g_param_spec_string("ca", "ca", "ca", NULL,
                            G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_HOST_SUBJECT,
        g_param_spec_string("host-subject", "host-subject", "host-subject", NULL,
                            G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_FULLSCREEN,
        g_param_spec_int("fullscreen", "fullscreen", "fullscreen", 0, 1, 0,
                         G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_TITLE,
        g_param_spec_string("title", "title", "title", NULL,
                            G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_TOGGLE_FULLSCREEN,
        g_param_spec_string("toggle-fullscreen", "toggle-fullscreen", "toggle-fullscreen", NULL,
                            G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_RELEASE_CURSOR,
        g_param_spec_string("release-cursor", "release-cursor", "release-cursor", NULL,
                            G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_SECURE_ATTENTION,
        g_param_spec_string("secure-attention", "secure-attention", "secure-attention", NULL,
                            G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_ENABLE_SMARTCARD,
        g_param_spec_int("enable-smartcard", "enable-smartcard", "enable-smartcard", 0, 1, 0,
                         G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_ENABLE_USBREDIR,
        g_param_spec_int("enable-usbredir", "enable-usbredir", "enable-usbredir", 0, 1, 0,
                         G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_COLOR_DEPTH,
        g_param_spec_int("color-depth", "color-depth", "color-depth", 0, 32, 0,
                         G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_ENABLE_USB_AUTOSHARE,
        g_param_spec_int("enable-usb-autoshare", "enable-usb-autoshare", "enable-usb-autoshare", 0, 1, 0,
                         G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_USB_FILTER,
        g_param_spec_string("usb-filter", "usb-filter", "usb-filter", NULL,
                            G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_DISABLE_CHANNELS,
        g_param_spec_boxed("disable-channels", "disable-channels", "disable-channels", G_TYPE_STRV,
                           G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_DISABLE_EFFECTS,
        g_param_spec_boxed("disable-effects", "disable-effects", "disable-effects", G_TYPE_STRV,
                           G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_PROXY,
        g_param_spec_string("proxy", "proxy", "proxy", NULL,
                            G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_VERSION,
        g_param_spec_string("version", "version", "version", NULL,
                            G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_SECURE_CHANNELS,
        g_param_spec_boxed("secure-channels", "secure-channels", "secure-channels", G_TYPE_STRV,
                           G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));

    g_object_class_install_property(G_OBJECT_CLASS(klass), PROP_DELETE_THIS_FILE,
        g_param_spec_int("delete-this-file", "delete-this-file", "delete-this-file", 0, 1, 0,
                         G_PARAM_STATIC_STRINGS | G_PARAM_READWRITE));
}
