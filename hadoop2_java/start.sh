#!/bin/bash

# 设置JVM参数
# --add-opens 允许Spark访问java.base模块中的sun.nio.ch包
JAVA_OPTS="--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"

# 添加其他常见需要开放的包
JAVA_OPTS="$JAVA_OPTS --add-opens=java.base/java.nio=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens=java.base/java.lang=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens=java.base/java.util=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens=java.base/java.util.concurrent=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens=java.base/java.net=ALL-UNNAMED"

echo "启动应用程序，使用以下JVM参数: $JAVA_OPTS"

# 运行jar包，替换成实际的jar路径
java $JAVA_OPTS -jar hadoop2-server/target/hadoop2-server-0.0.1.jar 