# 0001 — Arquitetura interna do Backend: camadas por módulo com DDD leve

## Estado

Aceite (2026-11-01)

## Contexto

A Matriz é um monólito modular (D03, [architecture.md](../architecture.md))
implementado em Java 21 com Quarkus e PostgreSQL (D01, D02). Falta decidir
como cada módulo (Contas, Projetos, Tarefas, Comentários, Histórico) se
organiza por dentro.

Fatores relevantes:

- **Dimensão:** cerca de 20 endpoints na Fase 1 e até 50 pessoas (RNF04).
- **Regras de negócio que têm de ser sempre cumpridas** (invariantes), por
  exemplo: a mesma pessoa não pode ficar atribuída duas vezes à mesma tarefa
  (RN11); só membros ativos podem ser responsáveis (RI02); nada se altera num
  projeto arquivado (RI10); o Gestor é sempre membro e não pode ser removido
  (RI01); uma alteração com versão desatualizada é recusada (D08); cada
  alteração de uma tarefa gera histórico (RI12).
- **Regras que cruzam módulos:** remover um membro mexe nas tarefas (RN08);
  eliminar um projeto apaga tudo o que ele contém (RI17).
- **Framework:** o Quarkus com Panache trabalha naturalmente com entidades JPA.
- **Testes:** as regras devem poder ser testadas (D11).

## Opções consideradas

**A. Camadas simples por módulo.** Recurso REST → serviço → entidade
(Panache *active record*). As regras ficam nos serviços; as entidades são só
dados.

- ✅ Rápido de escrever e natural no Quarkus.
- ❌ As invariantes ficam dispersas pelos serviços e é fácil esquecer uma
  verificação num caminho novo.
- ❌ As regras só se testam com base de dados.

**B. Camadas por módulo com DDD leve.** A mesma estrutura em camadas, mas as
entidades têm comportamento e protegem as suas invariantes (agregados); os
serviços orquestram a transação, as permissões e a comunicação entre módulos.

- ✅ Cada invariante tem um único sítio no código, dentro do agregado.
- ✅ As regras dos agregados podem ser testadas sem base de dados.
- ✅ Encaixa no Quarkus, usando o *repository pattern* do Panache.
- ❌ As entidades continuam a ser entidades JPA: o domínio não fica
  independente do framework.

**C. Onion / Hexagonal com DDD.** Núcleo de domínio em Java puro, portas
(interfaces) e adaptadores para REST e JPA, com entidades JPA separadas do
modelo de domínio.

- ✅ Domínio totalmente independente do framework e da base de dados.
- ✅ Máxima testabilidade.
- ❌ Cerca do dobro das classes (domínio, portas, adaptadores, conversões entre
  modelos) para um MVP pequeno.
- ❌ Vai contra o modelo do Panache, que assume entidades JPA.

## Decisão

Escolhemos a **opção B**, porque protege as invariantes num único sítio sem o
custo de manter dois modelos, adequado à dimensão do MVP.

Regras de organização:

1. **Cada módulo é um package** e corresponde a um contexto delimitado: `contas`,
   `projetos`, `tarefas`, `comentarios` e `historico`, mais um package `comum`
   (formato de erro, autenticação, configuração).
2. **Dentro de cada módulo há três camadas:**
   - **recurso** — endpoints REST; recebe e devolve DTOs (records Java), nunca
     entidades;
   - **serviço** — caso de uso: abre a transação, verifica permissões, carrega
     os agregados, chama o seu comportamento e grava;
   - **domínio e persistência** — entidades JPA com comportamento e
     repositórios Panache (*repository pattern*).
3. **Agregados principais:**
   - `Projeto`, com os seus membros: adicionar ou remover membros, editar,
     regras de arquivo e do Gestor;
   - `Tarefa`, com as suas atribuições: editar, mudar estado, atribuir e
     remover responsáveis, controlo de versão;
   - `Utilizador` e `Sessao`, no módulo de contas.
4. **As invariantes ficam no agregado.** Por exemplo, `tarefa.atribuir(pessoa)`
   recusa uma atribuição repetida, e `projeto.garantirAlteravel()` recusa
   alterações num projeto arquivado. Os serviços não repetem estas
   verificações.
5. **Entre módulos, só através do serviço do outro módulo.** Um módulo nunca
   usa diretamente as entidades ou os repositórios de outro. Por exemplo,
   Tarefas pergunta a Projetos se uma pessoa é membro ativo; não lê a tabela
   de membros.
6. **As permissões são verificadas nos serviços**, a partir da identidade da
   sessão (S03), antes de qualquer alteração.

## Consequências

- ✅ Cada regra de negócio tem um sítio previsível, o que facilita a revisão
  do código e a orientação de agentes de IA.
- ✅ Testes unitários aos agregados, sem base de dados, para as invariantes;
  testes de integração (RestAssured) para permissões e persistência.
- ✅ Os DTOs separam o contrato da API (openapi.yaml) do modelo interno.
- ❌ O domínio depende de anotações JPA. Se um dia for preciso separá-lo,
  terá de ser feito à parte.
- ❌ Há conversões entre entidades e DTOs em cada recurso.
- ❌ É preciso disciplina para não pôr regras nos serviços quando pertencem a
  um agregado; este ponto é verificado na revisão de cada incremento.

## Verificação automática das regras (2026-11-18)

As regras 1, 2 e 5 são verificadas pelo teste unitário
`backend/src/test/java/pt/esmad/matriz/ArquiteturaTest.java`, com a
biblioteca ArchUnit. Se alguma for violada, a etapa de testes unitários da
pipeline falha.

- **Regra 2 (camadas):** os recursos não usam repositórios nem entidades; os
  repositórios só são usados pelos serviços; as entidades só são usadas pelos
  serviços e pelos repositórios. As camadas distinguem-se pelo sufixo do nome
  (`*Recurso`, `*Servico`, `*Repositorio`) e pela anotação `@Entity`. A classe
  `AcessoProjeto` não tem o sufixo `Servico`, mas conta como parte da camada de
  serviço: apoia os serviços de Projetos, carregando o projeto e verificando o
  acesso.
- **Regra 5 (entre módulos):** um módulo não usa entidades, repositórios nem
  recursos de outro módulo.
- **Regra 1 (`comum`):** o package `comum` não depende de nenhum módulo.

As regras 3, 4 e 6 (agregados, invariantes no agregado e permissões nos
serviços) não se podem verificar assim e continuam a ser verificadas na revisão
de cada incremento.

## Dependências circulares entre módulos (2026-11-18)

Há três ciclos entre módulos, todos através de serviços, como a regra 5 exige:

- **Contas ↔ Projetos:** Contas usa o `GestoresServico` (desativar um Gestor
  obriga a indicar um novo Gestor para cada projeto que ele gere, US14);
  Projetos usa o `ContasServico` (nomes das pessoas e se uma conta está ativa).
- **Contas ↔ Tarefas:** Contas usa o `AtribuicoesServico` (desativar uma conta
  retira-a das tarefas não concluídas, US14); Tarefas usa o `ContasServico`
  (nomes das pessoas).
- **Projetos ↔ Tarefas:** Projetos usa o `AtribuicoesServico` (remover um membro
  retira-o das tarefas não concluídas, RN08) e o `ContagemTarefasServico`
  (contagens por estado); Tarefas usa o `AcessoProjetosServico` (acesso ao
  projeto e se uma pessoa é membro ativo).

São um compromisso aceite: nenhum módulo mexe nas entidades nem nas tabelas do
outro, e num monólito desta dimensão o custo de os eliminar é maior do que o
benefício. Por isso o `ArquiteturaTest` não verifica a ausência de ciclos.

Se um dia for preciso eliminá-los (por exemplo, para separar um módulo), o
caminho é o módulo de cima publicar eventos em vez de chamar o de baixo (por
exemplo, Projetos anuncia «membro removido» e Tarefas reage, sem Projetos
conhecer Tarefas), ou mover a orquestração para um serviço acima de ambos.
