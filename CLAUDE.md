# CLAUDE.md

Orientações para o Claude Code (claude.ai/code) neste repositório.

## O que é este projeto

Porte do InkShelf (leitor de quadrinhos/livros digitais) do Android para **Windows desktop**,
com Compose Multiplatform. O app mobile original fica em
`C:\Users\jpedr\AndroidStudioProjects\InkShelf` e é a fonte de referência do porte — não é
dependência nem submódulo, é outro repositório.

Decisões de escopo já tomadas:
- repositórios separados (este não compartilha código com o Android);
- **sem sincronização** com o app do celular — a biblioteca do desktop é independente;
- **EPUB fica para depois do MVP** (no mobile ele depende de WebView, que não existe aqui).

## Comandos

```powershell
./gradlew :core:test          # testes do módulo de dados
./gradlew :ui:test            # testes de ViewModel
./gradlew build               # compila tudo + roda testes
./gradlew :app:run            # abre o app
./gradlew :app:packageMsi     # gera o instalador Windows
```

Sempre rodar `./gradlew build` após alterar código, antes de reportar conclusão.

## Arquitetura

Três módulos Gradle, com a fronteira do Compose no meio:

| Módulo  | Conteúdo | Depende de |
|---------|----------|------------|
| `:core` | Kotlin/JVM puro — banco, scanner, extractors. **Sem Compose.** | — |
| `:ui`   | Tema, componentes, telas, viewmodels, navegação | `:core` |
| `:app`  | `main()`, janela, grafo Koin, empacotamento nativo | `:ui` |

Manter `:core` livre de Compose é proposital: é essa fronteira que permitiria, no futuro,
transformá-lo num módulo KMP compartilhado com o Android.

## Estado do porte

- **Fase 0 — concluída.** Esqueleto dos módulos, tema (8 presets, claro/escuro, tipografia),
  `model/`, empacotamento MSI/EXE.
- **Fase 1 — concluída.** Banco (Room), scanner do sistema de arquivos, `SyncEngine`,
  preferências e estatísticas.
- **Fase 2 — concluída.** `PageExtractor` (CBZ/CBR/RAR5/PDF), `ReaderCacheStore`,
  `CoverExtractor`, `ComicInfoExtractor`, `LibraryContentRepository` e a fachada
  `LibraryRepository`. O `:core` está fechado: 61 testes.
- **Fase 3 — concluída no essencial.** Grafo Koin, barra lateral de seções e as telas de
  Biblioteca (grade + trilha de navegação), Favoritos e Continuar Lendo. Mais o seletor de pasta
  (`JFileChooser`) e a geração de capas em segundo plano (`CoverGenerator`). Fica para depois a
  tela de **Início** (o painel com trilhos de sugestões — 1.764 linhas no mobile), que é
  conveniência: a barra lateral já dá acesso a tudo.
- **Fase 4 — concluída.** Leitor de imagem: `LeitorViewModel` + `LeitorScreen`, com navegação
  por roda do mouse e teclado, zoom (Ctrl+roda), arrasto, tela cheia (F11), retomada da posição
  e marcação automática de lido. 11 testes de integração.
- **Fases 5–7** — telas restantes (Início, Configurações, Estatísticas, Histórico, Busca),
  acabamento desktop, Microsoft Store.

## Convenções e diferenças em relação ao mobile

**IDs.** No Android, o ID de pasta/arquivo era o document ID do SAF em Base64. Aqui é o
**caminho canônico** (`File.canonicalPath`) — canônico, não absoluto, para que a mesma pasta
alcançada por dois caminhos não duplique a biblioteca. A raiz escolhida pelo usuário não vira
linha no banco: seus filhos usam o `parentId` literal `"root"`, o que permite várias raízes numa
biblioteca só.

**Sem abstração de arquivo.** O porte não introduziu um wrapper tipo `InkFile` sobre
`java.io.File`: sem SAF e sem código compartilhado com o Android, ele custaria em todo call site
sem comprar nada.

**Banco.** O mobile está na versão 14 com 13 migrações; aqui o banco **nasce na v1 com o schema
final**, porque não existe instalação anterior para migrar. A primeira migração real será v1→v2.
`fallbackToDestructiveMigration()` **não** é usado (no mobile ele está ativo e apaga a biblioteca
em silêncio se uma migração falhar).

**Transações.** `db.withTransaction { }` é do `room-ktx`, que só existe para Android. O
equivalente aqui é `db.inkTransaction { }` (`data/db/Transactions.kt`).

**Diretórios.** `%LOCALAPPDATA%\InkShelf\data` (banco) e `...\cache` (páginas e capas
extraídas), resolvidos por `InkPaths`. Nos testes, `InkPaths.useRootForTesting()`.

**Preferências.** As chaves e defaults ficam em `LibrarySettingsKeys`, não no `companion object`
da fachada como no mobile.

**Leitura de arquivos.** O mobile copiava o `.cbz` inteiro para o cache antes de abrir, porque um
`Uri` do SAF não permite acesso aleatório. Aqui o arquivo é lido direto de onde está — não existe
mais cache de arquivo compactado, só de páginas extraídas. O nome do diretório de cache é um
hash SHA-256 do `fileId`, já que o `fileId` agora é um caminho (contém `\` e `:`) e ainda
esbarraria no limite de 260 caracteres do Windows.

**Capas.** No mobile o `CoverExtractor` tinha 512 linhas e reimplementava detecção de formato,
leitura de ZIP/RAR/RAR5/PDF e ordenação natural — tudo duplicado do `PageExtractor`. Aqui ele
pede a primeira página ao `PageExtractor` e reduz. Ao mexer nele, não reintroduzir essa
duplicação.

**PDF.** `PdfRenderer` (Android) → PDFBox. `Loader.loadPDF(File)` + `PDFRenderer.renderImage`.
O `PDDocument` **não é thread-safe**: a renderização é serializada por `synchronized(session.lock)`.

**Navegação.** As seções de topo ficam numa barra lateral (`NavigationRail`), não na barra
inferior do mobile: a janela do desktop é larga e baixa, e gastar altura com navegação tira
espaço de onde as capas aparecem.

**Grades.** `GridCells.Adaptive`, não `Fixed`. A preferência de "colunas" herdada do mobile vale
como *largura mínima do card*; o número real de colunas acompanha a largura da janela.

**Capas.** A varredura não gera capas — quem faz isso é o `CoverGenerator`, chamado depois dela,
em quatro corrotinas paralelas. Gerar durante a varredura deixaria a tela vazia por minutos numa
biblioteca grande.

**Descartado do mobile:** os 5 widgets Glance, `AppIconManager`, WorkManager e as notificações
de leitura (incluindo `ReadingRoutinePreferences`, cujas chamadas foram removidas do repositório
de estatísticas).

## Cuidados

- O `SyncEngine` é o ponto mais perigoso do código: uma varredura que sobrescreva em vez de
  mesclar apaga favoritos, progresso e metadados do usuário em silêncio. `SyncEngineTest` existe
  exatamente para isso — se mexer no diff, os testes têm que continuar verdes.
- `InkShelfDatabase.getInstance` memoriza a instância por nome de banco, mas o caminho em disco
  vem de `InkPaths`. Em teste, chamar `InkShelfDatabase.closeAll()` **antes** de
  `InkPaths.useRootForTesting()` — senão o teste recebe o banco aberto pelo teste anterior,
  apontando para outro diretório temporário.
- O `ComicInfo.xml` vem de dentro de arquivos de origem desconhecida. O parser desliga DOCTYPE e
  entidades externas (XXE); não reativar por conveniência.
- Testes que exercitam ViewModel + banco + extração são **de integração**: usam espera real com
  prazo (`awaitUntil`), não `runTest` com tempo virtual. O ViewModel extrai em `Dispatchers.IO`
  e o Room consulta fora do dispatcher de teste, então `advanceUntilIdle()` retorna antes de o
  trabalho terminar e o teste passa ou falha por sorte. Ver `LeitorViewModelTest`.
- No leitor, `Key.Home` **não** é a tecla Home do teclado (é a tecla "home" do sistema Android e
  está depreciada). A do teclado é `Key.MoveHome`.
- O app mobile usa `catch (_: Exception)` mudo em todo lugar. Não replicar: ao portar um trecho
  desses, ou tratar o erro ou registrar por quê é seguro engolir.
