/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.appserver.integration.tests.carbonappservice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.appserver.integration.common.utils.ASIntegrationTest;
import org.wso2.appserver.integration.common.utils.CarAppDeploymentUtil;
import org.wso2.appserver.integration.common.utils.WebAppDeploymentUtil;
import org.wso2.appserver.integration.common.utils.WebAppTypes;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.ApplicationAdminClient;
import org.wso2.carbon.integration.common.admin.client.CarbonAppUploaderClient;

import javax.activation.DataHandler;
import java.io.File;
import java.net.URL;
import java.util.Arrays;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/*
  This class can be used to upload .car application to the server and test deployed services
 */
public class WSAS1798CAppWarUndeployTest extends ASIntegrationTest {

    private static final Log log = LogFactory.getLog(WSAS1798CAppWarUndeployTest.class);
    private final String cAppName = "WarCApp_1.0.0";
    private final String webAppName = "appServer-valid-deploymant-1.0.0";
    private ApplicationAdminClient appAdminClient;
    private TestUserMode userMode;
    private String webAppUrl;

    @Factory(dataProvider = "userModeProvider")
    public WSAS1798CAppWarUndeployTest(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    private static TestUserMode[][] userModeProvider() {
        return new TestUserMode[][]{
                new TestUserMode[]{TestUserMode.SUPER_TENANT_USER},
                new TestUserMode[]{TestUserMode.TENANT_USER},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init(userMode);
        appAdminClient = new ApplicationAdminClient(backendURL, sessionCookie);
        webAppUrl = getWebAppURL(WebAppTypes.WEBAPPS) + "/" + webAppName;
    }

    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        super.cleanup();
        if (checkCAppInAppList()) {
            appAdminClient.deleteApplication(cAppName);
            assertTrue(CarAppDeploymentUtil.isCarFileUnDeployed(backendURL, sessionCookie, cAppName)
                    , cAppName + " Car file undeployment faild for user" + userInfo.getUserName());
        }
    }

    @Test(groups = "wso2.as", description = "upload car file and verify deployment")
    public void carApplicationUploadTest() throws Exception {
        deployCApp();
        assertTrue(CarAppDeploymentUtil.isCarFileDeployed(backendURL, sessionCookie, cAppName),
                   "CApp Deployment failed");
        log.info(cAppName + ".car uploaded successfully for user " + userInfo.getUserName());
    }

    @Test(groups = "wso2.as", description = "verify the deployed services list",
          dependsOnMethods = "carApplicationUploadTest")
    public void verifyWebAppDeploymentTest() throws Exception {
        assertTrue(WebAppDeploymentUtil.
                isWebApplicationDeployed(backendURL, sessionCookie, webAppName)
                , "car app failed to deploy the web application for user " + userInfo.getUserName());
        assertTrue(WebAppDeploymentUtil.isWebApplicationAvailable(webAppUrl),
                   "Web App is not deployed on worker node for user " + userInfo.getUserName());
        HttpResponse response = HttpRequestUtil.sendGetRequest(webAppUrl, null);
        assertNotNull(response, "No HTTP response from web application deployed from CAPP");
        assertEquals(response.getData(), "<status>success</status>", "Web Application invocation failed");
    }

    @Test(groups = "wso2.as", description = "Delete Composite Application",
          dependsOnMethods = "verifyWebAppDeploymentTest")
    public void carAppDeleteTest()
            throws Exception {   // deletes the car application and the service
        appAdminClient.deleteApplication(cAppName);
        assertTrue(CarAppDeploymentUtil.isCarFileUnDeployed(backendURL, sessionCookie, cAppName)
                , "Car file undeployment failed for user " + userInfo.getUserName());
        log.info(cAppName + " CApp deleted");
    }

    @Test(groups = "wso2.as", description = "Web application undeployment",
          dependsOnMethods = "carAppDeleteTest")
    public void verifyWebAppUnDeploymentTest() throws Exception {
        assertTrue(WebAppDeploymentUtil.
                           isWebApplicationUnDeployed(backendURL, sessionCookie, webAppName),
                   "CApp failed to undeploy the wep application for user " + userInfo.getUserName());
        assertTrue(WebAppDeploymentUtil.isWebApplicationNotAvailable(webAppUrl),
                   "Web App is not undeployed on worker node for user " + userInfo.getUserName());
    }

    @Test(groups = "wso2.as", description = "War CApp Re-Deployment",
          dependsOnMethods = "verifyWebAppUnDeploymentTest")
    public void carAppReDeploymentTest() throws Exception {
        deployCApp();
        assertTrue(CarAppDeploymentUtil.isCarFileDeployed(backendURL, sessionCookie, cAppName),
                   "CApp Deployment failed for user" + userInfo.getUserName());
        assertTrue(WebAppDeploymentUtil.
                isWebApplicationDeployed(backendURL, sessionCookie, webAppName)
                , "car app failed to deploy the web application for user " + userInfo.getUserName());
        assertTrue(WebAppDeploymentUtil.isWebApplicationAvailable(webAppUrl),
                   "Web App is not deployed on worker node for user " + userInfo.getUserName());
        HttpResponse response = HttpRequestUtil.sendGetRequest(webAppUrl, null);
        assertNotNull(response, "No HTTP response from web application deployed from CAPP");
        assertEquals(response.getData(), "<status>success</status>", "Web Application invocation failed");
        log.info(cAppName + ".car uploaded successfully");
    }

    private void deployCApp() throws Exception {
        CarbonAppUploaderClient carbonAppClient = new CarbonAppUploaderClient(backendURL, sessionCookie);

        URL url = new URL("file://" + FrameworkPathUtil.getSystemResourceLocation() +
                          "artifacts" + File.separator + "AS" + File.separator + "car" + File.separator +
                          cAppName + ".car");
        DataHandler dataHandler = new DataHandler(url);
        carbonAppClient.uploadCarbonAppArtifact(cAppName + ".car", dataHandler);

    }

    private boolean checkCAppInAppList() throws Exception {
        String[] applicationList = appAdminClient.listAllApplications();
        if(applicationList == null || applicationList.length < 1) {
            return false;
        }
        return Arrays.asList(applicationList).contains(cAppName);
    }

}
