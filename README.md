# test-tools
This project is used for repository testing, including deployment, testing, cleaning.
## Preparation
- ASK cluster: a cluster to run code.
- install kubevela in ask cluster.
## params
you should input a yaml format string.
Attention: 
- askConfig must be encoder by base64.
- If some of the parameters are not included, set it to null.
- you can add or delete params in "helm" and "ENV" segment .
##### example
###### deploy
```agsl
yamlString: 
  action: deploy
  namespace: rocketmq-457628-0
  askConfig: ***********
  waitTimes: 1200
  velaAppDescription: rocketmq-push-ci-0@abcdefg
  repoName: rocketmq
  velauxUsername: admin
  velauxPassword: velaux12345
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

###### test
```agsl
yamlString: |
  action: test
  namespace: rocketmq-457628-0
  askConfig: ***********
  API_VERSION: v1
  KIND: Pod
  RESTART_POLICY: Never
  ENV:
    CODE: https://ghproxy.com/https://github.com/apache/rocketmq-e2e
    BRANCH: master
    CODE_PATH: java/e2e-v4
    CMD: mvn -B test
    ALL_IP: null
  CONTAINER:
    IMAGE: cloudnativeofalibabacloud/test-runner:v0.0.3
    RESOURCE_LIMITS:
      cpu: 8
      memory: 8Gi
    RESOURCE_REQUIRE:
      cpu: 8
      memory: 8Gi
```
###### clean
```agsl
yamlString: |
  action: clean
  namespace: rocketmq-457628-0
  askConfig: ***********
  velauxUsername: admin
  velauxPassword: velaux12345
```


## Usage

## start project
### by java jar
```agsl
cd test-tools
mvn clean install -Dmaven.test.skip=true
mv /target/rocketmq-test-tools-*-SNAPSHOT-jar-*.jar ./rocketmq-test-tools.jar
# quick start run
jar -jar rocketmq-test-tools.jar -yamlString=${your yamlString}
```
### by docker images
```
# build docker images
docker build -t test-tools
# quick start run
docker run -it test-tools -yamlString=${your yamlString}
```
### deploy in github action
Attention: if you use this resposity dockerfile, make sure all params input and are in order. Example follow：
#### rocketmq example
```
test:
    name: Deploy RocketMQ
    runs-on: ubuntu-latest
    steps:
      - uses: Wuyunfan-BUPT/test-tools@main
        name: Deploy, run e2etest and clean rocketmq
        with:
          testRepo: "rocketmq"
          action: "deploy"
          version: "your-version"
          askConfig: "your ask config"
          velauxUsername: "your velaux username"
          velauxPassword: "your velaux password"
          chartGit: "https://ghproxy.com/https://github.com/apache/rocketmq-docker.git"
          chartBranch: "master"
          chartPath: "./rocketmq-k8s-helm"
          testCodeGit: "https://ghproxy.com/https://github.com/apache/rocketmq-e2e.git"
          testCodeBranch: "master"
          testCodePath: "java/e2e"
          testCmdBase: "mvn -B test"
          jobIndex: your job index
          helmValue: |
            nameserver:
              image:
                repository: wuyfeedocker/rocketm-ci
                tag: develop-82ca7301-3b14-4f86-aaa8-4881ebe4762d-ubuntu
            broker:
              image:
                repository: wuyfeedocker/rocketm-ci
                tag: develop-82ca7301-3b14-4f86-aaa8-4881ebe4762d-ubuntu
            proxy:
              image:
                repository: wuyfeedocker/rocketm-ci
                tag: develop-82ca7301-3b14-4f86-aaa8-4881ebe4762d-ubuntu
```
#### nacos example
```
test:
    name: Deploy nacos-server
    runs-on: ubuntu-latest
      - uses: Wuyunfan-BUPT/test-tools@main
        name: Deploy and run nacos test
        with:
          testRepo: "nacos"
          action: ""
          version: your-version
          askConfig: "your asc config"
          velauxUsername: "your velaux username"
          velauxPassword: "your velaux password"
          chartGit: "https://ghproxy.com/https://github.com/Wuyunfan-BUPT/nacos-docker.git"
          chartBranch: "master"
          chartPath: "./helm"
          testCodeGit: "https://github.com/nacos-group/nacos-e2e.git"
          testCodeBranch: "master"
          testCodePath: "java/nacos-2X"
          testCmdBase: 'mvn clean test -B'
          jobIndex: your index
          helmValue: |
            global:
              mode: cluster
            nacos:
              replicaCount: 3
              image: 
                repository: wuyfeedocker/nacos-ci
                tag: develop-88ccc682-10b0-4948-811a-8eba750e14ea-8
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