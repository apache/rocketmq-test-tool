package org.apache.process;

import org.apache.process.report_utils.GetGithubRepoInfo;
import org.apache.process.report_utils.RepoFileInfo;
import org.apache.process.utils.ConfigUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

public class GetGithubRepoInfoTest {

    @Test
    public void testGetAllFilePath() {
        GetGithubRepoInfo client = new GetGithubRepoInfo();
        HashMap<String, RepoFileInfo> map = new HashMap<>();
        String url = "https://api.github.com/repos/apache/rocketmq-e2e/contents/java/e2e";
        String gitBranch = "master";
        String githubToken = "";

        client.getAllFilePath(url, gitBranch, githubToken, map);
        for (String key : map.keySet()) {
            System.out.println(key + ": " + map.get(key).getFileUrl());
        }
        for (String key : map.keySet()) {
            System.out.println(key + ": " + map.get(key).getFileName());
        }
        for (String key : map.keySet()) {
            System.out.println(key + ": " + map.get(key).getSuffix());
        }
    }

    @Test
    public void testGetFunctionRowFromFile() throws IOException {
        GetGithubRepoInfo client = new GetGithubRepoInfo();
//        String url = "https://api.github.com/repos/apache/rocketmq-e2e/contents/csharp/rocketmq-client-csharp-tests/test/BaseTest.cs";
//        String keyword = "BaseTest";
        String className = "FifoMsgTest";
        String url = "https://api.github.com/repos/apache/rocketmq-e2e/contents/csharp/rocketmq-client-csharp-tests/test/FifoMsgTest.cs";
        String keyword = "TestSendFifoMsgSyncSimpleConsumerRecv";
        String githubToken = "";
        HashMap<String, RepoFileInfo> fileInfoMap = new HashMap<>();
        RepoFileInfo repoFileInfo = new RepoFileInfo();
        repoFileInfo.setFileUrl(url);
        repoFileInfo.setFileName("FifoMsgTest.cs");
        fileInfoMap.put(className, repoFileInfo);
        Assert.assertEquals(client.getFunctionRowFromFile(className, url, keyword, githubToken, fileInfoMap), 40);
    }

    @Test
    public void testGetCaseUrl() throws IOException, InterruptedException {
        GetGithubRepoInfo client = new GetGithubRepoInfo();
        HashMap<String, RepoFileInfo> golangMap = new HashMap<>();

        String gitBranch = "master";
        String repo = "apache/rocketmq-e2e";
        String githubToken = "";

        // go test
        String url = "https://api.github.com/repos/apache/rocketmq-e2e/contents/golang";
        client.getAllFilePath(url, gitBranch, githubToken, golangMap);

        String completeUrlGo = client.getCaseUrl(golangMap, githubToken, "delay", "TestDelayMsgAsync", repo, gitBranch);
        Assert.assertEquals(completeUrlGo, "https://github.com/apache/rocketmq-e2e/blob/master/golang/mqgotest/delay/delaymsg_test.go#L77");
        Thread.sleep(20000);

        // csharp test
        HashMap<String, RepoFileInfo> csharpMap = new HashMap<>();
        String csharpUrl = "https://api.github.com/repos/apache/rocketmq-e2e/contents/csharp";
        client.getAllFilePath(csharpUrl, gitBranch, githubToken, csharpMap);
        String completeUrlCsharp = client.getCaseUrl(csharpMap, githubToken, "FifoMsgTest.cs", "TestSendFifoMsgSyncSimpleConsumerRecv", repo, gitBranch);
        Assert.assertEquals(completeUrlCsharp, "https://github.com/apache/rocketmq-e2e/blob/master/csharp/rocketmq-client-csharp-tests/test/FifoMsgTest.cs#L40");
        Thread.sleep(60000);

        // java test
        HashMap<String, RepoFileInfo> javaMap = new HashMap<>();
        String javaUrl = "https://api.github.com/repos/apache/rocketmq-e2e/contents/java/e2e";
        client.getAllFilePath(javaUrl, gitBranch, githubToken, javaMap);
        String completeUrlJava = client.getCaseUrl(javaMap, githubToken, "ConsumerGroupTest.java", "testSystemInnerConsumerGroup", repo, gitBranch);
        Assert.assertEquals(completeUrlJava, "https://github.com/apache/rocketmq-e2e/blob/master/java/e2e/src/test/java/org/apache/rocketmq/broker/client/consumer/ConsumerGroupTest.java#L65");

    }

    @Test
    public void testGetUsernameFromConfig() {
        ConfigUtils configUtils = new ConfigUtils();
        String config = "wdjweiygduyegdfi3grqwqw8343y9f73yf7wdfwe93gf93f0fu0wwqeqwefveoirvh3984hv934hv348yv3489yvh34urfhc9347yfh394hv==";
        String result[] = configUtils.getAuthInfoFromConfig(config).split(":");
        Assert.assertEquals("vhhfyc", result[0]);
        Assert.assertEquals("v4h7c4h9v8v4", result[1]);

        String config1 = "3dqwdqwdg23dqwdqwdqwfg2qwdqeq38fg23ytyf238Efg238fKt32L87fds3sdA8fB32Cf7X3tA==";
        String result1[] = configUtils.getAuthInfoFromConfig(config1).split(":");
        Assert.assertEquals("atxfcb", result1[0]);
        Assert.assertEquals("A3X7f2B8A3s7", result1[1]);

        String config2 = "3gqewqeqweqewqe23dfg2qweqweq38eqwfg23ytyf238Efg238fKt32L87fds3sdA8fB32Cf7X3tA5==";
        String result2[] = configUtils.getAuthInfoFromConfig(config2).split(":");
        Assert.assertEquals("atxfcb", result2[0]);
        Assert.assertEquals("A3X7f2B8A3s7", result2[1]);
    }
}
