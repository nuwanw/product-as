/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.appserver.integration.tests.webapp.concurrency;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.appserver.integration.common.utils.ASIntegrationTest;
import org.wso2.appserver.integration.common.utils.TestExceptionHandler;
import org.wso2.appserver.integration.common.utils.WebAppDeploymentUtil;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.testng.Assert.assertTrue;

public class MultipleWebAppByUsersTestCase extends ASIntegrationTest {

    private String webAppFileName1 = "Calendar";
    private String webAppFileName2 = "myServletWAR";
    private String webAppFileName3 = "sample";
    private String filePath1;
    private String filePath2;
    private String filePath3;
    private WebAppWorker worker1;
    private WebAppWorker worker2;
    private WebAppWorker worker3;
    private String webAppURL1;
    private String webAppURL2;
    private String webAppURL3;
    private String user1SessionCookie;
    private String user2SessionCookie;
    private String user3SessionCookie;
    private String mgtHostName;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        filePath1 = FrameworkPathUtil.getSystemResourceLocation() +
                    "artifacts" + File.separator + "AS" + File.separator + "war"
                    + File.separator + "Calendar.war";

        filePath2 = FrameworkPathUtil.getSystemResourceLocation() +
                    "artifacts" + File.separator + "AS" + File.separator + "war"
                    + File.separator + "myServletWAR.war";

        filePath3 = FrameworkPathUtil.getSystemResourceLocation() +
                    "artifacts" + File.separator + "AS" + File.separator + "war"
                    + File.separator + "sample.war";

        webAppURL1 = webAppURL + "/" + webAppFileName1 + "/Calendar.html";
        webAppURL2 = webAppURL + "/" + webAppFileName2 + "/hello";
        webAppURL3 = webAppURL + "/" + webAppFileName3 + "/hello.jsp";
        mgtHostName = new URL(backendURL).getHost();
    }

    @Test(groups = "wso2.as", description = "Deploying web application using multiple threads")
    public void testWebApplicationDeployment() throws Exception {

        User user1 =  asServer.getContextTenant().getTenantUser("user1");
        user1SessionCookie = loginLogoutClient.login(user1.getUserName(), user1.getPassword(), mgtHostName);
        worker1 = new WebAppWorker(user1SessionCookie, backendURL, filePath1);


        User user2 =  asServer.getContextTenant().getTenantUser("user2");
        user2SessionCookie = loginLogoutClient.login(user2.getUserName(), user2.getPassword(), mgtHostName);
        worker2 = new WebAppWorker(user2SessionCookie, backendURL, filePath2);

        User user3 =  asServer.getContextTenant().getTenantUser("user3");
        user3SessionCookie = loginLogoutClient.login(user3.getUserName(), user3.getPassword(), mgtHostName);
        worker3 = new WebAppWorker(user3SessionCookie, backendURL, filePath3);

        TestExceptionHandler exHandler = new TestExceptionHandler();

        Thread t1 = new Thread(worker1);
        t1.start();

        Thread t2 = new Thread(worker2);
        t2.start();

        Thread t3 = new Thread(worker3);
        t3.start();

        try {
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException ignored) {
        }

        if (exHandler.throwable != null) {
            exHandler.throwable.printStackTrace();
            exHandler.throwable.getMessage();
        }

        assertTrue(WebAppDeploymentUtil.isWebApplicationDeployed(
                backendURL, user1SessionCookie, webAppFileName1),
                   "Webapp has not deployed");

        assertTrue(WebAppDeploymentUtil.isWebApplicationDeployed(
                backendURL, user2SessionCookie, webAppFileName2),
                   "Webapp has not deployed");

        assertTrue(WebAppDeploymentUtil.isWebApplicationDeployed(
                backendURL, user3SessionCookie, webAppFileName3),
                   "Webapp has not deployed");


    }

    @Test(groups = "wso2.as", description = "multiple webapp uploader test case - invoke webapps",
          dependsOnMethods = "testWebApplicationDeployment")
    public void testInvokeWebapps() throws IOException {
        assertTrue(WebAppDeploymentUtil.isWebApplicationAvailable(webAppURL1), "Web App not available " +
                                                                               "on worker node " + webAppFileName1);
        HttpResponse response1 = HttpRequestUtil.sendGetRequest(webAppURL1, null);
        assertTrue(response1.getData().contains("<h1>GWT Calendar</h1>"), "Webapp invocation fail");

        assertTrue(WebAppDeploymentUtil.isWebApplicationAvailable(webAppURL2), "Web App not available " +
                                                                               "on worker node " + webAppFileName2);
        HttpResponse response2 = HttpRequestUtil.sendGetRequest(webAppURL2, null);
        assertTrue(response2.getData().contains("HelloServlet in myServletWAR!"),
                   "Webapp invocation fail");

        assertTrue(WebAppDeploymentUtil.isWebApplicationAvailable(webAppURL3), "Web App not available " +
                                                                               "on worker node " + webAppFileName3);
        HttpResponse response3 = HttpRequestUtil.sendGetRequest(webAppURL3, null);
        assertTrue(response3.getData().contains("Sample Application JSP Page"),
                   "Webapp invocation fail");
    }

    @AfterClass(alwaysRun = true)
    public void testCleanup() throws Exception {
        worker1.deleteWebApp();
        worker2.deleteWebApp();
        worker3.deleteWebApp();

        assertTrue(WebAppDeploymentUtil.isWebApplicationUnDeployed(
                backendURL, user1SessionCookie, webAppFileName1),
                   "Webapp has not deployed");

        assertTrue(WebAppDeploymentUtil.isWebApplicationUnDeployed(
                backendURL, user2SessionCookie, webAppFileName2),
                   "Webapp has not deployed");

        assertTrue(WebAppDeploymentUtil.isWebApplicationUnDeployed(
                backendURL, user3SessionCookie, webAppFileName2),
                   "Webapp has not deployed");

        assertTrue(WebAppDeploymentUtil.isWebApplicationNotAvailable(webAppURL1), "Web App still available " +
                                                                                  "on worker node " + webAppFileName1);
        assertTrue(WebAppDeploymentUtil.isWebApplicationNotAvailable(webAppURL2), "Web App still available " +
                                                                                  "on worker node " + webAppFileName2);
        assertTrue(WebAppDeploymentUtil.isWebApplicationNotAvailable(webAppURL3), "Web App still available " +
                                                                                  "on worker node " + webAppFileName3);
    }
}
