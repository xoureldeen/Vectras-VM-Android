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
#ifndef __VIRT_VIEWER_FILE_H__
#define __VIRT_VIEWER_FILE_H__

G_BEGIN_DECLS

#define VIRT_VIEWER_TYPE_FILE            (virt_viewer_file_get_type ())
#define VIRT_VIEWER_FILE(obj)            (G_TYPE_CHECK_INSTANCE_CAST ((obj), VIRT_VIEWER_TYPE_FILE, VirtViewerFile))
#define VIRT_VIEWER_FILE_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST ((klass), VIRT_VIEWER_TYPE_FILE, VirtViewerFileClass))
#define VIRT_VIEWER_IS_FILE(obj)         (G_TYPE_CHECK_INSTANCE_TYPE ((obj), VIRT_VIEWER_TYPE_FILE))
#define VIRT_VIEWER_IS_FILE_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE ((klass), VIRT_VIEWER_TYPE_FILE))
#define VIRT_VIEWER_FILE_GET_CLASS(obj)  (G_TYPE_INSTANCE_GET_CLASS ((obj), VIRT_VIEWER_TYPE_FILE, VirtViewerFileClass))

typedef struct _VirtViewerFile VirtViewerFile;
typedef struct _VirtViewerFileClass VirtViewerFileClass;
typedef struct _VirtViewerFilePrivate VirtViewerFilePrivate;

struct _VirtViewerFile
{
    GObject parent;
    VirtViewerFilePrivate *priv;
};

struct _VirtViewerFileClass
{
    GObjectClass parent_class;
};

GType virt_viewer_file_get_type(void);

VirtViewerFile* virt_viewer_file_new(const gchar* path, GError** error);
gboolean virt_viewer_file_is_set(VirtViewerFile* self, const gchar* key);

gchar* virt_viewer_file_get_ca(VirtViewerFile* self);
void virt_viewer_file_set_ca(VirtViewerFile* self, const gchar* value);
gchar* virt_viewer_file_get_host(VirtViewerFile* self);
void virt_viewer_file_set_host(VirtViewerFile* self, const gchar* value);
gchar* virt_viewer_file_get_file_type(VirtViewerFile* self);
void virt_viewer_file_set_type(VirtViewerFile* self, const gchar* value);
gint virt_viewer_file_get_port(VirtViewerFile* self);
void virt_viewer_file_set_port(VirtViewerFile* self, gint value);
gint virt_viewer_file_get_tls_port(VirtViewerFile* self);
void virt_viewer_file_set_tls_port(VirtViewerFile* self, gint value);
gchar* virt_viewer_file_get_username(VirtViewerFile* self);
void virt_viewer_file_set_username(VirtViewerFile* self, const gchar* value);
gchar* virt_viewer_file_get_password(VirtViewerFile* self);
void virt_viewer_file_set_password(VirtViewerFile* self, const gchar* value);
gchar** virt_viewer_file_get_disable_channels(VirtViewerFile* self, gsize* length);
void virt_viewer_file_set_disable_channels(VirtViewerFile* self, const gchar* const* value, gsize length);
gchar** virt_viewer_file_get_disable_effects(VirtViewerFile* self, gsize* length);
void virt_viewer_file_set_disable_effects(VirtViewerFile* self, const gchar* const* value, gsize length);
gchar* virt_viewer_file_get_tls_ciphers(VirtViewerFile* self);
void virt_viewer_file_set_tls_ciphers(VirtViewerFile* self, const gchar* value);
gchar* virt_viewer_file_get_host_subject(VirtViewerFile* self);
void virt_viewer_file_set_host_subject(VirtViewerFile* self, const gchar* value);
gint virt_viewer_file_get_fullscreen(VirtViewerFile* self);
void virt_viewer_file_set_fullscreen(VirtViewerFile* self, gint value);
gchar* virt_viewer_file_get_title(VirtViewerFile* self);
void virt_viewer_file_set_title(VirtViewerFile* self, const gchar* value);
gchar* virt_viewer_file_get_toggle_fullscreen(VirtViewerFile* self);
void virt_viewer_file_set_toggle_fullscreen(VirtViewerFile* self, const gchar* value);
gchar* virt_viewer_file_get_release_cursor(VirtViewerFile* self);
void virt_viewer_file_set_release_cursor(VirtViewerFile* self, const gchar* value);
gint virt_viewer_file_get_enable_smartcard(VirtViewerFile* self);
void virt_viewer_file_set_enable_smartcard(VirtViewerFile* self, gint value);
gint virt_viewer_file_get_enable_usbredir(VirtViewerFile* self);
void virt_viewer_file_set_enable_usbredir(VirtViewerFile* self, gint value);
gint virt_viewer_file_get_color_depth(VirtViewerFile* self);
void virt_viewer_file_set_color_depth(VirtViewerFile* self, gint value);
gint virt_viewer_file_get_enable_usb_autoshare(VirtViewerFile* self);
void virt_viewer_file_set_enable_usb_autoshare(VirtViewerFile* self, gint value);
gchar* virt_viewer_file_get_usb_filter(VirtViewerFile* self);
void virt_viewer_file_set_usb_filter(VirtViewerFile* self, const gchar* value);
gchar* virt_viewer_file_get_smartcard_insert(VirtViewerFile* self);
void virt_viewer_file_set_smartcard_insert(VirtViewerFile* self, const gchar* value);
gchar* virt_viewer_file_get_smartcard_remove(VirtViewerFile* self);
void virt_viewer_file_set_smartcard_remove(VirtViewerFile* self, const gchar* value);
gchar* virt_viewer_file_get_proxy(VirtViewerFile* self);
void virt_viewer_file_set_proxy(VirtViewerFile* self, const gchar* value);
gchar* virt_viewer_file_get_version(VirtViewerFile* self);
void virt_viewer_file_set_version(VirtViewerFile* self, const gchar* value);
gchar** virt_viewer_file_get_secure_channels(VirtViewerFile* self, gsize* length);
void virt_viewer_file_set_secure_channels(VirtViewerFile* self, const gchar* const* value, gsize length);
gint virt_viewer_file_get_delete_this_file(VirtViewerFile* self);
void virt_viewer_file_set_delete_this_file(VirtViewerFile* self, gint value);
gchar* virt_viewer_file_get_secure_attention(VirtViewerFile* self);
void virt_viewer_file_set_secure_attention(VirtViewerFile* self, const gchar* value);

G_END_DECLS

#endif /* __VIRT_VIEWER_FILE_H__ */
