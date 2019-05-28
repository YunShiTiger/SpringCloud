/**
 *    Copyright 2009-2018 the original author or authors.
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
package org.apache.ibatis;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 提供用于进行测试阶段数据源的抽象基类
 */
public abstract class BaseDataTest {

  //关于blog的配置信息  连接数据库的配置信息 数据库的表结构 数据库中表的数据
  public static final String BLOG_PROPERTIES = "org/apache/ibatis/databases/blog/blog-derby.properties";
  public static final String BLOG_DDL = "org/apache/ibatis/databases/blog/blog-derby-schema.sql";
  public static final String BLOG_DATA = "org/apache/ibatis/databases/blog/blog-derby-dataload.sql";

  //关于jpetstore的配置信息  连接数据库的配置信息 数据库的表结构 数据库中表的数据
  public static final String JPETSTORE_PROPERTIES = "org/apache/ibatis/databases/jpetstore/jpetstore-hsqldb.properties";
  public static final String JPETSTORE_DDL = "org/apache/ibatis/databases/jpetstore/jpetstore-hsqldb-schema.sql";
  public static final String JPETSTORE_DATA = "org/apache/ibatis/databases/jpetstore/jpetstore-hsqldb-dataload.sql";


  /**
   * 根据提供的配置数据创建对应的非池化的数据源对象
   */
  public static UnpooledDataSource createUnpooledDataSource(String resource) throws IOException {
    //根据提供的配置文件路径获取对应的配置属性对象
    Properties props = Resources.getResourceAsProperties(resource);
    //创建非池化数据源对象
    UnpooledDataSource ds = new UnpooledDataSource();
    //配置非池化数据源对象的相关属性信息
    ds.setDriver(props.getProperty("driver"));
    ds.setUrl(props.getProperty("url"));
    ds.setUsername(props.getProperty("username"));
    ds.setPassword(props.getProperty("password"));
    //返回创建好的非池化数据源对象
    return ds;
  }

  /**
   * 根据提供的配置数据创建对应的池化的数据源对象
   */
  public static PooledDataSource createPooledDataSource(String resource) throws IOException {
    //根据提供的配置文件路径获取对应的配置属性对象
    Properties props = Resources.getResourceAsProperties(resource);
    //创建池化数据源对象
    PooledDataSource ds = new PooledDataSource();
    //配置池化数据源对象的相关属性信息
    ds.setDriver(props.getProperty("driver"));
    ds.setUrl(props.getProperty("url"));
    ds.setUsername(props.getProperty("username"));
    ds.setPassword(props.getProperty("password"));
    //返回创建好的池化数据源对象
    return ds;
  }

  /**
   * 根据提供的数据源和脚本文件路径触发执行脚本操作处理
   */
  public static void runScript(DataSource ds, String resource) throws IOException, SQLException {
    //尝试获取与对应数据库的连接对象
    try (Connection connection = ds.getConnection()) {
      //根据连接对象创建对应的脚本执行器对象
      ScriptRunner runner = new ScriptRunner(connection);
      //设置脚本执行器的相关属性
      //设置执行脚本过程中是否进行自动提交操作处理
      runner.setAutoCommit(true);
      //设置执行脚本过程中遇到错误不进行抛出错误 继续执行后续脚本内容的处理
      runner.setStopOnError(false);

      //此处设置了不进行对应日志的输出处理-------在代码分析阶段可以将此处的语句进行注释掉
      runner.setLogWriter(null);
      runner.setErrorLogWriter(null);

      //自己测试参数
      //1 测试脚本文件的执行方式 是整体执行 还是依次执行
      //runner.setSendFullScript(true);

      //使用配置的脚本执行器执行对应的脚本
      runScript(runner, resource);
    }
  }

  /**
   * 根据提供的脚本执行器对象和脚本路径来执行对应的脚本
   */
  public static void runScript(ScriptRunner runner, String resource) throws IOException, SQLException {
    //尝试加载对应的脚本创建对应的读取对象
    try (Reader reader = Resources.getResourceAsReader(resource)) {
      //通过脚本执行器执行对应的脚本
      runner.runScript(reader);
    }
  }

  /**
   * 创建blog需要的数据源对象
   */
  public static DataSource createBlogDataSource() throws IOException, SQLException {
    //创建对应的数据源对象
    DataSource ds = createUnpooledDataSource(BLOG_PROPERTIES);
    //在对应的数据源上执行建表的脚本
    runScript(ds, BLOG_DDL);
    //在对应的数据源上执行在表中插入数据的脚本
    runScript(ds, BLOG_DATA);
    //返回对应的数据源对象
    return ds;
  }

  /**
   * 创建jpetstore需要的数据源对象
   */
  public static DataSource createJPetstoreDataSource() throws IOException, SQLException {
    //创建对应的数据源对象
    DataSource ds = createUnpooledDataSource(JPETSTORE_PROPERTIES);
    //在对应的数据源上执行建表的脚本
    runScript(ds, JPETSTORE_DDL);
    //在对应的数据源上执行在表中插入数据的脚本
    runScript(ds, JPETSTORE_DATA);
    //返回对应的数据源对象
    return ds;
  }
}
