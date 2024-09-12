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
JOB_INDEX=${9}
HELM_VALUES=${10}
TEST_TIME=${11}

export VERSION
export CHART_GIT
export CHART_BRANCH
export CHART_PATH
export REPO_NAME=`echo ${GITHUB_REPOSITORY#*/} | sed -e "s/\//-/g" | cut -c1-36 | tr '[A-Z]' '[a-z]'`
export WORKFLOW_NAME=${GITHUB_WORKFLOW}
export RUN_ID=${GITHUB_RUN_ID}
export YAML_VALUES=`echo "${HELM_VALUES}" | sed -s 's/^/          /g'`

#tmq集群 连接rocke
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
  rm -f log_analysis.py consumer_performance_data.csv producer_performance_data.csv
  ls

  # 判断 CI 是否通过
  consumer_benchmark="consumer_benchmark_result.csv"
  producer_benchmark="producer_benchmark_result.csv"

  # 打印测试结果
  echo "====================benchmark result===================="
  echo "Consumer benchmark result: "
  if [ -f ${consumer_benchmark} ]; then
      cat ${consumer_benchmark}
  else
      echo "Consumer benchmark file not found."
  fi
  echo "===================================="
  echo "Producer benchmark result: "
  if [ -f ${producer_benchmark} ]; then
      cat ${producer_benchmark}
  else
      echo "Producer benchmark file not found."
  fi
  echo "========================================================"

  # Producer 阈值
  MIN_SEND_TPS_THRESHOLD=19000
  MAX_RT_MS_THRESHOLD=700
  AVG_RT_MS_THRESHOLD=4

  # Consumer 阈值
  MIN_CONSUME_TPS_THRESHOLD=19000
  MAX_S2C_RT_MS_THRESHOLD=60
  MAX_B2C_RT_MS_THRESHOLD=60
  AVG_S2C_RT_MS_THRESHOLD=1.5
  AVG_B2C_RT_MS_THRESHOLD=1.5

  get_csv_value() {
      local file=$1
      local metric=$2
      local column=$3
      awk -F',' -v metric="$metric" -v column="$column" '
      $1 == metric {print $column; exit}
      ' "$file"
  }

  # 获取相关指标
  consume_tps_min=$(get_csv_value "$consumer_benchmark" "Consume TPS" 2)
  max_s2c_rt=$(get_csv_value "$consumer_benchmark" "MAX(S2C) RT (ms)" 2)
  max_b2c_rt=$(get_csv_value "$consumer_benchmark" "MAX(B2C) RT (ms)" 2)
  avg_s2c_rt=$(get_csv_value "$consumer_benchmark" "AVG(S2C) RT (ms)" 2)
  avg_b2c_rt=$(get_csv_value "$consumer_benchmark" "AVG(B2C) RT (ms)" 2)

  send_tps_min=$(get_csv_value "$producer_benchmark" "Send TPS" 2)
  max_rt=$(get_csv_value "$producer_benchmark" "Max RT (ms)" 2)
  avg_rt=$(get_csv_value "$producer_benchmark" "Average RT (ms)" 2)

  # 校验 Consumer 阈值
  consumer_tps_pass=$(awk -v a="$consume_tps_min" -v b="$MIN_CONSUME_TPS_THRESHOLD" 'BEGIN {print (a >= b) ? "true" : "false"}')
  consumer_latency_pass=$(awk -v max_s2c="$max_s2c_rt" -v max_b2c="$max_b2c_rt" -v avg_s2c="$avg_s2c_rt" -v avg_b2c="$avg_b2c_rt" \
      -v max_s2c_thr="$MAX_S2C_RT_MS_THRESHOLD" -v max_b2c_thr="$MAX_B2C_RT_MS_THRESHOLD" -v avg_s2c_thr="$AVG_S2C_RT_MS_THRESHOLD" -v avg_b2c_thr="$AVG_B2C_RT_MS_THRESHOLD" \
      'BEGIN {print (max_s2c <= max_s2c_thr && max_b2c <= max_b2c_thr && avg_s2c <= avg_s2c_thr && avg_b2c <= avg_b2c_thr) ? "true" : "false"}')

  # 校验 Producer 阈值
  producer_tps_pass=$(awk -v a="$send_tps_min" -v b="$MIN_SEND_TPS_THRESHOLD" 'BEGIN {print (a >= b) ? "true" : "false"}')
  producer_latency_pass=$(awk -v max_rt="$max_rt" -v avg_rt="$avg_rt" -v max_rt_thr="$MAX_RT_MS_THRESHOLD" -v avg_rt_thr="$AVG_RT_MS_THRESHOLD" \
      'BEGIN {print (max_rt <= max_rt_thr && avg_rt <= avg_rt_thr) ? "true" : "false"}')

  # 判断测试结果是否通过
  if [ "$consumer_tps_pass" = "true" ] && [ "$consumer_latency_pass" = "true" ] && \
    [ "$producer_tps_pass" = "true" ] && [ "$producer_latency_pass" = "true" ]; then
      echo "All benchmarks passed."
      exit 0
  else
      echo "One or more benchmarks failed."
      [ "$consumer_tps_pass" = "false" ] && echo "Consumer TPS test failed."
      [ "$consumer_latency_pass" = "false" ] && echo "Consumer latency test failed."
      [ "$producer_tps_pass" = "false" ] && echo "Producer TPS test failed."
      [ "$producer_latency_pass" = "false" ] && echo "Producer latency test failed."
      exit 1
  fi
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