# User Stories — Matriz (MVP)

Backlog inicial do MVP da Matriz. Cada User Story segue o formato
*Como [tipo de utilizador], quero [funcionalidade], para [benefício]* e tem
critérios de aceitação no formato **Dado / Quando / Então** (Given / When / Then).

Termos usados:

- **Membro** — qualquer pessoa da empresa com conta ativa, exceto o
  Administrador. Só vê os projetos de que faz parte. "Membro do projeto" é um
  Membro que faz parte desse projeto (inclui o seu Gestor).
- **Gestor** — o Membro que gere um projeto: quem o criou, ou quem o
  Administrador nomear ao desativar o Gestor anterior (US14). Um único Gestor
  por projeto.
- **Administrador** — conta definida na instalação da Matriz, usada apenas
  para desativar e reativar contas; não participa em projetos.

Prioridades: **Alta** (sem isto o MVP não cumpre o pedido do cliente),
**Média** (importante, mas o MVP funciona sem ela), **Baixa** (valor
complementar).

Requisitos e pressupostos de suporte: ver [requirements.md](requirements.md)
e [constraints.md](constraints.md).

## Resumo do backlog

| ID   | User Story                                         | Prioridade |
|------|----------------------------------------------------|------------|
| US01 | Registo com email da empresa *(adiada, ver L07)*   | Alta       |
| US02 | Entrar e sair da conta                             | Alta       |
| US03 | Criar projeto                                      | Alta       |
| US04 | Gerir membros do projeto                           | Alta       |
| US05 | Criar e editar tarefas                             | Alta       |
| US06 | Mudar o estado de uma tarefa                       | Alta       |
| US07 | Ver o quadro Kanban                                | Alta       |
| US08 | Arquivar e reativar projeto                        | Média      |
| US09 | Eliminar tarefas                                   | Média      |
| US10 | Ver percentagem de conclusão do projeto            | Média      |
| US11 | Ver as minhas tarefas                              | Média      |
| US12 | Comentar tarefas                                   | Média      |
| US13 | Ver histórico de alterações de uma tarefa          | Média      |
| US14 | Desativar e reativar contas (Administrador)        | Alta       |
| US16 | Editar e eliminar projeto                          | Alta       |
| US17 | Pesquisar projetos e filtrar tarefas               | Média      |
| US18 | Ver o painel inicial                               | Média      |

A US15 (etiquetas) ficou fora do MVP: ver a secção [Fora do MVP](#fora-do-mvp)
no fim deste documento. As US16 a US18 foram acrescentadas na Ficha PL 03.
A US01 foi adiada na Ficha PL 03: enquanto não for implementada, as contas de
Membro são criadas por seed (L07 em [constraints.md](constraints.md)).

---

## US01 — Registo com email da empresa · Alta *(adiada)*

Como **pessoa da empresa**, quero registar-me com o meu email da empresa, para
poder aceder à Matriz.

1. **Dado** que não tenho conta, **quando** me registo com nome, email do
   domínio da empresa e palavra-passe com pelo menos 8 caracteres, **então** a
   conta é criada no estado "por confirmar" e recebo um email com um link de
   confirmação.
2. **Dado** que tenho a conta por confirmar, **quando** abro o link nas 24 horas
   seguintes ao registo, **então** a conta fica ativa e posso entrar.
3. **Dado** que tenho a conta por confirmar, **quando** tento entrar, **então**
   o acesso é recusado com a indicação de que falta confirmar o email.
4. **Dado** um email fora do domínio da empresa, **quando** me tento registar,
   **então** o registo é recusado e nenhuma conta é criada.
5. **Dado** um email já registado, **quando** me tento registar com ele,
   **então** o registo é recusado e nenhuma conta nova é criada.
6. **Dado** uma palavra-passe com menos de 8 caracteres, **quando** me tento
   registar, **então** o registo é recusado e nenhuma conta é criada.
7. **Dado** que tenho a conta por confirmar e o link expirou, **quando** peço
   um novo link, **então** recebo um novo email com um link válido durante 24
   horas e o link anterior deixa de funcionar.

## US02 — Entrar e sair da conta · Alta

Como **Membro** ou **Administrador**, quero entrar e sair da minha conta, para
que só eu aceda à minha informação.

1. **Dado** que tenho uma conta de Membro ativa, **quando** entro com o meu
   email e palavra-passe corretos, **então** acedo ao painel inicial, com os
   meus projetos (US18).
2. **Dado** que tenho uma conta de Administrador, **quando** entro com o meu
   email e palavra-passe corretos, **então** acedo apenas à gestão de contas.
3. **Dado** que indico um email ou palavra-passe errados, **quando** tento
   entrar, **então** o acesso é recusado com uma mensagem genérica, que não
   indica qual dos dois está errado.
4. **Dado** que tenho sessão iniciada, **quando** saio da conta, **então** volto
   ao ecrã de entrada e não consigo ver nenhuma página da Matriz sem voltar a
   entrar.

## US03 — Criar projeto · Alta

Como **Membro**, quero criar um projeto, para organizar o trabalho da minha
equipa.

1. **Dado** que sou Membro com sessão iniciada, **quando** crio um projeto
   indicando um nome (e, opcionalmente, uma descrição), **então** o projeto é criado, fico
   como seu Gestor e ele aparece na minha lista de projetos.
2. **Dado** que não indico nome, ou indico um nome só com espaços ou fora dos
   limites (RN23), **quando** tento criar o projeto, **então** a criação é
   recusada.
3. **Dado** que criei um projeto, **quando** outro Membro que não faz parte do
   projeto abre a sua lista de projetos, **então** esse projeto não aparece.

## US04 — Gerir membros do projeto · Alta

Como **Gestor**, quero adicionar e remover membros do meu projeto, para definir
quem trabalha nele.

1. **Dado** que sou Gestor, **quando** adiciono um Membro ativo que ainda
   não está no projeto, **então** ele passa a ver o projeto, como Membro do
   projeto.
2. **Dado** que sou Membro (não Gestor), **quando** abro o projeto, **então**
   não tenho opção para adicionar ou remover membros.
3. **Dado** que um Membro já está no projeto, **quando** o tento adicionar
   outra vez, **então** não fica duplicado.
4. **Dado** um membro responsável por tarefas não concluídas, **quando** o
   Gestor o remove, **então** ele deixa de ser responsável por essas tarefas,
   deixa de ver o projeto e o histórico mantém o nome dele.
5. **Dado** que sou o Gestor, **quando** me tento remover do projeto, **então**
   a ação é recusada.

## US05 — Criar e editar tarefas · Alta

Como **membro**, quero criar e editar tarefas (descrição, prazo, prioridade,
responsáveis), para registar o trabalho a fazer.

1. **Dado** que sou membro de um projeto não arquivado, **quando** crio uma
   tarefa com um título, **então** a tarefa é criada no estado "A fazer" e
   aparece nessa coluna do quadro.
2. **Dado** que estou a criar ou editar uma tarefa, **então** só o título é
   obrigatório; descrição, prazo, prioridade (Baixa / Média / Alta) e
   responsáveis são opcionais.
3. **Dado** que não indico título, ou indico um título só com espaços, ou um
   texto fora dos limites (RN23), **quando** tento guardar a tarefa, **então**
   a operação é recusada e o que escrevi não se perde.
4. **Dado** que escolho responsáveis, **então** só posso escolher entre os
   membros do projeto com conta ativa; posso escolher vários, e a mesma pessoa
   não fica atribuída duas vezes à mesma tarefa.
5. **Dado** que indico um prazo, **então** é aceite qualquer data válida,
   incluindo datas passadas.
6. **Dado** que edito um campo de uma tarefa, **quando** guardo, **então** a
   alteração fica visível para todos os membros e é registada no histórico.
7. **Dado** uma tarefa com responsáveis, **quando** removo um deles, **então**
   só essa atribuição é removida e os outros responsáveis mantêm-se.
8. **Dado** que outra pessoa guardou a mesma tarefa depois de eu a abrir,
   **quando** guardo, **então** a alteração é recusada, é-me pedido que
   recarregue a tarefa e o que escrevi não se perde (D08).

## US06 — Mudar o estado de uma tarefa · Alta

Como **membro**, quero mudar o estado de uma tarefa, para mostrar em que ponto
está o trabalho.

1. **Dado** que sou membro do projeto, **quando** mudo uma tarefa para qualquer
   outro dos 4 estados (A fazer, Em curso, Em revisão, Concluída), **então** ela
   aparece na coluna desse estado no quadro Kanban.
2. **Dado** que mudei o estado, **então** o histórico regista quem, quando, o
   estado anterior e o novo.
3. **Dado** que o projeto está arquivado, **quando** tento mudar o estado de uma
   tarefa, **então** a ação é recusada.

## US07 — Ver o quadro Kanban · Alta

Como **membro**, quero ver as tarefas num quadro Kanban, para perceber de
relance o estado do projeto.

1. **Dado** que abro um projeto do qual sou membro, **então** vejo 4 colunas,
   por esta ordem: A fazer, Em curso, Em revisão, Concluída.
2. **Dado** o quadro do projeto, **então** cada tarefa aparece uma única vez, na
   coluna do seu estado atual.
3. **Dado** o quadro do projeto, **então** cada cartão mostra o título, os
   responsáveis (ou "Sem responsável") e, quando existam, a prioridade e o
   prazo.
4. **Dado** que não sou membro de um projeto, **quando** tento abrir o seu
   quadro, **então** o acesso é recusado.

## US08 — Arquivar e reativar projeto · Média

Como **Gestor**, quero arquivar e reativar o meu projeto, para retirar projetos
terminados da lista sem perder dados.

1. **Dado** que sou Gestor de um projeto ativo, **quando** o arquivo, **então**
   o projeto sai da lista principal dos membros e passa a aparecer numa lista
   de projetos arquivados.
2. **Dado** um projeto arquivado, **então** os membros conseguem consultar as
   tarefas, comentários e histórico, mas não conseguem alterar nada.
3. **Dado** que sou Gestor de um projeto arquivado, **quando** o reativo,
   **então** ele volta à lista principal e pode ser alterado normalmente.
4. **Dado** que sou Membro (não Gestor), **então** não tenho opção para
   arquivar ou reativar o projeto.

## US09 — Eliminar tarefas · Média

Como **Gestor**, quero eliminar tarefas, para remover tarefas criadas por
engano.

1. **Dado** que sou Gestor, **quando** escolho eliminar uma tarefa, **então** é
   pedida confirmação antes de a eliminar.
2. **Dado** que confirmei a eliminação, **então** a tarefa, os seus comentários
   e o seu histórico deixam de aparecer em qualquer vista da Matriz, exceto no
   registo de eliminações.
3. **Dado** que sou Membro (não Gestor), **então** não tenho opção para
   eliminar tarefas.
4. **Dado** que uma tarefa foi eliminada, **então** fica guardado o seu título,
   quem a eliminou e quando, e o Gestor consegue consultar esse registo de
   eliminações no projeto.

## US10 — Ver percentagem de conclusão do projeto · Média

Como **membro**, quero ver a percentagem de conclusão do projeto, para saber
quanto falta.

1. **Dado** um projeto com tarefas, **então** a percentagem mostrada é
   (tarefas "Concluída" ÷ total de tarefas) × 100, arredondada para baixo às
   unidades. Exemplos: 3 concluídas em 4 tarefas → 75%; 199 concluídas em 200
   tarefas → 99%. Só aparece 100% quando todas as tarefas estão concluídas.
2. **Dado** um projeto sem tarefas, **então** a percentagem mostrada é 0%.
3. **Dado** que uma tarefa é criada, eliminada ou muda de estado, **quando**
   volto a ver o projeto, **então** a percentagem reflete essa alteração.

## US11 — Ver as minhas tarefas · Média

Como **Membro**, quero ver as tarefas por concluir atribuídas a mim, de
todos os projetos, para organizar o meu trabalho.

1. **Dado** que sou responsável por tarefas em vários projetos, **quando** abro
   "As minhas tarefas", **então** vejo numa só lista todas as tarefas não
   concluídas atribuídas a mim, cada uma com o nome do projeto e o estado.
2. **Dado** a lista "As minhas tarefas", **então** não aparecem tarefas de
   projetos arquivados.
3. **Dado** a lista "As minhas tarefas", **então** as tarefas estão ordenadas
   por prazo (o mais próximo primeiro) e as tarefas sem prazo aparecem no fim.
4. **Dado** que não tenho tarefas por concluir atribuídas, **então** a lista
   mostra uma mensagem a indicar que não há tarefas.
5. **Dado** "As minhas tarefas", **quando** escolho as concluídas, **então**
   vejo o histórico das tarefas concluídas atribuídas a mim (sem projetos
   arquivados), da conclusão mais recente para a mais antiga, cada uma com o
   nome do projeto e a data de conclusão. *(Acrescentado a 2026-11-17, por
   decisão do responsável do projeto.)*

## US12 — Comentar tarefas · Média

Como **membro**, quero comentar tarefas, para discutir o trabalho com a equipa.

1. **Dado** que sou membro de um projeto não arquivado, **quando** escrevo um
   comentário numa tarefa, **então** ele aparece na tarefa com o meu nome e a
   data e hora.
2. **Dado** um comentário vazio, só com espaços ou com mais de 2000
   caracteres (RN23), **quando** o tento publicar, **então** a publicação é
   recusada.
3. **Dado** que sou o autor de um comentário num projeto não arquivado,
   **então** posso editá-lo ou apagá-lo.
4. **Dado** que não sou o autor de um comentário, **então** não tenho opção para
   o editar ou apagar.

## US13 — Ver histórico de alterações de uma tarefa · Média

Como **membro**, quero ver o histórico de alterações de uma tarefa, para saber
quem mudou o quê e quando.

1. **Dado** uma tarefa, **então** o histórico regista a sua criação e cada
   alteração de título, descrição, estado, prazo e prioridade, com quem fez,
   quando, o valor anterior e o novo, e cada responsável atribuído ou
   removido, com quem fez e quando.
2. **Dado** o histórico de uma tarefa, **então** as entradas aparecem da mais
   recente para a mais antiga.
3. **Dado** o histórico de uma tarefa, **então** ninguém consegue editar nem
   apagar as suas entradas.

## US14 — Desativar e reativar contas · Alta

Como **Administrador**, quero desativar e reativar contas, para que pessoas que
saíram da empresa deixem de ter acesso à Matriz.

1. **Dado** que sou Administrador, **quando** desativo uma conta de Membro
   ativa, **então** essa pessoa deixa de conseguir aceder à Matriz, incluindo se
   tiver uma sessão iniciada.
2. **Dado** que desativei uma conta, **então** essa pessoa deixa de ser
   responsável pelas tarefas não concluídas que lhe estavam atribuídas,
   incluindo em projetos arquivados, e o histórico mantém o nome dela.
3. **Dado** que a pessoa a desativar é Gestora de um ou mais projetos
   (incluindo arquivados), **quando** a tento desativar, **então** vejo o nome
   desses projetos e a desativação só é concluída depois de eu escolher um novo
   Gestor para cada um, entre os Membros ativos; se o escolhido não for
   membro do projeto, passa a sê-lo.
4. **Dado** que sou Administrador, **então** as ações desta User Story são a
   única exceção às regras "projeto arquivado é só de leitura" e "só os membros
   veem um projeto": não me dão acesso ao conteúdo dos projetos (tarefas,
   comentários, histórico).
5. **Dado** uma conta desativada, **então** essa pessoa não pode ser adicionada
   a projetos nem escolhida como responsável de tarefas.
6. **Dado** uma conta desativada, **quando** a reativo, **então** a pessoa volta
   a conseguir entrar e a aceder aos projetos de que faz parte, como Membro
   do projeto, sem recuperar tarefas nem o papel de Gestor.
7. **Dado** que sou Administrador, **quando** tento desativar a minha própria
   conta ou a de outro Administrador, **então** a ação é recusada.
8. **Dado** que não sou Administrador, **então** não tenho acesso às opções de
   desativar ou reativar contas.
9. **Dado** que entro com uma conta de Administrador, **então** só tenho acesso
   à gestão de contas: não posso criar projetos, ser adicionado a projetos nem
   ter tarefas atribuídas.

## US16 — Editar e eliminar projeto · Alta

Como **Gestor**, quero editar e eliminar o meu projeto, para corrigir os seus
dados ou retirar um projeto que já não é necessário. *(Ficha PL 03)*

1. **Dado** que sou Gestor de um projeto não arquivado, **quando** altero o
   nome ou a descrição e guardo, **então** a alteração fica visível para todos
   os membros.
2. **Dado** que indico um nome vazio, só com espaços ou fora dos limites
   (RN23), **quando** guardo, **então** a alteração é recusada e o que escrevi
   não se perde.
3. **Dado** que sou Gestor, **quando** escolho eliminar o projeto, **então** é
   pedida confirmação antes de o eliminar.
4. **Dado** que confirmei a eliminação, **então** o projeto e tudo o que
   contém (membros, tarefas, responsáveis, comentários, histórico e registo de
   tarefas eliminadas) deixam de existir (L06).
5. **Dado** um projeto arquivado, **então** o Gestor não o pode editar, mas
   pode eliminá-lo (RN22).
6. **Dado** que sou Membro (não Gestor), **então** não tenho opção para editar
   ou eliminar o projeto.

## US17 — Pesquisar projetos e filtrar tarefas · Média

Como **membro**, quero pesquisar projetos e filtrar tarefas, para encontrar
depressa o que procuro. *(Ficha PL 03)*

1. **Dado** a lista dos meus projetos, **quando** pesquiso por parte do nome,
   sem distinguir maiúsculas de minúsculas, **então** só aparecem os projetos
   de que faço parte cujo nome contém esse texto.
2. **Dado** uma pesquisa sem resultados, **então** é mostrada uma mensagem a
   indicar que não há projetos.
3. **Dado** as tarefas de um projeto, **quando** filtro por estado e/ou por
   prioridade (incluindo "Sem prioridade"), **então** só aparecem as tarefas
   que cumprem todos os filtros escolhidos, e posso limpar os filtros para
   voltar a ver todas.
4. **Dado** uma listagem com mais resultados do que cabem numa página,
   **então** os resultados são mostrados por páginas e os totais indicados
   referem-se a todos os resultados, e não só à página visível.

## US18 — Ver o painel inicial · Média

Como **Membro**, quero ver um painel inicial com os meus projetos, para
perceber de relance o estado do trabalho. *(Ficha PL 03)*

1. **Dado** que entrei na Matriz, **então** o painel mostra o meu nome e os
   projetos não arquivados de que faço parte, cada um com o número de tarefas
   em cada um dos 4 estados.
2. **Dado** o painel, **quando** escolho um projeto, **então** abro o detalhe
   desse projeto.
3. **Dado** que não faço parte de nenhum projeto, **então** o painel mostra
   uma mensagem a indicar que não há projetos e a opção para criar um.
4. **Dado** que uma tarefa é criada, eliminada ou muda de estado, **quando**
   volto ao painel, **então** as contagens refletem essa alteração.

---

## Casos excecionais e casos-limite

| ID  | Situação                                                  | Comportamento esperado                                               | Referência     |
|-----|-----------------------------------------------------------|----------------------------------------------------------------------|----------------|
| CE1 | O Gestor remove um membro com tarefas atribuídas          | O membro deixa de ser responsável pelas tarefas não concluídas       | US04 (crit. 4) |
| CE2 | O Gestor tenta remover-se do próprio projeto              | Ação recusada                                                        | US04 (crit. 5) |
| CE3 | O link de confirmação é aberto depois das 24 horas        | Link recusado; a pessoa pode pedir um novo link de confirmação       | US01 (crit. 7) |
| CE4 | Tentativa de alterar qualquer coisa num projeto arquivado | Ação recusada; o projeto é só de leitura (única exceção: US14)       | US06, US08     |
| CE5 | Duas pessoas editam a mesma tarefa ao mesmo tempo         | A segunda gravação é recusada; a interface pede para recarregar, sem perder o que foi escrito (D08) | US05 (crit. 8) |
| CE6 | O Administrador desativa uma pessoa que é Gestora         | Tem de escolher primeiro um novo Gestor para cada projeto dela       | US14 (crit. 3) |
| CE7 | O Administrador tenta desativar a sua conta ou a de outro Administrador | Ação recusada                                          | US14 (crit. 7) |
| CE8 | Atribuir a uma tarefa uma pessoa que já é sua responsável | A atribuição não fica duplicada                                      | US05 (crit. 4) |
| CE9 | O Gestor elimina um projeto arquivado                     | Eliminação permitida, com confirmação                                | US16 (crit. 5) |

---

## Fora do MVP

User Stories já analisadas, mas que o cliente decidiu deixar para depois do
MVP. Ficam aqui guardadas para uma fase futura e **não devem ser implementadas**
no MVP.

### US15 — Etiquetar tarefas e filtrar o quadro por etiqueta · Baixa

Como **membro**, quero adicionar etiquetas às tarefas, para agrupar tarefas
semelhantes.

1. **Dado** que sou membro de um projeto não arquivado, **quando** adiciono a
   uma tarefa uma etiqueta nova ou já existente no projeto, **então** a
   etiqueta aparece no cartão da tarefa.
2. **Dado** que uma tarefa já tem uma etiqueta, **quando** a tento adicionar
   outra vez, **então** não fica duplicada.
3. **Dado** as etiquetas de um projeto, **então** não aparecem em nenhum outro
   projeto.
4. **Dado** o quadro Kanban, **quando** escolho filtrar por uma etiqueta,
   **então** só aparecem as tarefas que têm essa etiqueta, e posso remover o
   filtro para voltar a ver todas.
5. **Dado** uma tarefa com uma etiqueta, **quando** a removo dessa tarefa,
   **então** deixa de aparecer no cartão.

Quando a US15 entrar no âmbito, será preciso acrescentar as etiquetas aos
critérios da US05 (campos da tarefa), da US07 (cartão do quadro) e da US13
(histórico).
