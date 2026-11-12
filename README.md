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
- Tarefas com descrição, prazo, prioridade e um responsável.
- Quatro estados de tarefa: A fazer, Em curso, Em revisão, Concluída.
- Quadro Kanban.
- Percentagem de conclusão de cada projeto.
- Lista "As minhas tarefas", com as tarefas por concluir de todos os projetos.
- Comentários e histórico de alterações em cada tarefa.
- Arquivo e reativação de projetos.
- Desativação e reativação de contas por um Administrador.

O detalhe está em [docs/requirements.md](docs/requirements.md).

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

O ficheiro `backend/.env` guarda a configuração local (base de dados, conta de
Administrador e dados de demonstração) e nunca é enviado para o Git.

### Arrancar

```bash
docker compose up -d                    # PostgreSQL (dados no volume matriz-dados)
cd backend && ./mvnw quarkus:dev        # Backend em http://localhost:8081 (API em /api)
cd frontend && npm run dev              # Frontend em http://localhost:5173
```

No primeiro arranque do Backend, o Flyway cria o esquema da base de dados e são
criados a conta de Administrador e os dados de demonstração. Reiniciar o Backend
não apaga nem duplica dados.

- Estado do Backend e da ligação à base de dados: http://localhost:8081/q/health/ready
- Para parar a base de dados: `docker compose stop` (os dados mantêm-se).

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
cd frontend && npm test
```

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

**Em implementação** (Ficha PL 03). O plano e o estado de cada incremento estão
em [docs/implementation-plan.md](docs/implementation-plan.md).

- Specification Pack (Ficha PL 01): requisitos, User Stories e critérios de
  aceitação.
- Architecture Pack (Ficha PL 02): monólito modular, modelo de dados e
  diagrama ER, contrato OpenAPI e wireframe do quadro Kanban.
- Ficha PL 03: decisões técnicas tomadas (Java 21 + Quarkus, React +
  TypeScript, PostgreSQL, sessão com cookie; ver
  [docs/constraints.md](docs/constraints.md) §4), contrato OpenAPI alargado a
  toda a Fase 1, aplicação e base de dados preparadas (I0), autenticação (I1),
  gestão de projetos com painel inicial (I2), membros do projeto (I3) e
  tarefas com quadro Kanban (I4). Próximo: responsáveis e comentários (I5).
