package br.com.soapboxrace.func;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.jetty.util.security.Credential.MD5;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Event {

	private Random rand = new Random();
	private Functions fx = new Functions();

	public static int RaceReward = 0;
	public static int CardPackDrop = 1;
	public static int THReward = 2;

	private int exp = 0, cash = 0, rank = 0;
	private String Rewards = null;
	private String title = null, hash = null, icon = null, type = null;
	private boolean leveledUp = false;

	private String[] mapVisual = new String[] { "productsInCategory_NFSW_NA_EP_VISUALPARTS_LICENSEPLATES.xml",
			"productsInCategory_NFSW_NA_EP_VISUALPARTS_NEONS.xml", "productsInCategory_NFSW_NA_EP_VISUALPARTS_WHEELS.xml",
			"productsInCategory_STORE_VANITY_BODYKIT.xml", "productsInCategory_STORE_VANITY_HOOD.xml", "productsInCategory_STORE_VANITY_LICENSE_PLATE.xml",
			"productsInCategory_STORE_VANITY_LOWERING_KIT.xml", "productsInCategory_STORE_VANITY_NEON.xml", "productsInCategory_STORE_VANITY_SPOILER.xml",
			"productsInCategory_STORE_VANITY_WHEEL.xml", "productsInCategory_STORE_VANITY_WINDOW.xml" };
	private int[][] cashDrop = new int[][] { new int[] { 31, 69, 89, 1000, 2578, 5000, 10000, 20000, 50000, 100000, 1000000 },
			new int[] { 31, 69, 89, 1000, 50000 } };

	public void AddInventoryObject(Node objectData) {
		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document inventory = docBuilder.parse("www/soapbox/Engine.svc/personas/" + Functions.personaId + "/objects.xml");
			String md5 = MD5.digest(hash).replace("MD5:", "");
			if (new String(Files.readAllBytes(Paths.get("www/soapbox/Engine.svc/personas/" + Functions.personaId + "/objects.xml")), StandardCharsets.UTF_8)
					.contains("<EntitlementTag>" + md5 + "</EntitlementTag>")) {
				int index = fx
						.CountInstances(new String(Files.readAllBytes(Paths.get("www/soapbox/Engine.svc/personas/" + Functions.personaId + "/objects.xml")),
								StandardCharsets.UTF_8), "<InventoryItemTrans>", "<EntitlementTag>" + md5 + "</EntitlementTag>")
						- 1;
				String newAmount = String.valueOf(Integer.parseInt(inventory.getElementsByTagName("RemainingUseCount").item(index).getTextContent()) + 1);
				inventory.getElementsByTagName("RemainingUseCount").item(index).setTextContent(newAmount);
			} else {
				switch (((Element) objectData.getParentNode()).getElementsByTagName("ProductType").item(0).getTextContent()) {
				case "PERFORMANCEPART": {
					int newParts = Integer.parseInt(inventory.getElementsByTagName("PerformancePartsUsedSlotCount").item(0).getTextContent()) + 1;
					if (newParts > Integer.parseInt(inventory.getElementsByTagName("PerformancePartsCapacity").item(0).getTextContent())) {
						Functions.log("|| !!! -> Maximum amount of performance parts was reached. NO NEW PARTS WILL BE ADDED!");
						return;
					} else {
						inventory.getElementsByTagName("PerformancePartsUsedSlotCount").item(0).setTextContent(String.valueOf(newParts));
					}
					break;
				}
				case "SKILLMODPART": {
					int newParts = Integer.parseInt(inventory.getElementsByTagName("SkillModPartsUsedSlotCount").item(0).getTextContent()) + 1;
					if (newParts > Integer.parseInt(inventory.getElementsByTagName("SkillModPartsCapacity").item(0).getTextContent())) {
						Functions.log("|| !!! -> Maximum amount of skill mods was reached. NO NEW PARTS WILL BE ADDED!");
						return;
					} else {
						inventory.getElementsByTagName("SkillModPartsUsedSlotCount").item(0).setTextContent(String.valueOf(newParts));
					}
					break;
				}
				case "VISUALPART": {
					int newParts = Integer.parseInt(inventory.getElementsByTagName("VisualPartsUsedSlotCount").item(0).getTextContent()) + 1;
					if (newParts > Integer.parseInt(inventory.getElementsByTagName("VisualPartsCapacity").item(0).getTextContent())) {
						Functions.log("|| !!! -> Maximum amount of visual parts was reached. NO NEW PARTS WILL BE ADDED!");
						return;
					} else {
						inventory.getElementsByTagName("VisualPartsUsedSlotCount").item(0).setTextContent(String.valueOf(newParts));
					}
					break;
				}
				}

				Node parent = inventory.getElementsByTagName("InventoryItems").item(0);
				String child = "<InventoryItemTrans><EntitlementTag>" + md5 + "</EntitlementTag><ExpirationDate i:nil=\"true\"/>" + "<Hash>" + hash + "</Hash>"
						+ "<InventoryId>"
						+ String.valueOf(Integer.parseInt(inventory.getElementsByTagName("InventoryId")
								.item(inventory.getElementsByTagName("InventoryId").getLength() - 1).getTextContent()) + 1)
						+ "</InventoryId><ProductId>DO NOT USE ME</ProductId><RemainingUseCount>1</RemainingUseCount>" + "<ResellPrice>"
						+ String.valueOf((int) Math
								.round(Double.parseDouble(((Element) objectData.getParentNode()).getElementsByTagName("Price").item(0).getTextContent()) / 2.0))
						+ ".00000</ResellPrice><Status>ACTIVE</Status>" + "<StringHash>0x" + Long.toHexString(Long.parseLong(hash)) + "</StringHash>"
						+ "<VirtualItemType>" + type + "</VirtualItemType></InventoryItemTrans>";
				Node fragmentNode = docBuilder.parse(new InputSource(new StringReader(child))).getDocumentElement();
				fragmentNode = inventory.importNode(fragmentNode, true);
				parent.appendChild(fragmentNode);
			}
			fx.WriteXML(inventory, "www/soapbox/Engine.svc/personas/" + Functions.personaId + "/objects.xml");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void AddCar(String basketId) throws ParserConfigurationException, SAXException, IOException {
		fx.FixCarslots();

		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = docBuilder.parse(new File("www/soapbox/Engine.svc/personas/" + Functions.personaId + "/carslots.xml"));
		int lastIdIndex = doc.getElementsByTagName("Id").getLength() - 1;
		String carId = String.valueOf(Integer.parseInt(doc.getElementsByTagName("Id").item(lastIdIndex).getTextContent()) + 1);

		Document doc2 = docBuilder.parse(new File("www/basket/" + basketId + ".xml"));
		doc2.getElementsByTagName("Id").item(1).setTextContent(carId);

		Node carTrans = doc.importNode(doc2.getFirstChild(), true);
		doc.getElementsByTagName("CarsOwnedByPersona").item(0).appendChild(carTrans);
		int _carId = Integer.parseInt(carId) - 1;
		doc.getElementsByTagName("DefaultOwnedCarIndex").item(0).setTextContent(String.valueOf(_carId));
		fx.WriteXML(doc, "www/soapbox/Engine.svc/personas/" + Functions.personaId + "/carslots.xml");
		fx.WriteTempCar(new String(Files.readAllBytes(Paths.get("www/basket/" + basketId + ".xml")), StandardCharsets.UTF_8));
		Functions.log("|| -> New car has been added to the carslots of persona " + Functions.personaId + ".");
		Functions.log("|| -> Car Index has been changed to match the new car's ID.");
	}

	private void randCatalog() {
		try {
			int catId = 0;
			Document catalog = null;
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

			int dropNum = rank > 3 ? 0 : Functions.rankDrop[rank][rand.nextInt(10)];
			if (dropNum == -1) {
				int tipToe = 0, amount = 0;
				double d = Math.random() * 100;
				if ((d -= 10) < 0)
					tipToe = 1;
				d = Math.random() * 1000;
				if (tipToe == 0) {
					if ((d -= 1) < 0)
						amount = cashDrop[tipToe][10];
					else if ((d -= 5) < 0)
						amount = cashDrop[tipToe][9];
					else if ((d -= 100) < 0)
						amount = cashDrop[tipToe][8];
					else if ((d -= 250) < 0)
						amount = cashDrop[tipToe][7];
					else if ((d -= 500) < 0)
						amount = cashDrop[tipToe][6];
					else if ((d -= 750) < 0)
						amount = cashDrop[tipToe][5];
					else
						amount = cashDrop[tipToe][rand.nextInt(5)];
				} else {
					if ((d -= 10) < 0)
						amount = cashDrop[tipToe][4];
					else if ((d -= 50) < 0)
						amount = cashDrop[tipToe][3];
					else
						amount = cashDrop[tipToe][rand.nextInt(3)];
				}

				Economy economy = new Economy(String.valueOf(amount), String.valueOf(tipToe), true);
				economy.transCurrency(false);

				title = String.valueOf(amount) + (tipToe == 0 ? " IGC" : " BOOST");
				hash = "";
				icon = "128_cash";
				type = "CASH";
				return;
			} else if (dropNum == 0) {
				catalog = docBuilder.parse("www/soapbox/Engine.svc/catalog/productsInCategory_STORE_POWERUPS.xml");
			} else if (dropNum == 1) {
				catalog = docBuilder.parse("www/soapbox/Engine.svc/catalog/productsInCategory_NFSW_NA_EP_SKILLMODPARTS.xml");
			} else if (dropNum == 2) {
				catalog = docBuilder.parse("www/soapbox/Engine.svc/catalog/" + mapVisual[rand.nextInt(11)]);
			} else if (dropNum == 3) {
				catalog = docBuilder.parse("www/soapbox/Engine.svc/catalog/productsInCategory_NFSW_NA_EP_PERFORMANCEPARTS.xml");
				catId = rand.nextInt(catalog.getElementsByTagName("ProductTrans").getLength());
				title = (catalog.getElementsByTagName("Description").item(catId).getTextContent() + catalog.getElementsByTagName("ProductTitle").item(catId)
						.getTextContent().replace(catalog.getElementsByTagName("ProductTitle").item(catId).getTextContent().split(" ")[0], ""));
				hash = catalog.getElementsByTagName("Hash").item(catId).getTextContent();
				icon = catalog.getElementsByTagName("Icon").item(catId).getTextContent();
				type = catalog.getElementsByTagName("ProductType").item(catId).getTextContent();
				AddInventoryObject(catalog.getElementsByTagName("ProductTrans").item(catId));
				return;
			}
			catId = rand.nextInt(catalog.getElementsByTagName("ProductTrans").getLength());
			title = catalog.getElementsByTagName("ProductTitle").item(catId).getTextContent();
			hash = catalog.getElementsByTagName("Hash").item(catId).getTextContent();
			icon = catalog.getElementsByTagName("Icon").item(catId).getTextContent();
			type = catalog.getElementsByTagName("ProductType").item(catId).getTextContent();
			if (dropNum == 0) {
				int pAm = rand.nextInt(51);
				processPowerup(hash, pAm);
				title = String.valueOf(pAm) + "x " + title.replace(" x15", "");
			} else
				AddInventoryObject(catalog.getElementsByTagName("ProductTrans").item(catId));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void SetPrize(int RewardType) throws DOMException, SAXException, IOException, ParserConfigurationException {
		String LuckyDrawItem = GetPrize();
		if (RewardType == RaceReward) {
			Functions.answerData = "<Accolades xmlns=\"http://schemas.datacontract.org/2004/07/Victory.DataLayer.Serialization.Event\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\"><FinalRewards><Rep>"
					+ String.valueOf(exp)
					+ "</Rep><Tokens>" + String
							.valueOf(
									cash)
					+ "</Tokens></FinalRewards><HasLeveledUp>"
					+ String.valueOf(
							leveledUp)
					+ "</HasLeveledUp>"
					+ (type == "BUSTED" ? "<LuckyDrawInfo/>"
							: ("<LuckyDrawInfo><Boxes i:nil=\"true\"/><CardDeck>"
									+ (type == "PRESETCAR" ? "LD_CARD_SPECIAL_GOLD"
											: (rank == 1 ? "LD_CARD_GOLD" : (rank == 2 ? "LD_CARD_SILVER" : (rank == 3 ? "LD_CARD_BRONZE" : "LD_CARD_BLUE"))))
									+ "</CardDeck><CurrentStreak>0</CurrentStreak><IsStreakBroken>false</IsStreakBroken><Items>" + LuckyDrawItem
									+ "</Items><NumBoxAnimations>100</NumBoxAnimations><NumCards>5</NumCards></LuckyDrawInfo>"))
					+ "<OriginalRewards><Rep>0</Rep><Tokens>0</Tokens></OriginalRewards><RewardInfo>" + Rewards + "</RewardInfo></Accolades>";
		} else if (RewardType == THReward) {
			Functions.answerData = "<Accolades xmlns=\"http://schemas.datacontract.org/2004/07/Victory.DataLayer.Serialization.Event\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\"><FinalRewards><Rep>"
					+ String.valueOf(exp) + "</Rep><Tokens>" + String.valueOf(cash) + "</Tokens></FinalRewards><HasLeveledUp>" + String.valueOf(leveledUp)
					+ "</HasLeveledUp><LuckyDrawInfo><Boxes><LuckyBox><CardDeck>LD_CARD_SILVER</CardDeck></LuckyBox><LuckyBox><CardDeck>LD_CARD_SILVER</CardDeck></LuckyBox><LuckyBox><CardDeck>LD_CARD_SILVER</CardDeck></LuckyBox><LuckyBox><CardDeck>LD_CARD_SILVER</CardDeck></LuckyBox><LuckyBox><CardDeck>LD_CARD_SILVER</CardDeck></LuckyBox></Boxes><CurrentStreak>"
					+ String.valueOf(fx.GetTHStreak()) + "</CurrentStreak><IsStreakBroken>" + fx.GetIsTHStreakBroken() + "</IsStreakBroken><Items>"
					+ LuckyDrawItem
					+ "</Items><NumBoxAnimations>100</NumBoxAnimations></LuckyDrawInfo><OriginalRewards><Rep>0</Rep><Tokens>0</Tokens></OriginalRewards><RewardInfo>"
					+ Rewards + "</RewardInfo></Accolades>";
			fx.WriteText("www/soapbox/Engine.svc/serverSettings/THDate", LocalDate.now().toString());
		}
	}

	private String GetPrize() {
		return "<LuckyDrawItem><Description>" + title + "</Description><Hash>" + hash + "</Hash><Icon>" + icon
				+ "</Icon><RemainingUseCount>1</RemainingUseCount><ResellPrice>0</ResellPrice><VirtualItem>LARGE_DROP</VirtualItem><VirtualItemType>" + type
				+ "</VirtualItemType><WasSold>false</WasSold></LuckyDrawItem>";
	}

	private void saveAccolades() throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = docBuilder.parse("www/soapbox/Engine.svc/User/GetPermanentSession.xml");
		int sessionId = Integer.parseInt(doc.getElementsByTagName("defaultPersonaIdx").item(0).getTextContent());
		Document doc2 = docBuilder.parse("www/soapbox/Engine.svc/DriverPersona/GetPersonaInfo_" + Functions.personaId + ".xml");
		Document doc3 = docBuilder.parse("www/soapbox/Engine.svc/DriverPersona/GetPersonaBaseFromList_" + Functions.personaId + ".xml");

		int newCash = Integer.parseInt(doc2.getElementsByTagName("Cash").item(0).getTextContent());
		newCash = newCash + cash;
		if (newCash > 999999999) {
			newCash = 999999999;
		}
		doc.getElementsByTagName("Cash").item(sessionId).setTextContent(String.valueOf(newCash));
		doc2.getElementsByTagName("Cash").item(0).setTextContent(String.valueOf(newCash));

		int curLvl = Integer.parseInt(doc2.getElementsByTagName("Level").item(0).getTextContent()) - 1;

		if (curLvl < 69) {
			int newRep = Integer.parseInt(doc2.getElementsByTagName("Rep").item(0).getTextContent());
			int curLvlRep = Integer.parseInt(doc2.getElementsByTagName("RepAtCurrentLevel").item(0).getTextContent());
			newRep = newRep + exp;
			curLvlRep = curLvlRep + exp;

			Document expMap = docBuilder.parse("www/soapbox/Engine.svc/DriverPersona/GetExpLevelPointsMap.xml");
			int lvlExp = (Integer.parseInt(expMap.getElementsByTagName("int").item(curLvl).getTextContent())
					- (curLvl == 0 ? 0 : (Integer.parseInt(expMap.getElementsByTagName("int").item(curLvl - 1).getTextContent()))) - 100);
			leveledUp = (lvlExp - curLvlRep <= 0);

			if (leveledUp) {
				boolean _leveledUp = true;
				while (_leveledUp) {
					curLvlRep = curLvlRep - lvlExp;
					curLvl = curLvl + 1;

					if (curLvl % 10 == 9) {
						Document catalog = docBuilder.parse("www/soapbox/Engine.svc/catalog/productsInCategory_NFSW_NA_EP_PRESET_RIDES_ALL_Category.xml");
						int catId = rand.nextInt(catalog.getElementsByTagName("ProductTrans").getLength());
						title = catalog.getElementsByTagName("ProductTitle").item(catId).getTextContent();
						hash = catalog.getElementsByTagName("Hash").item(catId).getTextContent();
						icon = catalog.getElementsByTagName("Icon").item(catId).getTextContent();
						type = "PRESETCAR";
						AddCar(catalog.getElementsByTagName("ProductId").item(catId).getTextContent());
					}
					if (curLvl >= 69)
						break;
					lvlExp = (Integer.parseInt(expMap.getElementsByTagName("int").item(curLvl).getTextContent())
							- Integer.parseInt(expMap.getElementsByTagName("int").item(curLvl - 1).getTextContent())) - 100;
					_leveledUp = (lvlExp - curLvlRep <= 0);
				}
			}

			double percent = 0.0;
			if (curLvl >= 69) {
				curLvlRep = 0;
				curLvl = 69;
				percent = 0.0;
				newRep = 15882625;
			} else {
				percent = Double.valueOf((double) curLvlRep / (double) lvlExp) * 100;
			}

			doc.getElementsByTagName("Rep").item(sessionId).setTextContent(String.valueOf(newRep));
			doc2.getElementsByTagName("Rep").item(0).setTextContent(String.valueOf(newRep));
			doc.getElementsByTagName("Level").item(sessionId).setTextContent(String.valueOf(curLvl + 1));
			doc2.getElementsByTagName("Level").item(0).setTextContent(String.valueOf(curLvl + 1));
			doc3.getElementsByTagName("Level").item(0).setTextContent(String.valueOf(curLvl + 1));
			doc.getElementsByTagName("RepAtCurrentLevel").item(sessionId).setTextContent(String.valueOf(curLvlRep));
			doc2.getElementsByTagName("RepAtCurrentLevel").item(0).setTextContent(String.valueOf(curLvlRep));
			doc.getElementsByTagName("PercentToLevel").item(sessionId).setTextContent(String.valueOf(percent));
			doc2.getElementsByTagName("PercentToLevel").item(0).setTextContent(String.valueOf(percent));
		}

		fx.WriteXML(doc, "www/soapbox/Engine.svc/User/GetPermanentSession.xml");
		fx.WriteXML(doc2, "www/soapbox/Engine.svc/DriverPersona/GetPersonaInfo_" + Functions.personaId + ".xml");
		fx.WriteXML(doc3, "www/soapbox/Engine.svc/DriverPersona/GetPersonaBaseFromList_" + Functions.personaId + ".xml");
	}

	public void processPowerup(String PowerupHash, int amount) {
		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document inventory = docBuilder.parse("www/soapbox/Engine.svc/personas/" + Functions.personaId + "/objects.xml");
			int index = fx.CountInstances(new String(Files.readAllBytes(Paths.get("www/soapbox/Engine.svc/personas/" + Functions.personaId + "/objects.xml")),
					StandardCharsets.UTF_8), "<InventoryItemTrans>", "<Hash>" + PowerupHash + "</Hash>") - 1;
			String newAmount = String.valueOf(Integer.parseInt(inventory.getElementsByTagName("RemainingUseCount").item(index).getTextContent()) + amount);
			inventory.getElementsByTagName("RemainingUseCount").item(index).setTextContent(newAmount);
			fx.WriteXML(inventory, "www/soapbox/Engine.svc/personas/" + Functions.personaId + "/objects.xml");
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	public void ReadArbitration(String arbitrationData) {
		try {
			leveledUp = false;
			Rewards = "";
			int eventType = (arbitrationData.startsWith("<DragArbitrationPacket") ? 0 : (arbitrationData.startsWith("<TeamEscapeArbitrationPacket") ? 2 : 1));

			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = docBuilder.parse(new InputSource(new StringReader(arbitrationData)));
			if (arbitrationData.startsWith("<TeamEscapeArbitrationPacket") && doc.getElementsByTagName("FinishReason").item(0).getTextContent().equals("266")) {
				rank = 5;
				cash = 0;
				exp = 0;

				title = "BUSTED";
				hash = "BUSTED";
				icon = "BUSTED";
				type = "BUSTED";
			} else if (arbitrationData.equals("<TreasureHunt/>")) {
				cash = 1000;
				if (fx.GetLevel() == 70) {
					exp = 0;
				} else {
					exp = 500;
				}
				Rewards = "<RewardPart><RepPart>" + String.valueOf(exp)
						+ "</RepPart><RewardCategory>Rank</RewardCategory><RewardType>None</RewardType><TokenPart>" + String.valueOf(cash)
						+ "</TokenPart></RewardPart>";

				title = "500 BOOST";
				hash = "";
				icon = "128_cash";
				type = "CASH";
				saveAccolades();
				if (type.equals("CASH")) {
					Economy economy = new Economy("500", "1", true);
					economy.transCurrency(false);
				}
				SetPrize(THReward);
				return;
			} else {
				rank = arbitrationData.startsWith("<Pursuit") ? 2 : Integer.parseInt(doc.getElementsByTagName("Rank").item(0).getTextContent());
				int baseReward = Functions.rewards[0];
				cash = (int) Math.round((double) (rank == 1 ? baseReward
						: (rank == 2 ? ((double) baseReward / 2.0) : (rank == 3 ? ((double) baseReward / 4.0) : ((double) baseReward / 20.0))))
						* Functions.multipliers[eventType]);
				if (fx.GetLevel() >= 70) {
					exp = 0;
				} else {
					exp = (int) Math.round(
							(double) (50 * fx.GetLevel()) + ((53 * fx.GetLevel()) * (rank <= 3 ? Math.abs(rank - 5) : 1)) * Functions.multipliers[eventType]);
				}

				Rewards = "<RewardPart><RepPart>" + String.valueOf(exp)
						+ "</RepPart><RewardCategory>Rank</RewardCategory><RewardType>None</RewardType><TokenPart>" + String.valueOf(cash)
						+ "</TokenPart></RewardPart>";
				if (!arbitrationData.startsWith("<Pursuit")) {
					if (doc.getElementsByTagName("PerfectStart").item(0).getTextContent().equals("1")) {
						int[] perfRewards = new int[] { (int) Math.round((double) Functions.rewards[2] * Functions.multipliers[eventType]),
								(int) Math.round((double) Functions.rewards[1] * Functions.multipliers[eventType]) };
						cash = cash + perfRewards[0];
						exp += (exp == 0 ? 0 : perfRewards[1]);
						Rewards = Rewards + "<RewardPart><RepPart>" + (exp == 0 ? "0" : String.valueOf(perfRewards[1]))
								+ "</RepPart><RewardCategory>Bonus</RewardCategory><RewardType>PerfectStart</RewardType><TokenPart>"
								+ String.valueOf(perfRewards[1]) + "</TokenPart></RewardPart>";
					}
					int[] ramRewards = new int[] { (int) Math.round((double) Functions.rewards[4] * Functions.multipliers[eventType]),
							(int) Math.round((double) Functions.rewards[3] * Functions.multipliers[eventType]) };
					if (doc.getElementsByTagName("NumberOfCollisions").item(0).getTextContent().equals("0")) {
						cash = cash + ramRewards[0];
						exp += (exp == 0 ? 0 : ramRewards[1]);
						Rewards = Rewards + "<RewardPart><RepPart>" + (exp == 0 ? "0" : String.valueOf(ramRewards[1]))
								+ "</RepPart><RewardCategory>TeamBonus</RewardCategory><RewardType>TeamStrikeBonus</RewardType><TokenPart>"
								+ String.valueOf(ramRewards[0]) + "</TokenPart></RewardPart>";
					} else if (doc.getElementsByTagName("NumberOfCollisions").item(0).getTextContent().equals("1")) {
						cash = cash + (int) Math.round(100.0 * Functions.multipliers[eventType]);
						exp += (exp == 0 ? 0 : (int) Math.round(100.0 * Functions.multipliers[eventType]));
						Rewards = Rewards + "<RewardPart><RepPart>"
								+ (exp == 0 ? "0" : String.valueOf((int) Math.round(100.0 * Functions.multipliers[eventType])))
								+ "</RepPart><RewardCategory>TeamBonus</RewardCategory><RewardType>TeamStrikeBonus</RewardType><TokenPart>"
								+ String.valueOf((int) Math.round(100.0 * Functions.multipliers[eventType])) + "</TokenPart></RewardPart>";
					}
				}
				randCatalog();
				saveAccolades();
			}
			Functions.answerData = "<PursuitEventResult xmlns=\"http://schemas.datacontract.org/2004/07/Victory.DataLayer.Serialization.Event\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\"><Accolades><FinalRewards><Rep>"
					+ String.valueOf(exp) + "</Rep><Tokens>" + String.valueOf(cash) + "</Tokens></FinalRewards><HasLeveledUp>" + String.valueOf(leveledUp)
					+ "</HasLeveledUp>"
					+ ((cash == 0 && exp == 0) ? "<LuckyDrawInfo/>"
							: ("<LuckyDrawInfo><Boxes i:nil=\"true\"/><CardDeck>" + (type == "PRESETCAR" ? "LD_CARD_SPECIAL_GOLD" : "LD_CARD_SILVER")
									+ "</CardDeck><CurrentStreak>0</CurrentStreak><IsStreakBroken>false</IsStreakBroken><Items>" + GetPrize()
									+ "</Items><NumBoxAnimations>100</NumBoxAnimations><NumCards>5</NumCards></LuckyDrawInfo>"))
					+ "<OriginalRewards><Rep>0</Rep><Tokens>0</Tokens></OriginalRewards><RewardInfo>" + Rewards
					+ "</RewardInfo></Accolades><Durability>100</Durability><EventId>384</EventId><EventSessionId>1000000000</EventSessionId><ExitPath>ExitToFreeroam</ExitPath><InviteLifetimeInMilliseconds>0</InviteLifetimeInMilliseconds><LobbyInviteId>0</LobbyInviteId><PersonaId>RELAYPERSONA</PersonaId><Heat>1</Heat></PursuitEventResult>";
		} catch (IOException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
	}
}
