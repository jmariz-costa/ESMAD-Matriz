-- Esquema inicial da Matriz (docs/database.md).
-- As regras de negócio são aplicadas pelo Backend; as restrições abaixo são uma
-- defesa adicional na base de dados.

CREATE TABLE utilizador (
    id                      UUID PRIMARY KEY,
    nome                    VARCHAR(150) NOT NULL CHECK (btrim(nome) <> ''),
    email                   VARCHAR(254) NOT NULL,
    password_hash           VARCHAR(100) NOT NULL,
    tipo                    VARCHAR(20)  NOT NULL CHECK (tipo IN ('MEMBRO', 'ADMINISTRADOR')),
    estado                  VARCHAR(20)  NOT NULL CHECK (estado IN ('POR_CONFIRMAR', 'ATIVO', 'DESATIVADO')),
    token_confirmacao_hash  VARCHAR(100),
    token_expira_em         TIMESTAMPTZ,
    tentativas_falhadas     INTEGER      NOT NULL DEFAULT 0 CHECK (tentativas_falhadas >= 0),
    bloqueado_ate           TIMESTAMPTZ
);

-- Email único sem distinguir maiúsculas de minúsculas (RI13)
CREATE UNIQUE INDEX utilizador_email_uk ON utilizador (lower(email));

CREATE TABLE sessao (
    id                UUID PRIMARY KEY,
    utilizador_id     UUID        NOT NULL REFERENCES utilizador (id) ON DELETE CASCADE,
    token_hash        VARCHAR(64) NOT NULL UNIQUE,
    criada_em         TIMESTAMPTZ NOT NULL,
    ultima_atividade  TIMESTAMPTZ NOT NULL
);

CREATE INDEX sessao_utilizador_idx ON sessao (utilizador_id);

CREATE TABLE projeto (
    id         UUID PRIMARY KEY,
    nome       VARCHAR(100)  NOT NULL CHECK (btrim(nome) <> ''),
    descricao  VARCHAR(1000),
    arquivado  BOOLEAN       NOT NULL DEFAULT FALSE,
    gestor_id  UUID          NOT NULL REFERENCES utilizador (id)
);

CREATE INDEX projeto_gestor_idx ON projeto (gestor_id);

-- Eliminar um projeto apaga, em cascata, tudo o que ele contém (RI17)
CREATE TABLE membro_projeto (
    projeto_id     UUID NOT NULL REFERENCES projeto (id) ON DELETE CASCADE,
    utilizador_id  UUID NOT NULL REFERENCES utilizador (id),
    PRIMARY KEY (projeto_id, utilizador_id)
);

CREATE INDEX membro_projeto_utilizador_idx ON membro_projeto (utilizador_id);

CREATE TABLE tarefa (
    id          UUID PRIMARY KEY,
    projeto_id  UUID          NOT NULL REFERENCES projeto (id) ON DELETE CASCADE,
    titulo      VARCHAR(150)  NOT NULL CHECK (btrim(titulo) <> ''),
    descricao   VARCHAR(2000),
    prazo       DATE,
    prioridade  VARCHAR(10)   CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA')),
    estado      VARCHAR(20)   NOT NULL DEFAULT 'A_FAZER'
                              CHECK (estado IN ('A_FAZER', 'EM_CURSO', 'EM_REVISAO', 'CONCLUIDA')),
    versao      INTEGER       NOT NULL DEFAULT 0 CHECK (versao >= 0)
);

CREATE INDEX tarefa_projeto_estado_idx ON tarefa (projeto_id, estado);

-- A chave composta impede atribuições repetidas (RN11, CE8)
CREATE TABLE atribuicao_tarefa (
    tarefa_id      UUID NOT NULL REFERENCES tarefa (id) ON DELETE CASCADE,
    utilizador_id  UUID NOT NULL REFERENCES utilizador (id),
    PRIMARY KEY (tarefa_id, utilizador_id)
);

CREATE INDEX atribuicao_tarefa_utilizador_idx ON atribuicao_tarefa (utilizador_id);

CREATE TABLE comentario (
    id          UUID PRIMARY KEY,
    tarefa_id   UUID          NOT NULL REFERENCES tarefa (id) ON DELETE CASCADE,
    autor_id    UUID          NOT NULL REFERENCES utilizador (id),
    texto       VARCHAR(2000) NOT NULL CHECK (btrim(texto) <> ''),
    criado_em   TIMESTAMPTZ   NOT NULL,
    editado_em  TIMESTAMPTZ
);

CREATE INDEX comentario_tarefa_idx ON comentario (tarefa_id, criado_em);

CREATE TABLE historico_tarefa (
    id              UUID PRIMARY KEY,
    tarefa_id       UUID        NOT NULL REFERENCES tarefa (id) ON DELETE CASCADE,
    autor_id        UUID        NOT NULL REFERENCES utilizador (id),
    data_hora       TIMESTAMPTZ NOT NULL,
    tipo            VARCHAR(20) NOT NULL CHECK (tipo IN ('CRIACAO', 'ALTERACAO')),
    campo           VARCHAR(20) CHECK (campo IN ('titulo', 'descricao', 'estado', 'prazo', 'prioridade', 'responsavel')),
    valor_anterior  TEXT,
    valor_novo      TEXT,
    -- O campo fica vazio só na entrada de criação
    CHECK ((tipo = 'CRIACAO') = (campo IS NULL))
);

CREATE INDEX historico_tarefa_tarefa_idx ON historico_tarefa (tarefa_id, data_hora);

CREATE TABLE tarefa_eliminada (
    id             UUID PRIMARY KEY,
    projeto_id     UUID         NOT NULL REFERENCES projeto (id) ON DELETE CASCADE,
    titulo         VARCHAR(150) NOT NULL,
    eliminada_por  UUID         NOT NULL REFERENCES utilizador (id),
    eliminada_em   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX tarefa_eliminada_projeto_idx ON tarefa_eliminada (projeto_id);
