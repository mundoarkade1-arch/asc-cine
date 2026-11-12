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

## Reidentificação visual (violeta)
Feito: a cor de destaque deixou de ser verde-limão e passou a ser o violeta vibrante `#B56BFF`. Só cores mudaram: nenhuma função, tela, rota, API, login ou player foi alterada. Não compilado aqui (sem Android SDK): conferir o build no GitHub Actions.
Pendente (decisão do dono): ícone do app (`ic_launcher_foreground.xml`, `mipmap-*`) e `banner.png` seguem na cor antiga por serem logotipo/imagem.
