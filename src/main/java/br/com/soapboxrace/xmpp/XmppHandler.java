package br.com.soapboxrace.xmpp;

public class XmppHandler {

	private XmppTalk xmppTalk;

	public XmppHandler(XmppTalk xmppTalk) {
		this.xmppTalk = xmppTalk;
	}

	public String read() {
		String read = xmppTalk.read();
		return read;
	}

}
