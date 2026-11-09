# DECISIONS — decisões e motivos

1. **Pacote `com.asccine.tvbox`**, para não conflitar com o app antigo (`com.ascplay.tvbox`).
2. **Telas montadas em código Java** (sem XML de layout). Motivo: o projeto não pôde ser
   compilado pelo agente; sem XML há menos ids cruzados para errar. Também é leve.
3. **Reaproveitado do app antigo** (só lógica, não o visual): `XtreamApi`, `M3uApi`, `SearchIndex`,
   `ImageLoader`, `LoginChecker`, `Updater`, `DeviceLink`, `CredentialStore`, `ContinueStore`,
   `ParentalStore`, `PlaybackProgress`, `SeriesProgress`, `PinDialog`, `SearchKeyboard`.
   Mudanças: pacote, user-agent, chave do Keystore (`asc_cine_key`), dados por perfil, nota e data
   de adição nos itens, detalhes estruturados (ano, duração, gênero, fundo), imagem em dois tamanhos.
4. **Serviço do código do aparelho:** o mesmo Supabase do app antigo (o ASC Admin continua igual).
   A chave secreta do aparelho agora é guardada **criptografada** (`Secure`); antes ficava em texto.
5. **Perfis:** cada perfil tem arquivos próprios (`asc_login_<id>`, `asc_continue_<id>`,
   `asc_lists_<id>`). Progresso de reprodução e controle parental valem para o aparelho todo.
6. **Favoritos e bloqueados** são criptografados (itens de M3U carregam o endereço do vídeo).
   O item é reconhecido por tipo + nome.
7. **Top 10 de hoje:** o servidor não tem ranking. Regra: entre os 200 itens mais recentes
   (campo `added`), os 10 de maior nota (`rating`). Em lista M3U (sem nota/data): os 10 primeiros
   da primeira categoria. Os 5 destaques do banner vêm dos Top 10, filmes e séries intercalados.
8. **Fonte:** Poppins Bold e Medium (SIL OFL) em `res/font`, por ser geométrica e estar disponível
   sem internet. Para trocar por Space Grotesk ou Outfit, substitua os dois `.ttf` mantendo os nomes.
9. **Senha do app:** 4 números (o `PinDialog` herdado é de 4 dígitos). Pedido original: 4 a 6.
10. **Cartão em foco:** escala 1,06 (120 ms), contorno limão de 3dp e "cantinho" branco deslocado.
    Sem blur. Na grade o cartão fica "selecionado" e a `TvGrid` acende só quando a grade tem o foco.
11. **Imagens:** cache LRU de 16 MB, capas em até 480 px, banner em até 1280 px, JPEG em RGB_565.
    Reuso de bitmaps (`inBitmap`) NÃO foi feito.
12. **Lint desligado no release** (`abortOnError false`): o media3 tem APIs "instáveis" que o lint
    trata como erro; o código já usa `@OptIn`.
13. **Sem minificação (R8)** no release, por segurança de compilação; pode ser ligada depois.
14. **Atualização:** só HTTPS, `version.json` em `releases/latest/download`, URL do APK precisa
    começar com o prefixo do repositório, SHA-256 conferido antes de instalar, instalação por FileProvider.
