CREATE TABLE transacoes (
    id UUID PRIMARY KEY,
    parceiro_id VARCHAR(100) NOT NULL,
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('CREDITO', 'DEBITO')),
    valor NUMERIC(15, 2) NOT NULL CHECK (valor > 0),
    descricao VARCHAR(500),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDENTE', 'CONCLUIDA', 'FALHA')),
    chave_idempotencia VARCHAR(255) UNIQUE,
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transacoes_parceiro_id ON transacoes(parceiro_id);
CREATE INDEX idx_transacoes_status ON transacoes(status);
CREATE INDEX idx_transacoes_criado_em ON transacoes(criado_em);
