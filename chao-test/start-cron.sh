#!/bin/sh
CHAOSMESH_YAML_FILE=$1
LOG_FILE=$2
LIMIT_TIME=$3
POD_NAME=$4
ns=$5
REPORT_DIR=$6
CRON='* * * * *'



cleanup() {
  echo "Test done."
  echo "Performing cleanup..."
  crontab -l
  crontab -r
  kubectl cp -n ${ns} --container=openchaos-controller $POD_NAME:/chaos-framework/report "$REPORT_DIR"
  ls $REPORT_DIR
  kubectl delete deployment openchaos-controller -n ${ns}
  kubectl delete pod $POD_NAME -n ${ns}
  echo "Cleanup completed..."
}

# 设置 trap 捕获脚本退出或中断信号
trap cleanup EXIT

# test 
echo "Running chaos test..."
touch output.log
kubectl exec -i $POD_NAME -n ${ns} -c openchaos-controller -- /bin/sh -c "./start-openchaos.sh --driver driver-rocketmq/rocketmq.yaml -u rocketmq --output-dir ./report -t 180" > output.log 2>&1 &
# start openchaos
# kubectl exec -it $POD_NAME -n ${ns} -c openchaos-controller -- /bin/sh -c "./start-openchaos.sh --driver driver-rocketmq/rocketmq.yaml -u rocketmq --output-dir ./report -t 180" &
# sleep 10
# sh /root/chaos-test/inject_fault_cron.sh "$CHAOSMESH_YAML_FILE" "$LOG_FILE" "$LIMIT_TIME" "$POD_NAME" "$ns"
# start cron scheduler , the script path must use absolute path
./cron-scheduler.sh '* * * * *' /chaos-test/inject_fault_cron.sh "$REPORT_DIR/cron-log.txt" "$CHAOSMESH_YAML_FILE" "$LOG_FILE" "$LIMIT_TIME" "$POD_NAME" "$ns"

# 等待后台进程完成
wait
