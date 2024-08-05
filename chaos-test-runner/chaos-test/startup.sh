#!/bin/sh
CHAOSMESH_YAML_FILE=$1
DURITION=$2
POD_NAME=$3
NS=$4
REPORT_DIR=$5
OPENCHAOS_ARGS=$6
INTERVAL=$7
LOG_FILE='/chaos-framework/report/chaos-mesh-fault'

cleanup() {
  echo "Test done."
  echo "Performing cleanup..."
  kubectl cp --retries=10 -n ${NS} --container=openchaos-controller ${POD_NAME}:/chaos-framework/report "$REPORT_DIR"
  sleep 5
  ls $REPORT_DIR
  kubectl delete deployment openchaos-controller -n ${NS}
  configmap=$(kubectl get pods -n ${NS} ${POD_NAME} -o jsonpath='{.spec.volumes[*].configMap.name}')
  kubectl delete cm ${configmap} -n ${NS}
  kubectl delete pod ${POD_NAME} -n ${NS}
  echo "Cleanup completed..."
  check_report
}

check_report() {
  resulet_file=$(ls ${REPORT_DIR}/*RocketMQ-chaos-queue-result-file 2>/dev/null | head -n 1)
  # Check report number
  file_count=$(ls -1q $REPORT_DIR/* | wc -l)
  if [ "$file_count" -lt 6 ]; then
    echo "Test failed: Insufficient report files."
    exit 1
  fi

  # Check for missing messages
  lostMessageCount=$(grep "lostMessageCount:" "$resulet_file" | awk '{print $2}')
  atMostOnce=$(grep "atMostOnce:" "$resulet_file" | awk '{print $2}')
  atLeastOnce=$(grep "atLeastOnce:" "$resulet_file" | awk '{print $2}')
  exactlyOnce=$(grep "exactlyOnce:" "$resulet_file" | awk '{print $2}')

  if [ "$lostMessageCount" -eq 0 ] && [ "$atMostOnce" = "true" ] && [ "$atLeastOnce" = "true" ] && [ "$exactlyOnce" = "true" ]; then
    echo "Test Passed: All conditions met."
    exit 0
  else
    echo "Test failed: Conditions not met."
    if [ "$lostMessageCount" -ne 0 ]; then
      echo "Error: lostMessageCount is not 0."
    fi
    if [ "$atMostOnce" != "true" ]; then
      echo "Error: atMostOnce is not true."
    fi
    if [ "$atLeastOnce" != "true" ]; then
      echo "Error: atLeastOnce is not true."
    fi
    if [ "$exactlyOnce" != "true" ]; then
      echo "Error: exactlyOnce is not true."
    fi
    exit 1
  fi

}

trap cleanup EXIT

# Test 
echo "Running chaos test..."

# Extract the value of the -t parameter from OPENCHAOS_ARGS
total_time=$(echo "$OPENCHAOS_ARGS" | sed -n 's/.*-t \([0-9]*\).*/\1/p')
if [ -z "$total_time" ]; then
  echo "Error: -t parameter not found in OPENCHAOS_ARGS."
  exit 1
fi

# Start openchaos
kubectl exec -i ${POD_NAME} -n ${NS} -c openchaos-controller -- /bin/sh -c "./start-openchaos.sh --driver driver-rocketmq/openchaos-driver.yaml --output-dir ./report $OPENCHAOS_ARGS" > "$REPORT_DIR/output.log" 2>&1 &

if [ -n "$INTERVAL" ] && [ "$INTERVAL" != "" ] && [ -n "$DURITION" ]; then
# Use interval scheduler
  ./interval-scheduler.sh $total_time $INTERVAL $DURITION /chaos-test/inject-fault.sh  "$CHAOSMESH_YAML_FILE" "$LOG_FILE" "$DURITION" "$POD_NAME" "$NS"
else
  echo "Error: INTERVAL and DURITION must be provided."
  exit 1
fi

wait