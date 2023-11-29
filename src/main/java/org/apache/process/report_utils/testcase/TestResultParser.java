package org.apache.process.report_utils.testcase;

import java.io.File;
import java.util.List;

/**
 * Created by wangtong.wt on 2017/3/20.
 *
 * @author wangtong.wt
 * @date 2017/03/20
 */
public interface TestResultParser {

    TaskResult parseTestResult(List<File> files);
}
