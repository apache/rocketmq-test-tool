# Apache RocketMQ Test Tool
This tool uses Helm and KubeVela to deploy applications and execute tests in Kubernetes.
KubeVela needs to be installed in Kubernetes before use.


## Preparation
- Install kubevela in Kubernetes.
- An account in vela system. 
  - If you have an account, you should set this velauxUsername and velauxPassword in yamlString. 
  - If velauxUsername and velauxPassword is not included in yamlString, you should create an account in vela system, password and username should be created by following function, this tool will genarate username and password by ask config: 
  ```java
  /**
     * get velaUX username and password
     *
     * @param kubeConfig ask config
     * @return username:password
     */
    public String getAuthInfoFromConfig(String kubeConfig) {
        String text = kubeConfig.length() > 150 ? kubeConfig.substring(kubeConfig.length() - 150) : kubeConfig;
        StringBuilder userName = new StringBuilder();
        StringBuilder password = new StringBuilder();
        boolean digitMark = false;
        for (int index = text.length() - 1; index >= 0; index--) {
            if (userName.length() >= 6 && password.length() >= 12) {
                break;
            }
            boolean isLetter = Character.isLetter(text.charAt(index));
            boolean isDigit = Character.isDigit(text.charAt(index));
            if (isDigit || isLetter) {
                if (isLetter && userName.length() < 6) {
                    userName.append(Character.toLowerCase(text.charAt(index)));
                }
                if (password.length() < 12) {
                    if (digitMark && isDigit) {
                        password.append(text.charAt(index));
                        digitMark = false;
                    } else if (!digitMark && isLetter) {
                        password.append(text.charAt(index));
                        digitMark = true;
                    }
                }
            }
        }
        return userName + ":" + password;
    }
  - ```
    
#### example
you should input a yaml format string.
Attention:
- AskConfig must be encoder by base64.
- If some of the parameters are not included, set it to null.
- You can add or delete params in "helm" and "ENV" segment .
###### deploy
use kubevela API to deploy application.
```yaml
yamlString: 
  action: deploy
  namespace: rocketmq-457628-0
  askConfig: ***********
  velauxUsername: ***
  velauxPassword: ***
  projectName: wyftest
  waitTimes: 1200
  velaAppDescription: rocketmq-push-ci-123456@abcdefg
  repoName: rocketmq
  helm:
    chart: ./rocketmq-k8s-helm
    git:
      branch: master
    repoType: git
    retries: 3
    url: https://ghproxy.com/https://github.com/apache/rocketmq-docker.git
    values:
      nameserver:
        image:
          repository: wuyfeedocker/rocketm-ci
          tag: develop-3b416669-cab7-41b4-8cc8-4af851944de2-ubuntu
      broker:
        image:
          repository: wuyfeedocker/rocketm-ci
          tag: develop-3b416669-cab7-41b4-8cc8-4af851944de2-ubuntu
      proxy:
        image:
          repository: wuyfeedocker/rocketm-ci
          tag: develop-3b416669-cab7-41b4-8cc8-4af851944de2-ubuntu
```
| option             | description                                 | default | necessary |
|--------------------|---------------------------------------------|--------|-----------|
| action             | deploy                                      | null   | yes       |
|velauxUsername      | vela username                               | null   | no        |
| velauxPassword | vela password                               | null   | no        |
|projectName | vela project | wyftest | no |
| namespace          | pod namespace                               | null   | yes       |
| askConfig          | ask config                                  | null   | yes       |
| waitTimes          | deploy max time (second)                    | 900 | no        |
| velaAppDescription | vela app description                        | ""     | no        |
| repoName           | repo name(such as "nacos", "rocketmq" .etc) | null   | yes       |
| helm         | helm chart                                  | null   | yes       |

###### test
use kubernetes API to execute test.
```yaml
yamlString: |
  action: test
  namespace: rocketmq-457628-0
  askConfig: ***********
  API_VERSION: v1
  KIND: Pod
  RESTART_POLICY: Never
  ENV:
    WAIT_TIME: 600 
    REPO_NAME: apache/rocketmq-e2e
    CODE: https://github.com/apache/rocketmq-e2e
    BRANCH: master
    CODE_PATH: java/e2e-v4
    CMD: mvn -B test
    ALL_IP: null
  CONTAINER:
    IMAGE: cloudnativeofalibabacloud/test-runner:v0.0.4
    RESOURCE_LIMITS:
      cpu: 8
      memory: 8Gi
    RESOURCE_REQUIRE:
      cpu: 8
      memory: 8Gi
```
| option                            | description                       | default    | necessary |
|-----------------------------------|-----------------------------------|---------|-----------|
| action                            | test                              | null       | yes       |
| namespace                         | pod namespace                     | null       | yes       |
| askConfig                         | ask config                        | null       | yes       |
| API_VERSION                         | Kubernetes API version            | v1         | no        |
| KIND                | pod kind                          | Pod       | no        |
| RESTART_POLICY                          | pod restart policy                | Never       | no        |
| ENV.WAIT_TIME                     | test pod expiration time (second) | 900      | no        |
| ENV.REPO_NAME                     | repository whole name             | null       | yes       |
| ENV.CODE                          | test code url                     | null       | yes       |
| ENV.BRANCH                        | code branch                       | null       | yes       |
| ENV.CODE_PATH                     | test code path                    | null       | yes       |
| ENV.CMD                           | test command                      | null       | yes       |
| ENV.ALL_IP                        | cluster ips                       | null       | no        |
| CONTAINER.IMAGE                   | pod container image               | null       | yes       |
| CONTAINER.RESOURCE_LIMITS.cpu     | pod container cpu limit           | null       | no        |
| CONTAINER.RESOURCE_LIMITS.memory  | pod container memory limit        | null       | no        |
| CONTAINER.RESOURCE_REQUIRE.cpu    | pod container cpu require         | null       | no        |
| CONTAINER.RESOURCE_REQUIRE.memory | pod container memory require      | null       | no        |
###### clean
use kubernetes API and kubevela API to clean resource.
```yaml
yamlString: |
  action: clean
  namespace: rocketmq-457628-0
  velauxUsername: ***
  velauxPassword: ***
  askConfig: ***********
```
| option         | description   | default    | necessary |
|----------------|---------------|---------|-----------|
| action         | clean         | null     | yes       |
| velauxUsername | vela username |   null  | no |
| velauxPassword | vela password | null       | no        |
| namespace      | pod namespace | null       | yes       |
| askConfig      | ask config               | null   | yes       |


## Usage

<!-- start usage -->
## start project
### by java jar
```agsl
cd test-tools
mvn clean install -Dmaven.test.skip=true
mv /target/rocketmq-test-tool-*-SNAPSHOT-jar-*.jar ./rocketmq-test-tool.jar
# quick start run
jar -jar rocketmq-test-tool.jar -yamlString=${your yamlString}
```
### by docker images
```
# build docker images
docker build -t test-tool
# quick start run
docker run -it test-tool -yamlString=${your yamlString}
```
### in github action
#### deploy 
```yaml
- uses: apache/rocketmq-test-tool@java-dev
  name: Deploy nacos
  with:
    yamlString: |
      action: deploy
      namespace: nacos-123456789-0
      askConfig: ******
      waitTimes: 2000
      velaAppDescription: nacos-push-ci-123@$abcdefg
      repoName: nacos
      helm:
        chart: ./cicd/helm
        git:
          branch: main
        repoType: git
        retries: 3
        url: https://ghproxy.com/https://github.com/nacos-group/nacos-e2e.git
        values:
          namespace: nacos-123456789-0
          global:
            mode: cluster
          nacos:
            replicaCount: 3
            image:
              repository: wuyfeedocker/nacos-ci
              tag: develop-cee62800-0cb5-478f-9e42-aeb1124db716-8
            storage:
              type: mysql
              db:
                port: 3306
                username: nacos
                password: nacos
                param: characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useSSL=false
          service:
            nodePort: 30000
            type: ClusterIP
```
#### test
```yaml
steps:
  - uses: apache/rocketmq-test-tool@java-dev
    name: java e2e test
    with:
      yamlString: |
        action: test
        namespace: nacos-123456789-0
        askConfig: ******
        API_VERSION: v1
        KIND: Pod
        RESTART_POLICY: Never
        ENV:
          WAIT_TIME: 900
          REPO_NAME: nacos-group/nacos-e2e
          CODE: https://github.com/nacos-group/nacos-e2e
          BRANCH: main
          CODE_PATH: java/nacos-2X
          CMD: mvn clean test -B
          ALL_IP: null
        CONTAINER:
          IMAGE: cloudnativeofalibabacloud/test-runner:v0.0.4
          RESOURCE_LIMITS:
            cpu: 8
            memory: 8Gi
          RESOURCE_REQUIRE:
            cpu: 8
            memory: 8Gi
```
clean
```yaml
steps:
  - uses: apache/rocketmq-test-tool@java-dev
    name: clean
    with:
      yamlString: |
        action: clean
        namespace: nacos-123456789-0
        askConfig: ******
```
<!-- end usage -->
# License
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) Copyright (C) Apache Software Foundation
