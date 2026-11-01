# 0000 — Usar Architecture Decision Records

## Estado

Aceite (2026-11-01)

## Contexto

A Matriz já tomou várias decisões técnicas, registadas como linhas em
[constraints.md](../constraints.md) (D01 a D11). Esse registo diz *o que* foi
decidido, mas não guarda as alternativas consideradas nem os prós e contras.
Com o início da implementação (Ficha PL 03), as decisões arquiteturais passam
a ter impacto direto no código, e é preciso que o racional fique preservado e
versionado.

## Opções consideradas

- Continuar a registar as decisões apenas em `constraints.md` e
  `architecture.md`.
- Usar Architecture Decision Records (ADRs) em Markdown, no repositório.

## Decisão

Passam a ser usados ADRs em ficheiros Markdown, guardados em `docs/decisions/`,
com nomes no formato `NNNN-titulo-com-hifens.md` e um índice em
[README.md](README.md). Cada ADR segue a estrutura: Estado, Contexto, Opções
consideradas, Decisão e Consequências.

As decisões tomadas antes deste ADR (D01 a D11) continuam registadas em
`constraints.md`. Daqui em diante, cada nova decisão arquitetural significativa
tem o seu ADR, e `constraints.md` e `architecture.md` apontam para ele.

## Consequências

- O contexto, as alternativas e os trade-offs de cada decisão ficam guardados
  e versionados com o código.
- Quem chegar ao projeto (pessoas ou agentes de IA) percebe porque é que o
  sistema é como é.
- Há um pequeno custo de escrita em cada decisão.
- As decisões anteriores a este ADR não têm o mesmo nível de detalhe.
