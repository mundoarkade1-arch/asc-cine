package com.asccine.tvbox;

/** Cores do app (as mesmas de res/values/colors.xml, para usar direto no código). */
final class C {
    /** Fundo das telas (vinho muito escuro). */
    static final int BG = 0xFF2A0615;
    /** Cards e menus. */
    static final int SURFACE = 0xFF43101F;
    static final int SURFACE_FOCUS = 0xFF52192B;
    static final int TEXT = 0xFFFFFFFF;
    /** Texto secundário. */
    static final int TEXT2 = 0xFFD9C9CF;
    /** Dicas e textos apagados. */
    static final int TEXT3 = 0xFF9C8790;
    /** Borda sutil: branco com 8% de opacidade. */
    static final int LINE = 0x14FFFFFF;
    /** Destaque (botões, item selecionado, barra de progresso): vinho. */
    static final int ACCENT = 0xFF8C1D40;
    /** Destaque com 40% de opacidade (bordas discretas). */
    static final int ACCENT_40 = 0x66D4B06A;
    /** Dourado fosco (champanhe): contorno de foco, textos e ícones de destaque; contrasta com o vinho. */
    static final int GLOW = 0xFFD4B06A;
    /** Dourado das estrelas e do ícone: só detalhes pequenos (estrelas, títulos pequenos, ícones premium). */
    static final int GOLD = 0xFFE3C27A;
    /** Texto/ícone sobre elementos vinho de destaque. */
    static final int ON_ACCENT = 0xFFFFFFFF;
    static final int ERROR = 0xFFE5736F;
    static final int SHADE = 0xCC2A0615;

    private C() {
    }
}
