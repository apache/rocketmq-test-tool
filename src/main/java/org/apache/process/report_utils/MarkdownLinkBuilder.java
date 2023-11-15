package org.apache.process.report_utils;

/**
 * Created by wangtong.wt on 2017/3/20.
 *
 * @author wangtong.wt
 * @date 2017/03/20
 */
public class MarkdownLinkBuilder {

    private StringBuilder sb = new StringBuilder();

    private MarkdownLinkBuilder() {}

    public static MarkdownLinkBuilder builder() {
        return new MarkdownLinkBuilder();
    }

    public void setLink(String name, String url) {
        sb.append("[").append(name).append("]").append("(").append(url).append(")");
    }

    public String build() {
        return sb.toString();
    }
}
