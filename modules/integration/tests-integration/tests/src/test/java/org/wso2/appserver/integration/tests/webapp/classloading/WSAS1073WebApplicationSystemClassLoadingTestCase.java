/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.appserver.integration.tests.webapp.classloading;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.appserver.integration.common.clients.WebAppAdminClient;
import org.wso2.appserver.integration.common.utils.ASIntegrationTest;
import org.wso2.appserver.integration.common.utils.WebAppDeploymentUtil;
import org.wso2.appserver.integration.common.utils.WebAppTypes;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * This test case is to test system class loading in webapps.
 * Please refer WSAS-1073 for more info
 */
public class WSAS1073WebApplicationSystemClassLoadingTestCase extends ASIntegrationTest {
    private static final Log log = LogFactory.getLog(WSAS1073WebApplicationSystemClassLoadingTestCase.class);
    private final String webAppFileName = "SystemClassLoadingWebapp.war";
    private final String webAppName = "SystemClassLoadingWebapp";
    private HttpClient httpClient = new HttpClient();
    private WebAppAdminClient webAppAdminClient = null;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        webAppAdminClient = new WebAppAdminClient(backendURL, sessionCookie);
    }

    @Test(groups = "wso2.as", description = "Deploying web application")
    public void testWebAppDeployment() throws Exception {
        webAppAdminClient.uploadWarFile(Paths.get(FrameworkPathUtil.getSystemResourceLocation(),
                                                  "artifacts", "AS", "war", webAppFileName).toString());
        assertTrue(WebAppDeploymentUtil.isWebApplicationDeployed(backendURL, sessionCookie, webAppName),
                   "Web Application Deployment failed");
        assertTrue(WebAppDeploymentUtil.isWebApplicationAvailable(getWebAppURL(WebAppTypes.WEBAPPS) + "/" + webAppName),
                   "Web App is not available on worker nodes");
    }

    @Test(groups = "wso2.as", description = "Invoke the SystemClassLoadingWebapp",
            dependsOnMethods = {"testWebAppDeployment"})
    public void testSystemClassLoadingWebapp() throws IOException, XPathExpressionException {
        String url = getWebAppURL(WebAppTypes.WEBAPPS) + "/" + webAppName;
        GetMethod getMethod = new GetMethod(url);
        try {
            int statusCode = httpClient.executeMethod(getMethod);
            if (statusCode != HttpStatus.SC_OK) {
                fail("Method failed: " + getMethod.getStatusLine());
            }
        } finally {
            getMethod.releaseConnection();
        }
    }

    @AfterClass(alwaysRun = true, description = "Removing the webapp")
    public void cleanupWebApps() throws Exception {
        webAppAdminClient.deleteWebAppFile(webAppFileName, new URL(getWrkUrlHttp()).getHost());
        assertTrue(WebAppDeploymentUtil.isWebApplicationUnDeployed(backendURL, sessionCookie, webAppName),
                   "Web Application Undeployment failed");
    }
}
