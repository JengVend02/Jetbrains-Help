package com.zyp.help.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.core.io.ClassPathResource;

import java.io.*;

/**
 * 文件工具接口
 * 
 * <p>此接口提供了一系列静态方法用于处理应用程序中的文件操作。
 * 它能够智能地处理开发环境和生产环境下的文件路径问题，
 * 并提供了从类路径复制文件到外部目录的功能。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>文件存在性检查</li>
 *   <li>智能文件路径解析（支持开发和生产环境）</li>
 *   <li>自动从类路径复制文件到外部目录</li>
 *   <li>目录结构的自动创建</li>
 * </ul>
 * 
 * <p>路径解析规则：
 * <ul>
 *   <li>开发环境：直接使用类路径中的文件</li>
 *   <li>生产环境：JAR包启动时，使用应用目录下的文件</li>
 * </ul>
 * 
 * @author zyp
 * @version 1.0.0
 * @since 1.0.0
 */

public interface FileTools {

    // ==================== 常量定义 ====================
    
    /** 
     * Spring Boot应用程序主目录工具
     * 用于获取应用程序的主目录和源文件信息
     */
    ApplicationHome application = new ApplicationHome();

    // ==================== 核心方法 ====================


    /**
     * 检查指定路径的文件是否存在
     * 
     * <p>此方法会根据当前的运行环境（开发或生产）来决定文件的实际位置。
     * 在开发环境中，直接从类路径中查找文件；
     * 在生产环境中，从应用程序所在目录下查找文件。
     * 
     * @param path 相对路径，相对于类路径或应用程序目录
     * @return 如果文件存在则返回true，否则返回false
     */
    static boolean fileExists(String path) {
        return getFile(path).exists();
    }

    /**
     * 获取指定路径的文件对象
     * 
     * <p>此方法是FileTools的核心方法，它会根据当前的运行环境智能地选择文件路径。
     * 
     * <p>路径解析规则：
     * <ul>
     *   <li>开发环境（source为null）：直接使用类路径中的文件</li>
     *   <li>生产环境（JAR包运行）：使用应用程序所在目录下的文件</li>
     * </ul>
     * 
     * <p>这种设计允许应用程序在不同环境下都能正确地找到所需的文件。
     * 
     * @param path 相对路径，相对于类路径或应用程序目录
     * @return 文件对象（可能不存在）
     */
    static File getFile(String path) {
        // 获取应用程序的主目录和源文件
        File homeDir = application.getDir();
        File source = application.getSource();
        
        // 创建类路径资源对象，用于在开发环境中访问文件
        ClassPathResource classPathResource = new ClassPathResource(path);
        
        // 根据运行环境返回不同的文件对象
        return ObjectUtil.isNull(source) 
            ? FileUtil.file(classPathResource.getPath())  // 开发环境：使用类路径
            : FileUtil.file(homeDir, path);               // 生产环境：使用应用目录
    }

    /**
     * 获取指定路径的文件对象，如果文件不存在则从类路径复制
     * 
     * <p>此方法是一个增强版的文件获取方法，它在 {@link #getFile(String)} 的基础上
     * 添加了自动复制功能。当目标文件不存在时，会尝试从类路径中复制文件。
     * 
     * <p>使用场景：
     * <ul>
     *   <li>首次运行时初始化配置文件</li>
     *   <li>从内置资源复制模板文件</li>
     *   <li>确保在生产环境中有必要的文件</li>
     * </ul>
     * 
     * <p>复制过程：
     * <ol>
     *   <li>调用 {@link #getFile(String)} 获取目标文件对象</li>
     *   <li>在生产环境中，检查类路径中的源文件是否存在</li>
     *   <li>如果源文件存在但目标文件不存在，则进行复制操作</li>
     *   <li>复制过程中会自动创建必要的目录结构</li>
     * </ol>
     * 
     * @param path 相对路径，相对于类路径或应用程序目录
     * @return 文件对象（在此方法返回后应该存在）
     * @throws IllegalArgumentException 当文件读取或复制失败时抛出
     */
    static File getFileOrCreat(String path) {
        // 获取目标文件对象
        File file = getFile(path);
        
        // 在生产环境中处理文件复制
        if (ObjectUtil.isNotNull(application.getSource())) {
            // 创建类路径资源对象
            ClassPathResource classPathResource = new ClassPathResource(path);
            
            // 检查是否需要从类路径复制文件
            if (classPathResource.exists() && !file.exists()) {
                try (InputStream inputStream = classPathResource.getInputStream()) {
                    // 使用Hutool的文件工具进行复制
                    FileUtil.writeFromStream(inputStream, file);
                } catch (Exception e) {
                    // 抛出友好的错误信息
                    throw new IllegalArgumentException(
                            CharSequenceUtil.format("{} 文件读取失败!", path), e
                    );
                }
            }
        }
        
        return file;
    }
}
