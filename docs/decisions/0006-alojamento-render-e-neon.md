# 0006 — Alojamento: Render (Frontend e Backend) e Neon (PostgreSQL)

## Estado

Aceite (2026-11-16). Resolve a decisão pendente **D04** (alojamento,
[constraints.md](../constraints.md) §4). Usa as imagens publicadas no Docker
Hub pela pipeline ([ADR 0005](0005-pipeline-em-etapas-e-publicacao-no-docker-hub.md)).

## Contexto

Até aqui, a Matriz só corria localmente (Docker Compose, só HTTP). Para a
apresentação final, tem de estar acessível na Internet, com HTTPS (S02).

Fatores relevantes:

- **Escolha do utilizador:** Render para alojar o Frontend e o Backend, e
  Neon para o PostgreSQL; já tinha conta nas duas plataformas.
- **Mesma origem:** o cookie de sessão é `HttpOnly`, `SameSite=Strict` e
  `Path=/api` (D06). No Compose, o nginx do Frontend reencaminha `/api` para
  `backend:8081`, um nome que só existe na rede Docker (ADR 0002).
- **Imagens:** a pipeline já publica imagens testadas no Docker Hub, públicas
  (ADR 0005).
- **Neon:** exige SSL na ligação.
- **Plano gratuito:** no Render, um serviço adormece ao fim de 15 minutos sem
  pedidos e demora cerca de 1 minuto a acordar; no Neon, a base de dados
  suspende e acorda em segundos.
- **Controlo do que vai para produção:** a pipeline publica uma imagem em
  cada push para o `main`, mas nem todos os commits devem ir para produção.

## Opções consideradas e decisões

**1. Como servir o Frontend no Render**

- A. A imagem `matriz-frontend` do Docker Hub, com o nginx a reencaminhar
  `/api` para o endereço público do Backend.
- B. Static Site do Render, com uma regra que reencaminha `/api/*` para o
  Backend.
- C. O Frontend chama o Backend diretamente, noutro domínio.

Escolhida a **A**: mantém a mesma origem (o cookie funciona sem CORS e a D06
não muda) e põe em produção as imagens que a pipeline testa. Para isso, o
destino do `/api` passou a ser configurável (`MATRIZ_API_URL`,
`frontend/nginx.conf.template`). A B não adormecia, mas não usava a imagem; a
C exigia CORS e um cookie `SameSite=None`.

**2. Como se faz o deploy**

- A. Um workflow próprio, "Deploy prod", corrido à mão, em que se escolhe uma
  **release** do GitHub.
- B. Uma etapa 6 na pipeline, com deploy em cada push para o `main`.
- C. Deploy manual no painel do Render.

Escolhida a **A**, por decisão do utilizador: publicar e pôr em produção
passam a ser ações separadas. Quando uma release (por exemplo, `v0.2.0-rc.1`)
é publicada, o workflow "Release" acrescenta a etiqueta da versão às imagens
desse commit, sem build novo; o "Deploy prod" confirma a release e as
imagens e chama os deploy hooks do Render com a imagem exata (`imgURL`).

**3. Dados de demonstração em produção**

- A. Ligados, com uma palavra-passe nova.
- B. Desligados, só com o Administrador.

Escolhida a **A**: a apresentação mostra a Matriz com dados (projetos,
tarefas e as contas da Ana, do Bruno, da Carla e do Daniel). As palavras-passe
de produção são diferentes das do `.env.example` e das locais.

**4. Como se configura o Render**

- A. Um Blueprint (`render.yaml`) no repositório.
- B. Tudo à mão no painel.

Escolhida a **A**: a configuração fica versionada e revista como código. Os
valores secretos (palavras-passe e ligação ao Neon) estão marcados com
`sync: false` e foram preenchidos no painel. A sincronização automática do
Blueprint foi desligada, para que uma alteração ao `render.yaml` não faça
deploy da etiqueta `latest` por cima da release escolhida.

**5. Região**

- A. Render e Neon em Frankfurt.
- B. Neon em Londres e Render em Frankfurt (o Render não tem região em
  Londres).

Escolhida a **A**: cada pedido faz várias consultas à base de dados, e a
distância entre regiões somava-se a cada uma. Um primeiro projeto no Neon
foi criado em Londres e substituído por outro em Frankfurt.

**6. SSL na ligação ao Neon**

- A. `verify-full`: SSL obrigatório, com o certificado e o nome do servidor
  confirmados pelos certificados de confiança da JVM.
- B. `require`: SSL obrigatório, sem confirmar o servidor.

Escolhida a **A**, a mais segura. O URL da base de dados passou a ter
`sslmode=${MATRIZ_DB_SSLMODE:prefer}`: localmente fica `prefer` (o PostgreSQL
local não tem SSL), em produção `verify-full`. Usa-se o endereço direto do
Neon, sem `-pooler`, porque o intermediário de ligações do Neon não se dá bem
com o Flyway nem com o driver JDBC.

## Consequências

- ✅ A Matriz está acessível na Internet, com HTTPS do Render; o cookie de
  sessão leva `Secure` (`MATRIZ_COOKIE_SEGURO=true`).
- ✅ Vai para produção exatamente a imagem que passou nos smoke tests e no
  E2E, e só quando alguém escolhe uma release.
- ✅ O Docker Compose local funciona como antes: `MATRIZ_API_URL` tem como
  valor por omissão `http://backend:8081`.
- ✅ A configuração de produção está no `render.yaml`, sem segredos.
- ❌ Plano gratuito: o primeiro pedido depois de 15 minutos sem uso demora
  cerca de 1 minuto. Antes da apresentação, é preciso abrir a aplicação com
  antecedência.
- ❌ Os endereços têm sufixos dados pelo Render
  (`matriz-backend-eeka.onrender.com`, `matriz-frontend-tbdh.onrender.com`);
  o do Backend teve de ser posto à mão no `MATRIZ_API_URL` do Frontend.
- ❌ O nginx resolve o endereço do Backend quando arranca; se o Render mudar
  os endereços IP do Backend, o Frontend só os vê no arranque seguinte (que,
  no plano gratuito, acontece sempre que acorda).
- ❌ Dois segredos novos no GitHub (os deploy hooks do Render): quem os tiver
  consegue fazer deploy.
- ❌ O "Deploy prod" confirma que a produção responde, mas não que versão está
  a correr: durante o deploy, o Render mantém a versão anterior até a nova
  ficar saudável. A versão vê-se nos Events de cada serviço no Render.
- ❌ Os dados de produção estão no Neon (plano gratuito), sem cópias de
  segurança configuradas por nós.

Detalhe em [deployment.md](../deployment.md) §8.
