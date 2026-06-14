USE `sauf_br`;

-- Desliga a checagem de chaves para podermos recriar as tabelas sem erro
SET FOREIGN_KEY_CHECKS=0; 

-- -----------------------------------------------------
-- 1. TABELAS INDEPENDENTES
-- -----------------------------------------------------

DROP TABLE IF EXISTS `instituicoes`;
CREATE TABLE `instituicoes` (
  `id` char(36) NOT NULL,
  `codigo_emec` varchar(50) DEFAULT NULL,
  `nome` varchar(255) NOT NULL,
  `sigla` varchar(20) DEFAULT NULL,
  `tipo` varchar(100) DEFAULT NULL,
  `estado` char(2) DEFAULT NULL,
  `cidade` varchar(100) DEFAULT NULL,
  `endereco` varchar(255) DEFAULT NULL,
  `cep` varchar(10) DEFAULT NULL,
  `site` varchar(255) DEFAULT NULL,
  `logo_url` varchar(500) DEFAULT NULL,
  `ativa` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `formas_ingresso`;
CREATE TABLE `formas_ingresso` (
  `id` char(36) NOT NULL,
  `nome` varchar(100) NOT NULL,
  `descricao` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `parceiros`;
CREATE TABLE `parceiros` (
  `id` char(36) NOT NULL,
  `nome` varchar(255) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `telefone` varchar(30) DEFAULT NULL,
  `plano` varchar(100) DEFAULT NULL,
  `ativo` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `questionarios`;
CREATE TABLE `questionarios` (
  `id` char(36) NOT NULL,
  `titulo` varchar(255) DEFAULT NULL,
  `descricao` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `termos_uso`;
CREATE TABLE `termos_uso` (
  `id` char(36) NOT NULL,
  `versao` varchar(20) NOT NULL,
  `conteudo` text NOT NULL,
  `data_publicacao` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `usuarios`;
CREATE TABLE `usuarios` (
  `id` char(36) NOT NULL,
  `nome_completo` varchar(200) NOT NULL,
  `cpf` varchar(14) NOT NULL,
  `email` varchar(255) NOT NULL,
  `senha_hash` varchar(255) NOT NULL,
  `email_confirmado` tinyint(1) DEFAULT '0',
  `ativo` tinyint(1) DEFAULT '1',
  `data_cadastro` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `cpf` (`cpf`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `eventos_academicos`;
CREATE TABLE `eventos_academicos` (
  `id` char(36) NOT NULL,
  `nome` varchar(255) NOT NULL,
  `descricao` text,
  `local_evento` varchar(255) DEFAULT NULL,
  `data_evento` datetime DEFAULT NULL,
  `link_inscricao` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `noticias`;
CREATE TABLE `noticias` (
  `id` char(36) NOT NULL,
  `titulo` varchar(255) NOT NULL,
  `conteudo` text,
  `categoria` varchar(100) DEFAULT NULL,
  `imagem_url` varchar(500) DEFAULT NULL,
  `data_publicacao` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- -----------------------------------------------------
-- 2. TABELAS DEPENDENTES (Com chaves estrangeiras)
-- -----------------------------------------------------

DROP TABLE IF EXISTS `cursos`;
CREATE TABLE `cursos` (
  `id` char(36) NOT NULL,
  `instituicao_id` char(36) NOT NULL,
  `nome` varchar(255) NOT NULL,
  `area_estudo` varchar(100) DEFAULT NULL,
  `modalidade` varchar(50) DEFAULT NULL,
  `nivel_ensino` varchar(50) DEFAULT NULL,
  `descricao_resumida` text,
  `descricao_completa` text,
  `nota_corte` decimal(5,2) DEFAULT NULL,
  `duracao_semestres` int DEFAULT NULL,
  `mensalidade` decimal(10,2) DEFAULT NULL,
  `guia_estudos` text,
  `ativo` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `instituicao_id` (`instituicao_id`),
  CONSTRAINT `fk_curso_instituicao` FOREIGN KEY (`instituicao_id`) REFERENCES `instituicoes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `curso_forma_ingresso`;
CREATE TABLE `curso_forma_ingresso` (
  `curso_id` char(36) NOT NULL,
  `forma_ingresso_id` char(36) NOT NULL,
  PRIMARY KEY (`curso_id`,`forma_ingresso_id`),
  KEY `fk_cfi_forma` (`forma_ingresso_id`),
  CONSTRAINT `fk_cfi_curso` FOREIGN KEY (`curso_id`) REFERENCES `cursos` (`id`),
  CONSTRAINT `fk_cfi_forma` FOREIGN KEY (`forma_ingresso_id`) REFERENCES `formas_ingresso` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `favoritos`;
CREATE TABLE `favoritos` (
  `id` char(36) NOT NULL,
  `usuario_id` char(36) NOT NULL,
  `curso_id` char(36) NOT NULL,
  `data_favorito` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `usuario_id` (`usuario_id`,`curso_id`),
  CONSTRAINT `fk_fav_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_fav_curso` FOREIGN KEY (`curso_id`) REFERENCES `cursos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `anuncios`;
CREATE TABLE `anuncios` (
  `id` char(36) NOT NULL,
  `parceiro_id` char(36) NOT NULL,
  `titulo` varchar(255) DEFAULT NULL,
  `imagem_url` varchar(500) DEFAULT NULL,
  `link_destino` varchar(500) DEFAULT NULL,
  `data_inicio` datetime DEFAULT NULL,
  `data_fim` datetime DEFAULT NULL,
  `ativo` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_anuncio_parceiro` FOREIGN KEY (`parceiro_id`) REFERENCES `parceiros` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `pagamentos`;
CREATE TABLE `pagamentos` (
  `id` char(36) NOT NULL,
  `parceiro_id` char(36) NOT NULL,
  `valor` decimal(10,2) NOT NULL,
  `forma_pagamento` varchar(30) NOT NULL,
  `status_pagamento` varchar(30) DEFAULT NULL,
  `data_pagamento` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_pag_parceiro` FOREIGN KEY (`parceiro_id`) REFERENCES `parceiros` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `perguntas`;
CREATE TABLE `perguntas` (
  `id` char(36) NOT NULL,
  `questionario_id` char(36) NOT NULL,
  `enunciado` text NOT NULL,
  `ordem_exibicao` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_perg_quest` FOREIGN KEY (`questionario_id`) REFERENCES `questionarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `opcoes_pergunta`;
CREATE TABLE `opcoes_pergunta` (
  `id` char(36) NOT NULL,
  `pergunta_id` char(36) NOT NULL,
  `texto` varchar(255) NOT NULL,
  `area_relacionada` varchar(100) DEFAULT NULL,
  `pontuacao` int DEFAULT '0',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_opc_perg` FOREIGN KEY (`pergunta_id`) REFERENCES `perguntas` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `respostas_usuario`;
CREATE TABLE `respostas_usuario` (
  `id` char(36) NOT NULL,
  `usuario_id` char(36) NOT NULL,
  `pergunta_id` char(36) NOT NULL,
  `opcao_id` char(36) NOT NULL,
  `data_resposta` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_resp_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_resp_perg` FOREIGN KEY (`pergunta_id`) REFERENCES `perguntas` (`id`),
  CONSTRAINT `fk_resp_opcao` FOREIGN KEY (`opcao_id`) REFERENCES `opcoes_pergunta` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `resultados_vocacionais`;
CREATE TABLE `resultados_vocacionais` (
  `id` char(36) NOT NULL,
  `usuario_id` char(36) NOT NULL,
  `area_sugerida` varchar(100) DEFAULT NULL,
  `descricao` text,
  `data_resultado` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_resvoc_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `notificacoes`;
CREATE TABLE `notificacoes` (
  `id` char(36) NOT NULL,
  `usuario_id` char(36) NOT NULL,
  `titulo` varchar(255) NOT NULL,
  `mensagem` text NOT NULL,
  `lida` tinyint(1) DEFAULT '0',
  `data_envio` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_notif_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `usuarios_termos`;
CREATE TABLE `usuarios_termos` (
  `id` char(36) NOT NULL,
  `usuario_id` char(36) NOT NULL,
  `termo_id` char(36) NOT NULL,
  `data_aceite` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `usuario_id` (`usuario_id`),
  KEY `termo_id` (`termo_id`),
  CONSTRAINT `fk_usrterm_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_usrterm_termo` FOREIGN KEY (`termo_id`) REFERENCES `termos_uso` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Liga a checagem de chaves de volta (Boa prática de segurança)
SET FOREIGN_KEY_CHECKS=1;

-- -----------------------------------------------------
-- 3. ALTERAÇÕES NO BD
-- -----------------------------------------------------
ALTER TABLE `cursos` ADD COLUMN imagem_principal_url varchar(500) DEFAULT NULL AFTER `nome`;