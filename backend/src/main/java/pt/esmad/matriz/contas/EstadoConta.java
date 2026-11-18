package pt.esmad.matriz.contas;

public enum EstadoConta {
    /**
     * Sem uso desde que a US01 (registo com confirmação) foi retirada; o esquema da base de dados
     * ainda o aceita (database.md).
     */
    POR_CONFIRMAR,
    ATIVO,
    DESATIVADO
}
