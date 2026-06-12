@echo off
echo === ArgoDB Asset Dashboard Build ===
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo BUILD FAILED
    pause
    exit /b 1
)
echo.
echo === Build Success ===
echo JAR: target\argodb-asset-dashboard-1.0.0.jar
echo Run: java -jar target\argodb-asset-dashboard-1.0.0.jar
pause
