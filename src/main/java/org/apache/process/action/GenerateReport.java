package org.apache.process.action;

import lombok.extern.slf4j.Slf4j;
import org.apache.process.report_utils.testcase.TaskResult;
import org.apache.process.report_utils.testcase.xUnitTestResultParser;
import org.apache.process.utils.ConfigUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
public class GenerateReport {
    /**
     * genarate markdown by test xml report.
     *
     * @param inputMap params map.
     * @return boolean.
     */
    public boolean generateReportMarkDown(LinkedHashMap<String, Object> inputMap) {
        LinkedHashMap<String, Object> envMap = (LinkedHashMap) inputMap.get("ENV");
        // set test code base url.
        String repoBaseUrl = splitHttps(envMap.get("CODE").toString()) + "/tree/" + envMap.get("BRANCH").toString() + "/" + envMap.get("CODE_PATH").toString();
        // test-report path.
        String xmlPath = String.format("test_report/root/code/%s/target/surefire-reports", envMap.get("CODE_PATH").toString());
        List<File> fileList = new ArrayList<>();
        File filePath = new File(xmlPath);
        // filter .xml format files.
        String[] files = filePath.list((dir, name) -> {
            return name.endsWith(".xml");
        });
        if (files != null) {
            for (String file : files) {
                fileList.add(new File(xmlPath + "/" + file));
            }
        } else {
            log.error("xml files unfounded!");
            return false;
        }
        // parse test files.
        xUnitTestResultParser parser = new xUnitTestResultParser();
        TaskResult res = parser.parseTestResult(fileList);
        File f = new File("result.md");
        FileWriter fw;
        try {
            fw = new FileWriter(f);
            String githubToken = "";
            if (envMap.containsKey("GITHUB_TOKEN")) {
                githubToken = "token " + new ConfigUtils().base64Decoder(envMap.get("GITHUB_TOKEN").toString().replace("\\n", ""));
            }
            String str = res.toMarkdown(envMap.get("REPO_NAME").toString(), repoBaseUrl, envMap.get("BRANCH").toString(), envMap.get("CODE_PATH").toString(), githubToken.replace("\n", ""));
            fw.write(str);
            fw.close();
            log.info("Generate report success!");
            return true;
        } catch (IOException e) {
            log.error("Fail to generate report! Error message: {}", e.getMessage());
            return false;
        }
    }

    /**
     * if test code url contains proxy address, remove it.
     *
     * @param url test code url
     * @return test code url which hasn't proxy address.
     */
    public String splitHttps(String url) {
        String[] httpUrl = url.split("https://");
        if (httpUrl.length == 1) {
            return httpUrl[0];
        } else {
            return "https://" + httpUrl[httpUrl.length - 1];
        }
    }
}
