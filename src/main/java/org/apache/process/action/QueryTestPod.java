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
import lombok.extern.slf4j.Slf4j;
import org.apache.process.api.ExecuteCMD;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Slf4j
public class QueryTestPod {
    /**
     * get dictionary and file about test result from pod.
     *
     * @param testPodName  test pod name.
     * @param namespace    pod namespace.
     * @param testCodePath test code path.
     * @return boolean.
     * @throws IOException          io exception.
     * @throws InterruptedException interrupt exception.
     */
    public boolean getPodResult(String testPodName, String namespace, String testCodePath, int waitTimes) throws IOException, InterruptedException {
        log.info("****   query status and get result   ****");

        String podStatus = null;
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            podStatus = client.pods().inNamespace(namespace).withName(testPodName).get().getStatus().getPhase();
        } catch (Exception ignored) {
            log.warn("PodStatus set Pending..");
        }

        if (podStatus == null) {
            podStatus = "Pending";
        }
        // mark if program has been executed.
        boolean isWaitingTest = true;
        LocalDateTime startTime = LocalDateTime.now();
        while ("Pending".equals(podStatus) || "Running".equals(podStatus) || isWaitingTest) {
            TimeUnit.SECONDS.sleep(5);
            try (KubernetesClient client = new KubernetesClientBuilder().build()) {
                podStatus = client.pods().inNamespace(namespace).withName(testPodName).get().getStatus().getPhase();
            } catch (Exception e) {
                log.warn("Query pod fail! Errormessage: {}. Retry again...", e.getMessage());
            }

            if (podStatus == null) {
                podStatus = "Pending";
            }
            if (isWaitingTest) {
                log.info("Waiting for {} test done...", testPodName);
            } else {
                log.info("Current pod status is {}, waiting pod stop...", podStatus);
            }

            // Check if the execution of the test program has ended.
            if (isWaitingTest) {
                String cmdOutput = null;
                try (ExecuteCMD executeCMD = new ExecuteCMD()) {
                    cmdOutput = executeCMD.execCommandOnPod(testPodName, namespace, "/bin/bash", "-c", "ls /root | grep testdone\n");
                } catch (Exception e) {
                    log.warn("Query error! Error message: %s. Continue to query..." + e.getMessage());
                }

                boolean isTimeout = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now()) >= waitTimes;
                if (isTimeout) {
                    log.info("Current pod timeout! Stop pod...");
                }

                // if the test program ends, get the result.
                if ((cmdOutput != null && cmdOutput.contains("testdone")) || isTimeout) {
                    log.info("test done !");
                    isWaitingTest = false;

                    Path filePath = Paths.get("testlog.txt");
                    if (!Files.exists(filePath)) {
                        Files.createFile(filePath);
                    }
                    int downloaTimes = 5;
                    while (downloaTimes-- > 0) {
                        if (downloadFile(namespace, testPodName, testPodName, "/root/testlog.txt", filePath)) {
                            break;
                        }
                    }

                    Path dirPath = Paths.get("test_report");
                    if (!Files.exists(dirPath)) {
                        Files.createDirectory(dirPath);
                    }
                    downloaTimes = 5;
                    while (downloaTimes-- > 0) {
                        if (downloadDir(namespace, testPodName, testPodName, String.format("/root/code/%s/target/surefire-reports", testCodePath), dirPath)) {
                            break;
                        }
                    }
                }

                if (isTimeout) {
                    podStatus = "Failed";
                    break;
                }
            }
        }

//        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
//            client.pods().inNamespace(namespace).withName(testPodName).delete();
//            log.info("Delete test pod: {} success !", testPodName);
//        } catch (Exception e) {
//            log.warn("Delete test pod {} error !", testPodName);
//        }

        log.info("Test status: " + podStatus);
        return !"Failed".equals(podStatus);
    }

    /**
     * download file from pod.
     *
     * @param namespace     pod namespace.
     * @param podName       pod name.
     * @param containerName pod's container name.
     * @param srcPath       file path in pod.
     * @param targetPath    target file path.
     * @return boolean.
     */
    public boolean downloadFile(String namespace, String podName, String containerName, String srcPath, Path targetPath) {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            client.pods().inNamespace(namespace).withName(podName).inContainer(containerName).file(srcPath).copy(targetPath);
            log.info("File({}) copied successfully!", srcPath);
            return true;
        } catch (Exception e) {
            log.error("Fail to get {}! Error message: {}", srcPath, e.getMessage());
            return false;
        }
    }

    /**
     * download dictionary from pod.
     *
     * @param namespace     pod namespace.
     * @param podName       pod name.
     * @param containerName pod's container name.
     * @param srcPath       dictionary path in pod
     * @param tarPath       target dictionary path.
     * @return boolean.
     */
    public boolean downloadDir(String namespace, String podName, String containerName, String srcPath, Path tarPath) {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            client.pods().inNamespace(namespace).withName(podName).inContainer(containerName).dir(srcPath).copy(tarPath);
            TimeUnit.SECONDS.sleep(2);
            client.close();
            String xmlPath = tarPath + "/" + srcPath;
            File filePath = new File(xmlPath);
            String[] files = filePath.list((dir, name) -> {
                return name.endsWith(".xml");
            });
            if (files != null) {
                log.info("Directory({}) copied successfully!", srcPath);
                return true;
            } else {
                log.warn("Directory({}) copied fail! ", srcPath);
                return false;
            }

        } catch (Exception e) {
            log.error("Fail to get {}! Error message: {}", srcPath, e.getMessage());
            return false;
        }
    }

}
