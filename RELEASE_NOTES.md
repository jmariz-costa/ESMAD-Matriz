# Matriz — versão v0.2.0

**Data:** 2026-11-18 · **Estado:** versão (não candidata) · Release no GitHub
`v0.2.0`, posta em produção com o workflow "Deploy prod".

As notas das versões anteriores (`v0.1.0-rc.1` e `v0.2.0-rc.1`) estão no
[CHANGELOG.md](CHANGELOG.md) e nas respetivas tags.

## Visão geral

A `0.2.0` junta à Fase 1 do MVP parte da Fase 2: "As minhas tarefas" e a
gestão de contas completa pelo Administrador, que passa a ser quem cria as
contas de Membro (não há registo público). Traz também o novo design, com
tema escuro, e mais verificações automáticas na pipeline.

É uma versão `0.x`: a API ainda pode mudar.

## Novidades

- **Criar contas de Membro** (US21): o Administrador cria a conta com nome,
  email e uma palavra-passe temporária; se o domínio da empresa estiver
  configurado (`MATRIZ_DOMINIO_EMAIL`), só aceita emails desse domínio.
  Substitui o registo público (US01), que foi retirado.
- **Gestão de contas** (US14, US19): pesquisar e filtrar as contas de Membro,
  desativar (com a escolha do novo Gestor de cada projeto da pessoa) e
  reativar, editar o nome e repor o acesso com uma palavra-passe temporária.
- **Palavra-passe** (US20, PR20): cada Membro muda a sua a partir do nome no
  cabeçalho; com uma palavra-passe temporária, o servidor só deixa defini-la
  antes de continuar.
- **"As minhas tarefas"** (US11): as tarefas por concluir atribuídas a mim em
  todos os projetos e o histórico das concluídas.
- **Novo design** com tema claro e escuro (botão no cabeçalho).
- **`Makefile`** para configurar, arrancar e correr cada tipo de teste.
- **Regras de arquitetura** do ADR 0001 verificadas com ArchUnit, num passo
  próprio da pipeline.

O detalhe está no [CHANGELOG.md](CHANGELOG.md).

## Atualizar uma instalação existente

- **Base de dados:** no arranque, o Backend aplica a migração Flyway V2
  (acrescenta `deve_mudar_palavra_passe` à tabela `utilizador`). Os dados
  existentes mantêm-se.
- **Configuração:** nova variável opcional `MATRIZ_DOMINIO_EMAIL` (domínio
  das contas criadas pelo Administrador, sem `@`). Quem já tem um
  `backend/.env` pode acrescentá-la; o `.env.example` traz `empresa.pt`. Sem
  ela, o domínio não é verificado.

## Requisitos e instalação local

Docker Engine com Docker Compose v2 (e, para desenvolver, JDK 21 e Node.js
20.19+ ou 22.12+).

```bash
git clone git@github.com:jmariz-costa/ESMAD-Matriz.git
cd ESMAD-Matriz
git checkout v0.2.0
make configurar     # cria o backend/.env com palavras-passe aleatórias
make arrancar       # http://localhost:8090
```

Sem `make`: `cp backend/.env.example backend/.env`, mudar as três
palavras-passe e `docker compose up -d --build`. As contas de demonstração
estão no [README](README.md#contas-de-demonstração).

## Qualidade

| Verificação | Resultado nesta versão |
|-------------|------------------------|
| Regras de arquitetura (ArchUnit) | 3, sem falhas |
| Testes do Backend | 160 unitários (incluindo as 3 regras) e 166 de integração, sem falhas |
| Cobertura do Backend (JaCoCo) | 99,0% de linhas e 96,5% de ramos (mínimo: 95% e 95%) |
| Testes do Frontend (`npm test`) | 131, sem falhas |
| Teste E2E (Playwright, Firefox) | 1, passa contra as imagens na pipeline |
| Pipeline de CI | Verde nas 5 etapas; imagens publicadas no Docker Hub |
| Produção | Ver o registo do Deploy prod da `v0.2.0` em [docs/deployment.md](docs/deployment.md) §8 |

## Documentação

[README.md](README.md), [docs/user-stories.md](docs/user-stories.md) (US19
a US21), [docs/deployment.md](docs/deployment.md) (Docker, pipeline,
produção, release e deploy), [docs/test-plan.md](docs/test-plan.md) e
[docs/decisions/](docs/decisions/) (ADR 0001 com a verificação automática e
os ciclos entre módulos).

## Problemas conhecidos e limitações

- **Fase 2 por fazer:** arquivar projetos (US08), editar e apagar
  comentários (US12 crit. 3 e 4), consultar o histórico (US13) e consultar
  as tarefas eliminadas (US09 crit. 4).
- **Criar contas:** dois pedidos simultâneos com o mesmo email dão 500 no
  segundo (a base de dados impede a conta repetida); improvável com um só
  Administrador.
- **Plano gratuito em produção:** os serviços adormecem ao fim de 15 minutos
  sem pedidos e demoram cerca de 1 minuto a acordar; não há cópias de
  segurança da base de dados configuradas por nós.
- **Imagens públicas** no Docker Hub (sem `.env` nem palavras-passe).
- O "Deploy prod" confirma que a produção responde, mas não a versão que
  ficou a correr (vê-se nos Events dos serviços no Render).
- O `npm audit` da pipeline depende de uma base de dados de avisos externa:
  pode falhar sem alterações ao código (aconteceu com o commit `d987c38`,
  por um aviso do `picomatch` que o GitHub depois retirou).
- Mantêm-se as limitações anteriores: erros 500 fora do formato único (R3),
  pesquisa que distingue acentos, Q2, Q4, O1, sem análise de vulnerabilidades
  das imagens nem auditoria das dependências do Maven.
