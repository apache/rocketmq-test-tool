#!/bin/sh
CRON=$1
CHAOSMESH_YAML_FILE=$2
LIMIT_TIME=$3
POD_NAME=$4
NS=$5
REPORT_DIR=$6
OPENCHAOS_ARGS=$7
LOG_FILE='/chaos-framework/report/chaos-mesh-fault'


cleanup() {
  echo "Test done."
  echo "Performing cleanup..."
  crontab -r 2> /dev/null
  kubectl cp -n ${NS} --container=openchaos-controller ${POD_NAME}:/chaos-framework/report "$REPORT_DIR"
  ls $REPORT_DIR
  kubectl delete deployment openchaos-controller -n ${NS}
  kubectl delete pod ${POD_NAME} -n ${NS}
  echo "Cleanup completed..."
}

trap cleanup EXIT

# Test 
echo "Running chaos test..."

# Start openchaos
kubectl exec -i ${POD_NAME} -n ${NS} -c openchaos-controller -- /bin/sh -c "./start-openchaos.sh --driver driver-rocketmq/openchaos-driver.yaml --output-dir ./report $OPENCHAOS_ARGS" > "$REPORT_DIR/output.log" 2>&1 &

# Start cron scheduler , the script path must use absolute path
./cron-scheduler.sh "$CRON" /chaos-test/inject_fault_cron.sh  "$CHAOSMESH_YAML_FILE" "$LOG_FILE" "$LIMIT_TIME" "$POD_NAME" "$NS"

wait