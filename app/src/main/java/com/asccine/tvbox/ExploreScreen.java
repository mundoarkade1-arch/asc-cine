package com.asccine.tvbox;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Explorar: categorias de filmes ou de séries em cartões grandes. */
final class ExploreScreen extends Screen {

    private final FrameLayout root;
    private final TextView tabMovies;
    private final TextView tabSeries;
    private final TvGrid grid;
    private final TextView status;
    private final List<Catalog.Cat> cats = new ArrayList<>();
    private XtreamApi.Kind kind = XtreamApi.Kind.MOVIE;
    private int gen;

    private final BaseAdapter adapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return cats.size();
        }

        @Override
        public Object getItem(int i) {
            return cats.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convert, ViewGroup parent) {
            Tile t = convert instanceof Tile ? (Tile) convert : new Tile(act);
            t.bind(i + 1, cats.get(i).name);
            return t;
        }
    };

    ExploreScreen(ShellActivity act) {
        super(act);
        root = new FrameLayout(act);
        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout head = new LinearLayout(act);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(Ui.dp(act, 36), Ui.dp(act, 22), Ui.dp(act, 36), Ui.dp(act, 10));
        head.addView(Ui.title(act, "Explorar", 34, C.TEXT));
        View sp = new View(act);
        head.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
        tabMovies = Ui.button(act, "Filmes", Ui.ACCENT);
        tabSeries = Ui.button(act, "Séries", Ui.DARK);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(Ui.dp(act, 170), -2);
        head.addView(tabMovies, tlp);
        LinearLayout.LayoutParams t2 = new LinearLayout.LayoutParams(Ui.dp(act, 170), -2);
        t2.leftMargin = Ui.dp(act, 12);
        head.addView(tabSeries, t2);
        col.addView(head, new LinearLayout.LayoutParams(-1, -2));
        tabMovies.setOnClickListener(v -> switchKind(XtreamApi.Kind.MOVIE));
        tabSeries.setOnClickListener(v -> switchKind(XtreamApi.Kind.SERIES));

        grid = new TvGrid(act);
        int cols = 4;
        int width = (Ui.screenW(act) - Ui.dp(act, 72) - Ui.dp(act, 56) - Ui.dp(act, 14) * (cols - 1))
                / cols;
        grid.setNumColumns(cols);
        grid.setColumnWidth(width);
        grid.setHorizontalSpacing(Ui.dp(act, 14));
        grid.setVerticalSpacing(Ui.dp(act, 14));
        grid.setPadding(Ui.dp(act, 28), Ui.dp(act, 10), Ui.dp(act, 28), Ui.dp(act, 30));
        grid.setAdapter(adapter);
        col.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1f));
        grid.setOnItemClickListener((p, v, pos, id) -> {
            if (pos >= 0 && pos < cats.size()) {
                Catalog.Cat c = cats.get(pos);
                act.openGrid(kind, c.id, c.name);
            }
        });

        status = Ui.text(act, "Carregando...", 20, C.TEXT2);
        status.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(-1, -2);
        slp.gravity = Gravity.CENTER;
        root.addView(status, slp);
    }

    @Override
    View view() {
        return root;
    }

    @Override
    View firstFocus() {
        return cats.isEmpty() ? tabMovies : grid;
    }

    @Override
    void onShow() {
        load();
    }

    @Override
    void onHide() {
        gen++;
        super.onHide();
    }

    private void switchKind(XtreamApi.Kind k) {
        if (k == kind) {
            return;
        }
        kind = k;
        tabMovies.setBackground(Ui.tabBg(act, k == XtreamApi.Kind.MOVIE));
        tabMovies.setTextColor(k == XtreamApi.Kind.MOVIE ? C.ON_ACCENT : C.TEXT);
        tabSeries.setBackground(Ui.tabBg(act, k == XtreamApi.Kind.SERIES));
        tabSeries.setTextColor(k == XtreamApi.Kind.SERIES ? C.ON_ACCENT : C.TEXT);
        load();
    }

    private void load() {
        gen++;
        final int myGen = gen;
        cats.clear();
        adapter.notifyDataSetChanged();
        status.setText("Carregando...");
        status.setVisibility(View.VISIBLE);
        final XtreamApi.Kind k = kind;
        bg(() -> Catalog.visibleCategories(act, k), new Bg.Done<List<Catalog.Cat>>() {
            @Override
            public void ok(List<Catalog.Cat> list) {
                if (myGen != gen) {
                    return;
                }
                cats.addAll(list);
                adapter.notifyDataSetChanged();
                status.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                if (list.isEmpty()) {
                    status.setText("Nenhuma categoria nesta lista.");
                }
                act.focusReady(ExploreScreen.this);
            }

            @Override
            public void fail(Exception e) {
                if (myGen == gen) {
                    status.setText(errorText(e));
                }
            }
        });
    }

    /** Cartão grande de categoria: número no estilo ".01", nome e seta. */
    private static final class Tile extends FrameLayout {
        private final TextView num;
        private final TextView name;

        Tile(Context c) {
            super(c);
            setLayoutParams(new LayoutParams(-1, Ui.dp(c, 120)));
            setBackground(Glass.selectBg(c, Glass.MID, 16));
            num = Ui.title(c, "", 14, C.GLOW);
            LayoutParams nl = new LayoutParams(-2, -2);
            nl.setMargins(Ui.dp(c, 18), Ui.dp(c, 14), 0, 0);
            addView(num, nl);
            name = Ui.title(c, "", 22, C.TEXT);
            name.setMaxLines(2);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LayoutParams ml = new LayoutParams(-1, -2);
            ml.gravity = Gravity.BOTTOM;
            ml.setMargins(Ui.dp(c, 18), 0, Ui.dp(c, 18), Ui.dp(c, 14));
            addView(name, ml);
            android.widget.ImageView arrow = Ui.icon(c, R.drawable.ic_arrow_ne, 26, C.GLOW);
            LayoutParams al = new LayoutParams(Ui.dp(c, 26), Ui.dp(c, 26));
            al.gravity = Gravity.TOP | Gravity.END;
            al.setMargins(0, Ui.dp(c, 12), Ui.dp(c, 14), 0);
            addView(arrow, al);
        }

        void bind(int n, String title) {
            num.setText(String.format(java.util.Locale.US, ".%02d", n));
            name.setText(title);
        }
    }
}
