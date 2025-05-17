#!/bin/bash

# Script to run the Spring Boot application with minimal logging
# Discards all standard output and error except for critical messages

echo "Starting Spring Boot application in silent mode..."

# Java system properties to disable logging
JAVA_OPTS="-Dlogging.config=classpath:log4j2.xml -Dspring.main.banner-mode=off"

# Add specific Spring Boot properties
SPRING_OPTS="--logging.level.root=OFF --logging.level.org.springframework=OFF --logging.level.org.apache=OFF"

# Run the application with Maven or directly via java -jar
# Uncomment the appropriate line depending on how you usually start the application

# Option 1: Using Maven
mvn spring-boot:run -Dspring-boot.run.jvmArguments="${JAVA_OPTS}" -q ${SPRING_OPTS} > /dev/null 2>&1 &

# Option 2: Using Java directly (uncomment if you prefer this)
# java ${JAVA_OPTS} -jar target/your-application.jar ${SPRING_OPTS} > /dev/null 2>&1 &

# Store the process ID
PID=$!
echo "Application started with PID: $PID"
echo "To stop the application, run: kill $PID"
echo "Log files are being suppressed" 