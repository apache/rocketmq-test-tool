package org.apache.process.report_utils;

public class RepoFileInfo {

    private String suffix;
    private String fileUrl;
    private String content;

    private String fileName;
    public RepoFileInfo() {
        suffix = null;
        content = null;
        fileUrl = null;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getContent() {
        return content;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setSuffix(String suffix){
        this.suffix = suffix;
    }
}
