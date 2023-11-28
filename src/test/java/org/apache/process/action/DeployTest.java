package org.apache.process.action;

import org.apache.process.config.Configs;
import org.apache.process.utils.ConfigUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.apache.process.utils.GetParam.parseDeployInput;

public class DeployTest {
    @Test
    public void testNacosClusterStartDeploy() throws IOException, InterruptedException {
        String input = "action: deploy\n" +
                "ask-config: \"${{ secrets.ASK_CONFIG_VIRGINA }}\"\n" +
                "waitTimes: 1200\n" +
                "namespace: nacos-6196355040-3\n" +
                "velaAppDescription: nacos-push_ci-123456@abcdefghij\n" +
                "repoName: nacos\n" +
                "helm:\n" +
                "  chart: ./cicd/helm\n" +
                "  git:\n" +
                "    branch: main\n" +
                "  repoType: git\n" +
                "  retries: 3\n" +
                "  url: https://github.com/nacos-group/nacos-e2e.git\n" +
                "  values:\n" +
                "    namespace: nacos-6196355040-3\n" +
                "    global:\n" +
                "      mode: cluster\n" +
                "    nacos:\n" +
                "      replicaCount: 3\n" +
                "      image:\n" +
                "        repository: wuyfeedocker/nacos-ci\n" +
                "        tag: develop-cee62800-0cb5-478f-9e42-aeb1124db716-8\n" +
                "      storage:\n" +
                "        type: mysql\n" +
                "        db:\n" +
                "          port: 3306\n" +
                "          username: nacos\n" +
                "          password: nacos\n" +
                "          param: characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useSSL=false\n" +
                "    service:\n" +
                "      nodePort: 30015\n" +
                "      type: ClusterIP";
        ;
        // read config
        String usrHome = System.getProperty("user.home");
        String fileName = String.format("%s/.kube/config", usrHome);
        String config = new String(Files.readAllBytes(Paths.get(fileName)));
        System.out.println(input);

        // set vela username and password
        String[] authInfo = new ConfigUtils().getAuthInfoFromConfig(config).split(":");
        Configs.VELAUX_USERNAME = authInfo[0];
        Configs.VELAUX_PASSWORD = authInfo[1];

        //new PortForward().startPortForward(Configs.VELA_NAMESPACE, Configs.VELA_POD_LABELS, Configs.PORT_FROWARD);
        Deploy deploy = new Deploy();
        HashMap<String, Object> deployMap = parseDeployInput(input, "helm");
        Assert.assertTrue(deploy.startDeploy(deployMap));
    }

    @Test
    public void testNacosStandaloneStartDeploy() throws InterruptedException, IOException {
        String input = "action: deploy\n" +
                "ask-config: \"${{ secrets.ASK_CONFIG_VIRGINA }}\"\n" +
                "waitTimes: 1200\n" +
                "namespace: nacos-6196355040-0\n" +
                "velaAppDescription: nacos-push_ci-123456@abcdefghij\n" +
                "repoName: nacos\n" +
                "helm:\n" +
                "  chart: ./cicd/helm\n" +
                "  git:\n" +
                "    branch: main\n" +
                "  repoType: git\n" +
                "  retries: 3\n" +
                "  url: https://github.com/nacos-group/nacos-e2e.git\n" +
                "  values:\n" +
                "    namespace: nacos-6196355040-0\n" +
                "    global:\n" +
                "      mode: standalone\n" +
                "    nacos:\n" +
                "      replicaCount: 1\n" +
                "      image:\n" +
                "        repository: wuyfeedocker/nacos-ci\n" +
                "        tag: develop-cee62800-0cb5-478f-9e42-aeb1124db716-8\n" +
                "      storage:\n" +
                "        type: embedded\n" +
                "        db:\n" +
                "          port: 3306\n" +
                "          username: nacos\n" +
                "          password: nacos\n" +
                "          param: characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useSSL=false\n" +
                "    service:\n" +
                "      nodePort: 30003\n" +
                "      type: ClusterIP";
        String usrHome = System.getProperty("user.home");
        String fileName = String.format("%s/.kube/config", usrHome);
        String config = new String(Files.readAllBytes(Paths.get(fileName)));

        String authentificationInfo[] = new ConfigUtils().getAuthInfoFromConfig(config).split(":");
        Configs.VELAUX_USERNAME = authentificationInfo[0];
        Configs.VELAUX_PASSWORD = authentificationInfo[1];

        //new PortForward().startPortForward(Configs.VELA_NAMESPACE, Configs.VELA_POD_LABELS, Configs.PORT_FROWARD);
        Deploy deploy = new Deploy();
        HashMap<String, Object> deployMap = parseDeployInput(input, "helm");
        Assert.assertTrue(deploy.startDeploy(deployMap));

    }

    @Test
    public void testRocketmqStartDeploy() throws InterruptedException, IOException {
        String input = "action: deploy\n" +
                "ask-config: \"${{ secrets.ASK_CONFIG_VIRGINA }}\"\n" +
                "waitTimes: 1200\n" +
                "namespace: rocketmq-12345562-3\n" +
                "velaAppDescription: rocketmq-push_ci-123456@abcdefghij\n" +
                "repoName: rocketmq\n" +
                "helm:\n" +
                "  chart: ./rocketmq-k8s-helm\n" +
                "  git:\n" +
                "    branch: master\n" +
                "  repoType: git\n" +
                "  retries: 3\n" +
                "  url: https://ghproxy.com/https://github.com/apache/rocketmq-docker.git\n" +
                "  values:\n" +
                "    nameserver:\n" +
                "      image:\n" +
                "        repository: wangtong719/ci-test\n" +
                "        tag: test2-fd2c5471-bc7a-4388-a6d5-41501e29cfa8-ubuntu\n" +
                "    broker:\n" +
                "      image:\n" +
                "        repository: wangtong719/ci-test\n" +
                "        tag: test2-fd2c5471-bc7a-4388-a6d5-41501e29cfa8-ubuntu\n" +
                "    proxy:\n" +
                "      image:\n" +
                "        repository: wangtong719/ci-test\n" +
                "        tag: test2-fd2c5471-bc7a-4388-a6d5-41501e29cfa8-ubuntu\n";

        String usrHome = System.getProperty("user.home");
        String fileName = String.format("%s/.kube/config", usrHome);
        String config = new String(Files.readAllBytes(Paths.get(fileName)));

        ConfigUtils configUtils = new ConfigUtils();
        String[] authentificationInfo = configUtils.getAuthInfoFromConfig(config).split(":");
        Configs.VELAUX_USERNAME = authentificationInfo[0];
        Configs.VELAUX_PASSWORD = authentificationInfo[1];

        new PortForward().startPortForward(Configs.VELA_NAMESPACE, Configs.VELA_POD_LABELS, Configs.PORT_FROWARD);
        Deploy deploy = new Deploy();
        HashMap<String, Object> deployMap = parseDeployInput(input, "helm");
        Assert.assertTrue(deploy.startDeploy(deployMap));

    }

}
