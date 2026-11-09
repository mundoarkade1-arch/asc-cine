package com.asccine.tvbox;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Detalhe de filme ou série: capa, nota, sinopse, botões e (série) temporadas e episódios. */
public class DetailActivity extends Activity {

    private XtreamApi.Item item;
    private XtreamApi.Details details;
    private List<Catalog.Ep> eps = new ArrayList<>();
    private boolean loaded;
    private boolean autoplay;
    private int season = -1;
    private Bg.Handle task;

    private ScrollView sv;
    private LinearLayout col;
    private TextView metaTv;
    private TextView plotTv;
    private LinearLayout chips;
    private LinearLayout buttons;
    private LinearLayout episodesBox;
    private TextView epInfo;
    private ImageView poster;
    private ImageView backdrop;
    private TextView primary;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        AppPrefs.applyWindow(this);
        item = Nav.item;
        autoplay = Nav.autoplay;
        Nav.autoplay = false;
        if (item == null) {
            finish();
            return;
        }
        build();
        load();
    }

    private void build() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(C.BG);
        backdrop = new ImageView(this);
        backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backdrop.setAlpha(0.35f);
        root.addView(backdrop, new FrameLayout.LayoutParams(-1, Ui.screenH(this) / 2));
        View fade = new View(this);
        fade.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xFF101010, 0xFF101010, 0x00101010}));
        root.addView(fade, new FrameLayout.LayoutParams(-1, Ui.screenH(this) / 2));

        sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(this, 48), Ui.dp(this, 24), Ui.dp(this, 48), Ui.dp(this, 48));
        sv.addView(col, new FrameLayout.LayoutParams(-1, -2));
        root.addView(sv, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        TextView back = Ui.button(this, "\u2190  Voltar", Ui.DARK);
        back.setOnClickListener(v -> finish());
        col.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 170), -2));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, -2);
        tlp.topMargin = Ui.dp(this, 20);
        col.addView(top, tlp);

        int pw = Ui.dp(this, 250);
        poster = new ImageView(this);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackground(Ui.shape(C.SURFACE, Ui.dp(this, 16), 0, 0));
        final float rad = Ui.dp(this, 16);
        poster.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View v, android.graphics.Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), rad);
            }
        });
        poster.setClipToOutline(true);
        top.addView(poster, new LinearLayout.LayoutParams(pw, pw * 3 / 2));
        ImageLoader.load(poster, item.icon, 480);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0, -2, 1f);
        ilp.leftMargin = Ui.dp(this, 36);
        top.addView(info, ilp);

        boolean series = item.kind == XtreamApi.Kind.SERIES;
        info.addView(Ui.limeTag(this, series ? "SÉRIE" : "FILME"));
        TextView title = Ui.title(this, item.name, 40, C.TEXT);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(-2, -2);
        l1.topMargin = Ui.dp(this, 12);
        info.addView(title, l1);
        metaTv = Ui.text(this, HeroView.buildMeta("", item.rating, ""), 20, C.TEXT);
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(-2, -2);
        l2.topMargin = Ui.dp(this, 10);
        info.addView(metaTv, l2);

        buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams l3 = new LinearLayout.LayoutParams(-1, -2);
        l3.topMargin = Ui.dp(this, 20);
        info.addView(buttons, l3);

        chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams l4 = new LinearLayout.LayoutParams(-2, -2);
        l4.topMargin = Ui.dp(this, 18);
        info.addView(chips, l4);

        TextView sin = Ui.title(this, "Sinopse", 22, C.TEXT);
        LinearLayout.LayoutParams l5 = new LinearLayout.LayoutParams(-2, -2);
        l5.topMargin = Ui.dp(this, 20);
        info.addView(sin, l5);
        plotTv = Ui.text(this, "Carregando...", 18, C.TEXT2);
        plotTv.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams l6 = new LinearLayout.LayoutParams(-1, -2);
        l6.topMargin = Ui.dp(this, 8);
        info.addView(plotTv, l6);

        episodesBox = new LinearLayout(this);
        episodesBox.setOrientation(LinearLayout.VERTICAL);
        col.addView(episodesBox, new LinearLayout.LayoutParams(-1, -2));
        renderButtons();
    }

    private void load() {
        final android.content.Context c = this;
        task = Bg.run(this, () -> {
            Object[] r = new Object[2];
            r[0] = Catalog.details(c, item);
            if (item.kind == XtreamApi.Kind.SERIES) {
                r[1] = Catalog.episodes(c, item);
            }
            return r;
        }, new Bg.Done<Object[]>() {
            @SuppressWarnings("unchecked")
            @Override
            public void ok(Object[] r) {
                details = (XtreamApi.Details) r[0];
                if (r[1] != null) {
                    eps = (List<Catalog.Ep>) r[1];
                }
                loaded = true;
                applyDetails();
                renderButtons();
                renderEpisodes();
                if (autoplay) {
                    autoplay = false;
                    playPrimary();
                }
            }

            @Override
            public void fail(Exception e) {
                loaded = true;
                plotTv.setText("Não foi possível carregar os detalhes agora.");
                renderButtons();
            }
        });
    }

    private void applyDetails() {
        if (details == null) {
            return;
        }
        double r = item.rating;
        if (r <= 0 && !details.rating.isEmpty()) {
            try {
                r = Double.parseDouble(details.rating.replace(',', '.'));
            } catch (NumberFormatException ignored) {
                r = 0;
            }
        }
        metaTv.setText(HeroView.buildMeta(details.year, r, details.duration));
        plotTv.setText(details.plot.isEmpty() ? "Sem sinopse nesta lista." : details.plot);
        if (!details.backdrop.isEmpty()) {
            ImageLoader.load(backdrop, details.backdrop, 1280);
        }
        chips.removeAllViews();
        if (!details.genre.isEmpty()) {
            String[] parts = details.genre.split("[,/|]");
            for (int i = 0; i < parts.length && i < 4; i++) {
                String g = parts[i].trim();
                if (g.isEmpty()) {
                    continue;
                }
                TextView chip = Ui.pill(this, g, 16, C.SURFACE_FOCUS, C.TEXT);
                chip.setPadding(Ui.dp(this, 16), Ui.dp(this, 7), Ui.dp(this, 16), Ui.dp(this, 7));
                chip.setBackground(Ui.shape(C.SURFACE, Ui.dp(this, 30), C.LINE, Ui.dp(this, 1)));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
                lp.rightMargin = Ui.dp(this, 10);
                chips.addView(chip, lp);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (loaded) {
            renderButtons();
            renderEpisodes();
        }
    }

    // ---------- Botões ----------

    private int continueIndex() {
        if (eps.isEmpty()) {
            return -1;
        }
        int saved = SeriesProgress.load(this, Catalog.seriesKey(this, item));
        if (saved < 0 || saved >= eps.size()) {
            return -1;
        }
        PlaybackProgress.Mark m = markOf(saved);
        if (m != null && m.isFinished() && saved + 1 < eps.size()) {
            return saved + 1;
        }
        return saved;
    }

    private PlaybackProgress.Mark markOf(int i) {
        Catalog.Ep e = eps.get(i);
        return PlaybackProgress.load(this, Catalog.progressKey(this, true, e.id,
                Catalog.urlOf(this, e)));
    }

    private void renderButtons() {
        buttons.removeAllViews();
        boolean series = item.kind == XtreamApi.Kind.SERIES;
        String label;
        PlaybackProgress.Mark movieMark = null;
        int cont = -1;
        if (series) {
            cont = continueIndex();
            if (!loaded) {
                label = "Carregando...";
            } else if (eps.isEmpty()) {
                label = "Sem episódios";
            } else if (cont >= 0) {
                Catalog.Ep e = eps.get(cont);
                label = "Continuar T" + e.season + " Ep. " + e.number;
            } else {
                Catalog.Ep e = eps.get(0);
                label = "Assistir T" + e.season + " Ep. " + e.number;
            }
        } else {
            String url = Catalog.urlOf(this, item);
            movieMark = PlaybackProgress.load(this, Catalog.progressKey(this, false, item.id,
                    url));
            label = movieMark != null && movieMark.isPartial()
                    ? "Continuar de " + fmt(movieMark.positionMs) : "Reproduzir";
        }
        primary = Ui.button(this, label, Ui.PRIMARY);
        primary.setTypeface(Ui.bold(this));
        primary.setCompoundDrawablePadding(Ui.dp(this, 10));
        primary.setCompoundDrawablesWithIntrinsicBounds(tinted(R.drawable.ic_play, C.BG), null,
                null, null);
        primary.setOnClickListener(v -> playPrimary());
        buttons.addView(primary, new LinearLayout.LayoutParams(-1, -2));

        boolean partialMovie = movieMark != null && movieMark.isPartial();
        if (partialMovie || cont >= 0) {
            TextView again = Ui.button(this, series ? "Assistir do começo" : "Reproduzir do início",
                    Ui.DARK);
            again.setOnClickListener(v -> playFromStart());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = Ui.dp(this, 10);
            buttons.addView(again, lp);
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.topMargin = Ui.dp(this, 10);
        buttons.addView(row, rlp);
        boolean fav = UserLists.isFavorite(this, item);
        final TextView favBtn = Ui.button(this, fav ? "Nos favoritos" : "Favorito", Ui.DARK);
        favBtn.setOnClickListener(v -> {
            boolean now = UserLists.toggleFavorite(this, item);
            favBtn.setText(now ? "Nos favoritos" : "Favorito");
        });
        row.addView(favBtn, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView blockBtn = Ui.button(this, "Bloquear", Ui.DARK);
        blockBtn.setOnClickListener(v -> Dialogs.confirm(this, item.name,
                "Bloquear esconde este item de todas as telas deste perfil. Para desbloquear, "
                        + "abra Ajustes e Itens bloqueados.", "Bloquear", () -> {
                    UserLists.block(this, item);
                    finish();
                }));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0, -2, 1f);
        blp.leftMargin = Ui.dp(this, 10);
        row.addView(blockBtn, blp);
        if (!loaded || (series && eps.isEmpty())) {
            primary.setAlpha(0.6f);
        }
        primary.requestFocus();
    }

    private android.graphics.drawable.Drawable tinted(int res, int color) {
        android.graphics.drawable.Drawable d = getResources().getDrawable(res).mutate();
        d.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        return d;
    }

    // ---------- Tocar ----------

    private void playPrimary() {
        if (!loaded) {
            return;
        }
        if (item.kind == XtreamApi.Kind.SERIES) {
            if (eps.isEmpty()) {
                return;
            }
            int i = continueIndex();
            playEpisode(i >= 0 ? i : 0, false);
        } else {
            playMovie(false);
        }
    }

    private void playFromStart() {
        if (item.kind == XtreamApi.Kind.SERIES) {
            playEpisode(0, true);
        } else {
            playMovie(true);
        }
    }

    private void playMovie(boolean fromStart) {
        String url = Catalog.urlOf(this, item);
        if (url == null) {
            Ui.toast(this, "Não foi possível abrir este filme.");
            return;
        }
        String key = Catalog.progressKey(this, false, item.id, url);
        PlayQueue.clear();
        PlayerActivity.launch(this, url, item.name, key, "movie", item.id, item.ext, item.icon,
                "", fromStart);
    }

    private void playEpisode(int i, boolean fromStart) {
        if (i < 0 || i >= eps.size()) {
            return;
        }
        Catalog.Ep e = eps.get(i);
        String url = Catalog.urlOf(this, e);
        if (url == null) {
            Ui.toast(this, "Não foi possível abrir este episódio.");
            return;
        }
        String sk = Catalog.seriesKey(this, item);
        PlayQueue.set(eps, i, item, sk);
        PlayQueue.remember(this);
        String key = Catalog.progressKey(this, true, e.id, url);
        PlayerActivity.launch(this, url, PlayQueue.titleFor(i), key, "episode", e.id, e.ext,
                item.icon, sk, fromStart);
    }

    // ---------- Temporadas e episódios ----------

    private List<Integer> seasons() {
        List<Integer> out = new ArrayList<>();
        for (Catalog.Ep e : eps) {
            if (!out.contains(e.season)) {
                out.add(e.season);
            }
        }
        return out;
    }

    private void renderEpisodes() {
        episodesBox.removeAllViews();
        if (item.kind != XtreamApi.Kind.SERIES || eps.isEmpty()) {
            return;
        }
        List<Integer> seasons = seasons();
        int cont = continueIndex();
        if (season < 0 || !seasons.contains(season)) {
            season = cont >= 0 ? eps.get(cont).season : seasons.get(0);
        }
        TextView h = Ui.title(this, "Episódios", 26, C.TEXT);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(-2, -2);
        hl.topMargin = Ui.dp(this, 30);
        episodesBox.addView(h, hl);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setClipToPadding(false);
        hs.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        LinearLayout line = new LinearLayout(this);
        hs.addView(line);
        for (final int s : seasons) {
            boolean on = s == season;
            TextView chip = Ui.button(this, "Temporada " + s, on ? Ui.LIME : Ui.DARK);
            chip.setOnClickListener(v -> {
                season = s;
                renderEpisodes();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = Ui.dp(this, 10);
            line.addView(chip, lp);
        }
        episodesBox.addView(hs, new LinearLayout.LayoutParams(-1, -2));

        epInfo = Ui.text(this, "", 18, C.TEXT2);
        LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(-2, -2);
        il.topMargin = Ui.dp(this, 8);
        il.bottomMargin = Ui.dp(this, 8);
        episodesBox.addView(epInfo, il);

        int perRow = 10;
        LinearLayout row = null;
        int inRow = 0;
        for (int i = 0; i < eps.size(); i++) {
            final Catalog.Ep e = eps.get(i);
            if (e.season != season) {
                continue;
            }
            if (row == null || inRow == perRow) {
                row = new LinearLayout(this);
                row.setClipChildren(false);
                episodesBox.addView(row, new LinearLayout.LayoutParams(-2, -2));
                inRow = 0;
            }
            final int index = i;
            row.addView(episodeTile(e, index, index == cont), tileLp());
            inRow++;
        }
    }

    private LinearLayout.LayoutParams tileLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 96),
                Ui.dp(this, 64));
        lp.setMargins(0, Ui.dp(this, 5), Ui.dp(this, 10), Ui.dp(this, 5));
        return lp;
    }

    /** Visto = limão. Atual = branco com contorno limão. O resto = cinza. */
    private TextView episodeTile(final Catalog.Ep e, final int index, boolean current) {
        PlaybackProgress.Mark m = markOf(index);
        boolean seen = m != null && m.isFinished();
        int fill;
        int text;
        int stroke = 0;
        int strokePx = 0;
        if (current) {
            fill = C.TEXT;
            text = C.BG;
            stroke = C.LIME;
            strokePx = Ui.dp(this, 3);
        } else if (seen) {
            fill = C.LIME;
            text = C.BG;
        } else {
            fill = C.LINE;
            text = C.TEXT2;
        }
        final TextView t = Ui.title(this, String.valueOf(e.number), 22, text);
        t.setGravity(Gravity.CENTER);
        t.setFocusable(true);
        t.setClickable(true);
        t.setBackground(Ui.focusBg(this, fill, fill, 12, stroke, strokePx));
        t.setOnClickListener(v -> playEpisode(index, false));
        t.setOnFocusChangeListener((v, has) -> {
            if (has && epInfo != null) {
                epInfo.setText("T" + e.season + " Ep. " + e.number
                        + (e.title.isEmpty() ? "" : "  \u2022  " + e.title));
            }
        });
        return t;
    }

    @Override
    protected void onDestroy() {
        if (task != null) {
            task.cancel();
        }
        super.onDestroy();
    }

    private static String fmt(long ms) {
        long s = ms / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return h > 0 ? String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, sec)
                : String.format(java.util.Locale.US, "%d:%02d", m, sec);
    }
}
