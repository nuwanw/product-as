/*
 * Copyright 2005,2014 WSO2, Inc. http://www.wso2.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.appserver.integration.tests.logviewer;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.appserver.integration.common.clients.LogViewerClient;
import org.wso2.appserver.integration.common.utils.ASIntegrationTest;
import org.wso2.carbon.automation.test.utils.axis2client.AxisServiceClient;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;
import org.wso2.carbon.logging.view.stub.types.carbon.PaginatedLogEvent;
import org.wso2.carbon.logging.view.stub.types.carbon.PaginatedLogFileInfo;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * This class test the log viewer feature in the super tenant domain
 */
public class LogViewerTestCase extends ASIntegrationTest {
    private static final Log log = LogFactory.getLog(LogViewerTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        AxisServiceClient axisServiceClient = new AxisServiceClient();
        String endpoint = getServiceUrlHttp("echo");
        axisServiceClient.sendReceive(createPayLoad(), endpoint, "echoString");
    }

    @Test(groups = "wso2.as", description = "Open the log viewer and get logs")
    public void testGetPaginatedLogEvents() throws Exception {
        LogViewerClient logViewerClient = new LogViewerClient(backendURL, sessionCookie);
        PaginatedLogEvent logEvents = logViewerClient
                .getPaginatedLogEvents(0, "ALL", "", "", "");

        LogEvent receivedLogEvent = logEvents.getLogInfo()[0];

        assertEquals(receivedLogEvent.getServerName(), "AS",
                     "Unexpected server name was returned.");
        assertEquals(receivedLogEvent.getTenantId(), Integer.toString(MultitenantConstants.SUPER_TENANT_ID),
                     "Unexpected tenant Id was returned.");
    }

    @Test(groups = "wso2.as", description = "Open the application log viewer and get logs")
    public void testGetApplicationLogs() throws Exception {
        LogViewerClient logViewerClient = new LogViewerClient(backendURL, sessionCookie);
        String appName = "echo";
        PaginatedLogEvent logEvents = logViewerClient.getPaginatedApplicationLogEvents(0, "ALL", "",
                                                                                       appName, "",
                                                                                       "");
        assertNotNull(logEvents, "No Log Event found for " + appName);
        assertTrue(logEvents.getLogInfo().length > 0, "No Log Event found for " + appName);
        LogEvent receivedLogEvent = logEvents.getLogInfo()[0];

        // should always return the correct app name as requested
        assertEquals(receivedLogEvent.getAppName(), appName,
                     "Invalid app name was returned.");
        assertEquals(receivedLogEvent.getTenantId(), Integer.toString(MultitenantConstants.SUPER_TENANT_ID),
                     "Unexpected tenant Id was returned.");
    }

    @Test(groups = "wso2.as", description = "Get the local log file information")
    public void testGetLocalLogFiles() throws Exception {
        LogViewerClient logViewerClient = new LogViewerClient(backendURL, sessionCookie);
        PaginatedLogFileInfo logFileInfo = logViewerClient.getLocalLogFiles(0, "", "");
        assertEquals(logFileInfo.getLogFileInfo()[0].getLogDate(), "0_Current Log",
                     "Unexpected log date was returned.");
    }

    private static OMElement createPayLoad() {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://echo.services.core.carbon.wso2.org", "ns");
        OMElement getOme = fac.createOMElement("echoString", omNs);
        OMElement getOmeTwo = fac.createOMElement("in", omNs);
        getOmeTwo.setText("25");
        getOme.addChild(getOmeTwo);
        return getOme;
    }

}
