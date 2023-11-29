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

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.apache.process.api.AppActions;
import org.apache.process.api.AuthAction;
import org.apache.process.api.EnvActions;
import org.apache.process.utils.PrintInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EnvClean {
    public boolean clean(HashMap<String, Object> paramsMap) {
        log.info("******  Delete app and env...  ******");
        String namespace = paramsMap.get("namespace").toString();

        boolean result = true;
        /* delete vela application and namespace */
        try {
            AuthAction authAction = new AuthAction();
            authAction.setToken("login");
            AppActions appActions = new AppActions();
            appActions.deleteOAM(namespace, namespace).close();
            int times = 5;
            while (times-- > 0) {
                if (PrintInfo.isResponseSuccess(appActions.deleteApplication(namespace))) {
                    break;
                }
                TimeUnit.SECONDS.sleep(3);
                authAction.setToken("refresh_token");
            }

            if (times < 0) {
                log.warn("vela application {} delete fail!", namespace);
            } else {
                log.info("vela application {} delete success!", namespace);
            }


            EnvActions envActions = new EnvActions();
            times = 5;
            while (times-- > 0) {
                if (PrintInfo.isResponseSuccess(envActions.deleteEnv(namespace))) {
                    break;
                }
                TimeUnit.SECONDS.sleep(3);
                authAction.setToken("refresh_token");
            }

            if (times < 0) {
                log.warn("vela application {} delete fail!", namespace);
            } else {
                log.info("vela namespace {} delete success!", namespace);
            }

        } catch (Exception e) {
            log.error("Fail to delete vela application/namespace! Error message: {}", e.getMessage());
        }


        /* delete kubernetes pods and relevant namespace */
        for (int retryTimes = 6; retryTimes > 0; retryTimes--) {
            try (KubernetesClient client = new KubernetesClientBuilder().build()) {
                client.namespaces().withName(namespace).delete();
                log.info("Delete namespace {} success!", namespace);
                break;
            } catch (KubernetesClientException e) {
                log.error("Delete namespace fail! Message: {}", e.getMessage());
                result = false;
            }
        }
        return result;
    }
}
