package org.apache.process.report_utils;

/**
 * Created by wangtong.wt on 2017/3/20.
 *
 * @author wangtong.wt
 * @date 2017/03/20
 */
public class MarkdownTableBuilder {
    private StringBuilder sb = new StringBuilder();
    private MarkdownTableBuilder() {}

    public static MarkdownTableBuilder builder() {
        return new MarkdownTableBuilder();
    }

    public MarkdownTableBuilder addHead(String... item) {
        StringBuilder head = new StringBuilder();
        head.append("|");
        for (String s : item) {
            head.append(s).append("|");
        }
        head.append("\n");

        head.append("|");
        for (String s : item) {
            for (int i = 0; i < s.getBytes().length; i++) {
                head.append("-");
            }
            head.append("|");
        }
        head.append("\n");

        sb.append(head);
        return this;
    }

    public MarkdownTableBuilder addRow(Object... item) {
        StringBuilder row = new StringBuilder();
        row.append("|");
        for (Object s : item) {
            row.append(s.toString()).append("|");
        }
        row.append("\n");

        sb.append(row);
        return this;
    }

    public String build() {
        return sb.toString();
    }
}
