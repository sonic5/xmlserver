package com.xmlserver.main;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMFactory;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Main {
	private static final Logger logger = Logger.getLogger(Main.class);
	public static OMFactory fac = OMAbstractFactory.getOMFactory();

	public static void main(String[] arg) throws Exception {

		logger.info("server start");
		HttpServer server = HttpServer.create(new InetSocketAddress(8001), 0);
		server.createContext("/upload", new UploadHandler());
		server.start();

	}

	static class UploadHandler implements HttpHandler {

		public void handle(final HttpExchange exchange) {
			new Thread(new Runnable() {

				public void run() {

					try {
						BPClient bpclient = new BPClient();
						// (get)

						String queryString = exchange.getRequestURI()
								.getQuery();

						// (post)
						String postString = IOUtils.toString(exchange
								.getRequestBody());
						if (queryString == null) {
							queryString = "";
						}
						if (postString == null) {
							postString = "";
						} else {
							// JSON.parseObject(postString,ConfigModel.class);

							bpclient.setTransConfigMap(JSONObject
									.parseObject(postString));
						}
						String result = "";
						logger.info("BPClient invocation Started");
						// System.setProperty("javax.net.debug", "all");
						try {
							// TODO: test

							if (bpclient.getTransConfigMap() != null
									&& !bpclient.getTransConfigMap()
											.get("payLoad").equals("")) {

								String pl = bpclient
										.readPayload((String) bpclient
												.getTransConfigMap().get(
														"payLoad"));
								bpclient.setPayload(pl);
							} else {
								/*
								 * System.out.println(bpclient.getJarPath()
								 * +"OEM_SOAP_Request.xml"); String
								 * pl=bpclient.readPayload(
								 * bpclient.getJarPath() +
								 * "OEM_SOAP_Request.xml");
								 * bpclient.setPayload(pl);
								 */
							}

							// System.out.println(pl);

							bpclient.omNs = bpclient.fac.createOMNamespace(
									"http://ciscoB2BService.com/xsd", "mesha");

							MessageContext messageContext = new MessageContext();
							messageContext.setEnvelope(OMAbstractFactory
									.getSOAP11Factory().getDefaultEnvelope());
							ServiceClient sender = bpclient
									.setThinClientPushHeaders(
											new ServiceClient(), messageContext);

							bpclient.targetEPR = new EndpointReference(bpclient
									.getProperty("serverAddr"));

							result = bpclient
									.doThinClientDemo(sender,
											"urn:pushB2BAction", "push",
											messageContext);

						} catch (Exception exception) {
							

							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							exception.printStackTrace(new PrintStream(baos));
							result = baos.toString();
							logger.error(result);
						}

						exchange.sendResponseHeaders(200, 0);
						OutputStream os = exchange.getResponseBody();

						os.write(result.getBytes());

						os.close();
					} catch (IOException e) {
						logger.error(e);
					} catch (Exception e) {
						logger.error(e);
					}
				}
			}).start();
		}
	}

}
