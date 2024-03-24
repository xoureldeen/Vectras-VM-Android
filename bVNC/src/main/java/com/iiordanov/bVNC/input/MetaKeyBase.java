/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2009 Michael A. MacDonald
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

package com.iiordanov.bVNC.input;

/**
 * @author Michael A. MacDonald
 *
 */
public class MetaKeyBase implements Comparable<MetaKeyBase> {
    int keySym;
    int mouseButtons;
    int keyEvent;
    String name;
    boolean isMouse;
    boolean isKeyEvent;

    MetaKeyBase(int mouseButtons, String name) {
        this.mouseButtons = mouseButtons;
        this.name = name;
        this.isMouse = true;
        this.isKeyEvent = false;
    }

    MetaKeyBase(String name, int keySym, int keyEvent) {
        this.name = name;
        this.keySym = keySym;
        this.keyEvent = keyEvent;
        this.isMouse = false;
        this.isKeyEvent = true;
    }

    MetaKeyBase(String name, int keySym) {
        this.name = name;
        this.keySym = keySym;
        this.isMouse = false;
        this.isKeyEvent = false;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(MetaKeyBase another) {
        return name.compareTo(another.name);
    }
}
