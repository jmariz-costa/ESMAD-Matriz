# Execução com Docker, integração contínua e produção — Matriz

Como construir, executar e validar a Matriz em contentores, e o que a pipeline
de integração contínua verifica. Preparado na Ficha PL 05. As decisões estão
no [ADR 0002](decisions/0002-execucao-em-contentores-e-ci.md) e o diagrama de
execução em [architecture.md](architecture.md) §5.

O Docker Compose (§1 a §6) serve para **execução local e validação**, só com
HTTP. A produção corre no Render e no Neon, com HTTPS, a partir das imagens
publicadas pela pipeline (§8, [ADR 0006](decisions/0006-alojamento-render-e-neon.md)).
Limitações em [§9](#9-limitações).

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
| `POSTGRES_*`, `MATRIZ_ADMIN_*`, `MATRIZ_SEED_DEMO`, `MATRIZ_DEMO_PASSWORD`, `MATRIZ_COOKIE_SEGURO`, `MATRIZ_DOMINIO_EMAIL` (opcional) | `backend/.env` | Os valores do `.env.example` são públicos: **mudar as palavras-passe** |
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
  (`frontend/nginx.conf.template`, variável `MATRIZ_API_URL`), tal como o
  proxy do Vite em desenvolvimento. Por isso, o cookie de sessão funciona sem
  CORS.
- **Volume:** `matriz-dados`, com o nome de projeto fixo `esmad-matriz` (o
  volume chama-se `esmad-matriz_matriz-dados`, seja qual for o nome da pasta).
- **Só localmente:** todas as portas publicadas estão em `127.0.0.1`.
- **Reinício:** `restart: unless-stopped` nos três serviços.
- **Nomes dos contentores:** dados pelo Compose a partir do nome do projeto
  (`esmad-matriz-postgres-1`, `esmad-matriz-backend-1`…). Outra instalação
  pode coexistir com `docker compose -p <outro-nome> …`, com volume próprio;
  como as portas do host são fixas, só uma de cada vez pode estar a correr.

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
Os resultados estão no separador **Actions** do repositório. As decisões
estão na [ADR 0002](decisions/0002-execucao-em-contentores-e-ci.md) e na
[ADR 0005](decisions/0005-pipeline-em-etapas-e-publicacao-no-docker-hub.md).
Os workflows de release e de deploy em produção (`release.yml` e
`deploy-prod.yml`) estão no §8.

A pipeline tem 5 etapas. Cada etapa só começa quando as anteriores passam
(quality gates):

```
Etapa 1 Build      Etapa 2 Testes           Etapa 3      Etapa 4                Etapa 5
build-backend  ─┬─> testes-unitarios ──┐
                └─> testes-integracao ─┴─> cobertura ─┐
                                                      ├─> imagens ───────────> publicar
build-frontend ───> testes-frontend ──────────────────┘   (smoke tests + E2E)   (só no main)
```

| Etapa | Job | Verificações |
|-------|-----|--------------|
| 1 | `build-backend` | Java 21; `./mvnw -B package -DskipTests` |
| 1 | `build-frontend` | Node 24; `npm ci`, oxlint, `npm run build` (inclui `tsc -b`), `npm audit --audit-level=high` |
| 2 | `testes-unitarios` | Primeiro, num passo próprio, as regras de arquitetura do ADR 0001 (`./mvnw -B test -Dgroups=arquitetura`: `ArquiteturaTest`, com ArchUnit). Depois, `./mvnw -B test -DexcludedGroups=integracao,arquitetura`: os restantes testes sem `@Tag("integracao")`, sem Quarkus nem base de dados. Guarda `jacoco-unit.exec` |
| 2 | `testes-integracao` | `./mvnw -B test -Dgroups=integracao`: os `@QuarkusTest`, com o PostgreSQL dos Dev Services. Guarda `jacoco-integracao.exec` |
| 2 | `testes-frontend` | `npm ci`, `npm test` (Vitest) |
| 3 | `cobertura` | Junta os dois ficheiros de cobertura (`jacoco:merge`), gera o relatório (`jacoco:report`) e **falha abaixo de 95% de linhas ou de ramos** (`jacoco:check`). Mostra a tabela da cobertura no resumo da execução e guarda o relatório HTML durante 14 dias (`relatorio-cobertura`) |
| 4 | `imagens` | `docker compose config -q`, `build`, `up --wait`; smoke tests (health do Backend, página inicial, `/api/auth/me` → 401 através do nginx); teste E2E com o Firefox contra a stack; `down -v` no fim. No `main`, exporta as imagens testadas (`docker save`) para a etapa 5 |
| 5 | `publicar` | Só em `push` para o `main`. Carrega as imagens da etapa 4 (as mesmas que passaram no E2E, sem as construir de novo) e publica-as no Docker Hub |

Nos testes do Backend, a CI copia o `backend/.env.example` para
`backend/.env`, como num computador de desenvolvimento: o Admin e os dados de
demonstração são criados no arranque dos testes, com os valores fictícios do
exemplo. A base de dados dos testes é temporária.

**Cobertura (JaCoCo).** Configurada no `backend/pom.xml`
(`jacoco-maven-plugin` e extensão `quarkus-jacoco`, que mede as classes que o
Quarkus transforma nos `@QuarkusTest`). Localmente, `./mvnw verify` corre
todos os testes, gera o relatório em `backend/target/site/jacoco/index.html`
e verifica os mínimos (propriedades `cobertura.minima.linhas` e
`cobertura.minima.ramos`, ambas 0.95).

**Imagens publicadas (Docker Hub):**

| Imagem | Etiquetas |
|--------|-----------|
| `<utilizador>/matriz-backend` | SHA curto do commit (ex.: `06e1351`) e `latest` |
| `<utilizador>/matriz-frontend` | SHA curto do commit e `latest` |

`<utilizador>` é a conta do Docker Hub (secret `DOCKERHUB_USERNAME`). No
plano gratuito, os repositórios do Docker Hub são **públicos**. As imagens não
têm nenhum `.env` nem palavras-passe (`.dockerignore`): a configuração é dada
no arranque, por variáveis de ambiente.

**Segurança da pipeline:**

- `permissions: contents: read` em todo o workflow; `persist-credentials: false`
  no checkout.
- **Um único segredo:** o token do Docker Hub, em *repository secrets*
  (`DOCKERHUB_USERNAME` e `DOCKERHUB_TOKEN`, token com permissão Read & Write).
  Só o job `publicar` o usa, e só em `push` para o `main`; nos pull requests
  não é lido. O login usa `docker login --password-stdin` e termina com
  `docker logout`.
- As palavras-passe da stack da etapa 4 são geradas por `openssl rand` em cada
  execução e escondidas nos logs (`::add-mask::`). Desaparecem com a máquina
  do runner.
- Só actions oficiais (`actions/checkout`, `setup-java`, `setup-node`,
  `upload-artifact`, `download-artifact`), referidas pelo **SHA do commit**,
  com a versão em comentário. Para as atualizar, é preciso trocar o SHA à mão.
- Num pull request, um push novo cancela a execução anterior; no `main` não,
  para não interromper uma publicação a meio.
- Em caso de falha, ficam os logs do Compose e, durante 7 dias, os relatórios
  dos testes (`relatorios-unitarios`, `relatorios-integracao`, `relatorio-e2e`).

**Validação da pipeline (2026-11-13):** a pipeline ficou verde em `2a42d56`.
Para confirmar que bloqueia, foi introduzida uma falha controlada em
`TarefaTest` (`0f62fae`): o `backend-tests` falhou e o `docker-build` não
correu. A falha foi revertida (`01f9cfd`) e a pipeline voltou a verde.

**Validação das 5 etapas (2026-11-16):** em `7e82e7c`, a etapa 3 falhou
(90% de linhas e 91% de ramos), e as etapas 4 e 5 não correram: sem
`backend/.env` na CI, o Admin e os dados de demonstração não eram criados nos
testes. Corrigido em `06e1351` (cópia do `.env.example`, ver acima); a
pipeline ficou verde e as imagens foram publicadas no Docker Hub.

## 8. Produção: Render e Neon

Decisões na [ADR 0006](decisions/0006-alojamento-render-e-neon.md).

```
browser ──HTTPS──> matriz-frontend (Render, nginx) ──/api, HTTPS──> matriz-backend (Render, Quarkus) ──SSL──> Neon (PostgreSQL 17)
```

| Componente | Onde | Endereço |
|------------|------|----------|
| Frontend | Render, serviço web `matriz-frontend` (imagem `jmariz/matriz-frontend`), Frankfurt, plano gratuito | https://matriz-frontend-tbdh.onrender.com |
| Backend | Render, serviço web `matriz-backend` (imagem `jmariz/matriz-backend`), Frankfurt, plano gratuito | https://matriz-backend-eeka.onrender.com (só usado pelo Frontend) |
| Base de dados | Neon, PostgreSQL 17, AWS Europe Central 1 (Frankfurt), plano gratuito | Endereço direto (sem `-pooler`), só no Render |

**Configuração.** Os serviços estão definidos no Blueprint
[`render.yaml`](../render.yaml) (Blueprint `matriz` no Render, com a
sincronização automática desligada). Os valores fixos estão no ficheiro; os
secretos (`sync: false`) foram preenchidos no painel do Render e só lá
existem:

| Variável | Serviço | Valor |
|----------|---------|-------|
| `MATRIZ_DB_HOST`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Backend | Ligação ao Neon (painel) |
| `MATRIZ_DB_SSLMODE` | Backend | `verify-full` |
| `MATRIZ_COOKIE_SEGURO` | Backend | `true` |
| `MATRIZ_ADMIN_EMAIL`, `MATRIZ_ADMIN_PASSWORD` | Backend | Administrador (painel) |
| `MATRIZ_SEED_DEMO`, `MATRIZ_DEMO_PASSWORD` | Backend | `true`; palavra-passe de produção das contas de demonstração (painel) |
| `MATRIZ_DOMINIO_EMAIL` | Backend | `empresa.pt`: só se criam contas de Membro com email deste domínio (US21). Opcional; definida no painel a 2026-11-18, aplica-se a partir do Deploy prod da `v0.2.0` |
| `MATRIZ_API_URL` | Frontend | `https://matriz-backend-eeka.onrender.com`, sem barra no fim (painel) |
| `PORT` | Ambos | `8081` (Backend) e `8080` (Frontend): porta dentro do contentor |

As palavras-passe de produção são diferentes das do `.env.example` e das
locais. As contas de demonstração só são criadas no primeiro arranque: mudar
depois o `MATRIZ_DEMO_PASSWORD` não muda as contas que já existem.

**Release e deploy.** Publicar e pôr em produção são ações separadas:

```
push para o main ──> pipeline (ci.yml) ──> imagens :<sha> e :latest no Docker Hub
release vX publicada no GitHub ──> workflow "Release" (release.yml) ──> etiqueta :vX nas imagens desse commit
Run workflow "Deploy prod" (deploy-prod.yml), release vX ──> Render passa a correr :vX
```

1. **Criar a release** no GitHub (Releases → Draft a new release), com uma
   tag `v…` num commit do `main` em que a pipeline passou.
2. O workflow **Release** corre sozinho: espera pelas imagens desse commit
   (até 20 minutos) e acrescenta-lhes a etiqueta da versão com
   `docker buildx imagetools create`, sem as descarregar nem construir.
3. No separador Actions, **Deploy prod** → Run workflow → escrever a release
   (por exemplo `v0.2.0-rc.1`). O workflow confirma que a release existe e
   não é rascunho, confirma as imagens `:vX`, chama os deploy hooks do Render
   com a imagem exata (`imgURL`) e espera até a produção responder (página
   200 e `/api/auth/me` 401, até 15 minutos).

Secrets e variáveis do GitHub usados por estes workflows:

| Nome | Tipo | Uso |
|------|------|-----|
| `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN` | Secrets | Etiquetar as imagens (Release) |
| `RENDER_DEPLOY_HOOK_BACKEND`, `RENDER_DEPLOY_HOOK_FRONTEND` | Secrets | Deploy no Render (Deploy prod). São URLs com uma chave: quem os tiver consegue fazer deploy |
| `MATRIZ_PROD_URL` | Variável | Endereço do Frontend, para a verificação final (Deploy prod) |

**Validação (2026-11-16):** os dois serviços foram criados pelo Blueprint e
ficaram *live* com as imagens do `230179e`; o Backend criou o esquema no
Neon, o Administrador e os dados de demonstração; entrar como a Ana no
endereço do Frontend funcionou.

**Primeira release em produção (2026-11-17):** a release `v0.2.0-rc.1` foi
posta em produção com o "Deploy prod".

**Antes de uma apresentação:** abrir o endereço do Frontend alguns minutos
antes, para acordar os dois serviços (plano gratuito).

## 9. Limitações

- **Docker Compose só com HTTP.** Localmente não há HTTPS; em produção, o
  Render serve por HTTPS e o cookie leva `Secure` (§8).
- **Plano gratuito em produção:** os serviços do Render adormecem ao fim de
  15 minutos sem pedidos e demoram cerca de 1 minuto a acordar; não há
  cópias de segurança da base de dados configuradas por nós.
- **O nginx resolve o endereço do Backend no arranque:** se o Render mudar os
  endereços IP do Backend, o Frontend só os vê quando voltar a arrancar.
- **O "Deploy prod" não confirma a versão** que ficou a correr, só que a
  produção responde; a versão vê-se nos Events de cada serviço no Render.
- **Imagem `postgres:17`** sem versão exata, como desde o I0; as imagens da
  Matriz usam versões exatas.
- **Sem análise de vulnerabilidades das imagens** (por exemplo, Trivy) e sem
  auditoria das dependências do Maven; só `npm audit`.
- O E2E da pipeline tem um teste (criar tarefa); o resto do comportamento é
  coberto pelos testes do Backend e do Frontend.
- **Autenticação "trust" dentro do contentor do PostgreSQL** (aviso do
  `initdb` no primeiro arranque): as ligações feitas de dentro do contentor
  (socket local ou `127.0.0.1` do próprio contentor, com `docker exec`) não
  pedem palavra-passe; as ligações pela rede Docker e pela porta publicada no
  host pedem (verificado com uma palavra-passe errada, recusada). Quem
  consegue usar `docker exec` já controla o Docker do host, por isso ficou
  registado como limitação (Ficha PL 05, V1).
- **Portas do host fixas** (8090 e 5432): duas instalações não podem correr
  ao mesmo tempo nesta máquina.

## 10. Troubleshooting

Problemas encontrados na Ficha PL 05 e problemas frequentes:

| Problema | Causa | Diagnóstico / solução |
|----------|-------|-----------------------|
| Build do Backend: "Failed to validate Maven distribution SHA-256" | Sem `unzip`, o `mvnw` descarrega o `.tar.gz` do Maven, mas a soma configurada é a do `.zip` | Instalar `unzip` na fase de compilação (já está no `backend/Dockerfile`). A soma está certa: não a remover |
| "address already in use" ao publicar uma porta | Outro processo no host usa a porta (nesta máquina, a 8080) | `ss -ltnp \| grep <porta>`; por isso o Frontend usa a 8090 |
| Testes do Backend falham na CI com `SRCFG00040 … matriz.admin.email` | Os testes dependiam do `backend/.env` local; sem ele, o Quarkus não arrancava | Corrigido em `2a42d56`: as propriedades são opcionais. Para reproduzir, correr os testes numa cópia sem `.env` |
| Cobertura abaixo do mínimo na CI, mas acima localmente | O Quarkus também lê o `backend/.env` nos testes: localmente, o Admin e os dados de demonstração correm; numa máquina sem `.env`, não | Corrigido em `06e1351` (a CI copia o `.env.example`). Para medir como a CI, correr os testes numa cópia limpa (`git clone`) com o `.env.example` copiado para `.env` |
| Um serviço fica `unhealthy` ou não arranca | Erro no arranque ou BD indisponível | `docker compose ps`; `docker compose logs backend` (ou `postgres`, `frontend`) |
| O Backend não liga à BD | Credenciais diferentes das do volume: o `POSTGRES_PASSWORD` só é aplicado quando o volume é criado | Para mudar a palavra-passe num volume que já existe: `ALTER USER` dentro da BD, ou recriar o volume (`down -v`, apaga os dados) |
| Produção: a página abre, mas entrar dá erro (502/504) | `MATRIZ_API_URL` do Frontend errado (o Render acrescenta um sufixo ao nome do serviço) ou Backend a acordar | Confirmar no Render o endereço do Backend e o `MATRIZ_API_URL` (sem barra no fim); no plano gratuito, esperar cerca de 1 minuto e tentar de novo |
| Produção: o Backend não arranca, com `PKIX` ou `certificate` nos logs | O certificado do Neon não foi aceite com `verify-full` | Confirmar o host (sem `-pooler`); em último caso, `MATRIZ_DB_SSLMODE=require` (SSL sem confirmar o servidor) |
| Deploy prod: "Não existe a imagem matriz-…:vX" | O workflow Release dessa release falhou ou ainda está a correr | Ver o workflow Release no separador Actions; repeti-lo com Run workflow e o nome da release |
| Alterações ao código não aparecem | Imagem antiga | `docker compose up -d --build` |
| Os dados desapareceram | Volume removido (`down -v`) ou nome de projeto diferente | `docker volume ls`: deve existir `esmad-matriz_matriz-dados` |
| `initdb: warning: enabling "trust" authentication for local connections` nos logs do PostgreSQL | Comportamento normal da imagem oficial no primeiro arranque | Ver §9 (limitação V1) |
| Página em branco ou 404 numa rota como `/projetos/<id>` | nginx sem o fallback para o `index.html` | Confirmar `try_files … /index.html` em `frontend/nginx.conf.template` |
| "Conflict. The container name … is already in use" ao arrancar uma segunda instalação | Até à `0.1.0-rc.1`, o PostgreSQL tinha o nome fixo `matriz-postgres` | Corrigido: o nome é dado pelo Compose. Usar `docker compose -p <outro-nome>` para a segunda instalação |
| "can not modify /etc/nginx/conf.d/default.conf (read-only file system?)" nos logs do nginx | O script da imagem tenta acrescentar escuta IPv6 e o sistema de ficheiros é só de leitura | Inofensivo: a configuração é a da Matriz |
