# Plano de implementação — Matriz

Plano executável do MVP da Matriz (Ficha PL 03). Traduz a documentação em
incrementos pequenos, cada um com componentes, critérios de aceitação e forma
de verificação. O estado de cada incremento é atualizado ao longo da
implementação.

Documentos de referência: [requirements.md](requirements.md),
[user-stories.md](user-stories.md), [constraints.md](constraints.md),
[architecture.md](architecture.md), [database.md](database.md),
[../openapi.yaml](../openapi.yaml) e [ui/](ui/).

## 1. Conciliação entre a Ficha PL 03 e o MVP

Diferenças encontradas entre o âmbito da Ficha PL 03 e o MVP aprovado nas
Fichas PL 01 e PL 02, e a decisão tomada para cada uma (Exercício 1).

| # | Diferença | Decisão | Documentos atualizados |
|---|-----------|---------|------------------------|
| 1 | A ficha pede vários responsáveis por tarefa; o MVP tinha no máximo um (RN11). | Passar a vários responsáveis, sem duplicados. | RN11, US05, US07, US13, US14, database (AtribuicaoTarefa), OpenAPI, wireframe |
| 2 | A ficha pede eliminar projetos; o MVP só permitia arquivar (RN15, L06). | Só o Gestor elimina, com confirmação e em cascata; também em projetos arquivados. O arquivo mantém-se. | RN15, RN22, L06, US16, RI10, RI15, RI17 |
| 3 | A ficha considera o registo público opcional; no MVP era a US01 (Alta). | US01 adiada; contas de demonstração criadas por seed. | L07, requirements §7, US01 |
| 4 | A ficha pede editar projeto, pesquisa, filtros, painel e paginação, que não estavam no MVP. | Entram no âmbito: US16, US17 e US18. | RF16–RF18, US16–US18, OpenAPI, wireframes |
| 5 | O MVP tem User Stories que a ficha não exige (US04, US07–US14). | Implementadas por fases: Fase 1 = mínimo da ficha + US04 + Kanban; Fase 2 = restantes. | Este plano |
| 6 | Limites de texto não definidos no MVP. | Os do exemplo da ficha, sem contar espaços nas pontas. | RN23, database, OpenAPI |
| 7 | No exemplo da ficha a prioridade é obrigatória (MEDIUM). | Mantém-se opcional (RN10); filtro com "Sem prioridade". | — |
| 8 | A ficha pede adaptar o layout a várias larguras; o MVP define ≥1280 px. | Layout adaptativo, sem alterar o requisito RNF01. | architecture, wireframes |
| 9 | Os estados do exemplo da ficha são TODO/IN_PROGRESS/DONE. | Mantêm-se os 4 estados do contrato adotado (RN13). | — |

**Decisões técnicas** (detalhe em [constraints.md](constraints.md) §4):
Java 21 + Quarkus (D01), React + TypeScript + Vite (D01), PostgreSQL em
Docker com Flyway (D02), sessão na base de dados com cookie HttpOnly e 30 min
de inatividade (D06), Administrador criado no arranque a partir de variáveis
de ambiente (D07), bloqueio otimista (D08), 5 falhas → 15 min (D09), testes
JUnit + RestAssured e Vitest + Testing Library (D11), identificadores UUID.
Arquitetura interna do Backend: camadas por módulo com DDD leve
([ADR 0001](decisions/0001-arquitetura-interna-backend.md)).

## 2. Regras de acesso consolidadas

| Ação | Quem pode | Origem |
|------|-----------|--------|
| Ver projeto, tarefas, responsáveis e comentários | Membros do projeto | RN20, S04 |
| Criar projeto | Qualquer Membro (fica Gestor) | RN05 |
| Editar ou eliminar projeto, gerir membros, arquivar e reativar | Só o Gestor | RN07, RN22, PR07 |
| Criar e editar tarefas, mudar estado e prioridade | Membros do projeto | RN09, RN14 |
| Eliminar tarefa | Só o Gestor | RN09 |
| Atribuir e remover responsáveis | Membros do projeto | US05 |
| Elegíveis como responsáveis | Membros do projeto com conta ativa | RI02 |
| Comentar | Membros do projeto | US12 |
| Editar ou apagar comentário | Só o autor | PR11 |
| Alterar algo num projeto arquivado | Ninguém, exceto reativar, eliminar e as ações da US14 | RI10 |
| Administrador | Nada dentro dos projetos (403) | RN19 |

A identidade (Gestor de um projeto novo, autor de comentários e do histórico)
vem sempre da sessão, e nunca de dados enviados pelo cliente.

## 3. Estrutura prevista do repositório

```
ESMAD-Matriz/
├── backend/                 Quarkus (Maven wrapper)
│   └── src/main/java/…/matriz/
│       ├── comum/           erros, autenticação, configuração
│       ├── contas/          utilizadores, sessões, login, Administrador
│       ├── projetos/        projetos, membros, contagens
│       ├── tarefas/         tarefas, responsáveis
│       ├── comentarios/
│       └── historico/
│   ├── src/main/resources/db/migration/   migrations Flyway
│   └── .env.example         valores fictícios (o .env real não vai para o Git)
├── frontend/                React + TypeScript (Vite)
│   └── src/ api/ (cliente central), componentes/, ecras/
├── docker-compose.yml       PostgreSQL com volume persistente
└── docs/
```

## 4. Fase 1 — incrementos

Estado: ⬜ por fazer · 🟨 em curso · ✅ concluído.

Em todos os incrementos: pedir primeiro a análise e o plano do incremento,
implementar depois de validado, executar as verificações, rever o diff e
atualizar este plano antes de passar ao seguinte.

### I0 — Preparar a aplicação e a persistência ✅

**Ficha:** Exercício 2. **Depende de:** nada.

- Criar `backend/` (Quarkus: REST + Jackson, Hibernate ORM com Panache,
  Hibernate Validator, JDBC PostgreSQL, Flyway, SmallRye Health) e
  `frontend/` (Vite, React, TypeScript, React Router, Vitest).
- `docker-compose.yml` com PostgreSQL e volume persistente; `.env.example`
  com valores fictícios; `.gitignore` para `.env`, `target/`, `node_modules/`.
- Migration inicial com todo o esquema de [database.md](database.md),
  incluindo as chaves compostas, as FK com eliminação em cascata (RI17) e os
  índices necessários.
- Seed de demonstração: pelo menos 3 Membros e 2 projetos com membros
  diferentes, aplicado uma única vez (nunca apaga dados no arranque).
- Criação do Administrador no arranque a partir de variáveis de ambiente (D07).
- Proxy `/api` do Vite para o Backend.

**Critérios de aceitação**
- `docker compose up -d`, o Backend e o Frontend arrancam seguindo o README.
- O esquema é criado pelas migrations; reiniciar o Backend não apaga dados.
- O health check confirma a ligação à base de dados.

**Verificação:** arranque manual, health check, consulta às tabelas e ao seed,
reinício do Backend; comandos registados no README.

**Resultado (2026-11-01)**
- Quarkus 3.40.1.1 (LTS), Java 21; Vite 8, React 19, TypeScript, React Router,
  Vitest; PostgreSQL 17.
- Migration `V1__esquema_inicial.sql` aplicada; 9 tabelas criadas.
- Seed (`DadosDemonstracao`): 4 Membros, 2 projetos, 10 tarefas com
  responsáveis e histórico, 1 comentário. Administrador criado a partir do
  `.env` (`AdministradorInicial`). Palavras-passe guardadas em bcrypt.
- Segundo arranque: nada é criado nem apagado (totais iguais).
- `/q/health/ready` = UP com o JAR e com `./mvnw quarkus:dev`.
- O proxy do Vite reencaminha `/api` para o Backend (pedido a um caminho
  inexistente devolve o 404 do Quarkus).
- Testes: `./mvnw test` (1 teste, arranque com PostgreSQL do Dev Services) e
  `npm test` (1 teste) passam; `npm run build` e `npm run lint` sem erros.

**Desvios ao plano**
- O `.env.example` (e o `.env`) ficam em `backend/`, e não na raiz: um único
  ficheiro serve o Docker Compose e o Quarkus.
- O Backend usa a porta 8081, porque a 8080 estava ocupada nesta máquina.
- O seed escreve diretamente em SQL as tabelas de vários módulos: é código de
  arranque, fora da regra "só através do serviço do outro módulo" (ADR 0001),
  porque ainda não existem os serviços.

### I1 — Autenticação ✅

**Ficha:** Exercício 3. **US:** US02. **Decisões:** D06, D09, S01, PR03.

- Backend: entidade Sessao; `POST /auth/login`, `POST /auth/logout`,
  `GET /auth/me`; filtro que identifica o utilizador pelo cookie e renova a
  sessão; hash bcrypt; limite de tentativas; formato único de erro; 401 sem
  sessão válida; 403 para o Administrador nas operações de projetos.
- Frontend: ecrã de início de sessão (wireframe-ecras, ecrã 1); cliente HTTP
  central (endereço da API num único sítio); contexto do utilizador; rotas
  protegidas; em 401, voltar ao início de sessão com a mensagem
  "A sua sessão terminou…"; botão Sair.

**Critérios de aceitação:** US02 crit. 1, 3 e 4; D09; sessão expira após 30
min sem atividade; palavra-passe nunca guardada em claro.

**Verificação:** testes de integração (login válido, inválido, bloqueio após 5
falhas, pedido sem sessão, logout invalida a sessão); teste de componente do
formulário; verificação manual no browser.

**Resultado (2026-11-05)**
- Backend (módulo `contas`): entidades `Utilizador` (limite de tentativas,
  D09) e `Sessao` (30 min de inatividade, D06); `AutenticacaoServico`;
  `POST /auth/login`, `GET /auth/me`, `POST /auth/logout`. Token aleatório de
  256 bits no cookie `matriz_sessao` (HttpOnly, SameSite=Strict, Path=/api);
  a base de dados guarda só o hash SHA-256. Login sem distinguir maiúsculas no
  email; qualquer falha devolve a mesma resposta (PR03), incluindo conta
  inexistente (verificada contra um hash fictício) ou não ativa.
- Backend (`comum`): formato único de erro (`comum/erros`), incluindo JSON mal
  formado e campos desconhecidos (400 `VALIDACAO`); filtro de sessão fechado
  por omissão, só os endpoints `@Publico` dispensam sessão; identidade do
  pedido em `UtilizadorAtual`, para os outros módulos.
- Frontend: cliente HTTP central (`api/cliente.ts`), contexto de sessão, rota
  protegida com regresso ao ecrã de origem, ecrã de início de sessão (ecrã 1),
  cabeçalho com Sair e página inicial provisória (Membro: "Olá, …";
  Administrador: gestão de contas numa fase seguinte).
- README: secção "Autenticação e sessão" com o ciclo da sessão e exemplos
  `curl`, verificados contra o Backend em modo dev.
- Testes: `./mvnw test` (29: login, bloqueio após 5 falhas, conta desativada,
  validação, pedido sem sessão, logout, expiração e renovação, regras D06/D09
  sem base de dados) e `npm test` (17: cliente HTTP, formulário, rotas
  protegidas, sair, sessão terminada); `tsc` e `npm run lint` sem erros.
- Verificação manual no browser: feita (2026-11-05), com as contas de
  demonstração e a do Administrador.

**Desvios ao plano**
- O 403 para o Administrador nas operações de projetos passa para o I2, onde
  surgem as primeiras rotas de projetos. O mesmo acontece com a verificação
  com dois utilizadores diferentes (Ficha, Ex. 3, ponto 5).
- O filtro de sessão fica em `contas` (precisa do serviço de autenticação) e
  a identidade do pedido em `comum`, para que os restantes módulos não
  dependam de `contas` (ADR 0001, regra 5).
- Cookie `Secure` configurável por `MATRIZ_COOKIE_SEGURO` (por omissão
  `false`, para funcionar em `http://localhost`).
- Navegação do cabeçalho só com "Início"; as restantes entradas são
  acrescentadas quando os ecrãs existirem.

### I2 — Gestão de projetos e painel ✅

**Ficha:** Exercício 4. **US:** US03, US16, US17 (pesquisa e paginação), US18, US10.

- Backend: `GET/POST /projetos`, `GET/PATCH/DELETE /projetos/{id}`; Gestor
  obtido da sessão e adicionado como membro (RI01); listagem limitada aos
  projetos do utilizador; pesquisa sem distinguir maiúsculas; paginação;
  contagens por estado; eliminação em cascata; validação RN23; 403 para o
  Administrador em todas as operações de projetos (RN19, transitado do I1).
- Frontend: painel inicial e ecrã de projetos (wireframe-ecras, ecrãs 2 e 3);
  formulário de criar/editar; confirmação antes de eliminar; percentagem de
  conclusão calculada a partir das contagens (PR09).

**Critérios de aceitação:** US03, US16, US17 crit. 1, 2 e 4, US18, US10 crit.
1 e 2. **Primeiro percurso completo da ficha:** iniciar sessão, criar um
projeto, vê-lo na listagem e voltar a consultá-lo após recarregar a página e
reiniciar o Backend.

**Verificação:** testes de integração (validação do nome, criar e consultar
com persistência, outro utilizador recebe 403, o Administrador recebe 403,
só o Gestor edita e elimina, cascata sem órfãos, paginação e total);
verificação manual do percurso completo.

**Resultado (2026-11-05)**
- Backend (módulo `projetos`): agregado `Projeto` com os membros (RN23 sem
  contar espaços nas pontas, contando caracteres e não unidades UTF-16; RI01;
  `garantirAlteravel` para RI10); `ProjetosServico` com as permissões (403
  para o Administrador e para quem não é membro, 403 para quem não é Gestor
  ao editar e eliminar, 404 para id inexistente ou inválido);
  `GET/POST /projetos`, `GET/PATCH/DELETE /projetos/{id}`. Listagem dos
  projetos não arquivados do membro, ordenados pelo nome, com pesquisa por
  parte do nome (`%` e `_` tratados como texto) e paginação; contagens e
  Gestores obtidos numa consulta para toda a página. O `PATCH` distingue o
  campo omitido de `descricao: null`.
- Backend (outros módulos): `ContasServico` (nome do Gestor) e módulo
  `tarefas` mínimo com `ContagemTarefasServico` (só as contagens por estado);
  em `comum`, paginação (`PedidoPagina`, `Pagina`), erros por campo
  (`Validacao`), regras de texto e conversão de identificadores.
- Frontend: painel (ecrã 2), ecrã de projetos (ecrã 3) com pesquisa enquanto
  se escreve e pesquisa e página guardadas no URL, detalhe provisório do
  projeto, formulário de criar e editar, confirmação antes de eliminar,
  percentagem de conclusão (US10). Navegação passa a ter "Projetos".
- Percurso da ficha verificado contra o Backend real: login, criar, ver na
  listagem, reiniciar o Backend e voltar a consultar com a mesma sessão; a
  página do projeto abre por endereço direto (recarregar).
- Testes: `./mvnw test` (66; 37 novos: agregado, criar e consultar,
  listagem, editar e eliminar com cascata sem órfãos) e `npm test` (42; 21
  novos: percentagem, painel, ecrã de projetos, formulário, detalhe); `tsc`,
  `npm run lint` e `npm run build` sem erros.
- Verificação manual no browser: feita (2026-11-05).

**Desvios ao plano**
- O detalhe do projeto (`/projetos/:id`) é provisório: mostra os dados e as
  contagens; o quadro Kanban é acrescentado no I4.
- O painel mostra os primeiros 20 projetos e, se houver mais, a ligação
  "Ver todos os projetos (N)" para o ecrã de projetos.
- A confirmação de eliminação não inclui ainda a sugestão de arquivar do
  wireframe, porque arquivar é da Fase 2 (US08); volta com a US08.
- Um pedido mal formado dá 400 antes de se verificarem as permissões.

### I3 — Membros do projeto ✅

**US:** US04 (base das regras de acesso e da elegibilidade).

- Backend: `GET/POST /projetos/{id}/membros`,
  `DELETE /projetos/{id}/membros/{utilizadorId}`, `GET /utilizadores`; só
  Membros ativos (RI16); 409 `JA_EXISTE`; o Gestor não pode ser removido;
  remover um membro retira as suas atribuições em tarefas não concluídas (RI04).
- Frontend: janela "Membros" no detalhe do projeto (só Gestor).

**Critérios de aceitação:** US04 crit. 1 a 5.

**Verificação:** testes de integração (adicionar, duplicado, remover Gestor,
membro removido deixa de aceder); verificação manual com dois utilizadores.

**Resultado (2026-11-05)**
- Backend (módulo `projetos`): regras no agregado `Projeto`
  (`adicionarMembro`: 409 `JA_EXISTE`; `removerMembro`: 409
  `GESTOR_NAO_REMOVIVEL`, 404 se não for membro; ambos recusam projetos
  arquivados com 409); `AcessoProjeto` concentra a ordem das verificações de
  acesso (Administrador 403 → inexistente 404 → não membro 403 → não Gestor
  403), usada também pelo `ProjetosServico`; `MembrosServico` e
  `MembrosRecurso` (`GET`/`POST /projetos/{id}/membros`,
  `DELETE /projetos/{id}/membros/{utilizadorId}`).
- Backend (outros módulos): `AtribuicoesServico` no módulo `tarefas` retira a
  pessoa das tarefas não concluídas do projeto, na mesma transação da remoção
  (RN08, RI04); `ContasServico.eMembroAtivo` (RI16) e `GET /utilizadores`
  (Membros ativos, pesquisa no nome ou no email, só id, nome e email, S07);
  pesquisa `LIKE` passou para `comum` (`Texto.padraoPesquisa`).
- Frontend: secção "Membros (N)" no detalhe do projeto, visível para todos
  os membros, com as etiquetas "Gestor" e "Conta desativada"; o Gestor tem
  "+ Adicionar membro" (pesquisa enquanto se escreve, esconde quem já é
  membro, janela fica aberta para várias adições) e "Remover" com
  confirmação; nada disto aparece em projetos arquivados.
- Percurso com dois utilizadores verificado contra o Backend real (Ana
  adiciona o Daniel, que passa de 403 a ver o projeto; duplicado dá 409; o
  Daniel não remove a Ana; depois de removido volta a 403).
- Testes: `./mvnw test` (84; 18 novos) e `npm test` (48; 6 novos); `tsc`,
  `npm run lint` e `npm run build` sem erros.
- Verificação manual no browser: feita (2026-11-05).

**Desvios ao plano**
- A lista de membros é visível para todos os membros do projeto (só
  leitura); adicionar e remover continuam só do Gestor. O contrato já
  permitia a qualquer membro listar.
- Remover um membro pede confirmação na interface.
- `GET /utilizadores` está disponível para qualquer Membro (o Administrador
  recebe 403): o pedido não indica um projeto, por isso não é possível
  exigir que seja Gestor.
- Retirar as atribuições ao remover um membro não gera entrada no histórico
  nem altera a `versao` das tarefas: o histórico é da Fase 2 (US13) e a
  versão é tratada no I4/I5.
- A janela de adicionar mostra até 10 resultados, sem paginação: para
  encontrar outras pessoas, pesquisa-se.

### I4 — Gestão de tarefas e quadro Kanban ✅

**Ficha:** Exercício 5. **US:** US05, US06, US07, US09 (crit. 1 a 3), US17 (filtros).

- Backend: `GET/POST /projetos/{id}/tarefas` (filtros e paginação),
  `GET/PATCH/DELETE /tarefas/{id}`; estado inicial `A_FAZER`; `versao` e 409
  `VERSAO_DESATUALIZADA` (D08); entradas no histórico na criação e em cada
  alteração (RI12; a consulta do histórico é da Fase 2); eliminar só pelo
  Gestor, com registo em TarefaEliminada (RI09).
- Frontend: detalhe do projeto com o quadro (wireframe.html): uma listagem
  paginada por coluna com "Ver mais", filtros, mudar estado por arrastar e
  pelo seletor, formulário de criação e detalhe da tarefa, confirmação antes
  de eliminar; listagens e contagens atualizadas sem recarregar a página.

**Critérios de aceitação:** US05 crit. 1, 2, 3, 5, 6, 8; US06; US07; US09
crit. 1 a 3; US17 crit. 3; textos só com espaços rejeitados.

**Verificação:** testes de integração (validação do título, estado inicial,
filtros combinados, versão desatualizada, eliminar só pelo Gestor, tarefa
persiste após reiniciar); testes de componente (formulário mantém os dados
quando a API devolve erro); verificação manual do quadro.

**Resultado (2026-11-12)**
- Backend (módulo `tarefas`): agregado `Tarefa` com as atribuições
  (`criar`: estado `A_FAZER`, versão 0, título 3–150 e descrição até 2000 sem
  contar os espaços nas pontas; `editar`: só os campos enviados, `null` limpa
  os opcionais, devolve os campos que mudaram e recusa uma versão antiga com
  409 `VERSAO_DESATUALIZADA`); a `versao` usa `@Version` do JPA, que também
  impede que duas gravações simultâneas se sobreponham; `TarefaEliminada`
  (RI09); `TarefasServico`, `TarefasProjetoRecurso`
  (`GET`/`POST /projetos/{id}/tarefas`, filtros `estado` e `prioridade`
  combináveis, incluindo `SEM_PRIORIDADE`, paginação, ordenação por prazo com
  as tarefas sem prazo no fim e depois pelo título) e `TarefasRecurso`
  (`GET`/`PATCH`/`DELETE /tarefas/{id}`). Os corpos são lidos campo a campo
  (`PedidoTarefa`), para indicar o campo com problemas.
- Backend (outros módulos): novo módulo `historico` (ADR 0001), com
  `HistoricoServico` a registar a criação e uma entrada por campo alterado
  (valor anterior e novo, autor da sessão); `AcessoProjetosServico` no módulo
  `projetos`, por onde Tarefas pergunta o acesso ao projeto (membro, membro
  para alterar, Gestor para alterar); `ErroAplicacao.versaoDesatualizada`.
- Frontend: o detalhe do projeto passa a ter o quadro Kanban (wireframe.html):
  4 colunas pela ordem dos estados, com o total de cada uma, até 20 cartões e
  "Ver mais (N)"; cartão com título, responsáveis ou "Sem responsável",
  prioridade e prazo; filtros de estado e prioridade no endereço, com
  "Limpar filtros" (filtrar por estado mostra só essa coluna); mudar o estado
  arrastando o cartão (arrastar nativo do browser) ou no seletor do detalhe;
  "+ Nova tarefa" e detalhe da tarefa em diálogos; "Eliminar" só para o
  Gestor, com confirmação; depois de cada operação o quadro e as contagens do
  projeto são atualizados sem recarregar a página. Num projeto arquivado o
  quadro e o detalhe são só de leitura.
- Verificação contra o Backend real (pedidos pelo proxy do Vite): quadro dos
  dados de demonstração, filtros, criar, título só com espaços (400), mudar o
  estado como Membro, versão antiga (409), contagens, eliminar como Membro
  (403) e como Gestor (204 e registo em `tarefa_eliminada`); uma tarefa criada
  continua disponível depois de reiniciar o Backend.
- Testes: `./mvnw test` (120; 36 novos) e `npm test` (68; 20 novos); `tsc`,
  `npm run lint` e `npm run build` sem erros.
- Verificação manual no browser: feita (2026-11-12).

**Desvios ao plano**
- "Tarefa persiste após reiniciar" foi verificado à mão (reiniciando o
  Backend), e não com um teste automático; os testes de integração consultam
  a tarefa num pedido separado.
- A gravação simultânea de duas pessoas (o `@Version` dá 409) não tem teste
  automático; o caso da versão antiga tem.
- Um PATCH sem alterações reais devolve 200 sem aumentar a versão nem criar
  entradas no histórico.
- No histórico, os valores ficam como códigos (`EM_CURSO`, `ALTA`) e as datas
  como AAAA-MM-DD; a tradução fica para quando o histórico for mostrado
  (Fase 2, US13).
- Depois de um 409 de versão, "Recarregar tarefa" mantém os campos que o
  utilizador mudou e mostra os valores mais recentes nos outros (US05
  crit. 8). Até recarregar, "Guardar" fica inativo.
- Os filtros do quadro ficam no endereço (`?estado=…&prioridade=…`). Depois
  de cada operação, cada coluna volta a pedir as páginas que já tinha
  carregado; mudar um filtro volta à primeira página.
- A confirmação de eliminar aparece dentro do diálogo do detalhe; o
  "+ Nova tarefa" está só na barra dos filtros (não também na mensagem de
  projeto vazio).
- Responsáveis: o quadro e o detalhe mostram-nos, mas atribuir e remover é do
  I5. Continua por tratar no I5 que retirar as atribuições ao remover um
  membro (I3) aumente a `versao` das tarefas.

### I5 — Responsáveis e comentários ✅

**Ficha:** Exercício 6. **US:** US05 (crit. 4 e 7), US12 (crit. 1 e 2).

- Backend: `POST /tarefas/{id}/responsaveis`,
  `DELETE /tarefas/{id}/responsaveis/{utilizadorId}`; só membros ativos do
  projeto (RI02); 409 `JA_EXISTE`; histórico de cada atribuição;
  `GET/POST /tarefas/{id}/comentarios`, com autor e data definidos no Backend.
- Frontend: responsáveis no detalhe da tarefa (etiquetas com ✕ e
  "+ Adicionar" só com elegíveis); separador de comentários com autor e data.

**Critérios de aceitação:** atribuir vários, não duplicar, remover só uma
atribuição; comentário com 1 a 2000 caracteres e não só espaços; comentários
e atribuições disponíveis após reabrir a tarefa.

**Verificação:** testes de integração (duplicado → 409, não membro → 400,
autor do comentário é o da sessão mesmo que o pedido traga outro);
verificação manual.

**Resultado (2026-11-12)**
- Backend (módulo `tarefas`): `Tarefa.atribuir` (409 `JA_EXISTE`) e
  `Tarefa.removerResponsavel` (404 se não for responsável);
  `POST /tarefas/{id}/responsaveis` (201 com a tarefa; qualquer membro; só
  membros do projeto com conta ativa, senão 400) e
  `DELETE /tarefas/{id}/responsaveis/{utilizadorId}` (204, só essa
  atribuição); cada atribuição e remoção aumenta a versão da tarefa e fica no
  histórico com o nome da pessoa; projeto arquivado → 409.
  `AcessoTarefasServico`, por onde outros módulos perguntam o acesso a uma
  tarefa.
- Backend (outros módulos): novo módulo `comentarios` (ADR 0001), com o
  agregado `Comentario` (1 a 2000 caracteres sem contar os espaços nas
  pontas), `GET /tarefas/{id}/comentarios` (do mais antigo para o mais
  recente, paginado) e `POST` (autor da sessão e data do servidor; projeto
  arquivado → 409); `AcessoProjetosServico.eMembroAtivo` (RI02);
  `Identificadores.doCorpo` em `comum`, usado também pelos membros.
- Pendente do I3 resolvido: retirar as atribuições ao remover um membro passa
  pelo agregado `Tarefa`, aumenta a versão das tarefas afetadas e regista a
  remoção no histórico, com o Gestor como autor.
- Frontend: no detalhe da tarefa (diálogo mais largo), os responsáveis como
  etiquetas com ✕ (remove só essa atribuição) e "+ Adicionar" (só membros
  ativos do projeto que ainda não são responsáveis), gravados logo, sem
  "Guardar"; depois de atribuir ou remover, o formulário passa a usar a versão
  nova, sem perder o que estava escrito; secção "Comentários (N)" com autor,
  data e hora, os primeiros 20 e "Ver mais comentários", e a caixa
  "Escrever comentário"; num projeto arquivado, só leitura.
- Verificação contra o Backend real (pedidos pelo proxy do Vite, num projeto
  temporário eliminado no fim): atribuir vários, repetido (409), não membro
  (400), remover só um, histórico com o autor certo, comentário com o autor
  da sessão, pedido com `autor` (400), texto só com espaços (400), não membro
  (403), remover um membro do projeto aumenta a versão da tarefa e regista a
  remoção.
- Testes: `./mvnw test` (138; 18 novos) e `npm test` (79; 11 novos); `tsc`,
  `npm run lint` e `npm run build` sem erros.
- Verificação manual no browser: feita (2026-11-12).

**Desvios ao plano**
- Um pedido de comentário que traga `autor` (ou `criadoEm`) é recusado com
  400, e não ignorado: o contrato não aceita campos desconhecidos
  (`additionalProperties: false`). O autor é sempre o da sessão.
- Os comentários ficam numa secção por baixo dos campos, e não num
  separador: os separadores "Comentários | Histórico" entram com o histórico
  (Fase 2, US13).
- Comentar não aumenta a versão da tarefa nem fica no histórico, porque não
  altera os campos da tarefa.
- O ✕ dos responsáveis remove sem pedir confirmação (como no wireframe).
- A data dos comentários é guardada ao microssegundo (a precisão do
  PostgreSQL) e mostrada na hora local do browser.

### I6 — Revisão da interface ✅

**Ficha:** Exercício 7.

- Confirmar em todos os ecrãs: carregamento, lista vazia, sucesso e erro;
  dados preservados em erro; botões inativos durante pedidos; sem registos
  duplicados; totais coerentes com os filtros; sem mocks.
- Layout adaptativo em larguras menores.

**Verificação:** percurso manual completo no Firefox, registado em
verification.md.

**Resultado (2026-11-12)**
- Revisão dos 5 ecrãs do Exercício 7 contra os critérios da ficha: já
  cumpriam o endereço do Backend centralizado (`vite.config.ts`, `/api`), os
  estados de carregamento, vazio e erro, os dados preservados em erro, os
  botões inativos durante os pedidos, os totais de todo o conjunto e a
  ausência de mocks.
- Mensagem de sucesso uniforme (`FornecedorNotificacao`, `useNotificar`), no
  canto do ecrã durante 4 segundos e anunciada aos leitores de ecrã, depois de
  criar, guardar ou eliminar projetos e tarefas, mudar o estado, remover um
  membro e publicar um comentário.
- Layout adaptativo: abaixo de 900 px o quadro passa a 2 colunas; abaixo de
  600 px, a 1 coluna, com a tabela de projetos em scroll horizontal, o
  cabeçalho e as barras a quebrar linha e os diálogos ajustados ao ecrã.
- Corrigido: remover um membro passa a atualizar o quadro (os cartões deixavam
  de estar certos até recarregar a página); abrir ou guardar uma tarefa que
  outra pessoa eliminou tira o cartão do quadro; a coluna "Ações" da tabela de
  projetos ficava desalinhada em larguras menores (`display: flex` na célula).
- `docs/verification.md` criado, com o percurso P01–P11 e o resultado de cada
  passo.
- Testes: `npm test` (84; 5 novos, dos quais os da sincronização falham sem a
  correção); `tsc`, `npm run lint` e `npm run build` sem erros. Backend sem
  alterações (138).

**Desvios ao plano**
- O percurso no Firefox foi feito pelo agente de IA, a pedido do responsável
  do projeto, com o Firefox 141 do Playwright sem janela (script fora do
  repositório), e não manualmente; está registado como verificação
  automatizada pelo agente. As capturas de ecrã das larguras menores foram
  vistas uma a uma.
- No P09, a falta de ligação ao Backend foi simulada cortando os pedidos
  `/api` no browser, em vez de parar o Backend.
- O P08 foi feito num projeto temporário, para não retirar o Bruno das
  tarefas dos dados de demonstração.
- Atribuir e remover responsáveis não mostra mensagem de sucesso: o
  resultado aparece logo no próprio diálogo.
- O layout adaptativo não tem testes automatizados (o jsdom não aplica CSS);
  fica coberto pelo P11.

### I7 — Verificação do MVP ✅

**Ficha:** Exercício 8.

- Criar `docs/verification.md` com os casos 01 a 09 da ficha: resultado
  esperado, observado e evidência.
- Pelo menos três testes automatizados relevantes (validação, autorização,
  criação com consulta posterior), distinguindo testes automatizados de
  verificações manuais.
- Para cada bug: reproduzir, investigar a causa, corrigir de forma limitada e
  acrescentar um teste de regressão.

**Resultado (2026-11-12)**
- `docs/verification.md` com os casos 01 a 09: resultado esperado, observado
  e evidência (testes pelo nome, passos do percurso do I6 e comandos), os três
  testes obrigatórios identificados, os comandos executados com os
  resultados, o registo de problemas e as limitações.
- Novo `AutorizacaoApiTest` (caso 08): um membro do projeto A, com os
  identificadores do projeto B, recebe 403 em 15 operações e o B fica igual;
  misturar identificadores de projetos diferentes é recusado; o Administrador
  recebe 403 e os pedidos sem sessão 401. Não revelou problemas.
- Novo teste de Frontend (caso 09): um erro da API ao criar, guardar ou
  eliminar não mostra mensagem de sucesso e mantém o formulário.
- Caso 03: tarefa criada pela API continua disponível depois de reiniciar o
  Backend (comandos e respostas registados).
- Registo de problemas B01–B03 (os do I6) no formato do Exercício 8
  (reprodução, causa, correção, teste de regressão). No I7 não surgiram
  problemas novos.
- Testes: `./mvnw test` (143; 5 novos) e `npm test` (85; 1 novo); `tsc`,
  `npm run lint` e `npm run build` sem erros.

**Desvios ao plano**
- Não surgiram bugs novos no I7; o registo de problemas usa os três
  encontrados no I6, que já tinham correção e, quando possível, teste de
  regressão (o B03, de CSS, não tem teste automatizado).
- O caso 03 (reinício do Backend) foi verificado pelo agente com `curl`, e
  não com um teste automatizado.

### I8 — Revisão e entrega ⬜

**Ficha:** Exercício 9.

- Rever o diff (permissões, validações, erros, alterações fora do âmbito).
- Uma refatoração justificada, num passo separado.
- Atualizar OpenAPI, documentos, AGENTS.md (comandos) e README.md
  (requisitos, instalação, configuração, esquema, seed, contas de
  demonstração, arranque e testes).
- Três exemplos de utilização da IA em verification.md.
- Confirmar que o projeto arranca a partir de uma cópia limpa.

**Refatoração (decisão registada, 2026-11-12)**
- **Oportunidade:** a contagem de caracteres e a regra "obrigatório / fora dos
  limites" dos textos (RN23) estavam copiadas em três formulários do Frontend
  (`FormularioProjeto.tsx`, `tarefas/formulario.ts`, `ComentariosTarefa.tsx`).
  Mudar a regra obrigava a lembrar-se das três cópias, e no Backend a mesma
  regra já está num só sítio (`Texto.java`).
- **Decisão:** juntar a regra em `frontend/src/comum/texto.ts`
  (`comprimento` e `validarTexto`), usada pelos três formulários, com as mesmas
  mensagens. Foram consideradas e não escolhidas: juntar a validação de
  tamanho dos três agregados do Backend (mexe em mais código) e um método
  privado para o 409 de versão no `TarefasServico` (benefício pequeno).
- **Comportamento preservado:** os 85 testes do Frontend que já existiam
  passam sem alterações; acrescentados 3 testes unitários de
  `comum/texto.ts`. Feita num commit separado, sem outras mudanças.

## 5. Fase 2 — restantes User Stories do MVP

Cada uma é um incremento próprio, com o mesmo método, e acrescenta os seus
endpoints ao OpenAPI quando for implementada.

| Incremento | US | Estado |
|------------|----|--------|
| F2.1 Arquivar e reativar projeto | US08 | ⬜ |
| F2.2 Consultar o registo de tarefas eliminadas | US09 crit. 4 | ⬜ |
| F2.3 Consultar o histórico de uma tarefa | US13 | ⬜ |
| F2.4 As minhas tarefas | US11 | ⬜ |
| F2.5 Editar e apagar os próprios comentários | US12 crit. 3 e 4 | ⬜ |
| F2.6 Administrador: desativar e reativar contas | US14 | ⬜ |
| F2.7 Registo com confirmação por email (extensão) | US01 | ⬜ |

## 6. Pontos em aberto

- A pesquisa de projetos distingue acentos ("gestao" não encontra "Gestão");
  a US17 só exige ignorar maiúsculas. Ignorar acentos exige a extensão
  `unaccent` do PostgreSQL.

Resolvidos no I0: a palavra-passe das contas de demonstração vem de
`MATRIZ_DEMO_PASSWORD` no `backend/.env` (seed em Java, só se não existirem
Membros); o package base é `pt.esmad.matriz`.
