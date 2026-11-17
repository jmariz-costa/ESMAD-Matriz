# Changelog

Todas as alterações relevantes da Matriz ficam registadas neste ficheiro.

O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-PT/1.1.0/)
e as versões seguem o [Semantic Versioning](https://semver.org/lang/pt-PT/).
Enquanto a versão for `0.x`, a API não é considerada estável.

## [Unreleased]

### Added

- **`Makefile`** para correr localmente: `make configurar` (cria o
  `backend/.env` com palavras-passe aleatórias e mostra-as uma vez),
  `make arrancar`/`parar`/`registos`, e cada tipo de teste em separado
  (`testes-unitarios`, `testes-integracao`, `testes-frontend`, `testes-e2e`,
  `cobertura`, `verificar`). O E2E lê a palavra-passe do `backend/.env`.
- **Editar o nome e repor o acesso** (US19, incremento F2.8): na gestão de
  contas, o Administrador corrige o nome de uma pessoa (o email não se
  altera) e repõe o acesso de quem se esqueceu da palavra-passe, com uma
  palavra-passe temporária. As sessões dessa pessoa terminam e, no login
  seguinte, ela tem de definir uma palavra-passe nova; até lá, o servidor
  recusa os outros pedidos (403 `PALAVRA_PASSE_TEMPORARIA`). Migração Flyway
  V2 (`deve_mudar_palavra_passe`).
- **Mudar a própria palavra-passe** (US20): a partir do nome no cabeçalho;
  as outras sessões da pessoa terminam. Novos endpoints `PATCH /contas/{id}`,
  `POST /contas/{id}/reposicao-acesso` e `POST /auth/palavra-passe`
  (OpenAPI 0.6.0).
- **Gestão de contas do Administrador** (US14, incremento F2.6): a página do
  Administrador lista as contas de Membro, com pesquisa por nome ou email e
  filtro por estado. Desativar pede confirmação e, se a pessoa for Gestora,
  o novo Gestor de cada projeto (também arquivados); a pessoa deixa de
  conseguir entrar, as sessões terminam e sai de responsável das tarefas por
  concluir, com o Administrador como autor no histórico. Reativar devolve o
  acesso como Membro. Novos endpoints em `/contas` (OpenAPI 0.5.0).
- **"As minhas tarefas"** (US11, incremento F2.4): novo separador no menu com
  as tarefas atribuídas a mim em todos os projetos não arquivados, em dois
  separadores: por concluir (por prazo) e concluídas (da conclusão mais
  recente para a mais antiga, com a data). Clicar numa tarefa abre o detalhe.
  Novo endpoint `GET /minhas-tarefas` (OpenAPI 0.4.0) e critério 5 novo na
  US11 (histórico das concluídas).
- **Tema escuro:** botão com sol/lua no cabeçalho para alternar entre o tema
  claro e o escuro. A escolha fica guardada no browser (`localStorage`); sem
  escolha, a interface segue o tema do sistema operativo. O tema guardado é
  aplicado antes de a página aparecer, também no início de sessão.

### Changed

- **Recuperação da palavra-passe** (L01 revista): continua a não haver
  recuperação automática por email, mas o Administrador pode repor o acesso
  (US19). Novas regras PR20, PR21 e RI21; nome de uma pessoa entre 2 e 150
  caracteres (RN23).
- **Novo aspeto da interface** (proposta «Suave»): cores em tokens CSS, com
  verde-petróleo como cor de destaque, cartões com sombra suave e cantos
  arredondados, colunas do quadro com a cor do estado e etiquetas com a cor
  da prioridade. A disposição dos ecrãs continua a dos wireframes.
- **Letra Plus Jakarta Sans** incluída na aplicação
  (`@fontsource-variable/plus-jakarta-sans`), sem pedidos a servidores
  externos.

## [0.2.0-rc.1] - 2026-11-16

Segunda versão **candidata**: a Matriz passa a estar em produção (Render e
Neon), com pipeline em 5 etapas, cobertura mínima e deploy por release. As
funcionalidades são as da `0.1.0-rc.1` (Fase 1 do MVP).

### Added

- **Produção no Render e no Neon** (D04, ADR 0006): Frontend e Backend no
  Render, a partir das imagens do Docker Hub, e PostgreSQL no Neon, com HTTPS
  e SSL `verify-full` na ligação à base de dados. Configuração no Blueprint
  `render.yaml`, sem segredos.
- **Workflow "Release"** (`release.yml`): ao publicar uma release no GitHub,
  acrescenta a etiqueta da versão às imagens desse commit, sem build novo.
- **Workflow "Deploy prod"** (`deploy-prod.yml`): põe em produção a release
  escolhida, depois de confirmar a release e as imagens, e verifica que a
  produção responde.
- **Testes unitários do Backend:** 66 testes novos (`PedidoTarefaTest`,
  `EditarProjetoPedidoTest`, `PedidoPaginaTest`, `IdentificadoresTest`,
  `FiltrosEContagensTest`, `AdministradorInicialTest`,
  `DadosDemonstracaoTest`), sem Quarkus nem base de dados. O Backend passa a
  ter 112 testes unitários e 123 de integração (antes, 46 e 123).
- **Cobertura (JaCoCo):** `jacoco-maven-plugin` e extensão `quarkus-jacoco`;
  `./mvnw verify` gera o relatório e falha abaixo de 95% de linhas ou de
  ramos (98,9% e 95,6% em `06e1351`).
- **Publicação das imagens no Docker Hub** (`matriz-backend` e
  `matriz-frontend`, etiquetas SHA curto e `latest`), só em push para o
  `main`, com as imagens que passaram nos smoke tests e no E2E.

### Changed

- **Destino do `/api` configurável no nginx** (`MATRIZ_API_URL`,
  `frontend/nginx.conf.template`); por omissão continua `http://backend:8081`.
- **URL da base de dados com modo SSL configurável** (`MATRIZ_DB_SSLMODE`;
  por omissão `prefer`, como até aqui).
- **Pipeline em 5 etapas:** build; testes unitários, de integração e do
  Frontend em jobs separados; cobertura; imagens com smoke tests e E2E;
  publicação. Os testes de integração do Backend passam a ter
  `@Tag("integracao")`.

### Fixed

- Os testes do Backend na CI corriam sem `backend/.env` e, por isso, sem o
  Admin e os dados de demonstração, ao contrário do que acontece localmente;
  a CI passa a copiar o `.env.example` (`06e1351`).

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
- O contentor do PostgreSQL deixa de ter o nome fixo `matriz-postgres`
  (passa a `esmad-matriz-postgres-1`, com o mesmo volume), para permitir uma
  segunda instalação com `docker compose -p`.
- O README passa a ter a secção "Tecnologias".

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
- Valores de exemplo novos para as três palavras-passe do `.env.example`;
  os anteriores tinham sido usados numa instalação local (S1).

[Unreleased]: https://github.com/jmariz-costa/ESMAD-Matriz/compare/v0.2.0-rc.1...HEAD
[0.2.0-rc.1]: https://github.com/jmariz-costa/ESMAD-Matriz/compare/v0.1.0-rc.1...v0.2.0-rc.1
[0.1.0-rc.1]: https://github.com/jmariz-costa/ESMAD-Matriz/tree/v0.1.0-rc.1
