/*
 * #
 * # Licensed to the Apache Software Foundation (ASF) under one or more
 * # contributor license agreements.  See the NOTICE file distributed with
 * # this work for additional information regarding copyright ownership.
 * # The ASF licenses this file to You under the Apache License, Version 2.0
 * # (the "License"); you may not use this file except in compliance with
 * # the License.  You may obtain a copy of the License at
 * #
 * #     http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 * #
 */

package org.apache.process.action;

import okhttp3.Response;
import org.apache.process.api.AppActions;
import org.apache.process.api.AuthAction;
import org.apache.process.api.EnvActions;
import org.apache.process.config.Configs;
import org.apache.process.model.Deploymodel;
import org.apache.process.utils.PrintInfo;
import org.json.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Deploy {
    public boolean startDeploy(HashMap<String, Object> paramsMap) {
        log.info("****  Create namespace and deploy...  ****");

        AuthAction authAction = new AuthAction();
        authAction.setToken("login");

        String namespace = paramsMap.get("namespace").toString();
        log.info("Generate namespace {}", namespace);

        EnvActions envActions = new EnvActions();
        String envBodyContent = String.format(Deploymodel.ENV_BODY, namespace, namespace, Configs.PROJECT_NAME, namespace);
        try (Response response = envActions.createEnv(envBodyContent);) {
            PrintInfo.printRocketInfo(response, String.format("Generate namespace(%s) success!", namespace));
        } catch (Exception e) {
            log.error("Create env Error! Message: {}", e.getMessage());
            return false;
        }

        log.info("Generate {} Application", namespace);
        AppActions appActions = new AppActions();

        authAction.setToken("refresh_token");
        String componentProperty = paramsMap.get("helm").toString();
        String bodyContent = String.format(Deploymodel.APPLICATION_BODY_COMPONENT, namespace, Configs.PROJECT_NAME, paramsMap.get("velaAppDescription"), namespace, namespace, paramsMap.get("repoName"), componentProperty);
        try (Response createAppResponse = appActions.createApplication(bodyContent)) {
            PrintInfo.printRocketInfo(createAppResponse, String.format(String.format("Generate %s Application success!", namespace)));
        } catch (Exception e) {
            log.error("Create application Error! Message: {}", e.getMessage());
            return false;
        }

        log.info("deploy {} Application", namespace);

        String workflowName = "workflow-" + namespace;
        String deployBodyContent = String.format(Deploymodel.DEPLOY_APP_BODY, workflowName);
        authAction.setToken("refresh_token");
        try (Response response = appActions.deployOrUpgradeApplication(namespace, deployBodyContent);) {
            if (!PrintInfo.printRocketInfo(response, String.format("Deploy %s Application success!", namespace))) {
                return false;
            }
        } catch (Exception e) {
            log.error("Deploy application Error! Message: {}", e.getMessage());
            return false;
        }

        log.info("Query pod {} Application status", namespace);
        int waitTimes = Integer.parseInt(paramsMap.get("waitTimes").toString());
        LocalDateTime startTime = LocalDateTime.now();
        Response response = null;

        while (ChronoUnit.SECONDS.between(startTime, LocalDateTime.now()) <= waitTimes) {
            try {
                authAction.setToken("refresh_token");
                response = appActions.getApplicationStatus(namespace, namespace);
                JSONObject json;
                if (response.body() != null) {
                    json = new JSONObject(response.body().string());
                    response.close();
                } else {
                    response.close();
                    continue;
                }

                String workflowsStatus = json.getJSONObject("status").getJSONObject("workflow").getString("status");
                String message = new JSONObject(json.getJSONObject("status").getJSONArray("services").get(0).toString()).getString("message");

                if ("succeeded".equals(workflowsStatus)) {
                    log.info("Success! Message: " + message);
                    break;
                } else if ("executing".equals(workflowsStatus)) {
                    log.info("Waiting... Message : " + message);
                    TimeUnit.SECONDS.sleep(5);
                } else {
                    log.error("Fail! Message: {}", message);
                    return false;
                }
            } catch (Exception e) {
                log.error("Error! Message: {}", e.getMessage());
            }
        }
        if (response != null) {
            response.close();
        }

        if (ChronoUnit.SECONDS.between(startTime, LocalDateTime.now()) > waitTimes) {
            log.error("Error! Deploy timeout !");
            return false;
        }
        return true;
    }
}
