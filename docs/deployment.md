# Execução com Docker e integração contínua — Matriz

Como construir, executar e validar a Matriz em contentores, e o que a pipeline
de integração contínua verifica. Preparado na Ficha PL 05. As decisões estão
no [ADR 0002](decisions/0002-execucao-em-contentores-e-ci.md) e o diagrama de
execução em [architecture.md](architecture.md) §5.

Esta é uma configuração para **execução local e validação**, não para
produção: não há HTTPS, nem alojamento, nem publicação de imagens (ver
[§8](#8-limitações)).

## 1. Dois modos de execução

| Modo | Para quê | Comando | Endereço |
|------|----------|---------|----------|
| **Docker Compose** | Executar a aplicação completa, como será entregue | `docker compose up -d --build` | http://localhost:8090 |
| **Desenvolvimento** | Programar, com recarregamento automático | `docker compose up -d postgres`, depois `./mvnw quarkus:dev` e `npm run dev` | http://localhost:5173 |

Os dois modos usam o **mesmo** `compose.yaml`, o mesmo `backend/.env` e o
**mesmo volume**, por isso veem os mesmos dados. Usam portas diferentes no
host (8090 e 5173/8081), por isso podem até correr ao mesmo tempo.

## 2. Requisitos

- Docker Engine com Docker Compose v2 (testado com Docker 29.9 e Compose 5.6).
- Para o modo Docker Compose não é preciso Java nem Node.js: as imagens
  compilam o Backend e o Frontend.

## 3. Configuração

```bash
cp backend/.env.example backend/.env    # depois, mudar as três palavras-passe
```

O `backend/.env` é o único ficheiro de configuração local e nunca vai para o
Git (`.gitignore`). O Compose passa-o aos serviços `postgres` e `backend`
(`env_file`). As variáveis estão descritas no [README](../README.md#primeira-configuração).

| Variável | Onde é definida | Nota |
|----------|-----------------|------|
| `POSTGRES_*`, `MATRIZ_ADMIN_*`, `MATRIZ_SEED_DEMO`, `MATRIZ_DEMO_PASSWORD`, `MATRIZ_COOKIE_SEGURO` | `backend/.env` | Os valores do `.env.example` são públicos: **mudar as palavras-passe** |
| `MATRIZ_DB_HOST` | `compose.yaml` (`postgres`) | Só dentro do Docker; em modo de desenvolvimento fica `localhost` (`application.properties`) |

Se `MATRIZ_ADMIN_EMAIL` ou `MATRIZ_ADMIN_PASSWORD` faltarem, o Backend arranca
na mesma, avisa ("Administrador não configurado") e não cria a conta.

Nenhum Dockerfile nem o `compose.yaml` contém credenciais: as imagens recebem
as variáveis só no arranque.

## 4. Executar e validar

```bash
docker compose config -q          # valida o compose.yaml
docker compose build              # constrói as imagens backend e frontend
docker compose up -d --wait       # arranca e espera que os três serviços fiquem saudáveis
docker compose ps                 # estado: os três devem estar "healthy"
docker compose logs --tail=50     # logs de todos os serviços
```

Depois, abrir http://localhost:8090 e entrar com uma conta de demonstração
(README, "Contas de demonstração").

**Parar e recomeçar:**

| Comando | Efeito |
|---------|--------|
| `docker compose stop` | Para os contentores; tudo se mantém |
| `docker compose down` | Remove contentores e redes; **os dados mantêm-se** no volume |
| `docker compose down -v` | Remove também o volume: **apaga todos os dados** |
| `docker compose up -d --build` | Reconstrói as imagens depois de alterar o código |

**Verificações manuais úteis:**

```bash
docker compose exec backend curl -fsS http://localhost:8081/q/health/ready   # Backend e BD
curl -I http://localhost:8090/                                               # Frontend
curl -i http://localhost:8090/api/auth/me                                    # API via nginx: 401 sem sessão
```

## 5. Serviços, redes e volume

| Serviço | Imagem | Porta no host | Redes | Healthcheck |
|---------|--------|---------------|-------|-------------|
| `postgres` | `postgres:17` | `127.0.0.1:5432` (para o modo de desenvolvimento) | `data` | `pg_isready` |
| `backend` | `backend/Dockerfile` | nenhuma | `data`, `web` | `curl /q/health/ready` (inclui a ligação à BD) |
| `frontend` | `frontend/Dockerfile` | `127.0.0.1:8090` | `web` | `curl /` |

- **Ordem de arranque:** `postgres` saudável → `backend` saudável →
  `frontend` (`depends_on` com `condition: service_healthy`).
- **Redes:** `data` liga o Backend à base de dados; `web` liga o Frontend ao
  Backend. O Frontend não consegue resolver nem contactar o `postgres`.
- **Mesma origem:** o browser só fala com o nginx (`:8090`). O nginx serve o
  build do React e reencaminha `/api/` para `backend:8081`
  (`frontend/nginx.conf`), tal como o proxy do Vite em desenvolvimento. Por
  isso, o cookie de sessão funciona sem CORS.
- **Volume:** `matriz-dados`, com o nome de projeto fixo `esmad-matriz` (o
  volume chama-se `esmad-matriz_matriz-dados`, seja qual for o nome da pasta).
- **Só localmente:** todas as portas publicadas estão em `127.0.0.1`.
- **Reinício:** `restart: unless-stopped` nos três serviços.

**Hardening** do `backend` e do `frontend` (o `postgres` precisa de
privilégios para arrancar e não o tem):

| Configuração | Efeito |
|--------------|--------|
| `security_opt: no-new-privileges:true` | Nenhum processo ganha privilégios extra |
| `cap_drop: ALL` | Sem capacidades Linux (nenhuma é necessária: portas acima de 1024, sem root) |
| `read_only: true` + `tmpfs: /tmp` | Sistema de ficheiros só de leitura; só `/tmp` aceita escrita, em memória |
| `pids_limit` | 512 no Backend (cada thread da JVM conta; cerca de 57 em repouso) e 100 no Frontend |

## 6. Imagens

| Imagem | Construção | Execução | Tamanho |
|--------|-----------|----------|---------|
| Backend | `eclipse-temurin:21.0.13_9-jdk-noble` + `./mvnw package -DskipTests` | `eclipse-temurin:21.0.13_9-jre-noble`, utilizador `matriz` (uid 10001), `java -jar quarkus-run.jar` | cerca de 334 MB |
| Frontend | `node:24.22.3-alpine3.24` + `npm ci` + `npm run build` | `nginxinc/nginx-unprivileged:1.30.6-alpine3.24`, utilizador `nginx`, porta 8080 | cerca de 60 MB |

- Builds em duas fases: as ferramentas de compilação não ficam na imagem final.
- As dependências ficam numa camada própria e só voltam a ser descarregadas
  quando o `pom.xml` ou o `package-lock.json` mudam.
- `.dockerignore`: o do Backend é uma lista de permissões (`mvnw`, `.mvn/`,
  `pom.xml`, `src/`); ambos excluem qualquer `.env`.
- Os testes do Backend não correm no `docker build`, porque precisam de
  Docker (Dev Services). Correm na pipeline, antes das imagens.

## 7. Integração contínua (GitHub Actions)

Workflow: [`.github/workflows/ci.yml`](../.github/workflows/ci.yml). Corre em
cada `push` e `pull_request` para `main`, e manualmente (`workflow_dispatch`).
Os resultados estão no separador **Actions** do repositório.

```
backend-tests ──┐
                ├──> docker-build
frontend-checks ┘
```

| Job | Verificações |
|-----|--------------|
| `backend-tests` | Java 21; `./mvnw -B test` (testes unitários e de integração, com o PostgreSQL dos Dev Services) |
| `frontend-checks` | Node 24; `npm ci`, oxlint, `npm test`, `npm run build` (inclui `tsc -b`), `npm audit --audit-level=high` |
| `docker-build` | Só depois dos dois anteriores (`needs`). `docker compose config -q`, `build`, `up --wait`; smoke tests (health do Backend, página inicial, `/api/auth/me` → 401 através do nginx); teste E2E com o Firefox contra a stack; `down -v` no fim |

**Segurança da pipeline:**

- `permissions: contents: read` em todo o workflow; `persist-credentials: false`
  no checkout.
- **Sem segredos:** o `docker-build` cria um `backend/.env` a partir do
  `.env.example`, com palavras-passe geradas por `openssl rand` em cada
  execução e escondidas nos logs (`::add-mask::`). Desaparecem com a máquina
  do runner.
- Só actions oficiais (`actions/checkout`, `setup-java`, `setup-node`,
  `upload-artifact`), referidas pelo **SHA do commit**, com a versão em
  comentário. Para as atualizar, é preciso trocar o SHA à mão.
- Em caso de falha, ficam os logs do Compose e, durante 7 dias, os relatórios
  dos testes (`relatorios-backend`, `relatorio-e2e`).

**Validação da pipeline (2026-11-13):** a pipeline ficou verde em `2a42d56`.
Para confirmar que bloqueia, foi introduzida uma falha controlada em
`TarefaTest` (`0f62fae`): o `backend-tests` falhou e o `docker-build` não
correu. A falha foi revertida (`01f9cfd`) e a pipeline voltou a verde.

## 8. Limitações

- **Só HTTP.** O HTTPS exigido em produção (S02) depende do alojamento, que
  continua pendente (D04). Com HTTPS, definir `MATRIZ_COOKIE_SEGURO=true`.
- **Sem deployment nem registry:** as imagens só são construídas localmente e
  na pipeline; não são publicadas.
- **Imagem `postgres:17`** sem versão exata, como desde o I0; as imagens da
  Matriz usam versões exatas.
- **Sem análise de vulnerabilidades das imagens** (por exemplo, Trivy) e sem
  auditoria das dependências do Maven; só `npm audit`.
- O E2E da pipeline tem um teste (criar tarefa); o resto do comportamento é
  coberto pelos testes do Backend e do Frontend.

## 9. Troubleshooting

Problemas encontrados na Ficha PL 05 e problemas frequentes:

| Problema | Causa | Diagnóstico / solução |
|----------|-------|-----------------------|
| Build do Backend: "Failed to validate Maven distribution SHA-256" | Sem `unzip`, o `mvnw` descarrega o `.tar.gz` do Maven, mas a soma configurada é a do `.zip` | Instalar `unzip` na fase de compilação (já está no `backend/Dockerfile`). A soma está certa: não a remover |
| "address already in use" ao publicar uma porta | Outro processo no host usa a porta (nesta máquina, a 8080) | `ss -ltnp \| grep <porta>`; por isso o Frontend usa a 8090 |
| Testes do Backend falham na CI com `SRCFG00040 … matriz.admin.email` | Os testes dependiam do `backend/.env` local; sem ele, o Quarkus não arrancava | Corrigido em `2a42d56`: as propriedades são opcionais. Para reproduzir, correr os testes numa cópia sem `.env` |
| Um serviço fica `unhealthy` ou não arranca | Erro no arranque ou BD indisponível | `docker compose ps`; `docker compose logs backend` (ou `postgres`, `frontend`) |
| O Backend não liga à BD | Credenciais diferentes das do volume: o `POSTGRES_PASSWORD` só é aplicado quando o volume é criado | Para mudar a palavra-passe num volume que já existe: `ALTER USER` dentro da BD, ou recriar o volume (`down -v`, apaga os dados) |
| Alterações ao código não aparecem | Imagem antiga | `docker compose up -d --build` |
| Os dados desapareceram | Volume removido (`down -v`) ou nome de projeto diferente | `docker volume ls`: deve existir `esmad-matriz_matriz-dados` |
| Página em branco ou 404 numa rota como `/projetos/<id>` | nginx sem o fallback para o `index.html` | Confirmar `try_files … /index.html` em `frontend/nginx.conf` |
| "can not modify /etc/nginx/conf.d/default.conf (read-only file system?)" nos logs do nginx | O script da imagem tenta acrescentar escuta IPv6 e o sistema de ficheiros é só de leitura | Inofensivo: a configuração é a da Matriz |
