#!/bin/bash

# 定义颜色代码
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # 无颜色

# 打印带颜色的消息
echo_green() {
    echo -e "${GREEN}$1${NC}"
}

echo_yellow() {
    echo -e "${YELLOW}$1${NC}"
}

echo_red() {
    echo -e "${RED}$1${NC}"
}

# 确保使用Java 11
JAVA_HOME=/opt/jdk/jdk-11
PATH=$JAVA_HOME/bin:$PATH

# 检查Java版本
java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo_green "使用Java版本: $java_version"

if [[ "$java_version" != 11* ]]; then
    echo_red "警告: 当前Java版本不是Java 11，请确保环境变量正确设置"
    exit 1
fi

# 检查是否存在Maven的POM文件
if [ ! -f "hadoop2_java/pom.xml" ]; then
    echo_red "未找到POM文件，请确保您在正确的目录中运行此脚本"
    exit 1
fi

# 切换到后端项目目录
cd hadoop2_java

# 检查前次构建是否存在，如不存在则执行完整构建
if [ ! -d "target/classes" ]; then
    echo_yellow "未检测到前次构建，执行完整Maven构建..."
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo_red "Maven构建失败"
        exit 1
    fi
fi

# 获取Kafka依赖JAR路径
KAFKA_JARS=$(find ~/.m2/repository -name "spark-streaming-kafka-0-10*.jar" | tr '\n' ':')
KAFKA_CLIENTS_JARS=$(find ~/.m2/repository -name "kafka-clients*.jar" | tr '\n' ':')

# 设置类路径，确保包含所有需要的JAR文件
CLASSPATH="target/classes:target/dependency/*:$KAFKA_JARS:$KAFKA_CLIENTS_JARS"
echo_yellow "添加Kafka相关JAR到类路径：$KAFKA_JARS"
echo_yellow "添加Kafka客户端JAR到类路径：$KAFKA_CLIENTS_JARS"

# 设置Spark相关环境变量
export SPARK_DIST_CLASSPATH=$CLASSPATH
export SPARK_CLASSPATH=$CLASSPATH

# 设置Java选项
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp"
JAVA_OPTS="$JAVA_OPTS -Dsun.io.useCanonCaches=false -Djava.net.preferIPv4Stack=true"
JAVA_OPTS="$JAVA_OPTS -Dlog4j.configurationFile=src/main/resources/log4j2.xml"
JAVA_OPTS="$JAVA_OPTS -Dlog4j2.debug=true -Dsun.misc.URLClassPath.disableJarChecking=true"
JAVA_OPTS="$JAVA_OPTS -Dspark.driver.allowMultipleContexts=true"
JAVA_OPTS="$JAVA_OPTS -Dspark.driver.userClassPathFirst=true"
JAVA_OPTS="$JAVA_OPTS -Dspark.executor.userClassPathFirst=true"

# 运行Spring Boot应用
echo_green "正在启动农业大数据平台后端服务..."
echo_yellow "使用Java选项: $JAVA_OPTS"
echo_yellow "使用类路径: $CLASSPATH"

# 使用Maven Spring Boot插件运行，同时传递所有需要的参数
mvn spring-boot:run -Dspring-boot.run.jvmArguments="$JAVA_OPTS" -Dspring-boot.run.arguments="--kafka.bootstrap-servers=192.168.1.192:9092 --spark.enabled=true"

# 捕获应用退出状态
EXIT_STATUS=$?

# 返回到原始目录
cd ..

# 根据退出状态打印消息
if [ $EXIT_STATUS -eq 0 ]; then
    echo_green "应用已正常退出"
else
    echo_red "应用异常退出，退出码: $EXIT_STATUS"
fi

exit $EXIT_STATUS
