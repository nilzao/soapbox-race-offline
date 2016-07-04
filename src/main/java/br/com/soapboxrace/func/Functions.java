package br.com.soapboxrace.func;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import br.com.soapboxrace.srv.HttpSrv;

public class Functions {

	public static String personaId = "100";
	public static String answerData = null;
	private static javax.swing.JTextArea logTextArea;

	public static void setLogTextArea(javax.swing.JTextArea logTextArea) {
		Functions.logTextArea = logTextArea;
	}

	public static int[][] rankDrop = new int[][] { new int[] {}, new int[] { 1, 0, 3, 2, 0, -1, 1, 2, 3, 0 },
			new int[] { 1, 0, 0, 2, 0, -1, 1, 2, 0, 0 }, new int[] { 1, 0, 0, 1, 0, -1, 1, 1, 0, 0 } };
	public static int[] rewards = new int[] { 20000, 250, 1000, 500, 1000 };
	public static double[] multipliers = new double[] { 0.5, 1.0, 1.45 };

	public String ReadCarIndex() throws ParserConfigurationException, SAXException, IOException {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse("www/soapbox/Engine.svc/personas/" + personaId + "/carslots.xml")
				.getElementsByTagName("DefaultOwnedCarIndex").item(0).getTextContent();
	}

	public void ChangeCarIndex(String carId, Boolean literal) {
		try {
			log("|| ChangeCarIndex has been called to set carIndex.");
			String carIndex;
			if (literal) {
				carIndex = carId;
			} else {
				carIndex = String.valueOf(CountInstances(new String(
						Files.readAllBytes(Paths.get("www/soapbox/Engine.svc/personas/" + personaId + "/carslots.xml")),
						StandardCharsets.UTF_8), "<OwnedCarTrans>", "<Id>" + carId + "</Id>") - 1);
			}
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = docBuilder.parse("www/soapbox/Engine.svc/personas/" + personaId + "/carslots.xml");

			doc.getElementsByTagName("DefaultOwnedCarIndex").item(0).setTextContent(carIndex);
			log("|| -> Car Index has been set to " + carIndex);
			Node OwnedCar = doc.getElementsByTagName("OwnedCarTrans").item(Integer.parseInt(carIndex));
			DOMImplementationLS lsImpl = (DOMImplementationLS) OwnedCar.getOwnerDocument().getImplementation()
					.getFeature("LS", "3.0");
			LSSerializer serializer = lsImpl.createLSSerializer();
			serializer.getDomConfig().setParameter("xml-declaration", false);
			String StringOwnedCar = serializer.writeToString(OwnedCar);
			WriteTempCar(StringOwnedCar);

			WriteXML(doc, "www/soapbox/Engine.svc/personas/" + personaId + "/carslots.xml");
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (SAXException sae) {
			sae.printStackTrace();
		}
	}

	public void FixCarslots() {
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
					.parse("www/soapbox/Engine.svc/personas/" + personaId + "/carslots.xml");

			int _carId = 0;
			int ids = CountInstances(new String(
					Files.readAllBytes(Paths.get("www/soapbox/Engine.svc/personas/" + personaId + "/carslots.xml")),
					StandardCharsets.UTF_8), "<Id>", "</CarsOwnedByPersona>") - 1;
			for (int i = 0; i <= ids; i++) {
				if (i % 2 != 0) {
					_carId++;
					doc.getElementsByTagName("Id").item(i).setTextContent(String.valueOf(_carId));
				}
			}
			WriteXML(doc, "www/soapbox/Engine.svc/personas/" + personaId + "/carslots.xml");

			log("|| Carslots of persona " + personaId + " was reconstructed.");
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	public void ChangeDefaultPersona(String persona) {
		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = docBuilder.parse("www/soapbox/Engine.svc/User/GetPermanentSession.xml");
			doc.getElementsByTagName("defaultPersonaIdx").item(0).setTextContent(persona);
			WriteXML(doc, "www/soapbox/Engine.svc/User/GetPermanentSession.xml");
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void WriteTempCar(String carData) {
		if ("" != carData) {
			BufferedWriter bw;
			try {
				// defaultcar
				bw = new BufferedWriter(
						new FileWriter(new File("www/soapbox/Engine.svc/personas/" + personaId + "/defaultcar.xml")
								.getAbsoluteFile()));
				bw.write(carData);
				bw.close();
				log("  -> DefaultCar has been (re)written with new car data.");
				// cars
				String carsW = carData.replace("<OwnedCarTrans>",
						"<OwnedCarTrans xmlns=\"http://schemas.datacontract.org/2004/07/Victory.DataLayer.Serialization\" xmlns:i=\"http://../.w3.org/2001/XMLSchema-instance\">");
				bw = new BufferedWriter(new FileWriter(
						new File("www/soapbox/Engine.svc/personas/" + personaId + "/cars.xml").getAbsoluteFile()));
				bw.write(carsW);
				bw.close();
				log("  -> Cars has been (re)written with new car data.");
				// commerce
				if (HttpSrv.modifiedTarget == "commerce") {
					String commerceW = carData.replace("<OwnedCarTrans>", "<UpdatedCar>").replace("</OwnedCarTrans>",
							"</UpdatedCar>");
					Functions.answerData = "<CommerceSessionResultTrans xmlns=\"http://schemas.datacontract.org/2004/07/Victory.DataLayer.Serialization\" xmlns:i=\"http://../.w3.org/2001/XMLSchema-instance\"><InvalidBasket i:nil=\"true\"/><InventoryItems><InventoryItemTrans><EntitlementTag>SPOILER_STYLE01_LARGE</EntitlementTag><ExpirationDate i:nil=\"true\"/><Hash>-232471336</Hash><InventoryId>2898928898</InventoryId><ProductId>DO NOT USE ME</ProductId><RemainingUseCount>1</RemainingUseCount><ResellPrice>0.00000</ResellPrice><Status>ACTIVE</Status><StringHash>0xf224c4d8</StringHash><VirtualItemType>visualpart</VirtualItemType></InventoryItemTrans></InventoryItems><Status>Success</Status>"
							+ commerceW + "<Wallets><WalletTrans><Balance>" + String.valueOf(Economy.amount)
							+ "</Balance><Currency>" + (Economy.type == 0 ? "CASH" : "BOOST")
							+ "</Currency></WalletTrans></Wallets></CommerceResultTrans>";
					log("  -> Commerce has been processed using new car data.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void ChangeBadges(String BadgesPacket) {
		try {
			log("|| Change Badges action detected.");
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document packet = docBuilder.parse(new InputSource(new StringReader(BadgesPacket)));
			Document achievements = docBuilder.parse("www/soapbox/Engine.svc/achievements/achdef.xml");
			Document doc = docBuilder
					.parse("www/soapbox/Engine.svc/DriverPersona/GetPersonaBaseFromList_" + personaId + ".xml");
			Document doc2 = docBuilder
					.parse("www/soapbox/Engine.svc/DriverPersona/GetPersonaInfo_" + personaId + ".xml");
			log("|| -> 4 documents have been loaded into memory.");
			int loopA = CountInstances(BadgesPacket, "<SlotId>", "</BadgeBundle>");
			log("|| -> Amount of new badges: " + String.valueOf(loopA) + ". Starting loop to rewrite badge data.");
			for (int i = 1; i <= loopA; i++) {
				int slotId = Integer.parseInt(packet.getElementsByTagName("SlotId").item(i - 1).getTextContent());
				String badgeId = packet.getElementsByTagName("BadgeDefinitionId").item(i - 1).getTextContent();

				int achId = CountInstances(
						new String(Files.readAllBytes(Paths.get("www/soapbox/Engine.svc/achievements/achdef.xml")),
								StandardCharsets.UTF_8),
						"<AchievementRanks>", "<BadgeDefinitionId>" + badgeId + "</BadgeDefinitionId>") - 1;

				String rankId = achievements.getElementsByTagName("AchievementRanks").item(achId).getLastChild()
						.getChildNodes().item(1).getTextContent();
				String isRare = achievements.getElementsByTagName("AchievementRanks").item(achId).getLastChild()
						.getChildNodes().item(2).getTextContent();
				String rarity = achievements.getElementsByTagName("AchievementRanks").item(achId).getLastChild()
						.getChildNodes().item(5).getTextContent();
				log("  ||  New badge data ->  RankID: " + rankId + ", isRare: " + isRare + ", Rarity: " + rarity + ".");
				doc.getElementsByTagName("AchievementRankId").item(slotId).setTextContent(rankId);
				doc.getElementsByTagName("BadgeDefinitionId").item(slotId).setTextContent(badgeId);
				doc.getElementsByTagName("IsRare").item(slotId).setTextContent(isRare);
				doc.getElementsByTagName("Rarity").item(slotId).setTextContent(rarity);
				doc2.getElementsByTagName("AchievementRankId").item(slotId).setTextContent(rankId);
				doc2.getElementsByTagName("BadgeDefinitionId").item(slotId).setTextContent(badgeId);
				doc2.getElementsByTagName("IsRare").item(slotId).setTextContent(isRare);
				doc2.getElementsByTagName("Rarity").item(slotId).setTextContent(rarity);
				log("  || -> New badge data written.");
			}

			WriteXML(doc, "www/soapbox/Engine.svc/DriverPersona/GetPersonaBaseFromList_" + personaId + ".xml");
			WriteXML(doc2, "www/soapbox/Engine.svc/DriverPersona/GetPersonaInfo_" + personaId + ".xml");

			log("|| Change Badges action finalized.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void StartNewTH(boolean isCompleted) {
		try {
			Functions.log("|| -> Generating new TH.");
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = docBuilder.parse(new File("www/soapbox/Engine.svc/events/gettreasurehunteventsession.xml"));
			Random random = new Random();
			int seed = random.nextInt();

			if (isCompleted && doc.getElementsByTagName("CoinsCollected").item(0).getTextContent().equals("32767")) { // On
																														// Streak
				doc.getElementsByTagName("Seed").item(0).setTextContent(String.valueOf(seed));
				doc.getElementsByTagName("CoinsCollected").item(0).setTextContent("0");

				WriteXML(doc, "www/soapbox/Engine.svc/events/gettreasurehunteventsession.xml");
				log("|| -> You're on a roll! A new TH for today has been generated.");
			} else if (!isCompleted
					&& !doc.getElementsByTagName("IsStreakBroken").item(0).getTextContent().equals("true")) { // Lost
																												// Streak
				doc.getElementsByTagName("Seed").item(0).setTextContent(String.valueOf(seed));
				doc.getElementsByTagName("CoinsCollected").item(0).setTextContent("0");
				doc.getElementsByTagName("IsStreakBroken").item(0).setTextContent("true");

				WriteXML(doc, "www/soapbox/Engine.svc/events/gettreasurehunteventsession.xml");
				log("|| -> A new TH has been generated.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void SaveTHProgress(String coins) {
		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = docBuilder.parse(new File("www/soapbox/Engine.svc/events/gettreasurehunteventsession.xml"));

			doc.getElementsByTagName("CoinsCollected").item(0).setTextContent(coins);
			if (coins.equals("32767"))
				doc.getElementsByTagName("Streak").item(0).setTextContent(String
						.valueOf(Integer.parseInt(doc.getElementsByTagName("Streak").item(0).getTextContent()) + 1));

			WriteXML(doc, "www/soapbox/Engine.svc/events/gettreasurehunteventsession.xml");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int GetLevel() throws ParserConfigurationException, SAXException, IOException {
		return Integer.parseInt(DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse("www/soapbox/Engine.svc/DriverPersona/GetPersonaBaseFromList_" + personaId + ".xml")
				.getElementsByTagName("Level").item(0).getTextContent());
	}

	public int GetTHStreak() throws DOMException, SAXException, IOException, ParserConfigurationException {
		return Integer.parseInt(DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse("www/soapbox/Engine.svc/events/gettreasurehunteventsession.xml").getElementsByTagName("Streak")
				.item(0).getTextContent());
	}

	public String GetIsTHStreakBroken() throws DOMException, SAXException, IOException, ParserConfigurationException {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse("www/soapbox/Engine.svc/events/gettreasurehunteventsession.xml")
				.getElementsByTagName("IsStreakBroken").item(0).getTextContent();
	}

	public int CountInstances(String dataString, String toCount, String toFind) {
		int maxIndex = dataString.indexOf(toFind);
		int currentIndex = 0;
		int occurrences = 0;

		while (currentIndex < maxIndex) {
			currentIndex = dataString.indexOf(toCount, currentIndex) + toCount.length();
			if (currentIndex > maxIndex || currentIndex == (toCount.length() - 1))
				break;

			occurrences++;
		}
		return occurrences;
	}

	public String parseBasketId(String basketTrans) {
		String basketId = "";
		String pattern = "<ProductId>(.*)</ProductId>";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(basketTrans);
		if (m.find() && m.groupCount() > 0) {
			basketId = m.group(1).replace(":", "");
		} else {
			log("BasketId Parse Error.");
		}
		return basketId;
	}

	public int[] StringArrayToIntArray(String input) {
		String[] strArray = input.split(",");
		int[] intArray = new int[strArray.length];
		for (int i = 0; i < strArray.length; i++) {
			intArray[i] = Integer.parseInt(strArray[i]);
		}
		return intArray;
	}

	public static void log(String text) {
		Functions.logTextArea.append(text + "\n");
		System.out.println(text);
	}

	public String ReadText(String location) {
		try {
			return new String(Files.readAllBytes(Paths.get(location)), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void WriteText(String location, String text) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(location).getAbsoluteFile()));
			bw.write(text);
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void WriteXML(Document doc, String location) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter sw = new StringWriter();
			StreamResult result = new StreamResult(sw);
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);

			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(location).getAbsoluteFile()));
			bw.write(sw.toString());
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
