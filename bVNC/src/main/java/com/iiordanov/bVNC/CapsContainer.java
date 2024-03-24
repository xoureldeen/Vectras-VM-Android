/**
 * Copyright (C) 2003 Constantin Kaplinsky.  All Rights Reserved.
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

//
// CapsContainer.java - A container of capabilities as used in the RFB
// protocol 3.130
//
package com.iiordanov.bVNC;

import java.util.Hashtable;
import java.util.Vector;

class CapsContainer {

    // Public methods

    protected Hashtable<Integer, CapabilityInfo> infoMap;
    protected Vector<Integer> orderedList;

    public CapsContainer() {
        infoMap = new Hashtable<Integer, CapabilityInfo>(64, (float) 0.25);
        orderedList = new Vector<Integer>(32, 8);
    }

    public void add(CapabilityInfo capinfo) {
        Integer key = new Integer(capinfo.getCode());
        infoMap.put(key, capinfo);
    }

    public void add(int code, String vendor, String name, String desc) {
        Integer key = new Integer(code);
        infoMap.put(key, new CapabilityInfo(code, vendor, name, desc));
    }

    public boolean isKnown(int code) {
        return infoMap.containsKey(new Integer(code));
    }

    public CapabilityInfo getInfo(int code) {
        return (CapabilityInfo) infoMap.get(new Integer(code));
    }

    public String getDescription(int code) {
        CapabilityInfo capinfo = (CapabilityInfo) infoMap.get(new Integer(code));
        if (capinfo == null)
            return null;

        return capinfo.getDescription();
    }

    public boolean enable(CapabilityInfo other) {
        Integer key = new Integer(other.getCode());
        CapabilityInfo capinfo = (CapabilityInfo) infoMap.get(key);
        if (capinfo == null)
            return false;

        boolean enabled = capinfo.enableIfEquals(other);
        if (enabled)
            orderedList.addElement(key);

        return enabled;
    }

    public boolean isEnabled(int code) {
        CapabilityInfo capinfo = (CapabilityInfo) infoMap.get(new Integer(code));
        if (capinfo == null)
            return false;

        return capinfo.isEnabled();
    }

    // Protected data

    public int numEnabled() {
        return orderedList.size();
    }

    public int getByOrder(int idx) {
        int code;
        try {
            code = ((Integer) orderedList.elementAt(idx)).intValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            code = 0;
        }
        return code;
    }
}

