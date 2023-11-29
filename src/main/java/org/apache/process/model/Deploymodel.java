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

package org.apache.process.model;

import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

public class Deploymodel {
    public static final String APPLICATION_BODY_COMPONENT = "{\n" +
            "    \"name\": \"%s\",\n" +
            "    \"project\": \"%s\",\n" +
            "    \"description\": \"%s\",\n" +
            "    \"alias\": \"%s\",\n" +
            "    \"envBinding\": [\n" +
            "        {\n" +
            "            \"name\": \"%s\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"component\": {\n" +
            "        \"name\": \"%s\",\n" +
            "        \"componentType\": \"helm\",\n" +
            "        \"properties\": \"%s\"" +
            "    }\n" +
            "}";


    public static final String ENV_BODY = "{\n" +
            "  \"allowTargetConflict\": true,\n" +
            "  \"description\": \"create env for test\",\n" +
            "  \"name\": \"%s\",\n" +
            "  \"namespace\": \"%s\",\n" +
            "  \"project\": \"%s\",\n" +
            "  \"alias\": \"%s\"\n" +
            "}";

    public static final String DEPLOY_APP_BODY = "{\n" +
            "  \"force\": true,\n" +
            "  \"note\": \"test deploy by http request\",\n" +
            "  \"triggerType\": \"webhook\",\n" +
            "  \"workflowName\": \"%s\"\n" +
            "}";

    public static String generateComponentProperties(String helmValue, String chartPath, String chartBranch, String chartGit) {
        StringBuilder builder = new StringBuilder();
        String baseString =
                "chart: %s\n" +
                        "git:\n" +
                        "  branch: %s\n" +
                        "repoType: git\n" +
                        "retries: 3\n" +
                        "url: %s\n" +
                        "values:\n";
        builder.append(String.format(baseString, chartPath, chartBranch, chartGit));
        String[] lines = helmValue.split("\n");
        for (String line : lines) {
            builder.append("  ").append(line).append("\n");
        }
        Yaml yaml = new Yaml();

        Map<String, Object> map = (Map<String, Object>) yaml.load(builder.toString());
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject.toString().replaceAll("\"", "\\\\\"");
    }


}
