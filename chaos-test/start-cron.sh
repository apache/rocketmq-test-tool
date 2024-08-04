#!/bin/sh
CRON=$1
CHAOSMESH_YAML_FILE=$2
DURITION=$3
POD_NAME=$4
NS=$5
REPORT_DIR=$6
OPENCHAOS_ARGS=$7
INTERVAL=$8
LOG_FILE='/chaos-framework/report/chaos-mesh-fault'


cleanup() {
  echo "Test done."
  echo "Performing cleanup..."
  crontab -r 2> /dev/null
  kubectl cp -n ${NS} --container=openchaos-controller ${POD_NAME}:/chaos-framework/report "$REPORT_DIR"
  ls $REPORT_DIR
  kubectl delete deployment openchaos-controller -n ${NS}
  configmap=$(kubectl get pods -n ${NS} ${POD_NAME} -o jsonpath='{.spec.volumes[*].configMap.name}')
  kubectl delete cm ${configmap} -n ${NS}
  kubectl delete pod ${POD_NAME} -n ${NS}
  echo "Cleanup completed..."
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
OPENCHAOS_PID=$!

if [ -n "$CRON" ] && [ "$CRON" != "" ]; then
# Start cron scheduler , the script path must use absolute path
  ./cron-scheduler.sh "$CRON" /chaos-test/inject-fault.sh  "$CHAOSMESH_YAML_FILE" "$LOG_FILE" "$DURITION" "$POD_NAME" "$NS"
elif [ -n "$INTERVAL" ] && [ "$INTERVAL" != "" ] && [ -n "$DURITION" ]; then
  ./interval-scheduler.sh $total_time $INTERVAL $DURITION /chaos-test/inject-fault.sh  "$CHAOSMESH_YAML_FILE" "$LOG_FILE" "$DURITION" "$POD_NAME" "$NS"
else
  echo "Error: Either CRON or INTERVAL must be provided, but not both."
  exit 1
fi


OPENCHAOS_EXIT_CODE=$?
if [ $OPENCHAOS_EXIT_CODE -ne 0 ]; then
  echo "OpenChaos process failed."
  echo "Check the error log for details: $REPORT_DIR/error.log"
  exit $OPENCHAOS_EXIT_CODE
fi

wait