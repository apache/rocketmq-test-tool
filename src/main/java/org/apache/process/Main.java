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

package org.apache.process;

import lombok.extern.slf4j.Slf4j;
import org.apache.process.action.Deploy;
import org.apache.process.action.PortForward;
import org.apache.process.action.EnvClean;
import org.apache.process.action.RepoTest;
import org.apache.process.action.GenerateReport;
import org.apache.commons.cli.*;
import org.apache.process.config.Configs;
import org.apache.process.utils.GetParam;
import org.apache.process.utils.ConfigUtils;

import java.util.*;

import static org.apache.process.utils.GetParam.parseDeployInput;
import static org.apache.process.utils.GetParam.yamlToMap;

@Slf4j
public class Main {
    public static void main(String[] args) {
        /* define input parameters */
        Options options = new Options();
        options.addOption(Option.builder("yamlString").longOpt("yamlString").argName("yamlString").desc("yaml String").hasArg().required(true).build());

        /* parse input parameters */
        CommandLine cmd;
        CommandLineParser parser = new DefaultParser();
        HelpFormatter helper = new HelpFormatter();
        HashMap<String, String> paramsMap = null;
        try {
            cmd = parser.parse(options, args);
            GetParam getParam = new GetParam();
            paramsMap = getParam.setParam(cmd);
        } catch (ParseException e) {
            log.info("Params input error! Message: {}", e.getMessage());
            helper.printHelp("Usage:", options);
            System.exit(1);
        }

        try {
            String inputYamlString = paramsMap.get("yamlString");
            LinkedHashMap<String, Object> inputMap = yamlToMap(inputYamlString);
            String action = inputMap.get("action").toString();


            ConfigUtils configUtils = new ConfigUtils();
            String askConfig = configUtils.base64Decoder(inputMap.get("askConfig").toString().replace("\\n", ""));
            if (inputMap.getOrDefault("velauxUsername", null) != null && inputMap.getOrDefault("velauxPassword", null) != null) {
                Configs.VELAUX_USERNAME = inputMap.get("velauxUsername").toString();
                Configs.VELAUX_PASSWORD = inputMap.get("velauxPassword").toString();
            } else {
                String authentificationInfo[] = configUtils.getAuthInfoFromConfig(askConfig).split(":");
                Configs.VELAUX_USERNAME = authentificationInfo[0];
                Configs.VELAUX_PASSWORD = authentificationInfo[1];
            }

            configUtils.setConfig(askConfig);
            new PortForward().startPortForward(Configs.VELA_NAMESPACE, Configs.VELA_POD_LABELS, Configs.PORT_FROWARD);

            boolean isSuccessed = false;
            if ("deploy".equals(action)) {
                HashMap<String, Object> deployMap = parseDeployInput(inputYamlString, "helm");
                isSuccessed = new Deploy().startDeploy(deployMap);
            } else if ("test".equals(action)) {
                isSuccessed = new RepoTest().runTest(inputMap);
                isSuccessed = new GenerateReport().generateReportMarkDown(inputMap) && isSuccessed;
            } else if ("clean".equals(action)) {
                isSuccessed = new EnvClean().clean(inputMap);
            } else {
                log.error("Error! Please input action!");
            }

            if (isSuccessed && Configs.IS_ALL_CASE_SUCCESS) {
                System.exit(0);
            }
        } catch (Exception e) {
            log.error("Execute error! Message: {}", e.getMessage());
        }
        System.exit(1);
    }
}
