package com.asccine.tvbox;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Favoritos do perfil: grade de capas. OK longo no cartão tira ou bloqueia. */
final class FavoritesScreen extends Screen {

    private final FrameLayout root;
    private final TvGrid grid;
    private final CardAdapter adapter;
    private final TextView empty;

    FavoritesScreen(ShellActivity act) {
        super(act);
        root = new FrameLayout(act);
        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout head = new LinearLayout(act);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(Ui.dp(act, 36), Ui.dp(act, 22), Ui.dp(act, 36), Ui.dp(act, 6));
        head.addView(Ui.title(act, "Favoritos", 34, C.TEXT));
        col.addView(head, new LinearLayout.LayoutParams(-1, -2));

        grid = new TvGrid(act);
        adapter = new CardAdapter(act, grid);
        CardAdapter.setup(grid, adapter);
        col.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1f));
        grid.setOnItemClickListener((p, v, pos, id) -> {
            if (pos >= 0 && pos < adapter.items.size()) {
                Nav.openDetail(act, adapter.items.get(pos), false);
            }
        });
        grid.setLongListener(pos -> {
            if (pos >= 0 && pos < adapter.items.size()) {
                act.itemMenu(adapter.items.get(pos), this::refresh);
            }
        });

        empty = Ui.text(act, "Você ainda não tem favoritos.\nAperte OK por mais tempo em um "
                + "filme ou série e escolha \"Adicionar aos favoritos\".", 20, C.TEXT2);
        empty.setGravity(Gravity.CENTER);
        empty.setLineSpacing(0, 1.2f);
        empty.setVisibility(View.GONE);
        FrameLayout.LayoutParams elp = new FrameLayout.LayoutParams(-1, -2);
        elp.gravity = Gravity.CENTER;
        root.addView(empty, elp);
    }

    @Override
    View view() {
        return root;
    }

    @Override
    View firstFocus() {
        return adapter.items.isEmpty() ? null : grid;
    }

    @Override
    void onShow() {
        refresh();
    }

    @Override
    void onResume() {
        refresh();
    }

    private void refresh() {
        List<XtreamApi.Item> favs = Catalog.filter(act, UserLists.favorites(act), false);
        adapter.items.clear();
        adapter.items.addAll(favs);
        adapter.notifyDataSetChanged();
        empty.setVisibility(favs.isEmpty() ? View.VISIBLE : View.GONE);
        act.focusReady(this);
    }
}
