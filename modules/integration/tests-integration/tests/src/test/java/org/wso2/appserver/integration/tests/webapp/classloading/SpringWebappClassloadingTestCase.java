/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.appserver.integration.tests.webapp.classloading;

import org.json.JSONArray;
import org.json.JSONException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.appserver.integration.common.clients.WebAppAdminClient;
import org.wso2.appserver.integration.common.utils.ASIntegrationConstants;
import org.wso2.appserver.integration.common.utils.ASIntegrationTest;
import org.wso2.appserver.integration.common.utils.SqlDataSourceUtil;
import org.wso2.appserver.integration.common.utils.WebAppDeploymentUtil;
import org.wso2.appserver.integration.common.utils.WebAppMode;
import org.wso2.appserver.integration.common.utils.WebAppTypes;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.extensions.servers.utils.FileManipulator;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertTrue;

//jar files required to be copied
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class SpringWebappClassloadingTestCase extends ASIntegrationTest {

    private WebAppMode webAppMode;
    private static ServerConfigurationManager serverConfigurationManager;
    private WebAppAdminClient webAppAdminClient;
    private SqlDataSourceUtil sqlDataSource;
    private static File destRuntimeLibDir;
    private static int isRestarted = 0;

    @Factory(dataProvider = "webAppModeProvider")
    public SpringWebappClassloadingTestCase(WebAppMode webAppMode) {
        this.webAppMode = webAppMode;
    }

    @DataProvider
    private static WebAppMode[][] webAppModeProvider() {
        return new WebAppMode[][] {
                new WebAppMode[] {new WebAppMode("spring3-restful-jndi-service", TestUserMode.SUPER_TENANT_ADMIN)},
                new WebAppMode[] {new WebAppMode("spring3-restful-jndi-service", TestUserMode.TENANT_USER)},
//                new WebAppMode[] {new WebAppMode("spring4-restful-jndi-service", TestUserMode.SUPER_TENANT_ADMIN)},
//                new WebAppMode[] {new WebAppMode("spring4-restful-jndi-service", TestUserMode.TENANT_USER)},
        };
    }


    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init(webAppMode.getUserMode());
        String webappClassloadingEnv = "webapp-classloading-environments.xml";

        webAppURL = getWebAppURL(WebAppTypes.WEBAPPS);

        //Restart the Server only once
        if (isRestarted == 0) {
            serverConfigurationManager =
                    new ServerConfigurationManager(new AutomationContext("AS", TestUserMode.SUPER_TENANT_ADMIN));
            File sourceWebappClassloadingDir = new File(
                    FrameworkPathUtil.getSystemResourceLocation() + "artifacts" + File.separator + "AS" +
                    File.separator + "spring" + File.separator + "webapp" + File.separator + "classloading" +
                    File.separator + webappClassloadingEnv);
            File destWebappClassloadingDir = new File(
                    System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "repository" + File.separator +
                    "conf" + File.separator + "tomcat" + File.separator + webappClassloadingEnv);

            File sourceRuntimeLibDir = new File(
                    ASIntegrationConstants.TARGET_RESOURCE_LOCATION + "spring" + File.separator + "spring3-runtime");
            destRuntimeLibDir = new File(
                    System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "lib" + File.separator +
                    "runtimes" + File.separator + "spring3");
            FileManipulator.copyDir(sourceRuntimeLibDir, destRuntimeLibDir);

            serverConfigurationManager.applyConfiguration(sourceWebappClassloadingDir, destWebappClassloadingDir, true,
                                                          true);
        }
        ++isRestarted;
        sessionCookie = loginLogoutClient.login();

        createTable();
        createDataSource(webAppMode.getWebAppName(), sqlDataSource);
    }

    @Test(groups = "wso2.as", description = "Upload Spring WAR and verify deployment")
    public void testSpringWARUpload() throws Exception {
        String springWarFilePath = ASIntegrationConstants.TARGET_RESOURCE_LOCATION + "spring" + File.separator +
                                   webAppMode.getWebAppName() + ".war";
        webAppAdminClient = new WebAppAdminClient(backendURL, sessionCookie);
        webAppAdminClient.uploadWarFile(springWarFilePath);
        assertTrue(WebAppDeploymentUtil.isWebApplicationDeployed(backendURL, sessionCookie, webAppMode.getWebAppName()));
    }

    @Test(groups = "wso2.as", description = "Verify Get Operation", dependsOnMethods = "testSpringWARUpload")
    public void testGetOperation() throws Exception {
        String endpointURL = "/student";
        String endpoint = webAppURL + "/" + webAppMode.getWebAppName() + endpointURL;
        HttpResponse response = HttpRequestUtil.sendGetRequest(endpoint, null);
        try {
            JSONArray jsonArray = new JSONArray(response.getData());
            assertTrue(jsonArray.length() > 0);
        } catch (JSONException e) {
            assertTrue(false);
        }
    }

    @AfterClass(alwaysRun = true)
    public void restoreServer() throws Exception {
        sessionCookie = loginLogoutClient.login();
        webAppAdminClient = new WebAppAdminClient(backendURL, sessionCookie);
        webAppAdminClient.deleteWebAppFile(webAppMode.getWebAppName() + ".war", asServer.getInstance().getHosts().get("default"));

        //Revert and restart only once
        --isRestarted;
        if (isRestarted == 0) {
            FileManipulator.deleteDir(destRuntimeLibDir);
            serverConfigurationManager.restoreToLastConfiguration();
        }
    }

    private void createTable() throws Exception {
        sqlDataSource = new SqlDataSourceUtil(sessionCookie,asServer.getContextUrls().getBackEndUrl());
        File sqlFile = new File(TestConfigurationProvider.getResourceLocation() + "artifacts" + File.separator + "AS" +
                                File.separator + "spring" + File.separator + "studentDb.sql");
        List<File> sqlFileList = new ArrayList<>();
        sqlFileList.add(sqlFile);
        sqlDataSource.createDataSource(sqlFileList, "dataService");
    }
}
