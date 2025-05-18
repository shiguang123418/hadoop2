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

# 检查类路径中是否包含特定类
check_class() {
    local class_name=$1
    local found_files=()
    local jar_files

    # 获取类路径
    if [ -f "hadoop2_java/target/classes" ]; then
        CLASSPATH="hadoop2_java/target/classes"
    fi

    # 在Maven仓库中查找JAR文件
    jar_files=$(find ~/.m2/repository -name "*.jar" | grep -v "sources" | grep -v "javadoc")

    echo_yellow "正在检查类 $class_name 是否存在于Maven仓库JAR文件中..."
    for jar in $jar_files; do
        if jar tf "$jar" | grep -q "$class_name"; then
            found_files+=("$jar")
            echo_green "  找到类: $class_name 在JAR文件: $jar"
        fi
    done

    # 检查Spark类路径
    if [ ${#found_files[@]} -eq 0 ]; then
        echo_red "  未找到类: $class_name 在任何JAR文件中!"
    else
        echo_green "共找到 ${#found_files[@]} 个包含类 $class_name 的JAR文件"
    fi
}

# 检查特定类的存在
check_classes() {
    local classes=(
        "org/apache/spark/streaming/kafka010/KafkaRDDPartition.class"
        "org/apache/spark/streaming/kafka010/KafkaUtils.class"
        "org/apache/spark/streaming/kafka010/LocationStrategies.class"
        "org/apache/spark/streaming/kafka010/ConsumerStrategies.class"
        "org/apache/kafka/clients/consumer/KafkaConsumer.class"
    )

    for class in "${classes[@]}"; do
        check_class "$class"
        echo ""
    done
}

# 检查Spring Boot类
check_spring_boot_classes() {
    local classes=(
        "org/springframework/boot/SpringApplication.class"
        "org/springframework/boot/autoconfigure/SpringBootApplication.class"
    )

    for class in "${classes[@]}"; do
        check_class "$class"
        echo ""
    done
}

# 显示帮助
show_help() {
    echo "用法: $0 [选项]"
    echo "选项:"
    echo "  --kafka   检查Kafka Streaming相关类"
    echo "  --spring  检查Spring Boot相关类"
    echo "  --all     检查所有类"
    echo "  --help    显示帮助信息"
}

# 主函数
main() {
    if [ $# -eq 0 ]; then
        show_help
        exit 0
    fi

    for arg in "$@"; do
        case $arg in
            --kafka)
                echo_green "检查Kafka Streaming相关类..."
                check_classes
                ;;
            --spring)
                echo_green "检查Spring Boot相关类..."
                check_spring_boot_classes
                ;;
            --all)
                echo_green "检查所有类..."
                check_classes
                check_spring_boot_classes
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                echo_red "未知选项: $arg"
                show_help
                exit 1
                ;;
        esac
    done
}

# 运行主函数
main "$@" 