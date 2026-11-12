# Plano de testes — tarefas e permissões

Plano da Ficha PL 04 (Quality Assurance) para a funcionalidade **tarefas**
(US05, US06, US07, US09) e para as **permissões** de acesso às tarefas
(RN09, RN19, RN20). Diz que cenários são testados, com que entradas, que
resultado se espera e em que nível, e liga cada cenário aos testes que o
verificam.

Secções: [como foi feito](#como-foi-feito) ·
[pressupostos](#pressupostos-verificados) ·
[matriz de cenários](#matriz-de-cenários) ·
[testes novos](#testes-novos-da-ficha-pl-04) ·
[fora do âmbito](#fora-do-âmbito) · [resultados](#resultados)

## Como foi feito

1. **Fontes dos resultados esperados:**
   - [user-stories.md](user-stories.md): US05 a US07, US09 e os casos CE4, CE5 e CE8;
   - [requirements.md](requirements.md): RN09 a RN14, RN19, RN20 e RN23;
   - o contrato [openapi.yaml](../openapi.yaml), com as regras gerais nas linhas 16–55 e as tarefas a partir da linha 417.

   O resultado esperado vem destes documentos, **e não do código**.
2. **Pedido à IA:** foi usado o prompt de apoio da ficha (Ficha 04, l. 88–102), limitado a "uma matriz concisa de cenários para tarefas e respetivas permissões, com entradas, resultado esperado e nível de teste". O agente (Claude Code) propôs a matriz.
3. **Risco conhecido:** o mesmo agente escreveu a implementação e a maior parte dos testes na Ficha PL 03. Um teste escrito a partir do código pode repetir o mesmo erro de interpretação (PDF 12, "Questão para discussão"). Por isso, cada resultado esperado foi conferido com os requisitos e com o contrato. Onde os requisitos não chegam para decidir, o pressuposto fica registado na secção seguinte.
4. **Revisão humana:** o utilizador revê a matriz, os pressupostos e a escolha dos cenários essenciais antes de serem escritos os testes novos.

**Níveis:**
- **U**: unitário, sem base de dados (JUnit no Backend, Vitest + Testing Library no Frontend);
- **I**: integração, com a API e o PostgreSQL reais, isto é, RestAssured e Dev Services, que criam uma base descartável por execução;
- **E2E**: percurso no browser, com o Playwright e a aplicação a correr.

Os testes indicam-se como `Classe.método`. Os do Backend estão em `backend/src/test/java/pt/esmad/matriz/`; os do Frontend, em `frontend/src/`, indicam-se pelo ficheiro e pelo nome do teste.

## Pressupostos verificados

Cada linha é um pressuposto errado que uma matriz de cenários poderia fazer sem fundamento: ou é uma prática habitual noutras aplicações, ou preenche um ponto que as user stories deixam em aberto. **Não é o que a Matriz faz**; a regra da Matriz está na coluna "Decidido como". Para cada um, foi conferido o que dizem os requisitos e o contrato. Não houve uma primeira versão da matriz com estes erros: a matriz foi escrita já conferida, e a tabela regista as verificações feitas.

| # | Pressuposto errado que foi evitado | Problema | Decidido como | Fonte |
|---|------------------------------------|----------|---------------|-------|
| P1 | "Utilizador sem acesso → 404, para não revelar que a tarefa existe" | É uma prática comum, mas não é a regra da Matriz. | Recurso que existe, sem acesso → **403** `SEM_PERMISSAO`. Identificador inexistente ou que não é UUID → **404**. | openapi.yaml l. 33–35 |
| P2 | "O Administrador consulta tudo" | O Administrador não participa em projetos. | **403** em todas as operações de tarefas, mesmo quando a tarefa não existe. | RN19; openapi.yaml l. 29–31 |
| P3 | "Só quem criou a tarefa a pode editar" | Os requisitos não falam do criador. | **Qualquer membro do projeto** edita e muda o estado; só o Gestor elimina. | RN09, RN14 |
| P4 | "Título até 255 caracteres", que é o limite habitual de uma coluna de texto | É um valor inventado. | Entre **3 e 150**, com os limites incluídos e sem contar os espaços nas pontas: `" ab "` tem 2 e é recusado. | RN23 |
| P5 | "Cada carácter conta como uma unidade da string" | Em JavaScript e em Java, um emoji ocupa 2 unidades. | Conta-se o que o utilizador vê: `"😀😀😀"` tem 3 caracteres. Os requisitos não o dizem; é uma decisão da Ficha PL 03 e fica marcada como pressuposto. | Ficha PL 03, I8 (R1); `comum/texto.ts` |
| P6 | "Projeto arquivado → as tarefas deixam de estar acessíveis" | Arquivado quer dizer só de leitura. | Consultar continua a ser permitido. Criar, editar, mudar o estado e eliminar → **409** `PROJETO_ARQUIVADO`. | RN15, CE4; openapi.yaml l. 37–39 |
| P7 | "Prazo no passado → erro de validação" | Contraria a regra. | Qualquer data válida é aceite, incluindo datas passadas. | RN12, US05 crit. 5 |

## Matriz de cenários

★ marca os cenários essenciais pedidos na Ficha 04 (l. 36): título inválido, criação e consulta, e acesso sem autorização. "Novo" quer dizer que o teste é acrescentado nesta ficha (ver [testes novos](#testes-novos-da-ficha-pl-04)).

| ID | Cenário | Entradas | Resultado esperado | Fonte | Nível | Testes |
|----|---------|----------|--------------------|-------|-------|--------|
| T01 | Criar uma tarefa só com o título | Membro de um projeto não arquivado; `POST /api/projetos/{id}/tarefas` com `{"titulo": "  Preparar reunião  "}` | 201 com `Location`; título sem os espaços; `A_FAZER`; sem responsáveis; versão 0; opcionais a `null`. No quadro, o cartão aparece em "A fazer". | US05 crit. 1–2; RN10 | I, U (Frontend) | `TarefasApiTest.tarefaSoComTituloComecaEmAFazerSemResponsaveisENaVersao0`; `DialogosTarefa.test.tsx` "cria a tarefa e ela aparece em "A fazer" sem recarregar a página" |
| T02 ★ | Criar e depois consultar | Membro cria uma tarefa com todos os campos e consulta-a com `GET /api/tarefas/{id}` | O `GET` devolve 200 com os mesmos valores do `201`; a linha existe na tabela `tarefa` com esses valores. | US05 crit. 1, 6; RN10 | I | **Novo:** `CriarConsultarTarefaApiTest` |
| T03 | Campos opcionais e prazo passado | `descricao`, `prazo: "2020-01-31"`, `prioridade: "ALTA"` | 201 com os valores enviados. | US05 crit. 2, 5; RN12 | I | `TarefasApiTest.aceitaTodosOsCamposOpcionaisEUmPrazoPassado` |
| T04 ★ | Título em falta, vazio ou só com espaços (incluindo os invisíveis, como U+00A0, U+200B e U+FEFF) | Sem `titulo`; `""`; `"    "`; 3× U+00A0, U+200B ou U+FEFF | 400 `VALIDACAO` com `detalhes.campo = "titulo"`; **nada é gravado**. No Frontend, o pedido não chega a ser enviado. | US05 crit. 3; RN23 | U, I, U (Frontend) | `TarefaTest.tituloObrigatorioEntre3E150Caracteres`; `TarefasApiTest.recusaTituloEmFaltaSoComEspacosOuForaDosLimites`; `DialogosTarefa.test.tsx` "recusa um título só com espaços sem enviar o pedido"; **novo:** confirmar que nada fica na tabela, em `CriarConsultarTarefaApiTest`; títulos invisíveis em `TextoTest` e `texto.test.ts`, regressão do Q1 (`quality-security-review.md`) |
| T05 ★ | Limites do título | 2, 3, 150 e 151 caracteres, sem espaços nas pontas | 2 e 151 → recusado; 3 e 150 → aceite. | RN23 | U | Existentes cobrem 2, 150 e 151 (`TarefaTest.tituloObrigatorioEntre3E150Caracteres`); **novo:** teste parametrizado com os quatro limites em `TarefaTest` |
| T06 | Contagem de caracteres | `"😀😀😀"` | Aceite, com 3 caracteres (pressuposto P5). | RN23; P5 | U | `TarefaTest.contaCaracteresComoOUtilizadorOsVe`; `comum/texto.test.ts` "conta os caracteres como o utilizador os vê" |
| T07 | Limite da descrição | 2000 e 2001 caracteres | 2000 → aceite; 2001 → 400 no campo `descricao`. | RN23 | U, I | `TarefaTest.descricaoAte2000Caracteres`; `TarefasApiTest.recusaDescricaoComMaisDe2000Caracteres` |
| T08 | O que foi escrito não se perde num erro | A API devolve 400 ao guardar | O formulário mantém os valores e mostra o erro no campo. | US05 crit. 3 | U (Frontend) | `DialogosTarefa.test.tsx` "se a API devolver erro, o formulário mantém os dados escritos" |
| T09 ★ | Tarefa inexistente | `GET /api/tarefas/{UUID aleatório}`; `GET /api/tarefas/nao-e-um-id` | 404 nos dois casos. | openapi.yaml l. 33–35 | I | `TarefasApiTest.consultarRecusaNaoMembrosAdministradorETarefasInexistentes`; **novo:** em `CriarConsultarTarefaApiTest` |
| T10 ★ | Não-membro consulta uma tarefa de outro projeto (IDOR) | A ana, autenticada, usa o id de uma tarefa de um projeto do bruno | 403 `SEM_PERMISSAO`, sem dados da tarefa no corpo. | RN20; US07 crit. 4; S03 | I, manual | `TarefasApiTest.consultarRecusaNaoMembrosAdministradorETarefasInexistentes`; `AutorizacaoApiTest.aAnaNaoAcedeNemAlteraNadaDoProjetoBComOsIdentificadoresDele`; **novo:** verificação manual com as contas de demonstração, na revisão de segurança (Ex. 5) |
| T11 ★ | Não-membro altera, elimina ou cria tarefas noutro projeto | `PATCH`, `DELETE` e `POST` com os ids do projeto B, pela ana | 403 em todos; o projeto B fica exatamente como estava (tarefas, membros, comentários). | RN20; S03 | I | `AutorizacaoApiTest.aAnaNaoAcedeNemAlteraNadaDoProjetoBComOsIdentificadoresDele`, `.misturarIdentificadoresDeProjetosDiferentesERecusado` |
| T12 | Administrador | Qualquer operação de tarefas, com o Administrador | 403, também para tarefas inexistentes (P2). | RN19 | I | `AutorizacaoApiTest.oAdministradorNaoAcedeAosProjetosNemAsTarefas`; `TarefasApiTest.consultarRecusaNaoMembrosAdministradorETarefasInexistentes` |
| T13 | Sem sessão | Qualquer operação sem cookie | 401 `NAO_AUTENTICADO`. | openapi.yaml l. 16–21 | I | `AutorizacaoApiTest.semSessaoTudoDevolve401` |
| T14 | Um membro que não é Gestor edita e muda o estado | `PATCH` com `versao` e `estado` | 200; a versão sobe; o histórico regista o autor, o valor anterior e o novo. | RN09; RN13–14; US05 crit. 6; US06 | I | `EditarEliminarTarefaApiTest.qualquerMembroEditaEFicaComoAutorNoHistorico`, `.qualquerEstadoPassaAQualquerOutroEOHistoricoRegistaOAnteriorEONovo` |
| T15 | Só o Gestor elimina | `DELETE /api/tarefas/{id}` por um membro e pelo Gestor | Membro → 403 e sem botão na interface; Gestor → 204, com o registo de eliminação. | RN09; US09 crit. 2–4 | I, U (Frontend) | `EditarEliminarTarefaApiTest.soOGestorElimina`, `.gestorEliminaATarefaComTudoOQueELaContemEFicaORegisto`; `DialogosTarefa.test.tsx` "um Membro que não é Gestor não tem a opção de eliminar" |
| T16 | Projeto arquivado | Criar, editar, mudar o estado e eliminar; consultar | 409 `PROJETO_ARQUIVADO` nas alterações; consultar → 200 (P6). | RN15; CE4; US06 crit. 3 | I | `EditarEliminarTarefaApiTest.projetoArquivadoRecusaEditarMudarEstadoEEliminar`; `TarefasApiTest.soMembrosCriamTarefasEmProjetosNaoArquivados`, `.tarefaDeUmProjetoArquivadoContinuaAPoderSerConsultada` |
| T17 | Duas gravações ao mesmo tempo | Dois `PATCH` com a mesma `versao` | Um 200 e um 409; a versão sobe uma só vez; uma só entrada no histórico. | US05 crit. 8; CE5; D08 | I | `GravacoesSimultaneasApiTest.duasGravacoesComAMesmaVersaoUmaEAceiteEAOutraRecusada`; `EditarEliminarTarefaApiTest.versaoAntigaERecusadaENadaMuda` |
| T18 ★ | Percurso do utilizador | Login → abrir o projeto → "+ Nova tarefa" → guardar | O cartão com o título aparece na coluna "A fazer" do quadro. | US02; US05 crit. 1; US07 crit. 1–2 | E2E | **Novo:** `frontend/e2e/criar-tarefa.spec.ts` |

## Testes novos da Ficha PL 04

| Exercício | Teste | Cenários | Porquê |
|-----------|-------|----------|--------|
| Ex. 2 (unitário) | Teste parametrizado com os limites do título em `TarefaTest` | T05 | Os testes atuais não verificam o limite válido de 3 caracteres. |
| Ex. 3 (integração) | `CriarConsultarTarefaApiTest`: criar → consultar → confirmar na base de dados; título inválido não grava nada; id inexistente → 404 | T02, T04, T09 | Hoje a criação e a consulta são verificadas em testes separados, e a persistência só é confirmada pela resposta da API. |
| Ex. 4 (E2E) | `frontend/e2e/criar-tarefa.spec.ts` (Playwright, Firefox) | T18 | Não havia nenhum teste E2E no repositório. |
| Ex. 5 (segurança) | Verificação manual de IDOR com a ana e o bruno, contra o Backend real | T10 | Os testes automáticos usam contas fictícias; a ficha pede as contas de demonstração (l. 122–123). |

## Fora do âmbito

- Responsáveis e comentários (US05 crit. 4 e 7, US12). Estão cobertos por `ResponsaveisApiTest` e `ComentariosApiTest`, mas não entram nesta matriz, que se limita a criar, consultar e aceder a tarefas.
- Mudar o estado e ver o painel no E2E (passos 4 e 5 do anexo `PromptTestesE2E.txt`). Ficam de fora por decisão do utilizador: o E2E segue só o percurso da Ficha 04 (l. 75–76).
- Outros browsers além do Firefox, testes de carga e testes em produção.

## Resultados

Execuções reais a 2026-11-12. Os do Backend usaram os Dev Services (PostgreSQL 17 descartável em Docker). Não precisam de servidores ligados.

| Teste | Comando | Resultado |
|-------|---------|-----------|
| Ex. 2: `TarefaTest.limitesDoTituloSemContarOsEspacosNasPontas` (4 casos: 2, 3, 150 e 151, cada um com e sem espaços nas pontas) | `cd backend && ./mvnw test -Dtest=TarefaTest` | ✅ 13 testes no `TarefaTest`, 0 falhas |
| Ex. 3: `CriarConsultarTarefaApiTest` (`criarEConsultarDevolveOsMesmosDadosQueFicaramNaBaseDeDados`, `tarefaSoComTituloGuardaOsOpcionaisComoNulos`, `tituloInvalidoERecusadoENaoGravaNada`, `tarefaInexistenteDevolve404`) | `cd backend && ./mvnw test -Dtest=CriarConsultarTarefaApiTest` | ✅ 4 testes, 0 falhas |
| Bateria completa do Backend | `cd backend && ./mvnw test` | ✅ 155 testes depois dos Ex. 2 e 3 (147 antes da Ficha 04, +8); **169** depois das correções do Ex. 5 (+14 em `TextoTest`). 0 falhas, 0 ignorados |
| Bateria completa do Frontend | `cd frontend && npm test` | ✅ 88 testes até ao Ex. 4; **102** depois das correções do Ex. 5 (+14 em `comum/texto.test.ts`). 0 falhas |
| Ex. 4: E2E `frontend/e2e/criar-tarefa.spec.ts` (Playwright 1.55.1, Firefox 141), contra a aplicação a correr com a base de dados local | `cd frontend && npm run e2e` (ver o README para as variáveis) | ❌ 1.ª execução falhou (problema do teste, ver abaixo); depois da correção, ✅ 3 execuções seguidas, 1 teste, 0 falhas (cerca de 4 s cada) |

**Revisão das asserções (Ex. 2.3):**
- **Valores-limite:** os resultados esperados vêm da RN23 e coincidem com o comportamento atual, por isso não houve defeito a corrigir nem regra alterada.
- **Mensagem de erro:** o teste parametrizado verificava primeiro o texto exato da mensagem. Essa asserção foi retirada, porque prende o teste a um pormenor de implementação (PDF 12, "testes frágeis"); verifica-se o campo `titulo`.
- **Persistência:** o teste de integração confirma a linha na tabela `tarefa` com SQL e não só pela resposta da API. Assim, uma resposta montada sem gravar seria detetada.

**Análise de falhas.** A ficha pede que se distinga um defeito da aplicação de um problema do teste ou de uma limitação do ambiente (Ficha 04, l. 108).

| # | Teste | Sintoma | Diagnóstico | Classificação | Ação |
|---|-------|---------|-------------|---------------|------|
| F1 | E2E, 1.ª execução | Ao abrir o projeto novo, a coluna "A fazer" não aparece e o teste esgota o tempo no passo 2. | O `error-context.md` do Playwright mostra "Este projeto ainda não tem tarefas.". Esse é o estado "Vazio" desenhado no wireframe (`docs/ui/wireframe.html:236-241`) e decidido no I4 (`docs/implementation-plan.md:381-383`). O teste tinha assumido que o quadro mostra sempre as quatro colunas. | **Problema do teste** | O teste passou a verificar a mensagem de projeto vazio antes de criar a tarefa. A aplicação não foi alterada. |
| F2 | E2E, instalação | `npm audit` acusa uma vulnerabilidade alta no `playwright@1.55.0`: descarrega os browsers sem verificar o certificado SSL (GHSA-7mvr-c777-76hp). | Foi escolhida a 1.55.0 para aproveitar o Firefox já descarregado. A versão corrigida, 1.55.1, usa a mesma revisão do Firefox (1490). | **Problema do ambiente de teste**, numa dependência de desenvolvimento | Passou para a 1.55.1; o `npm audit` deu 0 vulnerabilidades. |

**Observação O1, a partir da F1.** A US07, crit. 1 (`user-stories.md:171-172`), diz "quando abro um projeto do qual sou membro, então vejo 4 colunas". Num projeto sem tarefas, porém, o wireframe e a implementação mostram uma mensagem em vez das colunas. Não é um defeito da implementação, que segue o desenho aprovado, mas o texto do requisito e o desenho não coincidem. Por decisão do utilizador, fica só registado e o requisito não foi alterado.

**Limpeza dos dados do E2E.** O projeto de teste é eliminado no `afterEach`, que corre mesmo quando o teste falha. Depois das quatro execuções, incluindo a que falhou, a base de dados local tinha 0 projetos com nome `E2E …`, e os restantes projetos não foram tocados.

**Limitações do E2E:**
- Corre contra a base de dados local de desenvolvimento, e não contra uma base só para testes. O isolamento vem do nome único e da limpeza no fim.
- No Ubuntu 26.04, o Playwright 1.55 não reconhece o sistema. É preciso definir `PLAYWRIGHT_HOST_PLATFORM_OVERRIDE=ubuntu24.04-x64` para usar o Firefox compilado para o Ubuntu 24.04.
- Só foi executado no Firefox.

