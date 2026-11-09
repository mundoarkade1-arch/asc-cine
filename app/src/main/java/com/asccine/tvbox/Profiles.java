package com.asccine.tvbox;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Perfis do aparelho (no máximo 5). Cada perfil tem a sua lista (login), favoritos,
 * bloqueados e "Continue assistindo": esses dados ficam em arquivos separados, cujo nome
 * termina com o id do perfil. Aqui só ficam nome, avatar e qual perfil está ativo.
 */
public final class Profiles {

    public static final int MAX = 5;
    private static final String PREFS = "asc_profiles";
    private static final String DEFAULT_ID = "p1";

    /** Cores dos avatares (todas derivadas da paleta preto, branco e limão). */
    private static final int[] AVATAR_BG = {
            0xFFC8FF00, 0xFFFFFFFF, 0xFFA8A8A8, 0xFFE4FF80, 0xFF6F8F00, 0xFF3A3A3A};
    private static final int[] AVATAR_FG = {
            0xFF101010, 0xFF101010, 0xFF101010, 0xFF101010, 0xFFFFFFFF, 0xFFC8FF00};

    public static final class P {
        public final String id;
        public final String name;
        public final int avatar;

        P(String id, String name, int avatar) {
            this.id = id;
            this.name = name;
            this.avatar = avatar;
        }
    }

    private static List<P> cache;
    private static String cacheCurrent;

    private Profiles() {
    }

    public static int avatarCount() {
        return AVATAR_BG.length;
    }

    public static int avatarBg(int i) {
        return AVATAR_BG[Math.abs(i) % AVATAR_BG.length];
    }

    public static int avatarFg(int i) {
        return AVATAR_FG[Math.abs(i) % AVATAR_FG.length];
    }

    /** Primeira letra do nome, em maiúscula. */
    public static String initial(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        return name.trim().substring(0, 1).toUpperCase();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized List<P> list(Context c) {
        if (cache != null) {
            return new ArrayList<>(cache);
        }
        List<P> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString("list", "[]"));
            for (int i = 0; i < a.length() && out.size() < MAX; i++) {
                JSONObject o = a.getJSONObject(i);
                String id = o.optString("id", "");
                if (id.matches("[a-z0-9]{1,12}")) {
                    out.add(new P(id, cutName(o.optString("name", "")), o.optInt("av", 0)));
                }
            }
        } catch (Exception ignored) {
            // lista estragada: recomeça com o perfil padrão
        }
        if (out.isEmpty()) {
            out.add(new P(DEFAULT_ID, "Visitante", 0));
            write(c, out);
        }
        cache = out;
        return new ArrayList<>(out);
    }

    public static synchronized String currentId(Context c) {
        List<P> all = list(c);
        String cur = cacheCurrent != null ? cacheCurrent : prefs(c).getString("current", "");
        for (P p : all) {
            if (p.id.equals(cur)) {
                cacheCurrent = cur;
                return cur;
            }
        }
        cacheCurrent = all.get(0).id;
        return cacheCurrent;
    }

    public static P current(Context c) {
        String id = currentId(c);
        for (P p : list(c)) {
            if (p.id.equals(id)) {
                return p;
            }
        }
        return list(c).get(0);
    }

    public static synchronized void setCurrent(Context c, String id) {
        cacheCurrent = id;
        prefs(c).edit().putString("current", id).apply();
        UserLists.reset();
    }

    /** Cria um perfil. Devolve null se já houver 5. */
    public static synchronized P add(Context c, String name, int avatar) {
        List<P> all = list(c);
        if (all.size() >= MAX) {
            return null;
        }
        String id = "p" + Long.toString(System.currentTimeMillis() % 100000000L, 36);
        P p = new P(id, cutName(name), avatar);
        all.add(p);
        write(c, all);
        return p;
    }

    public static synchronized void update(Context c, String id, String name, int avatar) {
        List<P> all = list(c);
        List<P> out = new ArrayList<>();
        for (P p : all) {
            out.add(p.id.equals(id) ? new P(id, cutName(name), avatar) : p);
        }
        write(c, out);
    }

    /** Apaga o perfil e os dados dele. Não apaga o último perfil. */
    public static synchronized boolean remove(Context c, String id) {
        List<P> all = list(c);
        if (all.size() <= 1) {
            return false;
        }
        List<P> out = new ArrayList<>();
        for (P p : all) {
            if (!p.id.equals(id)) {
                out.add(p);
            }
        }
        write(c, out);
        String[] files = {"asc_login_", "asc_continue_", "asc_lists_"};
        for (String f : files) {
            c.getApplicationContext().getSharedPreferences(f + id, Context.MODE_PRIVATE)
                    .edit().clear().apply();
        }
        if (id.equals(cacheCurrent)) {
            setCurrent(c, out.get(0).id);
        }
        return true;
    }

    private static void write(Context c, List<P> all) {
        try {
            JSONArray a = new JSONArray();
            for (P p : all) {
                JSONObject o = new JSONObject();
                o.put("id", p.id);
                o.put("name", p.name);
                o.put("av", p.avatar);
                a.put(o);
            }
            prefs(c).edit().putString("list", a.toString()).apply();
            cache = new ArrayList<>(all);
        } catch (Exception ignored) {
            // não acontece com texto simples
        }
    }

    private static String cutName(String s) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) {
            t = "Perfil";
        }
        return t.length() > 16 ? t.substring(0, 16) : t;
    }
}
