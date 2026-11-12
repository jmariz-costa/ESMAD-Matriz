# Verificação da Matriz

Registo das verificações do MVP (Ficha PL 03). Distingue:

- **testes automatizados** — correm com `cd backend && ./mvnw test` e
  `cd frontend && npm test` (ver [README.md](../README.md));
- **verificações manuais** — feitas por uma pessoa na aplicação a correr, com
  o Backend e a base de dados reais.

Os casos 01 a 09 do Exercício 8 entram no I7.

## Percurso manual no Firefox (I6, Exercício 7)

**Objetivo:** confirmar, em todos os ecrãs, os estados de carregamento, lista
vazia, sucesso e erro; que os dados se mantêm quando uma submissão falha; que
não há submissões repetidas; que listagens, filtros e contagens se atualizam
sem recarregar a página; e que o layout se adapta a larguras menores.

**Preparação:** `docker compose up -d`; `cd backend && ./mvnw quarkus:dev`;
`cd frontend && npm run dev`; abrir http://localhost:5173 no Firefox. Contas
de demonstração com a palavra-passe `MATRIZ_DEMO_PASSWORD` do `backend/.env`:
ana@empresa.pt (Gestora de "Website institucional") e bruno@empresa.pt
(membro desse projeto).

**Executado em:** 2026-11-12, Firefox 141.0 sem janela (Playwright), pelo agente
de IA, a pedido do responsável do projeto, contra a aplicação a correr
(Backend em modo de desenvolvimento, PostgreSQL em Docker, dados de
demonstração). É uma verificação automatizada pelo agente, e não manual: o
script ficou fora do repositório e as capturas de ecrã do P11 foram vistas uma
a uma. Os dados criados no percurso foram eliminados no fim.

| # | Ecrã | Passos | Resultado esperado | Observado |
|---|------|--------|--------------------|-----------|
| P01 | Início de sessão | Carregar em "Entrar" com os campos vazios; depois, email da Ana e palavra-passe errada; depois, a palavra-passe certa. | Mensagens junto aos campos, sem pedido; a seguir, a mensagem genérica, com o email mantido e a palavra-passe limpa; "Entrar" fica inativo durante o pedido; por fim, abre o painel. | ✅ Sem pedido com os campos vazios; "Email ou palavra-passe incorretos.", email mantido, palavra-passe limpa, "A entrar…" inativo; abre "Olá, Ana Exemplo". |
| P02 | Painel inicial | Ver o painel; criar o projeto "Teste I6". | "Olá, Ana Exemplo", os projetos com percentagem e contagens por estado; depois de criar, aparece "Projeto criado." e o novo projeto. | ✅ 3 projetos com percentagem e contagens; "Projeto criado." e "Teste I6" no painel. |
| P03 | Projetos | Pesquisar "web"; pesquisar "zzz"; editar o nome de "Teste I6"; eliminar "Teste I6". | Só "Website institucional"; "Nenhum projeto encontrado."; "Alterações guardadas."; confirmação a explicar o que é apagado, "Projeto eliminado." e o total atualizado. | ✅ "1 projeto encontrado"; "Nenhum projeto encontrado."; "Alterações guardadas."; confirmação "Esta ação é definitiva. Serão apagadas também a tarefa do projeto, com os seus responsáveis, comentários e histórico."; "Projeto eliminado." e total a 0. |
| P04 | Detalhe do projeto | Abrir "Website institucional"; filtrar Prioridade "Alta" e Estado "A fazer"; "Limpar filtros"; "+ Nova tarefa" com título só com espaços e depois "Teste I6"; arrastar o cartão para "Em curso". | 4 colunas com os totais; os filtros combinam-se e o estado mostra só essa coluna; o título só com espaços é recusado sem perder o resto; "Tarefa criada." e o cartão em "A fazer"; ao arrastar, a mensagem de mudança de estado e a percentagem e as contagens atualizadas, sem recarregar. | ✅ Colunas pela ordem; com os dois filtros, só "A fazer" com "Definir mapa do site" (filtros no endereço); "O título é obrigatório." com a descrição mantida; "Tarefa criada."; ao arrastar, "«Teste I6» passou para Em curso." e a contagem "Em curso" de 2 para 3. |
| P05 | Detalhe da tarefa | Abrir "Teste I6": mudar o estado e a prioridade e guardar; atribuir o Bruno e a Carla e remover um; publicar um comentário e um só com espaços; fechar e reabrir. | "Alterações guardadas." e o cartão na coluna certa; os responsáveis mudam logo, também no cartão; o comentário aparece com o autor e a data; o só com espaços é recusado; ao reabrir, tudo continua lá. | ✅ "Alterações guardadas." e o cartão em "Em revisão"; Bruno e Carla atribuídos, Carla removida, o cartão mostra o Bruno; comentário com "Ana Exemplo 12/11/2026, 19:38"; "O comentário não pode estar vazio."; ao reabrir, o responsável e o comentário continuam lá. |
| P06 | Detalhe da tarefa (conflito) | Abrir a mesma tarefa em duas abas; guardar uma alteração numa e depois noutra. | Na segunda, o aviso de versão, "Guardar" inativo e "Recarregar tarefa", que mantém o que foi escrito. | ✅ Aviso de versão e "Guardar" inativo; "Recarregar tarefa" trouxe o título da 1.ª aba e manteve a descrição escrita na 2.ª; ao guardar, ficaram as duas alterações. |
| P07 | Detalhe da tarefa (eliminar) | Como Bruno, abrir a tarefa; como Ana, eliminar "Teste I6". | O Bruno não tem "Eliminar"; a Ana confirma, vê "Tarefa eliminada." e o cartão sai do quadro. | ✅ O Bruno tem "Guardar" mas não "Eliminar"; a Ana confirma, "Tarefa eliminada." e o cartão sai do quadro. |
| P08 | Membros | No projeto "Teste I6" (criado no P02, para não mexer nas atribuições dos dados de demonstração): adicionar o Bruno, atribuí-lo a uma tarefa, removê-lo do projeto e voltar a adicioná-lo. | Confirmação antes de remover; "Bruno Exemplo foi removido do projeto." e o cartão passa a "Sem responsável", sem recarregar a página. | ✅ Confirmação "Deixa de ver o projeto e de ser responsável pelas tarefas não concluídas."; a mensagem de remoção e o cartão a "Sem responsável" sem recarregar. Executado antes do P03, que elimina o projeto. |
| P09 | Erros de ligação | Com um ecrã aberto, parar o Backend e recarregar; voltar a arrancá-lo e carregar em "Tentar novamente". | Mensagem de erro com "Tentar novamente"; depois, os dados voltam. | ✅ "Não foi possível carregar os projetos."; com "Tentar novamente", a lista volta. A falta de ligação foi simulada cortando os pedidos `/api` no browser, em vez de parar o Backend. |
| P10 | Submissões repetidas | Em "+ Nova tarefa", carregar duas vezes seguidas em "Criar". | Só é criada uma tarefa. | ✅ Duplo clique: 1 pedido de criação e 1 cartão. |
| P11 | Larguras menores | No modo de design adaptável do Firefox (Ctrl+Shift+M): 800 px e 500 px de largura, no painel, nos projetos, no quadro e no detalhe da tarefa. | A 800 px, o quadro com 2 colunas; a 500 px, 1 coluna, a tabela de projetos com scroll horizontal, o cabeçalho em duas linhas e os diálogos dentro do ecrã. | ✅ A 800 px, 2 colunas; a 500 px, 1 coluna, tabela com scroll horizontal, cabeçalho em duas linhas, diálogo com 484 px dentro do ecrã, sem scroll horizontal na página. Depois da correção do problema 1. |

**Problemas encontrados**

1. **Tabela de projetos desalinhada em larguras menores** (P11). A 500 px, as
   linhas da coluna "Ações" não acompanhavam a altura das outras células.
   Causa: `display: flex` na própria célula (`.tabela .acoes`, desde o I2), que
   deixa de se comportar como célula da tabela. Correção: a célula volta a ser
   uma célula normal, sem quebra de linha, com margem entre os botões. O P11
   foi repetido com a captura de ecrã e ficou alinhado.

**Notas**

- O campo de data mostra o formato do idioma do browser (no Firefox usado, em
  inglês, MM/DD/AAAA); num Firefox em português aparece DD/MM/AAAA. O valor
  enviado à API é sempre AAAA-MM-DD.
