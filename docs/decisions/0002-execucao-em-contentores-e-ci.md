# 0002 — Execução em contentores e integração contínua

## Estado

Aceite (2026-11-13). Alargada pela [ADR 0005](0005-pipeline-em-etapas-e-publicacao-no-docker-hub.md)
(2026-11-16): a pipeline passou a ter 5 etapas e a publicar as imagens no
Docker Hub, com um segredo; a consequência "a pipeline não guarda nem expõe
segredos" deixou de se aplicar. O endereço `backend:8081` do nginx passou a
ser configurável (`MATRIZ_API_URL`) com a
[ADR 0006](0006-alojamento-render-e-neon.md) (2026-11-16).

## Contexto

Na Ficha PL 05, a Matriz tem de poder ser construída, testada e executada de
forma reproduzível, com Docker, Docker Compose e GitHub Actions. Até aqui, só
o PostgreSQL corria em Docker; o Backend e o Frontend corriam no host, em
modo de desenvolvimento.

Fatores relevantes:

- **Mesma origem:** a sessão usa um cookie `HttpOnly`, `SameSite=Strict`,
  `Path=/api` (D06). Em desenvolvimento, o proxy do Vite põe Frontend e
  Backend na mesma origem, para não precisar de CORS.
- **Configuração:** já existe um único `backend/.env` (nunca versionado) e um
  `backend/.env.example` público (S08).
- **Testes:** os testes de integração do Backend precisam de Docker (Quarkus
  Dev Services, D11); existe um teste E2E com o Playwright.
- **Segurança da pipeline:** permissões mínimas, sem segredos expostos, e o
  risco de actions de terceiros na cadeia de fornecimento.

## Opções consideradas e decisões

**1. Como servir o Frontend no contentor**

- A. **nginx** serve o build do Vite e reencaminha `/api/` para o Backend.
- B. Vite em modo de desenvolvimento dentro do contentor.
- C. O Quarkus serve os ficheiros do build, numa só imagem.

Escolhida a **A**: mantém a mesma origem (o cookie funciona sem CORS), serve
ficheiros estáticos, como numa entrega real, e mantém Frontend e Backend em
imagens separadas. A B não é uma entrega a sério, e a C junta os dois builds.

**2. Onde fica a configuração local**

- A. Manter o `backend/.env` como único ficheiro; o Compose lê-o com `env_file`.
- B. Passar para um `.env` na raiz, como no material da aula.

Escolhida a **A**: o modo de desenvolvimento não muda, e há só um ficheiro
para configurar e proteger.

**3. Que palavras-passe usa a pipeline**

- A. Geradas ao acaso em cada execução (`openssl rand`), escondidas nos logs.
- B. GitHub Secrets criados à mão.
- C. Um valor fixo escrito no `ci.yml`.

Escolhida a **A**: a base de dados da pipeline é temporária e destruída no
fim do job, por isso não há segredo a guardar nem a gerir, e nada fica
versionado. Os testes do Backend não precisam de palavras-passe.

**4. Como referir as actions**

- A. Pelo SHA do commit, com a versão em comentário.
- B. Pela tag da versão principal (`@v7`).

Escolhida a **A**: o SHA é imutável; uma tag pode ser movida para outro
código por quem a controla.

Decisões complementares:

- **Um só `compose.yaml`** para a aplicação completa e para o modo de
  desenvolvimento (`docker compose up -d postgres`). O PostgreSQL continua a
  publicar `127.0.0.1:5432` para o Backend no host.
- **Frontend em `127.0.0.1:8090`**; o Backend não publica porta.
- **Redes separadas:** `data` (Backend ↔ BD) e `web` (Frontend ↔ Backend).
- **Hardening** do Backend e do Frontend: `no-new-privileges`, `cap_drop: ALL`,
  `read_only` com `tmpfs: /tmp` e `pids_limit`.
- **E2E na pipeline**, contra a stack do Compose, depois dos testes e do build.

## Consequências

- ✅ A aplicação completa arranca com `docker compose up -d --build`, sem Java
  nem Node.js no host.
- ✅ Cada push para `main` é testado, e as imagens só são construídas depois
  de os testes passarem.
- ✅ A pipeline não guarda nem expõe segredos.
- ✅ A CI encontrou um problema que os testes locais escondiam: os testes do
  Backend dependiam do `backend/.env` (corrigido em `2a42d56`).
- ❌ O nginx é mais um componente a manter (versão, `nginx.conf`).
- ❌ O endereço `backend:8081` está fixo no `nginx.conf`: só funciona numa rede
  Docker onde o Backend tenha esse nome.
- ❌ As actions referidas por SHA não recebem correções sozinhas: é preciso
  atualizá-las à mão.
- ❌ Configuração para execução local, só com HTTP; o HTTPS (S02) depende do
  alojamento (D04, pendente).

Detalhe em [deployment.md](../deployment.md).
