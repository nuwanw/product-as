/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.wso2.appserver.integration.common.utils;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.application.mgt.stub.ApplicationAdminExceptionException;
import org.wso2.carbon.integration.common.admin.client.ApplicationAdminClient;

import java.rmi.RemoteException;
import java.util.Calendar;

public class CarAppDeploymentUtil {
    private static final Log log = LogFactory.getLog(CarAppDeploymentUtil.class);
    private static final int MAX_TIME = 5 * 60 * 1000 ;

    /**
     * This will check whether given car file is deployed successfully within the defined time period
     * @param backendURL management server url
     * @param sessionCookie user session cookie
     * @param carFileName car file name
     * @return true if car file is deployed within the defined time
     * @throws ApplicationAdminExceptionException when error occurred in service
     * @throws RemoteException when error occurred while calling the service
     */
    public static boolean isCarFileDeployed(String backendURL, String sessionCookie, String carFileName)
            throws ApplicationAdminExceptionException, RemoteException {
        ApplicationAdminClient appAdminClient =  new ApplicationAdminClient(backendURL,sessionCookie);

        log.info("waiting " + MAX_TIME + " millis for car deployment " + carFileName);
        boolean isCarFileDeployed = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) < MAX_TIME) {
            String[] applicationList = appAdminClient.listAllApplications();
            if (applicationList != null) {
                if (ArrayUtils.contains(applicationList, carFileName.replace("-", "_"))) {
                    isCarFileDeployed = true;
                    log.info("car file deployed in " + time + " mills");
                    return isCarFileDeployed;
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //ignore
            }

        }
        return isCarFileDeployed;
    }

    /**
     * This will check whether given car file is undeployed successfully within the defined time period
     * @param backendURL management server url
     * @param sessionCookie user session cookie
     * @param carFileName car file name
     * @return true if car file is undeployed within the defined time
     * @throws ApplicationAdminExceptionException when error occurred in service
     * @throws RemoteException when error occurred while calling the service
     */
    public static boolean isCarFileUnDeployed(String backendURL, String sessionCookie, String carFileName)
            throws ApplicationAdminExceptionException, RemoteException {

        log.info("waiting " + MAX_TIME + " millis for car undeployment " + carFileName);
        ApplicationAdminClient appAdminClient =  new ApplicationAdminClient(backendURL,sessionCookie);
        boolean isCarFileUnDeployed = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) < MAX_TIME) {
            String[] applicationList = appAdminClient.listAllApplications();
            if (applicationList != null) {
                if (!ArrayUtils.contains(applicationList, carFileName.replace("-", "_"))) {
                    isCarFileUnDeployed = true;
                    log.info("car file undeployed in " + time + " mills");
                    return isCarFileUnDeployed;
                }
            } else {
                //no application in app list
                return true;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //ignore
            }

        }
        return isCarFileUnDeployed;
    }
}
