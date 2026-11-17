# Restrições, pressupostos e decisões pendentes — Matriz

Este documento separa o que **não foi confirmado pelo cliente** (pressupostos)
do que está em [requirements.md](requirements.md) (requisitos confirmados).
Inclui também os requisitos de segurança e privacidade, as decisões técnicas
(tomadas e pendentes) e as limitações de âmbito do MVP.

## 1. Restrições conhecidas

| ID   | Restrição                                                                                              |
|------|--------------------------------------------------------------------------------------------------------|
| R01  | Nas Fichas PL 01 e PL 02 não é implementado código: apenas análise, especificação e conceção. O MVP é implementado a partir da Ficha PL 03. |
| R02  | A Matriz é usada por uma única empresa.                                                                 |
| R03  | A Matriz é uma aplicação web, usada apenas em computador; não há app móvel nem suporte a telemóvel.     |
| R04  | Deve funcionar apenas no Firefox, nas 2 versões principais mais recentes, em ecrãs a partir de 1280 px de largura. |
| R05  | O número de pessoas previsto é de até 50.                                                              |
| R06  | Só podem ter conta pessoas com email do domínio da empresa.                                            |
| R07  | A interface do MVP é apenas em português de Portugal.                                                  |

### Informação em falta

| ID   | Informação                                                                             |
|------|----------------------------------------------------------------------------------------|
| IF01 | O domínio de email da empresa (ex.: `@empresa.pt`) ainda não foi indicado pelo cliente. |

## 2. Pressupostos assumidos

Detalhes que o cliente não especificou. Foram validados durante a análise,
mas são pressupostos e não requisitos pedidos pelo cliente: podem ser revistos.

| ID   | Pressuposto                                                                                                                                      | Afeta       |
|------|--------------------------------------------------------------------------------------------------------------------------------------------------|-------------|
| PR01 | Cada projeto tem um único Gestor: quem o criou, ou quem o Administrador nomear ao desativar o Gestor anterior (US14).                             | US03, US04  |
| PR02 | O login é feito com email e palavra-passe. Nas contas de Membro, a palavra-passe é definida no registo; nas de Administrador, na instalação (D07). | US01, US02  |
| PR03 | Se o login falhar, a mensagem não indica se o erro foi no email ou na palavra-passe.                                                              | US02        |
| PR04 | Um projeto tem nome obrigatório e descrição opcional.                                                                                             | US03        |
| PR05 | Uma tarefa nova começa no estado "A fazer"; só o título é obrigatório.                                                                            | US05        |
| PR06 | Cada cartão do quadro Kanban mostra título, responsáveis, prioridade e prazo.                                                                     | US07        |
| PR07 | Só o Gestor arquiva e reativa o projeto; num projeto arquivado os membros continuam a poder consultar tudo, numa lista à parte.                    | US08        |
| PR08 | Eliminar uma tarefa pede confirmação e apaga também os seus comentários e histórico; fica apenas um registo com o título, quem eliminou e quando. | US09        |
| PR09 | Percentagem de conclusão = tarefas "Concluída" ÷ total de tarefas × 100, arredondada para baixo às unidades; projeto sem tarefas = 0%.            | US10        |
| PR10 | "As minhas tarefas" mostra só tarefas não concluídas, sem projetos arquivados, ordenadas pelo prazo mais próximo (sem prazo no fim). As concluídas aparecem à parte, num histórico ordenado pela data de conclusão (a mais recente primeiro; crit. 5, 2026-11-17). | US11        |
| PR11 | Um comentário só pode ser editado ou apagado pelo seu autor.                                                                                      | US12        |
| PR12 | O histórico mostra as entradas da mais recente para a mais antiga e ninguém o pode editar ou apagar.                                             | US13        |
| PR13 | As etiquetas são criadas pelos membros e pertencem ao projeto onde foram criadas. (Só se aplica quando a US15 entrar no âmbito.)                 | US15 (fora do MVP) |
| PR14 | O novo Gestor escolhido pelo Administrador pode ser qualquer Membro ativo; se não for membro do projeto, passa a sê-lo. | US14        |
| PR15 | Quem é desativado continua na lista de membros; ao ser reativado volta como Membro, sem recuperar tarefas nem o papel de Gestor.                 | US14        |
| PR16 | Desativar uma conta termina de imediato qualquer sessão aberta dessa pessoa.                                                                      | US14        |
| PR17 | A empresa está sediada na União Europeia, pelo que os dados pessoais estão sujeitos ao RGPD.                                                      | Segurança   |
| PR18 | Os textos da interface são preparados desde o início para acrescentar inglês no futuro, sem refazer a interface.                                 | RNF03       |
| PR19 | As contas de Administrador usam um email da empresa próprio da função (ex.: `admin@empresa.pt`), diferente do email individual da pessoa. Assim, quem é Administrador pode ter também uma conta de Membro com o seu email individual da empresa. | US14, D07   |

## 3. Segurança e privacidade

| ID   | Requisito                                                                                                                        |
|------|----------------------------------------------------------------------------------------------------------------------------------|
| S01  | As palavras-passe nunca são guardadas em texto legível; são guardadas de forma irreversível (hash), com um algoritmo próprio para palavras-passe. |
| S02  | Toda a comunicação entre o browser e o servidor é cifrada (HTTPS).                                                              |
| S03  | As permissões (Membro, Gestor, Administrador) são verificadas no servidor, e não apenas escondendo opções na interface.         |
| S04  | Um Membro só consegue ver projetos, tarefas e comentários dos projetos de que é membro. Única exceção: o Administrador vê o nome dos projetos de uma pessoa que está a desativar (US14). |
| S05  | Os links de confirmação de email são de uso único e expiram ao fim de 24 horas.                                                  |
| S06  | Uma conta de Membro só fica ativa depois de o email ser confirmado. As contas de Administrador são configuradas na instalação (D07). |
| S07  | Só são recolhidos os dados pessoais necessários: nome, email e palavra-passe (minimização de dados, RGPD).                       |
| S08  | Credenciais, chaves e outros segredos nunca são guardados no repositório.                                                        |

### Riscos aceites

| ID   | Risco                                                                                                                                       | Motivo da aceitação                                                                 |
|------|---------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| RA01 | O registo indica quando um email já tem conta (US01, crit. 5), o que permite descobrir que emails estão registados, apesar do PR03.          | Aplicação interna de até 50 pessoas; os emails dos colegas já são conhecidos dentro da empresa. |

## 4. Decisões técnicas

As decisões D01 a D03 e D06 a D09 e D11 foram tomadas nas Fichas PL 02 e
PL 03, e a D04 depois da Ficha PL 05. As restantes continuam pendentes.

| ID   | Decisão                                                                                                   |
|------|-----------------------------------------------------------------------------------------------------------|
| D01  | Linguagem e framework (frontend e backend). **Decidida** (Ficha PL 03): backend em Java com Quarkus; frontend em React (Vite). |
| D02  | Base de dados. **Decidida** (Ficha PL 03): PostgreSQL, executado em Docker; esquema criado por migrations (Flyway). Alternativas e justificação no [ADR 0003](decisions/0003-base-de-dados-relacional-postgresql.md). |
| D03  | Arquitetura da aplicação. **Decidida** (Ficha PL 02): monólito modular, ver [architecture.md](architecture.md). |
| D04  | Alojamento (onde a Matriz vai correr). **Decidida** (2026-11-16): Frontend e Backend no Render (imagens do Docker Hub, região Frankfurt) e PostgreSQL no Neon (Frankfurt), com HTTPS; deploy por release com o workflow "Deploy prod". Alternativas e justificação no [ADR 0006](decisions/0006-alojamento-render-e-neon.md). |
| D05  | Serviço de envio de emails (confirmação de conta).                                                        |
| D06  | Mecanismo de autenticação e de sessão, incluindo o tempo até uma sessão inativa expirar. **Decidida** (Ficha PL 03): sessão guardada na base de dados; o browser recebe um token aleatório num cookie HttpOnly e o servidor guarda apenas o hash do token. A sessão expira ao fim de 30 minutos sem atividade (cada pedido renova o prazo). Sair apaga a sessão; desativar uma conta apaga todas as sessões dessa pessoa (PR16). Alternativas (JWT, SSO) e justificação no [ADR 0004](decisions/0004-autenticacao-sessao-na-base-de-dados.md). |
| D07  | Como são configuradas as contas de Administrador na instalação. **Decidida** (Ficha PL 03): no arranque, se a conta de Administrador ainda não existir, é criada com o email e a palavra-passe indicados em variáveis de ambiente. Uma conta já existente nunca é alterada. |
| D08  | O que acontece quando duas pessoas editam a mesma tarefa ao mesmo tempo (caso CE5). **Decidida** (Ficha PL 03): bloqueio otimista. A tarefa tem um número de versão; se outra pessoa a tiver guardado entretanto, a alteração é recusada (409) e a interface pede para recarregar, sem perder o que foi escrito. |
| D09  | Limite de tentativas de login falhadas e o que acontece quando é ultrapassado. **Decidida** (Ficha PL 03): depois de 5 tentativas falhadas seguidas para o mesmo email, o login com esse email é recusado durante 15 minutos, com a mesma mensagem genérica (PR03). Um login correto volta a pôr o contador a zero. |
| D10  | Como responder a um pedido de apagamento de dados pessoais (RGPD), dado que o histórico mantém nomes.     |
| D11  | Estratégia de testes. **Decidida** (Ficha PL 03): testes de integração ao backend (JUnit + RestAssured) contra um PostgreSQL real criado automaticamente para os testes; testes de componentes ao frontend (Vitest + Testing Library); verificações manuais registadas em [verification.md](verification.md). |

## 5. Limitações de âmbito

Consequências conhecidas das decisões tomadas para o MVP:

| ID   | Limitação                                                                                                                       |
|------|---------------------------------------------------------------------------------------------------------------------------------|
| L01  | Não há recuperação de palavra-passe: quem se esquecer dela não consegue voltar a entrar.                                        |
| L02  | Não há notificações: uma pessoa só sabe que lhe atribuíram uma tarefa ou que comentaram quando abre a Matriz.                  |
| L03  | O estado "Em revisão" é apenas informativo: como os movimentos são livres e qualquer membro pode concluir, nada obriga a rever. |
| L04  | A interface existe apenas em português.                                                                                         |
| L05  | Não é possível anexar ficheiros às tarefas.                                                                                     |
| L06  | Eliminar um projeto é definitivo: apaga também os seus membros, tarefas, responsáveis, comentários, histórico e registo de tarefas eliminadas, sem deixar registo da eliminação. Para guardar os dados, o Gestor deve arquivar em vez de eliminar. |
| L07  | O registo público (US01) foi adiado: enquanto não for implementado, as contas de Membro são criadas por seed (dados de demonstração). |
