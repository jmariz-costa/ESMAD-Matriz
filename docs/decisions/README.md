# Architecture Decision Records — Matriz

Registo das decisões arquiteturais da Matriz (ver ADR 0000). As decisões
anteriores a estes ADRs (D01 a D11) estão em
[constraints.md](../constraints.md).

| ADR | Decisão | Estado |
|-----|---------|--------|
| [0000](0000-usar-architecture-decision-records.md) | Usar Architecture Decision Records | Aceite |
| [0001](0001-arquitetura-interna-backend.md) | Arquitetura interna do Backend: camadas por módulo com DDD leve | Aceite |
| [0002](0002-execucao-em-contentores-e-ci.md) | Execução em contentores (Docker Compose, nginx) e integração contínua (GitHub Actions) | Aceite |
| [0003](0003-base-de-dados-relacional-postgresql.md) | Base de dados relacional: PostgreSQL (registo retroativo de D02) | Aceite |
| [0004](0004-autenticacao-sessao-na-base-de-dados.md) | Autenticação: sessão na base de dados com cookie, em vez de JWT ou SSO (registo retroativo de D06) | Aceite |
| [0005](0005-pipeline-em-etapas-e-publicacao-no-docker-hub.md) | Pipeline em 5 etapas, cobertura mínima de 95% (JaCoCo) e publicação das imagens no Docker Hub | Aceite |
