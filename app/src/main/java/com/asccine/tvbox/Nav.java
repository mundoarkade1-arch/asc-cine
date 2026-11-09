package com.asccine.tvbox;

import android.app.Activity;
import android.content.Intent;

/**
 * Passa o item escolhido para a tela de detalhe sem pôr endereços de vídeo (que podem ter
 * usuário e senha) dentro do Intent.
 */
final class Nav {

    static XtreamApi.Item item;
    static boolean autoplay;

    private Nav() {
    }

    static void openDetail(Activity a, XtreamApi.Item it, boolean play) {
        item = it;
        autoplay = play;
        a.startActivity(new Intent(a, DetailActivity.class));
    }
}
