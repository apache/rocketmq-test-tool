FROM registry.cn-guangzhou.aliyuncs.com/cc-aliyun/ubuntu-python:v1.1.0

RUN mkdir /benchmark/
RUN pip install numpy pandas matplotlib

COPY consumer.py /benchmark/consumer.py
COPY producer.py /benchmark/producer.py
COPY entry.sh /entry.sh

ENTRYPOINT ["/entry.sh"]