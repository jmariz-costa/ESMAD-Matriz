# 0003 — Base de dados: PostgreSQL (relacional)

## Estado

Aceite. Decisão tomada a 2026-11-01 (Ficha PL 03, D02 em
[constraints.md](../constraints.md)); registada como ADR a 2026-11-14.

**Registo retroativo:** na altura, só ficou registada a escolha. As
alternativas e a justificação abaixo foram reconstruídas depois, a partir do
modelo de dados e do código existentes, e não são uma ata do que foi pesado
na altura.

## Contexto

O modelo de dados da Matriz ([database.md](../database.md)) é feito de
entidades fortemente ligadas: utilizadores, projetos, membros de projeto,
tarefas, responsáveis, comentários, histórico e sessões.

Várias regras de negócio dependem dessas ligações e têm de ser sempre
cumpridas:

- a mesma pessoa não pode ficar atribuída duas vezes à mesma tarefa (RN11);
- cada responsável tem de ser membro do projeto (RI02);
- eliminar um projeto apaga, sem deixar órfãos, os membros, as tarefas, as
  atribuições, os comentários e o histórico (RI17);
- duas gravações simultâneas da mesma tarefa não se podem sobrepor (D08,
  bloqueio otimista com número de versão);
- um email só pode existir uma vez, sem distinguir maiúsculas.

A escala é pequena: até 50 pessoas, numa única empresa (RNF04, RN01).

## Opções consideradas

**A. Base de dados relacional (PostgreSQL).**

- ✅ As regras acima ficam na própria base de dados: chaves estrangeiras,
  `ON DELETE CASCADE`, chaves primárias compostas e índices únicos. Valem
  mesmo que o código se engane.
- ✅ Transações ACID que abrangem várias tabelas, por exemplo ao remover um
  membro e retirar as suas atribuições (RN08).
- ✅ Encaixa no Quarkus com Hibernate ORM/Panache e nas migrations Flyway.
- ✅ Os testes de integração correm contra um PostgreSQL real (Dev Services,
  D11).
- ❌ Mudar o esquema exige migrations.

**B. Base de dados de documentos (MongoDB).**

- ✅ Esquema flexível, e um projeto com as suas tarefas poderia ser guardado
  num único documento.
- ❌ As ligações entre projetos, pessoas e tarefas são muitas para muitos
  (membros, responsáveis), o que obriga a referências entre documentos ou a
  duplicar dados.
- ❌ A unicidade das atribuições, a cascata ao eliminar e a integridade das
  referências passariam a depender só do código da aplicação.
- ❌ Menos natural no ecossistema escolhido (D01), que assume JPA.

**C. Base de dados embebida (por exemplo, H2 ou SQLite).**

- ✅ Sem servidor de base de dados para instalar.
- ❌ Diferenças de comportamento entre a base dos testes e a de produção, ou
  uma base pouco adequada a vários acessos ao mesmo tempo.

## Decisão

**PostgreSQL**, executado em Docker, com o esquema criado e evoluído só por
migrations Flyway. O Hibernate não altera tabelas.

## Consequências

- ✅ As regras de integridade estão no esquema
  (`backend/src/main/resources/db/migration/V1__esquema_inicial.sql`), por
  exemplo a `PRIMARY KEY (tarefa_id, utilizador_id)` das atribuições, os
  `ON DELETE CASCADE` e o índice único em `lower(email)`.
- ✅ Testes, desenvolvimento e Docker Compose usam o mesmo motor (PostgreSQL
  17).
- ❌ É mais um serviço a executar e a proteger: volume persistente, palavra-passe
  e porta só em `127.0.0.1` (Ficha PL 04, S1).
- ❌ A pesquisa sem acentos exigiria a extensão `unaccent` (ponto em aberto no
  [plano](../implementation-plan.md) §6).
