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

package org.apache.process.utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class GetParam {
    public HashMap<String, String> setParam(CommandLine cmd) {
        HashMap<String, String> result = new HashMap<>();
        for (Option option : cmd.getOptions()) {
            result.put(option.getLongOpt(), option.getValue());
        }
        return result;
    }

    public static LinkedHashMap<String, Object> yamlToMap(String input) {
        Yaml yaml = new Yaml();
        return (LinkedHashMap<String, Object>) yaml.load(input);
    }

    public static HashMap<String, Object> parseDeployInput(String input, String target) {
        LinkedHashMap<String, Object> builderMap = yamlToMap(input);
        LinkedHashMap<String, Object> helmValuesMap = (LinkedHashMap<String, Object>) builderMap.get(target);
        JSONObject jsonObject = new JSONObject(helmValuesMap);
        builderMap.put(target, jsonObject.toString().replaceAll("\"", "\\\\\"").toString());
        return builderMap;
    }
}
