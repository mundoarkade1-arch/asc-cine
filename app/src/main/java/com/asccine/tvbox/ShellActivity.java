package com.asccine.tvbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.FrameLayout;

/**
 * Tela principal: barra lateral fina + a tela escolhida (Início, Filmes, Séries, Explorar,
 * Busca, Favoritos, Ajustes). Só uma tela fica montada por vez, para poupar memória.
 */
public class ShellActivity extends Activity implements Sidebar.Listener {

    private FrameLayout content;
    private Sidebar sidebar;
    private Screen current;
    private int currentId = -1;
    private boolean armed = true;

    /** Quando outra tela pede "abrir Filmes nesta categoria". */
    XtreamApi.Kind pendingKind;
    String pendingCat;
    String pendingCatName;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        AppPrefs.applyWindow(this);
        if (Catalog.creds(this) == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(C.BG);
        content = new FrameLayout(this);
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(-1, -1);
        clp.leftMargin = Ui.dp(this, 72);
        root.addView(content, clp);
        Profiles.P p = Profiles.current(this);
        sidebar = new Sidebar(this, this, p.name, p.avatar);
        root.addView(sidebar);
        setContentView(root);
        showScreen(Sidebar.HOME, true);
        DeviceLink.syncInBackground(this);
        UpdateUi.checkOnce(this);
    }

    // ---------- Telas ----------

    void showScreen(int id, boolean force) {
        if (!force && id == currentId) {
            if (current != null) {
                current.focusContent();
            }
            return;
        }
        if (current != null) {
            current.onHide();
        }
        content.removeAllViews();
        Screen s;
        switch (id) {
            case Sidebar.FAVORITES:
                s = new FavoritesScreen(this);
                break;
            case Sidebar.SEARCH:
                s = new SearchScreen(this);
                break;
            case Sidebar.MOVIES:
                s = new GridScreen(this, XtreamApi.Kind.MOVIE);
                break;
            case Sidebar.SERIES:
                s = new GridScreen(this, XtreamApi.Kind.SERIES);
                break;
            case Sidebar.EXPLORE:
                s = new ExploreScreen(this);
                break;
            case Sidebar.SETTINGS:
                s = new SettingsScreen(this);
                break;
            default:
                id = Sidebar.HOME;
                s = new HomeScreen(this);
                break;
        }
        current = s;
        currentId = id;
        sidebar.setActive(id);
        content.addView(s.view(), new FrameLayout.LayoutParams(-1, -1));
        armed = true;
        s.onShow();
    }

    /** Abre a tela de Filmes ou Séries já numa categoria. */
    void openGrid(XtreamApi.Kind kind, String catId, String catName) {
        pendingKind = kind;
        pendingCat = catId;
        pendingCatName = catName;
        showScreen(kind == XtreamApi.Kind.MOVIE ? Sidebar.MOVIES : Sidebar.SERIES, true);
    }

    /** A tela avisa que já tem conteúdo: se a pessoa não mexeu na barra, o foco vai para ele. */
    void focusReady(Screen s) {
        if (s != current || !armed) {
            return;
        }
        if (sidebar.hasFocus() || getCurrentFocus() == null) {
            if (s.focusContent()) {
                armed = false;
            }
        }
    }

    // ---------- Barra lateral ----------

    @Override
    public void onSelect(int id) {
        if (id == Sidebar.PROFILE) {
            Intent i = new Intent(this, ProfilesActivity.class);
            i.putExtra("pick", true);
            startActivity(i);
            finish();
            return;
        }
        if (id == Sidebar.REFRESH) {
            Catalog.invalidate();
            ImageLoader.clear();
            final android.content.Context app = getApplicationContext();
            new Thread(() -> SearchIndex.clear(app)).start();
            showScreen(currentId < 0 ? Sidebar.HOME : currentId, true);
            return;
        }
        showScreen(id, false);
    }

    @Override
    public boolean onRight() {
        return current != null && current.focusContent();
    }

    @Override
    public void onTouched() {
        armed = false;
    }

    // ---------- Menu do item (OK longo) ----------

    void itemMenu(final XtreamApi.Item item, final Runnable changed) {
        final boolean fav = UserLists.isFavorite(this, item);
        String[] labels = {"Abrir",
                fav ? "Tirar dos favoritos" : "Adicionar aos favoritos",
                "Bloquear este item"};
        Dialogs.choose(this, item.name, labels, i -> {
            if (i == 0) {
                Nav.openDetail(this, item, false);
            } else if (i == 1) {
                boolean now = UserLists.toggleFavorite(this, item);
                Ui.toast(this, now ? "Adicionado aos favoritos." : "Tirado dos favoritos.");
                if (changed != null) {
                    changed.run();
                }
            } else {
                UserLists.block(this, item);
                Ui.toast(this, "Item bloqueado. Para desbloquear, abra Ajustes.");
                if (changed != null) {
                    changed.run();
                }
            }
        });
    }

    // ---------- Teclas ----------

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            armed = false;
            sidebar.focusActive();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (sidebar.hasFocus()) {
            if (currentId != Sidebar.HOME) {
                showScreen(Sidebar.HOME, false);
            } else {
                finish();
            }
        } else {
            armed = false;
            sidebar.focusActive();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (current != null) {
            current.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        if (current != null) {
            current.onHide();
        }
        super.onDestroy();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            ImageLoader.clear();
        }
    }
}
