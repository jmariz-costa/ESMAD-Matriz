# Verificação da Matriz

Registo das verificações do MVP (Ficha PL 03). Distingue:

- **testes automatizados** — correm com `cd backend && ./mvnw test` e
  `cd frontend && npm test` (ver [README.md](../README.md));
- **verificações manuais** — feitas por uma pessoa na aplicação a correr, com
  o Backend e a base de dados reais.

Secções: [casos de verificação 01 a 09](#casos-de-verificação-do-mvp-i7-exercício-8)
(I7), [testes automatizados obrigatórios](#testes-automatizados-obrigatórios),
[comandos executados](#comandos-executados-e-resultados),
[percurso da interface](#percurso-manual-no-firefox-i6-exercício-7) (I6),
[registo de problemas](#registo-de-problemas),
[limitações](#limitações-e-o-que-não-foi-executado) e
[utilização da IA](#utilização-da-ia-i8-exercício-9) (I8).

## Casos de verificação do MVP (I7, Exercício 8)

Executados a 2026-11-12. Na coluna "Evidência", os testes automatizados são
indicados como `Classe.método` (Backend em `backend/src/test/java/pt/esmad/matriz/`)
ou pelo ficheiro e nome do teste (Frontend em `frontend/src/`); os passos P01 a
P11 são os do [percurso da interface](#percurso-manual-no-firefox-i6-exercício-7).

| Caso | Cenário | Resultado esperado | Resultado observado | Evidência |
|------|---------|--------------------|---------------------|-----------|
| 01 | Acesso à aplicação | Credenciais válidas entram; inválidas recebem sempre a mesma mensagem; pedidos sem sessão recebem 401. | ✅ Login com credenciais válidas cria a sessão; palavra-passe errada, email inexistente e conta desativada dão a mesma resposta; 5 falhas bloqueiam; sem cookie ou com token inventado, 401 `NAO_AUTENTICADO` em todos os recursos. | `LoginTest.credenciaisCorretasIniciamSessao`, `.palavraPasseErradaERecusada`, `.emailInexistenteTemAMesmaResposta`, `.cincoFalhasBloqueiamMesmoComAPalavraPasseCerta`; `SessaoApiTest.semCookieDevolve401`, `.tokenInventadoDevolve401`, `.sessaoExpiraAoFimDeTrintaMinutosSemAtividade`; `AutorizacaoApiTest.semSessaoTudoDevolve401`; P01. |
| 02 | Gestão de projetos | Criar, pesquisar, editar e eliminar funciona; editar e eliminar só pelo Gestor. | ✅ Quem cria fica Gestor (o Gestor enviado pelo cliente é ignorado); pesquisa sem distinguir maiúsculas, com paginação e só nos projetos de que é membro; outro membro recebe 403 ao editar ou eliminar; eliminar apaga em cascata. | `ProjetosApiTest.naoAceitaOGestorEnviadoPeloCliente`, `.outroUtilizadorNaoConsultaOProjeto`; `ListarProjetosApiTest.pesquisaPeloNomeSemDistinguirMaiusculas`, `.paginacao`, `.soMostraOsProjetosDeQueSouMembro`; `EditarEliminarProjetoApiTest.soOGestorEdita`, `.soOGestorElimina`, `.gestorEliminaEmCascataSemDeixarOrfaos`; P02, P03. |
| 03 | Persistência de tarefas | A tarefa aparece sem recarregar e mantém-se depois de reiniciar o Backend. | ✅ Depois de criar, o cartão aparece em "A fazer" sem recarregar. Criada pela API, a tarefa "Persistência I7" continuou disponível depois de parar e voltar a arrancar o Backend, com os mesmos dados e na coluna "A fazer" (ver [comandos](#caso-03-persistência-depois-de-reiniciar-o-backend)). | `DialogosTarefa.test.tsx` "cria a tarefa e ela aparece em "A fazer" sem recarregar a página"; `TarefasApiTest.membroConsultaATarefaComOsResponsaveisOrdenadosPeloNome`; verificação executada pelo agente com `curl` e reinício do Backend; P04. |
| 04 | Validação de dados | Campos obrigatórios vazios ou só com espaços e valores inválidos são recusados com 400 e o campo indicado. | ✅ Título em falta, só com espaços ou fora de 3–150; descrição com mais de 2000; prazo e prioridade inválidos; estado na criação; versão em falta; comentário vazio ou só com espaços; login sem campos. No Frontend, o título só com espaços é recusado antes de enviar. | `TarefasApiTest.recusaTituloEmFaltaSoComEspacosOuForaDosLimites`, `.recusaPrazoPrioridadeECamposInvalidos`; `EditarEliminarTarefaApiTest.recusaPedidosInvalidosSemAlterarATarefa`; `ComentariosApiTest.recusaComentarioVazioSoComEspacosOuComMaisDe2000Caracteres`; `ProjetosApiTest.validaONomeEADescricao`; `LoginTest.camposEmFaltaDevolvemValidacao`; `TarefaTest`, `ComentarioTest`; P04, P05. |
| 05 | Estado e prioridade | Mudar o estado ou a prioridade atualiza a tarefa, os filtros e as contagens de forma coerente. | ✅ Qualquer estado passa a qualquer outro, com o histórico; a tarefa passa a aparecer no filtro do novo estado e as contagens do projeto mudam; os filtros de estado e prioridade combinam-se, incluindo "Sem prioridade"; uma versão antiga é recusada com 409. | `EditarEliminarTarefaApiTest.qualquerEstadoPassaAQualquerOutroEOHistoricoRegistaOAnteriorEONovo`, `.versaoAntigaERecusadaENadaMuda`; `TarefasApiTest.filtrosDeEstadoEPrioridadeCombinam`; `QuadroKanban.test.tsx` "arrastar um cartão para outra coluna muda o estado e atualiza o quadro e as contagens"; P04, P05, P06. |
| 06 | Responsáveis | Associar e remover funciona; a mesma associação não fica duplicada. | ✅ Vários responsáveis por tarefa; repetir dá 409 `JA_EXISTE` e fica uma só linha na base de dados; remover tira só essa atribuição; só membros ativos do projeto (400); tudo continua lá ao reabrir. | `ResponsaveisApiTest.qualquerMembroAtribuiVariosResponsaveisQueFicamOrdenadosPeloNome`, `.aMesmaPessoaNaoFicaDuasVezes`, `.removerTiraSoEssaAtribuicao`, `.soMembrosDoProjetoComContaAtiva`, `.asAtribuicoesAparecemAoReabrirATarefaENaListagem`; `ResponsaveisComentarios.test.tsx`; P05, P08. |
| 07 | Comentários | O texto, o autor da sessão e a data ficam guardados e visíveis. | ✅ O autor é o da sessão e a data a do servidor; um pedido com `autor` ou `criadoEm` é recusado; listados do mais antigo para o mais recente, com o autor e a data na interface; continuam lá ao reabrir. | `ComentariosApiTest.comentarioFicaComOAutorDaSessaoEAData`, `.oPedidoNaoPodeDefinirOAutorNemAData`, `.listaDoMaisAntigoParaOMaisRecenteComPaginacao`, `.osComentariosContinuamDisponiveisAoReabrirATarefa`; `ResponsaveisComentarios.test.tsx`; P05. |
| 08 | Autorização na API | Outro utilizador não acede nem altera recursos sem permissão, mesmo mudando os identificadores no pedido. | ✅ Um membro do projeto A, com os identificadores do projeto B, recebe 403 nas 15 operações sobre o projeto, os membros, as tarefas, os responsáveis e os comentários do B, e o B fica igual; o B não aparece nas suas listagens; misturar identificadores de projetos diferentes é recusado (400 ou 404); o Administrador recebe 403. Não foi encontrado nenhum problema. | `AutorizacaoApiTest.aAnaNaoAcedeNemAlteraNadaDoProjetoBComOsIdentificadoresDele`, `.oProjetoBNaoApareceNasListagensDaAna`, `.misturarIdentificadoresDeProjetosDiferentesERecusado`, `.oAdministradorNaoAcedeAosProjetosNemAsTarefas`; P07. |
| 09 | Eliminação e erros | A eliminação respeita as dependências; erros da API não mostram mensagens de sucesso nem perdem os dados do formulário. | ✅ Eliminar uma tarefa apaga as atribuições, os comentários e o histórico e deixa o registo em `tarefa_eliminada`; eliminar um projeto apaga tudo o que contém; só o Gestor elimina. Na interface, um erro ao criar, guardar ou eliminar mostra o erro, mantém o que foi escrito e não mostra nenhuma mensagem de sucesso. | `EditarEliminarTarefaApiTest.gestorEliminaATarefaComTudoOQueELaContemEFicaORegisto`, `.soOGestorElimina`; `EditarEliminarProjetoApiTest.gestorEliminaEmCascataSemDeixarOrfaos`; `RevisaoInterface.test.tsx` "um erro ao criar, guardar ou eliminar não mostra mensagem de sucesso e mantém o formulário"; `DialogosTarefa.test.tsx` "se a API devolver erro, o formulário mantém os dados escritos"; P03, P07, P09. |

## Testes automatizados obrigatórios

Os três tipos pedidos no Exercício 8 (todos em `backend/src/test/java/pt/esmad/matriz/`):

| Tipo | Teste | O que confirma |
|------|-------|----------------|
| Regra de validação | `tarefas/TarefasApiTest.recusaTituloEmFaltaSoComEspacosOuForaDosLimites` | Título em falta, só com espaços, com 2 ou com 151 caracteres dá 400 `VALIDACAO` no campo `titulo`; 150 caracteres com espaços nas pontas é aceite. |
| Cenário de autorização | `AutorizacaoApiTest.aAnaNaoAcedeNemAlteraNadaDoProjetoBComOsIdentificadoresDele` | 15 operações com os identificadores de um projeto alheio dão 403, e a base de dados confirma que nada mudou. |
| Criação com consulta posterior | `tarefas/ResponsaveisApiTest.asAtribuicoesAparecemAoReabrirATarefaENaListagem` e `comentarios/ComentariosApiTest.osComentariosContinuamDisponiveisAoReabrirATarefa` | O que foi criado volta a aparecer quando a tarefa é consultada num pedido separado (e na listagem). `TarefasApiTest.criacaoFicaNoHistoricoComQuemTemASessaoComoAutor` consulta diretamente a base de dados depois de criar. |

## Comandos executados e resultados

**Testes automatizados** (2026-11-12):

| Comando | Resultado |
|---------|-----------|
| `cd backend && ./mvnw test` | 143 testes, 0 falhas, 0 erros (cria um PostgreSQL temporário com o Docker); 147 depois da revisão do I8 |
| `cd frontend && npm test` | 85 testes em 13 ficheiros, todos passaram; 88 em 14 ficheiros depois da refatoração do I8 |
| `cd frontend && npx tsc -b` | Sem erros |
| `cd frontend && npm run lint` | Sem avisos |
| `cd frontend && npm run build` | Build concluído |

### Caso 03: persistência depois de reiniciar o Backend

Executado pelo agente, com `docker compose up -d` e `./mvnw quarkus:dev`, a
pedir diretamente ao Backend (porta 8081) com a sessão da Ana:

```text
$ POST /api/projetos/{website}/tarefas {"titulo":"Persistência I7","prioridade":"ALTA","prazo":"2026-12-01"}
{"id":"7e442951-…","titulo":"Persistência I7","prazo":"2026-12-01","prioridade":"ALTA","estado":"A_FAZER","responsaveis":[],"versao":0}  → 201

# Backend parado: porta 8081 livre; um pedido sem resposta (curl: código 000)
# Backend arrancado de novo

$ GET /api/tarefas/7e442951-… (depois de reiniciar)
{"id":"7e442951-…","titulo":"Persistência I7","prazo":"2026-12-01","prioridade":"ALTA","estado":"A_FAZER","responsaveis":[],"versao":0}  → 200
$ GET /api/projetos/{website}/tarefas?estado=A_FAZER
  total 3 ['Definir mapa do site', 'Persistência I7', 'Escolher fotografias']
```

A sessão também se manteve, porque está guardada na base de dados (D06). A
tarefa foi eliminada no fim, com o respetivo registo de eliminação.

### Arranque a partir de uma cópia limpa (I8)

Executado pelo agente a 2026-11-12, numa cópia nova do repositório (com as
alterações de documentação do I8 ainda por gravar aplicadas), seguindo só o
README:

| Passo do README | Resultado |
|-----------------|-----------|
| `cp backend/.env.example backend/.env`; `cd frontend && npm install` | A cópia não trazia `backend/.env` nem `node_modules`; instalação sem vulnerabilidades |
| `docker compose up -d` | Contentor e volume próprios da cópia; ligação à base de dados UP em `/q/health/ready` |
| `cd backend && ./mvnw quarkus:dev` | O Flyway aplicou `V1 esquema inicial`; o seed criou 5 utilizadores (4 Membros e o Administrador), 2 projetos (8 e 2 tarefas), 10 tarefas e 1 comentário |
| Contas | Login da Ana com `MATRIZ_DEMO_PASSWORD` e do Administrador com `MATRIZ_ADMIN_PASSWORD` do exemplo: 200 |
| `cd frontend && npm run dev` | No Firefox (Playwright): painel com 2 projetos, quadro com 4 colunas e 8 cartões, tarefa criada e comentada, ainda presente depois de recarregar a página |
| `./mvnw test`, `npm test`, `npx tsc -b && npm run lint && npm run build` | Backend 147 testes e Frontend 88, sem falhas; tipos, lint e build sem erros |

O `docker-compose.yml` usa um nome de contentor fixo (`matriz-postgres`) e a
porta 5432, por isso só pode correr uma base de dados da Matriz de cada vez.
Para o teste, o contentor de desenvolvimento foi renomeado temporariamente
(com aprovação); no fim, o contentor e o volume da cópia foram apagados e os
dados de desenvolvimento confirmados intactos.


## Percurso manual no Firefox (I6, Exercício 7)

**Objetivo:** confirmar, em todos os ecrãs, os estados de carregamento, lista
vazia, sucesso e erro; que os dados se mantêm quando uma submissão falha; que
não há submissões repetidas; que listagens, filtros e contagens se atualizam
sem recarregar a página; e que o layout se adapta a larguras menores.

**Preparação:** `docker compose up -d`; `cd backend && ./mvnw quarkus:dev`;
`cd frontend && npm run dev`; abrir http://localhost:5173 no Firefox. Contas
de demonstração com a palavra-passe `MATRIZ_DEMO_PASSWORD` do `backend/.env`:
ana@empresa.pt (Gestora de "Website institucional") e bruno@empresa.pt
(membro desse projeto).

**Executado em:** 2026-11-12, Firefox 141.0 sem janela (Playwright), pelo agente
de IA, a pedido do responsável do projeto, contra a aplicação a correr
(Backend em modo de desenvolvimento, PostgreSQL em Docker, dados de
demonstração). É uma verificação automatizada pelo agente, e não manual: o
script ficou fora do repositório e as capturas de ecrã do P11 foram vistas uma
a uma. Os dados criados no percurso foram eliminados no fim. Logo a seguir, o
responsável do projeto repetiu os testes de forma manual no browser.

| # | Ecrã | Passos | Resultado esperado | Observado |
|---|------|--------|--------------------|-----------|
| P01 | Início de sessão | Carregar em "Entrar" com os campos vazios; depois, email da Ana e palavra-passe errada; depois, a palavra-passe certa. | Mensagens junto aos campos, sem pedido; a seguir, a mensagem genérica, com o email mantido e a palavra-passe limpa; "Entrar" fica inativo durante o pedido; por fim, abre o painel. | ✅ Sem pedido com os campos vazios; "Email ou palavra-passe incorretos.", email mantido, palavra-passe limpa, "A entrar…" inativo; abre "Olá, Ana Exemplo". |
| P02 | Painel inicial | Ver o painel; criar o projeto "Teste I6". | "Olá, Ana Exemplo", os projetos com percentagem e contagens por estado; depois de criar, aparece "Projeto criado." e o novo projeto. | ✅ 3 projetos com percentagem e contagens; "Projeto criado." e "Teste I6" no painel. |
| P03 | Projetos | Pesquisar "web"; pesquisar "zzz"; editar o nome de "Teste I6"; eliminar "Teste I6". | Só "Website institucional"; "Nenhum projeto encontrado."; "Alterações guardadas."; confirmação a explicar o que é apagado, "Projeto eliminado." e o total atualizado. | ✅ "1 projeto encontrado"; "Nenhum projeto encontrado."; "Alterações guardadas."; confirmação "Esta ação é definitiva. Serão apagadas também a tarefa do projeto, com os seus responsáveis, comentários e histórico."; "Projeto eliminado." e total a 0. |
| P04 | Detalhe do projeto | Abrir "Website institucional"; filtrar Prioridade "Alta" e Estado "A fazer"; "Limpar filtros"; "+ Nova tarefa" com título só com espaços e depois "Teste I6"; arrastar o cartão para "Em curso". | 4 colunas com os totais; os filtros combinam-se e o estado mostra só essa coluna; o título só com espaços é recusado sem perder o resto; "Tarefa criada." e o cartão em "A fazer"; ao arrastar, a mensagem de mudança de estado e a percentagem e as contagens atualizadas, sem recarregar. | ✅ Colunas pela ordem; com os dois filtros, só "A fazer" com "Definir mapa do site" (filtros no endereço); "O título é obrigatório." com a descrição mantida; "Tarefa criada."; ao arrastar, "«Teste I6» passou para Em curso." e a contagem "Em curso" de 2 para 3. |
| P05 | Detalhe da tarefa | Abrir "Teste I6": mudar o estado e a prioridade e guardar; atribuir o Bruno e a Carla e remover um; publicar um comentário e um só com espaços; fechar e reabrir. | "Alterações guardadas." e o cartão na coluna certa; os responsáveis mudam logo, também no cartão; o comentário aparece com o autor e a data; o só com espaços é recusado; ao reabrir, tudo continua lá. | ✅ "Alterações guardadas." e o cartão em "Em revisão"; Bruno e Carla atribuídos, Carla removida, o cartão mostra o Bruno; comentário com "Ana Exemplo 12/11/2026, 19:38"; "O comentário não pode estar vazio."; ao reabrir, o responsável e o comentário continuam lá. |
| P06 | Detalhe da tarefa (conflito) | Abrir a mesma tarefa em duas abas; guardar uma alteração numa e depois noutra. | Na segunda, o aviso de versão, "Guardar" inativo e "Recarregar tarefa", que mantém o que foi escrito. | ✅ Aviso de versão e "Guardar" inativo; "Recarregar tarefa" trouxe o título da 1.ª aba e manteve a descrição escrita na 2.ª; ao guardar, ficaram as duas alterações. |
| P07 | Detalhe da tarefa (eliminar) | Como Bruno, abrir a tarefa; como Ana, eliminar "Teste I6". | O Bruno não tem "Eliminar"; a Ana confirma, vê "Tarefa eliminada." e o cartão sai do quadro. | ✅ O Bruno tem "Guardar" mas não "Eliminar"; a Ana confirma, "Tarefa eliminada." e o cartão sai do quadro. |
| P08 | Membros | No projeto "Teste I6" (criado no P02, para não mexer nas atribuições dos dados de demonstração): adicionar o Bruno, atribuí-lo a uma tarefa, removê-lo do projeto e voltar a adicioná-lo. | Confirmação antes de remover; "Bruno Exemplo foi removido do projeto." e o cartão passa a "Sem responsável", sem recarregar a página. | ✅ Confirmação "Deixa de ver o projeto e de ser responsável pelas tarefas não concluídas."; a mensagem de remoção e o cartão a "Sem responsável" sem recarregar. Executado antes do P03, que elimina o projeto. |
| P09 | Erros de ligação | Com um ecrã aberto, parar o Backend e recarregar; voltar a arrancá-lo e carregar em "Tentar novamente". | Mensagem de erro com "Tentar novamente"; depois, os dados voltam. | ✅ "Não foi possível carregar os projetos."; com "Tentar novamente", a lista volta. A falta de ligação foi simulada cortando os pedidos `/api` no browser, em vez de parar o Backend. |
| P10 | Submissões repetidas | Em "+ Nova tarefa", carregar duas vezes seguidas em "Criar". | Só é criada uma tarefa. | ✅ Duplo clique: 1 pedido de criação e 1 cartão. |
| P11 | Larguras menores | No modo de design adaptável do Firefox (Ctrl+Shift+M): 800 px e 500 px de largura, no painel, nos projetos, no quadro e no detalhe da tarefa. | A 800 px, o quadro com 2 colunas; a 500 px, 1 coluna, a tabela de projetos com scroll horizontal, o cabeçalho em duas linhas e os diálogos dentro do ecrã. | ✅ A 800 px, 2 colunas; a 500 px, 1 coluna, tabela com scroll horizontal, cabeçalho em duas linhas, diálogo com 484 px dentro do ecrã, sem scroll horizontal na página. Depois da correção do problema 1. |

**Problemas encontrados:** a tabela de projetos desalinhada a 500 px (P11);
ver [B03 no registo de problemas](#registo-de-problemas).

**Notas**

- O campo de data mostra o formato do idioma do browser (no Firefox usado, em
  inglês, MM/DD/AAAA); num Firefox em português aparece DD/MM/AAAA. O valor
  enviado à API é sempre AAAA-MM-DD.

## Registo de problemas

Cada problema foi reproduzido, investigado até à causa e corrigido de forma
limitada, com um teste de regressão sempre que é possível automatizar.

| # | Problema | Reprodução e evidência | Causa | Correção | Teste de regressão |
|---|----------|------------------------|-------|----------|--------------------|
| B01 | Remover um membro não atualizava o quadro (I6, F2). | Atribuir o Bruno a uma tarefa por concluir e removê-lo do projeto: o cartão continuava a mostrá-lo até se recarregar a página, embora o Backend já o tivesse retirado (I5). | A secção de membros e o quadro carregavam os dados de forma independente: remover um membro só recarregava a lista de membros. | `MembrosProjeto` avisa o detalhe do projeto (`aoAlterarTarefas`), que faz o quadro voltar a carregar (`sinal` em `QuadroKanban` e `useQuadro`). | `RevisaoInterface.test.tsx` "remover um membro atualiza o quadro, que deixa de o mostrar como responsável (F2)"; falha sem a correção. |
| B02 | Uma tarefa eliminada por outra pessoa continuava no quadro (I6, F3). | Com o quadro aberto, outra pessoa elimina a tarefa; ao abrir o cartão aparecia "Esta tarefa já não existe.", mas o cartão ficava no quadro. | O 404 só era mostrado no diálogo; o quadro não era avisado. | O detalhe da tarefa avisa o quadro quando recebe 404 ao abrir ou ao guardar. | `RevisaoInterface.test.tsx` "abrir uma tarefa que outra pessoa eliminou tira-a do quadro (F3)"; falha sem a correção. |
| B03 | Tabela de projetos desalinhada a 500 px (I6, P11). | Captura de ecrã do P11 a 500 px: as linhas da coluna "Ações" não acompanhavam a altura das outras células quando o nome e o Gestor passavam a duas linhas. | `display: flex` na própria célula (`.tabela .acoes`, desde o I2), que deixa de se comportar como célula da tabela. | A célula volta a ser uma célula normal, sem quebra de linha, com margem entre os botões. | Sem teste automatizado (o jsdom não aplica CSS); o P11 foi repetido e a captura confirmou o alinhamento. |

No I7 não foram encontrados problemas novos: o teste do caso 08 e o teste de
erros do caso 09 passaram à primeira.

## Limitações e o que não foi executado

- **O percurso da interface (P01–P11) foi executado pelo agente de IA**, com o
  Firefox 141 do Playwright sem janela. Logo a seguir, o responsável do
  projeto repetiu os testes de forma manual no browser; os resultados
  registados na tabela são os da execução do agente.
- **No P09, a falta de ligação ao Backend foi simulada** cortando os pedidos
  `/api` no browser, e não parando o Backend.
- **O caso 03 (reinício do Backend) não tem teste automatizado**: foi feito
  com `curl` e o reinício do Backend, com o resultado registado acima.
- **Erros inesperados do servidor (500)** não vêm no formato único de erro e
  o contrato não os define (revisão do I8, R3). O Frontend mostra "Ocorreu um
  erro inesperado. Tente novamente." quando a resposta não tem esse formato.
  Fica como limitação conhecida, para não alterar o contrato na entrega.
- **Layout adaptativo** sem teste automatizado (o jsdom não aplica CSS);
  coberto pelo P11.
- **Não executado**: testes noutros browsers além do Firefox (fora do âmbito,
  RNF02); testes de carga e de desempenho; a aplicação servida em produção
  (com HTTPS e `MATRIZ_COOKIE_SEGURO=true`), que fica para a preparação da
  entrega (I8).

## Utilização da IA (I8, Exercício 9)

Três exemplos da Ficha PL 03, em que a IA (Claude Code) propôs e o
programador decidiu. Em cada incremento, a IA apresentou primeiro o
mapeamento "o que a ficha pede → contrato" e as decisões em aberto, com uma
opção recomendada; o programador escolheu antes de qualquer alteração.

### 1. Mudar o estado arrastando o cartão (I4)

- **Objetivo do pedido:** implementar no quadro Kanban a mudança de estado
  por arrastar (US06, nota 3 do wireframe), mantendo uma alternativa por
  teclado.
- **Sugestão da IA:** duas opções — (a) o arrastar nativo do browser (HTML5),
  sem dependências novas, recomendada; (b) a biblioteca `@dnd-kit`, mais
  polida, mas uma dependência nova.
- **Decisão do programador:** (a), arrastar nativo, com o seletor de estado no
  detalhe da tarefa como alternativa por teclado.
- **Verificação:** testes de componente em `QuadroKanban.test.tsx` (arrastar
  envia `PATCH` com a versão e o novo estado, atualiza o quadro e as
  contagens; largar na mesma coluna não faz pedidos; 409 de versão mostra o
  aviso); arrastar real no Firefox no passo P04 do percurso; teste no browser
  pelo programador.

### 2. Percurso da interface no Firefox (I6)

- **Objetivo do pedido:** fazer o percurso completo da interface no Firefox
  (Exercício 7) e registá-lo neste documento.
- **Sugestão da IA:** o plano previa um percurso manual feito pelo
  programador. Quando este perguntou se a IA o podia fazer, a IA propôs três
  opções — (a) Playwright com o Firefox, fora do repositório, recomendada;
  (b) Chrome com a extensão Claude in Chrome; (c) manual — e avisou que, se
  fosse ela a fazê-lo, ficaria registado como verificação do agente, e não
  como manual.
- **Decisão do programador:** (a), aprovando a instalação do Playwright e do
  seu Firefox fora do repositório (`AGENTS.md`, secção 6). Logo a seguir à
  execução do agente, o programador **repetiu os testes de forma manual** no
  browser.
- **Verificação:** os 11 passos (P01–P11) passaram; as capturas de ecrã das
  larguras menores foram vistas uma a uma e revelaram a tabela de projetos
  desalinhada a 500 px (B03), que foi corrigida e confirmada com nova
  captura. O registo indica que a execução foi do agente e que o programador
  a repetiu manualmente.

### 3. Sugestão corrigida: o autor de um comentário (I5)

- **Objetivo do pedido:** garantir que o autor e a data de um comentário são
  definidos pelo Backend (US12 crit. 1; Exercício 6).
- **Sugestão inicial e correção:** o plano, escrito com a IA no Exercício 1,
  previa testar que "o autor do comentário é o da sessão mesmo que o pedido
  traga outro" — ou seja, ignorar o `autor` enviado. Ao implementar, a IA
  confirmou no contrato que o corpo não aceita campos desconhecidos
  (`additionalProperties: false`) e corrigiu a abordagem: um pedido com
  `autor` ou `criadoEm` é **recusado com 400**, e o autor é sempre o da
  sessão. A diferença ficou registada nos desvios do I5.
- **Decisão do programador:** aceitou a correção (seguir o contrato) ao rever
  o fecho do I5.
- **Verificação:** `ComentariosApiTest.oPedidoNaoPodeDefinirOAutorNemAData` e
  `AutorizacaoApiTest.misturarIdentificadoresDeProjetosDiferentesERecusado`;
  verificação com `curl` contra o Backend real (pedido com `autor` → 400).
- **Outra sugestão corrigida:** a primeira versão do detalhe da tarefa, escrita
  pela IA, fazia o pedido à API durante a renderização do componente; a
  própria IA detetou o erro antes de correr os testes e passou a usar o
  `useDados`, como nos outros ecrãs.
