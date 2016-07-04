package br.com.soapboxrace.srv;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;

import br.com.soapboxrace.func.Basket;
import br.com.soapboxrace.func.Commerce;
import br.com.soapboxrace.func.Event;
import br.com.soapboxrace.func.Functions;
import br.com.soapboxrace.swing.MainWindow;
import br.com.soapboxrace.xmpp.SubjectCalc;
import br.com.soapboxrace.xmpp.XmppSrv;

public class HttpSrv extends GzipHandler {

	private Basket basket = new Basket();
	private Commerce commerce = new Commerce();
	private Event event = new Event();
	private static Functions fx = new Functions();

	public static String modifiedTarget;
	public static boolean THBroken = false;
	private int iEvent = 0;

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
		try {
			String sLastTarget = target.split("/")[target.split("/").length - 1];
			if (!sLastTarget.contains("cryptoticket") && !sLastTarget.contains("getrebroadcasters")
					&& !sLastTarget.contains("heartbeat")) {
				Functions.log(baseRequest.getMethod() + ": " + sLastTarget);
			}
			modifiedTarget = target;
			boolean isXmpp = false;

			if (target.matches("/soapbox/Engine.svc/User/SecureLoginPersona")) {
				Functions.personaId = baseRequest.getParameter("personaId");
				fx.ChangeDefaultPersona(
						String.valueOf((Integer.parseInt(baseRequest.getParameter("personaId")) / 100) - 1));
			} else if (target.matches("/soapbox/Engine.svc/setusersettings")) {
				fx.WriteText("www/soapbox/Engine.svc/getusersettings.xml",
						new String(Files.readAllBytes(Paths.get("www/soapbox/Engine.svc/getusersettings.xml")),
								StandardCharsets.UTF_8).replace("<starterPackApplied>false</starterPackApplied>",
										"<starterPackApplied>true</starterPackApplied>"));
			} else if (target.matches("/soapbox/Engine.svc/catalog/productsInCategory")) {
				modifiedTarget = target + "_" + baseRequest.getParameter("categoryName");
			} else if (target.matches("/soapbox/Engine.svc/catalog/categories")) {
				modifiedTarget = target + "_" + baseRequest.getParameter("categoryName");
			} else if (target.matches("/soapbox/Engine.svc/powerups/activated(.*)")) {
				isXmpp = true;
				event.processPowerup(sLastTarget, -1);
			} else if (target.matches("/soapbox/Engine.svc/matchmaking/joinqueueevent(.*)")) {
				iEvent = Integer.parseInt(sLastTarget);
				Functions.log("|| Fake Event ID loaded: " + sLastTarget + ". Launch a singleplayer event to load it!");
			} else if (target.matches("/soapbox/Engine.svc/matchmaking/launchevent(.*)")) {
				if (sLastTarget != String.valueOf(iEvent) && iEvent != 0) {
					modifiedTarget = "/soapbox/Engine.svc/matchmaking/launchevent/" + String.valueOf(iEvent);
					iEvent = 0;
					Functions.log("|| -> Fake Event ID has been reset.");
				}
			} else if (target.matches("/soapbox/Engine.svc/badges/set"))
				fx.ChangeBadges(readInputStream(request));
			else if (target.matches("/soapbox/Engine.svc/personas/(.*)/baskets")) {
				modifiedTarget = "baskets";
				basket.processBasket(readInputStream(request));
			} else if (target.matches("/soapbox/Engine.svc/personas/(.*)/commerce")) {
				modifiedTarget = "commerce";
				commerce.saveCommerceData(readInputStream(request));
			} else if (target.matches("/soapbox/Engine.svc/personas/inventory/sell/(.*)")) {
				commerce.sell(sLastTarget, 0);
			} else if (target.matches("/soapbox/Engine.svc/personas/(.*)/defaultcar/(.*)")) {
				fx.ChangeCarIndex(target.split("/")[6], false);
			} else if (target.matches("/soapbox/Engine.svc/personas/(.*)/cars") && baseRequest.getMethod() == "POST") {
				basket.SellCar(baseRequest.getParameter("serialNumber"));
			} else if (target.matches("/soapbox/Engine.svc/personas/(.*)/carslots")) {
				fx.FixCarslots();
			} else if (target.matches("/soapbox/Engine.svc/DriverPersona/GetPersonaInfo")) {
				modifiedTarget = target + "_" + Functions.personaId;
			} else if (target.matches("/soapbox/Engine.svc/DriverPersona/GetPersonaBaseFromList")) {
				modifiedTarget = target + "_" + Functions.personaId;
			} else if (target.matches("/soapbox/Engine.svc/personas/inventory/objects")) {
				modifiedTarget = "/soapbox/Engine.svc/personas/" + Functions.personaId + "/objects";
			} else if (target.matches("/soapbox/Engine.svc/events/notifycoincollected")) {
				fx.SaveTHProgress(baseRequest.getParameter("coins"));
				if (baseRequest.getParameter("coins").equals("32767")) {
					Functions.log("|| Detected TH Finished event.");
					if (fx.GetIsTHStreakBroken().equals("true")) {
						THBroken = true;
						modifiedTarget = "THBroken";
						Functions.log("|| -> Your TH Streak is broken.");
						Functions.answerData = "<Accolades xmlns=\"http://schemas.datacontract.org/2004/07/Victory.DataLayer.Serialization.Event\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\"><FinalRewards><Rep>25</Rep><Tokens>78</Tokens></FinalRewards><HasLeveledUp>false</HasLeveledUp><LuckyDrawInfo><Boxes><LuckyBox><CardDeck>LD_CARD_SILVER</CardDeck></LuckyBox><LuckyBox><CardDeck>LD_CARD_SILVER</CardDeck></LuckyBox><LuckyBox><CardDeck>LD_CARD_SILVER</CardDeck></LuckyBox><LuckyBox><CardDeck>LD_CARD_SILVER</CardDeck></LuckyBox><LuckyBox><CardDeck>LD_CARD_SILVER</CardDeck></LuckyBox></Boxes><CurrentStreak>"
								+ String.valueOf(fx.GetTHStreak())
								+ "</CurrentStreak><IsStreakBroken>true</IsStreakBroken><Items></Items><NumBoxAnimations>100</NumBoxAnimations></LuckyDrawInfo><OriginalRewards><Rep>0</Rep><Tokens>0</Tokens></OriginalRewards><RewardInfo/></Accolades>";
					} else {
						event.ReadArbitration("<TreasureHunt/>");
						modifiedTarget = "THCompleted";
					}
				}
			} else if (target.matches("/soapbox/Engine.svc/event/arbitration")) {
				event.ReadArbitration(readInputStream(request));
				modifiedTarget = "Arbitration";
			} else if (target.matches("/soapbox/Engine.svc/events/accolades")) {
				if (THBroken) {
					Functions.log("|| -> Your TH Streak will be revived for 1000 Boost.");
					event.ReadArbitration("<TreasureHunt/>");
					modifiedTarget = "THCompleted";
					THBroken = false;
				}
			} else if (target.matches("/soapbox/Engine.svc/events/instancedaccolades")) {
				event.SetPrize(Event.RaceReward);
				modifiedTarget = "RaceReward";
			}

			if (target.contains(".jpg")) {
				response.setContentType("image/jpeg");
			} else {
				response.setContentType("application/xml;charset=utf-8");
			}
			response.setStatus(HttpServletResponse.SC_OK);
			response.setHeader("Connection", "close");
			response.setHeader("Content-Encoding", "gzip");

			byte[] content = null;
			if (Files.exists(Paths.get("www" + modifiedTarget + ".xml"))) {
				content = Files.readAllBytes(Paths.get("www" + modifiedTarget + ".xml"));
			} else if (Files.exists(Paths.get("www" + modifiedTarget))
					&& !Files.isDirectory(Paths.get("www" + modifiedTarget))) {
				content = Files.readAllBytes(Paths.get("www" + modifiedTarget));
			} else if (modifiedTarget != target) {
				content = Functions.answerData.getBytes(StandardCharsets.UTF_8);
			}

			if (content == null) {
				response.getOutputStream().println();
				response.getOutputStream().flush();
			} else {
				if (!target.contains(".jpg")) {
					String sContent = new String(content, StandardCharsets.UTF_8);
					if (sContent.contains("RELAYPERSONA"))
						sContent = sContent.replace("RELAYPERSONA", Functions.personaId);

					content = gzip(sContent.getBytes(StandardCharsets.UTF_8));
					response.setContentLength(content.length);

					response.getOutputStream().write(content);
					response.getOutputStream().flush();
				} else {
					response.setContentLength(content.length);

					response.getOutputStream().write(content);
					response.getOutputStream().flush();
					response.getOutputStream().println();
					response.getOutputStream().flush();
				}
			}
			baseRequest.setHandled(true);
			if (isXmpp) {
				sendXmpp(target);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String readInputStream(HttpServletRequest request) {
		StringBuilder buffer = new StringBuilder();
		try {
			BufferedReader reader = request.getReader();
			String line;
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buffer.toString();
	}

	private byte[] gzip(byte[] data) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream(data.length);
		try {
			OutputStream gzipout = new GZIPOutputStream(byteStream) {
				{
					def.setLevel(1);
				}
			};
			try {
				gzipout.write(data);
			} finally {
				gzipout.close();
			}
		} finally {
			byteStream.close();
		}
		return byteStream.toByteArray();
	}

	private static String setXmppSubject(String msg) {
		String[] splitMsg = msg.split("<body>|</body>");
		String[] splitMsgTo = splitMsg[0].split("\\\"");
		String msgTo = splitMsgTo[5];
		String msgBody = splitMsg[1];
		msgBody = msgBody.replace("&lt;", "<");
		msgBody = msgBody.replace("&gt;", ">");
		Long subject = SubjectCalc.calculateHash(msgTo.toCharArray(), msgBody.toCharArray());
		msg = msg.replace("LOLnope.", subject.toString());
		return msg;
	}

	private void sendXmpp(String target) {
		try {
			String path = "www" + target + "_xmpp.xml";
			File fxmpp = new File(path);
			byte[] encoded = null;
			if (fxmpp.exists()) {
				encoded = Files.readAllBytes(Paths.get(path));
				if (encoded != null) {
					String msg = new String(encoded, StandardCharsets.UTF_8).replace("RELAYPERSONA",
							Functions.personaId);
					Long personaIdLong = Long.decode(Functions.personaId);
					msg = setXmppSubject(msg);
					XmppSrv.sendMsg(personaIdLong, msg);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		MainWindow mainWindow = new MainWindow();
		mainWindow.setVisible(true);
		Functions.setLogTextArea(mainWindow.getLogTextArea());
		Functions.log("Starting offline server");
		System.setProperty("jsse.enableCBCProtection", "false");
		try {
			Locale newLocale = new Locale("en", "GB");
			Locale.setDefault(newLocale);

			Server server = new Server(7331);
			server.setHandler(new HttpSrv());
			server.start();

			XmppSrv xmppSrv = new XmppSrv();
			xmppSrv.start();

			Functions.log("");
			String THDate = fx.ReadText("www/soapbox/Engine.svc/serverSettings/THDate");
			if (THDate != LocalDate.now().toString()) {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.ENGLISH);
				LocalDate lastCompletedTHDate = LocalDate.parse(THDate, formatter);
				LocalDate nowDate = LocalDate.now();
				long days = ChronoUnit.DAYS.between(lastCompletedTHDate, nowDate);

				Functions.log("|| Last TH completed was on " + lastCompletedTHDate.toString() + ".");
				if (days == 0) {
					Functions.log("|| -> Since that date is today, nothing will be done.");
				} else if (days == 1) {
					fx.StartNewTH(true);
				} else if (days >= 2) {
					fx.StartNewTH(false);
					Functions.log("|| -> Since that date, it's been " + String.valueOf(days)
							+ " days. Your TH Streak is broken.");
				} else {
					Functions.log("|| !! -> Go back where you came from time traveller!");
				}
			}

			String[] settings = Files.readAllLines(Paths.get("www/soapbox/Engine.svc/serverSettings/settings"))
					.toArray(new String[] {});
			Functions.rewards = new int[] { Integer.parseInt(settings[1]), Integer.parseInt(settings[5]),
					Integer.parseInt(settings[6]), Integer.parseInt(settings[7]), Integer.parseInt(settings[8]) };
			Functions.multipliers = new double[] { Double.parseDouble(settings[2]), Double.parseDouble(settings[3]),
					Double.parseDouble(settings[4]) };
			Functions.rankDrop = new int[][] { new int[] {}, fx.StringArrayToIntArray(settings[10]),
					fx.StringArrayToIntArray(settings[11]), fx.StringArrayToIntArray(settings[12]),
					fx.StringArrayToIntArray(settings[13]) };
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
