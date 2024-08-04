#!/bin/sh -l
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
OPENCHAOS_DRIVER=${15}
CHAOSMESH_YAML_FILE=${16}
OPENCHAOS_ARGS=${17}
FAULT_DURITION=${18}
FAULT_SCHEDULER_INTERVAL=${19}
HELM_CHART_REPO=${20}
HELM_CHART_VERSION=${21}
CHART=${22}
NODE_LABLE=${23}
META_NODE_LABLE=${24}

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

echo "Start test version: ${GITHUB_REPOSITORY}@${VERSION}"

echo "************************************"
echo "*          Set config...           *"
echo "************************************"
mkdir -p ${HOME}/.kube
# kube_config=$(echo "${ASK_CONFIG}" | base64 -d)
# TODO : use kubevela
kube_config=$(echo "${ASK_CONFIG}")
echo "${kube_config}" > ${HOME}/.kube/config
export KUBECONFIG="${HOME}/.kube/config"

# install helm
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh
helm version

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

env_uuid=${REPO_NAME}-${GITHUB_RUN_ID}-${JOB_INDEX}
chaos_mesh_ns="chaos-mesh-${GITHUB_RUN_ID}-${JOB_INDEX}"


check_pods_status() {
  local namespace=$1

  pods_status=$(kubectl get pods -n ${namespace} -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.status.phase}{"\t"}{range .status.conditions[?(@.type=="Ready")]}{.status}{"\n"}{end}{end}')
  all_ready=true

  echo "$pods_status" > /tmp/pods_status.txt

  while read -r pod; do
    pod_name=$(echo "$pod" | awk '{print $1}')
    pod_phase=$(echo "$pod" | awk '{print $2}')
    pod_ready=$(echo "$pod" | awk '{print $3}')

    if [ "$pod_phase" != "Running" ] || [ "$pod_ready" != "True" ]; then
      echo "Pod $pod_name is not ready (Phase: $pod_phase, Ready: $pod_ready)"
      all_ready=false
    fi
  done < /tmp/pods_status.txt

  if [ "$all_ready" = "true" ]; then
    return 0
  else
    return 1
  fi
}

wait_for_pods_ready() {
  local namespace=$1
  local timeout=$2
  local count=0

  while true; do
    if check_pods_status ${namespace}; then
      echo "All Pods are ready"
      kubectl get pods -n "${namespace}"
      break
    fi

    if [ $count -gt $timeout ]; then
      echo "Deployment timeout..."
      exit 1
    fi

    echo "Waiting for Pods to be ready..."
    sleep 5
    count=$((count + 1))
  done
}

if [ ${ACTION} == "deploy" ]; then
  echo "************************************"
  echo "*     Create env and deploy...     *"
  echo "************************************"

  echo ${VERSION}: ${env_uuid} deploy start

  # vela env init ${env_uuid} --namespace ${env_uuid}

  export VELA_APP_NAME=${env_uuid}
  envsubst < ./velaapp.yaml > velaapp-${REPO_NAME}.yaml
  cat velaapp-${REPO_NAME}.yaml

  # vela env set ${env_uuid}
  # vela up -f "velaapp-${REPO_NAME}.yaml"

  app=${env_uuid}
  kubectl create ns ${env_uuid}
  helm repo add my_rocketmq ${HELM_CHART_REPO}
  helm repo update
  helm install ${app} -n ${env_uuid} my_rocketmq/${CHART} --version ${HELM_CHART_VERSION}
  
  check_helm_release_status() {
  status=$(helm status ${app} -n ${env_uuid} | grep "STATUS:" | awk '{print $2}')
  if [ "${status}" == "deployed" ]; then
    return 0
  else
    return 1
  fi
}

count=0
while true; do
  if check_helm_release_status && check_pods_status ${env_uuid}; then
    echo "Helm release and all Pods are ready"
    kubectl get pods -n ${env_uuid}
    break
  fi

  if [ $count -gt 240 ]; then
    echo "Deployment timeout..."
    exit 1
  fi

  echo "Waiting for Helm release and Pods to be ready..."
  sleep 5
  count=$((count + 1))
done

#   status=`vela status ${app} -n ${app}`
#   echo $status
#   res=`echo $status | grep "Create helm release successfully"`
#   let count=0
#   while [ -z "$res" ]
#   do
#       if [ $count -gt 240 ]; then
#         echo "env ${app} deploy timeout..."
#         exit 1
#       fi
#       echo "waiting for env ${app} ready..."
#       sleep 5
#       status=`vela status ${app} -n ${app}`
#       stopped=`echo $status | grep "not found"`
#       if [ ! -z "$stopped" ]; then
#           echo "env ${app} deploy stopped..."
#           exit 1
#       fi
#       res=`echo $status | grep "Create helm release successfully"`
#       let count=${count}+1
#   done
fi

TEST_POD_TEMPLATE='
apiVersion: v1
kind: Pod
metadata:
  name: ${test_pod_name}
  namespace: ${ns}
spec:
  restartPolicy: Never
  containers:
  - name: ${test_pod_name}
    image: cloudnativeofalibabacloud/test-runner:v0.0.3
    resources:
          limits:
            cpu: "8"
            memory: "8Gi"
          requests:
            cpu: "8"
            memory: "8Gi"
    env:
    - name: CODE
      value: ${TEST_CODE_GIT}
    - name: BRANCH
      value: ${TEST_CODE_BRANCH}
    - name: CODE_PATH
      value: ${TEST_CODE_PATH}
    - name: ALL_IP
      value: ${ALL_IP}
    - name: CMD
      value: |
${TEST_CMD}
'

echo -e "${TEST_POD_TEMPLATE}" > ./testpod.yaml
sed -i '1d' ./testpod.yaml

if [ ${ACTION} == "test" ]; then
  echo "************************************"
  echo "*            E2E Test...           *"
  echo "************************************"

  ns=${env_uuid}
  test_pod_name=test-${env_uuid}-${RANDOM}
  export test_pod_name

  echo namespace: $ns
  all_pod_name=`kubectl get pods --no-headers -o custom-columns=":metadata.name" -n ${ns}`
  ALL_IP=""
  for pod in $all_pod_name;
  do
      if [ ! -z `echo "${pod}" | grep "test-${env_uuid}"` ]; then
        continue
      fi
      POD_HOST=$(kubectl get pod ${pod} --template={{.status.podIP}} -n ${ns})
      ALL_IP=${pod}:${POD_HOST},${ALL_IP}
  done

  echo $ALL_IP
  echo $TEST_CODE_GIT
  echo $TEST_CMD_BASE

  export ALL_IP
  export ns

  TEST_CMD=`echo "${TEST_CMD_BASE}" | sed -s 's/^/        /g'`

  echo $TEST_CMD
  export TEST_CMD

  envsubst < ./testpod.yaml > ./testpod-${ns}.yaml
  cat ./testpod-${ns}.yaml

  kubectl apply -f ./testpod-${ns}.yaml

  sleep 5

  pod_status=`kubectl get pod ${test_pod_name} --template={{.status.phase}} -n ${ns}`
  if [ -z "$pod_status" ]; then
      pod_status="Pending"
  fi

  while [ "${pod_status}" == "Pending" ] || [ "${pod_status}" == "Running" ]
  do
      echo waiting for ${test_pod_name} test done...
      sleep 5
      pod_status=`kubectl get pod ${test_pod_name} --template={{.status.phase}} -n ${ns}`
      if [ -z "$pod_status" ]; then
          pod_status="Pending"
      fi
      test_done=`kubectl exec -i ${test_pod_name} -n ${ns} -- ls /root | grep testdone`
      if [ ! -z "$test_done" ]; then
        echo "Test status: test done"
          if [ ! -d "./test_report" ]; then
            echo "Copy test reports"
            kubectl cp --retries=10 ${test_pod_name}:/root/testlog.txt testlog.txt -n ${ns}
            mkdir -p test_report
            cd test_report
            kubectl cp --retries=10 ${test_pod_name}:/root/code/${TEST_CODE_PATH}/target/surefire-reports/. . -n ${ns}
            rm -rf *.txt
            ls
            cd -
          fi
      fi
  done

  exit_code=`kubectl get pod ${test_pod_name} --output="jsonpath={.status.containerStatuses[].state.terminated.exitCode}" -n ${ns}`
  kubectl delete pod ${test_pod_name} -n ${ns}
  echo E2E Test exit code: ${exit_code}
  exit ${exit_code}
fi

if [ ${ACTION} == "test_local" ]; then
  echo "************************************"
  echo "*        E2E Test local...         *"
  echo "************************************"

  wget https://dlcdn.apache.org/maven/maven-3/3.8.7/binaries/apache-maven-3.8.7-bin.tar.gz
  tar -zxvf apache-maven-3.8.7-bin.tar.gz -C /opt/
  export PATH=$PATH:/opt/apache-maven-3.8.7/bin

  ns=${env_uuid}

  echo namespace: $ns
  all_pod_name=`kubectl get pods --no-headers -o custom-columns=":metadata.name" -n ${ns}`
  ALL_IP=""
  for pod in $all_pod_name;
  do
      label=`kubectl get pod ${pod} --output="jsonpath={.metadata.labels.app\.kubernetes\.io/name}" -n ${ns}`
      pod_port=`kubectl get -o json services --selector="app.kubernetes.io/name=${label}" -n ${ns} | jq -r '.items[].spec.ports[].port'`
      echo "${pod}: ${pod_port}"
      for port in ${pod_port};
      do
          kubectl port-forward ${pod} ${port}:${port} -n ${ns} &
          res=$?
          if [ ${res} -ne 0 ]; then
            echo "kubectl port-forward error: ${pod} ${port}:${port}"
            exit ${res}
          fi
      done
      ALL_IP=${pod}:"127.0.0.1",${ALL_IP}
      sleep 3
  done

  echo $ALL_IP
  echo $TEST_CODE_GIT
  echo $TEST_CMD_BASE

  export ALL_IP
  export ns
  is_mvn_cmd=`echo $TEST_CMD_BASE | grep "mvn"`
  if [ ! -z "$is_mvn_cmd" ]; then
      TEST_CMD="$TEST_CMD_BASE -DALL_IP=${ALL_IP}"
  else
      TEST_CMD=$TEST_CMD_BASE
  fi
  echo $TEST_CMD

  git clone $TEST_CODE_GIT -b $TEST_CODE_BRANCH code

  cd code
  cd $TEST_CODE_PATH
  ${TEST_CMD}
  exit_code=$?

  killall kubectl
  exit ${exit_code}
fi

if [ ${ACTION} == "chaos-test" ]; then
    echo "************************************"
    echo "*         Chaos test...            *"
    echo "************************************"

    # Deploy chaos-mesh
    helm repo add chaos-mesh https://charts.chaos-mesh.org
    kubectl create ns "${chaos_mesh_ns}"
    helm install chaos-mesh chaos-mesh/chaos-mesh -n="${chaos_mesh_ns}" --set chaosDaemon.runtime=containerd --set chaosDaemon.socketPath=/run/containerd/containerd.sock --version 2.6.3
    sleep 10

    # Check chaos-mesh pod status
    wait_for_pods_ready ${chaos_mesh_ns} 240
    
    # Deploy a pod for test ：openchaos-controller
    # ConfigMap
    openchaos_driver_file=$(cat "$OPENCHAOS_DRIVER")
    echo -e "$openchaos_driver_file" > ./openchaos-driver-template.yaml

    # Replace the placeholders in the configuration file with the ip of the worker node and the metaNode node
    node_ips=$(kubectl get pods -n ${env_uuid} -l "$NODE_LABLE" -o jsonpath='{.items[*].status.podIP}')
    set -- $node_ips

    i=1
    for ip in "$@"; do
      export node_$i=$ip
      i=$((i + 1))
    done

    meta_node_ips=$(kubectl get pods -n ${env_uuid} -l "$META_NODE_LABLE" -o jsonpath='{.items[*].status.podIP}')
    set -- $meta_node_ips 

    i=1
    for ip in "$@"; do
      export meta_node_$i=$ip
      i=$((i + 1))
    done
    envsubst < ./openchaos-driver-template.yaml > ./openchaos-driver.yaml
    
    app=${env_uuid}
    kubectl create configmap ${app}-configmap --from-file=openchaos-driver.yaml --namespace=${env_uuid} -o yaml --dry-run=client >  ${app}-configmap.yaml
    cat ./${app}-configmap.yaml
    kubectl apply -f ./${app}-configmap.yaml -n ${env_uuid}
    
    configmap_name="${app}-configmap"
    export configmap_name
    envsubst < /chaos-test/openchaos/chaos-controller-template.yaml > ./chaos-controller.yaml

    kubectl apply -f ./chaos-controller.yaml -n ${env_uuid}
    sleep 10
    test_pod_name=$(kubectl get pods -n ${env_uuid} -l app=openchaos-controller -o jsonpath='{.items[0].metadata.name}')
    
    wait_for_pods_ready ${env_uuid} 240
   
    chaosmesh_yaml_template=$(cat "$CHAOSMESH_YAML_FILE")
    echo -e "${chaosmesh_yaml_template}" > ./chaos-mesh-fault.yaml

    ns=${env_uuid}
    export app
    export ns
    envsubst < ./chaos-mesh-fault.yaml > ./network-chaos.yaml
    fault_file="$(pwd)/network-chaos.yaml"
    # Check fault file
    cat $fault_file
    # Execute the startup script
    mkdir -p chaos-test-report
    REPORT_DIR="$(pwd)/chaos-test-report"
    touch $REPORT_DIR/output.log
    
    cd /chaos-test
    sh ./startup.sh "$fault_file" "$FAULT_DURITION" "$test_pod_name" "$ns" "$REPORT_DIR" "$OPENCHAOS_ARGS" "$FAULT_SCHEDULER_INTERVAL"
    exit_code=$?
    cd -
    exit ${exit_code}
fi


if [ ${ACTION} == "clean" ]; then
    echo "************************************"
    echo "*       Delete app and env...      *"
    echo "************************************"

    env=${env_uuid}
    app=${env_uuid}

    # vela delete ${env} -n ${env} -y
    
    helm uninstall ${app} -n ${env}
    helm uninstall chaos-mesh -n ${chaos_mesh_ns}

    delete_pods_in_namespace() {
      local namespace=$1
      all_pod_name=$(kubectl get pods --no-headers -o custom-columns=":metadata.name" -n ${namespace})
      for pod in ${all_pod_name}; do
          kubectl delete pod ${pod} -n ${namespace} --grace-period=30 --wait=true
      done
      kubectl wait --for=delete pod --all -n ${namespace} --timeout=60s
    }

    for ns in ${chaos_mesh_ns} ${env}; do
      pod_count=$(kubectl get pods -n ${ns} --no-headers | wc -l)
      if [ "$pod_count" -gt 0 ]; then
        echo "Pods exist in namespace ${ns}, deleting..."
        delete_pods_in_namespace ${ns}
      else
        echo "No pods found in namespace ${ns}"
      fi
    done

    kubectl proxy &
    PID=$!
    sleep 3

    DELETE_ENV=${env}

    # vela env delete ${DELETE_ENV} -y
    sleep 3
    # Delete namespaces if they exist
    for ns in ${chaos_mesh_ns} ${DELETE_ENV}; do
        if kubectl get namespace ${ns}; then
            kubectl delete namespace ${ns} --wait=false
            # Remove finalizers
            kubectl get ns ${ns} -o json | jq '.spec.finalizers=[]' > ns-without-finalizers.json
            cat ns-without-finalizers.json
            curl -X PUT http://localhost:8001/api/v1/namespaces/${ns}/finalize -H "Content-Type: application/json" --data-binary @ns-without-finalizers.json
        fi
    done
    
    kill $PID
fi

if [ ${ACTION} == "try" ]; then
  kubectl get pods --all-namespaces
fi


