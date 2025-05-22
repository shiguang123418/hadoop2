## 设置JAVA安装目录
export JAVA_HOME=/opt/jdk/jdk1.8
## 设置hadoop命令路径
export SPARK_DIST_CLASSPATH=$(/opt/hadoop/bin/hadoop classpath)
## 以上两行在local模式中已经添加，如果有请勿重复配置
## HADOOP软件配置文件目录，读取HDFS上文件和运行YARN集群
HADOOP_CONF_DIR=/opt/hadoop/etc/hadoop
YARN_CONF_DIR=/opt/hadoop/etc/hadoop
# export SPARK_SSH_OPTS="-p 66"
## 指定spark老大Master的IP和提交任务的通信端口
# 告知Spark的master运行在哪个机器上
export SPARK_MASTER_HOST=shiguang
# 告知sparkmaster的通讯端口
export SPARK_MASTER_PORT=7077
# 告知spark master的webui端口
export SPARK_MASTER_WEBUI_PORT=8081
# worker cpu可用核数
export SPARK_WORKER_CORES=2
# worker可用内存
export SPARK_WORKER_MEMORY=2g
# worker的工作通讯地址
export SPARK_WORKER_PORT=7078
# worker的webui地址
export SPARK_WORKER_WEBUI_PORT=8082
## 设置历史服务器
# 配置的意思是  将spark程序运行的历史日志 存到hdfs的/sparklog文件夹中
export SPARK_HISTORY_OPTS="
-Dspark.history.ui.port=18080 
-Dspark.history.fs.logDirectory=hdfs://shiguang:9000/sparklog/ 
-Dspark.history.retainedApplications=30"