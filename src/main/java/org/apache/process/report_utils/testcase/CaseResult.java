package org.apache.process.report_utils.testcase;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by wangtong.wt on 2017/3/20.
 *
 * @author wangtong.wt
 * @date 2017/03/20
 */
@Data
public class CaseResult {

    public static final String CASE_RESULT_SUCCESS = "success";

    public static final String CASE_RESULT_FAILURE = "failure";

    public static final String CASE_RESULT_ERROR = "error";

    public static final String CASE_RESULT_SKIPPED = "skipped";

    private String className;

    private String methodName;

    private String caseDisplayName;

    private String result;

    private double time;

    private String simpleClassName;

    private String detailInfo;

    private String sysoutLog;

    public CaseResult(String className, String methodName, double time, String result, String detailInfo) {
        this(className, methodName, null, time, result, detailInfo);
    }

    public CaseResult(String className, String methodName, String caseDisplayName, double time, String result,
                      String detailInfo) {
        this.className = className;
        this.methodName = filterContent(methodName);
        this.time = time;
        this.result = result;
        this.detailInfo = filterContent(detailInfo);
        this.simpleClassName = StringUtils.substring(className, StringUtils.lastIndexOf(className, ".") + 1);
        this.caseDisplayName = caseDisplayName;
    }

    public CaseResult(String className, String methodName, double time, String result, String detailInfo,
                      String sysoutLog) {
        this(className, methodName, time, result, detailInfo);
        this.sysoutLog = sysoutLog;
    }

    public CaseResult(String className, String methodName, String caseDisplayName, double time, String result,
                      String detailInfo,
                      String sysoutLog) {
        this(className, methodName, time, result, detailInfo, sysoutLog);
        this.caseDisplayName = caseDisplayName;
    }

    private String filterContent(String content) {
        if (StringUtils.isBlank(content)) {
            return content;
        }
        if (StringUtils.length(content) > 10000) {
            content = StringUtils.substring(content, 0, 10000);
        }
        return content.replace("*+TDDL", "++TDDL").replace("!+TDDL", "++TDDL").replace("*TDDL:", "++TDDL");
    }
}
