package com.asccine.tvbox;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Detalhe de filme ou série. Tudo cabe numa tela de 1080p, sem rolar:
 * coluna da esquerda com "Voltar" e a capa; coluna da direita com selo, título, nota,
 * sinopse curta (com "Ler mais"), botões e, nas séries, o botão de Temporada e os primeiros
 * 30 episódios em grade de números. Passou de 30: botão "Ver mais" abre uma tela inteira com
 * todos os episódios; ao escolher um, a tela fecha e o episódio começa a tocar.
 */
public class DetailActivity extends Activity {

    /** Quantos episódios aparecem direto na tela de detalhe. */
    private static final int VISIBLE_EPISODES = 30;
    private static final int PER_ROW = 10;

    private XtreamApi.Item item;
    private XtreamApi.Details details;
    private List<Catalog.Ep> eps = new ArrayList<>();
    private boolean loaded;
    private boolean autoplay;
    private boolean series;
    private int season = -1;
    private Bg.Handle task;

    private TextView metaTv;
    private TextView genreTv;
    private TextView plotTv;
    private TextView readMore;
    private LinearLayout buttons;
    private LinearLayout episodesBox;
    private TextView epInfo;
    private TextView seasonBtn;
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
        series = item.kind == XtreamApi.Kind.SERIES;
        build();
        load();
    }

    /** Botão menor, para caber mais coisa na tela. */
    private TextView cbtn(String label, int style) {
        TextView t = Ui.button(this, label, style);
        t.setTextSize(16);
        t.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    private void build() {
        FrameLayout root = new FrameLayout(this);
        backdrop = new ImageView(this);
        backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backdrop.setAlpha(0.35f);
        root.addView(backdrop, new FrameLayout.LayoutParams(-1, Ui.screenH(this) / 2));
        View fade = new View(this);
        fade.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xFF101010, 0xFF101010, 0x00101010}));
        root.addView(fade, new FrameLayout.LayoutParams(-1, Ui.screenH(this) / 2));

        // Se a tela for muito pequena, rola (rede de segurança). Em 1080p não rola.
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.HORIZONTAL);
        main.setPadding(Ui.dp(this, 40), Ui.dp(this, 16), Ui.dp(this, 40), Ui.dp(this, 12));
        sv.addView(main, new FrameLayout.LayoutParams(-1, -2));
        root.addView(sv, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        // ----- Coluna da esquerda: Voltar + capa -----
        int pw = Ui.dp(this, series ? 150 : 200);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        main.addView(left, new LinearLayout.LayoutParams(pw, -2));

        TextView back = cbtn("\u2190  Voltar", Ui.DARK);
        back.setOnClickListener(v -> finish());
        left.addView(back, new LinearLayout.LayoutParams(-1, -2));

        poster = new ImageView(this);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackground(Glass.drawable(this, Glass.LIGHT, 14));
        final float rad = Ui.dp(this, 14);
        poster.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View v, android.graphics.Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), rad);
            }
        });
        poster.setClipToOutline(true);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(pw, pw * 3 / 2);
        plp.topMargin = Ui.dp(this, 12);
        left.addView(poster, plp);
        ImageLoader.load(poster, item.icon, 480);

        // ----- Coluna da direita: informações, botões e episódios -----
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0, -2, 1f);
        ilp.leftMargin = Ui.dp(this, 28);
        main.addView(info, ilp);

        info.addView(Ui.accentTag(this, series ? "SÉRIE" : "FILME"));
        TextView title = Ui.title(this, item.name, series ? 26 : 34, C.TEXT);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(-2, -2);
        l1.topMargin = Ui.dp(this, 6);
        info.addView(title, l1);

        metaTv = Ui.text(this, HeroView.buildMeta("", item.rating, ""), series ? 17 : 20,
                C.TEXT);
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(-2, -2);
        l2.topMargin = Ui.dp(this, 4);
        info.addView(metaTv, l2);

        genreTv = Ui.text(this, "", series ? 15 : 17, C.ACCENT);
        genreTv.setSingleLine(true);
        genreTv.setEllipsize(TextUtils.TruncateAt.END);
        genreTv.setVisibility(View.GONE);
        LinearLayout.LayoutParams lg = new LinearLayout.LayoutParams(-2, -2);
        lg.topMargin = Ui.dp(this, 3);
        info.addView(genreTv, lg);

        plotTv = Ui.text(this, "Carregando...", series ? 14 : 18, C.TEXT2);
        plotTv.setLineSpacing(0, 1.1f);
        plotTv.setMaxLines(series ? 3 : 6);
        plotTv.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams l6 = new LinearLayout.LayoutParams(-1, -2);
        l6.topMargin = Ui.dp(this, 8);
        info.addView(plotTv, l6);

        readMore = Ui.text(this, "Ler mais", 15, C.ACCENT);
        readMore.setFocusable(true);
        readMore.setClickable(true);
        readMore.setPadding(Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 4));
        readMore.setBackground(Glass.focusOnly(this, Glass.LIGHT, 8));
        readMore.setVisibility(View.GONE);
        readMore.setOnClickListener(v -> Dialogs.info(this, item.name,
                details == null || details.plot.isEmpty() ? "Sem sinopse nesta lista."
                        : details.plot));
        LinearLayout.LayoutParams lr = new LinearLayout.LayoutParams(-2, -2);
        lr.topMargin = Ui.dp(this, 2);
        info.addView(readMore, lr);

        buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams l3 = new LinearLayout.LayoutParams(-1, -2);
        l3.topMargin = Ui.dp(this, 10);
        info.addView(buttons, l3);

        episodesBox = new LinearLayout(this);
        episodesBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams l7 = new LinearLayout.LayoutParams(-1, -2);
        l7.topMargin = Ui.dp(this, 12);
        info.addView(episodesBox, l7);
        renderButtons();
    }

    private void load() {
        final android.content.Context c = this;
        task = Bg.run(this, () -> {
            Object[] r = new Object[2];
            r[0] = Catalog.details(c, item);
            if (series) {
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
        genreTv.setVisibility(View.GONE);
        if (!details.genre.isEmpty()) {
            String[] parts = details.genre.split("[,/|]");
            StringBuilder g = new StringBuilder();
            for (int i = 0; i < parts.length && i < 4; i++) {
                String one = parts[i].trim();
                if (one.isEmpty()) {
                    continue;
                }
                if (g.length() > 0) {
                    g.append("  \u2022  ");
                }
                g.append(one);
            }
            if (g.length() > 0) {
                genreTv.setText(g.toString());
                genreTv.setVisibility(View.VISIBLE);
            }
        }
        // Se a sinopse foi cortada, aparece o "Ler mais".
        plotTv.post(() -> {
            Layout lay = plotTv.getLayout();
            boolean cut = lay != null && lay.getLineCount() > 0
                    && lay.getEllipsisCount(lay.getLineCount() - 1) > 0;
            readMore.setVisibility(cut ? View.VISIBLE : View.GONE);
        });
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
        primary = cbtn(label, Ui.PRIMARY);
        primary.setTypeface(Ui.bold(this));
        primary.setCompoundDrawablePadding(Ui.dp(this, 8));
        primary.setCompoundDrawablesWithIntrinsicBounds(tinted(R.drawable.ic_play, C.BG), null,
                null, null);
        primary.setOnClickListener(v -> playPrimary());
        buttons.addView(primary, new LinearLayout.LayoutParams(0, -2, 2.2f));

        boolean partialMovie = movieMark != null && movieMark.isPartial();
        if (partialMovie || cont >= 0) {
            TextView again = cbtn("Do começo", Ui.DARK);
            again.setOnClickListener(v -> playFromStart());
            buttons.addView(again, buttonLp(1.3f));
        }

        boolean fav = UserLists.isFavorite(this, item);
        final TextView favBtn = cbtn(fav ? "Nos favoritos" : "Favorito", Ui.DARK);
        favBtn.setOnClickListener(v -> {
            boolean now = UserLists.toggleFavorite(this, item);
            favBtn.setText(now ? "Nos favoritos" : "Favorito");
        });
        buttons.addView(favBtn, buttonLp(1.4f));

        TextView blockBtn = cbtn("Bloquear", Ui.DARK);
        blockBtn.setOnClickListener(v -> Dialogs.confirm(this, item.name,
                "Bloquear esconde este item de todas as telas deste perfil. Para desbloquear, "
                        + "abra Ajustes e Itens bloqueados.", "Bloquear", () -> {
                    UserLists.block(this, item);
                    finish();
                }));
        buttons.addView(blockBtn, buttonLp(1.1f));

        if (!loaded || (series && eps.isEmpty())) {
            primary.setAlpha(0.6f);
        }
        primary.requestFocus();
    }

    private LinearLayout.LayoutParams buttonLp(float weight) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, weight);
        lp.leftMargin = Ui.dp(this, 8);
        return lp;
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
        if (series) {
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
        if (series) {
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

    private static String seasonName(int s) {
        return s == 0 ? "Especiais" : "Temporada " + s;
    }

    /** Posições (na lista completa) dos episódios de uma temporada. */
    private List<Integer> indexesOf(int s) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < eps.size(); i++) {
            if (eps.get(i).season == s) {
                out.add(i);
            }
        }
        return out;
    }

    private void renderEpisodes() {
        episodesBox.removeAllViews();
        epInfo = null;
        seasonBtn = null;
        if (!series || eps.isEmpty()) {
            return;
        }
        final List<Integer> seasons = seasons();
        int cont = continueIndex();
        if (season < 0 || !seasons.contains(season)) {
            season = cont >= 0 ? eps.get(cont).season : seasons.get(0);
        }
        final List<Integer> inSeason = indexesOf(season);

        // Linha: [Temporada N ▾]  [Ver mais]  texto do episódio em foco
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        episodesBox.addView(bar, new LinearLayout.LayoutParams(-1, -2));

        seasonBtn = cbtn(seasonName(season) + "  \u25BE", Ui.PRIMARY);
        seasonBtn.setTypeface(Ui.bold(this));
        seasonBtn.setOnClickListener(v -> chooseSeason(seasons));
        bar.addView(seasonBtn, new LinearLayout.LayoutParams(-2, -2));

        if (inSeason.size() > VISIBLE_EPISODES) {
            TextView more = cbtn("Ver mais", Ui.DARK);
            more.setTextColor(C.ACCENT);
            more.setOnClickListener(v -> showAllEpisodes(inSeason));
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-2, -2);
            mlp.leftMargin = Ui.dp(this, 10);
            bar.addView(more, mlp);
        }

        epInfo = Ui.text(this, "", 15, C.TEXT2);
        epInfo.setSingleLine(true);
        epInfo.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(0, -2, 1f);
        elp.leftMargin = Ui.dp(this, 16);
        bar.addView(epInfo, elp);
        epInfo.setText(Math.min(VISIBLE_EPISODES, inSeason.size()) + " de " + inSeason.size()
                + (inSeason.size() == 1 ? " episódio" : " episódios"));

        // Os primeiros 30 episódios, em grade compacta de números.
        LinearLayout row = null;
        int inRow = 0;
        int shown = Math.min(VISIBLE_EPISODES, inSeason.size());
        for (int n = 0; n < shown; n++) {
            int index = inSeason.get(n);
            if (row == null || inRow == PER_ROW) {
                row = new LinearLayout(this);
                row.setClipChildren(false);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-2, -2);
                rlp.topMargin = n == 0 ? Ui.dp(this, 8) : 0;
                episodesBox.addView(row, rlp);
                inRow = 0;
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 56),
                    Ui.dp(this, 38));
            lp.setMargins(0, Ui.dp(this, 3), Ui.dp(this, 8), Ui.dp(this, 3));
            row.addView(episodeTile(eps.get(index), index, index == cont, 18, epInfo), lp);
            inRow++;
        }
    }

    /** Botão Temporada: abre a escolha da temporada. */
    private void chooseSeason(final List<Integer> seasons) {
        String[] labels = new String[seasons.size()];
        for (int i = 0; i < labels.length; i++) {
            int n = indexesOf(seasons.get(i)).size();
            labels[i] = seasonName(seasons.get(i)) + "  (" + n
                    + (n == 1 ? " episódio)" : " episódios)");
        }
        Dialogs.list(this, "Escolha a temporada", labels, which -> {
            season = seasons.get(which);
            renderEpisodes();
            if (seasonBtn != null) {
                seasonBtn.requestFocus();
            }
        });
    }

    /** "Ver mais": tela inteira só com todos os episódios da temporada (grade grande). */
    private void showAllEpisodes(List<Integer> inSeason) {
        final Dialog d = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        d.setCancelable(true);
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(Glass.backdrop(this));
        }
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(this, 48), Ui.dp(this, 26), Ui.dp(this, 48), Ui.dp(this, 36));
        scroll.addView(col, new FrameLayout.LayoutParams(-1, -2));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        col.addView(head, new LinearLayout.LayoutParams(-1, -2));
        head.addView(Ui.title(this, Ui.highlight(seasonName(season) + " - todos os episódios",
                seasonName(season), C.ACCENT), 28, C.TEXT));
        final TextView info = Ui.text(this, "Escolha o episódio. Voltar fecha.", 17, C.TEXT2);
        info.setSingleLine(true);
        info.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0, -2, 1f);
        ilp.leftMargin = Ui.dp(this, 24);
        head.addView(info, ilp);

        int tileW = Ui.dp(this, 104);
        int tileH = Ui.dp(this, 66);
        int gap = Ui.dp(this, 12);
        int usable = Ui.screenW(this) - Ui.dp(this, 96);
        int perRow = Math.max(4, usable / (tileW + gap));
        int cont = continueIndex();
        View focusTile = null;
        View firstTile = null;
        LinearLayout row = null;
        int inRow = 0;
        for (int n = 0; n < inSeason.size(); n++) {
            final int index = inSeason.get(n);
            if (row == null || inRow == perRow) {
                row = new LinearLayout(this);
                row.setClipChildren(false);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-2, -2);
                rlp.topMargin = n == 0 ? Ui.dp(this, 18) : 0;
                col.addView(row, rlp);
                inRow = 0;
            }
            TextView tile = episodeTile(eps.get(index), index, index == cont, 24, info);
            tile.setOnClickListener(v -> {
                d.dismiss();
                playEpisode(index, false);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tileW, tileH);
            lp.setMargins(0, Ui.dp(this, 6), gap, Ui.dp(this, 6));
            row.addView(tile, lp);
            inRow++;
            if (firstTile == null) {
                firstTile = tile;
            }
            if (index == cont) {
                focusTile = tile;
            }
        }
        d.setContentView(scroll);
        if (focusTile != null) {
            focusTile.requestFocus();
        } else if (firstTile != null) {
            firstTile.requestFocus();
        }
        if (!isFinishing() && !isDestroyed()) {
            d.show();
        }
    }

    /** Visto = violeta. Atual = branco com contorno violeta. O resto = cinza. */
    private TextView episodeTile(final Catalog.Ep e, final int index, boolean current, float sp,
                                 final TextView infoTarget) {
        PlaybackProgress.Mark m = markOf(index);
        boolean seen = m != null && m.isFinished();
        int fill;
        int text;
        int stroke = 0;
        int strokePx = 0;
        if (current) {
            fill = C.TEXT;
            text = C.BG;
            stroke = C.ACCENT;
            strokePx = Ui.dp(this, 3);
        } else if (seen) {
            fill = C.ACCENT;
            text = C.ON_ACCENT;
        } else {
            fill = C.LINE;
            text = C.TEXT2;
        }
        final TextView t = Ui.title(this, String.valueOf(e.number), sp, text);
        t.setGravity(Gravity.CENTER);
        t.setFocusable(true);
        t.setClickable(true);
        if (!current && !seen) {
            t.setBackground(Glass.focusBg(this, Glass.LIGHT, 10));
        } else {
            t.setBackground(Ui.focusBg(this, fill, fill, 10, stroke, strokePx));
        }
        t.setOnClickListener(v -> playEpisode(index, false));
        t.setOnFocusChangeListener((v, has) -> {
            if (has && infoTarget != null) {
                infoTarget.setText("T" + e.season + " Ep. " + e.number
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
