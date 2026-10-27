-- MariaDB dump 10.19  Distrib 10.4.32-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: simplenote
-- ------------------------------------------------------
-- Server version	10.4.32-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `simplenote`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `simplenote` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;

USE `simplenote`;

--
-- Table structure for table `app_settings`
--

DROP TABLE IF EXISTS `app_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_settings` (
  `config_key` varchar(64) NOT NULL,
  `config_value` varchar(128) NOT NULL,
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `app_settings`
--

LOCK TABLES `app_settings` WRITE;
/*!40000 ALTER TABLE `app_settings` DISABLE KEYS */;
INSERT INTO `app_settings` VALUES ('default_color','4');
/*!40000 ALTER TABLE `app_settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `note_colors`
--

DROP TABLE IF EXISTS `note_colors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `note_colors` (
  `id` int(11) NOT NULL,
  `name` varchar(20) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `note_colors`
--

LOCK TABLES `note_colors` WRITE;
/*!40000 ALTER TABLE `note_colors` DISABLE KEYS */;
INSERT INTO `note_colors` VALUES (1,'Padrão'),(2,''),(3,'Dicas'),(4,'Coisas que eu gosto'),(5,''),(6,'');
/*!40000 ALTER TABLE `note_colors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notes`
--

DROP TABLE IF EXISTS `notes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(150) NOT NULL,
  `content` text DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `updated_at` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `custom_title` varchar(120) DEFAULT NULL,
  `color_id` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notes`
--

LOCK TABLES `notes` WRITE;
/*!40000 ALTER TABLE `notes` DISABLE KEYS */;
INSERT INTO `notes` VALUES (1,'asdfasdfasldkjflasdf','asdfasdfasldkjflasdf','2026-10-27 01:44:34','2026-10-27 02:58:29',NULL,1),(2,'asdfasdfasdfas\'','asdfasdfasdfas\'','2026-10-27 01:44:38','2026-10-27 01:44:38',NULL,1),(3,'asfd','asfd\n','2026-10-27 02:45:09','2026-10-27 02:45:09',NULL,1),(4,'sadf','sadf','2026-10-27 02:45:09','2026-10-27 02:45:09',NULL,1),(5,'laksdjfj','laksdjfj','2026-10-27 02:45:09','2026-10-27 02:45:09',NULL,1),(6,'sadf','sadf','2026-10-27 02:45:22','2026-10-27 02:45:22',NULL,1),(7,'asdf\'','asdf\'','2026-10-27 02:58:12','2026-10-27 02:58:12',NULL,1),(8,'adfas','adfas','2026-10-27 02:58:12','2026-10-27 02:58:12',NULL,1),(9,'asdfasdf','asdfasdf','2026-10-27 02:58:12','2026-10-27 02:58:12',NULL,1),(10,'asdf','asdf','2026-10-27 03:08:30','2026-10-27 03:08:30','asdfasdfasdfasdfkjashdklfhaskdfakhsdgfhagsjdhfgakjshdgfakljsdhflashdljfhaskjdhfakljshdflkjahsdklfjahsldkjfahlskdjhfalskd',1),(11,'asdf','asdf','2026-10-27 03:59:23','2026-10-27 03:59:23',NULL,3),(12,'Inventário do explorador','Inventário do explorador\n\n[x] Mapa estelar\n\n[ ] Cristal de energia\n\n[x] Nave abastecida\n\n[ ] Salvar a galáxia','2026-10-27 04:41:12','2026-10-27 04:41:12',NULL,2),(13,'Projeto: Máquina do Tempo','Projeto: Máquina do Tempo\n\nDestino: 2049.\n\nObjetivo: descobrir se finalmente corrigimos todos os bugs.\n\nAviso: não alterar a linha temporal.','2026-10-27 04:41:30','2026-10-27 04:41:30',NULL,1),(14,'Terminal do programador','Terminal do programador\n\nsudo apt install criatividade\n\nDependências:\nJava 25\nCafé\nPersistência\nPaciência','2026-10-27 04:41:48','2026-10-27 04:41:48',NULL,3),(15,'Missão Jedi: JavaFX','Missão Jedi: JavaFX\n\nDominar a Força.\n\nEliminar os bugs.\n\nNão ceder ao lado sombrio do NullPointerException.','2026-10-27 04:42:03','2026-10-27 04:42:03',NULL,4),(16,'Protocolo 42','Protocolo 42\n\nA resposta para tudo já existe.\n\nAgora só precisamos descobrir qual era a pergunta.\n\n\n\n','2026-10-27 04:42:39','2026-10-27 04:42:39',NULL,5),(17,'Diário de bordo — 27.10.2026','Diário de bordo — 27.10.2026\n\nMissão: transformar café em código.\n\nStatus: compilando sonhos.\n\nPróxima parada: o multiverso.\n','2026-10-27 04:42:39','2026-10-27 04:44:22',NULL,1);
/*!40000 ALTER TABLE `notes` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-27  5:05:15
