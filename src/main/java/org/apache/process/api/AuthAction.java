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

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.process.config.Configs;
import org.json.JSONObject;

import java.util.Objects;

@Slf4j
public class AuthAction {
    public static final String APP_API = "auth";
    private final String URL;
    private final OkHttpClient client;

    public AuthAction() {
        client = new OkHttpClient();
        URL = "http://" + Configs.IP + "/" + Configs.KUBEVELA_API + "/" + APP_API;
    }

    public void setToken(String action) {

        Request request = null;
        if ("login".equals(action)) {
            String url = URL + "/" + "login";
            String bodyContent = String.format("{\n \"code\": \"string\",\n \"password\": \"%s\",\n  \"username\": \"%s\"\n}", Configs.VELAUX_PASSWORD, Configs.VELAUX_USERNAME);
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), bodyContent);
            request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json, application/xml")
                    .build();
        } else {
            String url = URL + "/" + "refresh_token";
            request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Accept", "application/json, application/xml")
                    .addHeader("accessToken", Configs.TOKEN)
                    .addHeader("refreshToken", Configs.REFRESH_TOKEN)
                    .build();
        }

        int retryTimes = 3;
        boolean isSuccess = false;
        while (retryTimes-- > 0 && !isSuccess) {
            try (Response response = client.newCall(request).execute()) {
                JSONObject json = new JSONObject(response.body().string());
                if (json.has("accessToken") && !Objects.equals(json.getString("accessToken"), "")) {
                    Configs.TOKEN = json.getString("accessToken");
                    Configs.Authorization = "Bearer " + Configs.TOKEN;
                    Configs.REFRESH_TOKEN = json.getString("refreshToken");
                    isSuccess = true;
                } else {
                    log.info("{} fail! ", action);
                }
            } catch (Exception e) {
                log.error("{} error! ", action);
            }
        }

    }
}
