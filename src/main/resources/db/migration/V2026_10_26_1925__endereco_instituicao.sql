-- Endereço da sede da IES, vindo do arquivo de IES do Censo (fase C, D-58). CEP só com dígitos.
ALTER TABLE instituicao ADD COLUMN endereco_logradouro VARCHAR(200);
ALTER TABLE instituicao ADD COLUMN endereco_numero VARCHAR(20);
ALTER TABLE instituicao ADD COLUMN endereco_complemento VARCHAR(200);
ALTER TABLE instituicao ADD COLUMN endereco_bairro VARCHAR(100);
ALTER TABLE instituicao ADD COLUMN endereco_cep VARCHAR(8);
