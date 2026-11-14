# 0004 — Autenticação: sessão guardada na base de dados, com cookie

## Estado

Aceite. Decisão tomada a 2026-11-01 (Ficha PL 03, D06 em
[constraints.md](../constraints.md)); registada como ADR a 2026-11-14.

**Registo retroativo:** na altura, só ficou registada a escolha. As
alternativas e a justificação abaixo foram reconstruídas depois, a partir dos
requisitos e do código existentes, e não são uma ata do que foi pesado na
altura.

## Contexto

A Matriz é uma aplicação web interna, usada por uma única empresa (RN01),
com um único cliente: o Frontend React, servido na mesma origem que a API
(proxy do Vite em desenvolvimento, nginx no Docker Compose; ADR 0002).

Requisitos que a autenticação tem de cumprir:

- palavras-passe guardadas só como hash próprio para palavras-passe (S01);
- a mensagem de erro do login não diz se falhou o email ou a palavra-passe
  (PR03), e há bloqueio depois de 5 falhas (D09);
- a sessão expira ao fim de 30 minutos **sem atividade**, e cada pedido
  renova o prazo (D06);
- sair termina a sessão, e **desativar uma conta termina de imediato** todas
  as sessões dessa pessoa (PR16, US14, da Fase 2);
- as permissões são sempre verificadas no servidor (S03).

## Opções consideradas

**A. Sessão guardada na base de dados, identificada por um cookie.**
O servidor gera um token aleatório, envia-o num cookie `HttpOnly`,
`SameSite=Strict`, e guarda apenas o hash do token na tabela `sessao`.

- ✅ Revogação imediata: apagar a linha da sessão termina-a no pedido
  seguinte (logout, PR16).
- ✅ A expiração por inatividade é natural (`ultima_atividade`, atualizada em
  cada pedido).
- ✅ O cookie `HttpOnly` não é acessível a JavaScript; `SameSite=Strict` reduz
  o risco de CSRF, e a mesma origem dispensa CORS.
- ✅ Se a base de dados for lida por alguém, os hashes não servem como
  tokens.
- ❌ Cada pedido autenticado consulta a base de dados (aceitável para até 50
  pessoas, RNF04).
- ❌ Depende da mesma origem: um cliente noutro domínio exigiria outra
  solução.

**B. JWT sem estado (assinado pelo servidor e guardado no cliente).**

- ✅ O servidor não precisa de guardar sessões; útil com vários serviços ou
  clientes.
- ❌ Um token válido continua válido até expirar: o logout e o PR16 exigiriam
  uma lista de tokens revogados, ou seja, voltar a guardar estado.
- ❌ A expiração por inatividade obrigaria a renovar e reemitir tokens.
- ❌ Guardado no `localStorage`, ficaria exposto a XSS; guardado num cookie,
  perde a vantagem principal face à opção A.
- ❌ A arquitetura é um monólito com um único cliente (D03), por isso a
  vantagem de não ter estado não se aplica.

**C. OAuth 2.0 / OpenID Connect com o fornecedor de identidade da empresa
(SSO).**

- ✅ Encaixaria em "só pessoas da empresa": contas e palavras-passe geridas
  pela empresa, sem palavras-passe guardadas na Matriz.
- ❌ Exige um fornecedor de identidade da empresa, que não está identificado
  nos requisitos, e a configuração da Matriz como cliente desse fornecedor.
- ❌ Os requisitos pedem contas próprias, com registo pelo email da empresa
  (US01) e desativação pelo Administrador (US14).
- ℹ️ Mesmo com SSO, a aplicação continuaria a precisar de uma sessão própria
  depois do login; a opção A não impede esta evolução.

## Decisão

**Opção A:** sessão guardada na base de dados, com um token aleatório
(`SecureRandom`) enviado num cookie `matriz_sessao` (`HttpOnly`,
`SameSite=Strict`, `Path=/api`, e `Secure` com HTTPS) e guardado só como hash
SHA-256 (`TokenSessao.java`). Palavras-passe com bcrypt. Expira ao fim de 30
minutos sem atividade.

## Consequências

- ✅ O logout e a desativação de contas terminam as sessões de imediato.
- ✅ A identidade usada nas regras (Gestor, autor de um comentário, autor do
  histórico) vem sempre da sessão, nunca dos dados do pedido.
- ✅ Frontend e Backend na mesma origem, sem CORS (proxy do Vite e nginx).
- ❌ Uma consulta à tabela `sessao` por pedido autenticado.
- ❌ O cookie só leva `Secure` quando `MATRIZ_COOKIE_SEGURO=true`; em
  produção, depende do HTTPS (S02) e do alojamento (D04, pendente).
- ❌ Passar a SSO (opção C) seria uma decisão nova, e mudaria o registo e a
  gestão de contas.
