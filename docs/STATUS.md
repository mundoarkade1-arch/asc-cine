# STATUS

**Etapa atual:** primeira versão completa escrita. **Ainda NÃO foi compilada** (o agente que a
escreveu não tinha o Android SDK). O primeiro build no GitHub Actions é o primeiro teste real.

## Pronto (no código)
- Perfis (até 5), login por servidor / M3U / código do aparelho, entrada direta se já há login.
- Início (banner com 5 destaques, Continue assistindo, Top 10, favoritos, faixas por categoria).
- Filmes, Séries, Explorar, Busca (teclado próprio), Favoritos, Bloquear, Ajustes.
- Detalhe de filme e de série (temporadas e episódios), Player (ExoPlayer).
- Controle parental, senha do app, atualização automática, aviso de vencimento.
- Workflow do GitHub, ícone, banner, documentos.

## Para conferir no aparelho (ninguém testou ainda)
Foco nos cards, rolagem a 60 fps no TX9, OK longo na grade, teclado de busca, pular de 10 s,
legendas/áudio, atualização automática, código do aparelho com o ASC Admin.

## Diferenças em relação ao pedido (ver DECISIONS.md)
- Fonte Poppins (no lugar de Space Grotesk/Outfit), senha do app só de 4 números,
  sem reaproveitamento de bitmaps (inBitmap).

## Lote de melhorias (sem teste no aparelho ainda)
Feito no código: cache da Início, banner automático, menu lateral, Top 10 maior, detalhe de série sem rolar, fluxo de perfis, 12 avatares, login único. Não compilado aqui (sem Android SDK): conferir o build no GitHub Actions e testar no TX9.

## Identidade visual cinema premium (vinho e dourado)
Feito: paleta vinho (`#2A0615` fundo, `#43101F` cards, `#8C1D40` destaque, `#C4486F` brilho, `#E3C27A` dourado só em detalhes), novo `bg_main.jpg`, ícone (`ic_launcher_*`, `mipmap-*`) e `banner.png`. Só cores, imagens e a estrela dourada mudaram: nenhuma função, tela, rota, API, login ou player foi alterada. Não compilado aqui (sem Android SDK): conferir o build no GitHub Actions e a leitura dos textos no TX9.

## Vidro + fundo global
Feito no código: fundo global, `Glass.java`, cards/botões/campos/menu/janelas/teclados em vidro. Não compilado aqui (sem Android SDK; só conferi a sintaxe): ver o build no GitHub Actions. Conferir no TX9: fluidez da rolagem, legibilidade sobre o fundo, se o logo da imagem atrapalha atrás do banner, e o foco em todas as telas.
