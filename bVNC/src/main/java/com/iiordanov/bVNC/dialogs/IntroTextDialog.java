/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2010 Michael A. MacDonald
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

package com.iiordanov.bVNC.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.iiordanov.bVNC.ConnectionBean;
import com.iiordanov.bVNC.Database;
import com.iiordanov.bVNC.MostRecentBean;
import com.iiordanov.bVNC.Utils;
import com.undatech.remoteClientUi.R;

import net.sqlcipher.database.SQLiteDatabase;

/**
 * @author Michael A. MacDonald
 */
public class IntroTextDialog extends Dialog {

    static IntroTextDialog dialog = null;
    private final PackageInfo packageInfo;
    private final Database database;
    private boolean donate = false;

    /**
     * @param context -- Containing dialog
     */
    private IntroTextDialog(Activity context, PackageInfo pi, Database database) {
        super(context);
        setOwnerActivity(context);
        packageInfo = pi;
        this.database = database;
    }

    public static void showIntroTextIfNecessary(Activity context, Database database, boolean show) {
        PackageInfo pi;
        try {
            String packageName = Utils.pName(context);
            pi = context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException nnfe) {
            return;
        }
        MostRecentBean mr = ConnectionBean.getMostRecent(database.getReadableDatabase());
        database.close();
        boolean newLaunchOrVersion = (mr == null || mr.getShowSplashVersion() != pi.versionCode);
        if (dialog == null && show && (mr == null || mr.getShowSplashVersion() != pi.versionCode)) {
            dialog = new IntroTextDialog(context, pi, database);
            dialog.show();
        }
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String pkgName = Utils.pName(this.getContext());
        if (pkgName.contains("free")) {
            donate = true;
        }

        setContentView(R.layout.intro_dialog);
        Window window = getWindow();
        if (window != null) {
            window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        }

        Context context = this.getContext();
        String title = getContext().getResources().getString(R.string.intro_title);
        if (Utils.isRdp(context)) {
            title = getContext().getResources().getString(R.string.rdp_intro_title);
        } else if (Utils.isSpice(context)) {
            title = getContext().getResources().getString(R.string.spice_intro_title);
        }
        StringBuilder sb = new StringBuilder(title);
        setTitle(sb);
        sb.delete(0, sb.length());
        if (pkgName.contains("SPICE")) {
            sb.append(getContext().getResources().getString(R.string.ad_donate_text_spice));
            sb.append("<br>");
            sb.append("<br>");
        } else if (pkgName.contains("RDP")) {
            sb.append(getContext().getResources().getString(R.string.ad_donate_text_rdp));
            sb.append("<br>");
            sb.append("<br>");
        }
        sb.append(getContext().getResources().getString(R.string.ad_donate_text0));
        sb.append("<br>");
        sb.append("<br>");

        if (donate) {
            sb.append("<a href=\"");
            sb.append(Utils.getDonationPackageLink(getContext()));
            sb.append("\">");
            sb.append(getContext().getResources().getString(R.string.ad_donate_text1));
            sb.append("</a>");
            sb.append("<br>");
            sb.append("<br>");
            sb.append(getContext().getResources().getString(R.string.ad_donate_text2));
            sb.append("<br>");
            sb.append("<br>");
            sb.append(getContext().getResources().getString(R.string.ad_donate_text3));
            sb.append(" <a href=\"market://details?id=com.iiordanov.bVNC\">VNC</a>");
            sb.append(", ");
            sb.append("<a href=\"market://details?id=com.iiordanov.aRDP\">RDP</a>");
            sb.append(", ");
            sb.append("<a href=\"market://details?id=com.iiordanov.aSPICE\">SPICE</a>");
            sb.append(", ");
            sb.append("<a href=\"market://details?id=com.undatech.opaque\">oVirt/RHEV/Proxmox</a>");
            sb.append("<br>");
            sb.append("<br>");
        }

        sb.append(getContext().getResources().getString(R.string.intro_header));
        if (Utils.isVnc(context)) {
            sb.append(getContext().getResources().getString(R.string.intro_text));
        } else if (Utils.isRdp(context)) {
            sb.append(getContext().getResources().getString(R.string.rdp_intro_text));
        } else if (Utils.isSpice(context)) {
            sb.append(getContext().getResources().getString(R.string.spice_intro_text));
        }
        sb.append("\n");
        sb.append(getContext().getResources().getString(R.string.intro_version_text));
        TextView introTextView = (TextView) findViewById(R.id.textIntroText);
        introTextView.setText(Html.fromHtml(sb.toString()));
        introTextView.setMovementMethod(LinkMovementMethod.getInstance());
        /* (non-Javadoc)
         * @see android.view.View.OnClickListener#onClick(android.view.View)
         */
        ((Button) findViewById(R.id.buttonCloseIntro)).setOnClickListener(v -> showAgain(true));

        Button buttonCloseIntroDontShow = (Button) findViewById(R.id.buttonCloseIntroDontShow);
        if (donate) {
            buttonCloseIntroDontShow.setVisibility(View.GONE);
        } else {
            /* (non-Javadoc)
             * @see android.view.View.OnClickListener#onClick(android.view.View)
             */
            buttonCloseIntroDontShow.setOnClickListener(v -> showAgain(false));
        }
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        Activity activity = getOwnerActivity();
        if (activity != null) {
            getOwnerActivity().getMenuInflater().inflate(R.menu.intro_dialog_menu, menu);
        }
        menu.findItem(R.id.itemClose).setOnMenuItemClickListener(item -> {
            showAgain(true);
            return true;
        });
        menu.findItem(R.id.itemDontShowAgain).setOnMenuItemClickListener(item -> {
            showAgain(false);
            return true;
        });
        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Dialog#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        showAgain(true);
    }

    private void showAgain(boolean show) {
        SQLiteDatabase db = database.getWritableDatabase();
        MostRecentBean mostRecent = ConnectionBean.getMostRecent(db);
        if (mostRecent != null) {
            int value = -1;
            if (!show) {
                value = packageInfo.versionCode;
            }
            mostRecent.setShowSplashVersion(value);
            mostRecent.Gen_update(db);
        }
        database.close();
        dismiss();
        dialog = null;
    }
}
