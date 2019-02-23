//package com.cisco.mesha;
//package com.herakles.test;
package com.xmlserver.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.llom.util.AXIOMUtil;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.axis2.transport.http.HttpTransportProperties.Authenticator;
import org.apache.axis2.util.UUIDGenerator;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONObject;

public class BPClient {
	private static final Logger logger = Logger.getLogger(BPClient.class);

	public   OMFactory fac = OMAbstractFactory.getOMFactory();
	public   EndpointReference targetEPR = null;
	public   OMNamespace omNs;
	public   SOAPFactory soapFactory = OMAbstractFactory.getSOAP11Factory();
	// 从客户端传过来的配置
	private Map<String, Object> transConfigMap = new JSONObject();

	private static String payload;
	private static Properties properties = new Properties();
	static {
		InputStream in = null;
		try {

			String pa = BPClient.getJarPath() + "xmlserver.conf";

			File filep = new File(pa);
			in = new FileInputStream(filep);

			BPClient.properties.load(in);

		} catch (Exception e) {
			logger.error("read config error", e);
		}
	}

	public String getProperty(String key) {
		
		// 以客户端传过来的为准
		if (transConfigMap==null){
			if (key.equals("messagePropertiesProperty1")){
				return properties.getProperty(key).trim()+System.currentTimeMillis();
			}
			return properties.getProperty(key).trim();
		}
		String v = (String) transConfigMap.get(key);
		if (v == null  ) {
			if (key.equals("messagePropertiesProperty1")){
				return properties.getProperty(key).trim()+System.currentTimeMillis();
			}
			return properties.getProperty(key).trim();
		} else {
			
			return v;
		}

	}

	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}

 

	public static String getJarPath() {
		String path = BPClient.class.getResource("/").getPath();
		File file = new File(path);
		String pa = file.getAbsolutePath() + File.separator;
		return pa;
	}

	public   String getPayload() {
		return payload;
	}

	public   void setPayload(String payload) {
		BPClient.payload = payload;
	}

	public static String getJREPath() {
		URL url;
		// 得到jar程序的路径
		url = BPClient.class.getProtectionDomain().getCodeSource().getLocation();
		// 将url路径转码，主要应用于汉字
		String temp = "";
		try {
			temp = URLDecoder.decode(url.getFile(), "UTF-8");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		System.out.println(temp);
		return temp;
	}

	public String readPayload(String payLoad) throws FileNotFoundException {
		 
		System.out.println("Loading the payload");
		String quoteDoc = null;
		InputStream inStream = null;
		String fileName = null;

		//inStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(payLoad);
		inStream=new FileInputStream(new File(payLoad));

		BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
		StringBuilder builder = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append("\n"); // appende a new line
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				inStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// System.out.println(builder.toString());

		return builder.toString();
	}

	public static String getZuluTimeStamp() {
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormatter.format(new java.util.Date());
	}
	private String getUtf8Nobom(String quoteDoc) throws UnsupportedEncodingException{
		 byte[] b = quoteDoc.getBytes(); 
		 String r=quoteDoc;
		 System.out.println("-----------------");
		 System.out.println(b[0]+""+b[1]+""+b[2]+""+b[3]+""+b[4]+""+b[5]);
		 if ((b[0]& 0xff)==0xef && (b[1]& 0xff)==0xbb && ((b[2]& 0xff)==0xbf || (b[2]& 0xff)==63)){
			 System.out.println("----utf8 bom");
			 r = new String(b,3,b.length-3,"UTF-8"); 
		 }
          
         return r;
         
	}
	private OMElement getPayload(String quoteDoc) throws XMLStreamException, UnsupportedEncodingException {
		OMElement document = null;

		 
			//quoteDoc = URLEncoder.encode(quoteDoc, "UTF-8");
			quoteDoc=getUtf8Nobom(quoteDoc);
			document = AXIOMUtil.stringToOM(quoteDoc);

		 
		return document;
	}

	public String doThinClientDemo(ServiceClient sender, String action, String mepType, MessageContext mc) throws Exception {
		String quoteRequest = null;
	 
			Options options = new Options();
			options.setAction(action);
			options.setTo(targetEPR);
			options.setTimeOutInMilliSeconds(200 * 60 * 60);

			options.setProperty("__CHUNKED__", Boolean.FALSE);
			HttpTransportProperties.Authenticator basicAuth = new HttpTransportProperties.Authenticator();

			List auth = new ArrayList();
			auth.add(Authenticator.BASIC);
			basicAuth.setAuthSchemes(auth);

			basicAuth.setUsername(properties.getProperty("username").trim());
			basicAuth.setPassword(properties.getProperty("password").trim());

			basicAuth.setPreemptiveAuthentication(true);
			options.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, basicAuth);

			sender.setOptions(options);
			OperationClient mepClient = sender.createClient(ServiceClient.ANON_OUT_IN_OP);
			mepClient.addMessageContext(mc);

			// String jksFile =
			// BPClient.class.getClassLoader().getResource(p.getProperty("keyfile").trim()).getPath();
			String jksFile = getJarPath() + properties.getProperty("keyfile").trim();
			 
			System.setProperty("javax.net.ssl.trustStore", jksFile);
			System.setProperty("javax.net.ssl.trustStorePassword", properties.getProperty("keypass").trim());
			System.setProperty("javax.net.ssl.trustStoreType", "JKS");

			quoteRequest = getPayload();
			if (quoteRequest==null || quoteRequest.equals("")){
				throw new Exception("payloader is null");	
			}
			mc.getEnvelope().getBody().addChild(getPayload(quoteRequest));
			logger.info("Invoking the service ::::");
			logger.info("Here is the request" + mc.getEnvelope());
			 
			 
			mepClient.execute(true);
			MessageContext response = mepClient.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
			logger.info("Acknowledgement from Service::" + response.getEnvelope());
			String result = response.getEnvelope().toString();
			logger.info("Service Invoked Sucessfully");

			sender.cleanup();
			sender = null;
			options = null;
			return result;
		 
	}

	public ServiceClient setThinClientPushHeaders(ServiceClient sender, MessageContext messageContext) {

		try {
			String ret = UUIDGenerator.getUUID();
			ret = ret + "@" + getProperty("fromURI");
			logger.info("Message Id/UPID for this transaction " + ret);
			messageContext.setMessageID(ret);

			SOAPHeaderBlock messaging = soapFactory.createSOAPHeaderBlock("Messaging", omNs);
			messaging.addAttribute(
					soapFactory.createOMAttribute("schemaLocation", omNs, "http://www.w3.org/2001/XMLSchema-instance"
							+ " " + "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/core/ebms-header-3_0-200704.xsd"));

			messaging.setMustUnderstand(false);

			OMFactory fac = OMAbstractFactory.getOMFactory();
			OMNamespace ebOmNs = fac.createOMNamespace("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/",
					"eb");
			OMElement userMessage = fac.createOMElement("UserMessage", ebOmNs);
			OMElement messageInfo = fac.createOMElement("MessageInfo", ebOmNs);
			OMElement timeStamp = fac.createOMElement("Timestamp", ebOmNs);
			OMElement messageId = fac.createOMElement("MessageId", ebOmNs);
			timeStamp.addChild(fac.createOMText(messageInfo, getZuluTimeStamp()));
			messageId.addChild(fac.createOMText(messageId, ret));
			messageInfo.addChild(timeStamp);
			messageInfo.addChild(messageId);
			userMessage.addChild(messageInfo);

			OMElement partyInfo = fac.createOMElement("PartyInfo", ebOmNs);
			OMElement partyInfoFrom = fac.createOMElement("From", ebOmNs);
			OMElement partyInfoFromPartyId = fac.createOMElement("PartyId", ebOmNs);
			OMElement partyInfoFromRole = fac.createOMElement("Role", ebOmNs);
			OMElement partyInfoTo = fac.createOMElement("To", ebOmNs);
			OMElement partyInfoToPartyId = fac.createOMElement("PartyId", ebOmNs);
			OMElement partyInfoToRole = fac.createOMElement("Role", ebOmNs);

			partyInfoFromPartyId.addChild(fac.createOMText(partyInfoFromPartyId, getProperty("fromURI")));
			partyInfoFromRole.addChild(fac.createOMText(partyInfoFromRole, getProperty("role")));
			partyInfoToPartyId.addChild(fac.createOMText(partyInfoToPartyId, getProperty("toURI")));
			partyInfoToRole.addChild(fac.createOMText(partyInfoToRole, getProperty("partyId")));
			partyInfoFrom.addChild(partyInfoFromPartyId);
			partyInfoFrom.addChild(partyInfoFromRole);
			partyInfoTo.addChild(partyInfoToPartyId);
			partyInfoTo.addChild(partyInfoToRole);
			partyInfo.addChild(partyInfoFrom);
			partyInfo.addChild(partyInfoTo);
			userMessage.addChild(partyInfo);

			OMElement collaborationInfo = fac.createOMElement("CollaborationInfo", ebOmNs);
			OMElement collaborationInfoAgreementRef = fac.createOMElement("AgreementRef", ebOmNs);
			OMElement collaborationInfoService = fac.createOMElement("Service", ebOmNs);
			OMElement collaborationInfoAction = fac.createOMElement("Action", ebOmNs);
			OMElement collaborationInfoConversationId = fac.createOMElement("ConversationId", ebOmNs);
			collaborationInfoAgreementRef
					.addChild(fac.createOMText(collaborationInfoAgreementRef, getProperty("agreementRef")));
			collaborationInfoService.addChild(fac.createOMText(collaborationInfoService, "QuoteToCollect"));
			collaborationInfoAction.addChild(fac.createOMText(collaborationInfoAction, "NewPurchaseOrder"));
			// collaborationInfoConversationId.addChild(fac.createOMText(collaborationInfoConversationId,
			// "4321"));
			collaborationInfoConversationId.addChild(fac.createOMText(collaborationInfoConversationId, ""));
			collaborationInfo.addChild(collaborationInfoConversationId);
			collaborationInfoService.addAttribute("type", "MyServiceTypes", null);
			userMessage.addChild(collaborationInfo);

			OMElement messageProperties = fac.createOMElement("MessageProperties", ebOmNs);
			OMElement messagePropertiesProperty1 = fac.createOMElement("Property", ebOmNs);
			OMElement messagePropertiesProperty2 = fac.createOMElement("Property", ebOmNs);
			messagePropertiesProperty1
					.addChild(fac.createOMText(messagePropertiesProperty1, getProperty("messagePropertiesProperty1")));
			messagePropertiesProperty2
					.addChild(fac.createOMText(messagePropertiesProperty2, getProperty("messagePropertiesProperty2")));
			messagePropertiesProperty1.addAttribute("name", "ProcessInst", null);
			messagePropertiesProperty2.addAttribute("name", "ContextID", null);

			userMessage.addChild(messageProperties);

			OMElement payloadInfo = fac.createOMElement("PayloadInfo", ebOmNs);

			{

				OMElement payloadInfoPartInfo = fac.createOMElement("PartInfo", ebOmNs);
				payloadInfoPartInfo.addAttribute("href", getProperty("partInfoHref"), null);
				OMElement payloadInfoPartInfoSchema = fac.createOMElement("Schema", ebOmNs);
				payloadInfoPartInfoSchema.addAttribute("location", "http://www.cisco.com/assets/wsx_xsd/QWS/root.xsd",
						null);

				OMElement payloadInfoPartInfoPartProperties = fac.createOMElement("PartProperties", ebOmNs);
				OMElement payloadInfoPartInfoPartPropertiesProperty1 = fac.createOMElement("Property", ebOmNs);

				payloadInfoPartInfoPartPropertiesProperty1.addAttribute("name", "Description", null);
				payloadInfoPartInfoPartPropertiesProperty1.addChild(fac.createOMText(
						payloadInfoPartInfoPartPropertiesProperty1, getProperty("payloadPropertiesProperty1")));

				OMElement payloadInfoPartInfoPartPropertiesProperty2 = fac.createOMElement("Property", ebOmNs);
				payloadInfoPartInfoPartPropertiesProperty2.addAttribute("name", "MimeType", null);
				payloadInfoPartInfoPartPropertiesProperty2
						.addChild(fac.createOMText(payloadInfoPartInfoPartPropertiesProperty2, "application/xml"));// "application/xml"
				OMElement payloadInfoPartInfoPartPropertiesProperty4 = fac.createOMElement("Property", ebOmNs);
				payloadInfoPartInfoPartPropertiesProperty4.addAttribute("name", "CharacterSet", null);
				payloadInfoPartInfoPartPropertiesProperty4
						.addChild(fac.createOMText(payloadInfoPartInfoPartPropertiesProperty4, "utf-8"));
				payloadInfoPartInfoPartProperties.addChild(payloadInfoPartInfoPartPropertiesProperty4);

				payloadInfoPartInfoPartProperties.addChild(payloadInfoPartInfoPartPropertiesProperty1);
				payloadInfoPartInfoPartProperties.addChild(payloadInfoPartInfoPartPropertiesProperty2);

				payloadInfoPartInfo.addChild(payloadInfoPartInfoSchema);
				payloadInfoPartInfo.addChild(payloadInfoPartInfoPartProperties);
				payloadInfo.addChild(payloadInfoPartInfo);
			}

			userMessage.addChild(payloadInfo);
			messaging.addChild(ElementHelper.toSOAPHeaderBlock(userMessage, soapFactory));
			logger.info("messageing:" + messaging);
			logger.info("header:" + messageContext.getEnvelope().getHeader());
			messageContext.getEnvelope().getHeader().addChild(messaging);

		} catch (Exception exception) {
			exception.printStackTrace();
			logger.error(exception);
		}
		return sender;
	}

	public Map<String, Object> getTransConfigMap() {
		return transConfigMap;
	}

	public void setTransConfigMap(Map<String, Object> transConfigMap) {
		this.transConfigMap = transConfigMap;
	}

}