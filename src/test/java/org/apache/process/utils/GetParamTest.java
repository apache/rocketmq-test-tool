package org.apache.process.utils;

import junit.framework.Assert;
import org.apache.process.model.Deploymodel;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;

import static org.apache.process.utils.GetParam.*;

public class GetParamTest {
    @Test
    public void testyamlToMap() {
        String input = "action: test\n" +
                "namespace: nacos-12345562-2331\n" +
                "ask-config: \"${{ secrets.ASK_CONFIG_VIRGINA }}\"\n" +
                "API_VERSION: \"v1\"\n" +
                "KIND: \"Pod\"\n" +
                "RESTART_POLICY: \"Never\" \n" +
                "ENV:\n" +
                "  CODE: https://github.com/nacos-group/nacos-e2e.git\n" +
                "  BRANCH: master\n" +
                "  CODE_PATH: java/nacos-2X\n" +
                "  CMD: mvn clean test -B -Dnacos.client.version=2.2.3\n" +
                "  ALL_IP: null\n" +
                "CONTAINER:\n" +
                "  IMAGE: \"cloudnativeofalibabacloud/test-runner:v0.0.1\"\n" +
                "  RESOURCE_LIMITS:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi\n" +
                "  RESOURCE_REQUIRE:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi";

        LinkedHashMap<String, Object> paramsMap = yamlToMap(input);
        Assert.assertEquals(paramsMap.get("action"), "test");
        Assert.assertEquals(paramsMap.get("namespace"), "nacos-12345562-2331");
        Assert.assertEquals(paramsMap.get("ask-config"), "${{ secrets.ASK_CONFIG_VIRGINA }}");
        Assert.assertEquals(paramsMap.get("API_VERSION"), "v1");
        Assert.assertEquals(paramsMap.get("KIND"), "Pod");
        Assert.assertEquals(paramsMap.get("RESTART_POLICY"), "Never");


        LinkedHashMap<String, Object> envMap = (LinkedHashMap) paramsMap.get("ENV");
        Assert.assertEquals(envMap.get("CODE"), "https://github.com/nacos-group/nacos-e2e.git");
        Assert.assertEquals(envMap.get("BRANCH"), "master");
        Assert.assertEquals(envMap.get("CODE_PATH"), "java/nacos-2X");
        Assert.assertEquals(envMap.get("CMD"), "mvn clean test -B");
        Assert.assertNull(envMap.get("ALL_IP"));


        LinkedHashMap<String, Object> containerMap = (LinkedHashMap) paramsMap.get("CONTAINER");
        Assert.assertEquals(containerMap.get("IMAGE"), "cloudnativeofalibabacloud/test-runner:v0.0.1");

        LinkedHashMap<String, Object> limitsMap = (LinkedHashMap) containerMap.get("RESOURCE_LIMITS");
        Assert.assertEquals(limitsMap.get("cpu").toString(), "8");
        Assert.assertEquals(limitsMap.get("memory"), "8Gi");

        LinkedHashMap<String, Object> requireMap = (LinkedHashMap) containerMap.get("RESOURCE_REQUIRE");
        Assert.assertEquals(requireMap.get("cpu").toString(), "8");
        Assert.assertEquals(requireMap.get("memory"), "8Gi");
    }

    @Test
    public void testParseDeployInput() {
        String helmvalues =
                "global:\n" +
                        "  mode: standalone\n" +
                        "nacos:\n" +
                        "  replicaCount: 1\n" +
                        "  image: \n" +
                        "    repository: wuyfeedocker/nacos-ci\n" +
                        "    tag: develop-4f26def4-ccb0-45e5-9989-874e78424bea-8\n" +
                        "  storage:\n" +
                        "    type: embedded\n" +
                        "    db:\n" +
                        "      port: 3306\n" +
                        "      username: nacos\n" +
                        "      password: nacos\n" +
                        "      param: characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useSSL=false\n" +
                        "service:\n" +
                        "  nodePort: 30009\n" +
                        "  type: ClusterIP";
        String params = "action: deploy\n" +
                "namespace: nacos-12345562-2331\n" +
                "velaUsername: *****\n" +
                "velaPassword: ******\n" +
                "askConfig: ${{ secrets.ASK_CONFIG_VIRGINA }}\n" +
                "helm:\n" +
                "  chart: ./helm\n" +
                "  git:\n" +
                "    branch: master\n" +
                "  repoType: git\n" +
                "  retries: 3\n" +
                "  url: https://ghproxy.com/https://github.com/apache/rocketmq-e2e.git\n" +
                "  values:\n" +
                "    global:\n" +
                "      mode: standalone\n" +
                "    nacos:\n" +
                "      replicaCount: 1\n" +
                "      image: \n" +
                "        repository: wuyfeedocker/nacos-ci\n" +
                "        tag: develop-4f26def4-ccb0-45e5-9989-874e78424bea-8\n" +
                "      storage:\n" +
                "        type: embedded\n" +
                "        db:\n" +
                "          port: 3306\n" +
                "          username: nacos\n" +
                "          password: nacos\n" +
                "          param: characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useSSL=false\n" +
                "    service:\n" +
                "      nodePort: 30009\n" +
                "      type: ClusterIP";
        String target = "helm";
        HashMap<String, Object> paramsMap = parseDeployInput(params, target);
        Assert.assertEquals(paramsMap.get("action"), "deploy");
        Assert.assertEquals(paramsMap.get("namespace"), "nacos-12345562-2331");
        Assert.assertEquals(paramsMap.get("velaUsername"), "****");
        Assert.assertEquals(paramsMap.get("velaPassword"), "*****");
        Assert.assertEquals(paramsMap.get("askConfig"), "${{ secrets.ASK_CONFIG_VIRGINA }}");

        Assert.assertEquals(Deploymodel.generateComponentProperties(helmvalues, "./helm", "master", "https://ghproxy.com/https://github.com/apache/rocketmq-e2e.git").length(), paramsMap.get("helm").toString().length());
    }
}
