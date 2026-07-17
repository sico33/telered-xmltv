package com.playtv.premium;

import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes2.dex */
final class Category {
    final List<Channel> channels = new ArrayList();
    final String name;

    Category(String str) {
        this.name = str;
    }
}
