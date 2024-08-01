#!/bin/sh

if [ "$#" -lt 2 ]; then
    echo "Usage: $0 '<cron_expression>' '<script_path> [script_args...]'"
    exit 1
fi


CRON_EXPR="$1"
SCRIPT_PATH="$2"
shift 2 
SCRIPT_ARGS="$@"

chmod +x $SCRIPT_PATH

KUBECONFIG_PATH=$(printenv KUBECONFIG)

# Start a cron job
CRON_JOB="$CRON_EXPR KUBECONFIG=$KUBECONFIG_PATH $SCRIPT_PATH $SCRIPT_ARGS"

# Ensure there are no duplicate cron jobs
(crontab -l 2>/dev/null; echo "$CRON_JOB") | sort - | uniq - | crontab -

echo "$CRON_JOB added"