# Copyright (C) 2013- Iordan Iordanov
#
# This is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 3 of the License, or
# (at your option) any later version.
#
# This software is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this software; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
# USA.
#

import sys
import os
import logging
import collections
import re

root = logging.getLogger()
root.setLevel(logging.DEBUG)

handler = logging.StreamHandler(sys.stdout)
handler.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
handler.setFormatter(formatter)
root.addHandler(handler)

layoutLocation = "/usr/share/qemu/keymaps/"
targetLocation = "../src/main/assets/layouts/"
layouts = {"en-us" : "English (US)", "en-gb" : "English (UK)", "de" : "German (Germany)", "fr" : "French (France)",
           "es" : "Spanish (Spain, Traditional Sort)", "sv" : "Swedish (Sweden)", "pl" : "Polish (Programmers)",
           "it" : "Italian (Italy)", "hu" : "Hungarian (Hungary)", "da" : "Danish", "pt" : "Portuguese (Portugal)",
           "pt-br" : "Portuguese (Brazil)", "de-ch" : "German (Switzerland)", "fr-ch" : "French (Switzerland)",
           "sl": "Slovenian" }
commonKeyCodeFile = "common_keycodes.in"
nameToUnicodeFile = "name_to_unicode.in"
ignoreListFile = "ignore.in"

unicodeMask = 0x100000
shiftMask = 0x10000
altGrMask = 0x20000

def loadIgnoreList(ignoreListFile):
    f = open(ignoreListFile)
    ignoreList = []
    for l in f.readlines():
        ignoreList.append(l.strip())
    return ignoreList


def loadNameToUnicode (nameToUnicodeFile):
    f = open(nameToUnicodeFile)
    nameToUnicode = {}
    for l in f.readlines():
        l = l.split()
        if l != None:
            nameToUnicode[l[0]] = l[1]

    return nameToUnicode

def loadCommonKeyCodes(commonFile, keyMap):
    f = open(commonFile)
    for l in f.readlines():
        l = l.split()
        if l != None:
            key = int(l[1])
            name = l[0]
            rawScanCode = int(l[2])
            scanCodes = [int(l[2])]
            addKeyToKeyMap(commonFile, keyMap, key, name, rawScanCode, scanCodes)

    return keyMap

def addKeyToKeyMap(keyMapFile, keyMap, key, name, rawScanCode, scanCodes):
    # We prefer smaller "raw" scancodes when creating the map to avoid using extended keyboard keys
    # when there are more than one way of typing the same character. This makes the scancode more likely
    # to result in a valid keystroke for the selected server-side keyboard layout.
    #if key == 1048585 and keyMapFile == '/usr/share/qemu/keymaps/en-us':
    #    print("Debug a key with breakpoint.")

    if key not in keyMap or rawScanCode < keyMap[key]["rawScanCode"]:
        keyMap[key] = { "scanCodes": scanCodes, "name": name, "rawScanCode": rawScanCode }
    else:
        logging.info(f"While processing {keyMapFile}, discarded duplicate larger "
                     f"rawScanCode: {rawScanCode} value for keyMap entry for {key}. "
                     f"Value in keyMap[key]: {keyMap[key]}")
    return keyMap

def loadKeyMap(nameToUnicode, keyMapFile, keyMap, ignoreList):
    try:
        f = open(keyMapFile)
    except:
        logging.info(f"Unable to open {keyMapFile} file, skipping")
        return keyMap
    lines = f.readlines()
    deadKeys = {}
    for l in lines:
        l = l.split()
        if len(l) > 1 and l[0] != "#" and l[0] != "include":
            name = str(l[0])

            scanCodes = list(l[1:])

            # Convert first scancode to an decimal integer (this is the actual scancode)
            rawScanCode = int(scanCodes[0], 16)
            scanCodes[0] = rawScanCode

            # Go through the scan codes and apply shift, altGr mask and skip if numlock is found
            # We iterate on a copy of the list of scanCodes in order to be able to safely modify
            # the original list.
            skip = False
            addUpper = False
            for s in list(scanCodes):
                if s == "shift":
                    scanCodes.remove(s)
                    scanCodes[0] |= shiftMask
                elif s == "altgr":
                    scanCodes.remove(s)
                    scanCodes[0] |= altGrMask
                elif s == "localstate":
                    scanCodes.remove(s)
                elif s == "numlock":
                    skip = True
                elif s == "addupper":
                    scanCodes.remove(s)
                    addUpper = True
                elif s == "inhibit":
                    scanCodes.remove(s)

            # If we're supposed to skip this entry, continue to next iteration.
            if skip:
                continue

            if addUpper:
                upperCaseScanCodes = list(scanCodes)
                upperCaseScanCodes[0] |= shiftMask
                unicode = int(nameToUnicode[name], 16) - 0x20
                key = unicode | unicodeMask
                addKeyToKeyMap(keyMapFile, keyMap, key, name, rawScanCode, upperCaseScanCodes)
            try:
                unicode = int(nameToUnicode[name], 16)
                key = unicode | unicodeMask

                addKeyToKeyMap(keyMapFile, keyMap, key, name, rawScanCode, scanCodes)
            except KeyError as e:
                if re.match('U[0-9A-Fa-f]{4}', name):
                    key = int(name[1:], 16) | unicodeMask
                    logging.info(f"Name {e} in qemu keymap looks like a raw unicode character, adding the numeric value after U in its 'name' {name} with unicodeMask to keymap")
                    addKeyToKeyMap(keyMapFile, keyMap, key, name, rawScanCode, scanCodes)
                else:
                    if name not in ignoreList:
                        logging.warning(f"Could not find unicode value for: {name}, ignoreList {ignoreList}")
                pass

            # Detect and store dead keys
            if name.startswith("dead_"):
                deadKeys[name[5:]] = scanCodes

    # Get a list of all name-unicode pairs that are made with a dead key
    # present in this layout.
    for name, unicode in nameToUnicode.items():
        add = False
        scodes = []
        for key, scanCode in deadKeys.items():
            if str(name).__contains__(str(key)):
                scodes += scanCode
                name = str(name).replace(str(key), "")
                if name.endswith("_"):
                    name = name[:name.__len__()-1]
                    add = True
                elif name.__len__() == 1:
                    add = True

        # If we've discovered a unicode key with an accent (dead-key) for which this layout has
        # all the necessary dead keys, make a new entry in keyMap with the list of scanCodes
        # containing each dead-key's scan code and ending on the scan code of the plain character.
        if add:
            unicode = int(unicode, 16) | unicodeMask
            plainLetterUnicode = int(nameToUnicode[name], 16) | unicodeMask
            try:
                scodes += keyMap[plainLetterUnicode]["scanCodes"]
                # Do not replace existing mappings with deadkey sequences as that would override
                # key mappings that are directly accessible from a button on the keyboard. Examples
                # are a, o, u umlaut in German
                if unicode not in keyMap:
                    logging.info(f"Generated scanCodes {scodes} for unicode key with accent (dead-key)")
                    addKeyToKeyMap(keyMapFile, keyMap, unicode, name, scodes[0], scodes)

            except KeyError as e:
                logging.warning(f"Could not find plainLetterUnicode {plainLetterUnicode} in keyMap {keyMapFile}, name: {name}, unicode: {unicode}")
                pass
    return keyMap

def generateLayoutMap():
    map = {}
    for layout in os.listdir(layoutLocation):
        f = open(os.path.join(layoutLocation, layout))
        name = "NONE"
        lines = f.readlines()
        for l in lines:
            if l.startswith("# name:"):
                try:
                    name = l.replace("# name: ", "").replace('"', '').strip()
                    map[layout] = name
                except:
                    logging.error(f"Error parsing the name of layout {l}. Manually add it to layouts map.")
                    pass
                break
    logging.info(f"Automatically generated map: {map}")
    return map

if __name__ == "__main__":
    nameToUnicode = loadNameToUnicode(nameToUnicodeFile)
    # Generate keymaps automatically from qemu keymaps directory
    auto_layouts = generateLayoutMap()
    # Update with manually crafted names.
    auto_layouts.update(layouts)

    for k, v in auto_layouts.items():
        keyMap = {}
        ignoreList = loadIgnoreList(ignoreListFile)
        keyMap = loadCommonKeyCodes (commonKeyCodeFile, keyMap)
        keyMap = loadKeyMap(nameToUnicode, layoutLocation + "common", keyMap, ignoreList)
        keyMap = loadKeyMap(nameToUnicode, layoutLocation + str(k), keyMap, ignoreList)

        f = open(targetLocation + str(v), "w")
        for u, s in sorted(keyMap.items()):
            name = str(keyMap[u]["name"])
            f.write(str(u) + " " + " ".join(map(str, s["scanCodes"])) + "\n")
            #f.write(name + " " + str(u) + " " + " ".join(map(str, s["scanCodes"])) + "\n")
        f.close()
