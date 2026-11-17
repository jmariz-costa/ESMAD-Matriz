-- Palavra-passe temporária definida pelo Administrador ao repor o acesso (US19, PR20, RI21).
-- Enquanto for verdadeiro, a pessoa só pode mudar a palavra-passe, consultar a sessão e sair.
ALTER TABLE utilizador
    ADD COLUMN deve_mudar_palavra_passe BOOLEAN NOT NULL DEFAULT FALSE;
