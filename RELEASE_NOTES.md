# Matriz — versão candidata v0.1.0-rc.1

**Data:** 2026-11-13 · **Estado:** candidata, em validação (Ficha PL 05, Exercício 6) ·
**Não publicada** como release.

## Visão geral

Primeira versão identificada da Matriz, a aplicação web interna de gestão de
projetos e tarefas. Inclui a **Fase 1 do MVP**, e pode ser construída, testada
e executada com Docker Compose e GitHub Actions.

É uma versão `0.x`: a API ainda pode mudar. O sufixo `-rc.1` indica a
primeira candidata à versão `0.1.0`.

## Funcionalidades

- Entrar e sair da conta, com sessão por cookie (expira ao fim de 30 minutos
  sem atividade) e bloqueio depois de 5 tentativas falhadas.
- Projetos: criar, editar, eliminar, pesquisar e paginar; percentagem de
  conclusão; painel inicial com as tarefas por estado.
- Membros: o Gestor adiciona e remove membros do projeto.
- Tarefas: criar, editar e eliminar, com prazo, prioridade e vários
  responsáveis; quatro estados; quadro Kanban com arrastar e filtros.
- Comentários nas tarefas.
- Histórico das alterações das tarefas (registado; a consulta é da Fase 2).

O detalhe está no [CHANGELOG.md](CHANGELOG.md).

## Requisitos

- Docker Engine com Docker Compose v2.
- Para desenvolver: JDK 21, Node.js 20.19+ (ou 22.12+) e npm.

## Instalação

```bash
git clone git@github.com:jmariz-costa/ESMAD-Matriz.git
cd ESMAD-Matriz
git checkout v0.1.0-rc.1
cp backend/.env.example backend/.env    # mudar as três palavras-passe
docker compose up -d --build
```

Abrir http://localhost:8090. As contas de demonstração estão no
[README](README.md#contas-de-demonstração).

## Qualidade

| Verificação | Resultado nesta versão |
|-------------|------------------------|
| Testes do Backend (`./mvnw test`) | 169, sem falhas |
| Testes do Frontend (`npm test`) | 102, sem falhas |
| Teste E2E (Playwright, Firefox) | 1, passa contra o Docker Compose |
| Lint, tipos, build e `npm audit` do Frontend | Sem erros nem vulnerabilidades altas |
| Pipeline de CI (GitHub Actions) | Verde; a falha controlada confirmou que bloqueia o build das imagens |

Os resultados da validação final desta candidata ficam registados no
Exercício 6.

## Documentação

[README.md](README.md), [docs/deployment.md](docs/deployment.md) (execução com
Docker e pipeline), [docs/architecture.md](docs/architecture.md),
[openapi.yaml](openapi.yaml) (contrato da API, versão 0.3.0) e
[docs/decisions/](docs/decisions/).

## Problemas conhecidos e limitações

- **Fase 2 por fazer:** arquivar projetos (US08), "As minhas tarefas" (US11),
  editar e apagar comentários (US12 crit. 3 e 4), consultar o histórico
  (US13), consultar as tarefas eliminadas (US09 crit. 4), Administrador
  (US14) e registo com confirmação por email (US01). Hoje o Administrador só
  vê uma página provisória.
- **Só HTTP:** o HTTPS exigido em produção (S02) depende do alojamento (D04,
  pendente). Com HTTPS, definir `MATRIZ_COOKIE_SEGURO=true`.
- **Sem deployment nem imagens publicadas:** as imagens só são construídas
  localmente e na pipeline.
- Os erros 500 não seguem o formato único de erro da API (R3).
- A pesquisa de projetos distingue acentos.
- O teste de gravações simultâneas não força a sobreposição dos pedidos (Q2).
- Parâmetros de URL desconhecidos são ignorados em silêncio (Q4).
- O texto da US07, crit. 1, não coincide com o desenho aprovado para um
  projeto sem tarefas (O1).
- Não há análise de vulnerabilidades das imagens Docker nem auditoria das
  dependências do Maven.
- **Configuração:** os valores do `backend/.env.example` são públicos; cada
  instalação tem de definir as suas palavras-passe antes do primeiro arranque.
