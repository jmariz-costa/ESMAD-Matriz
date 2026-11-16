# 0005 — Pipeline em etapas, cobertura mínima e publicação das imagens no Docker Hub

## Estado

Aceite (2026-11-16). Alarga a [ADR 0002](0002-execucao-em-contentores-e-ci.md):
a pipeline passa a ter um segredo (o token do Docker Hub), por isso a
consequência "a pipeline não guarda nem expõe segredos" da ADR 0002 deixa de
se aplicar.

## Contexto

Depois da Ficha PL 05, a pipeline tinha três jobs (`backend-tests`,
`frontend-checks` e `docker-build`) e as imagens só eram construídas, sem
serem publicadas. Para preparar o deploy numa cloud, as imagens têm de estar
num registry de onde o alojamento as possa obter.

O objetivo foi separar a pipeline em 5 etapas, cada uma um quality gate:
(1) build do Frontend e do Backend; (2) testes unitários, de integração e do
Frontend; (3) cobertura com JaCoCo; (4) criação das imagens; (5) publicação.

Fatores relevantes:

- **Testes do Backend:** 46 unitários e 123 de integração (`@QuarkusTest`,
  com PostgreSQL dos Dev Services), todos no mesmo `./mvnw test`.
- **E2E:** precisa da aplicação a correr, ou seja, das imagens.
- **Segredos:** até aqui, a pipeline não usava nenhum (ADR 0002, decisão 3).
- **Repositório privado:** uma imagem pública expõe a aplicação compilada.
- **Configuração dos testes:** o Quarkus também lê o `backend/.env` nos
  testes; na CI, esse ficheiro não existe.

## Opções consideradas e decisões

**1. Onde corre o E2E**

- A. Depois da criação das imagens, contra as imagens que vão ser publicadas.
- B. Na etapa dos testes, construindo as imagens só para o E2E.

Escolhida a **A**: testa exatamente o que é publicado e não constrói as
imagens duas vezes. A etapa 5 publica as imagens da etapa 4 (`docker save` e
`docker load`), sem as construir de novo.

**2. Registry das imagens**

- A. **Docker Hub.**
- B. GitHub Container Registry (GHCR), como no Desafio C da Ficha PL 05.
- C. Não publicar.

Escolhida a **A**, por decisão do utilizador: é o registry por omissão do
Docker e os alojamentos obtêm as imagens públicas sem configuração extra. A B
não precisava de segredos (usa o `GITHUB_TOKEN`) e as imagens ficariam
privadas como o repositório; a C não permite o deploy a partir de imagens.

**3. Quando se publica**

- A. Em cada push para o `main`, com as etiquetas SHA curto do commit e
  `latest`; nos pull requests, as imagens são construídas e testadas, mas não
  publicadas.
- B. Só quando é criada uma tag `v*`.

Escolhida a **A**: ainda não há versão nova planeada (a `0.1.0-rc.1` é a
última), e com a B nada seria publicado até lá. O SHA permite saber de que
commit vem cada imagem.

**4. Cobertura (JaCoCo)**

- A. Só gerar o relatório, sem bloquear.
- B. Gerar o relatório e falhar abaixo de um mínimo.

Escolhida a **B**, com mínimo de **95% de linhas e 95% de ramos**. Os ramos
estavam em 89,7%: foram escritos 66 testes unitários (validação dos pedidos,
paginação, identificadores, arranque) até 95,6%, o que também equilibrou a
pirâmide de testes (112 unitários e 123 de integração). Foi necessária a
extensão `quarkus-jacoco`: sem ela, as classes transformadas pelo Quarkus não
eram contadas e a cobertura caía para 56%.

**5. Configuração dos testes na CI**

A primeira execução falhou na etapa 3 (90% de linhas, 91% de ramos): sem
`backend/.env`, o Admin e os dados de demonstração não eram criados nos
testes, ao contrário do que acontecia localmente.

- A. Copiar o `backend/.env.example` para `backend/.env` antes dos testes.
- B. Fixar a configuração no `pom.xml` (independente do `.env`) e escrever um
  teste próprio para o Admin e os dados de demonstração.
- C. Excluir os dados de demonstração da cobertura.

Escolhida a **A**, por decisão do utilizador: reutiliza um ficheiro que já
existe e faz a CI correr como um computador de desenvolvimento. Os valores
são fictícios e a base de dados dos testes é temporária.

**6. Onde fica o token do Docker Hub**

- A. *Repository secrets* (`DOCKERHUB_USERNAME` e `DOCKERHUB_TOKEN`).
- B. *Environment secrets*, num environment limitado ao `main`.

Escolhida a **A**, pelo utilizador: é a forma mais simples, e num
repositório privado os environments só existem nos planos pagos do GitHub
(Pro, Team ou Enterprise). Para compensar, só o job `publicar` lê os secrets,
e só em push para o `main`.

## Consequências

- ✅ Cada etapa é um job próprio: no separador Actions vê-se logo onde falhou.
- ✅ As imagens publicadas são as mesmas que passaram nos smoke tests e no
  E2E.
- ✅ A cobertura é verificada em cada push, e o relatório fica disponível
  durante 14 dias.
- ✅ Testes unitários e de integração correm em paralelo e em separado
  (`@Tag("integracao")`).
- ❌ A pipeline passa a ter um segredo. Fica cifrado no GitHub, escondido nos
  logs e não entra nas imagens, mas é preciso renová-lo se o token expirar ou
  for revogado.
- ❌ As imagens são **públicas** no Docker Hub (plano gratuito): quem as
  descarregar fica com a aplicação compilada de um repositório privado. Não
  levam `.env` nem palavras-passe.
- ❌ A cobertura do Admin e dos dados de demonstração é incidental: esse
  código corre no arranque dos testes, mas nenhum teste verifica o que cria.
  Se o seed for desligado no `.env.example`, a cobertura desce.
- ❌ A margem nos ramos é pequena (95,6%): código novo sem testes pode fazer
  a pipeline falhar.
- ❌ A pipeline demora mais: há mais jobs e cada um prepara o seu ambiente
  (Java ou Node, dependências).

Detalhe em [deployment.md](../deployment.md) §7.
