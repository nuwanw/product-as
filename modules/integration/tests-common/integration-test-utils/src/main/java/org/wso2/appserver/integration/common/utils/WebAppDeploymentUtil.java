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
package org.wso2.appserver.integration.common.utils;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.appserver.integration.common.clients.WebAppAdminClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

public class WebAppDeploymentUtil {
    private static Log log = LogFactory.getLog(WebAppDeploymentUtil.class);
    private static int WEBAPP_DEPLOYMENT_DELAY = 90 * 1000;

    public static boolean isWebApplicationDeployed(String backEndUrl, String sessionCookie,
                                                   String webAppName) throws Exception {
        log.info("waiting " + WEBAPP_DEPLOYMENT_DELAY + " millis for Service deployment " + webAppName);
        WebAppAdminClient webAppAdminClient = new WebAppAdminClient(backEndUrl, sessionCookie);
        List<String> webAppList;
        List<String> faultyWebAppList;
        String warName = webAppName + ".war";

        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) < WEBAPP_DEPLOYMENT_DELAY) {
            webAppList = webAppAdminClient.getWebApplist(webAppName);
            faultyWebAppList = webAppAdminClient.getFaultyWebAppList(webAppName);

            for (String faultWebAppName : faultyWebAppList) {
                //need to check both unpacked webapps and war file based webapps
                if (webAppName.equalsIgnoreCase(faultWebAppName) || warName.equalsIgnoreCase(faultWebAppName)) {
                    log.info(webAppName + "- Web Application is faulty");
                    return Boolean.FALSE;
                }
            }

            for (String name : webAppList) {
                if (webAppName.equalsIgnoreCase(name) || warName.equalsIgnoreCase(name)) {
                    log.info(webAppName + " Web Application deployed in " + time + " millis");
                    return Boolean.TRUE;
                }
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                log.warn(ignored.getMessage());

            }
        }
        return Boolean.FALSE;
    }

    /**
     * This will check whether the given web application url(GET) can be accessed within the defined
     * time period by sending the request periodically and check it return 200 or 202 https response codes
     * @param getUrl web application url(GET)
     * @return true if the web application can be accessed
     * @throws IOException when IO error occurred while sending http get request
     */
    public static boolean isWebApplicationAvailable(String getUrl) throws IOException {
        log.info("waiting " + WEBAPP_DEPLOYMENT_DELAY + " millis for web app availability " + getUrl);

        Calendar startTime = Calendar.getInstance();
        HttpResponse response;
        while ((Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis()) < WEBAPP_DEPLOYMENT_DELAY) {
            response = HttpRequestUtil.sendGetRequest(getUrl, null);
            if(response != null) {
                if(response.getResponseCode() == HttpStatus.SC_OK || response.getResponseCode() == HttpStatus.SC_ACCEPTED) {
                    return Boolean.TRUE;
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                log.warn(ignored.getMessage());
            }
        }
        return Boolean.FALSE;
    }

    /**
     * This will check whether the given web application url(GET) can not be accessed within the defined
     * time period by sending the request periodically and check it does not return 200 or 202 https
     * response codes
     * @param getUrl web application url(GET)
     * @return true if the web application can be accessed
     * @throws IOException when IO error occurred while sending http get request
     */
    public static boolean isWebApplicationNotAvailable(String getUrl) throws Exception {
        log.info("waiting " + WEBAPP_DEPLOYMENT_DELAY + " millis for web app availability " + getUrl);

        Calendar startTime = Calendar.getInstance();
        HttpResponse response;
        while ((Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis()) < WEBAPP_DEPLOYMENT_DELAY) {
            response = HttpRequestUtil.sendGetRequest(getUrl, null);
            if(response != null) {
                if(!(response.getResponseCode() == HttpStatus.SC_OK
                     || response.getResponseCode() == HttpStatus.SC_ACCEPTED)) {
                    return Boolean.TRUE;
                }
            } else {
                return Boolean.TRUE;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                log.warn(ignored.getMessage());
            }
        }
        return Boolean.FALSE;
    }

    public static boolean isWebApplicationUnDeployed(String backEndUrl, String sessionCookie,
                                                     String webAppName) throws Exception {
        log.info("waiting " + WEBAPP_DEPLOYMENT_DELAY + " millis for webApp undeployment " + webAppName);
        WebAppAdminClient webAppAdminClient = new WebAppAdminClient(backEndUrl, sessionCookie);
        List<String> webAppList;
        String warName = webAppName + ".war";

        Calendar startTime = Calendar.getInstance();
        while ((Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis()) < WEBAPP_DEPLOYMENT_DELAY) {
            webAppList = webAppAdminClient.getWebApplist(webAppName);
            if (webAppList.size() != 0) {
                for (String name : webAppList) {
                    if (webAppName.equalsIgnoreCase(name) || warName.equalsIgnoreCase(name)) {
                        log.info(webAppName + " -  Web Application not undeployed yet");
                        break;
                    }
                }
            } else {
                return Boolean.TRUE;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                log.warn(ignored.getMessage());
            }
        }
        return Boolean.FALSE;
    }

    public static boolean isFaultyWebApplicationUnDeployed(String backEndUrl, String sessionCookie,
                                                           String webAppName) throws Exception {
        log.info("waiting " + WEBAPP_DEPLOYMENT_DELAY + " millis for Service undeployment " + webAppName);
        WebAppAdminClient webAppAdminClient = new WebAppAdminClient(backEndUrl, sessionCookie);
        List<String> faultyWebAppList;
        String warName = webAppName + ".war";

        Calendar startTime = Calendar.getInstance();
        while ((Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis()) < WEBAPP_DEPLOYMENT_DELAY) {
            faultyWebAppList = webAppAdminClient.getFaultyWebAppList(webAppName);
            if (faultyWebAppList.size() != 0) {
                for (String faultWebAppName : faultyWebAppList) {
                    if (webAppName.equalsIgnoreCase(faultWebAppName) || warName.equalsIgnoreCase(faultWebAppName)) {
                        log.info(webAppName + "- Web Application is faulty");
                        break;
                    }
                }
            } else {
                return Boolean.TRUE;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                log.warn(ignored.getMessage());
            }
        }
        return Boolean.FALSE;
    }

    public static void deployWebApplication(String backendURL, String sessionCookie, String webAppFilePath)
            throws RemoteException {
        log.info("Deploying web application : " + webAppFilePath);
        WebAppAdminClient webAppAdminClient = new WebAppAdminClient(backendURL, sessionCookie);
        webAppAdminClient.uploadWarFile(webAppFilePath);
    }

    public static void unDeployWebApplication(String backendURL, String hostname, String sessionCookie,
                                              String webAppFileName) throws RemoteException {
        log.info("UnDeploying web application : " + webAppFileName);
        WebAppAdminClient webAppAdminClient = new WebAppAdminClient(backendURL, sessionCookie);
        webAppAdminClient.deleteWebAppFile(webAppFileName, hostname);
    }

    public static boolean isWebAppRedeployed(String webAppName, String previousData, String endpoint) {
        log.info("waiting " + WEBAPP_DEPLOYMENT_DELAY + " millis for webApp undeployment " + webAppName);
        HttpResponse response;

        Calendar startTime = Calendar.getInstance();
        while ((Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis()) < WEBAPP_DEPLOYMENT_DELAY) {
            try {
                response = HttpRequestUtil.sendGetRequest(endpoint, null);
                if (response != null && !response.getData().isEmpty() && !response.getData().equalsIgnoreCase(previousData)) {
                    return Boolean.TRUE;
                }
            } catch (IOException e) {
                //Ignore IOExceptions
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {

            }

        }
        return Boolean.FALSE;
    }
}