# Matriz

A Matriz é uma aplicação web interna de gestão de projetos e tarefas, no
estilo de ferramentas como Jira, Azure DevOps ou Trello, pensada para as
equipas de uma única empresa.

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

## Funcionalidades previstas para o MVP

- Entrada e saída da conta. O registo com email da empresa (com confirmação
  por email) foi adiado: as contas de demonstração são criadas por seed (L07).
- Criação de projetos; quem cria é o Gestor e gere os membros.
- Tarefas com descrição, prazo, prioridade e vários responsáveis.
- Quatro estados de tarefa: A fazer, Em curso, Em revisão, Concluída.
- Quadro Kanban.
- Percentagem de conclusão de cada projeto.
- Lista "As minhas tarefas", com as tarefas por concluir de todos os projetos.
- Comentários e histórico de alterações em cada tarefa.
- Arquivo e reativação de projetos.
- Desativação e reativação de contas por um Administrador.

O detalhe está em [docs/requirements.md](docs/requirements.md). Na Ficha PL 03
foi implementada a Fase 1 (autenticação, projetos, membros, tarefas e quadro
Kanban, responsáveis e comentários); o arquivo, "As minhas tarefas", a
consulta do histórico e o Administrador ficam para a Fase 2 (ver
[Estado atual](#estado-atual)).

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
| [docs/decisions/](docs/decisions/)             | Architecture Decision Records (ADRs), com índice no README da pasta                           |
| [AGENTS.md](AGENTS.md)                         | Instruções para agentes de IA que trabalhem neste repositório                                 |

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
ver `docker-compose.yml`).

| Variável                | Para que serve                                                                 |
|-------------------------|--------------------------------------------------------------------------------|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Base de dados criada no contentor e usada pelo Backend (`localhost:5432`) |
| `MATRIZ_ADMIN_NOME`, `MATRIZ_ADMIN_EMAIL`, `MATRIZ_ADMIN_PASSWORD` | Conta de Administrador, criada no arranque se ainda não existir (D07) |
| `MATRIZ_SEED_DEMO`      | `true` cria os dados de demonstração no arranque, se ainda não existirem Membros |
| `MATRIZ_DEMO_PASSWORD`  | Palavra-passe das contas de demonstração                                        |
| `MATRIZ_COOKIE_SEGURO`  | `true` quando a aplicação é servida por HTTPS (cookie `Secure`, D06)           |

O endereço do Backend usado pelo Frontend em desenvolvimento está num único
sítio, `frontend/vite.config.ts` (variável opcional `MATRIZ_BACKEND_URL`, por
omissão `http://localhost:8081`); o código usa sempre o caminho relativo `/api`.

### Arrancar

```bash
docker compose up -d                    # PostgreSQL (dados no volume matriz-dados)
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
cd backend && ./mvnw test               # precisa do Docker: cria um PostgreSQL temporário
cd frontend && npm test                 # Vitest + Testing Library
cd frontend && npx tsc -b && npm run lint && npm run build   # tipos, lint e build
```

Com o Backend de desenvolvimento a correr na porta 8081, os testes do Backend
precisam de outra porta: `./mvnw test -Dquarkus.http.test-port=0`. O registo
das verificações do MVP está em [docs/verification.md](docs/verification.md).

**Testes E2E** (Playwright, Firefox). Correm contra a aplicação a correr: a
base de dados, o Backend e o Frontend têm de estar ligados (ver "Arrancar").
Cada execução cria um projeto com nome único na conta da Ana e elimina-o no
fim.

```bash
cd frontend && npx playwright install firefox   # só da primeira vez: descarrega o Firefox do Playwright
cd frontend && MATRIZ_DEMO_PASSWORD=<valor do backend/.env> npm run e2e
```

- `MATRIZ_DEMO_PASSWORD` é a palavra-passe das contas de demonstração, a
  mesma do `backend/.env`. Não é guardada em nenhum ficheiro do Frontend.
- `MATRIZ_E2E_URL` (opcional) muda o endereço testado; por omissão é
  `http://localhost:5173`.
- Em sistemas que o Playwright 1.55 ainda não reconhece (por exemplo, Ubuntu
  26.04), acrescentar `PLAYWRIGHT_HOST_PLATFORM_OVERRIDE=ubuntu24.04-x64`.
- Para ver o browser: `npm run e2e -- --headed`; passo a passo:
  `npm run e2e -- --debug`. Em caso de falha, o trace e o screenshot ficam em
  `frontend/test-results/` e o relatório em `frontend/playwright-report/`
  (`npx playwright show-report`).

**Análise estática e dependências:** `cd frontend && npm run lint` (oxlint) e
`cd frontend && npm audit`. O Backend não tem análise estática configurada.
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

Próximo: Fase 2 (arquivo, "As minhas tarefas", histórico consultável,
edição de comentários e Administrador).
