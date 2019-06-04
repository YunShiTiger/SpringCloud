/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.logging;

import java.lang.reflect.Constructor;

/**
 * 日志处理工厂类  主要完成确实使用那种具体的日志处理类
 *   相关的介绍文档
 *   https://blog.csdn.net/qq_40348465/article/details/84376811
 *   https://blog.csdn.net/m0_37770300/article/details/81132254
 *
 *   使用本工厂可以获取对应的日志处理实现类  其实在对应的日志处理实现类中真正封装了进行日志处理的处理类
 *      通过这种方式能够进行对后期日志处理类的扩展处理  这里并不是写死了  提高了灵活性和后期的扩展性
 */
public final class LogFactory {

  /**
   * Marker to be used by logging implementations that support markers.
   */
  public static final String MARKER = "MYBATIS";

  //用于记录对应的日志处理类的构造函数------>主要通过本构造函数来创建对应的日志处理类对象
  private static Constructor<? extends Log> logConstructor;

  /**
   * 通过静态代码块的方式来完成初始化默认日志处理类对象的处理
   */
  static {
    tryImplementation(LogFactory::useSlf4jLogging);
    tryImplementation(LogFactory::useCommonsLogging);
    tryImplementation(LogFactory::useLog4J2Logging);
    tryImplementation(LogFactory::useLog4JLogging);
    tryImplementation(LogFactory::useJdkLogging);
    tryImplementation(LogFactory::useNoLogging);
  }

  private static void tryImplementation(Runnable runnable) {
    //检测当前是否已经确定了日志处理类对象的构造函数
    if (logConstructor == null) {
      try {
        //在没有确认的情况下 进行尝试本日志处理类
        runnable.run();
      } catch (Throwable t) {
        // ignore
        //在使用对应的日志处理类过程中 出现了异常就进行忽略处理
      }
    }
  }

  /**
   * 使用私有的构造方式防止外部直接进行创建对应的处理
   */
  private LogFactory() {
    // disable construction
  }

  /**
   * 根据提供的类来获取对应的日志处理类对象
   */
  public static Log getLog(Class<?> aClass) {
    return getLog(aClass.getName());
  }

  /**
   * 根据提供的类的全限定名来获取对应的日志处理实现类
   */
  public static Log getLog(String logger) {
    try {
      return logConstructor.newInstance(logger);
    } catch (Throwable t) {
      throw new LogException("Error creating logger for logger " + logger + ".  Cause: " + t, t);
    }
  }

  /**
   * 对外暴露的添加用户自定义的日志处理实现类对象的处理函数
   */
  public static synchronized void useCustomLogging(Class<? extends Log> clazz) {
    setImplementation(clazz);
  }

  /**
   * 使用Slf4j日志处理类
   */
  public static synchronized void useSlf4jLogging() {
    setImplementation(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);
  }

  /**
   * 使用Log4J2日志处理类
   */
  public static synchronized void useCommonsLogging() {
    setImplementation(org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl.class);
  }

  /**
   * 使用Log4J日志处理类
   */
  public static synchronized void useLog4JLogging() {
    setImplementation(org.apache.ibatis.logging.log4j.Log4jImpl.class);
  }

  /**
   * 使用Log4J2日志处理类
   */
  public static synchronized void useLog4J2Logging() {
    setImplementation(org.apache.ibatis.logging.log4j2.Log4j2Impl.class);
  }

  /**
   * 使用jdk自带的日志处理实现类
   */
  public static synchronized void useJdkLogging() {
    setImplementation(org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl.class);
  }

  /**
   * 使用标准输出的日志处理实现类
   */
  public static synchronized void useStdOutLogging() {
    setImplementation(org.apache.ibatis.logging.stdout.StdOutImpl.class);
  }

  /**
   * 不进行日志处理的实现类
   */
  public static synchronized void useNoLogging() {
    setImplementation(org.apache.ibatis.logging.nologging.NoLoggingImpl.class);
  }

  /**
   * 设置对应的日志处理实现类
   */
  private static void setImplementation(Class<? extends Log> implClass) {
    try {
      //首先获取对应的日志实现类的构造函数
      Constructor<? extends Log> candidate = implClass.getConstructor(String.class);
      //根据对应的构造函数创建对应的日志处理实现类对象
      Log log = candidate.newInstance(LogFactory.class.getName());
      //检测是否开启了调试输入模式
      if (log.isDebugEnabled()) {
        //输出对应的调试信息
        log.debug("Logging initialized using '" + implClass + "' adapter.");
      }
      //设置记录对应的构造函数
      logConstructor = candidate;
    } catch (Throwable t) {
      throw new LogException("Error setting Log implementation.  Cause: " + t, t);
    }
  }

}
