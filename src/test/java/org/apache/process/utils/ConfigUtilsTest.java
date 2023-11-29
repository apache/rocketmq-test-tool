package org.apache.process.utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigUtilsTest {
    @Test
    public void testBase64Decoder() throws IOException {
        String usrHome = System.getProperty("user.home");
        String fileName = String.format("%s/.kube/config", usrHome);
        String config = new String(Files.readAllBytes(Paths.get(fileName)));

        String encoderFileName = String.format("%s/project/config_base64_new", usrHome);
        String encoderString = new String(Files.readAllBytes(Paths.get(encoderFileName)));
        Assert.assertEquals(config, new ConfigUtils().base64Decoder(encoderString));
    }
}
