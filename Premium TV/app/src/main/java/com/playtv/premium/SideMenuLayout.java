package com.playtv.premium;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import androidx.core.view.ViewCompat;
import androidx.media3.ui.PlayerView;

/* JADX INFO: loaded from: classes2.dex */
final class SideMenuLayout extends LinearLayout {
    final View contentView;
    final View menuContainer;
    final ListView menuList;
    private boolean menuOpen;

    SideMenuLayout(Context context, View view) {
        super(context);
        this.menuOpen = false;
        this.contentView = view;
        setOrientation(0);
        setBackgroundColor(ViewCompat.MEASURED_STATE_MASK);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        linearLayout.setVisibility(8);
        this.menuContainer = linearLayout;
        this.menuList = new ListView(context);
        this.menuList.setBackgroundColor(Color.rgb(20, 24, 26));
        this.menuList.setFocusable(true);
        this.menuList.setFocusableInTouchMode(true);
        linearLayout.addView(this.menuList, new LinearLayout.LayoutParams(-1, -1));
        addView(this.menuContainer, new LinearLayout.LayoutParams(0, -1, 0.4f));
        addView(view, new LinearLayout.LayoutParams(0, -1, 1.0f));
    }

    private void setMenuOpen(boolean z) {
        if (this.menuOpen == z) {
            return;
        }
        this.menuOpen = z;
        this.menuContainer.setVisibility(this.menuOpen ? 0 : 8);
        if (this.menuOpen) {
            this.menuList.requestFocus();
            return;
        }
        boolean z2 = this.contentView instanceof ViewGroup;
        View view = this.contentView;
        if (!z2) {
            view.requestFocus();
            return;
        }
        ViewGroup viewGroup = (ViewGroup) view;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            if (viewGroup.getChildAt(i) instanceof PlayerView) {
                viewGroup.getChildAt(i).requestFocus();
                return;
            }
        }
    }

    void closeMenu() {
        setMenuOpen(false);
    }

    boolean isMenuOpen() {
        return this.menuOpen;
    }

    void openMenu() {
        setMenuOpen(true);
    }

    void toggleMenu() {
        setMenuOpen(!this.menuOpen);
    }
}
