FROM registry.cn-guangzhou.aliyuncs.com/cc-aliyun/ubuntu-python:v1.1.0

RUN mkdir /benchmark/
RUN apt-get install bc
RUN pip install numpy pandas matplotlib

COPY log_analysis.py /benchmark/log_analysis.py
COPY entry.sh /entry.sh

ENTRYPOINT ["/entry.sh"]