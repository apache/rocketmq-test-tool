#!/bin/sh
CHAOSMESH_YAML_FILE=$1  
LOG_FILE=$2  
DURITION=$3 
POD_NAME=$4
NS=$5

export KUBECTL_PATH=/usr/local/bin/kubectl


if [ -z "$CHAOSMESH_YAML_FILE" ] || [ -z "$LOG_FILE" ] || [ -z "$DURITION" ] || [ -z "$POD_NAME" ] || [ -z "$NS" ]; then
  echo "Usage: $0 <chaos_experiment.yaml> <log_file> <DURITION> <pod_name> <namespace>"
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
  if $KUBECTL_PATH apply -f $CHAOSMESH_YAML_FILE; then
    log_fault_event "start" "chaos-mesh-fault"
  else
    log_fault_event "error" "chaos-mesh-fault"
  fi
}

clear_fault() {
  if $KUBECTL_PATH delete -f $CHAOSMESH_YAML_FILE; then
    log_fault_event "end" "chaos-mesh-fault"
  else
    log_fault_event "error_clear" "chaos-mesh-fault"
  fi
}

inject_fault

# Wait for a period of time equal to the duration of a single fault
sleep $DURITION

clear_fault
