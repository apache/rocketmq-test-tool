#!/bin/sh
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

ACTION=$1
VERSION=$2
ASK_CONFIG=$3
DOCKER_REPO_USERNAME=$4
DOCKER_REPO_PASSWORD=$5
CHART_GIT=$6
CHART_BRANCH=$7
CHART_PATH=$8
TEST_CODE_GIT=${9}
TEST_CODE_BRANCH=${10}
TEST_CODE_PATH=${11}
TEST_CMD_BASE=${12}
JOB_INDEX=${13}
HELM_VALUES=${14}
TEST_TIME=${15}

export VERSION
export CHART_GIT
export CHART_BRANCH
export CHART_PATH
export REPO_NAME=`echo ${GITHUB_REPOSITORY#*/} | sed -e "s/\//-/g" | cut -c1-36 | tr '[A-Z]' '[a-z]'`
export WORKFLOW_NAME=${GITHUB_WORKFLOW}
export RUN_ID=${GITHUB_RUN_ID}
export TEST_CODE_GIT
export TEST_CODE_BRANCH
export TEST_CODE_PATH
export YAML_VALUES=`echo "${HELM_VALUES}" | sed -s 's/^/          /g'`

# 连接rocketmq集群
# 创建压力机Pod ： consumer和producer
# 执行压力测试脚本
# 收集测试数据
# 与提前设置的阈值作比较，作为ci通过的条件

mkdir -p ${HOME}/.kube
kube_config=$(echo "${ASK_CONFIG}")
echo "${kube_config}" > ${HOME}/.kube/config
export KUBECONFIG="${HOME}/.kube/config"

# 检查集群连接
echo "Checking Kubernetes cluster connection..."
kubectl get nodes
if [ $? -ne 0 ]; then
  echo "Error: Cannot connect to Kubernetes cluster."
  exit 1
fi

env_uuid=${REPO_NAME}-${GITHUB_RUN_ID}-${JOB_INDEX}

VELA_APP_TEMPLATE='
apiVersion: core.oam.dev/v1beta1
kind: Application
metadata:
  name: ${VELA_APP_NAME}
  description: ${REPO_NAME}-${WORKFLOW_NAME}-${RUN_ID}@${VERSION}
spec:
  components:
    - name: ${REPO_NAME}
      type: helm
      properties:
        chart: ${CHART_PATH}
        git:
          branch: ${CHART_BRANCH}
        repoType: git
        retries: 3
        secretRef: \047\047
        url: ${CHART_GIT}
        values:
${YAML_VALUES}'

echo -e "${VELA_APP_TEMPLATE}" > ./velaapp.yaml
sed -i '1d' ./velaapp.yaml

if [ "${ACTION}" = "deploy" ]; then
  echo "************************************"
  echo "*     Create env and deploy...     *"
  echo "************************************"

  echo ${VERSION}: ${env_uuid} deploy start

  vela env init ${env_uuid} --namespace ${env_uuid}

  export VELA_APP_NAME=${env_uuid}
  envsubst < ./velaapp.yaml > velaapp-${REPO_NAME}.yaml
  cat velaapp-${REPO_NAME}.yaml

  vela env set ${env_uuid}
  vela up -f "velaapp-${REPO_NAME}.yaml"

  app=${env_uuid}

  status=`vela status ${app} -n ${app}`
  echo $status
  res=`echo $status | grep "Create helm release successfully"`
  count=$((0))
  while [ -z "$res" ]
  do
      if [ $count -gt 240 ]; then
        echo "env ${app} deploy timeout..."
        exit 1
      fi
      echo "waiting for env ${app} ready..."
      sleep 5
      status=`vela status ${app} -n ${app}`
      stopped=`echo $status | grep "not found"`
      if [ ! -z "$stopped" ]; then
          echo "env ${app} deploy stopped..."
          exit 1
      fi
      res=`echo $status | grep "Create helm release successfully"`
      count=$((count + 1))
  done
fi

CLIENT_POD_TEMPLATE='
apiVersion: v1
kind: Pod
metadata:
  name: ${test_pod_name}
  labels:
    app: rocketmq-benchmark-${test_pod_name}
spec:
  containers:
    - name: ${test_pod_name}
      image: registry.cn-guangzhou.aliyuncs.com/cc-aliyun/rocketmq:latest
      command: ["/bin/sh", "-c"]
      env:
        - name: NAMESRV_ADDR
          value: ${namesrv}
        - name: TIMESTAMP
          value: "${timestamp}"
        - name: TEST_CMD
          value: ${TEST_CMD}
      args: ["tail -f /dev/null"]
      resources:
        requests:
          memory: "4Gi"
          cpu: "4"
        limits:
          memory: "4Gi"
          cpu: "4"
      volumeMounts:
        - name: report-volume
          mountPath: /mnt/report
  volumes:
    - name: report-volume
      emptyDir: {}
  restartPolicy: Never
'

if [ "${ACTION}" = "performance-benchmark" ]; then

  echo -e "${CLIENT_POD_TEMPLATE}" > ./consumer_pod.yaml
  sed -i '1d' ./consumer_pod.yaml

  timestamp=$(date +%Y%m%d_%H%M%S)
  export timestamp
  ns=${env_uuid}
  export ns
  export test_pod_name="consumer"-${env_uuid}
  consumer_pod_name=${test_pod_name}
  namesrv_svc=$(kubectl get svc -n ${ns} | grep nameserver | awk '{print $1}')
  export namesrv=${namesrv_svc}:9876

  # 定义 Consumer 执行命令
  TEST_CMD='sh mqadmin updatetopic -n $NAMESRV_ADDR -t TestTopic_$TIMESTAMP -c DefaultCluster && cd ../benchmark/ && sh consumer.sh -n $NAMESRV_ADDR -t TestTopic_$TIMESTAMP > /mnt/report/consumer_$TIMESTAMP.log 2>&1'
  export TEST_CMD
  envsubst < ./consumer_pod.yaml > ${consumer_pod_name}.yaml
  cat ${consumer_pod_name}.yaml
  sleep 5

  kubectl apply -f ${consumer_pod_name}.yaml -n ${ns} --validate=false
  kubectl wait --for=condition=Ready pod/${consumer_pod_name} -n ${ns} --timeout=300s
  kubectl exec -i ${consumer_pod_name} -n ${ns} -- /bin/sh -c "$TEST_CMD" &

  # 部署producer
  echo -e "${CLIENT_POD_TEMPLATE}" > ./producer_pod.yaml
  sed -i '1d' ./producer_pod.yaml

  export test_pod_name="producer"-${env_uuid}
  producer_pod_name=${test_pod_name}

  TEST_CMD='cd ../benchmark/ && sh producer.sh -n $NAMESRV_ADDR -t TestTopic_$TIMESTAMP > /mnt/report/producer_$TIMESTAMP.log 2>&1'
  export TEST_CMD
  envsubst < ./producer_pod.yaml > ${producer_pod_name}.yaml
  cat ${producer_pod_name}.yaml
  sleep 5

  kubectl apply -f ${producer_pod_name}.yaml -n ${ns} --validate=false
  kubectl wait --for=condition=Ready pod/${producer_pod_name} -n ${ns} --timeout=300s
  kubectl exec -i ${producer_pod_name} -n ${ns} -- /bin/sh -c "$TEST_CMD" &

  echo "Waiting for benchmark test done..."
  sleep ${TEST_TIME}

  # 停止benchmark测试
  kubectl exec -i ${consumer_pod_name} -n ${ns} -- /bin/sh -c "sh ../benchmark/shutdown.sh consumer"
  kubectl exec -i ${producer_pod_name} -n ${ns} -- /bin/sh -c "sh ../benchmark/shutdown.sh producer"

  # 收集报告
  path=$(pwd)
  mkdir -p ${path}/benchmark/
  report_path=${path}/benchmark/

  kubectl cp --retries=10 ${consumer_pod_name}:/mnt/report/ ${report_path} -n ${ns} 
  kubectl cp --retries=10 ${producer_pod_name}:/mnt/report/ ${report_path} -n ${ns} 
  sleep 10
  kubectl delete pod ${consumer_pod_name} -n ${ns}
  kubectl delete pod ${producer_pod_name} -n ${ns}

  # 处理数据，生成图表
  cd ${report_path}
  cp /benchmark/log_analysis.py ./log_analysis.py
  python3 log_analysis.py
  rm -f log_analysis.py && rm -f *.csv
  ls
  cd -
fi

if [ "${ACTION}" = "clean" ]; then
    echo "************************************"
    echo "*       Delete app and env...      *"
    echo "************************************"

    env=${env_uuid}

    vela delete ${env} -n ${env} -y
    all_pod_name=`kubectl get pods --no-headers -o custom-columns=":metadata.name" -n ${env}`
    for pod in $all_pod_name;
    do
      kubectl delete pod ${pod} -n ${env}
    done

    sleep 30

    kubectl proxy &
    PID=$!
    sleep 3

    DELETE_ENV=${env}

    vela env delete ${DELETE_ENV} -y
    sleep 3
    kubectl delete namespace ${DELETE_ENV} --wait=false
    kubectl get ns ${DELETE_ENV} -o json | jq '.spec.finalizers=[]' > ns-without-finalizers.json
    cat ns-without-finalizers.json
    curl -X PUT http://localhost:8001/api/v1/namespaces/${DELETE_ENV}/finalize -H "Content-Type: application/json" --data-binary @ns-without-finalizers.json

    kill $PID
fi