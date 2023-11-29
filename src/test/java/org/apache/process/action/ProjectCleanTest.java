package org.apache.process.action;

import lombok.extern.slf4j.Slf4j;
import org.apache.process.config.Configs;
import org.apache.process.utils.ConfigUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

import static org.apache.process.utils.GetParam.yamlToMap;

@Slf4j
public class ProjectCleanTest {
    @Test
    public void testNacosClean() throws IOException, InterruptedException {
        String input = "action: clean\n" +
                "namespace: nacos-6196355040-2\n" +
                "ask-config: ${{ secrets.ASK_CONFIG_VIRGINA }}\n";
        String usrHome = System.getProperty("user.home");
        String fileName = String.format("%s/.kube/config", usrHome);
        String config = new String(Files.readAllBytes(Paths.get(fileName)));

        String[] authentificationInfo = new ConfigUtils().getAuthInfoFromConfig(config).split(":");
        Configs.VELAUX_USERNAME = authentificationInfo[0];
        Configs.VELAUX_PASSWORD = authentificationInfo[1];

        //new PortForward().startPortForward(Configs.VELA_NAMESPACE, Configs.VELA_POD_LABELS, Configs.PORT_FROWARD);
        LinkedHashMap<String, Object> inputMap = yamlToMap(input);
        EnvClean envClean = new EnvClean();
        Assert.assertTrue(envClean.clean(inputMap));
    }

    @Test
    public void testRocketmqClean() throws IOException, InterruptedException {
        String input = "action: clean\n" +
                "namespace: rocketmq-12345562-23315\n" +
                "ask-config: ${{ secrets.ASK_CONFIG_VIRGINA }}\n";
        String usrHome = System.getProperty("user.home");
        String fileName = String.format("%s/.kube/config", usrHome);
        String config = new String(Files.readAllBytes(Paths.get(fileName)));

        String[] authentificationInfo = new ConfigUtils().getAuthInfoFromConfig(config).split(":");
        Configs.VELAUX_USERNAME = authentificationInfo[0];
        Configs.VELAUX_PASSWORD = authentificationInfo[1];

        new PortForward().startPortForward(Configs.VELA_NAMESPACE, Configs.VELA_POD_LABELS, Configs.PORT_FROWARD);
        LinkedHashMap<String, Object> inputMap = yamlToMap(input);
        EnvClean envClean = new EnvClean();
        Assert.assertTrue(envClean.clean(inputMap));
    }
}
