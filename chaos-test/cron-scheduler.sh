#!/bin/sh

# 检查是否有两个参数
if [ "$#" -lt 2 ]; then
    echo "Usage: $0 '<cron_expression>' '<script_path> [script_args...]'"
    exit 1
fi

# 获取cron表达式和脚本路径
CRON_EXPR="$1"
SCRIPT_PATH="$2"
LOG_FILE="$3"
# 脚本参数
shift 3 
SCRIPT_ARGS="$@"

chmod +x $SCRIPT_PATH

KUBECONFIG_PATH=$(printenv KUBECONFIG)

# 构建cron作业的命令
CRON_JOB="$CRON_EXPR KUBECONFIG=$KUBECONFIG_PATH $SCRIPT_PATH $SCRIPT_ARGS >> $LOG_FILE 2>&1"

# 查看现有的cron作业
(crontab -l 2>/dev/null; echo "$CRON_JOB") | sort - | uniq - | crontab -

echo "$CRON_JOB added"