# ARQUIVOS — mapa e regras técnicas

## Raiz
`settings.gradle`, `build.gradle`, `gradle.properties`, `.gitignore`, `README.md`, `AGENTS.md`,
`.github/workflows/apk.yml` (compila, assina, cria `version.json`, publica a Release `vN`).

## app/src/main (Java, pacote com.asccine.tvbox)
| Arquivo | Para quê |
|---|---|
| MainActivity, ProfilesActivity, LoginActivity | entrada, perfis, login |
| ShellActivity, Sidebar, Screen | casca com barra lateral e telas |
| Glass | vidro escuro: fundo global, cards, painéis e menus (configuração no topo do arquivo) |
| HomeScreen, HeroView | início e banner |
| GridScreen, TvGrid, CardAdapter, Card | grade de cartões grandes |
| ExploreScreen, SearchScreen, FavoritesScreen, SettingsScreen | demais telas |
| DetailActivity, PlayerActivity, PlayQueue | detalhe, player, fila de episódios |
| Catalog | ponte única para a lista (Xtream/M3U) |
| XtreamApi, M3uApi, SearchIndex | acesso à lista e busca |
| CredentialStore, ContinueStore, UserLists, Profiles, AppLock, ParentalStore, Secure | dados e segurança |
| PlaybackProgress, SeriesProgress | onde parou |
| DeviceLink, LoginChecker, Updater, UpdateUi | código do aparelho, teste de login, atualização |
| Ui, C, Bg, Nav, Dialogs, OutlineNumber, ImageLoader, PinDialog, SearchKeyboard, AppPrefs | utilidades visuais |

## Res
`values/` (strings, colors, styles), `drawable/` (ícones vetoriais, `bg_button`), `drawable-xhdpi/banner.png`,
`mipmap-*` e `mipmap-anydpi-v26` (ícone), `font/` (Poppins), `anim/` (fade), `xml/file_paths.xml`.

## Regras técnicas
- Ids de pacote e de FileProvider: `com.asccine.tvbox` / `.fileprovider`.
- `Updater.REPOSITORY` = `mundoarkade1-arch/asc-cine`. O workflow publica em `v<VERSION_CODE>`.
- `Nav` guarda o item aberto em memória (para o endereço do vídeo não ir no Intent).

## Novos neste lote
- HomeCache.java: guarda em arquivo os itens mais novos (validade 6 h).
- res/drawable/avatar_01.xml a avatar_12.xml: os 12 avatares em vetor.

## Fundo e vidro
- `res/drawable-nodpi/bg_main.jpg`: imagem de fundo de todo o app. Para trocar, substitua o arquivo (mesmo nome).
- `Glass.java`: constantes no topo (`BACKGROUND_OVERLAY`, `BLUR_OVERLAY`, `BLUR`, `FOCUS_X/Y`) ajustam escuridão, desfoque e corte da imagem.
- `AppPrefs.applyWindow` coloca o fundo na janela; por isso as telas NÃO têm mais cor de fundo própria.
- Card novo: use `Glass.drawable` (parado), `Glass.focusBg` (vidro com foco), `Glass.focusOnly` (transparente até ter foco) ou `Glass.selectBg` (listas/grades). Nunca `C.SURFACE` direto.
- `res/drawable/bg_button.xml` não é mais usado (as teclas usam `Glass.selectBg`); pode ser apagado.
