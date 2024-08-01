#!/bin/sh

CHAOSMESH_YAML_FILE=$1  # e.g., "/path/to/chaos_experiment.yaml"
LOG_FILE=$2  # e.g., "/path/to/chaos_mesh_log.txt"
LIMIT_TIME=$3
POD_NAME=$4
NS=$5

export KUBECTL_PATH=/usr/local/bin/kubectl


if [ -z "$CHAOSMESH_YAML_FILE" ] || [ -z "$LOG_FILE" ] || [ -z "$LIMIT_TIME" ] || [ -z "$POD_NAME" ] || [ -z "$NS" ]; then
  echo "Usage: $0 <chaos_experiment.yaml> <log_file> <limit_time> <pod_name> <namespace>"
  exit 1
fi


if [ ! -f "$CHAOSMESH_YAML_FILE" ]; then
  echo "Chaos Mesh YAML file not found: $CHAOSMESH_YAML_FILE"
  exit 1
fi

current_millis() {
  echo $(( $(date +%s%N) / 1000000 ))
}

log_fault_event() {
  event_type=$1
  fault_type=$2
  timestamp=$(current_millis)
  $KUBECTL_PATH exec -i $POD_NAME -n ${NS} -c sidecar-container -- /bin/sh -c "echo -e 'fault\t$fault_type\t$event_type\t$timestamp' >> $LOG_FILE"
}

inject_fault() {
  echo "injecting fault..."
  log_fault_event "start" "chaos-mesh-fault"
  if $KUBECTL_PATH apply -f $CHAOSMESH_YAML_FILE; then
    echo "Fault injected successfully"
  else
    echo "Failed to inject fault"
    log_fault_event "error" "chaos-mesh-fault"
  fi
}

clear_fault() {
  echo "cleaning fault..."
  if $KUBECTL_PATH delete -f $CHAOSMESH_YAML_FILE; then
    log_fault_event "end" "chaos-mesh-fault"
  else
    echo "Failed to clear fault"
    log_fault_event "error_clear" "chaos-mesh-fault"
  fi
}

inject_fault

# Wait for a period of time equal to the duration of a single fault
sleep $LIMIT_TIME

clear_fault
