package org.shiguang.spark.processor;

import java.io.Serializable;

/**
 * 传感器数据处理器接口
 * <p>
 * 定义了传感器数据处理器应该具备的基本功能，包括初始化、启动、停止等操作。
 * 任何实现该接口的类都应该提供对传感器数据流的处理能力。
 * </p>
 */
public interface SensorProcessor extends Serializable {
    
    /**
     * 初始化处理器资源和配置
     */
    void init();
    
    /**
     * 启动处理器开始处理数据
     * @return 如果成功启动则返回true，否则返回false
     */
    boolean start();
    
    /**
     * 停止处理器并释放资源
     */
    void stop();
    
    /**
     * 判断处理器是否正在运行
     * @return 如果处理器正在运行则返回true，否则返回false
     */
    boolean isRunning();
} 