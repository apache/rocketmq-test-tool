package org.apache.process.report_utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;

@Slf4j
public class GetGithubRepoInfo {
    public static final String API_BASE_URL = "https://api.github.com/repos";

    private final OkHttpClient httpClient;

    public GetGithubRepoInfo() {
        this.httpClient = new OkHttpClient();
    }

    /**
     * find all files in current path.
     *
     * @param url         repository http url.
     * @param gitBranch   reposity branch.
     * @param githubToken github token.
     * @param fileMap     file Map.
     */
    public void getAllFilePath(String url, String gitBranch, String githubToken, HashMap<String, RepoFileInfo> fileMap) {
        Request request;
        if (githubToken != null && githubToken.length() != 0) {
            request = new Request.Builder()
                    .url(url + "?ref=" + gitBranch)
                    .addHeader("Authorization", githubToken)
                    .build();
        } else {
            request = new Request.Builder()
                    .url(url + "?ref=" + gitBranch)
                    .build();
        }

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONArray jsonArray = new JSONArray(response.body().string());
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String type = jsonObject.getString("type");
                    String fileUrl = jsonObject.getString("html_url");
                    String[] array = jsonObject.getString("path").split("/");
                    String filename = array[array.length - 1];
                    if ("dir".equals(type)) {
                        getAllFilePath(url + "/" + filename, gitBranch, githubToken, fileMap);
                    } else {
                        RepoFileInfo repoFileInfo = new RepoFileInfo();
                        repoFileInfo.setFileName(filename);
                        repoFileInfo.setFileUrl(fileUrl);
                        if (filename.split("\\.").length == 2) {
                            repoFileInfo.setSuffix(filename.split("\\.")[1]);
                            fileMap.put(filename.split("\\.")[0], repoFileInfo);
                        } else {
                            fileMap.put(filename, repoFileInfo);
                        }
                    }
                }
            } else if (response.code() == 404) {
                log.error("Directory does not exist!");
            } else {
                log.error("Error! response code: {}, response message: {}", response.code(), response.message());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * find target function lines.
     *
     * @param className   class name.
     * @param url         repository http url.
     * @param keyword     function name.
     * @param githubToken github token.
     * @param fileInfoMap file info map.
     * @return lines number.
     * @throws IOException not found exception.
     */
    public int getFunctionRowFromFile(String className, String url, String keyword, String githubToken, HashMap<String, RepoFileInfo> fileInfoMap) throws IOException {
        if (fileInfoMap.get(className).getContent() == null) {
            Request request;
            if (githubToken != null && githubToken.length() != 0) {
                request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", githubToken)
                        .build();
            } else {
                request = new Request.Builder()
                        .url(url)
                        .build();
            }

            Response response = httpClient.newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            String responseBody = response.body().string();
            JSONObject jsonObject = new JSONObject(responseBody);
            String s = jsonObject.getString("content");
            fileInfoMap.get(className).setContent(s.replace("\n", ""));
        }
        String content = new String(Base64.getDecoder().decode(fileInfoMap.get(className).getContent()));

        String[] lines = content.split("\n");
        int result = 0;

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(keyword)) {
                result = i + 1;
                break;
            }
        }
        return result;
    }

    /**
     * get URL of the test case.
     *
     * @param fileInfoMap  class to fileInfo map.
     * @param githubToken  github token.
     * @param className    test class name.
     * @param functionName test function.
     * @param repo         username/repository.
     * @param branch       repository branch.
     * @return complete url.
     */
    public String getCaseUrl(HashMap<String, RepoFileInfo> fileInfoMap, String githubToken, String className, String functionName, String repo, String branch) throws IOException {
        String url = "";
        int row = 0;
        if (fileInfoMap.containsKey(className)) {
            String[] array = fileInfoMap.get(className).getFileUrl().split(repo);
            String fileUrl = API_BASE_URL + "/" + repo + "/contents" + array[1].split(branch)[1];
            row = getFunctionRowFromFile(className, fileUrl, functionName, githubToken, fileInfoMap);
            url = fileInfoMap.get(className).getFileUrl() + "#L" + row;
        } else {
            for (String fileName : fileInfoMap.keySet()) {

                String[] array = fileInfoMap.get(fileName).getFileUrl().split(repo);
                String fileUrl = API_BASE_URL + "/" + repo + "/contents" + array[1].split(branch, 2)[1];
                row = getFunctionRowFromFile(fileName, fileUrl, functionName, githubToken, fileInfoMap);
                if (row != 0) {
                    url = fileInfoMap.get(fileName).getFileUrl() + "#L" + row;
                    break;
                }
            }
        }
        return url;
    }
}
