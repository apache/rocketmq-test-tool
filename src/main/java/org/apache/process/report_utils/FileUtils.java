package org.apache.process.report_utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.*;

/**
 * Created by wangtong.wt on 2017/3/20.
 *
 * @author wangtong.wt
 * @date 2017/03/20
 */
@Slf4j
public class FileUtils {
    public static String getFileMd5(File file) {
        try {
            InputStream inputStream = new FileInputStream(file);
            return DigestUtils.md5Hex(inputStream);
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
        }
        return null;
    }

}
