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

#ifndef VIRT_VIEWER_UTIL_H
#define VIRT_VIEWER_UTIL_H

#include <glib-object.h>

extern gboolean doDebug;

enum {
    VIRT_VIEWER_ERROR_FAILED,
};

#define DEBUG_LOG(s, ...) do { if (doDebug) g_debug(s, ## __VA_ARGS__); } while (0)
#define ARRAY_CARDINALITY(Array) (sizeof (Array) / sizeof *(Array))

#define VIRT_VIEWER_ERROR virt_viewer_error_quark ()

GQuark virt_viewer_error_quark(void);

void virt_viewer_util_init(const char *appname);

int virt_viewer_util_extract_host(const char *uristr,
                                  char **scheme,
                                  char **host,
                                  char **transport,
                                  char **user,
                                  int *port);

gulong virt_viewer_signal_connect_object(gpointer instance,
                                         const gchar *detailed_signal,
                                         GCallback c_handler,
                                         gpointer gobject,
                                         GConnectFlags connect_flags);

gchar* spice_hotkey_to_gtk_accelerator(const gchar *key);
gint virt_viewer_compare_version(const gchar *s1, const gchar *s2);

#endif

/*
 * Local variables:
 *  c-indent-level: 4
 *  c-basic-offset: 4
 *  indent-tabs-mode: nil
 * End:
 */
