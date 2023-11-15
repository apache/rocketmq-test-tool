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

package org.apache.process.api;


import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.LocalPortForward;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.process.config.Configs;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FabricPortForward {
    public void podPortForward(String namespace, String podLabels, int localPort) {

        try (KubernetesClient client = new KubernetesClientBuilder().build()) {//.withConfig(config)
            PodList pode = client.pods().inNamespace(namespace).list();
            for (Pod p : pode.getItems()) {
                String labels = p.getMetadata().getLabels().get("app.oam.dev/name");
                if (podLabels.equals(labels)) {
                    int containerPort = p.getSpec().getContainers().get(0).getPorts().get(0).getContainerPort();
                    client.pods().inNamespace(namespace).withName(p.getMetadata().getName()).waitUntilReady(10, TimeUnit.SECONDS);

                    InetAddress inetAddress = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
                    LocalPortForward portForward = client.pods().inNamespace(namespace).withName(p.getMetadata().getName()).portForward(containerPort,
                            inetAddress, localPort);

                    log.info("Checking forwarded port......");
                    int times = 5;
                    boolean isForwarded = true;
                    while (isForwarded && times-- > 0) {
                        try {
                            new OkHttpClient()
                                    .newCall(new Request.Builder().get().url("http://127.0.0.1:" + portForward.getLocalPort()).build()).execute()
                                    .body();
                            log.info("check forwarded port success! ");
                            isForwarded = false;
                        } catch (IOException e) {
                            log.error("check forwarded port fail! retry... ");
                        }
                        ;
                    }
                    TimeUnit.MINUTES.sleep(Configs.MAX_RUN_TIME);
                    log.info("Closing forwarded port");
                    portForward.close();
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

