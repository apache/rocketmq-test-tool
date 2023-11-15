/*
 * #
 * # Licensed to the Apache Software Foundation (ASF) under one or more
 * # contributor license agreements.  See the NOTICE file distributed with
 * # this work for additional information regarding copyright ownership.
 * # The ASF licenses this file to You under the Apache License, Version 2.0
 * # (the "License"); you may not use this file except in compliance with
 * # the License.  You may obtain a copy of the License at
 * #
 * #     http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 * #
 */

package org.apache.process.api;

import okhttp3.*;
import org.apache.process.config.Configs;

import java.io.IOException;

public class AppActions {
    public static final String APP_API = "applications";
    private final OkHttpClient client;
    private final String URL;
    private final MediaType mediaType;

    public AppActions() {
        client = new OkHttpClient();
        URL = "http://" + Configs.IP + "/" + Configs.KUBEVELA_API + "/" + APP_API;
        mediaType = MediaType.parse("application/json");
    }

    public Response createApplication(String bodyContent) throws IOException {
        RequestBody body = RequestBody.create(mediaType, bodyContent);
        Request request = new Request.Builder()
                .url(URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json, application/xml")
                .addHeader("Authorization", Configs.Authorization)
                .build();

        return client.newCall(request).execute();
    }

    public Response deployOrUpgradeApplication(String appName, String bodyContent) throws IOException {
        String url = URL + "/" + appName + "/deploy";

        RequestBody body = RequestBody.create(mediaType, bodyContent);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json, application/xml")
                .addHeader("Authorization", Configs.Authorization)
                .build();

        return client.newCall(request).execute();
    }

    public Response getApplicationStatus(String appName, String envName) throws IOException {
        String url = URL + "/" + appName + "/envs/" + envName + "/status";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Accept", "application/json, application/xml")
                .addHeader("Authorization", Configs.Authorization)
                .build();

        return client.newCall(request).execute();
    }

    public Response deleteApplication(String appName) throws IOException {
        String url = URL + "/" + appName;
        Request request = new Request.Builder()
                .url(url)
                .delete(null)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json, application/xml")
                .addHeader("Authorization", Configs.Authorization)
                .build();

        return client.newCall(request).execute();
    }

    public Response deleteOAM(String namespace, String appName) throws IOException {
        String url = "http://" + Configs.IP + "/v1/namespaces/" + namespace + "/applications/" + appName;
        Request request = new Request.Builder()
                .url(url)
                .delete(null)
                .addHeader("Authorization", Configs.Authorization)
                .build();

        return client.newCall(request).execute();
    }
}
