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

package org.apache.process.config;

public class Configs {

    // velaux bearer token
    public static String  Authorization = "";

    // velaux token
    public static String  TOKEN = "";

    // velaux refresh token
    public static String REFRESH_TOKEN = "";

    // velaux username
    public static String VELAUX_USERNAME = "";

    // velaux password
    public static String VELAUX_PASSWORD = "";

    // kubevela api version
    public static final String KUBEVELA_API = "api/v1";

    // Maximum time(MINUTES) required for testing.
    public static final int MAX_RUN_TIME = 30;

    public static String PROJECT_NAME = "wyftest";

    public static String VELA_NAMESPACE = "vela-system";
    public static String VELA_POD_LABELS = "addon-velaux";
    public static boolean IS_ALL_CASE_SUCCESS = true;
    // open port
    public static final int PORT_FROWARD = 9082;
    // velaux IP
    public static final String IP = "127.0.0.1:"+PORT_FROWARD;

}
