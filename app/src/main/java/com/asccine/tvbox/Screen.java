package com.asccine.tvbox;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Uma tela do menu lateral (Início, Filmes, Séries, Busca...). Montada em código, leve. */
abstract class Screen {

    final ShellActivity act;
    private final List<Bg.Handle> tasks = new ArrayList<>();

    Screen(ShellActivity act) {
        this.act = act;
    }

    /** A view da tela (criada uma vez). */
    abstract View view();

    /** A tela foi colocada na janela: começar a carregar. */
    void onShow() {
    }

    /** A pessoa voltou para esta tela (por exemplo, depois de assistir algo). */
    void onResume() {
    }

    /** A tela saiu: cancelar tudo e soltar memória. */
    void onHide() {
        for (Bg.Handle h : tasks) {
            h.cancel();
        }
        tasks.clear();
    }

    /** Onde começa o foco quando a pessoa entra no conteúdo. */
    View firstFocus() {
        return null;
    }

    boolean focusContent() {
        View v = firstFocus();
        if (v != null && v.getVisibility() == View.VISIBLE) {
            return v.requestFocus();
        }
        return false;
    }

    <T> void bg(Bg.Work<T> work, Bg.Done<T> done) {
        tasks.add(Bg.run(act, work, done));
    }

    <T> Bg.Handle bgH(Bg.WorkH<T> work, Bg.Done<T> done) {
        Bg.Handle h = Bg.runH(act, work, done);
        tasks.add(h);
        return h;
    }

    /** Mensagem de erro em português simples. */
    static String errorText(Exception e) {
        return "Não foi possível carregar agora. Confira a internet e tente de novo.";
    }
}
