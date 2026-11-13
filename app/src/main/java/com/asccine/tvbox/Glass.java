package com.asccine.tvbox;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.View;
import android.app.Activity;

/**
 * Identidade "vidro escuro" do ASC Cine: fundo global + cards, painéis e menus de vidro.
 *
 * COMO FUNCIONA (leve para TV Box fraca):
 * - O fundo é UMA imagem (res/drawable-nodpi/bg_main.jpg) desenhada como fundo da janela, com
 *   uma camada escura por cima. Nenhuma tela precisa de fundo próprio.
 * - O desfoque NÃO é feito em tempo real. A imagem é desfocada uma única vez, num bitmap
 *   minúsculo (96 px de largura). Cada card de vidro desenha o pedaço desse bitmap que está
 *   atrás dele, e por cima: tinta escura, véu claro, reflexo no topo e borda translúcida.
 *
 * PARA TROCAR O FUNDO: substitua o arquivo bg_main.jpg (mesmo nome). Nada mais muda.
 * PARA AJUSTAR: mexa só nas constantes da seção CONFIGURAÇÃO abaixo.
 */
final class Glass {

    /** Vidro leve: botões, campos, itens pequenos. */
    static final int LIGHT = 0;
    /** Vidro intermediário: cards principais, categorias, menu lateral. */
    static final int MID = 1;
    /** Vidro intenso: janelas, painéis importantes, menu lateral aberto. */
    static final int INTENSE = 2;

    // ---------- CONFIGURAÇÃO ----------

    /** Camada de vinho sobre o fundo (o primeiro par de dígitos é a opacidade: 66 = 40%). */
    static final int BACKGROUND_OVERLAY = 0x662A0615;
    /** Camada de vinho aplicada à imagem desfocada que aparece dentro dos cards. */
    static final int BLUR_OVERLAY = 0x802A0615;
    /** false = cards só translúcidos, sem a imagem desfocada (aparelho muito fraco). */
    static final boolean BLUR = true;
    /** Parte da imagem que fica visível quando ela é cortada (0,5 = centro). */
    static final float FOCUS_X = 0.5f;
    static final float FOCUS_Y = 0.5f;

    /** Tinta escura de cada nível (opacidade em hexadecimal no primeiro par). */
    private static final int[] TINT = {0xB343101F, 0xCC43101F, 0xE643101F};
    /** Opacidade do véu claro de cada nível. */
    private static final int[] VEIL = {0x04, 0x06, 0x08};
    /** Opacidade do reflexo no topo de cada nível. */
    private static final int[] SHINE = {0x0C, 0x10, 0x14};
    /** Opacidade da borda: em cima e embaixo (mais clara em cima, como reflexo). */
    private static final int[] EDGE_TOP = {0x1A, 0x1F, 0x26};
    private static final int[] EDGE_BOTTOM = {0x0F, 0x14, 0x1A};

    private static final int BLUR_WIDTH = 96;
    private static final int BLUR_PASSES = 2;
    private static final int BLUR_RADIUS = 3;

    // ---------- Imagens (carregadas uma vez, fora da thread principal) ----------

    private static volatile Bitmap background;
    private static volatile Bitmap blurred;
    private static volatile boolean started;
    private static volatile boolean failed;

    private Glass() {
    }

    /** Começa a carregar o fundo (uma vez só, em segundo plano). */
    static void preload(Context c) {
        synchronized (Glass.class) {
            if (started) {
                return;
            }
            started = true;
        }
        final Context app = c.getApplicationContext();
        Thread t = new Thread(() -> load(app), "glass-load");
        t.setPriority(Thread.MIN_PRIORITY);
        t.setDaemon(true);
        t.start();
    }

    private static void load(Context c) {
        try {
            Resources res = c.getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(res, R.drawable.bg_main, o);
            int sample = 1;
            while (o.outWidth / (sample * 2) >= dm.widthPixels) {
                sample *= 2;
            }
            BitmapFactory.Options d = new BitmapFactory.Options();
            d.inSampleSize = sample;
            Bitmap bg = BitmapFactory.decodeResource(res, R.drawable.bg_main, d);
            if (bg == null) {
                failed = true;
                return;
            }
            if (BLUR) {
                blurred = makeBlur(bg);
            }
            background = bg;
        } catch (Exception | OutOfMemoryError e) {
            failed = true;
        }
    }

    /** Versão minúscula e bem desfocada da imagem, já com a camada escura aplicada. */
    private static Bitmap makeBlur(Bitmap src) {
        int w = BLUR_WIDTH;
        int h = Math.max(1, Math.round(w * (float) src.getHeight() / src.getWidth()));
        Bitmap cur = src;
        while (cur.getWidth() >= w * 2) {
            Bitmap next = Bitmap.createScaledBitmap(cur, cur.getWidth() / 2,
                    cur.getHeight() / 2, true);
            if (cur != src) {
                cur.recycle();
            }
            cur = next;
        }
        Bitmap small = Bitmap.createScaledBitmap(cur, w, h, true);
        if (cur != src && cur != small) {
            cur.recycle();
        }
        int[] px = new int[w * h];
        small.getPixels(px, 0, w, 0, 0, w, h);
        if (small != src) {
            small.recycle();
        }
        int[] tmp = new int[px.length];
        for (int i = 0; i < BLUR_PASSES; i++) {
            box(px, tmp, w, h, true);
            box(tmp, px, w, h, false);
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(px, 0, w, 0, 0, w, h);
        new Canvas(out).drawColor(BLUR_OVERLAY);
        return out;
    }

    private static void box(int[] in, int[] out, int w, int h, boolean horizontal) {
        int n = BLUR_RADIUS * 2 + 1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = 0;
                int g = 0;
                int b = 0;
                for (int k = -BLUR_RADIUS; k <= BLUR_RADIUS; k++) {
                    int xx = horizontal ? Math.min(w - 1, Math.max(0, x + k)) : x;
                    int yy = horizontal ? y : Math.min(h - 1, Math.max(0, y + k));
                    int p = in[yy * w + xx];
                    r += (p >> 16) & 255;
                    g += (p >> 8) & 255;
                    b += p & 255;
                }
                out[y * w + x] = 0xFF000000 | ((r / n) << 16) | ((g / n) << 8) | (b / n);
            }
        }
    }

    /** Ajusta a imagem para cobrir w x h sem distorcer (corta o excesso). */
    private static void cover(Matrix m, int bw, int bh, int w, int h) {
        float s = Math.max(w / (float) bw, h / (float) bh);
        m.setScale(s, s);
        m.postTranslate((w - bw * s) * FOCUS_X, (h - bh * s) * FOCUS_Y);
    }

    // ---------- Fundo global ----------

    /** Coloca o fundo global na janela da tela. Chamado por AppPrefs.applyWindow. */
    static void applyBackdrop(Activity a) {
        preload(a);
        a.getWindow().setBackgroundDrawable(new Backdrop());
    }

    /** Fundo global para janelas próprias (teclados, diálogos de tela cheia). */
    static Drawable backdrop(Context c) {
        preload(c);
        return new Backdrop();
    }

    /** Imagem de fundo + camada escura. Se ainda não carregou, mostra o preto de sempre. */
    private static final class Backdrop extends Drawable {
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Matrix m = new Matrix();
        private final Runnable retry = this::invalidateSelf;

        @Override
        public void draw(Canvas canvas) {
            Bitmap bg = background;
            Rect b = getBounds();
            if (bg == null) {
                canvas.drawColor(C.BG);
                if (!failed) {
                    unscheduleSelf(retry);
                    scheduleSelf(retry, SystemClock.uptimeMillis() + 150);
                }
                return;
            }
            cover(m, bg.getWidth(), bg.getHeight(), b.width(), b.height());
            canvas.drawBitmap(bg, m, paint);
            canvas.drawColor(BACKGROUND_OVERLAY);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }

    // ---------- Cards e painéis ----------

    /** Vidro parado (sem estados). */
    static Drawable drawable(Context c, int level, float radiusDp) {
        return new GlassDrawable(level, Ui.dp(c, radiusDp), null, true, false, Ui.dp(c, 1), 0);
    }

    /** Vidro com borda de brilho discreta (destaques como aviso de vencimento e código). */
    static Drawable tinted(Context c, int level, float radiusDp, float strokeDp) {
        return new GlassDrawable(level, Ui.dp(c, radiusDp), null, true, false,
                Ui.dp(c, strokeDp), C.ACCENT_40);
    }

    /** Só moldura de vidro (borda e reflexo), sem preencher: vai por cima de capas. */
    static Drawable frame(Context c, float radiusDp) {
        return new GlassDrawable(LIGHT, Ui.dp(c, radiusDp), null, false, false, Ui.dp(c, 1), 0);
    }

    /** Menu lateral: cantos arredondados só do lado direito. */
    static Drawable sidebar(Context c, boolean open) {
        float r = Ui.dp(c, 20);
        float[] radii = {0, 0, r, r, r, r, 0, 0};
        return new GlassDrawable(open ? INTENSE : MID, r, radii, true, false, Ui.dp(c, 1), 0);
    }

    /** Em repouso é vidro; com foco ganha contorno de brilho e fica mais firme. */
    static StateListDrawable focusBg(Context c, int level, float radiusDp) {
        StateListDrawable s = new StateListDrawable();
        Drawable on = focusedGlass(c, level, radiusDp);
        s.addState(new int[]{android.R.attr.state_focused}, on);
        s.addState(new int[]{android.R.attr.state_pressed}, on);
        s.addState(new int[]{}, drawable(c, level, radiusDp));
        return s;
    }

    /** Em repouso é transparente; com foco vira vidro com contorno de brilho. */
    static StateListDrawable focusOnly(Context c, int level, float radiusDp) {
        StateListDrawable s = new StateListDrawable();
        Drawable on = focusedGlass(c, level, radiusDp);
        s.addState(new int[]{android.R.attr.state_focused}, on);
        s.addState(new int[]{android.R.attr.state_pressed}, on);
        s.addState(new int[]{}, new ColorDrawable(0x00000000));
        return s;
    }

    /** Como focusBg, mas também acende quando o item está "selecionado" (listas e grades). */
    static StateListDrawable selectBg(Context c, int level, float radiusDp) {
        StateListDrawable s = new StateListDrawable();
        Drawable on = focusedGlass(c, level, radiusDp);
        s.addState(new int[]{android.R.attr.state_selected}, on);
        s.addState(new int[]{android.R.attr.state_focused}, on);
        s.addState(new int[]{android.R.attr.state_pressed}, on);
        s.addState(new int[]{}, drawable(c, level, radiusDp));
        return s;
    }

    private static Drawable focusedGlass(Context c, int level, float radiusDp) {
        return new GlassDrawable(Math.max(level, MID), Ui.dp(c, radiusDp), null, true, true,
                Ui.dp(c, 3), 0);
    }

    // ---------- O desenho do vidro ----------

    private static final class GlassDrawable extends Drawable {
        private final int level;
        private final float radius;
        private final float[] radii;
        private final boolean fill;
        private final boolean focused;
        private final int borderPx;
        private final int borderColor;

        private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint tintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint veilPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint hazePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final RectF edgeRect = new RectF();
        private final Path path = new Path();
        private final Path edgePath = new Path();
        private final Matrix m = new Matrix();
        private final int[] loc = new int[2];
        private final Runnable retry = this::invalidateSelf;
        private BitmapShader shader;
        private Bitmap shaderBitmap;

        GlassDrawable(int level, float radius, float[] radii, boolean fill, boolean focused,
                      int borderPx, int borderColor) {
            this.level = level;
            this.radius = radius;
            this.radii = radii;
            this.fill = fill;
            this.focused = focused;
            this.borderPx = borderPx;
            this.borderColor = borderColor;
            tintPaint.setColor(TINT[level]);
            veilPaint.setColor((VEIL[level] << 24) | 0x00FFFFFF);
            hazePaint.setColor((C.GLOW & 0x00FFFFFF) | 0x1F000000);
            edgePaint.setStyle(Paint.Style.STROKE);
            edgePaint.setStrokeWidth(borderPx);
            if (focused) {
                edgePaint.setColor(C.GLOW);
            } else if (borderColor != 0) {
                edgePaint.setColor(borderColor);
            }
        }

        @Override
        protected void onBoundsChange(Rect b) {
            rect.set(b);
            float half = borderPx / 2f;
            edgeRect.set(b.left + half, b.top + half, b.right - half, b.bottom - half);
            if (radii != null) {
                path.reset();
                path.addRoundRect(rect, radii, Path.Direction.CW);
                edgePath.reset();
                edgePath.addRoundRect(edgeRect, radii, Path.Direction.CW);
            }
            int shine = (fill ? SHINE[level] : SHINE[LIGHT] - 4) << 24;
            shinePaint.setShader(new LinearGradient(0, b.top, 0, b.top + b.height() * 0.45f,
                    shine | 0x00FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            if (!focused && borderColor == 0) {
                edgePaint.setShader(new LinearGradient(0, b.top, 0, b.bottom,
                        (EDGE_TOP[level] << 24) | 0x00FFFFFF,
                        (EDGE_BOTTOM[level] << 24) | 0x00FFFFFF, Shader.TileMode.CLAMP));
            }
        }

        /** A View que usa este vidro (sobe pela cadeia de StateListDrawable até achar). */
        private View host() {
            Callback cb = getCallback();
            int guard = 0;
            while (cb instanceof Drawable && guard++ < 4) {
                cb = ((Drawable) cb).getCallback();
            }
            return cb instanceof View ? (View) cb : null;
        }

        private void shape(Canvas canvas, Paint p) {
            if (radii == null) {
                canvas.drawRoundRect(rect, radius, radius, p);
            } else {
                canvas.drawPath(path, p);
            }
        }

        @Override
        public void draw(Canvas canvas) {
            if (rect.isEmpty()) {
                return;
            }
            if (fill) {
                drawBase(canvas);
                shape(canvas, tintPaint);
                shape(canvas, veilPaint);
                if (focused) {
                    shape(canvas, hazePaint);
                }
            }
            shape(canvas, shinePaint);
            if (radii == null) {
                float r = Math.max(0f, radius - borderPx / 2f);
                canvas.drawRoundRect(edgeRect, r, r, edgePaint);
            } else {
                canvas.drawPath(edgePath, edgePaint);
            }
        }

        /** A imagem desfocada, alinhada com o fundo (o card parece mesmo "transparente"). */
        private void drawBase(Canvas canvas) {
            Bitmap blur = BLUR ? blurred : null;
            View host = host();
            if (blur == null || host == null) {
                basePaint.setShader(null);
                basePaint.setColor(0xE643101F);
                shape(canvas, basePaint);
                if (BLUR && !failed && blur == null) {
                    unscheduleSelf(retry);
                    scheduleSelf(retry, SystemClock.uptimeMillis() + 150);
                }
                return;
            }
            if (shaderBitmap != blur) {
                shader = new BitmapShader(blur, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                shaderBitmap = blur;
            }
            basePaint.setShader(shader);
            DisplayMetrics dm = host.getResources().getDisplayMetrics();
            host.getLocationOnScreen(loc);
            cover(m, blur.getWidth(), blur.getHeight(), dm.widthPixels, dm.heightPixels);
            m.postTranslate(-loc[0], -loc[1]);
            shader.setLocalMatrix(m);
            basePaint.setColor(0xFFFFFFFF);
            shape(canvas, basePaint);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
