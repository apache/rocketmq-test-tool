#!/bin/sh
TOTAL_TIME=$1
INTERVAL=$2
DURATION=$3
SCRIPT_PATH="$4"
shift 4
SCRIPT_ARGS="$@"

calculate_initial_wait_time() {
  local total=$1
  local interval=$2
  local duration=$3
  local current_time=$total

  while [ $current_time -gt 0 ]; do
    if [ $current_time -ge $duration ]; then
      current_time=$((current_time - duration))
    else
      echo $current_time
      return
    fi

    if [ $current_time -gt $interval ]; then
      current_time=$((current_time - interval))
    else
      echo $current_time
      return
    fi
  done

}

initial_wait_time=$(calculate_initial_wait_time $TOTAL_TIME $INTERVAL $DURATION)
sleep $initial_wait_time

fault_inject_time=0
fault_total_time=$((TOTAL_TIME - initial_wait_time))
while [ $fault_inject_time -lt $fault_total_time ]; do

  # Inject fault
  nohup sh $SCRIPT_PATH $SCRIPT_ARGS > /dev/null 2>&1 &

  # Wait for the fault injection operation to complete
  sleep $DURATION

  # Waiting for the next fault injection
  sleep $interval

  fault_inject_time=$((fault_inject_time + INTERVAL + DURATION))
done