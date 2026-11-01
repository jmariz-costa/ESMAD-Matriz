package pt.esmad.matriz.comum.demo;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.agroal.api.AgroalDataSource;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Dados de demonstração fictícios (Ficha PL 03, Exercício 2).
 *
 * <p>São criados no arranque apenas se {@code matriz.seed.demo=true} e ainda não
 * existir nenhum Membro. Nunca apagam nem alteram dados existentes. Todas as
 * contas usam a palavra-passe de {@code MATRIZ_DEMO_PASSWORD}.
 */
@ApplicationScoped
public class DadosDemonstracao {

    private final AgroalDataSource dataSource;
    private final boolean ativo;
    private final String palavraPasse;

    public DadosDemonstracao(AgroalDataSource dataSource,
                             @ConfigProperty(name = "matriz.seed.demo") boolean ativo,
                             @ConfigProperty(name = "matriz.seed.demo-password") String palavraPasse) {
        this.dataSource = dataSource;
        this.ativo = ativo;
        this.palavraPasse = palavraPasse;
    }

    @Transactional
    void aoArrancar(@Observes @Priority(10) StartupEvent evento) throws SQLException {
        if (!ativo) {
            return;
        }
        if (palavraPasse.length() < 8) {
            Log.error("MATRIZ_DEMO_PASSWORD tem de ter pelo menos 8 caracteres; dados de demonstração não criados.");
            return;
        }
        try (Connection ligacao = dataSource.getConnection()) {
            if (existemMembros(ligacao)) {
                return;
            }
            new Gerador(ligacao, BcryptUtil.bcryptHash(palavraPasse)).criar();
            Log.info("Dados de demonstração criados (ana, bruno, carla e daniel @empresa.pt).");
        }
    }

    private static boolean existemMembros(Connection ligacao) throws SQLException {
        try (PreparedStatement select = ligacao.prepareStatement(
                "SELECT 1 FROM utilizador WHERE tipo = 'MEMBRO' LIMIT 1");
             ResultSet resultado = select.executeQuery()) {
            return resultado.next();
        }
    }

    /** Escreve os dados de demonstração numa ligação já aberta. */
    private static final class Gerador {

        private final Connection ligacao;
        private final String hash;
        private final Instant agora = Instant.now();

        Gerador(Connection ligacao, String hash) {
            this.ligacao = ligacao;
            this.hash = hash;
        }

        void criar() throws SQLException {
            UUID ana = utilizador("Ana Exemplo", "ana@empresa.pt");
            UUID bruno = utilizador("Bruno Exemplo", "bruno@empresa.pt");
            UUID carla = utilizador("Carla Exemplo", "carla@empresa.pt");
            utilizador("Daniel Exemplo", "daniel@empresa.pt"); // sem projetos

            UUID website = projeto("Website institucional", "Renovação do site da empresa.", ana,
                    List.of(ana, bruno, carla));
            UUID portal = projeto("Portal do Cliente", "Nova área reservada para clientes.", bruno,
                    List.of(bruno, ana));

            tarefa(website, ana, "Definir mapa do site", "A_FAZER", "ALTA", LocalDate.of(2026, 11, 10), List.of());
            tarefa(website, ana, "Escolher fotografias", "A_FAZER", null, null, List.of(bruno));
            UUID maquete = tarefa(website, ana, "Criar maquete da página inicial", "EM_CURSO", "ALTA",
                    LocalDate.of(2026, 11, 5), List.of(ana, bruno));
            tarefa(website, carla, "Configurar formulário de contacto", "EM_CURSO", "BAIXA",
                    LocalDate.of(2026, 10, 28), List.of(carla));
            tarefa(website, ana, "Validar acessibilidade", "EM_REVISAO", "MEDIA",
                    LocalDate.of(2026, 11, 12), List.of(ana, carla));
            tarefa(website, bruno, "Reunião de arranque", "CONCLUIDA", null, null, List.of(bruno));
            tarefa(website, carla, "Levantamento de conteúdos", "CONCLUIDA", "MEDIA", null, List.of(carla));
            tarefa(website, ana, "Definir paleta de cores", "CONCLUIDA", "BAIXA",
                    LocalDate.of(2026, 10, 20), List.of());

            tarefa(portal, bruno, "Definir requisitos de autenticação", "A_FAZER", "ALTA", null, List.of(bruno));
            tarefa(portal, bruno, "Preparar ambiente de testes", "EM_CURSO", "MEDIA",
                    LocalDate.of(2026, 11, 20), List.of(ana));

            comentario(maquete, bruno, "Já tenho uma primeira versão em papel.");
        }

        private UUID utilizador(String nome, String email) throws SQLException {
            UUID id = UUID.randomUUID();
            executar("""
                    INSERT INTO utilizador (id, nome, email, password_hash, tipo, estado)
                    VALUES (?, ?, ?, ?, 'MEMBRO', 'ATIVO')
                    """, id, nome, email, hash);
            return id;
        }

        private UUID projeto(String nome, String descricao, UUID gestor, List<UUID> membros) throws SQLException {
            UUID id = UUID.randomUUID();
            executar("INSERT INTO projeto (id, nome, descricao, gestor_id) VALUES (?, ?, ?, ?)",
                    id, nome, descricao, gestor);
            for (UUID membro : membros) {
                executar("INSERT INTO membro_projeto (projeto_id, utilizador_id) VALUES (?, ?)", id, membro);
            }
            return id;
        }

        /** Cria a tarefa com a entrada de criação e uma entrada por responsável no histórico (RI12). */
        private UUID tarefa(UUID projeto, UUID autor, String titulo, String estado, String prioridade,
                            LocalDate prazo, List<UUID> responsaveis) throws SQLException {
            UUID id = UUID.randomUUID();
            executar("""
                    INSERT INTO tarefa (id, projeto_id, titulo, prazo, prioridade, estado)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, id, projeto, titulo, prazo == null ? null : Date.valueOf(prazo), prioridade, estado);
            historico(id, autor, "CRIACAO", null, null, null);
            for (UUID responsavel : responsaveis) {
                executar("INSERT INTO atribuicao_tarefa (tarefa_id, utilizador_id) VALUES (?, ?)", id, responsavel);
                historico(id, autor, "ALTERACAO", "responsavel", null, nome(responsavel));
            }
            return id;
        }

        private void comentario(UUID tarefa, UUID autor, String texto) throws SQLException {
            executar("""
                    INSERT INTO comentario (id, tarefa_id, autor_id, texto, criado_em)
                    VALUES (?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), tarefa, autor, texto, Timestamp.from(agora));
        }

        private void historico(UUID tarefa, UUID autor, String tipo, String campo,
                               String anterior, String novo) throws SQLException {
            executar("""
                    INSERT INTO historico_tarefa (id, tarefa_id, autor_id, data_hora, tipo, campo, valor_anterior, valor_novo)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), tarefa, autor, Timestamp.from(agora), tipo, campo, anterior, novo);
        }

        private String nome(UUID utilizador) throws SQLException {
            try (PreparedStatement select = ligacao.prepareStatement("SELECT nome FROM utilizador WHERE id = ?")) {
                select.setObject(1, utilizador);
                try (ResultSet resultado = select.executeQuery()) {
                    resultado.next();
                    return resultado.getString(1);
                }
            }
        }

        private void executar(String sql, Object... parametros) throws SQLException {
            try (PreparedStatement comando = ligacao.prepareStatement(sql)) {
                for (int i = 0; i < parametros.length; i++) {
                    comando.setObject(i + 1, parametros[i]);
                }
                comando.executeUpdate();
            }
        }
    }
}
