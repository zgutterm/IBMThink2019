--
-- Table structure for table `Vendor`
--

DROP TABLE IF EXISTS `Vendor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Vendor` (
  `id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Vendor`
--

LOCK TABLES `Vendor` WRITE;
INSERT INTO `Vendor` VALUES (1,'Bookmart, Inc.'),(2,'Lots O\'Books');
UNLOCK TABLES;

--
-- Table structure for table `Publisher`
--

DROP TABLE IF EXISTS `Publisher`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Publisher` (
  `id` int(11) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Publisher`
--

LOCK TABLES `Publisher` WRITE;
/*!40000 ALTER TABLE `Publisher` DISABLE KEYS */;
INSERT INTO `Publisher` VALUES (1,'orders@kiko.com','Kiko Publishing, Inc.'),(2,'admin@booksinc.com','Books Incorporated');
/*!40000 ALTER TABLE `Publisher` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `CatalogItem`
--

DROP TABLE IF EXISTS `CatalogItem`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CatalogItem` (
  `id` int(11) NOT NULL,
  `author` varchar(255) DEFAULT NULL,
  `category` varchar(255) DEFAULT NULL,
  `description` varchar(2000) DEFAULT NULL,
  `imagePath` varchar(255) DEFAULT NULL,
  `newItem` tinyint(1) DEFAULT NULL,
  `price` decimal(19,2) DEFAULT NULL,
  `sku` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `pub_id` int(11) DEFAULT NULL,
  `vendor_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_lx00vybz2233mnxykuzpr556` (`pub_id`),
  KEY `FK_lx00vybz2233mnxykuzpr557` (`vendor_id`),
  CONSTRAINT `FK_lx00vybz2233mnxykuzpr556` FOREIGN KEY (`pub_id`) REFERENCES `Publisher` (`id`),
  CONSTRAINT `FK_lx00vybz2233mnxykuzpr557` FOREIGN KEY (`vendor_id`) REFERENCES `Vendor` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `CatalogItem`
--

LOCK TABLES `CatalogItem` WRITE;
/*!40000 ALTER TABLE `CatalogItem` DISABLE KEYS */;
INSERT INTO `CatalogItem` VALUES (1,'Lt. Howard Payson','military','description 1','/images/books/BoyScouts.jpg',1,45.00,'ABC123','The Boy Scouts at the Panama-Pacific Exposition',1,1),(2,'Unknown','children','description 2','/images/books/3LittleKittens.jpg',0,15.00,'DEF456','The 3 Little Kittens',2,1),(3,'Beatrix Potter','children','description 3','/images/books/JemimaPuddleDuck.jpg',1,25.00,'GHI789','The Tale of Jemima Puddle Duck',1,null),(4,'Waldemar Bonsels','children','description 4','/images/books/AdventuresOfMayaTheBee.jpg',0,23.00,'123132','The Adventures of Maya the Bee',2,null),(5,'Sunzi','military','description 5','/images/books/art-of-war-sunzi-lionel-giles.jpg',0,20.00,'45678','The Art of War',1,null),(6,'Rafael Sabatini','children','description 6','/images/books/CaptainBlood.jpg',0,22.95,'45679','Captain Blood',2,null),(7,'Robert Brent','crafts','description 7','/images/books/ChemistryExperiments.png',0,32.95,'45680','The Golden Book of Chemistry Experiments',1,null),(8,'Eleanor M. Ingram','children','description 8','/images/books/coverthingfromthelake.jpg',0,25.95,'45681','The Thing from the Lake',1,null),(9,'Flora Klickmann','crafts','description 9','/images/books/CraftOfCrochet.jpg',0,35.95,'45682','The Craft of Crochet Hook',2,null),(10,'Charles Robert Dumas','children','description 10','/images/books/Design-Book-cover-Juvenile-Contes-Mauves.jpg',0,45.95,'45683','Contes Mauves de ma Mere-Grand',2,null),(11,'Joseph Jacobs','children','description 11','/images/books/EnglishFairyTales.jpg',0,25.95,'45684','English Fairy Tales',1,null),(12,'Flora Klickmann','crafts','description 12','/images/books/FancyStitchery.jpg',0,45.95,'45685','The Home Art Book of Fancy Stitchery',2,null),(13,'Lela Nargi','crafts','description 13','/images/books/FarmersWifeCanning.jpg',0,20.95,'45686','The Farmer\'s Wife Canning and Preserving Cookbook',1,null),(14,'Flora Kickmann','crafts','description 14','/images/books/HomeArtCrochet.jpg',0,15.95,'45687','The Home Art of Crochet Book',2,null),(15,'Three Initiates','crafts','description 15','/images/books/LeKybalion.jpg',0,30.95,'45688','Le Kybalion',2,null),(16,'Jane Eayre Fryer','children','description 16','/images/books/MaryFrancesGarden.jpg',0,40.95,'45689','The Mary Frances Garden Book',1,null),(17,'Jane Eayre Fryer','children','description 17','/images/books/MaryFrancesHousekeeper.jpg',0,55.95,'45690','The Mary Frances Housekeeper',2,null),(18,'Religious Tract Society','children','description 18','/images/books/NewAlphabet.jpg',0,22.95,'45691','My New Alphabet Book',2,null),(19,'Beatrix Potter','children','description 19','/images/books/PeterRabbit.png',0,14.95,'45691','The Tale of Peter Rabbit',2,null),(20,'Unknown','comics','expensive sucker','/images/books/CameraComics.jpg',0,195.00,'45677','Camera Comics',2,null),(21,'Unknown','comics','expensive sucker','/images/books/PoliceCases.jpg',0,200.00,'45692','Authentic Police Cases',2,null),(22,'G.Griffin Lewis','crafts','description 20','/images/books/PracticalBookOfOrientalRugs.jpg',0,205.00,'45693','The Practical Book of Oriental Rugs',1,null),(23,'Priscilla Publishing Co.','crafts','description 21','/images/books/PriscillaCrochetBook.jpg',0,40.00,'45694','Priscilla Juniors\' Crochet Book',1,null),(24,'Tony Laidig','crafts','description 22','/images/books/PublicDomainCodeBook.jpg',1,25.00,'45695','The Public Domain Code Book',2,null),(25,'Thornton W. Burgess','children','description 23','/images/books/ReddyFox.jpg',1,10.95,'45696','The Adventures of Reddy Fox',1,null),(26,'Wallace Wattles','crafts','description 24','/images/books/ScienceOfGettingRich.jpg',1,5.95,'45697','The Science of Getting Rich',2,null),(27,'Arthur Conan Doyle','children','description 25','/images/books/SherlockHolmes.jpg',1,20.95,'45698','The Casebook of Sherlock Holmes',2,null),(28,'Laura Lee Hope','children','description 26','/images/books/StuffedElephant.jpg',1,10.95,'45698','The Story of a Stuffed Elephant',2,null),(29,'DC Comics','comics','description 27','/images/books/SuperGirl.jpg',1,4.95,'45699','Supergirl',2,null),(30,'Beatrix Potter','children','description 28','/images/books/TaleOfPiglingBland.jpg',1,14.95,'45600','The Tale of Pigling Bland',1,null),(31,'Grace McCleen','crafts','description 29','/images/books/The-Offering-lg.jpg',1,12.95,'45601','The Offering',1,null),(32,'Brian Fagan','crafts','description 30','/images/books/TimeDetectives.jpg',1,22.95,'45602','Time Detectives',2,null),(33,'Starr Flagg','comics','description 31','/images/books/UndercoverGirl.jpg',0,220.95,'45603','Undercover Girl',1,null),(34,'Andrew Lang','children','description 32','/images/books/YellowFairy.jpg',1,22.95,'45604','The Yellow Fairy Book',2,null);
UPDATE `CatalogItem` SET `vendor_id`=`pub_id`;
/*!40000 ALTER TABLE `CatalogItem` ENABLE KEYS */;
UNLOCK TABLES;
