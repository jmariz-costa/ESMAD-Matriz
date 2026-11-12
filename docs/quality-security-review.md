# Revisão de qualidade e de segurança

Ficha PL 04, Exercício 5. Tem três partes: o Code Review do último incremento (I8), a análise de segurança ao acesso a projetos e tarefas, e a análise estática com as ferramentas configuradas no projeto. Para cada finding regista-se a evidência, o impacto, a severidade e a correção proposta, e se é **confirmado** (observado numa execução real) ou **suspeita** (inferido, sem prova de execução). Ver também o [plano de testes](test-plan.md).

Secções: [âmbito](#âmbito-e-método) · [Code Review](#code-review-do-i8) ·
[Security Review](#security-review) · [análise estática](#análise-estática) ·
[decisões](#decisões-humanas) ·
[versão e pendentes](#versão-revista-e-problemas-pendentes) · [não verificado](#o-que-ficou-por-verificar)

## Âmbito e método

- **Data:** 2026-11-12. O agente (Claude Code) fez a revisão e o utilizador reviu-a.
- **Code Review:** limitado ao diff do I8, `0b61871..1658863`, que foi a decisão 2a do utilizador. O código desse diff é:
  - `backend/src/main/java/pt/esmad/matriz/comum/json/ConfiguracaoJson.java`;
  - `TiposJsonApiTest`, `GravacoesSimultaneasApiTest`;
  - `frontend/src/comum/texto.ts` e os três formulários que passaram a usá-lo.

  O resto do diff é documentação.
- **Security Review:** autorização sobre projetos, tarefas e comentários; dados de entrada; segredos; configuração; dependências.
- **Prompts:** os de apoio da ficha (Ficha 04, l. 130–145) e os dos anexos `PromptCodeReview.txt`, `PromptSecurityReview.txt` e `PromptSecurityChallenge.txt`.
- **Ambiente das verificações manuais:**
  - Backend real em `quarkus:dev`, com o PostgreSQL local, através de `curl` em `http://localhost:8081/api`;
  - contas de demonstração `ana@empresa.pt`, `bruno@empresa.pt` e o Administrador;
  - as palavras-passe foram lidas de `backend/.env` sem serem mostradas.

  Os dados de teste ficaram num projeto temporário da Ana, eliminado no fim (204). Os projetos que já existiam não foram tocados.
- **Severidade:** Critical, High, Medium e Low, como no `PromptCodeReview.txt`.

## Code Review do I8

| ID | Ficheiro e localização | Descrição e evidência | Impacto | Severidade | Estado | Correção proposta | Teste de regressão |
|----|------------------------|-----------------------|---------|------------|--------|-------------------|--------------------|
| Q1 | `backend/src/main/java/pt/esmad/matriz/comum/texto/Texto.java:16` (`strip()`, antes da correção); `frontend/src/comum/texto.ts:20` (`trim()`) | Os dois lados não removem os mesmos caracteres das pontas. O `strip()` do Java não remove U+00A0 (espaço não separável), U+200B (espaço de largura zero) nem U+FEFF; o `trim()` do JavaScript remove U+00A0 e U+FEFF, mas não U+200B. O comentário de `texto.ts:1` diz que as regras são "iguais às do Backend", e não são. Também o `CHECK (btrim(...) <> '')` da base de dados só remove o espaço normal (`V1__esquema_inicial.sql:7, 33, 53, 77`). **Executado:** `POST /api/projetos/{id}/tarefas` com o título feito de 3× U+00A0, de 3× U+200B ou de 3× U+FEFF → **201**, uma tarefa cujo título, à vista, está vazio. Com 3× U+2003 ou espaços normais → 400. | Um cliente que fale diretamente com a API cria tarefas, projetos e comentários com o nome "vazio", o que contraria a RN23 ("um texto obrigatório só com espaços é recusado"). O mesmo `Texto.normalizar` é usado em `Tarefa.java:71-72, 117-118`, `Projeto.java:54-55, 77-78`, `ProjetosServico.java:62` e `Comentario.java:47`. Pela interface, o U+00A0 e o U+FEFF são barrados, mas o U+200B passa. | Low | **Confirmado; corrigido** | No Backend, remover das pontas também os caracteres de espaço Unicode (`Character.isSpaceChar`) e os invisíveis de formatação (categoria Cf: U+200B, U+FEFF…). No Frontend, aplicar a mesma definição em `texto.ts`. | `TextoTest` (novo, unitário) com cada carácter; `CriarConsultarTarefaApiTest` com títulos invisíveis → 400; `texto.test.ts` com os mesmos casos. |
| Q2 | `backend/src/test/java/pt/esmad/matriz/tarefas/GravacoesSimultaneasApiTest.java:56-77` | O teste lança os dois pedidos "ao mesmo tempo" com um `CountDownLatch`, mas nada garante que se sobreponham no servidor. Se forem tratados um depois do outro, o resultado também é 200 + 409. | O teste não prova, de forma determinista, o caminho do bloqueio otimista (`@Version`) dentro da mesma transação. Só o exercita quando os pedidos se cruzam de facto. Correu várias vezes sem falhas, mas uma regressão nesse caminho pode passar despercebida. | Low | Suspeita (qualidade do teste) | Não corrigir agora. O teste unitário `TarefaTest.editarComVersaoAntigaERecusadoSemAlterarNada` cobre a regra. Fica registado como limitação. | — |
| Q3 | `backend/src/main/java/pt/esmad/matriz/comum/json/ConfiguracaoJson.java:20-23` | Só impede que números e booleanos sejam aceites como **texto**. Foi verificado o caso inverso, texto num campo numérico: `versao: "0"` e `versao: 1.5` são recusados por `PedidoTarefa.java:51`; um id numérico (`utilizadorId: 42`) é recusado (`TiposJsonApiTest`). Um objeto num campo de texto (`{"nome": {"a":1}}`) e JSON malformado → 400 `VALIDACAO`, sem pormenores internos. | Nenhum problema encontrado. | — | Verificado, sem defeito | — | — |
| Q4 | Parâmetros de URL das listagens | Um parâmetro desconhecido é ignorado em silêncio: `GET /api/projetos?pesquisa=…` devolve tudo, porque o parâmetro certo é `q`. No corpo, pelo contrário, campos desconhecidos dão 400. | Pode esconder erros de quem usa a API: o próprio agente usou primeiro `pesquisa` e julgou que a pesquisa falhava. Não afeta a segurança, porque a listagem continua limitada aos projetos de que a pessoa é membro. | Low | Confirmado (inconsistência) | Não corrigir. Fica como observação para a Fase 2. | — |

## Security Review

### IDOR / Broken Access Control com duas contas de demonstração

**Cenário:** a Ana cria o projeto temporário P, com a tarefa T e um comentário. O Bruno, autenticado e sem ser membro de P, usa os identificadores de P e de T (cenários T10 e T11 do plano de testes). **Resultado: não vulnerável**, confirmado contra o Backend real.

| Pedido do Bruno | Resultado |
|-----------------|-----------|
| `GET`, `PATCH` e `DELETE /api/projetos/{P}` | 403 `SEM_PERMISSAO` |
| `GET /api/projetos/{P}/membros`; `POST` a adicionar-se a si próprio | 403 |
| `GET` e `POST /api/projetos/{P}/tarefas` | 403 |
| `GET`, `PATCH` (`estado: CONCLUIDA`) e `DELETE /api/tarefas/{T}` | 403 |
| `POST /api/tarefas/{T}/responsaveis` a atribuir-se a si próprio | 403 |
| `GET` e `POST /api/tarefas/{T}/comentarios` | 403 |
| `GET /api/projetos?q=Revisão` | 200 com `total: 0` (P não aparece) |
| Estado depois, visto pela Ana | Tarefa em `A_FAZER`, versão 0, sem responsáveis; nome do projeto igual; só a Ana como membro; o comentário mantém-se |

Os 403 têm só `codigo` e `mensagem`, sem dados do recurso. O **Administrador**, também autenticado, recebe 403 em `GET` do projeto, da tarefa, dos comentários e da lista de tarefas (RN19).

**Causa da proteção:** cada operação passa por `AcessoProjetosServico` ou `AcessoTarefasServico`, que verificam se quem tem a sessão é membro do projeto do recurso. Não basta estar autenticado, que é exatamente o erro do exemplo vulnerável do PDF 12. A mesma verificação está automatizada em `AutorizacaoApiTest.aAnaNaoAcedeNemAlteraNadaDoProjetoBComOsIdentificadoresDele`, com 15 pedidos.

**Observação:** um id que existe dá 403 e um que não existe dá 404, o que revela se o id existe. Está assim no contrato (`openapi.yaml:33-35`), e os ids são UUID aleatórios, por isso adivinhá-los não é prático. Não é tratado como finding.

### Findings de segurança

| ID | Ficheiro e localização | Vulnerabilidade e evidência | Impacto | Severidade | Estado | Correção proposta | Teste de regressão |
|----|------------------------|-----------------------------|---------|------------|--------|-------------------|--------------------|
| S1 | `docker-compose.yml:10` (`"5432:5432"`, antes da correção); `backend/.env.example:7, 12, 16` | **Configuração insegura** (OWASP A05:2021 Security Misconfiguration; A07 Identification and Authentication Failures). O PostgreSQL fica publicado em todas as interfaces: `docker port matriz-postgres` → `0.0.0.0:5432` e `[::]:5432`. No ambiente local do utilizador, as três palavras-passe do `backend/.env` (base de dados, Administrador e demonstração) são **iguais aos valores de exemplo** do `.env.example`, que está versionado e publicado. Isto foi comparado sem mostrar os valores. O `.env` nunca foi versionado (`git log --all -- backend/.env` vazio). O Backend (8081) e o Vite (5173) só escutam em `127.0.0.1`. | Qualquer pessoa na mesma rede (por exemplo, o Wi-Fi da escola) pode ligar-se à base de dados com credenciais públicas, e ler ou alterar tudo, incluindo os hashes das palavras-passe. Também pode entrar como Administrador se chegar ao Backend, o que hoje não acontece, porque só escuta localmente. Só afeta o ambiente de desenvolvimento. Não houve verificação a partir de outra máquina. | Medium | **Confirmado**: a porta e a igualdade das palavras-passe. O acesso a partir de outra máquina não foi testado. **Corrigida a porta**; as palavras-passe ficam com o utilizador. | 1) `docker-compose.yml`: `"127.0.0.1:5432:5432"`. 2) O utilizador muda as palavras-passe do seu `backend/.env`; mudar `POSTGRES_PASSWORD` num volume que já existe exige `ALTER USER` dentro da base de dados, ou recriá-la. 3) Aviso mais claro no `.env.example` e no README. | Verificação com `docker port matriz-postgres` (esperado: `127.0.0.1:5432`). Um teste automático não se aplica, porque é configuração do Docker. |
| S2 | = Q1 | Validação de entrada: títulos, nomes e comentários invisíveis aceites pela API. | Ver Q1. Não permite executar código nem aceder a dados de outros; é integridade dos dados. | Low | **Confirmado** | Ver Q1. | Ver Q1. |

### Verificações sem finding

| Área | O que foi feito | Resultado |
|------|-----------------|-----------|
| Autenticação | Testes existentes (`LoginTest`, `SessaoApiTest`) | A mesma resposta para palavra-passe errada, email inexistente e conta desativada; bloqueio depois de 5 falhas; sessão expira ao fim de 30 min sem atividade. |
| Cookie de sessão | `Set-Cookie` no login real | `HttpOnly; SameSite=Strict; Path=/api`. Sem `Secure` porque o ambiente é HTTP; ativa-se com `MATRIZ_COOKIE_SEGURO` (`AutenticacaoRecurso.java:80-81`). |
| CORS | `OPTIONS` com `Origin: https://atacante.example` | Sem cabeçalhos `Access-Control-Allow-*`: nenhuma origem externa é autorizada. |
| SQL Injection | Procura de SQL montado por concatenação em `backend/src/main/java` | Nenhum caso. As consultas usam parâmetros com nome; o `LIKE` escapa `% _ \` (`Texto.java:25-34`, `ProjetoRepositorio.java:17`, `UtilizadorRepositorio.java:19`). |
| XSS | Procura de `dangerouslySetInnerHTML`, `innerHTML`, `eval` e `new Function` em `frontend/src` | Nenhum caso. O React escapa o texto apresentado. |
| Mass assignment | `POST` de tarefa com `estado`, `versao` e `id` | 400 `VALIDACAO`, "Campo desconhecido.", para cada um. O Gestor e os autores vêm sempre da sessão (`openapi.yaml:23-25`; `ProjetosApiTest.naoAceitaOGestorEnviadoPeloCliente`). |
| Exposição de dados | `GET /api/utilizadores` como Bruno | `id`, `nome` e `email` dos Membros ativos, sem hash nem tipo de conta. Previsto para escolher membros (US04). |
| Tratamento de erros | JSON malformado; objeto num campo de texto | 400 `VALIDACAO` sem stack trace. Os erros 500 não seguem o formato único (limitação R3, já registada em `verification.md`). |
| Segredos no repositório | `git grep` e `git log -p --all` à procura de palavras-passe e tokens | Só os valores de exemplo do `.env.example`, feitos para ser públicos. O problema está no uso local (S1). O código lê os segredos de variáveis de ambiente (`application.properties:23-25`). |

## Análise estática

| Ferramenta | Configurada | Comando | Resultado |
|------------|-------------|---------|-----------|
| oxlint 1.90.0 (lint de TypeScript e React, equivalente ao ESLint) | Sim (`frontend/.oxlintrc.json`) | `cd frontend && npm run lint` | 0 avisos e 0 erros em 64 ficheiros, com 116 regras. Durante a Ficha 04 acusou uma vez `triple-slash-reference` em `vite.config.ts`, que foi corrigido no Ex. 4. |
| `npm audit` (dependências do Frontend) | Vem com o npm | `cd frontend && npm audit` | 0 vulnerabilidades. Antes, acusou 2 de severidade alta no `@playwright/test@1.55.0` (GHSA-7mvr-c777-76hp), corrigidas com a passagem para a 1.55.1 (`test-plan.md`, F2). |
| SAST do Backend (Semgrep, SonarQube, SpotBugs) | **Não** | — | Não executado (decisão 3a do utilizador). A análise do Java foi só a revisão manual e com IA descrita acima. |
| Auditoria de dependências do Maven (OWASP Dependency-Check) | **Não** | — | Não executado. |

**Suspeitas contra confirmações.** A análise estática não encontrou o Q1 nem o S1. O Q1 é uma diferença de comportamento entre o Java e o JavaScript, e o S1 está numa configuração do Docker e num ficheiro que não é versionado. Os dois só apareceram ao executar pedidos e comandos reais, o que ilustra a "Comparação de resultados" do PDF 12.

## Decisões humanas

O utilizador reviu os findings e decidiu (Ficha 04, l. 126):

| Finding | Decisão | O que foi feito | Teste de regressão e verificação |
|---------|---------|-----------------|----------------------------------|
| S1 | **Corrigir** (1a) | `docker-compose.yml`: `"127.0.0.1:5432:5432"`, com um comentário. `backend/.env.example` e README: aviso para mudar as três palavras-passe antes do primeiro arranque. O contentor foi recriado com `docker compose up -d`; os dados ficaram no volume e os 3 projetos continuavam lá. | `docker port matriz-postgres` → `127.0.0.1:5432` (antes: `0.0.0.0:5432` e `[::]:5432`). Ligação TCP pelo IP da rede local → **recusada**; por `127.0.0.1` → aceite. Não há teste automático, por ser configuração do Docker. **Falta o utilizador mudar as palavras-passe do seu `backend/.env`.** A da base de dados exige também um `ALTER USER` na base que já existe; o agente não escreve valores (AGENTS.md §7). |
| Q1 / S2 | **Corrigir** (2a) | `Texto.normalizar` (`backend/src/main/java/pt/esmad/matriz/comum/texto/Texto.java`) retira das pontas os espaços (`Character.isWhitespace` e `isSpaceChar`) e os caracteres invisíveis de formatação (categoria Cf). `frontend/src/comum/texto.ts` ganhou `limpar()`, com a mesma regra, e `validarTexto` passou a usá-la. A linha da expressão regular tem uma exceção justificada ao `no-control-regex` do oxlint, porque U+001C–U+001F são intencionais. | `TextoTest` (novo, 14 testes): 12 caracteres, só ou nas pontas; o meio mantém-se, incluindo um emoji com U+200D; nulo e vazio. `CriarConsultarTarefaApiTest.tituloInvalidoERecusadoENaoGravaNada`: mais 4 títulos invisíveis → 400, e nada gravado. `texto.test.ts`: os mesmos 13 caracteres e o caso do meio (+14). **Os testes foram escritos antes da correção e falharam com o código antigo:** no Backend, 7 caracteres e o teste de integração; no Frontend, U+200B, U+200D, U+2060 e U+001C. Depois da correção passam. |
| Q2 | Não corrigir | — | Fica como limitação. |
| Q4 | Não corrigir | — | Fica como observação para a Fase 2. |

Resultado depois das correções: Backend com **169** testes, 0 falhas (`./mvnw test`); Frontend com **102**, 0 falhas (`npm test`); `tsc -b` sem erros; oxlint com 0 avisos e 0 erros. A correção do Q1 não foi repetida contra o Backend em `quarkus:dev`, porque o utilizador pediu para desligar os serviços. O teste de integração exercita a API com o PostgreSQL real dos Dev Services.

## Versão revista e problemas pendentes

- **Versão:** branch `main`. A Ficha PL 04 parte do commit `1658863`, a entrega da Ficha PL 03, e está nos commits seguintes, que levam "Ficha PL 04" na mensagem (`git log --oneline 1658863..main`).
- **Pendente do utilizador:** mudar as três palavras-passe do `backend/.env` local (S1).
- **Registados, sem correção:** Q2, Q4, a limitação R3 (`verification.md`) e a observação O1 (`test-plan.md`).

## O que ficou por verificar

- O acesso à base de dados a partir de outra máquina da rede (S1). Antes da correção, só se confirmou a porta aberta e as credenciais públicas; depois, a ligação pelo IP da rede local é recusada.
- Análise estática do Backend e auditoria das dependências do Maven, porque nenhuma ferramenta está configurada.
- Testes de carga, de força bruta distribuída e de segurança em produção (HTTPS, cabeçalhos de segurança do servidor), porque a aplicação só corre localmente.
- O Q2, porque o teste não força a sobreposição dos pedidos.
