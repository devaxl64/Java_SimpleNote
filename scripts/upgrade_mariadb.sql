-- Atualização não destrutiva: NÃO usar o SQL antigo do C# (tem DROP DATABASE).
-- Execute uma vez: /opt/lampp/bin/mysql -u root simplenote < scripts/upgrade_mariadb.sql
ALTER TABLE notes ADD COLUMN IF NOT EXISTS custom_title VARCHAR(120) NULL;
ALTER TABLE notes ADD COLUMN IF NOT EXISTS color_id INT NOT NULL DEFAULT 1;
CREATE TABLE IF NOT EXISTS note_colors (
  id INT PRIMARY KEY,
  name VARCHAR(20) NOT NULL DEFAULT ''
);
CREATE TABLE IF NOT EXISTS app_settings (
  config_key VARCHAR(64) PRIMARY KEY,
  config_value VARCHAR(128) NOT NULL
);
INSERT IGNORE INTO note_colors (id, name) VALUES (1,''),(2,''),(3,''),(4,''),(5,''),(6,'');
INSERT IGNORE INTO app_settings(config_key,config_value) VALUES ('default_color','1');
