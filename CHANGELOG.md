# Changelog

Todas as alterações relevantes da Matriz ficam registadas neste ficheiro.

O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-PT/1.1.0/)
e as versões seguem o [Semantic Versioning](https://semver.org/lang/pt-PT/).
Enquanto a versão for `0.x`, a API não é considerada estável.

## [Unreleased]

## [0.1.0-rc.1] - 2026-11-13

Primeira versão identificada: versão **candidata** com a Fase 1 do MVP
(Fichas PL 01 a 05). Não foi publicada uma release.

### Added

- **Autenticação:** entrar e sair, sessão guardada na base de dados com
  cookie `HttpOnly`, expiração ao fim de 30 minutos sem atividade e bloqueio
  de 15 minutos depois de 5 tentativas falhadas (US02).
- **Projetos:** criar, editar, eliminar e pesquisar projetos, com paginação,
  percentagem de conclusão e um painel inicial com as contagens de tarefas
  por estado (US03, US10, US16, US17, US18).
- **Membros:** o Gestor adiciona e remove membros; ao remover, as atribuições
  dessa pessoa nas tarefas por concluir são retiradas e registadas no
  histórico (US04).
- **Tarefas e quadro Kanban:** criar, editar e eliminar tarefas, com prazo e
  prioridade opcionais; quatro estados (A fazer, Em curso, Em revisão,
  Concluída); mudar de estado arrastando o cartão; filtros no quadro; recusa
  de gravações com versão desatualizada (409) (US05, US06, US07, US09 crit. 1
  a 3, US17 crit. 3).
- **Responsáveis e comentários:** vários responsáveis por tarefa, escolhidos
  entre os membros do projeto (US05), e comentários nas tarefas (US12 crit. 1
  e 2).
- **Histórico:** a criação e as alterações das tarefas são registadas (a
  consulta é da Fase 2).
- **Interface:** mensagens de sucesso e de erro, e layout adaptado a ecrãs
  com menos de 900 px e de 600 px.
- **Dados de demonstração** (opcionais) e conta de Administrador criada a
  partir da configuração.
- **Contrato da API** em OpenAPI 3.1 (`openapi.yaml`, versão 0.3.0).
- **Testes:** 169 no Backend (JUnit e RestAssured, com PostgreSQL dos Dev
  Services), 102 no Frontend (Vitest e Testing Library) e 1 E2E (Playwright,
  Firefox).
- **Docker:** Dockerfiles do Backend (JRE 21) e do Frontend (nginx), em duas
  fases e sem root, e `.dockerignore` sem ficheiros `.env`.
- **Docker Compose:** `compose.yaml` com PostgreSQL, Backend e Frontend,
  redes separadas, volume persistente, healthchecks e arranque em cadeia;
  aplicação em `http://localhost:8090`.
- **Integração contínua:** pipeline no GitHub Actions com testes do Backend;
  lint, testes, build e `npm audit` do Frontend; e imagens Docker, smoke tests
  e E2E contra o Docker Compose, só depois dos testes.
- **Documentação:** `docs/deployment.md`, ADR 0002, diagrama de execução em
  `docs/architecture.md`, este changelog e as notas da versão.

### Changed

- O modo de desenvolvimento passa a arrancar só a base de dados com
  `docker compose up -d postgres`; o `docker-compose.yml` foi substituído
  pelo `compose.yaml`.
- O endereço da base de dados pode vir de `MATRIZ_DB_HOST` (por omissão,
  `localhost`).

### Fixed

- Remover um membro passa a atualizar o quadro, e uma tarefa eliminada por
  outra pessoa sai do quadro quando se tenta abri-la ou guardá-la (I6).
- A API recusa números enviados onde o contrato pede texto (R1, `3a44ffb`).
- O Backend arranca sem `MATRIZ_ADMIN_*` e `MATRIZ_DEMO_PASSWORD` definidos,
  e avisa nos logs, em vez de falhar no arranque. Encontrado pela pipeline
  de CI (`2a42d56`).

### Security

- O PostgreSQL só aceita ligações do próprio computador (`127.0.0.1:5432`),
  e o `.env.example` e o README avisam para mudar as palavras-passe de
  exemplo (S1, `85a2a0c`).
- A API recusa títulos, nomes e comentários feitos só de espaços Unicode ou
  caracteres invisíveis (Q1/S2, `85a2a0c`).
- Contentores do Backend e do Frontend sem root, com o sistema de ficheiros só
  de leitura, sem capacidades Linux e com `no-new-privileges`.
- Pipeline com permissões mínimas (`contents: read`), sem segredos, e com as
  actions referidas pelo SHA do commit.

[Unreleased]: https://github.com/jmariz-costa/ESMAD-Matriz/compare/v0.1.0-rc.1...HEAD
[0.1.0-rc.1]: https://github.com/jmariz-costa/ESMAD-Matriz/tree/v0.1.0-rc.1
