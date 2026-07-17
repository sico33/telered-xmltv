package com.playtv.premium;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes2.dex */
final class CatalogState {
    static final List<Category> categories = new ArrayList();

    private CatalogState() {
    }

    static List<Channel> getAllChannels() {
        ArrayList arrayList = new ArrayList();
        Iterator<Category> it = categories.iterator();
        while (it.hasNext()) {
            arrayList.addAll(it.next().channels);
        }
        return arrayList;
    }

    static int getChannelIndex(Channel channel) {
        List<Channel> allChannels = getAllChannels();
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= allChannels.size()) {
                return -1;
            }
            if (allChannels.get(i2).originalUrl.equals(channel.originalUrl)) {
                return i2;
            }
            i = i2 + 1;
        }
    }
}
