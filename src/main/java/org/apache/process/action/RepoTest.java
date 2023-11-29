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


import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

@Slf4j
public class RepoTest {

    public boolean runTest(LinkedHashMap<String, Object> inputMap) throws IOException, InterruptedException {
        String namespace = inputMap.get("namespace").toString();
        String testPodName = "test-" + namespace + "-" + new Random().nextInt(100000);
        log.info("****    E2E TEST...    ****");
        log.info("namespace: {}, test pod name: {}", namespace, testPodName);

        LinkedHashMap<String, Object> containerMap = (LinkedHashMap) inputMap.get("CONTAINER");
        // build resource limits
        LinkedHashMap<String, Object> limitsResourceMap = (LinkedHashMap) containerMap.get("RESOURCE_LIMITS");
        HashMap<String, Quantity> resourcesLimits = new HashMap<>();
        for (String limitKey : limitsResourceMap.keySet()) {
            resourcesLimits.put(limitKey, new Quantity(limitsResourceMap.get(limitKey).toString()));
        }
        // build resource requests
        LinkedHashMap<String, Object> requestResourceMap = (LinkedHashMap) containerMap.get("RESOURCE_REQUIRE");
        HashMap<String, Quantity> resourceRequests = new HashMap<>();
        for (String requestKey : requestResourceMap.keySet()) {
            resourceRequests.put(requestKey, new Quantity(requestResourceMap.get(requestKey).toString()));
        }
        // biild env
        LinkedHashMap<String, Object> envMap = (LinkedHashMap) inputMap.get("ENV");
        if (!envMap.containsValue("ALL_IP") || envMap.get("ALL_IP") == null || "null".equals(envMap.get("ALL_IP"))) {
            /* get all IP */
            try (KubernetesClient client = new DefaultKubernetesClient()) {
                List<Pod> pods = client.pods().inNamespace(namespace).list().getItems();
                StringBuilder allIP = new StringBuilder();
                for (Pod pod : pods) {
                    allIP.append(pod.getMetadata().getName()).append(":").append(pod.getStatus().getPodIP()).append(",");
                }
                if (allIP.length() == 0) {
                    log.error("No pod found in current namespace: {}. Please check the namespace name and the pod name.", namespace);
                    return false;
                }
                envMap.put("ALL_IP", allIP.substring(0, allIP.length() - 1));
            }
        }
        // set env elements
        EnvVar[] envVars = new EnvVar[envMap.size()];
        int envItemIndex = 0;
        for (String envKey : envMap.keySet()) {
            envVars[envItemIndex] = new EnvVar(envKey, envMap.get(envKey).toString(), null);
            envItemIndex++;
        }

        try (final KubernetesClient client = new KubernetesClientBuilder().build()) {
            Pod pod = new PodBuilder()
                    .withApiVersion(inputMap.getOrDefault("API_VERSION", "v1").toString())
                    .withKind(inputMap.getOrDefault("KIND", "Pod").toString())
                    .withNewMetadata()
                    .withName(testPodName)
                    .withNamespace(namespace)
                    .endMetadata()
                    .withNewSpec()
                    .withRestartPolicy(inputMap.getOrDefault("RESTART_POLICY", "Never").toString())
                    .withContainers(new ContainerBuilder()
                            .withName(testPodName)
                            .withImage(containerMap.get("IMAGE").toString())
                            .withResources(new ResourceRequirementsBuilder()
                                    .withRequests(resourceRequests)
                                    .withLimits(resourcesLimits)
                                    .build()
                            )
                            .withEnv(envVars)
                            .build())
                    .endSpec()
                    .build();

            for (int retryTimes = 3; retryTimes > 0; retryTimes--) {
                try {
                    // 使用KubernetesClient创建Pod
                    client.pods().inNamespace(namespace).resource(pod).create();
                    break;
                } catch (Exception e) {
                    log.error("create pod {} failed, retry again...", testPodName);
                }
            }
        }
        return new QueryTestPod().getPodResult(testPodName, namespace, envMap.get("CODE_PATH").toString(), Integer.parseInt(envMap.getOrDefault("WAIT_TIME", "900").toString()));
    }
}
