package com.asccine.tvbox;

import android.content.Context;

import java.util.List;

/** Lista de episódios que está tocando (para o botão "Pular para o próximo episódio"). */
final class PlayQueue {

    static List<Catalog.Ep> eps;
    static int index;
    static XtreamApi.Item series;
    static String seriesKey = "";

    private PlayQueue() {
    }

    static void set(List<Catalog.Ep> list, int i, XtreamApi.Item s, String key) {
        eps = list;
        index = i;
        series = s;
        seriesKey = key == null ? "" : key;
    }

    static void clear() {
        eps = null;
        series = null;
        seriesKey = "";
        index = 0;
    }

    static boolean hasNext() {
        return eps != null && index + 1 < eps.size();
    }

    static String titleFor(int i) {
        if (eps == null || series == null || i < 0 || i >= eps.size()) {
            return "";
        }
        Catalog.Ep e = eps.get(i);
        return series.name + "  \u2022  T" + e.season + " Ep. " + e.number;
    }

    /** Remember where the series was left (used by the "Continuar" button). */
    static void remember(Context c) {
        if (eps != null && !seriesKey.isEmpty()) {
            SeriesProgress.save(c, seriesKey, index);
        }
    }
}
