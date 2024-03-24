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

typedef unsigned char uchar;

void updatePixels (uchar* dest, uchar* source, int x, int y, int width, int height, int buffwidth, int buffheight, int bpp);

void uiCallbackInvalidate (SpiceDisplayPrivate *d, gint x, gint y, gint w, gint h);
void uiCallbackSettingsChanged (gint instance, gint width, gint height, gint bpp);

gint get_display_id(SpiceDisplay *display);

void uiCallbackMouseMode(JNIEnv *env, gboolean relative);

void uiCallbackShowMessage(const gchar *message_text);