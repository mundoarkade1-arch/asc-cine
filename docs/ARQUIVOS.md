# ARQUIVOS — mapa e regras técnicas

## Raiz
`settings.gradle`, `build.gradle`, `gradle.properties`, `.gitignore`, `README.md`, `AGENTS.md`,
`.github/workflows/apk.yml` (compila, assina, cria `version.json`, publica a Release `vN`).

## app/src/main (Java, pacote com.asccine.tvbox)
| Arquivo | Para quê |
|---|---|
| MainActivity, ProfilesActivity, LoginActivity | entrada, perfis, login |
| ShellActivity, Sidebar, Screen | casca com barra lateral e telas |
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
