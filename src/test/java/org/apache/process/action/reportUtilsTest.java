package org.apache.process.action;

import org.junit.Assert;
import org.junit.Test;
import java.util.LinkedHashMap;

import static org.apache.process.utils.GetParam.yamlToMap;

public class reportUtilsTest {
    @Test
    public void testReportUtils() {
        String input = "action: test\n" +
                "CODE_PATH: java/e2e\n" +
                "REPO_URL: https://github.com/apache/rocketmq-e2e/tree/main/java/e2e/src/test/java\n" +
                "namespace: rocketmq-12345562-23315\n" +
                "askConfig: ${{ secrets.ASK_CONFIG_VIRGINA }}\n" +
                "API_VERSION: v1\n" +
                "KIND: Pod\n" +
                "RESTART_POLICY: Never\n" +
                "ENV:\n" +
                "  REPO_NAME: Wuyunfan-BUPT/rocketmq-e2e\n" +
                "  CODE: https://ghproxy.com/https://github.com/Wuyunfan-BUPT/rocketmq-e2e\n" +
                "  BRANCH: master\n" +
                "  CODE_PATH: golang\n" +
                "  CMD: |\n" +
                "    cd ../common &&  mvn -Prelease -DskipTests clean package -U \n" +
                "    cd ../rocketmq-admintools && source bin/env.sh \n" +
                "    cd ../golang && go get -u github.com/apache/rocketmq-clients/golang && gotestsum --junitfile ./target/surefire-reports/TEST-report.xml ./mqgotest/... -timeout 2m  -v\n" +
                "  ALL_IP: null\n" +
                "CONTAINER:\n" +
                "  IMAGE: cloudnativeofalibabacloud/test-runner:v0.0.3\n" +
                "  RESOURCE_LIMITS:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi\n" +
                "  RESOURCE_REQUIRE:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi";

        LinkedHashMap<String, Object> inputMap = yamlToMap(input);

        GenerateReport generateReport = new GenerateReport();
       generateReport.generateReportMarkDown(inputMap);
    }

    @Test
    public void testReportUtilsNacos() {
        String input = "action: test\n" +
                "CODE_PATH: java/nacos-2X\n" +
                "namespace: rocketmq-12345562-23315\n" +
                "askConfig: ${{ secrets.ASK_CONFIG_VIRGINA }}\n" +
                "API_VERSION: v1\n" +
                "KIND: Pod\n" +
                "RESTART_POLICY: Never\n" +
                "ENV:\n" +
                "  REPO_NAME: Wuyunfan-BUPT/nacos-e2e\n" +
                "  CODE: https://ghproxy.com/https://github.com/Wuyunfan-BUPT/nacos-e2e\n" +
                "  BRANCH: main\n" +
                "  CODE_PATH: java/nacos-2X\n" +
                "  CMD: |\n" +
                "    cd ../common &&  mvn -Prelease -DskipTests clean package -U \n" +
                "    cd ../rocketmq-admintools && source bin/env.sh \n" +
                "    cd ../golang && go get -u github.com/apache/rocketmq-clients/golang && gotestsum --junitfile ./target/surefire-reports/TEST-report.xml ./mqgotest/... -timeout 2m  -v\n" +
                "  ALL_IP: null\n" +
                "CONTAINER:\n" +
                "  IMAGE: cloudnativeofalibabacloud/test-runner:v0.0.3\n" +
                "  RESOURCE_LIMITS:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi\n" +
                "  RESOURCE_REQUIRE:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi";

        LinkedHashMap<String, Object> inputMap = yamlToMap(input);

        GenerateReport generateReport = new GenerateReport();
        Assert.assertTrue(generateReport.generateReportMarkDown(inputMap));
    }

    @Test
    public void testRocketmqCsharpRunTest(){
        String input = "action: test\n" +
                "namespace: rocketmq-12345562-3\n" +
                "askConfig: ${{ secrets.ASK_CONFIG_VIRGINA }}\n" +
                "API_VERSION: v1\n" +
                "KIND: Pod\n" +
                "RESTART_POLICY: Never\n" +
                "ENV:\n" +
                "  REPO_NAME: apache/rocketmq-e2e\n" +
                "  CODE: https://ghproxy.com/https://github.com/apache/rocketmq-e2e\n" +
                "  BRANCH: master\n" +
                "  CODE_PATH: csharp \n" +
                "  CMD: |\n" +
                "    cd ../common &&  mvn -Prelease -DskipTests clean package -U\n" +
                "    cd ../rocketmq-admintools && source bin/env.sh\n" +
                "    cd ../csharp/rocketmq-client-csharp-tests/ && dotnet test --logger:\"junit;LogFilePath=../target/surefire-reports/TEST-result.xml\" -l \"console;verbosity=detailed\" \n" +
                "  ALL_IP: null\n" +
                "CONTAINER:\n" +
                "  IMAGE: cloudnativeofalibabacloud/test-runner:v0.0.4\n" +
                "  RESOURCE_LIMITS:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi\n" +
                "  RESOURCE_REQUIRE:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi";

        LinkedHashMap<String, Object> inputMap = yamlToMap(input);

        GenerateReport generateReport = new GenerateReport();
        Assert.assertTrue(generateReport.generateReportMarkDown(inputMap));
    }

    @Test
    public void testSpliHttp(){
        GenerateReport generateReport = new GenerateReport();
        String input = "https://ghproxy.com/https://github.com/apache/rocketmq-e2e";
        System.out.println(generateReport.splitHttps(input));
    }
}
