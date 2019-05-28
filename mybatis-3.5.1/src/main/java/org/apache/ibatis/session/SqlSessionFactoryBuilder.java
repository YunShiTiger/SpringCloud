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
package org.apache.ibatis.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;

/**
 * Builds {@link SqlSession} instances.
 * 通过使用构建者模式来创建对应的SqlSessionFactory对象  即通过不同配置参数来组建对应的SqlSessionFactory对象
 *   主要是通过这个方式来提供不同方式进行构建SqlSessionFactory对象的形式
 *   其中最为重要的一个构建方法是build(Configuration config)  它根据对应的配置信息对象来创建对应的SqlSessionFactory对象
 *   ----->因此通过这种形式可以发现创建SqlSessionFactory对象的主要形式为
 *         1.通过对应的字符输入流来引入相应的配置进行并进行解析处理 最终构建对应的Configuration对象    字符流对象  Reader  --->  XMLConfigBuilder --->  parser.parse()  --->  Configuration  --->  SqlSessionFactory
 *         2.通过对应的字节输入流来引入相应的配置进行并进行解析处理 最终构建对应的Configuration对象    字节流对象  InputStream  --->  XMLConfigBuilder --->  parser.parse()  --->  Configuration  --->  SqlSessionFactory
 *         3.自己直接new Configuration对象来进行创建并调用构建方法   这样需要自己来创建并初始化Configuration类对象
 *   需要明确的问题是  Configuration类对象在运行期间只会保持一份  因此 对应的创建对应的SqlSessionFactory对象也会保持一个对象
 *
 *   通过分析两种流对象的处理可以发现   最终创建配置信息类对象的操作交给了对应的xml解析器对象进行处理  即通过创建了xml配置参数来具体解析并配置对应的配置信息类对象  进而完成根据配置的属性来装配配置信息类对象的处理
 */
public class SqlSessionFactoryBuilder {

  /**
   * 总体上通过提供配置信息字符流对象来构建DefaultSqlSessionFactory对象的操作处理
   */
  public SqlSessionFactory build(Reader reader) {
    return build(reader, null, null);
  }

  public SqlSessionFactory build(Reader reader, String environment) {
    return build(reader, environment, null);
  }

  public SqlSessionFactory build(Reader reader, Properties properties) {
    return build(reader, null, properties);
  }

  public SqlSessionFactory build(Reader reader, String environment, Properties properties) {
    try {
      //根据对应的字符流对象创建对应的xml解析构建器对象
      XMLConfigBuilder parser = new XMLConfigBuilder(reader, environment, properties);
      //解析提供的xml配置信息并构建出对应的配置信息类对象，进而创建对应的SqlSessionFactory对象
      return build(parser.parse());
    } catch (Exception e) {
      //遇到错误抛出对应的构建SqlSessionFactory对象出现错误的异常
      throw ExceptionFactory.wrapException("Error building SqlSession.", e);
    } finally {
      ErrorContext.instance().reset();
      try {
        //关闭对应的流对象
        reader.close();
      } catch (IOException e) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }

  /**
   * 总体上通过提供配置信息字节流对象来构建DefaultSqlSessionFactory对象的操作处理
   */
  public SqlSessionFactory build(InputStream inputStream) {
    return build(inputStream, null, null);
  }

  public SqlSessionFactory build(InputStream inputStream, String environment) {
    return build(inputStream, environment, null);
  }

  public SqlSessionFactory build(InputStream inputStream, Properties properties) {
    return build(inputStream, null, properties);
  }

  public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
    try {
      //根据对应的字节流对象创建对应的xml解析构建器对象
      XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
      //解析提供的xml配置信息并构建出对应的配置信息类对象，进而创建对应的SqlSessionFactory对象
      return build(parser.parse());
    } catch (Exception e) {
      //遇到错误抛出对应的构建SqlSessionFactory对象出现错误的异常
      throw ExceptionFactory.wrapException("Error building SqlSession.", e);
    } finally {
      ErrorContext.instance().reset();
      try {
        //关闭对应的流对象
        inputStream.close();
      } catch (IOException e) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }

  /**
   * 本构建方法是最终需要走的构建方法------------>本类中的关键方法
   *   即上述的两种方式最终都会构建对应的Configuration对象 进而启动创建默认的DefaultSqlSessionFactory对象
   */
  public SqlSessionFactory build(Configuration config) {
    //根据提供的配置信息类对象构建默认实现的SqlSessionFactory对象
    return new DefaultSqlSessionFactory(config);
  }

}
