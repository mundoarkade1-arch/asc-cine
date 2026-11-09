package com.asccine.tvbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Porta de entrada (sem tela própria): pede a senha do app se houver, depois decide entre
 * escolher perfil, entrar na lista ou ir direto para o início.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        AppPrefs.applyWindow(this);
        getWindow().getDecorView().setBackgroundColor(C.BG);
        if (AppLock.isEnabled(this)) {
            askLock();
        } else {
            route();
        }
    }

    private void askLock() {
        PinDialog.show(this, "Senha do app", "Digite a senha para abrir.",
                new PinDialog.Listener() {
                    @Override
                    public void onPin(PinDialog d, String pin) {
                        if (AppLock.check(MainActivity.this, pin)) {
                            d.dismiss();
                            route();
                        } else {
                            long wait = AppLock.lockedSeconds(MainActivity.this);
                            d.reset();
                            d.setMessage(wait > 0 ? "Muitos erros. Espere " + wait + " s."
                                    : "Senha incorreta.", true);
                        }
                    }

                    @Override
                    public void onCancel() {
                        finish();
                    }
                });
    }

    private void route() {
        if (Profiles.list(this).size() > 1) {
            startActivity(new Intent(this, ProfilesActivity.class).putExtra("pick", true));
        } else {
            startActivity(new Intent(this, Catalog.creds(this) == null
                    ? LoginActivity.class : ShellActivity.class));
        }
        finish();
    }
}
