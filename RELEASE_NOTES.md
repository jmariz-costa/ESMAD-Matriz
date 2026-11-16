# Matriz — versão candidata v0.2.0-rc.1

**Data:** 2026-11-16 · **Estado:** candidata · Release no GitHub `v0.2.0-rc.1`,
posta em produção com o workflow "Deploy prod".

As notas da versão anterior (`v0.1.0-rc.1`) estão no
[CHANGELOG.md](CHANGELOG.md) e na tag `v0.1.0-rc.1`.

## Visão geral

A Matriz passa a estar **em produção**, em
https://matriz-frontend-tbdh.onrender.com (Render e Neon). As
funcionalidades são as da `v0.1.0-rc.1` (Fase 1 do MVP); muda a forma de
testar, publicar e pôr em produção.

É uma versão `0.x`: a API ainda pode mudar. O sufixo `-rc.1` indica a
primeira candidata à versão `0.2.0`, que ficará para depois da revisão do
design do Frontend.

## Novidades

- **Produção:** Frontend e Backend no Render (imagens do Docker Hub), base de
  dados PostgreSQL 17 no Neon, tudo em Frankfurt, com HTTPS e SSL na ligação
  à base de dados ([ADR 0006](docs/decisions/0006-alojamento-render-e-neon.md)).
- **Release e deploy:** publicar uma release no GitHub etiqueta as imagens
  desse commit com a versão (workflow "Release"); o workflow "Deploy prod"
  põe em produção a release escolhida.
- **Pipeline em 5 etapas:** build; testes unitários, de integração e do
  Frontend; cobertura JaCoCo com mínimo de 95% de linhas e de ramos; imagens
  com smoke tests e E2E; publicação no Docker Hub
  ([ADR 0005](docs/decisions/0005-pipeline-em-etapas-e-publicacao-no-docker-hub.md)).
- **66 testes unitários novos** no Backend: a pirâmide de testes deixa de
  estar invertida.

O detalhe está no [CHANGELOG.md](CHANGELOG.md).

## Requisitos e instalação local

Iguais aos da versão anterior: Docker Engine com Docker Compose v2 (e, para
desenvolver, JDK 21 e Node.js 20.19+ ou 22.12+).

```bash
git clone git@github.com:jmariz-costa/ESMAD-Matriz.git
cd ESMAD-Matriz
git checkout v0.2.0-rc.1
cp backend/.env.example backend/.env    # mudar as três palavras-passe
docker compose up -d --build
```

Abrir http://localhost:8090. As contas de demonstração estão no
[README](README.md#contas-de-demonstração).

## Qualidade

| Verificação | Resultado nesta versão |
|-------------|------------------------|
| Testes do Backend | 112 unitários e 123 de integração, sem falhas |
| Cobertura do Backend (JaCoCo) | 98,9% de linhas e 95,6% de ramos (mínimo: 95% e 95%) |
| Testes do Frontend (`npm test`) | 102, sem falhas |
| Teste E2E (Playwright, Firefox) | 1, passa contra as imagens na pipeline |
| Pipeline de CI | Verde nas 5 etapas; imagens publicadas no Docker Hub |
| Produção | Serviços criados pelo Blueprint; esquema, Administrador e dados de demonstração criados no Neon; entrar como a Ana funciona |

## Documentação

[README.md](README.md), [docs/deployment.md](docs/deployment.md) (Docker,
pipeline, produção, release e deploy), [render.yaml](render.yaml),
[docs/test-plan.md](docs/test-plan.md) e [docs/decisions/](docs/decisions/)
(ADR 0005 e 0006).

## Problemas conhecidos e limitações

- **Fase 2 por fazer:** arquivar projetos (US08), "As minhas tarefas" (US11),
  editar e apagar comentários (US12 crit. 3 e 4), consultar o histórico
  (US13), consultar as tarefas eliminadas (US09 crit. 4), Administrador
  (US14) e registo com confirmação por email (US01).
- **Plano gratuito em produção:** os serviços adormecem ao fim de 15 minutos
  sem pedidos e demoram cerca de 1 minuto a acordar; não há cópias de
  segurança da base de dados configuradas por nós.
- **Imagens públicas** no Docker Hub (sem `.env` nem palavras-passe).
- O "Deploy prod" confirma que a produção responde, mas não a versão que
  ficou a correr (vê-se nos Events dos serviços no Render).
- A cobertura do arranque (Administrador e dados de demonstração) é
  incidental, e a margem nos ramos é pequena.
- Mantêm-se as limitações da `v0.1.0-rc.1`: erros 500 fora do formato único
  (R3), pesquisa que distingue acentos, Q2, Q4, O1, sem análise de
  vulnerabilidades das imagens nem auditoria das dependências do Maven.
