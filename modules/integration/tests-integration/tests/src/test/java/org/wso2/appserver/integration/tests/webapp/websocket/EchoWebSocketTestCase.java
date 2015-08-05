/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.appserver.integration.tests.webapp.websocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.tyrus.client.ClientManager;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.wso2.appserver.integration.common.utils.ASIntegrationTest;
import org.wso2.appserver.integration.common.utils.WebAppTypes;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

//web socket does not support when the system fronted with load balancer
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class EchoWebSocketTestCase extends ASIntegrationTest {

    private String baseWsUrl;

    private static CountDownLatch messageLatch;
    private static final String SENT_MESSAGE = "Hello World";
    private Log log = LogFactory.getLog(EchoWebSocketTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        baseWsUrl = getWebAppURL(WebAppTypes.WEBAPPS).replace("http", "ws");
    }

    @Test(groups = "wso2.as", description = "Testing websocket")
    public void testInvokeEchoSample() throws Exception {
        String echoProgrammaticEndpoint = baseWsUrl + "/example/websocket/echoProgrammatic";
        messageLatch = new CountDownLatch(1);

        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

        ClientManager client = ClientManager.createClient();
        ClientEndpoint clientEndpoint = new ClientEndpoint();
        Session session = client.connectToServer(clientEndpoint,
                                                 cec, new URI(echoProgrammaticEndpoint));

        session.getBasicRemote().sendText(SENT_MESSAGE);
        messageLatch.await(100, TimeUnit.SECONDS);

        String message = clientEndpoint.getMessage();
        Assert.assertNotNull(message, "Message is not received to client");
        Assert.assertEquals(message, SENT_MESSAGE,
                            "Websocket Echo response is incorrect.");

    }

    private class ClientEndpoint extends Endpoint {

        private String message = null;

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            log.info("Websocket session id: " + session.getId());
            session.addMessageHandler(new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String message) {
                    log.info("Received message: " + message);
                    setMessage(message);
                    messageLatch.countDown();
                }
            });
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }


    }

}
