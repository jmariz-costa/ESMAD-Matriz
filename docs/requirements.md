# Requisitos — Matriz

Este documento descreve **o que** a Matriz deve fazer. Contém requisitos
confirmados pelo cliente; os pontos que dependem de um pressuposto ainda não
confirmado estão assinalados com *(pressuposto PRxx)*. Os pressupostos, as restrições,
a segurança e as decisões pendentes estão em [constraints.md](constraints.md).
As User Stories e os critérios de aceitação estão em
[user-stories.md](user-stories.md).

## 1. Visão

A Matriz é uma aplicação web interna, para uma única empresa, onde as equipas
organizam projetos, gerem tarefas e acompanham o progresso do trabalho.

Pedido original do cliente:

> "Precisamos de uma aplicação onde as nossas equipas possam organizar
> projetos, gerir tarefas e acompanhar o progresso do trabalho."

## 2. Objetivos

| ID  | Objetivo                                                                 |
|-----|--------------------------------------------------------------------------|
| O1  | Centralizar num só sítio os projetos e as tarefas das equipas.           |
| O2  | Deixar claro quem é responsável por cada tarefa.                         |
| O3  | Tornar visível o progresso de cada projeto e o trabalho de cada pessoa.  |
| O4  | Permitir discutir o trabalho e saber quem alterou o quê e quando.        |
| O5  | Garantir que só pessoas da empresa acedem à informação.                  |

## 3. Tipos de utilizadores

Há três tipos de utilizadores: **Membro**, **Gestor** e **Administrador**.
Qualquer pessoa da empresa com conta é **Membro**. Em cada projeto, um Membro
pode ser **Gestor** desse projeto: a mesma pessoa pode ser Gestora num projeto
e Membro noutro. O **Administrador** é uma conta à parte, que serve apenas para
desativar e reativar contas.

| Tipo           | Quem é                                                         | O que pode fazer                                                                                     |
|----------------|----------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| Membro         | Qualquer pessoa da empresa com conta (exceto o Administrador)  | Criar projetos (tornando-se o seu Gestor) e ver "As minhas tarefas". Só vê os projetos de que faz parte; nesses, pode ver o quadro, criar e editar tarefas, atribuir responsáveis, mudar estados e comentar |
| Gestor         | O Membro que gere um projeto: quem o criou, ou quem o Administrador nomear (US14). Um por projeto — *pressuposto PR01* | Tudo o que o Membro faz, mais: editar e eliminar o projeto, gerir membros, eliminar tarefas, arquivar e reativar o projeto (*pressuposto PR07*) |
| Administrador  | Conta(s) definida(s) na instalação da Matriz, só para gerir contas | Desativar contas de Membros (por exemplo, quando alguém sai da empresa) e reativá-las, corrigir o nome e repor o acesso (US19). Não participa em projetos: só vê o nome dos projetos de um Gestor que está a desativar, para nomear o novo Gestor |

## 4. Regras de negócio confirmadas pelo cliente

| ID   | Regra                                                                                                                         |
|------|-------------------------------------------------------------------------------------------------------------------------------|
| RN01 | A Matriz é usada por uma única empresa.                                                                                       |
| RN02 | Qualquer pessoa pode registar-se, mas só com um email do domínio da empresa, e tem de o confirmar através de um link.         |
| RN03 | O link de confirmação é válido durante 24 horas.                                                                              |
| RN04 | A palavra-passe tem pelo menos 8 caracteres.                                                                                  |
| RN05 | Qualquer Membro pode criar projetos; quem cria um projeto torna-se o seu Gestor.                                              |
| RN06 | Uma pessoa pode pertencer a vários projetos ao mesmo tempo.                                                                   |
| RN07 | O Gestor adiciona e remove membros do seu projeto.                                                                            |
| RN08 | Quando um membro é removido, deixa de ser responsável pelas tarefas não concluídas desse projeto.                              |
| RN09 | O Gestor e os Membros criam e editam tarefas; só o Gestor as elimina.                                                         |
| RN10 | Uma tarefa tem título, descrição, prazo, prioridade (Baixa / Média / Alta) e responsáveis; só o título é obrigatório (*pressuposto PR05*). |
| RN11 | Uma tarefa pode ter vários responsáveis, ou nenhum; a mesma pessoa não pode ficar atribuída duas vezes à mesma tarefa. *(Alterada na Ficha PL 03: antes era no máximo um.)* |
| RN12 | O prazo de uma tarefa pode ser qualquer data, incluindo datas passadas.                                                       |
| RN13 | Há 4 estados fixos: A fazer, Em curso, Em revisão, Concluída. Uma tarefa pode passar de qualquer estado para qualquer outro.  |
| RN14 | Qualquer membro do projeto pode mudar o estado de uma tarefa, incluindo para "Concluída".                                     |
| RN15 | Um projeto terminado pode ser arquivado. Um projeto arquivado é só de leitura e pode ser reativado. *(Alterada na Ficha PL 03: antes os projetos nunca eram eliminados; ver RN22.)* |
| RN16 | As contas de Administrador são definidas na instalação; não existe forma de promover Membros a Administrador.                  |
| RN17 | Ao desativar uma pessoa que é Gestora, o Administrador tem de escolher primeiro um novo Gestor para cada projeto dela.         |
| RN18 | Quando uma conta é desativada, essa pessoa deixa de ser responsável pelas suas tarefas não concluídas. A conta pode ser reativada. |
| RN19 | As contas de Administrador servem apenas para gerir contas: não criam projetos, não são adicionadas a projetos e não têm tarefas atribuídas. |
| RN20 | Um Membro só vê os projetos de que faz parte.                                                                                 |
| RN21 | Um Administrador só desativa e reativa contas de Membros. As contas de Administrador não podem ser desativadas na Matriz; só se alteram na configuração da instalação (D07). |
| RN22 | Só o Gestor edita (nome e descrição) e elimina o projeto. A eliminação pede confirmação, é definitiva e apaga tudo o que o projeto contém (L06). Um projeto arquivado não pode ser editado, mas pode ser eliminado. *(Ficha PL 03)* |
| RN23 | Limites dos textos: nome do projeto entre 3 e 100 caracteres e descrição até 1000; título da tarefa entre 3 e 150 e descrição até 2000; comentário entre 1 e 2000; nome de uma pessoa entre 2 e 150 (US19). Os espaços no início e no fim não contam, e um texto obrigatório só com espaços é recusado. *(Ficha PL 03)* |

## 5. Requisitos funcionais

| ID   | Requisito                                                                                          | Prioridade | User Story |
|------|----------------------------------------------------------------------------------------------------|------------|------------|
| RF01 | Registo de conta com email da empresa e confirmação por link                                       | Alta       | US01       |
| RF02 | Entrar e sair da conta                                                                             | Alta       | US02       |
| RF03 | Criar projetos                                                                                     | Alta       | US03       |
| RF04 | Adicionar e remover membros de um projeto                                                          | Alta       | US04       |
| RF05 | Criar e editar tarefas                                                                             | Alta       | US05       |
| RF06 | Mudar o estado de uma tarefa                                                                       | Alta       | US06       |
| RF07 | Quadro Kanban com as 4 colunas de estado                                                           | Alta       | US07       |
| RF08 | Arquivar e reativar projetos                                                                       | Média      | US08       |
| RF09 | Eliminar tarefas, com registo de quem eliminou e quando                                            | Média      | US09       |
| RF10 | Percentagem de conclusão do projeto                                                                | Média      | US10       |
| RF11 | Lista "As minhas tarefas", com as tarefas por concluir atribuídas ao Membro em todos os projetos, e o histórico das concluídas | Média       | US11       |
| RF12 | Comentários em tarefas                                                                             | Média      | US12       |
| RF13 | Histórico de alterações de cada tarefa                                                             | Média      | US13       |
| RF14 | Desativar e reativar contas (Administrador)                                                        | Alta       | US14       |
| RF16 | Editar e eliminar projetos                                                                         | Alta       | US16       |
| RF17 | Pesquisar projetos pelo nome e filtrar as tarefas por estado e prioridade, com listagens paginadas | Média      | US17       |
| RF18 | Painel inicial com os projetos acessíveis e a contagem de tarefas por estado                       | Média      | US18       |
| RF19 | Editar o nome e repor o acesso de uma conta de Membro (Administrador), com palavra-passe temporária | Média      | US19       |
| RF20 | Mudar a própria palavra-passe (Membro)                                                             | Média      | US20       |

## 6. Requisitos não funcionais

| ID    | Requisito                                                                                                          |
|-------|--------------------------------------------------------------------------------------------------------------------|
| RNF01 | **Plataforma:** aplicação web, só para computador; utilizável sem deslocamento horizontal em ecrãs a partir de 1280 px de largura. |
| RNF02 | **Browser:** apenas Firefox, nas 2 versões principais mais recentes.                                                |
| RNF03 | **Língua:** interface em português de Portugal; os textos devem estar preparados para acrescentar inglês no futuro (*pressuposto PR18*). |
| RNF04 | **Escala:** até 50 pessoas.                                                                                         |
| RNF05 | **Segurança e privacidade:** ver a secção de segurança em [constraints.md](constraints.md).                         |

## 7. Âmbito do MVP

O MVP inclui os requisitos **RF01 a RF14** (User Stories US01 a US14) e,
desde a Ficha PL 03, os requisitos **RF16 a RF18** (US16 a US18). O RF15
(etiquetas, US15) foi retirado do MVP pelo cliente e está na secção seguinte.

Na Ficha PL 03, o RF01 (registo, US01) foi adiado: enquanto não for
implementado, as contas de Membro são criadas por seed (ver L07 em
[constraints.md](constraints.md)).

## 8. Fora do âmbito inicial

Funcionalidades identificadas mas deixadas para depois do MVP:

| Funcionalidade                                         | Motivo                                                       |
|--------------------------------------------------------|--------------------------------------------------------------|
| Interface em inglês                                    | Pedida pelo cliente, adiada para depois do MVP               |
| RF15 — Etiquetas em tarefas e filtro do quadro por etiqueta (US15) | Pedida pelo cliente, adiada para depois do MVP   |
| Recuperação automática de palavra-passe por email ("Esqueci-me") | Adiada pelo cliente; o Administrador repõe o acesso (US19, L01) |
| Anexos em tarefas                                      | Não pedido pelo cliente                                      |
| Notificações (email ou na aplicação)                   | Não pedido pelo cliente                                      |
| Destaque de tarefas em atraso                          | Não pedido pelo cliente                                      |
| App móvel nativa (Android / iOS)                       | Cliente optou por aplicação web                              |
| Utilização em telemóvel                                | Cliente optou por uso apenas em computador                   |
| Suporte a outros browsers além do Firefox              | Cliente optou por suportar apenas o Firefox                  |
| Várias empresas na mesma instalação                    | Cliente indicou uma única empresa                            |
| Estados de tarefa personalizáveis por projeto          | Cliente optou por 4 estados fixos                            |
| Promover Membros a Administrador                       | Cliente optou por Administradores definidos na instalação    |
