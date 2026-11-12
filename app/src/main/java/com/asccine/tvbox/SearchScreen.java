package com.asccine.tvbox;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Resultados da pesquisa (filmes e séries, até 100). A pesquisa começa na barra "Pesquisar"
 * da Início; aqui o botão do alto serve para pesquisar de novo.
 */
final class SearchScreen extends Screen {

    private static final int MAX_RESULTS = 100;

    private final FrameLayout root;
    private final TextView queryButton;
    private final TvGrid grid;
    private final CardAdapter adapter;
    private final TextView status;
    private Bg.Handle running;
    private int gen;
    private String lastQuery = "";

    SearchScreen(ShellActivity act) {
        super(act);
        root = new FrameLayout(act);
        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout head = new LinearLayout(act);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(Ui.dp(act, 36), Ui.dp(act, 22), Ui.dp(act, 36), Ui.dp(act, 6));
        head.addView(Ui.title(act, "Resultados", 34, C.TEXT));
        View sp = new View(act);
        head.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
        queryButton = Ui.button(act, "Pesquisar de novo", Ui.ACCENT);
        head.addView(queryButton, new LinearLayout.LayoutParams(Ui.dp(act, 420), -2));
        queryButton.setOnClickListener(v -> openKeyboard());
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
                act.itemMenu(adapter.items.get(pos), () -> run(lastQuery));
            }
        });

        status = Ui.text(act, "", 20, C.TEXT2);
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
        return adapter.items.isEmpty() ? queryButton : grid;
    }

    @Override
    boolean handleUp(View focused) {
        if (focused == grid) {
            queryButton.requestFocus();
            return true;
        }
        return false;
    }

    @Override
    void onShow() {
        status.setText("");
        String q = act.pendingQuery;
        act.pendingQuery = null;
        if (q != null && !q.trim().isEmpty()) {
            run(q);
        } else {
            root.post(this::openKeyboard);
        }
    }

    @Override
    void onHide() {
        gen++;
        super.onHide();
    }

    private void openKeyboard() {
        SearchKeyboard.show(act, "Pesquisar filmes e séries", "Buscar", 2,
                new SearchKeyboard.Listener() {
                    @Override
                    public void onSearch(String text) {
                        run(text);
                    }

                    @Override
                    public void onCancel() {
                        firstFocus().requestFocus();
                    }
                });
    }

    private void run(final String text) {
        if (text == null || text.trim().length() < 2) {
            return;
        }
        final String q = text.trim();
        lastQuery = q;
        gen++;
        final int myGen = gen;
        if (running != null) {
            running.cancel();
        }
        adapter.items.clear();
        adapter.notifyDataSetChanged();
        queryButton.setText("Busca: " + q);
        status.setText("Buscando...");
        status.setVisibility(View.VISIBLE);
        final int[] count = {0};
        running = bgH(h -> {
            Catalog.search(act, q, h, item -> {
                if (h.isCancelled()) {
                    return false;
                }
                count[0]++;
                final XtreamApi.Item found = item;
                Bg.ui(() -> {
                    if (myGen == gen) {
                        adapter.items.add(found);
                        adapter.notifyDataSetChanged();
                        status.setVisibility(View.GONE);
                    }
                });
                return count[0] < MAX_RESULTS;
            });
            return count[0];
        }, new Bg.Done<Integer>() {
            @Override
            public void ok(Integer n) {
                if (myGen != gen) {
                    return;
                }
                if (adapter.items.isEmpty()) {
                    status.setText("Nada encontrado para \"" + q + "\".");
                    status.setVisibility(View.VISIBLE);
                } else {
                    grid.requestFocus();
                }
            }

            @Override
            public void fail(Exception e) {
                if (myGen != gen) {
                    return;
                }
                if (adapter.items.isEmpty()) {
                    status.setText(errorText(e));
                    status.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
