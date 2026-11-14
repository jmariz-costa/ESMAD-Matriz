# Relatório de validação e entrega — Matriz v0.1.0-rc.1

Ficha PL 05 (DevOps e documentação), Exercício 6 e secção "Entrega".
Validação feita a 2026-11-14 sobre o commit `a001d67`, e repetida depois da
correção V2. Execução, pipeline e troubleshooting em
[deployment.md](deployment.md); decisões no
[ADR 0002](decisions/0002-execucao-em-contentores-e-ci.md) e no
[plano](implementation-plan.md) §8.

## 1. Requisitos da ficha

| Exercício | Requisito | Estado | Evidência |
|-----------|-----------|--------|-----------|
| 1 | Plano técnico antes de alterar ficheiros | ✅ | Plano §8 |
| 2 | Dockerfiles, `.dockerignore`, imagens base, arranque e portas | ✅ | `backend/Dockerfile`, `frontend/Dockerfile`, `frontend/nginx.conf` (`73a4f8b`) |
| 2 | Sem credenciais nos Dockerfiles; comportamento mantido | ✅ | Revisão do conteúdo e das camadas das imagens; percurso funcional |
| 3 | `compose.yaml` com serviços, redes, volumes, healthchecks | ✅ | `compose.yaml` (`e886eb4`) |
| 3 | `.env.example` | ✅ | `backend/.env.example` (decisão 2a: um só ficheiro, no Backend) |
| 3 | Aplicação acessível, componentes comunicam, persistência, logs | ✅ | §3 deste relatório; persistência depois de `down`/`up` no Ex. 3 |
| 4 | `ci.yml` em `push` e `pull_request`, testes, build, imagens | ✅ | `.github/workflows/ci.yml` (`f98f102`) |
| 4 | Falha quando um teste falha; Docker só depois das verificações | ✅ | Falha controlada `0f62fae` (vermelho, `docker-build` não correu) → `01f9cfd` (verde) |
| 4 | Permissões mínimas, sem segredos expostos | ✅ | `permissions: contents: read`; palavras-passe geradas por execução |
| 5 | README com os 7 pontos pedidos | ✅ | Descrição, funcionalidades, **tecnologias** (secção acrescentada na revisão final), pré-requisitos, instalação, comandos, testes, referências |
| 5 | `architecture.md` com Mermaid | ✅ | §5, diagrama de execução |
| 5 | Execução com Docker Compose documentada | ✅ | README e `deployment.md` |
| 5 | OpenAPI | ✅ | `openapi.yaml`: 21 operações, as mesmas 21 que o código |
| 5 | CHANGELOG, notas da versão, SemVer | ✅ | `CHANGELOG.md`, `RELEASE_NOTES.md`, versão `0.1.0-rc.1` (`a001d67`) |
| 6 | Validação global e relatório | ✅ | Este documento |

## 2. Alterações implementadas

| Commit | Alteração |
|--------|-----------|
| `73a4f8b` | Dockerfiles e `.dockerignore` do Backend e do Frontend; `MATRIZ_DB_HOST` |
| `e886eb4` | `compose.yaml` com a aplicação completa; modo de desenvolvimento com `up -d postgres` |
| `f98f102` | Pipeline do GitHub Actions |
| `2a42d56` | Correção: o Backend arranca sem `MATRIZ_ADMIN_*` e `MATRIZ_DEMO_PASSWORD` |
| `0f62fae`, `01f9cfd` | Falha controlada e o seu revert |
| `a001d67` | Documentação (`deployment.md`, ADR 0002, Mermaid, CHANGELOG, notas) e versão `0.1.0-rc.1` |
| Commits do Ex. 6 | Correção V2 (nome do contentor); validação final, secção "Tecnologias" no README e este relatório |

## 3. Validação final (Exercício 6)

| Tarefa | Como | Resultado |
|--------|------|-----------|
| 1. Executar a partir do README | `git clone` do GitHub para uma pasta temporária; secção "Executar com Docker Compose" seguida à letra, com palavras-passe novas e volume novo. Único desvio: `-p matriz-validacao`, para não tocar nos dados locais | Funcionou à primeira (1 min 25 s, com o build) |
| Primeiro arranque | Logs do Backend | Migração Flyway V1 aplicada, dados de demonstração e Administrador criados, `matriz-backend 0.1.0-rc.1` |
| 2. Estado dos contentores | `docker compose ps` | `postgres`, `backend` e `frontend` `healthy` |
| 3. Funcionalidades essenciais | Percurso por API através do nginx: login; projetos do seed; criar projeto; adicionar membro; criar tarefa; atribuir responsável; mudar de estado; versão antiga → 409; comentar; não membro → 403 em projeto, tarefa e comentário; Administrador nos projetos → 403; palavra-passe errada e sem sessão → 401; eliminar projeto → 204 e depois 404 | **24/24**. E2E (Playwright, Firefox): **1 passed**. Teste do utilizador no browser |
| 4. Pipeline | Separador Actions, execução de `a001d67` | Três jobs verdes (confirmado pelo utilizador). Sem capturas de ecrã: a evidência é o histórico do separador Actions, onde cada execução fica associada ao seu commit (`0f62fae` vermelha, `01f9cfd` e seguintes verdes) |
| 5. Logs | `docker compose logs`, à procura de `warn`/`error`/`fatal` | Backend e Frontend sem avisos; um aviso do PostgreSQL (V1) |
| 6. Credenciais | gitleaks v8.30.1 em todo o histórico (24 commits), sem rede e só em leitura; procura de ficheiros `.env` alguma vez versionados | **No leaks found**; nenhum `.env` versionado |
| 7. Problemas | Ver §5 | — |

Depois da correção V2, a validação da cópia limpa foi repetida **ao lado** da
instalação local, sem lhe mexer: os três serviços `healthy`, percurso 24/24 e
E2E 1 passed. O contentor local foi recriado com o nome novo e manteve os
dados (3 projetos).

## 4. Testes e pipeline

| Verificação | Resultado |
|-------------|-----------|
| Backend, `./mvnw test` | 169 testes, 0 falhas (também sem `backend/.env`) |
| Frontend, `npm test` | 102 testes, 0 falhas |
| Frontend: oxlint, `tsc -b` e build, `npm audit --audit-level=high` | Sem erros; 0 vulnerabilidades |
| E2E | 1 teste, passa contra o Docker Compose (local, cópia limpa e pipeline) |
| Pipeline | Verde desde `2a42d56`; vermelha só na falha controlada (`0f62fae`), como esperado |

## 5. Problemas encontrados e soluções

| # | Problema | Causa | Solução |
|---|----------|-------|---------|
| P1 | Build do Backend: "Failed to validate Maven distribution SHA-256" | Sem `unzip`, o `mvnw` descarrega o `.tar.gz`; a soma configurada é a do `.zip` | `unzip` na fase de compilação (`73a4f8b`); a soma foi confirmada contra o ficheiro oficial |
| P2 | Porta 8080 ocupada | Processo no host, fora do Docker | Frontend em `127.0.0.1:8090` |
| P3 | Primeira execução da pipeline: `backend-tests` falha (`SRCFG00040`) | Os testes só passavam com o `backend/.env` local; sem ele, as propriedades vazias rebentavam no arranque. O aviso "Administrador não configurado" nunca aparecia | `Optional<String>` (`2a42d56`); reproduzido e verificado numa cópia sem `.env` |
| V1 | Aviso "trust" do PostgreSQL | Comportamento da imagem oficial: o socket local não pede palavra-passe | Registado como limitação (decisão V1a) |
| V2 | Uma segunda instalação não arranca enquanto existir o contentor local | `container_name: matriz-postgres` fixo | Nome dado pelo Compose; validado lado a lado (decisão V2a) |
| R1 | README sem a secção "Tecnologias" pedida pela ficha | Omissão no Ex. 5 | Secção acrescentada, com as versões confirmadas no `package-lock.json` e nas dependências do Backend |

## 6. Utilização dos agentes de IA

Toda a ficha foi feita com um agente de IA (Claude Code) com acesso ao
repositório, num ciclo **o agente propõe → o utilizador decide → o agente
implementa e verifica → o utilizador testa**.

- **Decisões humanas:** 19 decisões, cada uma com opções e uma recomendação,
  tomadas pelo utilizador (plano §8): nginx, configuração, palavras-passe da
  CI, porta, hardening, actions por SHA, `npm audit`, forma da falha
  controlada, correção da P3, versão, tag, documentação, validação, V1 e V2.
  Antes de decidir, o utilizador pediu explicações sobre GitHub Secrets,
  referências por SHA, `rc` e as implicações de V1 e V2.
- **Commits e pushes:** só depois da autorização explícita do utilizador, com
  a mensagem mostrada antes.
- **Verificação:** o agente confirmou cada afirmação contra o sistema real
  (builds, `curl`, testes, logs, Docker Hub e GitHub para as versões), e o
  utilizador testou no browser e confirmou a pipeline no GitHub.
- **Erros do agente, corrigidos e registados:** usou `password` em vez de
  `palavraPasse` num teste manual; atribuiu números de US errados no
  CHANGELOG (US03 e US11); escreveu "JUnit 5" quando o projeto usa o 6; leu
  um relatório de testes antigo e julgou que havia uma falha. Todos foram
  detetados por verificação contra os ficheiros ou o código, antes do commit.
- **Segurança:** o agente nunca mostrou nem escreveu valores de palavras-passe;
  as comparações com o `.env` foram feitas sem os mostrar.

## 7. Limitações e pendentes

- **S1 (Ficha PL 04) resolvido a 2026-11-14:** as três palavras-passe do
  `backend/.env` local foram trocadas por valores aleatórios, diferentes entre
  si; na base de dados existente, `ALTER USER` e os hashes do Administrador e
  das 4 contas de demonstração foram atualizados com o mesmo `BcryptUtil` da
  aplicação. Verificado: as novas entram, as antigas dão 401, e o Backend liga
  à base de dados. O `.env.example` passou a ter valores de exemplo novos,
  que não são usados em nenhuma instalação.
- Só HTTP (S02 depende do alojamento, D04); sem deployment nem publicação de
  imagens.
- Sem análise de vulnerabilidades das imagens nem auditoria do Maven
  (desafio opcional A não feito); desafios B e C também não feitos.
- V1 (autenticação local "trust" no contentor do PostgreSQL); portas do host
  fixas.
- Herdados: Fase 2 do MVP por fazer; R3, Q2, Q4, O1; pesquisa sensível a
  acentos (ver [RELEASE_NOTES.md](../RELEASE_NOTES.md)).
- Uma execução local de `./mvnw test` terminou com código 1 sem escrever
  relatórios, com a saída escondida, por isso a causa não ficou registada; as
  duas execuções seguintes passaram.
- A pipeline do commit final é confirmada no Actions **antes** de criar a tag
  `v0.1.0-rc.1`.
