package com.asccine.tvbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** "Quem está assistindo?": até 5 perfis, cada um com a sua lista, favoritos e bloqueados. */
public class ProfilesActivity extends Activity {

    private boolean manage;
    private LinearLayout row;
    private LinearLayout root;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        AppPrefs.applyWindow(this);
        manage = getIntent().getBooleanExtra("manage", false);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(C.BG);
        setContentView(root);
        render();
    }

    private void render() {
        root.removeAllViews();
        TextView title = Ui.title(this, manage ? Ui.highlight("Gerenciar perfis", "perfis", C.LIME)
                : Ui.highlight("Quem está assistindo?", "assistindo", C.LIME), 44, C.TEXT);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-2, -2));
        TextView sub = Ui.text(this, manage ? "ESCOLHA UM PERFIL PARA MUDAR"
                : "ESCOLHA UM PERFIL PARA COMEÇAR", 16, C.TEXT2);
        sub.setLetterSpacing(0.15f);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-2, -2);
        slp.topMargin = Ui.dp(this, 10);
        root.addView(sub, slp);

        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setClipChildren(false);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-2, -2);
        rlp.topMargin = Ui.dp(this, 44);
        root.addView(row, rlp);

        List<Profiles.P> list = Profiles.list(this);
        String active = Profiles.currentId(this);
        View first = null;
        for (Profiles.P p : list) {
            View v = profileView(p, p.id.equals(active));
            row.addView(v);
            if (first == null || p.id.equals(active)) {
                first = v;
            }
        }
        if (list.size() < Profiles.MAX) {
            row.addView(addView());
        }
        if (first != null) {
            first.requestFocus();
        }
        if (manage) {
            TextView back = Ui.button(this, "Voltar", Ui.DARK);
            back.setOnClickListener(v -> finish());
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Ui.dp(this, 200), -2);
            blp.topMargin = Ui.dp(this, 40);
            root.addView(back, blp);
        }
    }

    private View profileView(final Profiles.P p, boolean active) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setFocusable(true);
        box.setClickable(true);
        box.setLongClickable(true);
        int pad = Ui.dp(this, 10);
        box.setPadding(pad, pad, pad, pad);
        box.setBackground(Ui.focusBg(this, 0x00000000, C.SURFACE, 22, 0x00000000, 0));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Ui.dp(this, 190), -2);
        blp.setMargins(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        box.setLayoutParams(blp);

        FrameLayout av = new FrameLayout(this);
        TextView letter = Ui.title(this, Profiles.initial(p.name), 64, Profiles.avatarFg(p.avatar));
        letter.setGravity(Gravity.CENTER);
        letter.setBackground(Ui.shape(Profiles.avatarBg(p.avatar), Ui.dp(this, 26), 0, 0));
        av.addView(letter, new FrameLayout.LayoutParams(Ui.dp(this, 150), Ui.dp(this, 150)));
        if (active) {
            TextView badge = Ui.limeTag(this, "ATIVO \u2713");
            FrameLayout.LayoutParams bl = new FrameLayout.LayoutParams(-2, -2);
            bl.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            bl.bottomMargin = Ui.dp(this, 6);
            av.addView(badge, bl);
        }
        box.addView(av, new LinearLayout.LayoutParams(Ui.dp(this, 150), Ui.dp(this, 150)));
        TextView name = Ui.title(this, p.name, 20, C.TEXT);
        name.setSingleLine(true);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(-2, -2);
        nlp.topMargin = Ui.dp(this, 12);
        box.addView(name, nlp);

        box.setOnClickListener(v -> {
            if (manage) {
                manageMenu(p);
            } else {
                choose(p);
            }
        });
        box.setOnLongClickListener(v -> {
            manageMenu(p);
            return true;
        });
        return box;
    }

    private View addView() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setFocusable(true);
        box.setClickable(true);
        int pad = Ui.dp(this, 10);
        box.setPadding(pad, pad, pad, pad);
        box.setBackground(Ui.focusBg(this, 0x00000000, C.SURFACE, 22, 0x00000000, 0));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Ui.dp(this, 190), -2);
        blp.setMargins(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        box.setLayoutParams(blp);
        TextView plus = Ui.title(this, "+", 56, C.LIME);
        plus.setGravity(Gravity.CENTER);
        plus.setBackground(Ui.shape(C.SURFACE, Ui.dp(this, 75), C.LIME_40, Ui.dp(this, 2)));
        box.addView(plus, new LinearLayout.LayoutParams(Ui.dp(this, 150), Ui.dp(this, 150)));
        TextView t = Ui.title(this, "+ ADICIONAR", 16, C.TEXT2);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-2, -2);
        tlp.topMargin = Ui.dp(this, 12);
        box.addView(t, tlp);
        box.setOnClickListener(v -> addProfile());
        return box;
    }

    private void choose(Profiles.P p) {
        Profiles.setCurrent(this, p.id);
        Catalog.invalidate();
        startActivity(new Intent(this, Catalog.creds(this) == null
                ? LoginActivity.class : ShellActivity.class));
        finish();
    }

    private void addProfile() {
        SearchKeyboard.show(this, "Nome do novo perfil", new SearchKeyboard.Listener() {
            @Override
            public void onSearch(final String text) {
                Dialogs.avatars(ProfilesActivity.this, text, idx -> {
                    Profiles.P np = Profiles.add(ProfilesActivity.this, text, idx);
                    if (np == null) {
                        Ui.toast(ProfilesActivity.this, "O limite é de 5 perfis.");
                    }
                    render();
                });
            }

            @Override
            public void onCancel() {
            }
        });
    }

    private void manageMenu(final Profiles.P p) {
        Dialogs.choose(this, p.name, new String[]{"Usar este perfil", "Mudar o nome",
                "Mudar o avatar", "Excluir perfil"}, i -> {
            if (i == 0) {
                choose(p);
            } else if (i == 1) {
                SearchKeyboard.show(this, "Novo nome", new SearchKeyboard.Listener() {
                    @Override
                    public void onSearch(String text) {
                        Profiles.update(ProfilesActivity.this, p.id, text, p.avatar);
                        render();
                    }

                    @Override
                    public void onCancel() {
                    }
                });
            } else if (i == 2) {
                Dialogs.avatars(this, p.name, idx -> {
                    Profiles.update(this, p.id, p.name, idx);
                    render();
                });
            } else {
                Dialogs.confirm(this, "Excluir " + p.name,
                        "Isso apaga a lista, os favoritos e os bloqueados deste perfil.",
                        "Excluir", () -> {
                            if (!Profiles.remove(this, p.id)) {
                                Ui.toast(this, "Precisa existir pelo menos um perfil.");
                            }
                            Catalog.invalidate();
                            render();
                        });
            }
        });
    }
}
