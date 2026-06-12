#!/bin/bash
echo "=== ArgoDB Asset Dashboard Build ==="
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "BUILD FAILED"
    exit 1
fi
echo ""
echo "=== Build Success ==="
echo "JAR: target/argodb-asset-dashboard-1.0.0.jar"
echo "Run: java -jar target/argodb-asset-dashboard-1.0.0.jar"
