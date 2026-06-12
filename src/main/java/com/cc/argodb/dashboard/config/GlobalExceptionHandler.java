package com.cc.argodb.dashboard.config;

import com.cc.argodb.dashboard.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.FileNotFoundException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleSqlException(SQLException e) {
        log.error("数据库连接失败: {}", e.getMessage());
        return ApiResponse.fail("数据库连接失败: " + e.getMessage());
    }

    @ExceptionHandler(ClassNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleClassNotFound(ClassNotFoundException e) {
        log.error("驱动类未找到: {}", e.getMessage());
        return ApiResponse.fail("驱动类未找到，请检查驱动类名是否正确: " + e.getMessage());
    }

    @ExceptionHandler(FileNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleFileNotFound(FileNotFoundException e) {
        log.error("驱动文件不存在: {}", e.getMessage());
        return ApiResponse.fail("驱动JAR文件不存在: " + e.getMessage());
    }

    @ExceptionHandler(SocketTimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public ApiResponse<Void> handleTimeout(SocketTimeoutException e) {
        log.error("连接超时: {}", e.getMessage());
        return ApiResponse.fail("连接超时，请检查IP和端口是否正确");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneral(Exception e) {
        log.error("服务器内部错误", e);
        return ApiResponse.fail("服务器内部错误: " + e.getMessage());
    }
}
