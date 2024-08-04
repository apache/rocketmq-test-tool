# Apache RocketMQ Test Tool

This tool uses Helm and KubeVela to deploy applications and execute E2E tests in Kubernetes.
KubeVela needs to be installed in Kubernetes before use.

# Usage

<!-- start usage -->
## Use helm chart to deploy your app in Kubernetes
```yaml
  - uses: apache/rocketmq-test-tool@v1
    name: Deploy
    with:
      action: "deploy"
      ask-config: "${{ secrets.KUBE_CONFIG }}"
      test-version: "v1.0"
      chart-git: "https://github.com/your-helm-chart.git"
      chart-branch: "main"
      chart-path: "."
      job-id: 1
      helm-values: |
        app:
          image:
            repository: ${{env.DOCKER_REPO}}
            tag: v1.0
```
## Execute your E2E test
```yaml
  - uses: apache/rocketmq-test-tool@v1
    name: e2e test
    with:
      action: "test"
      ask-config: "${{ secrets.KUBE_CONFIG }}"
      test-version: "v1.0"
      test-code-git: "https://github.com/your-e2e-test.git"
      test-code-branch: "main"
      test-code-path: ./
      test-cmd: "your test command"
      job-id: 1
  - uses: actions/upload-artifact@v3
    if: always()
    name: Upload test log
    with:
      name: testlog.txt
      path: testlog.txt
```
## Perform chaos tests in Kubernetes
```yaml
  - name: Checkout repository
    uses: actions/checkout@v2
  - uses: chi3316/rocketmq-test-tool@2aaa80a004acd0831645852987213dbdff61ce53
    name: Chaos test
    with:
      action: "chaos-test"
      ask-config: "${{ secrets.KUBE_CONFIG }}"
      job-id: 1
      openchaos-driver: ".github/chaos-configs/driver.yaml"
      chaos-mesh-fault-file: ".github/chaos-configs/network-delay.yaml"
      fault-scheduler-interval: "30"
      openchaos-args: "-t 240"
      fault-durition: "30"
   - uses: actions/upload-artifact@v4
     with:
     name: chaos-test-report
     path: chaos-test-report/
```
**Scheduling Fault Injection：**
You can schedule fault injection using fault-scheduler-interval for intervals (in seconds).Specify this parameter to inject faults at regular intervals.

**Defaults for OpenChaos:**
```shell
./start-openchaos.sh --driver driver-rocketmq/openchaos-driver.yaml --output-dir ./report $OPENCHAOS_ARGS
```
> Make sure not to duplicate the parameters in OPENCHAOS_ARGS.

**OpenChaos and Chaos-Mesh Configuration**
For YAML files, use placeholders like `${app}` and `${ns}` for application names and namespaces.For example, in your YAML files:
```yaml
selector:
  namespaces:
    - '${ns}'
  labelSelectors:
    "app.kubernetes.io/name": "${app}"
```
## Clean your app in Kubernetes
```yaml
  - uses: apache/rocketmq-test-tool@v1
    name: clean
    with:
      action: "clean"
      ask-config: "${{ secrets.KUBE_CONFIG }}"
      test-version: "v1.0"
      job-id: 1
```
<!-- end usage -->

# License
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) Copyright (C) Apache Software Foundation
