# Plano de implementação — Matriz

Plano executável do MVP da Matriz (Ficha PL 03). Traduz a documentação em
incrementos pequenos, cada um com componentes, critérios de aceitação e forma
de verificação. O estado de cada incremento é atualizado ao longo da
implementação.

Documentos de referência: [requirements.md](requirements.md),
[user-stories.md](user-stories.md), [constraints.md](constraints.md),
[architecture.md](architecture.md), [database.md](database.md),
[../openapi.yaml](../openapi.yaml) e [ui/](ui/).

## 1. Conciliação entre a Ficha PL 03 e o MVP

Diferenças encontradas entre o âmbito da Ficha PL 03 e o MVP aprovado nas
Fichas PL 01 e PL 02, e a decisão tomada para cada uma (Exercício 1).

| # | Diferença | Decisão | Documentos atualizados |
|---|-----------|---------|------------------------|
| 1 | A ficha pede vários responsáveis por tarefa; o MVP tinha no máximo um (RN11). | Passar a vários responsáveis, sem duplicados. | RN11, US05, US07, US13, US14, database (AtribuicaoTarefa), OpenAPI, wireframe |
| 2 | A ficha pede eliminar projetos; o MVP só permitia arquivar (RN15, L06). | Só o Gestor elimina, com confirmação e em cascata; também em projetos arquivados. O arquivo mantém-se. | RN15, RN22, L06, US16, RI10, RI15, RI17 |
| 3 | A ficha considera o registo público opcional; no MVP era a US01 (Alta). | US01 adiada; contas de demonstração criadas por seed. | L07, requirements §7, US01 |
| 4 | A ficha pede editar projeto, pesquisa, filtros, painel e paginação, que não estavam no MVP. | Entram no âmbito: US16, US17 e US18. | RF16–RF18, US16–US18, OpenAPI, wireframes |
| 5 | O MVP tem User Stories que a ficha não exige (US04, US07–US14). | Implementadas por fases: Fase 1 = mínimo da ficha + US04 + Kanban; Fase 2 = restantes. | Este plano |
| 6 | Limites de texto não definidos no MVP. | Os do exemplo da ficha, sem contar espaços nas pontas. | RN23, database, OpenAPI |
| 7 | No exemplo da ficha a prioridade é obrigatória (MEDIUM). | Mantém-se opcional (RN10); filtro com "Sem prioridade". | — |
| 8 | A ficha pede adaptar o layout a várias larguras; o MVP define ≥1280 px. | Layout adaptativo, sem alterar o requisito RNF01. | architecture, wireframes |
| 9 | Os estados do exemplo da ficha são TODO/IN_PROGRESS/DONE. | Mantêm-se os 4 estados do contrato adotado (RN13). | — |

**Decisões técnicas** (detalhe em [constraints.md](constraints.md) §4):
Java 21 + Quarkus (D01), React + TypeScript + Vite (D01), PostgreSQL em
Docker com Flyway (D02), sessão na base de dados com cookie HttpOnly e 30 min
de inatividade (D06), Administrador criado no arranque a partir de variáveis
de ambiente (D07), bloqueio otimista (D08), 5 falhas → 15 min (D09), testes
JUnit + RestAssured e Vitest + Testing Library (D11), identificadores UUID.
Arquitetura interna do Backend: camadas por módulo com DDD leve
([ADR 0001](decisions/0001-arquitetura-interna-backend.md)).

## 2. Regras de acesso consolidadas

| Ação | Quem pode | Origem |
|------|-----------|--------|
| Ver projeto, tarefas, responsáveis e comentários | Membros do projeto | RN20, S04 |
| Criar projeto | Qualquer Membro (fica Gestor) | RN05 |
| Editar ou eliminar projeto, gerir membros, arquivar e reativar | Só o Gestor | RN07, RN22, PR07 |
| Criar e editar tarefas, mudar estado e prioridade | Membros do projeto | RN09, RN14 |
| Eliminar tarefa | Só o Gestor | RN09 |
| Atribuir e remover responsáveis | Membros do projeto | US05 |
| Elegíveis como responsáveis | Membros do projeto com conta ativa | RI02 |
| Comentar | Membros do projeto | US12 |
| Editar ou apagar comentário | Só o autor | PR11 |
| Alterar algo num projeto arquivado | Ninguém, exceto reativar, eliminar e as ações da US14 | RI10 |
| Administrador | Nada dentro dos projetos (403) | RN19 |

A identidade (Gestor de um projeto novo, autor de comentários e do histórico)
vem sempre da sessão, e nunca de dados enviados pelo cliente.

## 3. Estrutura prevista do repositório

```
ESMAD-Matriz/
├── backend/                 Quarkus (Maven wrapper)
│   └── src/main/java/…/matriz/
│       ├── comum/           erros, autenticação, configuração
│       ├── contas/          utilizadores, sessões, login, Administrador
│       ├── projetos/        projetos, membros, contagens
│       ├── tarefas/         tarefas, responsáveis
│       ├── comentarios/
│       └── historico/
│   └── src/main/resources/db/migration/   migrations Flyway
├── frontend/                React + TypeScript (Vite)
│   └── src/ api/ (cliente central), componentes/, ecras/
├── docker-compose.yml       PostgreSQL com volume persistente
├── .env.example             valores fictícios
└── docs/
```

## 4. Fase 1 — incrementos

Estado: ⬜ por fazer · 🟨 em curso · ✅ concluído.

Em todos os incrementos: pedir primeiro a análise e o plano do incremento,
implementar depois de validado, executar as verificações, rever o diff e
atualizar este plano antes de passar ao seguinte.

### I0 — Preparar a aplicação e a persistência ⬜

**Ficha:** Exercício 2. **Depende de:** nada.

- Criar `backend/` (Quarkus: REST + Jackson, Hibernate ORM com Panache,
  Hibernate Validator, JDBC PostgreSQL, Flyway, SmallRye Health) e
  `frontend/` (Vite, React, TypeScript, React Router, Vitest).
- `docker-compose.yml` com PostgreSQL e volume persistente; `.env.example`
  com valores fictícios; `.gitignore` para `.env`, `target/`, `node_modules/`.
- Migration inicial com todo o esquema de [database.md](database.md),
  incluindo as chaves compostas, as FK com eliminação em cascata (RI17) e os
  índices necessários.
- Seed de demonstração: pelo menos 3 Membros e 2 projetos com membros
  diferentes, aplicado uma única vez (nunca apaga dados no arranque).
- Criação do Administrador no arranque a partir de variáveis de ambiente (D07).
- Proxy `/api` do Vite para o Backend.

**Critérios de aceitação**
- `docker compose up -d`, o Backend e o Frontend arrancam seguindo o README.
- O esquema é criado pelas migrations; reiniciar o Backend não apaga dados.
- O health check confirma a ligação à base de dados.

**Verificação:** arranque manual, health check, consulta às tabelas e ao seed,
reinício do Backend; comandos registados no README.

### I1 — Autenticação ⬜

**Ficha:** Exercício 3. **US:** US02. **Decisões:** D06, D09, S01, PR03.

- Backend: entidade Sessao; `POST /auth/login`, `POST /auth/logout`,
  `GET /auth/me`; filtro que identifica o utilizador pelo cookie e renova a
  sessão; hash bcrypt; limite de tentativas; formato único de erro; 401 sem
  sessão válida; 403 para o Administrador nas operações de projetos.
- Frontend: ecrã de início de sessão (wireframe-ecras, ecrã 1); cliente HTTP
  central (endereço da API num único sítio); contexto do utilizador; rotas
  protegidas; em 401, voltar ao início de sessão com a mensagem
  "A sua sessão terminou…"; botão Sair.

**Critérios de aceitação:** US02 crit. 1, 3 e 4; D09; sessão expira após 30
min sem atividade; palavra-passe nunca guardada em claro.

**Verificação:** testes de integração (login válido, inválido, bloqueio após 5
falhas, pedido sem sessão, logout invalida a sessão); teste de componente do
formulário; verificação manual no browser.

### I2 — Gestão de projetos e painel ⬜

**Ficha:** Exercício 4. **US:** US03, US16, US17 (pesquisa e paginação), US18, US10.

- Backend: `GET/POST /projetos`, `GET/PATCH/DELETE /projetos/{id}`; Gestor
  obtido da sessão e adicionado como membro (RI01); listagem limitada aos
  projetos do utilizador; pesquisa sem distinguir maiúsculas; paginação;
  contagens por estado; eliminação em cascata; validação RN23.
- Frontend: painel inicial e ecrã de projetos (wireframe-ecras, ecrãs 2 e 3);
  formulário de criar/editar; confirmação antes de eliminar; percentagem de
  conclusão calculada a partir das contagens (PR09).

**Critérios de aceitação:** US03, US16, US17 crit. 1, 2 e 4, US18, US10 crit.
1 e 2. **Primeiro percurso completo da ficha:** iniciar sessão, criar um
projeto, vê-lo na listagem e voltar a consultá-lo após recarregar a página e
reiniciar o Backend.

**Verificação:** testes de integração (validação do nome, criar e consultar
com persistência, outro utilizador recebe 403, só o Gestor edita e elimina,
cascata sem órfãos, paginação e total); verificação manual do percurso
completo.

### I3 — Membros do projeto ⬜

**US:** US04 (base das regras de acesso e da elegibilidade).

- Backend: `GET/POST /projetos/{id}/membros`,
  `DELETE /projetos/{id}/membros/{utilizadorId}`, `GET /utilizadores`; só
  Membros ativos (RI16); 409 `JA_EXISTE`; o Gestor não pode ser removido;
  remover um membro retira as suas atribuições em tarefas não concluídas (RI04).
- Frontend: janela "Membros" no detalhe do projeto (só Gestor).

**Critérios de aceitação:** US04 crit. 1 a 5.

**Verificação:** testes de integração (adicionar, duplicado, remover Gestor,
membro removido deixa de aceder); verificação manual com dois utilizadores.

### I4 — Gestão de tarefas e quadro Kanban ⬜

**Ficha:** Exercício 5. **US:** US05, US06, US07, US09 (crit. 1 a 3), US17 (filtros).

- Backend: `GET/POST /projetos/{id}/tarefas` (filtros e paginação),
  `GET/PATCH/DELETE /tarefas/{id}`; estado inicial `A_FAZER`; `versao` e 409
  `VERSAO_DESATUALIZADA` (D08); entradas no histórico na criação e em cada
  alteração (RI12; a consulta do histórico é da Fase 2); eliminar só pelo
  Gestor, com registo em TarefaEliminada (RI09).
- Frontend: detalhe do projeto com o quadro (wireframe.html): uma listagem
  paginada por coluna com "Ver mais", filtros, mudar estado por arrastar e
  pelo seletor, formulário de criação e detalhe da tarefa, confirmação antes
  de eliminar; listagens e contagens atualizadas sem recarregar a página.

**Critérios de aceitação:** US05 crit. 1, 2, 3, 5, 6, 8; US06; US07; US09
crit. 1 a 3; US17 crit. 3; textos só com espaços rejeitados.

**Verificação:** testes de integração (validação do título, estado inicial,
filtros combinados, versão desatualizada, eliminar só pelo Gestor, tarefa
persiste após reiniciar); testes de componente (formulário mantém os dados
quando a API devolve erro); verificação manual do quadro.

### I5 — Responsáveis e comentários ⬜

**Ficha:** Exercício 6. **US:** US05 (crit. 4 e 7), US12 (crit. 1 e 2).

- Backend: `POST /tarefas/{id}/responsaveis`,
  `DELETE /tarefas/{id}/responsaveis/{utilizadorId}`; só membros ativos do
  projeto (RI02); 409 `JA_EXISTE`; histórico de cada atribuição;
  `GET/POST /tarefas/{id}/comentarios`, com autor e data definidos no Backend.
- Frontend: responsáveis no detalhe da tarefa (etiquetas com ✕ e
  "+ Adicionar" só com elegíveis); separador de comentários com autor e data.

**Critérios de aceitação:** atribuir vários, não duplicar, remover só uma
atribuição; comentário com 1 a 2000 caracteres e não só espaços; comentários
e atribuições disponíveis após reabrir a tarefa.

**Verificação:** testes de integração (duplicado → 409, não membro → 400,
autor do comentário é o da sessão mesmo que o pedido traga outro);
verificação manual.

### I6 — Revisão da interface ⬜

**Ficha:** Exercício 7.

- Confirmar em todos os ecrãs: carregamento, lista vazia, sucesso e erro;
  dados preservados em erro; botões inativos durante pedidos; sem registos
  duplicados; totais coerentes com os filtros; sem mocks.
- Layout adaptativo em larguras menores.

**Verificação:** percurso manual completo no Firefox, registado em
verification.md.

### I7 — Verificação do MVP ⬜

**Ficha:** Exercício 8.

- Criar `docs/verification.md` com os casos 01 a 09 da ficha: resultado
  esperado, observado e evidência.
- Pelo menos três testes automatizados relevantes (validação, autorização,
  criação com consulta posterior), distinguindo testes automatizados de
  verificações manuais.
- Para cada bug: reproduzir, investigar a causa, corrigir de forma limitada e
  acrescentar um teste de regressão.

### I8 — Revisão e entrega ⬜

**Ficha:** Exercício 9.

- Rever o diff (permissões, validações, erros, alterações fora do âmbito).
- Uma refatoração justificada, num passo separado.
- Atualizar OpenAPI, documentos, AGENTS.md (comandos) e README.md
  (requisitos, instalação, configuração, esquema, seed, contas de
  demonstração, arranque e testes).
- Três exemplos de utilização da IA em verification.md.
- Confirmar que o projeto arranca a partir de uma cópia limpa.

## 5. Fase 2 — restantes User Stories do MVP

Cada uma é um incremento próprio, com o mesmo método, e acrescenta os seus
endpoints ao OpenAPI quando for implementada.

| Incremento | US | Estado |
|------------|----|--------|
| F2.1 Arquivar e reativar projeto | US08 | ⬜ |
| F2.2 Consultar o registo de tarefas eliminadas | US09 crit. 4 | ⬜ |
| F2.3 Consultar o histórico de uma tarefa | US13 | ⬜ |
| F2.4 As minhas tarefas | US11 | ⬜ |
| F2.5 Editar e apagar os próprios comentários | US12 crit. 3 e 4 | ⬜ |
| F2.6 Administrador: desativar e reativar contas | US14 | ⬜ |
| F2.7 Registo com confirmação por email (extensão) | US01 | ⬜ |

## 6. Pontos em aberto

| Ponto | Quando decidir |
|-------|----------------|
| Como fornecer a palavra-passe das contas de demonstração sem guardar segredos no repositório (S08, AGENTS.md §7). | I0 |
| Nome base dos packages Java. | I0 |
