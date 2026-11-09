package com.asccine.tvbox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Player (ExoPlayer / media3). Controle remoto: OK pausa e continua; esquerda e direita pulam
 * 10 s (segurando: 30 s e depois 60 s); seta para baixo pula para o próximo episódio quando o
 * aviso aparece; tecla de menu abre áudio e legendas. Lembra onde parou, marca como visto
 * depois de 95%, vigia travamentos e tenta voltar sozinho.
 */
@OptIn(markerClass = UnstableApi.class)
public class PlayerActivity extends Activity {

    private static final class Params {
        String url;
        String title;
        String key;
        String kind;
        String id;
        String ext;
        String icon;
        String seriesKey;
        boolean fromStart;
    }

    private static Params pending;

    static void launch(Activity a, String url, String title, String key, String kind, String id,
                       String ext, String icon, String seriesKey) {
        launch(a, url, title, key, kind, id, ext, icon, seriesKey, false);
    }

    static void launch(Activity a, String url, String title, String key, String kind, String id,
                       String ext, String icon, String seriesKey, boolean fromStart) {
        Params p = new Params();
        p.url = url;
        p.title = title == null ? "" : title;
        p.key = key == null ? "" : key;
        p.kind = kind == null ? "movie" : kind;
        p.id = id == null ? "" : id;
        p.ext = ext == null ? "" : ext;
        p.icon = icon == null ? "" : icon;
        p.seriesKey = seriesKey == null ? "" : seriesKey;
        p.fromStart = fromStart;
        pending = p;
        a.startActivity(new Intent(a, PlayerActivity.class));
    }

    private static final long OVERLAY_MS = 4000L;
    private static final long TICK_MS = 500L;
    private static final long SAVE_EVERY_MS = 10000L;
    private static final long STALL_MS = 25000L;
    private static final int MAX_RETRIES = 4;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Params cur;
    private ExoPlayer player;
    private PlayerView playerView;
    private FrameLayout overlay;
    private TextView titleTv;
    private TextView timeLeft;
    private TextView timeRight;
    private TextView message;
    private TextView skipBtn;
    private ImageView pauseIcon;
    private Bar bar;
    private View topShade;
    private View bottomShade;
    private long overlayUntil;
    private long lastSave;
    private long lastSeekAt;
    private long bufferingSince;
    private long resumeAtMs = -1;
    private int retries;
    private boolean finished;
    private boolean ended;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        AppPrefs.applyWindow(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cur = pending;
        if (cur == null || cur.url == null) {
            finish();
            return;
        }
        buildUi();
    }

    // ---------- Montagem da tela ----------

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        playerView = new PlayerView(this);
        playerView.setUseController(false);
        playerView.setKeepScreenOn(true);
        playerView.setShutterBackgroundColor(Color.BLACK);
        playerView.setFocusable(false);
        root.addView(playerView, new FrameLayout.LayoutParams(-1, -1));

        overlay = new FrameLayout(this);
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        topShade = new View(this);
        topShade.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xCC000000, 0x00000000}));
        overlay.addView(topShade, new FrameLayout.LayoutParams(-1, Ui.dp(this, 140)));
        bottomShade = new View(this);
        bottomShade.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xDD000000, 0x00000000}));
        FrameLayout.LayoutParams bs = new FrameLayout.LayoutParams(-1, Ui.dp(this, 200));
        bs.gravity = Gravity.BOTTOM;
        overlay.addView(bottomShade, bs);

        titleTv = Ui.title(this, "", 26, C.TEXT);
        titleTv.setSingleLine(true);
        titleTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(-1, -2);
        tl.setMargins(Ui.dp(this, 48), Ui.dp(this, 32), Ui.dp(this, 48), 0);
        overlay.addView(titleTv, tl);

        pauseIcon = new ImageView(this);
        pauseIcon.setImageResource(R.drawable.ic_play);
        pauseIcon.setColorFilter(C.LIME);
        pauseIcon.setVisibility(View.GONE);
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(Ui.dp(this, 96),
                Ui.dp(this, 96));
        pl.gravity = Gravity.CENTER;
        overlay.addView(pauseIcon, pl);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams bl = new FrameLayout.LayoutParams(-1, -2);
        bl.gravity = Gravity.BOTTOM;
        bl.setMargins(Ui.dp(this, 48), 0, Ui.dp(this, 48), Ui.dp(this, 40));
        overlay.addView(bottom, bl);
        timeLeft = Ui.text(this, "0:00", 18, C.TEXT);
        bottom.addView(timeLeft);
        bar = new Bar(this);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(0, Ui.dp(this, 24), 1f);
        barLp.setMargins(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        bottom.addView(bar, barLp);
        timeRight = Ui.text(this, "0:00", 18, C.TEXT);
        bottom.addView(timeRight);

        TextView hint = Ui.text(this, "\u25C0 \u25B6 pular 10 s     OK pausa     MENU áudio e "
                + "legendas", 15, C.TEXT2);
        FrameLayout.LayoutParams hl = new FrameLayout.LayoutParams(-2, -2);
        hl.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        hl.bottomMargin = Ui.dp(this, 10);
        overlay.addView(hint, hl);

        message = Ui.title(this, "", 22, C.LIME);
        message.setBackground(Ui.shape(0xCC101010, Ui.dp(this, 14), 0, 0));
        message.setPadding(Ui.dp(this, 20), Ui.dp(this, 10), Ui.dp(this, 20), Ui.dp(this, 10));
        message.setVisibility(View.GONE);
        FrameLayout.LayoutParams ml = new FrameLayout.LayoutParams(-2, -2);
        ml.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        ml.topMargin = Ui.dp(this, 90);
        root.addView(message, ml);

        skipBtn = Ui.button(this, "Pular para o próximo episódio  \u2193", Ui.LIME);
        skipBtn.setVisibility(View.GONE);
        skipBtn.setFocusable(false);
        FrameLayout.LayoutParams sl = new FrameLayout.LayoutParams(-2, -2);
        sl.gravity = Gravity.BOTTOM | Gravity.END;
        sl.setMargins(0, 0, Ui.dp(this, 48), Ui.dp(this, 110));
        root.addView(skipBtn, sl);
        skipBtn.setOnClickListener(v -> playNextEpisode());

        setContentView(root);
    }

    // ---------- Ciclo de vida ----------

    @Override
    protected void onStart() {
        super.onStart();
        if (cur != null && player == null) {
            startPlayer();
        }
    }

    @Override
    protected void onStop() {
        saveProgress(true);
        releasePlayer();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releasePlayer();
        super.onDestroy();
    }

    private void startPlayer() {
        DefaultTrackSelector ts = new DefaultTrackSelector(this);
        ts.setParameters(ts.buildUponParameters().setMaxVideoSize(1920, 1080)
                .setMaxVideoFrameRate(60));
        DefaultLoadControl lc = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(15000, 40000, 2500, 5000)
                .setTargetBufferBytes(32 * 1024 * 1024)
                .build();
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setUserAgent("ASC-Cine/1.0 (Android)")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000);
        player = new ExoPlayer.Builder(this)
                .setTrackSelector(ts)
                .setLoadControl(lc)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(http))
                .build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    if (bufferingSince == 0) {
                        bufferingSince = SystemClock.elapsedRealtime();
                    }
                } else {
                    bufferingSince = 0;
                }
                if (state == Player.STATE_READY) {
                    retries = 0;
                    hideMessage();
                }
                if (state == Player.STATE_ENDED) {
                    onEnded();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                retryOrFail();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                pauseIcon.setVisibility(isPlaying ? View.GONE : View.VISIBLE);
                if (!isPlaying) {
                    showOverlay();
                }
            }
        });
        prepareCurrent();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    private void prepareCurrent() {
        titleTv.setText(cur.title);
        long start = resumeAtMs;
        if (start < 0 && !cur.fromStart) {
            PlaybackProgress.Mark m = PlaybackProgress.load(this, cur.key);
            if (m != null && m.isPartial()) {
                start = m.positionMs;
                showMessage("Continuar de " + fmt(start), 3000);
            }
        }
        resumeAtMs = -1;
        cur.fromStart = false;
        player.setMediaItem(MediaItem.fromUri(cur.url));
        if (start > 0) {
            player.seekTo(start);
        }
        player.prepare();
        player.setPlayWhenReady(true);
        ended = false;
        finished = false;
        showOverlay();
    }

    private void releasePlayer() {
        handler.removeCallbacks(ticker);
        if (player != null) {
            player.release();
            player = null;
        }
        if (playerView != null) {
            playerView.setPlayer(null);
        }
    }

    // ---------- Vigia de travamento e erros ----------

    private void retryOrFail() {
        if (player == null) {
            return;
        }
        if (retries < MAX_RETRIES) {
            retries++;
            showMessage("Reconectando... (" + retries + ")", 0);
            final long pos = Math.max(0, player.getCurrentPosition());
            handler.postDelayed(() -> {
                if (player != null) {
                    player.seekTo(pos);
                    player.prepare();
                    player.setPlayWhenReady(true);
                }
            }, 2000L);
        } else {
            saveProgress(true);
            Dialogs.confirm(this, "Não foi possível reproduzir",
                    "O vídeo não abriu. Confira a internet ou tente outro item.",
                    "Tentar de novo", () -> {
                        retries = 0;
                        resumeAtMs = -1;
                        if (player != null) {
                            prepareCurrent();
                        }
                    });
        }
    }

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (player == null) {
                return;
            }
            long now = SystemClock.elapsedRealtime();
            if (bufferingSince > 0 && now - bufferingSince > STALL_MS) {
                bufferingSince = 0;
                retryOrFail();
            }
            long pos = Math.max(0, player.getCurrentPosition());
            long dur = player.getDuration();
            boolean show = now < overlayUntil || !player.isPlaying();
            overlay.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show && dur > 0) {
                bar.set(pos, dur, player.getBufferedPosition());
                timeLeft.setText(fmt(pos));
                timeRight.setText(fmt(dur));
            }
            boolean canSkip = PlayQueue.hasNext() && "episode".equals(cur.kind) && dur > 0
                    && dur - pos <= 120000L && dur - pos > 0;
            skipBtn.setVisibility(canSkip ? View.VISIBLE : View.GONE);
            if (now - lastSave >= SAVE_EVERY_MS) {
                saveProgress(false);
                lastSave = now;
            }
            handler.postDelayed(this, TICK_MS);
        }
    };

    private void saveProgress(boolean force) {
        if (player == null || cur == null) {
            return;
        }
        long pos = Math.max(0, player.getCurrentPosition());
        long dur = player.getDuration();
        if (dur <= 0 || pos <= 0) {
            return;
        }
        PlaybackProgress.save(this, cur.key, pos, dur);
        boolean done = ended || (double) pos / (double) dur >= 0.95;
        if (done) {
            ContinueStore.remove(this, cur.url);
            return;
        }
        if (pos >= 5000L) {
            ContinueStore.save(this, new ContinueStore.Saved(cur.kind, cur.title, cur.icon,
                    cur.url, cur.id, cur.ext, cur.seriesKey, pos, dur));
        }
        if ("episode".equals(cur.kind)) {
            PlayQueue.remember(this);
        }
    }

    private void onEnded() {
        ended = true;
        saveProgress(true);
        if (PlayQueue.hasNext() && "episode".equals(cur.kind)) {
            playNextEpisode();
        } else {
            finish();
        }
    }

    private void playNextEpisode() {
        if (!PlayQueue.hasNext() || player == null) {
            return;
        }
        saveProgress(true);
        PlayQueue.index++;
        Catalog.Ep e = PlayQueue.eps.get(PlayQueue.index);
        String url = Catalog.urlOf(this, e);
        if (url == null) {
            return;
        }
        Params p = new Params();
        p.url = url;
        p.title = PlayQueue.titleFor(PlayQueue.index);
        p.key = Catalog.progressKey(this, true, e.id, url);
        p.kind = "episode";
        p.id = e.id;
        p.ext = e.ext;
        p.icon = cur.icon;
        p.seriesKey = PlayQueue.seriesKey;
        cur = p;
        PlayQueue.remember(this);
        retries = 0;
        prepareCurrent();
    }

    // ---------- Teclas ----------

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (player == null) {
            return super.dispatchKeyEvent(e);
        }
        int code = e.getKeyCode();
        boolean down = e.getAction() == KeyEvent.ACTION_DOWN;
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (down && e.getRepeatCount() == 0) {
                    togglePause();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (down) {
                    player.play();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (down) {
                    player.pause();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                if (down) {
                    seekBy(-1, e.getRepeatCount());
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if (down) {
                    seekBy(1, e.getRepeatCount());
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (down) {
                    showOverlay();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (down) {
                    if (skipBtn.getVisibility() == View.VISIBLE) {
                        playNextEpisode();
                    } else {
                        showOverlay();
                    }
                }
                return true;
            case KeyEvent.KEYCODE_MENU:
                if (down && e.getRepeatCount() == 0) {
                    openTrackMenu();
                }
                return true;
            default:
                return super.dispatchKeyEvent(e);
        }
    }

    private void togglePause() {
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
        showOverlay();
    }

    /** Pula 10 s; segurando a tecla vai a 30 s e depois a 60 s (no máximo 4 pulos por segundo). */
    private void seekBy(int dir, int repeat) {
        long now = SystemClock.elapsedRealtime();
        if (repeat > 0 && now - lastSeekAt < 250L) {
            return;
        }
        lastSeekAt = now;
        long step = repeat < 6 ? 10000L : (repeat < 20 ? 30000L : 60000L);
        long target = player.getCurrentPosition() + dir * step;
        long dur = player.getDuration();
        if (target < 0) {
            target = 0;
        }
        if (dur > 0 && target > dur - 1000L) {
            target = dur - 1000L;
        }
        player.seekTo(target);
        showOverlay();
        showMessage((dir < 0 ? "\u2212 " : "+ ") + (step / 1000) + " s", 800);
    }

    // ---------- Áudio e legendas ----------

    private void openTrackMenu() {
        Dialogs.choose(this, "Áudio e legendas", new String[]{"Áudio", "Legendas"}, i ->
                chooseTrack(i == 0 ? androidx.media3.common.C.TRACK_TYPE_AUDIO
                        : androidx.media3.common.C.TRACK_TYPE_TEXT));
    }

    private void chooseTrack(final int type) {
        if (player == null) {
            return;
        }
        final List<Tracks.Group> groups = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        if (type == androidx.media3.common.C.TRACK_TYPE_TEXT) {
            labels.add("Sem legenda");
        }
        for (Tracks.Group g : player.getCurrentTracks().getGroups()) {
            if (g.getType() == type && g.length > 0) {
                groups.add(g);
                androidx.media3.common.Format f = g.getTrackFormat(0);
                String name = f.label != null ? f.label
                        : (f.language != null ? f.language : "Faixa " + groups.size());
                labels.add(name + (g.isSelected() ? "  \u2713" : ""));
            }
        }
        if (groups.isEmpty()) {
            Ui.toast(this, type == androidx.media3.common.C.TRACK_TYPE_TEXT
                    ? "Este vídeo não tem legendas." : "Este vídeo tem só uma faixa de áudio.");
            return;
        }
        final boolean hasOff = type == androidx.media3.common.C.TRACK_TYPE_TEXT;
        Dialogs.list(this, type == androidx.media3.common.C.TRACK_TYPE_TEXT ? "Legendas"
                : "Áudio", labels.toArray(new String[0]), which -> {
            if (player == null) {
                return;
            }
            TrackSelectionParameters.Builder b = player.getTrackSelectionParameters()
                    .buildUpon();
            if (hasOff && which == 0) {
                b.setTrackTypeDisabled(type, true);
            } else {
                Tracks.Group g = groups.get(hasOff ? which - 1 : which);
                b.setTrackTypeDisabled(type, false);
                b.setOverrideForType(new TrackSelectionOverride(g.getMediaTrackGroup(), 0));
            }
            player.setTrackSelectionParameters(b.build());
        });
    }

    // ---------- Mensagens e visual ----------

    private void showOverlay() {
        overlayUntil = SystemClock.elapsedRealtime() + OVERLAY_MS;
        overlay.setVisibility(View.VISIBLE);
    }

    private void showMessage(String text, long ms) {
        message.setText(text);
        message.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideMsg);
        if (ms > 0) {
            handler.postDelayed(hideMsg, ms);
        }
    }

    private final Runnable hideMsg = new Runnable() {
        @Override
        public void run() {
            message.setVisibility(View.GONE);
        }
    };

    private void hideMessage() {
        handler.removeCallbacks(hideMsg);
        message.setVisibility(View.GONE);
    }

    private static String fmt(long ms) {
        long s = Math.max(0, ms) / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return h > 0 ? String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, sec)
                : String.format(java.util.Locale.US, "%d:%02d", m, sec);
    }

    /** Barra de progresso fina em limão (o buffer aparece em cinza). */
    private static final class Bar extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private long pos;
        private long dur;
        private long buf;

        Bar(Context c) {
            super(c);
        }

        void set(long p, long d, long b) {
            pos = p;
            dur = d;
            buf = b;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float h = getHeight();
            float w = getWidth();
            float t = h / 4f;
            float top = (h - t) / 2f;
            paint.setColor(0x55FFFFFF);
            rect.set(0, top, w, top + t);
            canvas.drawRoundRect(rect, t, t, paint);
            if (dur > 0) {
                paint.setColor(0x88FFFFFF);
                rect.set(0, top, w * Math.min(1f, (float) buf / dur), top + t);
                canvas.drawRoundRect(rect, t, t, paint);
                paint.setColor(C.LIME);
                float x = w * Math.min(1f, (float) pos / dur);
                rect.set(0, top, x, top + t);
                canvas.drawRoundRect(rect, t, t, paint);
                canvas.drawCircle(x, h / 2f, h / 3f, paint);
            }
        }
    }
}
