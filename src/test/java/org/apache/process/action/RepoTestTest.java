package org.apache.process.action;

import org.apache.process.config.Configs;;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashMap;

import static org.apache.process.utils.GetParam.yamlToMap;

public class RepoTestTest {
    @Test
    public void testNacosRunJavaTest() throws IOException, InterruptedException {
        String input = "action: test\n" +
                "namespace: nacos-6196355040-2\n" +
                "ask-config: ${{ secrets.ASK_CONFIG_VIRGINA }}\n" +
                "API_VERSION: v1\n" +
                "KIND: Pod\n" +
                "RESTART_POLICY: \"Never\" \n" +
                "ENV:\n" +
                "  WAIT_TIME: 600\n" +
                "  REPO_NAME: Wuyunfan-BUPT/nacos-e2e\n" +
                "  CODE: https://github.com/nacos-group/nacos-e2e\n" +
                "  BRANCH: main\n" +
                "  CODE_PATH: java/nacos-2X\n" +
                "  CMD: mvn clean test -B\n" +
                "  ALL_IP: null\n" +
                "CONTAINER:\n" +
                "  IMAGE: cloudnativeofalibabacloud/test-runner:v0.0.4\n" +
                "  RESOURCE_LIMITS:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi\n" +
                "  RESOURCE_REQUIRE:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi";


        new PortForward().startPortForward(Configs.VELA_NAMESPACE, Configs.VELA_POD_LABELS, Configs.PORT_FROWARD);
        LinkedHashMap<String, Object> inputMap = yamlToMap(input);
        RepoTest project = new RepoTest();
        project.runTest(inputMap);

        LinkedHashMap<String, Object> inputMap1 = yamlToMap(input);
        GenerateReport generateReport = new GenerateReport();
        Assert.assertTrue(generateReport.generateReportMarkDown(inputMap1));

    }

    @Test
    public void testNacosRunGoTest() throws IOException, InterruptedException {
        String input = "action: test\n" +
                "namespace: nacos-6196355040-15\n" +
                "ask-config: ${{ secrets.ASK_CONFIG_VIRGINA }}\n" +
                "API_VERSION: v1\n" +
                "KIND: Pod\n" +
                "RESTART_POLICY: \"Never\" \n" +
                "ENV:\n" +
                "  WAIT_TIME: 600\n" +
                "  REPO_NAME: nacos-group/nacos-e2e\n" +
                "  CODE: https://github.com/nacos-group/nacos-e2e\n" +
                "  BRANCH: main\n" +
                "  CODE_PATH: golang\n" +
                "  CMD: |\n" +
                "    cd /root/code/golang\n" +
                "    go mod init nacos_go_test\n" +
                "    go mod tidy\n" +
                "    gotestsum --junitfile ./target/surefire-reports/TEST-report.xml ./nacosgotest\n" + //-timeout 2m  -v
                "  ALL_IP: null\n" +
                "CONTAINER:\n" +
                "  IMAGE: cloudnativeofalibabacloud/test-runner:v0.0.4\n" +
                "  RESOURCE_LIMITS:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi\n" +
                "  RESOURCE_REQUIRE:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi";

        new PortForward().startPortForward(Configs.VELA_NAMESPACE, Configs.VELA_POD_LABELS, Configs.PORT_FROWARD);
        LinkedHashMap<String, Object> inputMap = yamlToMap(input);

        RepoTest project = new RepoTest();
        project.runTest(inputMap);

        LinkedHashMap<String, Object> inputMap1 = yamlToMap(input);
        GenerateReport generateReport = new GenerateReport();
        Assert.assertTrue(generateReport.generateReportMarkDown(inputMap1));

    }

    @Test
    public void testNacosRunCsharpTest() throws IOException, InterruptedException {
        // nacos-group/nacos-e2e  main
        String input = "action: test\n" +
                "namespace: nacos-6196355040-4\n" +
                "ask-config: ${{ secrets.ASK_CONFIG_VIRGINA }}\n" +
                "API_VERSION: v1\n" +
                "KIND: Pod\n" +
                "RESTART_POLICY: \"Never\" \n" +
                "ENV:\n" +
                "  REPO_NAME: Wuyunfan-BUPT/nacos-e2e\n" +
                "  CODE: https://github.com/Wuyunfan-BUPT/nacos-e2e\n" +
                "  BRANCH: yf-dev\n" +
                "  CODE_PATH: csharp\n" +
                "  CMD: |\n" +
                "    rpm -Uvh https://packages.microsoft.com/config/centos/7/packages-microsoft-prod.rpm \n" +
                "    yum -y install dotnet-sdk-3.1 \n" +
                "    yum -y install aspnetcore-runtime-7.0 \n" +
                "    cd /root/code/csharp/nacos-csharp-sdk-test \n" +
                "    dotnet restore\n" +
                "    dotnet test --logger:\"junit;LogFilePath=../target/surefire-reports/TEST-result.xml\" \n" +
                "  ALL_IP: null\n" +
                "CONTAINER:\n" +
                "  IMAGE: cloudnativeofalibabacloud/test-runner:v0.0.4\n" +
                "  RESOURCE_LIMITS:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi\n" +
                "  RESOURCE_REQUIRE:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi";
        new PortForward().startPortForward(Configs.VELA_NAMESPACE, Configs.VELA_POD_LABELS, Configs.PORT_FROWARD);
        LinkedHashMap<String, Object> inputMap = yamlToMap(input);
        RepoTest project = new RepoTest();
        project.runTest(inputMap);

        LinkedHashMap<String, Object> inputMap1 = yamlToMap(input);
        GenerateReport generateReport = new GenerateReport();
        Assert.assertTrue(generateReport.generateReportMarkDown(inputMap1));

    }

    @Test
    public void testNacosRunCppTest() throws IOException, InterruptedException {
        String input = "action: test\n" +
                "namespace: nacos-6196355040-4\n" +
                "ask-config: ${{ secrets.ASK_CONFIG_VIRGINA }}\n" +
                "API_VERSION: v1\n" +
                "KIND: Pod\n" +
                "RESTART_POLICY: \"Never\" \n" +
                "ENV:\n" +
                "  REPO_NAME: Wuyunfan-BUPT/nacos-e2e\n" +
                "  CODE: https://github.com/Wuyunfan-BUPT/nacos-e2e\n" +
                "  BRANCH: yf-dev\n" +
                "  CODE_PATH: cpp\n" +
                "  CMD: |\n" +
                "    cd /root/code/cpp && make install\n " +
                "    echo \"export LD_LIBRARY_PATH=/usr/local/lib\" >> ~/.bashrc  && source ~/.bashrc\n" +
                "    cd /root/code/cpp/nacoscpptest \n" +
                "    g++ nacos_test.cpp -o nacos_test -lgtest -lpthread -I/usr/local/include/nacos/ -L/usr/local/lib/  -lnacos-cli \n" +
                "    chmod 777 nacos_test && ./nacos_test --gtest_output=\"xml:../target/surefire-reports/TEST-gtestresults.xml\" \n" +
                "  ALL_IP: null\n" +
                "CONTAINER:\n" +
                "  IMAGE: cloudnativeofalibabacloud/test-runner:v0.0.4\n" +
                "  RESOURCE_LIMITS:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi\n" +
                "  RESOURCE_REQUIRE:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi";

        //new PortForward().startPortForward(Configs.VELA_NAMESPACE, Configs.VELA_POD_LABELS, Configs.PORT_FROWARD);
        LinkedHashMap<String, Object> inputMap = yamlToMap(input);
        RepoTest project = new RepoTest();
        project.runTest(inputMap);

        LinkedHashMap<String, Object> inputMap1 = yamlToMap(input);
        GenerateReport generateReport = new GenerateReport();
        Assert.assertTrue(generateReport.generateReportMarkDown(inputMap1));
    }


    @Test
    public void testNacosRunPythonTest() throws IOException, InterruptedException {
        String input = "action: test\n" +
                "namespace: nacos-6196355040-4\n" +
                "ask-config: ${{ secrets.ASK_CONFIG_VIRGINA }}\n" +
                "API_VERSION: v1\n" +
                "KIND: Pod\n" +
                "RESTART_POLICY: \"Never\" \n" +
                "ENV:\n" +
                "  WAIT_TIME: 300\n" +
                "  REPO_NAME: Wuyunfan-BUPT/nacos-e2e\n" +
                "  CODE: https://github.com/Wuyunfan-BUPT/nacos-e2e\n" +
                "  BRANCH: yf-dev\n" +
                "  CODE_PATH: python\n" +
                "  CMD: |\n" +
                "    cd /root/code/python \n" +
                "    pip3 install -r requirements.txt \n" +
                "    source ~/.bashrc \n" +
                "    cd nacospythontest && pytest --junitxml ../target/surefire-reports/TEST-report.xml test/*_test.py  --log-cli-level=DEBUG\n" +//
                "  ALL_IP: null\n" +
                "CONTAINER:\n" +
                "  IMAGE: cloudnativeofalibabacloud/test-runner:v0.0.4\n" +
                "  RESOURCE_LIMITS:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi\n" +
                "  RESOURCE_REQUIRE:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi";

        //new PortForward().startPortForward(Configs.VELA_NAMESPACE, Configs.VELA_POD_LABELS, Configs.PORT_FROWARD);
        LinkedHashMap<String, Object> inputMap = yamlToMap(input);
        RepoTest project = new RepoTest();
        project.runTest(inputMap);

        LinkedHashMap<String, Object> inputMap1 = yamlToMap(input);
        GenerateReport generateReport = new GenerateReport();
        Assert.assertTrue(generateReport.generateReportMarkDown(inputMap1));
    }

    @Test
    public void testNacosRunNodejsTest() throws IOException, InterruptedException {
        String input = "action: test\n" +
                "namespace: nacos-6196355040-4\n" +
                "ask-config: ${{ secrets.ASK_CONFIG_VIRGINA }}\n" +
                "API_VERSION: v1\n" +
                "KIND: Pod\n" +
                "RESTART_POLICY: \"Never\" \n" +
                "ENV:\n" +
                "  WAIT_TIME: 600\n" +
                "  REPO_NAME: Wuyunfan-BUPT/nacos-e2e\n" +
                "  CODE: https://github.com/Wuyunfan-BUPT/nacos-e2e\n" +
                "  BRANCH: yf-dev\n" +
                "  CODE_PATH: nodejs\n" +
                "  CMD: |\n" +
                "    cd /root/code/nodejs/nacosnodejstest \n" +
                "    npm install \n" +
                "    mocha test --reporter mocha-junit-reporter --reporter-options mochaFile=../target/surefire-reports/TEST-report.xml \n" +
                "  ALL_IP: null\n" +
                "CONTAINER:\n" +
                "  IMAGE: cloudnativeofalibabacloud/test-runner:v0.0.4\n" +
                "  RESOURCE_LIMITS:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi\n" +
                "  RESOURCE_REQUIRE:\n" +
                "    cpu: 8\n" +
                "    memory: 8Gi";

        //new PortForward().startPortForward(Configs.VELA_NAMESPACE, Configs.VELA_POD_LABELS, Configs.PORT_FROWARD);
        LinkedHashMap<String, Object> inputMap = yamlToMap(input);
        RepoTest project = new RepoTest();
        project.runTest(inputMap);

        LinkedHashMap<String, Object> inputMap1 = yamlToMap(input);
        GenerateReport generateReport = new GenerateReport();
        Assert.assertTrue(generateReport.generateReportMarkDown(inputMap1));
    }

    @Test
    public void testRocketmqRunTest() throws IOException, InterruptedException {
        String input = "action: test\n" +
                "CODE_PATH: java/e2e\n" +
                "REPO_URL: https://github.com/apache/rocketmq-e2e/tree/main/java/e2e/src/test/java\n" +
                "namespace: rocketmq-12345562-23315\n" +
                "askConfig: ${{ secrets.ASK_CONFIG_VIRGINA }}\n" +
                "API_VERSION: v1\n" +
                "KIND: Pod\n" +
                "RESTART_POLICY: Never\n" +
                "ENV:\n" +
                "  CODE: https://ghproxy.com/https://github.com/apache/rocketmq-e2e\n" +
                "  BRANCH: master\n" +
                "  CODE_PATH: golang \n" +
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

        //new PortForward().startPortForward(Configs.VELA_NAMESPACE, Configs.VELA_POD_LABELS, Configs.PORT_FROWARD);
        LinkedHashMap<String, Object> inputMap = yamlToMap(input);

        RepoTest project = new RepoTest();
        Assert.assertTrue(project.runTest(inputMap));

    }

    @Test
    public void testRocketmqCsharpRunTest() throws InterruptedException, IOException {
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

        //new PortForward().startPortForward(Configs.VELA_NAMESPACE, Configs.VELA_POD_LABELS, Configs.PORT_FROWARD);

        LinkedHashMap<String, Object> inputMap = yamlToMap(input);

        RepoTest project = new RepoTest();
        Assert.assertTrue(project.runTest(inputMap));

        LinkedHashMap<String, Object> inputMap1 = yamlToMap(input);

        GenerateReport generateReport = new GenerateReport();
        Assert.assertTrue(generateReport.generateReportMarkDown(inputMap1));

    }
}
