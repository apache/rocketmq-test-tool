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
RUNTIME_PARAM=${25}
SOCKET_PATH_PARAM=${26}

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

# Install helm
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
  let count=0
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
      let count=${count}+1
  done
fi

if [ ${ACTION} == "chaos-test" ]; then
    echo "************************************"
    echo "*         Chaos test...            *"
    echo "************************************"

    # Deploy chaos-mesh
    helm repo add chaos-mesh https://charts.chaos-mesh.org
    kubectl create ns "${chaos_mesh_ns}"
    container_runtime=$(kubectl get nodes -o jsonpath='{.items[0].status.nodeInfo.containerRuntimeVersion}')
    # Default to /var/run/docker.sock
    runtime="docker"
    socket_path="/var/run/docker.sock"

    # Set the value according to the runtime
    if echo "$container_runtime" | grep -q "docker"; then
      runtime="docker"
      socket_path="/var/run/docker.sock"
    elif echo "$container_runtime" | grep -q "containerd"; then
      runtime="containerd"
      
      kubelet_version=$(kubectl get nodes -o jsonpath='{.items[0].status.nodeInfo.kubeletVersion}')
      if echo "$kubelet_version" | grep -q "k3s"; then
        socket_path="/run/k3s/containerd/containerd.sock"
      elif echo "$kubelet_version" | grep -q "microk8s"; then
        socket_path="/var/snap/microk8s/common/run/containerd.sock"
      else
        socket_path="/run/containerd/containerd.sock"
      fi

    elif echo "$container_runtime" | grep -q "crio"; then
      runtime="crio"
      socket_path="/var/run/crio/crio.sock"
    else
      if [ -n "$RUNTIME_PARAM" ] && [ "$RUNTIME_PARAM" != "" ] && [ -n "$SOCKET_PATH_PARAM" ] && [ "$SOCKET_PATH_PARAM" != "" ]; then
        runtime=$RUNTIME_PARAM
        socket_path=$SOCKET_PATH_PARAM
      else
        echo "Error : Unable to detect cri,please manually specify runtime and socket path."
        exit 1
      fi
    fi
    
    helm install chaos-mesh chaos-mesh/chaos-mesh --namespace=${chaos_mesh_ns} --set chaosDaemon.runtime=$runtime --set chaosDaemon.socketPath=$socket_path --version 2.6.3
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
    # openchaos
    app=${env_uuid}
    kubectl create configmap ${app}-configmap --from-file=openchaos-driver.yaml --namespace=${env_uuid} -o yaml --dry-run=client >  ${app}-configmap.yaml
    cat ./${app}-configmap.yaml
    kubectl apply -f ./${app}-configmap.yaml -n ${env_uuid}
    
    configmap_name="${app}-configmap"
    export configmap_name
    envsubst < /chaos-test/chaos-controller-template.yaml > ./chaos-controller.yaml

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

    vela delete ${env} -n ${env} -y
    
    helm uninstall chaos-mesh -n ${chaos_mesh_ns}
    sleep 10

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

    vela env delete ${DELETE_ENV} -y
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