package com.vectras.vm.utils;

import android.view.KeyEvent;

import com.vectras.vm.R;

import java.util.ArrayList;
import java.util.HashMap;

public class ListUtils {
    public static void setupMirrorListForListmap(ArrayList<HashMap<String, String>> listmapForSelectMirrors) {
        HashMap<String, String> mapForAddItems = new HashMap<>();

        mapForAddItems.put("location", "United States (Default)");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true, "dl-cdn.alpinelinux.org", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Australia");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(false,"mirror.aarnet.edu.au", "/pub/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Austria");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"mirror.alwyzon.net", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Bulgaria");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"mirrors.neterra.net", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Brazil");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"mirror.uepg.br", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Cambodia");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"mirror.sabay.com.kh", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Canada");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true, "mirror.csclub.uwaterloo.ca", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Chile");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"elmirror.cl", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "China");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true, "mirrors.tuna.tsinghua.edu.cn", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Czech Republic");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true, "mirror.fel.cvut.cz", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Denmark");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true, "mirrors.dotsrc.org", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Finland");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true, "mirror.5i.fi", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "France");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"mirrors.ircam.fr", "/pub/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Germany");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"ftp.halifax.rwth-aachen.de", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Hong Kong");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true, "mirror.xtom.com.hk", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Indonesia");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(false,"foobar.turbo.net.id", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Iran");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"mirror.bardia.tech", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Italy");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"alpinelinux.mirror.garr.it", ""));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Japan");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true, "repo.jing.rocks", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Kazakhstan");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"mirror.ps.kz", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Moldova");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"mirror.ihost.md", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Morocco");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true, "mirror.marwan.ma", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "New Caledonia");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"mirror.lagoon.nc", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "New Zealand");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"mirror.2degrees.nz", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Poland");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"ftp.icm.edu.pl", "/pub/Linux/distributions/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Portugal");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"mirror.leitecastro.com", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Romania");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"mirrors.hosterion.ro", "/alpinelinux"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Russia");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"mirror.hyperdedic.ru", "/alpinelinux"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Singapore");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"mirror.jingk.ai", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Slovenia");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"mirror.tux.si", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Spain");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"mirror.raiolanetworks.com", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Sweden");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true, "ftp.lysator.liu.se/pub", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Switzerland");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true, "pkg.adfinis.com", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Taiwan");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true, "mirror.twds.com.tw", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "Thailand");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"mirror.kku.ac.th", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "The Netherlands");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"alpine.mirror.wearetriple.com", ""));
        listmapForSelectMirrors.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("location", "United Kingdom");
        mapForAddItems.put("mirror", CommandUtils.createForSelectedMirror(true,"uk.alpinelinux.org", "/alpine"));
        listmapForSelectMirrors.add(mapForAddItems);
    }

    public static void setupSendKeyListForListmap(ArrayList<HashMap<String, Object>> listmapForSendKey) {
        HashMap<String, Object> mapForAddItems = new HashMap<>();

        mapForAddItems.put("keyname", "Ctrl + Alt + Del");
        mapForAddItems.put("keycode", 0);
        mapForAddItems.put("useKeyEvent", false);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Esc");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_ESCAPE);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Windows");
        mapForAddItems.put("keycode", 91);
        mapForAddItems.put("useKeyEvent", false);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.grid_view_24px);
        listmapForSendKey.add(mapForAddItems);

//		mapForAddItems = new HashMap<>();
//		mapForAddItems.put("keyname", "Menu");
//		mapForAddItems.put("keycode", 93);
//		mapForAddItems.put("useKeyEvent", false);
//		mapForAddItems.put("useIcon", true);
//		mapForAddItems.put("rIcon", R.drawable.menu_24px);
//		listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Backspace");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_DEL);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.backspace_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Enter");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_ENTER);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.keyboard_return_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Tab");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_TAB);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.sync_alt_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Up");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_DPAD_UP);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.arrow_upward_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Down");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_DPAD_DOWN);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.arrow_downward_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Left");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_DPAD_LEFT);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.arrow_back_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Left");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_DPAD_RIGHT);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.arrow_forward_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "Home");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_MOVE_HOME);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.vertical_align_top_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "End");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_MOVE_END);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.vertical_align_bottom_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "End");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_PAGE_UP);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.arrow_warm_up_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "End");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_PAGE_DOWN);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.arrow_cool_down_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "End");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_INSERT);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", true);
        mapForAddItems.put("rIcon", R.drawable.insert_text_24px);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F1");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F1);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F2");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F2);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F3");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F3);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F4");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F4);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F5");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F5);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F6");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F6);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F7");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F7);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F8");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F8);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F9");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F9);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F10");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F10);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F11");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F11);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);

        mapForAddItems = new HashMap<>();
        mapForAddItems.put("keyname", "F12");
        mapForAddItems.put("keycode", KeyEvent.KEYCODE_F12);
        mapForAddItems.put("useKeyEvent", true);
        mapForAddItems.put("useIcon", false);
        mapForAddItems.put("rIcon", 0);
        listmapForSendKey.add(mapForAddItems);
    }
}
