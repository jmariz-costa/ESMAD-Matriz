# Matriz

A Matriz é uma aplicação web interna de gestão de projetos e tarefas, no
estilo de ferramentas como Jira, Azure DevOps ou Trello, pensada para as
equipas de uma única empresa.

**Em produção:** https://matriz-frontend-tbdh.onrender.com (Render e Neon,
plano gratuito: o primeiro acesso depois de algum tempo sem uso pode demorar
cerca de 1 minuto). Ver [docs/deployment.md](docs/deployment.md) §8.

## Problema

As equipas da empresa precisam de um sítio comum para organizar projetos,
gerir tarefas e acompanhar o progresso do trabalho. O pedido original do
cliente foi:

> "Precisamos de uma aplicação onde as nossas equipas possam organizar
> projetos, gerir tarefas e acompanhar o progresso do trabalho."

## Objetivos principais

- Centralizar num só sítio os projetos e as tarefas das equipas.
- Deixar claro quem é responsável por cada tarefa.
- Tornar visível o progresso de cada projeto e o trabalho de cada pessoa.
- Permitir discutir o trabalho e saber quem alterou o quê e quando.
- Garantir que só pessoas da empresa acedem à informação.

## Funcionalidades

### Implementadas (Fase 1)

- Entrar e sair da conta, com sessão que expira ao fim de 30 minutos sem
  atividade e bloqueio de 15 minutos depois de 5 tentativas falhadas. As
  contas de demonstração são criadas por seed; não há registo público (L07).
- Projetos: criar, editar, eliminar e pesquisar, com paginação. Quem cria o
  projeto é o Gestor e gere os membros.
- Painel inicial com as contagens de tarefas por estado e percentagem de
  conclusão de cada projeto.
- Tarefas com descrição, prazo e prioridade opcionais e vários responsáveis,
  escolhidos entre os membros do projeto.
- Quatro estados de tarefa (A fazer, Em curso, Em revisão, Concluída) e
  quadro Kanban: mudar de estado arrastando o cartão, com filtros.
- Comentários nas tarefas.
- Registo das alterações das tarefas no histórico (a consulta é da Fase 2).

### Implementadas (Fase 2)

- "As minhas tarefas": as tarefas por concluir atribuídas a mim em todos os
  projetos, por prazo, e o histórico das concluídas, com a data de conclusão
  (US11).
- Gestão de contas pelo Administrador: pesquisar e filtrar as contas de
  Membro, desativar (com a escolha do novo Gestor de cada projeto da pessoa)
  e reativar (US14), editar o nome e repor o acesso com uma palavra-passe
  temporária, que a pessoa tem de mudar no login seguinte (US19).
- Criar contas de Membro pelo Administrador, com uma palavra-passe
  temporária a mudar no primeiro login e, se estiver configurado, só com
  email do domínio da empresa (US21). Substitui o registo público (US01).
- Mudar a própria palavra-passe, a partir do nome no cabeçalho (US20).

### Previstas (Fase 2)

- Consulta do histórico de cada tarefa e do registo de tarefas eliminadas.
- Editar e apagar os próprios comentários.
- Arquivo e reativação de projetos.

O detalhe está em [docs/requirements.md](docs/requirements.md) e o estado de
cada incremento em
[docs/implementation-plan.md](docs/implementation-plan.md) (ver também
[Estado atual](#estado-atual)).

## Tecnologias

Versões em uso na `0.2.0-rc.1`; as decisões estão em
[docs/constraints.md](docs/constraints.md) §4 e nos ADR
[0002](docs/decisions/0002-execucao-em-contentores-e-ci.md),
[0005](docs/decisions/0005-pipeline-em-etapas-e-publicacao-no-docker-hub.md) e
[0006](docs/decisions/0006-alojamento-render-e-neon.md).

| Camada | Tecnologias |
|--------|-------------|
| Backend | Java 21, Quarkus 3.40 (REST com Jackson, Hibernate ORM 7.4 com Panache, Hibernate Validator, SmallRye Health), Flyway 12, bcrypt |
| Base de dados | PostgreSQL 17 |
| Frontend | React 19, TypeScript 7.0, Vite 8, React Router 8; letra Plus Jakarta Sans (`@fontsource-variable`) |
| Testes | JUnit 6 e RestAssured 6 (Backend, com Quarkus Dev Services); ArchUnit 1.5 (regras de arquitetura do Backend); Vitest 5 e Testing Library (Frontend); Playwright 1.55 com Firefox (E2E) |
| Qualidade | oxlint, `npm audit`, `tsc` |
| Execução | Docker e Docker Compose; nginx 1.30 a servir o Frontend; JRE 21 (Eclipse Temurin) no Backend |
| CI/CD | GitHub Actions (pipeline em 5 etapas, release e deploy em produção); JaCoCo; imagens no Docker Hub |
| Produção | Render (Frontend e Backend) e Neon (PostgreSQL 17), região Frankfurt |

## Organização da documentação

| Ficheiro                                       | Conteúdo                                                                                      |
|------------------------------------------------|-----------------------------------------------------------------------------------------------|
| [docs/requirements.md](docs/requirements.md)   | Visão, objetivos, tipos de utilizadores, regras de negócio, requisitos, âmbito do MVP          |
| [docs/user-stories.md](docs/user-stories.md)   | User Stories do MVP, prioridades, critérios de aceitação e casos excecionais                  |
| [docs/constraints.md](docs/constraints.md)     | Restrições, pressupostos, segurança e privacidade, decisões pendentes, limitações de âmbito   |
| [docs/architecture.md](docs/architecture.md)   | Arquitetura escolhida (monólito modular), justificação, componentes e módulos                 |
| [docs/database.md](docs/database.md)           | Modelo de dados, diagrama ER em Mermaid e regras de integridade                               |
| [openapi.yaml](openapi.yaml)                   | Contrato REST (OpenAPI 3.1) da Fase 1: autenticação, projetos, membros, tarefas, comentários  |
| [docs/ui/](docs/ui/)                           | Wireframes do quadro Kanban e dos restantes ecrãs (PNG e HTML de origem)                      |
| [docs/implementation-plan.md](docs/implementation-plan.md) | Plano de implementação: incrementos I0–I8 da Fase 1, Fase 2 e estado de cada um   |
| [docs/verification.md](docs/verification.md)   | Registo das verificações do MVP: percurso da interface (I6) e casos de verificação (I7)       |
| [docs/test-plan.md](docs/test-plan.md)         | Plano de testes (Ficha PL 04): cenários de tarefas e permissões, níveis, testes e resultados  |
| [docs/quality-security-review.md](docs/quality-security-review.md) | Code Review e Security Review (Ficha PL 04): findings, evidências e decisões |
| [docs/deployment.md](docs/deployment.md)       | Execução com Docker Compose, imagens, pipeline de CI, produção (Render e Neon), release e deploy, limitações e troubleshooting |
| [render.yaml](render.yaml)                     | Blueprint do Render: serviços de produção (sem segredos)                                      |
| [docs/delivery-report.md](docs/delivery-report.md) | Validação final e relatório de entrega da `0.1.0-rc.1` (Ficha PL 05)                      |
| [docs/decisions/](docs/decisions/)             | Architecture Decision Records (ADRs), com índice no README da pasta                           |
| [CHANGELOG.md](CHANGELOG.md)                   | Histórico de alterações por versão (Keep a Changelog, Semantic Versioning)                    |
| [RELEASE_NOTES.md](RELEASE_NOTES.md)           | Notas da versão candidata `0.2.0-rc.1`                                                         |
| [AGENTS.md](AGENTS.md)                         | Instruções para agentes de IA que trabalhem neste repositório                                 |

## Executar com o Makefile

O [`Makefile`](Makefile) junta os comandos mais usados. Funciona em Linux e
macOS; no Windows, no WSL ou no Git Bash. `make` sem argumentos mostra a
lista.

```bash
make configurar        # primeira vez: cria o backend/.env com palavras-passe aleatórias
make arrancar          # aplicação completa em Docker: http://localhost:8090
make parar             # para os contentores (os dados mantêm-se)
make testes-unitarios  # Backend, sem Quarkus nem base de dados
make testes-arquitetura # Backend, só as regras de arquitetura do ADR 0001 (ArchUnit)
make testes-integracao # Backend, com um PostgreSQL temporário (precisa do Docker)
make testes-frontend   # Vitest
make testes-e2e        # Playwright, contra a aplicação do make arrancar
make testes            # unitários, integração e Frontend
make cobertura         # Backend, com o mínimo de 95% (JaCoCo)
make verificar         # Frontend: tipos, lint e build
```

- **Requisitos:** Docker com Docker Compose para a aplicação; JDK 21 para os
  testes do Backend; Node.js e npm para os do Frontend e o E2E; `openssl`
  para o `make configurar`.
- **Palavras-passe:** o `make configurar` copia o `backend/.env.example` para
  `backend/.env` e gera palavras-passe aleatórias para a base de dados, o
  Administrador e as contas de demonstração, como a pipeline. Mostra-as uma
  vez no fim e ficam guardadas no `backend/.env`, que nunca vai para o Git.
  Se o ficheiro já existir, não é alterado. Para escolher outras, editar o
  `backend/.env` **antes** do primeiro `make arrancar`.
- **Só contam no primeiro arranque:** as contas são criadas quando a base de
  dados está vazia; depois, mudar o `backend/.env` não as altera (cada pessoa
  muda a sua na aplicação, US20, ou o Administrador repõe o acesso, US19). A
  `POSTGRES_PASSWORD` também só é usada quando o volume é criado: mudá-la
  depois impede o Backend de se ligar. Para recomeçar com as palavras-passe
  novas, `docker compose down -v` apaga o volume com **todos** os dados.
- **O `make testes-e2e`** lê a `MATRIZ_DEMO_PASSWORD` do `backend/.env` (o
  mesmo ficheiro que a aplicação usa) e passa-a só ao Playwright; não a
  escreve noutro ficheiro. Em sistemas que o Playwright 1.55 ainda não
  reconhece: `PLAYWRIGHT_HOST_PLATFORM_OVERRIDE=ubuntu24.04-x64 make testes-e2e`.
- O nome do projeto Docker Compose é fixo (`esmad-matriz`): duas cópias do
  repositório no mesmo computador partilham o volume da base de dados. Para
  as separar: `COMPOSE_PROJECT_NAME=outro-nome make arrancar`.

## Executar com Docker Compose

Sem o Makefile: só precisa de Docker com Docker Compose (não precisa de Java
nem de Node.js).

```bash
cp backend/.env.example backend/.env    # depois, mudar as três palavras-passe
docker compose up -d --build            # PostgreSQL, Backend e Frontend
docker compose ps                       # os três serviços devem ficar "healthy"
```

Abrir **http://localhost:8090** e entrar com uma
[conta de demonstração](#contas-de-demonstração). Para parar:
`docker compose stop` (os dados mantêm-se no volume `matriz-dados`).

Serviços, redes, hardening, pipeline e troubleshooting em
[docs/deployment.md](docs/deployment.md).

## Executar localmente

### Requisitos

- JDK 21 ou superior (o Maven não é necessário: o projeto usa o Maven wrapper `./mvnw`)
- Node.js 20.19 ou superior (ou 22.12+) e npm
- Docker com Docker Compose

### Primeira configuração

```bash
cp backend/.env.example backend/.env    # depois, alterar as palavras-passe
cd frontend && npm install && cd ..
```

O ficheiro `backend/.env` guarda a configuração local e nunca é enviado para o
Git. É lido pelo contentor PostgreSQL e pelo Backend. **Mude as três
palavras-passe antes do primeiro arranque**: os valores do `.env.example` são
públicos. A base de dados só aceita ligações deste computador (`127.0.0.1:5432`,
ver `compose.yaml`).

| Variável                | Para que serve                                                                 |
|-------------------------|--------------------------------------------------------------------------------|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Base de dados criada no contentor e usada pelo Backend (`localhost:5432`) |
| `MATRIZ_ADMIN_NOME`, `MATRIZ_ADMIN_EMAIL`, `MATRIZ_ADMIN_PASSWORD` | Conta de Administrador, criada no arranque se ainda não existir (D07) |
| `MATRIZ_SEED_DEMO`      | `true` cria os dados de demonstração no arranque, se ainda não existirem Membros |
| `MATRIZ_DEMO_PASSWORD`  | Palavra-passe das contas de demonstração                                        |
| `MATRIZ_DOMINIO_EMAIL`  | Opcional. Domínio, sem `@`, das contas de Membro criadas pelo Administrador (ex.: `empresa.pt`); vazio ou ausente, o domínio não é verificado (US21, IF01) |
| `MATRIZ_COOKIE_SEGURO`  | `true` quando a aplicação é servida por HTTPS (cookie `Secure`, D06)           |

O endereço do Backend usado pelo Frontend em desenvolvimento está num único
sítio, `frontend/vite.config.ts` (variável opcional `MATRIZ_BACKEND_URL`, por
omissão `http://localhost:8081`); o código usa sempre o caminho relativo `/api`.

### Arrancar

```bash
docker compose up -d postgres           # Só o PostgreSQL (dados no volume matriz-dados)
cd backend && ./mvnw quarkus:dev        # Backend em http://localhost:8081 (API em /api)
cd frontend && npm run dev              # Frontend em http://localhost:5173
```

Cada comando fica a correr; usar um terminal para o Backend e outro para o
Frontend.

- Estado do Backend e da ligação à base de dados: http://localhost:8081/q/health/ready
- Para parar a base de dados: `docker compose stop` (os dados mantêm-se).

### Esquema da base de dados e dados de demonstração

- **Esquema:** criado e evoluído só por migrations Flyway, aplicadas
  automaticamente no arranque do Backend
  (`backend/src/main/resources/db/migration/`, hoje `V1__esquema_inicial.sql`).
  O Hibernate não altera tabelas.
- **Administrador:** criado no arranque a partir de `MATRIZ_ADMIN_*`, se ainda
  não existir.
- **Dados de demonstração (seed):** com `MATRIZ_SEED_DEMO=true`, o primeiro
  arranque cria 4 contas de Membro, 2 projetos ("Website institucional" e
  "Portal do Cliente"), 10 tarefas em vários estados, com responsáveis e
  histórico, e 1 comentário. Só corre se ainda não existirem Membros, por isso
  reiniciar o Backend não apaga nem duplica dados.
- **Recomeçar do zero:** `docker compose down -v` apaga o volume com **todos**
  os dados; no arranque seguinte, o esquema e os dados de demonstração são
  criados de novo.

### Contas de demonstração

Contas fictícias, todas com a palavra-passe definida em `MATRIZ_DEMO_PASSWORD`
no `backend/.env`:

| Conta               | Projetos                                                        |
|---------------------|-----------------------------------------------------------------|
| `ana@empresa.pt`    | Gestora de "Website institucional"; membro de "Portal do Cliente" |
| `bruno@empresa.pt`  | Gestor de "Portal do Cliente"; membro de "Website institucional"  |
| `carla@empresa.pt`  | Membro de "Website institucional"                                |
| `daniel@empresa.pt` | Sem projetos                                                     |

A conta de Administrador usa `MATRIZ_ADMIN_EMAIL` e `MATRIZ_ADMIN_PASSWORD`.

### Testes

```bash
cd backend && ./mvnw test               # todos (unitários e integração); precisa do Docker: cria um PostgreSQL temporário
cd backend && ./mvnw test -DexcludedGroups=integracao   # só os unitários (sem Quarkus nem base de dados)
cd backend && ./mvnw test -Dgroups=integracao           # só os de integração (@QuarkusTest)
cd backend && ./mvnw test -Dgroups=arquitetura          # só as regras de arquitetura (ArchUnit)
cd backend && ./mvnw verify             # todos, com relatório de cobertura e mínimo de 95% (JaCoCo)
cd frontend && npm test                 # Vitest + Testing Library
cd frontend && npx tsc -b && npm run lint && npm run build   # tipos, lint e build
```

O Backend tem 160 testes unitários e 166 de integração (com `@Tag("integracao")`).
Três dos unitários são as regras de arquitetura do
[ADR 0001](docs/decisions/0001-arquitetura-interna-backend.md), verificadas
com ArchUnit em `ArquiteturaTest`: camadas (recurso → serviço → repositório),
entre módulos só pelos serviços e o package `comum` sem dependências dos
módulos.
O relatório de cobertura fica em `backend/target/site/jacoco/index.html`; o
`verify` falha abaixo de 95% de linhas ou de ramos. Os testes também leem o
`backend/.env`: sem ele, o Admin e os dados de demonstração não são criados e
a cobertura desce (ver [docs/deployment.md](docs/deployment.md) §7).

Com o Backend de desenvolvimento a correr na porta 8081, os testes do Backend
precisam de outra porta: `./mvnw test -Dquarkus.http.test-port=0`. O registo
das verificações do MVP está em [docs/verification.md](docs/verification.md).

**Testes E2E** (Playwright, Firefox). Correm contra a aplicação a correr: a
base de dados, o Backend e o Frontend têm de estar ligados (ver "Arrancar"
ou "Executar com Docker Compose").
Cada execução cria um projeto com nome único na conta da Ana e elimina-o no
fim.

```bash
cd frontend && npx playwright install firefox   # só da primeira vez: descarrega o Firefox do Playwright
cd frontend && MATRIZ_DEMO_PASSWORD=<valor do backend/.env> npm run e2e
```

- `MATRIZ_DEMO_PASSWORD` é a palavra-passe das contas de demonstração, a
  mesma do `backend/.env`. Não é guardada em nenhum ficheiro do Frontend.
- `MATRIZ_E2E_URL` (opcional) muda o endereço testado; por omissão é
  `http://localhost:5173`. Com o Docker Compose: `MATRIZ_E2E_URL=http://localhost:8090`.
- Em sistemas que o Playwright 1.55 ainda não reconhece (por exemplo, Ubuntu
  26.04), acrescentar `PLAYWRIGHT_HOST_PLATFORM_OVERRIDE=ubuntu24.04-x64`.
- Para ver o browser: `npm run e2e -- --headed`; passo a passo:
  `npm run e2e -- --debug`. Em caso de falha, o trace e o screenshot ficam em
  `frontend/test-results/` e o relatório em `frontend/playwright-report/`
  (`npx playwright show-report`).

**Análise estática e dependências:** `cd frontend && npm run lint` (oxlint) e
`cd frontend && npm audit`. O Backend não tem análise estática configurada.

**Integração contínua:** cada push e Pull Request para `main` corre a pipeline
[`.github/workflows/ci.yml`](.github/workflows/ci.yml) no GitHub Actions, em
5 etapas: (1) build do Backend e do Frontend, com lint e `npm audit`;
(2) regras de arquitetura (ArchUnit) e testes unitários, de integração e do
Frontend; (3) cobertura JaCoCo, com
mínimo de 95% de linhas e de ramos; (4) imagens Docker, smoke tests e o teste
E2E contra o Docker Compose; (5) só em push para o `main`, publicação das
imagens testadas no Docker Hub (`matriz-backend` e `matriz-frontend`). Os
resultados estão no separador **Actions** do repositório (ver
[docs/deployment.md](docs/deployment.md) §7).
O plano de testes está em [docs/test-plan.md](docs/test-plan.md) e a revisão
de qualidade e segurança em
[docs/quality-security-review.md](docs/quality-security-review.md).

## Autenticação e sessão

A API usa uma sessão guardada na base de dados e identificada por um cookie
(D06 em [docs/constraints.md](docs/constraints.md)). O detalhe de cada
operação está no [openapi.yaml](openapi.yaml).

1. **Entrar:** `POST /api/auth/login` com `email` e `palavraPasse`. Se
   estiverem corretos, o Backend cria uma sessão e devolve o cookie
   `matriz_sessao` (HttpOnly, SameSite=Strict, Path=/api). O token é
   aleatório e a base de dados guarda apenas o seu hash.
2. **Pedidos seguintes:** o browser envia o cookie automaticamente e o Backend
   identifica o utilizador pela sessão. Quem cria um projeto ou escreve um
   comentário é sempre obtido daqui, nunca dos dados do pedido.
3. **Expiração:** cada pedido renova o prazo. Ao fim de 30 minutos sem pedidos,
   a sessão expira e o pedido seguinte recebe 401 `NAO_AUTENTICADO`.
4. **Sair:** `POST /api/auth/logout` apaga a sessão e o cookie.
5. **Consultar:** `GET /api/auth/me` devolve o utilizador da sessão atual.

**Regras de segurança:** palavras-passe guardadas só como hash bcrypt (S01).
Qualquer falha de login dá a mesma mensagem (PR03). 5 falhas seguidas bloqueiam
esse email durante 15 minutos (D09). Uma conta desativada perde o acesso, mesmo
com uma sessão aberta. Com HTTPS, deve definir-se `MATRIZ_COOKIE_SEGURO=true`
no `backend/.env`.

**Experimentar com curl** (com o Backend a correr):

```bash
curl -i -c cookies.txt -H 'Content-Type: application/json' \
  -d '{"email":"ana@empresa.pt","palavraPasse":"<MATRIZ_DEMO_PASSWORD>"}' \
  http://localhost:8081/api/auth/login
curl -b cookies.txt http://localhost:8081/api/auth/me
curl -b cookies.txt -X POST http://localhost:8081/api/auth/logout
```

## Estado atual

**Fase 1 implementada** (Ficha PL 03). O plano e o estado de cada incremento
estão em [docs/implementation-plan.md](docs/implementation-plan.md) e as
verificações em [docs/verification.md](docs/verification.md).

- Specification Pack (Ficha PL 01): requisitos, User Stories e critérios de
  aceitação.
- Architecture Pack (Ficha PL 02): monólito modular, modelo de dados e
  diagrama ER, contrato OpenAPI e wireframe do quadro Kanban.
- Ficha PL 03: decisões técnicas tomadas (Java 21 + Quarkus, React +
  TypeScript, PostgreSQL, sessão com cookie; ver
  [docs/constraints.md](docs/constraints.md) §4), contrato OpenAPI alargado a
  toda a Fase 1, aplicação e base de dados preparadas (I0), autenticação (I1),
  gestão de projetos com painel inicial (I2), membros do projeto (I3),
  tarefas com quadro Kanban (I4), responsáveis e comentários (I5), revisão
  da interface (I6), verificação do MVP (I7) e revisão e entrega (I8).
- Ficha PL 04 (Quality Assurance): plano de testes, testes unitário, de
  integração e E2E (Playwright), Code Review do I8 e Security Review (IDOR
  verificado com duas contas de demonstração), com duas correções e os
  respetivos testes de regressão (ver
  [docs/test-plan.md](docs/test-plan.md) e
  [docs/quality-security-review.md](docs/quality-security-review.md)).

- Ficha PL 05 (DevOps e documentação): Dockerfiles do Backend e do Frontend,
  Docker Compose com a aplicação completa, pipeline de CI no GitHub Actions,
  documentação de execução ([docs/deployment.md](docs/deployment.md),
  [ADR 0002](docs/decisions/0002-execucao-em-contentores-e-ci.md)) e versão
  candidata **`0.1.0-rc.1`** (tag `v0.1.0-rc.1`; [CHANGELOG.md](CHANGELOG.md),
  [RELEASE_NOTES.md](RELEASE_NOTES.md),
  [relatório de entrega](docs/delivery-report.md)).
- Depois da versão candidata: ADR 0003 (base de dados relacional PostgreSQL)
  e ADR 0004 (autenticação com sessão na base de dados), que registam
  decisões já tomadas na Ficha PL 03 (ver [docs/decisions/](docs/decisions/)).
- Depois da versão candidata: pipeline em 5 etapas (build, testes unitários e
  de integração separados, cobertura JaCoCo com mínimo de 95%, imagens e
  publicação no Docker Hub) e 66 testes unitários novos no Backend.
- Versão candidata **`0.2.0-rc.1`**: produção no Render e no Neon, com
  deploy por release (workflows "Release" e "Deploy prod"; ver
  [docs/deployment.md](docs/deployment.md) §8,
  [ADR 0006](docs/decisions/0006-alojamento-render-e-neon.md),
  [CHANGELOG.md](CHANGELOG.md) e [RELEASE_NOTES.md](RELEASE_NOTES.md)).
- Depois da versão candidata `0.2.0-rc.1`: novo design da interface, com
  tema claro e escuro (botão no cabeçalho; ver [CHANGELOG.md](CHANGELOG.md)).
- Fase 2, incremento F2.4: "As minhas tarefas" (US11), com o histórico das
  concluídas (ver [docs/implementation-plan.md](docs/implementation-plan.md) §5).
- Fase 2, incremento F2.6: gestão de contas pelo Administrador (US14).
- Fase 2, incremento F2.8: editar o nome e repor o acesso (US19) e mudar a
  própria palavra-passe (US20); a L01 foi revista.
- Regras de arquitetura do ADR 0001 verificadas com ArchUnit, num passo
  próprio da pipeline; os ciclos entre módulos ficam registados no ADR.
- Fase 2, incremento F2.9: o Administrador cria as contas de Membro (US21),
  que substitui o registo público (US01, retirada).

Próximo: Fase 2 (arquivo, histórico consultável e edição de comentários).
