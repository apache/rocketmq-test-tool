package org.apache.process.report_utils;

/**
 * Created by wangtong.wt on 2017/3/20.
 *
 * @author wangtong.wt
 * @date 2017/03/20
 */
public class MarkdownBuilder {

    private StringBuilder stringBuilder = new StringBuilder();

    private MarkdownBuilder(){}

    public static MarkdownBuilder builder() {
        return new MarkdownBuilder();
    }

    public MarkdownBuilder addHeader(String content, int level) {
        StringBuilder header = new StringBuilder();
        if (level > 6) {
            level = 6;
        }
        if (level <= 0) {
            level = 1;
        }
        for (int i = 0; i < level; i++) {
            header.append("#");
        }
        header.append(" ").append(content);
        stringBuilder.append(header).append("\n\n");
        return this;
    }

    public MarkdownBuilder newLine() {
        stringBuilder.append("\n");
        return this;
    }

    public MarkdownBuilder addBoldText(String content) {
        stringBuilder.append("**").append(content).append("**");
        return this;
    }

    public MarkdownBuilder addCollapse(String title, String msg) {
        stringBuilder.append("<details>").append("\n");
        stringBuilder.append("<summary>").append(title).append("</summary>").append("\n");
        stringBuilder.append("\n").append(msg).append("\n");
        stringBuilder.append("</details>").append("\n\n");

        return this;
    }

    public MarkdownBuilder addTable(MarkdownTableBuilder tableBuilder){
        stringBuilder.append("\n").append(tableBuilder.build()).append("\n\n");
        return this;
    }

    public MarkdownBuilder addText(String text) {
        stringBuilder.append(text);
        return this;
    }

    public String build() {
        return stringBuilder.toString();
    }
}
