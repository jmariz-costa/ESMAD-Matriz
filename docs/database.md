# Modelo de dados — Matriz

Este documento descreve o modelo conceptual de dados do MVP da Matriz:
entidades, atributos, relações, cardinalidades e regras de integridade. Os
tipos de dados são conceptuais. A base de dados é PostgreSQL (**D02**, ver
[constraints.md](constraints.md)); os tipos concretos ficam nas migrations.
Não inclui SQL.

## 1. Entidades

| Entidade            | Descrição                                                         | Requisitos                    |
|---------------------|-------------------------------------------------------------------|-------------------------------|
| **Utilizador**      | Conta de uma pessoa: Membro ou Administrador                      | RF01, RF02, RF14, S01, S07    |
| **Projeto**         | Projeto de uma equipa, com um único Gestor                        | RF03, RF08, PR01, PR04        |
| **MembroProjeto**   | Associação entre Utilizador e Projeto: quem pertence a que projeto | RN06, RF04                    |
| **Tarefa**          | Trabalho a fazer dentro de um projeto                             | RF05, RF06, RF07, RN10, RN13  |
| **AtribuicaoTarefa** | Associação entre Tarefa e Utilizador: quem é responsável por que tarefa | RN11, US05            |
| **Comentario**      | Comentário de um membro numa tarefa                               | RF12                          |
| **HistoricoTarefa** | Entrada do histórico de uma tarefa: criação ou alteração de um campo | RF13                       |
| **TarefaEliminada** | Registo de uma tarefa eliminada: título, quem e quando            | RF09, PR08                    |
| **Sessao**          | Sessão iniciada por um utilizador                                 | RF02, D06, PR16               |

Não são entidades:

- **Percentagem de conclusão** — é calculada a partir dos estados das tarefas
  (PR09), por isso não é guardada.
- **Etiquetas** — a US15 está fora do MVP.

## 2. Atributos

Legenda: **PK** identificador; **FK** referência a outra entidade;
*obrig.* obrigatório; *opc.* opcional. Todos os identificadores são UUID.
Os limites dos textos contam os caracteres depois de retirar os espaços no
início e no fim (RN23).

### Utilizador

| Atributo                 | Tipo conceptual | Restrições                                   | Origem          |
|--------------------------|-----------------|----------------------------------------------|-----------------|
| `id`                     | identificador   | PK                                           |                 |
| `nome`                   | texto           | obrig.                                       | US01, S07       |
| `email`                  | texto           | obrig., único, do domínio da empresa         | RN02, IF01      |
| `password_hash`          | texto           | obrig.; nunca a palavra-passe em claro       | S01             |
| `tipo`                   | enumerado       | Membro / Administrador                       | RN16, RN19      |
| `estado`                 | enumerado       | Por confirmar / Ativo / Desativado           | US01, US14      |
| `token_confirmacao_hash` | texto           | opc.; limpo ao confirmar a conta             | RN03, S05       |
| `token_expira_em`        | data-hora       | opc.; 24 h após gerar o link                 | RN03, S05       |
| `tentativas_falhadas`    | inteiro         | obrig.; começa a 0; volta a 0 num login correto | D09          |
| `bloqueado_ate`          | data-hora       | opc.; login recusado até esta data-hora      | D09             |
| `deve_mudar_palavra_passe` | booleano      | obrig.; verdadeiro depois de o Administrador repor o acesso, até a pessoa definir uma palavra-passe nova | US19, PR20 |

### Projeto

| Atributo    | Tipo conceptual | Restrições                     | Origem       |
|-------------|-----------------|--------------------------------|--------------|
| `id`        | identificador   | PK                             |              |
| `nome`      | texto           | obrig.; 3 a 100 caracteres     | PR04, RN23   |
| `descricao` | texto           | opc.; até 1000 caracteres      | PR04, RN23   |
| `arquivado` | booleano        | obrig.; começa a falso         | RN15, US08   |
| `gestor_id` | identificador   | FK → Utilizador, obrig.        | RN05, PR01   |

### MembroProjeto

| Atributo        | Tipo conceptual | Restrições                         | Origem       |
|-----------------|-----------------|------------------------------------|--------------|
| `projeto_id`    | identificador   | PK (composta), FK → Projeto        | RF04         |
| `utilizador_id` | identificador   | PK (composta), FK → Utilizador     | RF04         |

A chave composta impede que alguém fique duplicado no mesmo projeto
(US04 crit. 3).

### Tarefa

| Atributo         | Tipo conceptual | Restrições                                                  | Origem        |
|------------------|-----------------|-------------------------------------------------------------|---------------|
| `id`             | identificador   | PK                                                          |               |
| `projeto_id`     | identificador   | FK → Projeto, obrig.                                        |               |
| `titulo`         | texto           | obrig.; 3 a 150 caracteres                                  | RN10, RN23    |
| `descricao`      | texto           | opc.; até 2000 caracteres                                   | RN10, RN23    |
| `prazo`          | data            | opc.; qualquer data, incluindo passadas                     | RN10, RN12    |
| `prioridade`     | enumerado       | opc.; Baixa / Média / Alta                                  | RN10          |
| `estado`         | enumerado       | obrig.; A fazer / Em curso / Em revisão / Concluída; começa em "A fazer" | RN13, PR05 |
| `versao`         | inteiro         | obrig.; começa em 0 e aumenta a cada alteração dos campos ou dos responsáveis (os comentários não a alteram) | D08 |

Quem criou a tarefa e quando ficam na entrada de criação do histórico, pelo
que não se repetem na Tarefa. Os responsáveis estão em AtribuicaoTarefa.

### AtribuicaoTarefa

| Atributo        | Tipo conceptual | Restrições                         | Origem       |
|-----------------|-----------------|------------------------------------|--------------|
| `tarefa_id`     | identificador   | PK (composta), FK → Tarefa         | RN11         |
| `utilizador_id` | identificador   | PK (composta), FK → Utilizador     | RN11         |

A chave composta impede que a mesma pessoa fique atribuída duas vezes à mesma
tarefa (RN11, CE8). Quem atribuiu e quando fica no histórico.

### Comentario

| Atributo     | Tipo conceptual | Restrições                 | Origem       |
|--------------|-----------------|----------------------------|--------------|
| `id`         | identificador   | PK                         |              |
| `tarefa_id`  | identificador   | FK → Tarefa, obrig.        | US12         |
| `autor_id`   | identificador   | FK → Utilizador, obrig.    | US12, PR11   |
| `texto`      | texto           | obrig.; 1 a 2000 caracteres | US12 crit. 2, RN23 |
| `criado_em`  | data-hora       | obrig.                     | US12 crit. 1 |
| `editado_em` | data-hora       | opc.                       | US12 crit. 3 |

### HistoricoTarefa

| Atributo         | Tipo conceptual | Restrições                                                         | Origem       |
|------------------|-----------------|--------------------------------------------------------------------|--------------|
| `id`             | identificador   | PK                                                                 |              |
| `tarefa_id`      | identificador   | FK → Tarefa, obrig.                                                | US13         |
| `autor_id`       | identificador   | FK → Utilizador, obrig.                                            | US13         |
| `data_hora`      | data-hora       | obrig.                                                             | US13         |
| `tipo`           | enumerado       | Criação / Alteração                                                | US13 crit. 1 |
| `campo`          | enumerado       | titulo / descricao / estado / prazo / prioridade / responsavel; vazio na criação | US13 crit. 1 |
| `valor_anterior` | texto           | opc.                                                               | US13 crit. 1 |
| `valor_novo`     | texto           | opc.                                                               | US13 crit. 1 |

Os valores são guardados como texto, para que o histórico não mude depois de
registado. Estado e prioridade são guardados como códigos (ex.: `EM_CURSO`,
`ALTA`), os mesmos da API, e traduzidos ao mostrar (PR18); o responsável é
guardado pelo nome (ex.: "Ana Exemplo"). Como uma tarefa pode ter vários
responsáveis, cada atribuição é uma entrada com `campo = responsavel`: ao
atribuir, `valor_anterior` fica vazio e `valor_novo` tem o nome; ao remover,
`valor_anterior` tem o nome e `valor_novo` fica vazio.

### TarefaEliminada

| Atributo        | Tipo conceptual | Restrições                 | Origem       |
|-----------------|-----------------|----------------------------|--------------|
| `id`            | identificador   | PK                         |              |
| `projeto_id`    | identificador   | FK → Projeto, obrig.       | US09 crit. 4 |
| `titulo`        | texto           | obrig.                     | PR08         |
| `eliminada_por` | identificador   | FK → Utilizador, obrig.    | PR08         |
| `eliminada_em`  | data-hora       | obrig.                     | PR08         |

### Sessao

| Atributo           | Tipo conceptual | Restrições                                              | Origem    |
|--------------------|-----------------|---------------------------------------------------------|-----------|
| `id`               | identificador   | PK                                                      |           |
| `utilizador_id`    | identificador   | FK → Utilizador, obrig.                                 | D06       |
| `token_hash`       | texto           | obrig., único; nunca o token em claro                   | D06, S08  |
| `criada_em`        | data-hora       | obrig.                                                  | D06       |
| `ultima_atividade` | data-hora       | obrig.; a sessão expira 30 min depois deste momento     | D06       |

## 3. Relações e cardinalidades

| Relação                                  | Cardinalidade               | Notas                                                    |
|------------------------------------------|-----------------------------|----------------------------------------------------------|
| Utilizador **gere** Projeto              | 1 — 0..N                    | Cada projeto tem exatamente um Gestor (PR01)             |
| Utilizador **pertence a** Projeto        | N:M, através de MembroProjeto | Um projeto tem pelo menos um membro: o Gestor          |
| Projeto **tem** Tarefa                   | 1 — 0..N                    |                                                          |
| Utilizador **é responsável por** Tarefa  | N:M, através de AtribuicaoTarefa | Uma tarefa pode ter vários responsáveis ou nenhum (RN11) |
| Tarefa **tem** Comentario                | 1 — 0..N                    |                                                          |
| Utilizador **escreve** Comentario        | 1 — 0..N                    |                                                          |
| Tarefa **tem** HistoricoTarefa           | 1 — 1..N                    | Há sempre pelo menos a entrada de criação                |
| Utilizador **faz** HistoricoTarefa       | 1 — 0..N                    |                                                          |
| Projeto **regista** TarefaEliminada      | 1 — 0..N                    |                                                          |
| Utilizador **elimina** TarefaEliminada   | 1 — 0..N                    |                                                          |
| Utilizador **tem** Sessao                | 1 — 0..N                    | Uma pessoa pode ter sessões em vários browsers           |

## 4. Diagrama ER

```mermaid
erDiagram
    Utilizador ||--o{ Projeto : "gere"
    Utilizador ||--o{ MembroProjeto : "pertence a"
    Projeto ||--|{ MembroProjeto : "tem"
    Projeto ||--o{ Tarefa : "tem"
    Utilizador ||--o{ AtribuicaoTarefa : "é responsável"
    Tarefa ||--o{ AtribuicaoTarefa : "tem"
    Tarefa ||--o{ Comentario : "tem"
    Utilizador ||--o{ Comentario : "escreve"
    Tarefa ||--|{ HistoricoTarefa : "tem"
    Utilizador ||--o{ HistoricoTarefa : "faz"
    Projeto ||--o{ TarefaEliminada : "regista"
    Utilizador ||--o{ TarefaEliminada : "elimina"
    Utilizador ||--o{ Sessao : "tem"

    Utilizador {
        id id PK
        texto nome
        texto email UK
        texto password_hash
        enum tipo "Membro | Administrador"
        enum estado "Por confirmar | Ativo | Desativado"
        texto token_confirmacao_hash "opcional"
        datahora token_expira_em "opcional"
        inteiro tentativas_falhadas
        datahora bloqueado_ate "opcional"
        booleano deve_mudar_palavra_passe
    }

    Projeto {
        id id PK
        texto nome
        texto descricao "opcional"
        booleano arquivado
        id gestor_id FK
    }

    MembroProjeto {
        id projeto_id PK, FK
        id utilizador_id PK, FK
    }

    Tarefa {
        id id PK
        id projeto_id FK
        texto titulo
        texto descricao "opcional"
        data prazo "opcional"
        enum prioridade "opcional: Baixa | Média | Alta"
        enum estado "A fazer | Em curso | Em revisão | Concluída"
        inteiro versao
    }

    AtribuicaoTarefa {
        id tarefa_id PK, FK
        id utilizador_id PK, FK
    }

    Comentario {
        id id PK
        id tarefa_id FK
        id autor_id FK
        texto texto
        datahora criado_em
        datahora editado_em "opcional"
    }

    HistoricoTarefa {
        id id PK
        id tarefa_id FK
        id autor_id FK
        datahora data_hora
        enum tipo "Criação | Alteração"
        enum campo "vazio na criação"
        texto valor_anterior "opcional"
        texto valor_novo "opcional"
    }

    TarefaEliminada {
        id id PK
        id projeto_id FK
        texto titulo
        id eliminada_por FK
        datahora eliminada_em
    }

    Sessao {
        id id PK
        id utilizador_id FK
        texto token_hash UK
        datahora criada_em
        datahora ultima_atividade
    }
```

## 5. Regras de integridade

Regras que o diagrama não mostra e que a aplicação tem de garantir:

| ID   | Regra                                                                                                              | Origem            |
|------|--------------------------------------------------------------------------------------------------------------------|-------------------|
| RI01 | O Gestor de um projeto também consta em MembroProjeto desse projeto e não pode ser removido dele.                  | US04 crit. 5, PR14 |
| RI02 | Cada responsável de uma tarefa tem de ser membro do projeto e ter a conta ativa.                                   | US05 crit. 4, US14 crit. 5 |
| RI03 | Um Administrador nunca é Gestor, membro de projeto, responsável de tarefa nem autor de comentários. Só aparece como autor no histórico pelas alterações que resultam das ações da US14 (ver RI05). | RN19, US14 |
| RI04 | Remover um membro de um projeto tira-o de responsável das tarefas não concluídas desse projeto.                   | RN08              |
| RI05 | Desativar uma conta tira-a de responsável das tarefas não concluídas em todos os projetos, incluindo arquivados. Cada uma destas alterações fica no histórico, com o Administrador que desativou a conta como autor. | RN18, US13 crit. 1, US14 crit. 2 |
| RI06 | Uma pessoa desativada mantém-se em MembroProjeto; ao ser reativada volta como Membro, sem tarefas nem papel de Gestor. | PR15         |
| RI07 | Antes de desativar um Gestor, cada projeto dele recebe um novo Gestor, Membro ativo; se não for membro, passa a sê-lo. | RN17, PR14   |
| RI08 | As contas de Administrador estão sempre ativas: não podem ser desativadas na Matriz.                              | RN21              |
| RI09 | Eliminar uma tarefa apaga os seus comentários, atribuições e histórico e cria um registo em TarefaEliminada.       | PR08              |
| RI10 | Num projeto arquivado não se cria, altera nem apaga nada; exceções: reativar o projeto, pelo Gestor (US08), eliminar o projeto, pelo Gestor (RN22), e as ações da US14. | RN15, RN22, US08 crit. 3, US14 crit. 4 |
| RI11 | As entradas do histórico nunca são editadas nem apagadas (exceto ao eliminar a tarefa, RI09).                     | PR12              |
| RI12 | Criar uma tarefa, alterar título, descrição, estado, prazo ou prioridade, ou atribuir ou remover um responsável gera uma entrada no histórico. | RF13, US13 crit. 1 |
| RI13 | O email é único e pertence ao domínio da empresa.                                                                  | RN02, US01 crit. 4–5 |
| RI14 | O token de confirmação é guardado em hash, expira 24 h depois de gerado, é substituído quando se pede novo link e é limpo quando a conta é confirmada. | RN03, S05, US01 crit. 7 |
| RI15 | Utilizadores nunca são apagados (só desativados), pelo que as referências a eles se mantêm válidas. Projetos só são apagados quando o Gestor os elimina (RI17). | RN18, RN22 |
| RI16 | Só Membros com conta ativa podem ser adicionados a um projeto (nunca Administradores, contas por confirmar ou desativadas). | US04 crit. 1, US14 crit. 5, RN19 |
| RI17 | Eliminar um projeto apaga, em cascata, os seus membros (MembroProjeto), tarefas, atribuições, comentários, histórico e registos de TarefaEliminada, sem deixar dados órfãos. | RN22, L06 |
| RI18 | Cada alteração de uma tarefa aumenta a `versao`; uma alteração feita com uma versão desatualizada é recusada.      | D08               |
| RI19 | Uma sessão é válida só enquanto não passarem 30 minutos desde a `ultima_atividade`. Sair apaga a sessão; desativar uma conta apaga todas as sessões dessa pessoa. | D06, PR16, US02 crit. 4 |
| RI20 | Ao chegar a 5 tentativas falhadas seguidas, `bloqueado_ate` passa a 15 minutos depois; um login correto põe `tentativas_falhadas` a 0. | D09, PR03 |
| RI21 | Repor o acesso guarda o hash da palavra-passe temporária, põe `deve_mudar_palavra_passe` a verdadeiro, limpa o bloqueio (D09) e apaga as sessões da pessoa; mudar a palavra-passe põe-no a falso e apaga as outras sessões. | US19, US20, PR20, PR21 |

## 6. Pressupostos e decisões pendentes

**Decisões de modelação tomadas nesta fase:**

- O Administrador é um Utilizador com `tipo = Administrador`, e não uma
  entidade separada; as restrições da RN19 são garantidas pela aplicação (RI03).
- O Gestor é indicado por `gestor_id` no Projeto, o que garante pela estrutura
  um único Gestor por projeto (PR01).
- O link de confirmação é guardado em campos do Utilizador; gerar um novo link
  substitui o anterior (US01 crit. 7).
- Os valores do histórico são guardados como texto; estado e prioridade como
  códigos, traduzidos ao mostrar (PR18).
- Os identificadores são UUID (Ficha PL 03).
- Os vários responsáveis de uma tarefa ficam numa entidade de associação
  (AtribuicaoTarefa), tal como os membros de um projeto (Ficha PL 03).

**Decididas na Ficha PL 03**, com impacto no modelo: D02 (PostgreSQL), D06
(entidade Sessao), D08 (campo `versao` na Tarefa), D09 (campos de tentativas
no Utilizador) e os tamanhos máximos dos textos (RN23).

**Continuam pendentes** (ver [constraints.md](constraints.md)):

| ID   | Impacto no modelo                                                                                 |
|------|---------------------------------------------------------------------------------------------------|
| D10  | Pedido de apagamento de dados (RGPD): o histórico e os comentários guardam nomes de pessoas.      |
| IF01 | Domínio de email da empresa, necessário para validar a RI13 (só quando a US01 for implementada). |
