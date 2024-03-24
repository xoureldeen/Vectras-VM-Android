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

#ifndef ANDROID_CLIPBOARD_H
#define ANDROID_CLIPBOARD_H
#include "android-spice-widget.h"
#include "android-spicy.h"
#include "spice-common.h"
#include "spice/vd_agent.h"

#define CLIPBOARD_MAX_LENGTH 262144
typedef struct spice_clipboard {
    char* buffer;
    int length;
    int available;
} spice_clipboard;

spice_clipboard* spice_clipboard_alloc();
void spice_clipboard_connect_signals(SpiceChannel* channel, spice_connection *conn);
void spice_clipboard_selection_handler(
    SpiceMainChannel* channel, guint selection, guint type, gpointer data, guint size, spice_connection *conn);
void spice_clipboard_selection_grab_handler(
    SpiceMainChannel* channel, guint selection, guint32* types, guint ntypes, spice_connection *conn);
void spice_clipboard_selection_release_handler(
    SpiceMainChannel* channel, guint selection, spice_connection *conn);
void spice_clipboard_selection_request_handler(
    SpiceMainChannel* channel, guint selection, guint type, spice_connection *conn);
void spice_clipboard_selection_grab(SpiceMainChannel* channel, const guchar* text, size_t size);

#endif //ANDROID_CLIPBOARD_H
