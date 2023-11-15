package org.apache.process.report_utils.testcase;

import org.apache.process.report_utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by wangtong.wt on 2017/3/20.
 *
 * @author wangtong.wt
 * @date 2017/03/20
 */
@Slf4j
public class xUnitTestResultParser implements TestResultParser {

    protected TaskResult taskResult = new TaskResult();

    protected Set<String> parsedFiles = new HashSet<>();


    /**
     * parse the test result by test file.
     */
    @Override
    public TaskResult parseTestResult(List<File> files) {
        List<File> targetFiles = new ArrayList<>();
        //过滤测试结果文件
        files = files.stream().filter(file -> {
            return file.getName().matches(".*xml");
        }).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(files)) {
            return null;
        }

        for (File file : files) {
            // since the case of junit5 retrying the error will cover the report, it is judged according to the file md5.
            String fileMd5 = FileUtils.getFileMd5(file);
            if (StringUtils.isNotBlank(fileMd5) && !parsedFiles.contains(fileMd5)) {
                // determine whether the file has ended, and parse it if it has ended.
                if (StringUtils.contains(getLastLineFromFile(file.getAbsolutePath()), "</testsuites>") ||
                        StringUtils.contains(getLastLineFromFile(file.getAbsolutePath()), "</testsuite>")) {
                    parsedFiles.add(fileMd5);
                    targetFiles.add(file);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(targetFiles)) {
            parseTaskResult(targetFiles, taskResult);
        }

        return taskResult;

    }

    protected void parseTaskResult(List<File> files, TaskResult taskResult) {
        for (File file : files) {
            parseFileResult(file, taskResult);
        }
    }

    // parse a file.
    private void parseFileResult(File file, TaskResult taskResult) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(Files.newInputStream(file.toPath()));
                    Element testSuitesElement = document.getDocumentElement();
                    List<Element> testSuitElements = DomUtils.getChildElementsByTagName(testSuitesElement, "testsuite");
                    if (testSuitElements.isEmpty()) {
                        parseTestClass(testSuitesElement, taskResult);
                    } else {
                        for (Element testSuitElement : testSuitElements) {
                            parseTestClass(testSuitElement, taskResult);
                        }
                    }
        } catch (Exception e) {
            log.error(String.format("Parsing XML File[%s] failed!", file.getAbsoluteFile()), e);
        }
    }

    // parse test class.
    private void parseTestClass(Element testSuiteElement, TaskResult taskResult) {
        if (testSuiteElement.hasAttribute("time")) {
            taskResult.addCostTime(Double.parseDouble(testSuiteElement.getAttribute("time").replace(",", "").trim()));
        }

        String className;
        List<Element> testCaseElements = DomUtils.getChildElementsByTagName(testSuiteElement, "testcase");

        if (CollectionUtils.isNotEmpty(testCaseElements)) {
            for (Element testCaseElement : testCaseElements) {
                String methodName = testCaseElement.getAttribute("name");
                // case display name
                String caseDisplayName = testCaseElement.getAttribute("caseName");
                className = testCaseElement.getAttribute("classname");
                double time = Double.parseDouble(testCaseElement.getAttribute("time").replace(",", "").trim());

                if (!parseTestMethod(className, methodName, caseDisplayName, time, CaseResult.CASE_RESULT_FAILURE,
                    taskResult,
                    testCaseElement)) {
                    if (!parseTestMethod(className, methodName, caseDisplayName, time, CaseResult.CASE_RESULT_ERROR,
                        taskResult,
                        testCaseElement)) {
                        if (!parseTestMethod(className, methodName, caseDisplayName, time,
                            CaseResult.CASE_RESULT_SKIPPED, taskResult,
                            testCaseElement)) {
                            parseTestMethod(className, methodName, caseDisplayName, time,
                                CaseResult.CASE_RESULT_SUCCESS, taskResult,
                                testCaseElement);
                        }
                    }
                }
            }
        }
    }

    protected boolean parseTestMethod(String className, String methodName, String caseDisplayName, double time,
                                    String type,
                                    TaskResult taskResult, Element testCaseElement) {
        boolean done = false;
        if (StringUtils.equals(type, CaseResult.CASE_RESULT_SUCCESS)) {
            taskResult.addCase(new CaseResult(className, methodName, caseDisplayName, time, type, ""));
            done = true;
        } else {
            Element typedElement = DomUtils.getChildElementByTagName(testCaseElement, type);

            if (StringUtils.isEmpty(testCaseElement.getAttribute("status"))){
                if (typedElement != null) {
                    String content = "";
                    if (StringUtils.equals(type, CaseResult.CASE_RESULT_FAILURE) || StringUtils.equals(type,
                            CaseResult.CASE_RESULT_ERROR)) {
                        content = typedElement.getFirstChild().getNodeValue();
                        String sysoutLog = getSysoutInfo(testCaseElement);
                        if (StringUtils.length(sysoutLog) > 1024*1024) {
                            sysoutLog = StringUtils.substring(sysoutLog, 0, 1024*1024) + "\r\n";
                            sysoutLog += "System-out log over 1024 length, check it in run log......";
                        }
                        taskResult.addCase(
                                new CaseResult(className, methodName, caseDisplayName, time, type, content, sysoutLog));
                    } else if (StringUtils.equals(type, CaseResult.CASE_RESULT_SKIPPED)) {
                        taskResult.addCase(new CaseResult(className, methodName, caseDisplayName, time, type, content));
                    }
                    done = true;
                }
            } else {
                if (typedElement != null) {
                    String content = "";
                    if (StringUtils.equals(type, CaseResult.CASE_RESULT_FAILURE) || StringUtils.equals(type,
                            CaseResult.CASE_RESULT_ERROR)) {
                        content = typedElement.getAttribute("message");
                        String sysoutLog = typedElement.getFirstChild().getNodeValue();
                        if (StringUtils.length(sysoutLog) > 1024*1024) {
                            sysoutLog = StringUtils.substring(sysoutLog, 0, 1024*1024) + "\r\n";
                            sysoutLog += "System-out log over 1024 length, check it in run log......";
                        }
                        taskResult.addCase(
                                new CaseResult(className, methodName, caseDisplayName, time, type, content, sysoutLog));
                    }
                    done = true;
                }
                if (StringUtils.equals(type, CaseResult.CASE_RESULT_SKIPPED)) {
                    if (testCaseElement.getAttribute("status").equals("notrun")) {
                        taskResult.addCase(new CaseResult(className, methodName, caseDisplayName, time, type, ""));
                        done = true;
                    }
                }
            }
        }
        return done;
    }

    private String getSysoutInfo(Element testcaseElement) {
        StringBuilder sb = new StringBuilder();
        try {
            Element sysoutElement = DomUtils.getChildElementByTagName(testcaseElement, "system-out");
            if (sysoutElement != null) {
                String systemOut = new String(sysoutElement.getFirstChild().getNodeValue().getBytes(),
                    StandardCharsets.UTF_8);
                sb.append(systemOut).append("\r\n");
            }
            Element syserrElement = DomUtils.getChildElementByTagName(testcaseElement, "system-err");
            if (syserrElement != null) {
                sb.append(new String(syserrElement.getFirstChild().getNodeValue().getBytes(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.error("JunitTestResultParser getSysoutInfo exception:" + ExceptionUtils.getStackTrace(e));
        }
        return sb.toString();
    }

    protected String getLastLineFromFile(String file) {
        try {
            List<String> lines = IOUtils.readLines(Files.newInputStream(Paths.get(file)));
            if (CollectionUtils.isNotEmpty(lines)) {
                return lines.get(lines.size() - 1);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "";
    }
}
