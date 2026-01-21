package com.vectras.vm.utils;

import android.view.KeyEvent;

import com.vectras.vm.R;

import java.util.ArrayList;
import java.util.HashMap;

public class ListUtils {
    public static void setupMirrorListForListmap(ArrayList<HashMap<String, Object>> listmapForSelectMirrors) {
        HashMap<String, Object> mapForAddItems = new HashMap<>();

        mapForAddItems.put("name", "United States (Default)");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true, "dl-cdn.alpinelinux.org", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Australia");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(false,"mirror.aarnet.edu.au", "/pub/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Austria");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"mirror.alwyzon.net", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Bulgaria");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"mirrors.neterra.net", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Brazil");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"mirror.uepg.br", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Cambodia");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"mirror.sabay.com.kh", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Canada");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true, "mirror.csclub.uwaterloo.ca", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Chile");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"elmirror.cl", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "China");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true, "mirrors.tuna.tsinghua.edu.cn", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Czech Republic");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true, "mirror.fel.cvut.cz", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Denmark");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true, "mirrors.dotsrc.org", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Finland");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true, "mirror.5i.fi", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "France");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"mirrors.ircam.fr", "/pub/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Germany");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"ftp.halifax.rwth-aachen.de", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Hong Kong");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true, "mirror.xtom.com.hk", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Indonesia");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(false,"foobar.turbo.net.id", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Iran");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"mirror.bardia.tech", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Italy");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"alpinelinux.mirror.garr.it", ""));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Japan");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true, "repo.jing.rocks", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Kazakhstan");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"mirror.ps.kz", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Moldova");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"mirror.ihost.md", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Morocco");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true, "mirror.marwan.ma", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "New Caledonia");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"mirror.lagoon.nc", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "New Zealand");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"mirror.2degrees.nz", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Poland");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"ftp.icm.edu.pl", "/pub/Linux/distributions/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Portugal");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"mirror.leitecastro.com", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Romania");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"mirrors.hosterion.ro", "/alpinelinux"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Russia");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"mirror.hyperdedic.ru", "/alpinelinux"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Singapore");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"mirror.jingk.ai", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Slovenia");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"mirror.tux.si", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Spain");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"mirror.raiolanetworks.com", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Sweden");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true, "ftp.lysator.liu.se/pub", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Switzerland");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true, "pkg.adfinis.com", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Taiwan");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true, "mirror.twds.com.tw", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "Thailand");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"mirror.kku.ac.th", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "The Netherlands");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"alpine.mirror.wearetriple.com", ""));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("name", "United Kingdom");
        mapForAddItems.put("value", CommandUtils.createForSelectedMirror(true,"uk.alpinelinux.org", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);
    }

    public static void setupSendKeyListForListmap(ArrayList<HashMap<String, Object>> listmapForSendKey) {
        HashMap<String, Object> mapForAddItems = new HashMap<>();

        mapForAddItems.put("keyname", "Ctrl + Alt + Del");
        mapForAddItems.put("keycode", 0);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", false);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Esc");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_ESCAPE);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Windows");
        mapForAddItems.put("keycode", "KEY_LEFTMETA");
        mapForAddItems.put("useQMP", true);
        mapForAddItems.put("useKeyEvent", false);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.grid_view_24px);
        listmapForSendKey.add(mapForAddItems);

//		mapForAddItems = new HashMap<>();
//		mapForAddItems.put("keyname", "Menu");
//		mapForAddItems.put("keycode", 93);
//		mapForAddItems.put("useQMP", false);
//      mapForAddItems.put("useKeyEvent", false);
//		mapForAddItems.put("useIcon", true);
//		mapForAddItems.put("rIcon", R.drawable.menu_24px);
//		listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Backspace");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_DEL);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.backspace_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Enter");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_ENTER);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.keyboard_return_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Tab");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_TAB);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.sync_alt_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Up");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_DPAD_UP);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.arrow_upward_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Down");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_DPAD_DOWN);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.arrow_downward_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Left");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_DPAD_LEFT);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.arrow_back_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Left");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_DPAD_RIGHT);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.arrow_forward_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Home");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_MOVE_HOME);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.vertical_align_top_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "End");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_MOVE_END);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.vertical_align_bottom_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "End");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_PAGE_UP);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.arrow_warm_up_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "End");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_PAGE_DOWN);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.arrow_cool_down_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "End");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_INSERT);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.insert_text_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F1");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F1);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F2");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F2);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F3");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F3);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F4");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F4);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F5");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F5);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F6");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F6);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F7");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F7);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F8");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F8);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F9");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F9);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F10");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F10);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F11");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F11);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F12");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F12);
        mapForAddItems.put("useQMP", false);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);
    }
}
