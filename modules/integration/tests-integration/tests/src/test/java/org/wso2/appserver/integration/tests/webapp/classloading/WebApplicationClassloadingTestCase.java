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
package org.wso2.appserver.integration.tests.webapp.classloading;

import org.apache.axiom.om.OMElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.appserver.integration.common.clients.WebAppAdminClient;
import org.wso2.appserver.integration.common.utils.ASIntegrationTest;
import org.wso2.appserver.integration.common.utils.WebAppDeploymentUtil;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpClientUtil;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public abstract class WebApplicationClassloadingTestCase extends ASIntegrationTest {

	public static final String PASS = "Pass";
	public static final String FAIL = "Fail";
    private String hostName;

	private String webAppFileName;
	private String webAppName;
	protected WebAppAdminClient webAppAdminClient;
	

	@BeforeClass(alwaysRun = true)
	public void init() throws Exception {
		super.init();
		webAppAdminClient = new WebAppAdminClient(backendURL, sessionCookie);
        hostName = new URL(getWrkUrlHttp()).getHost();

	}

    @AfterClass(alwaysRun = true)
    public void cleanupWebApps() throws Exception {
        webAppAdminClient.deleteWebAppFile(webAppFileName, hostName);
        assertTrue(WebAppDeploymentUtil.isWebApplicationUnDeployed(
                backendURL, sessionCookie,
                webAppName), "Web Application unDeployment failed");

    }

	@Test(groups = "wso2.as", description = "Deploying web application")
	public void webApplicationDeploymentTest() throws Exception {
		webAppAdminClient
				.uploadWarFile(FrameworkPathUtil.getSystemResourceLocation()
						+ "artifacts" + File.separator + "AS" + File.separator
						+ "war" + File.separator + webAppFileName);

		assertTrue(WebAppDeploymentUtil.isWebApplicationDeployed(
				backendURL, sessionCookie,
				webAppName), "Web Application Deployment failed");
	}
	
	@Test(groups = "wso2.as", description = "Invoke web application", dependsOnMethods = "webApplicationDeploymentTest")
	public void testInvokeWebApp() throws Exception {
        assertTrue(WebAppDeploymentUtil.isWebApplicationAvailable(webAppURL), "Web App not deployed on workers");
		Map<String, String> results = toResultMap(runAndGetResultAsString(webAppURL));
		assertEquals(PASS, results.get("Tomcat"), "Web app response is incorrect");
		assertEquals(PASS, results.get("Carbon"), "Web app response is incorrect");
		assertEquals(PASS, results.get("CXF"), "Web app response is incorrect");
		assertEquals(PASS, results.get("Spring"), "Web app response is incorrect");
	}

	protected OMElement runAndGetResultAsOMElement(String webAppURL)
			throws Exception {
		HttpClientUtil client = new HttpClientUtil();
		return client.get(webAppURL);
	}

	protected String runAndGetResultAsString(String webAppURL) throws Exception {
		HttpClientUtil client = new HttpClientUtil();
		return client.get(webAppURL).toString();
	}

	protected Map<String, String> toResultMap(String resultString)
			throws Exception {
		if (resultString == null) {
			System.out.println("resultString is null");
			return null;
		}
		resultString = resultString.replace("<status>", "").replace(
				"</status>", "");
		Map<String, String> resultMap = new HashMap<String, String>();
		String[] resultArray = resultString.split(",");
		for (String s : resultArray) {
			String[] temp = s.split("-");
			if (temp != null && !temp.equals("")) {
				resultMap.put(temp[0], temp[1]);
			}
		}
		System.out.println(resultMap);
		return resultMap;
	}

	public String getWebAppFileName() {
		return webAppFileName;
	}

	public String getWebAppName() {
		return webAppName;
	}
	
	public void setWebAppFileName(String webAppFileName) {
		this.webAppFileName = webAppFileName;
	}

	public void setWebAppName(String webAppName) {
		this.webAppName = webAppName;
	}
	public String getWebAppURL() {
		return webAppURL;
	}

	public void setWebAppURL(String webAppURL) {
		this.webAppURL = webAppURL;
	}

}
