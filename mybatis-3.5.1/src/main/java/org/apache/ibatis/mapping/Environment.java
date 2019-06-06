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
package org.apache.ibatis.mapping;

import javax.sql.DataSource;

import org.apache.ibatis.transaction.TransactionFactory;

/**
 * 记录对应的数据库执行环境的处理类
 */
public final class Environment {

  //执行数据库环境的id标识值
  private final String id;
  //执行环境对应的事物管理工厂对象
  private final TransactionFactory transactionFactory;
  //执行环境对应的数据源对象
  private final DataSource dataSource;

  public Environment(String id, TransactionFactory transactionFactory, DataSource dataSource) {
    //检测相关参数是否为空异常
    if (id == null) {
      throw new IllegalArgumentException("Parameter 'id' must not be null");
    }
    if (transactionFactory == null) {
      throw new IllegalArgumentException("Parameter 'transactionFactory' must not be null");
    }
    if (dataSource == null) {
      throw new IllegalArgumentException("Parameter 'dataSource' must not be null");
    }
    //初始化对应的属性值
    this.id = id;
    this.transactionFactory = transactionFactory;
    this.dataSource = dataSource;
  }

  public String getId() {
    return this.id;
  }

  public TransactionFactory getTransactionFactory() {
    return this.transactionFactory;
  }

  public DataSource getDataSource() {
    return this.dataSource;
  }

  /**
   * 构建执行环境的构建器处理类
   *   提供配置数据库环境的相关参数方法 同时暴露根据配置的参数创建数据库运行环境对象
   */
  public static class Builder {

    private String id;
    private TransactionFactory transactionFactory;
    private DataSource dataSource;

    public Builder(String id) {
      this.id = id;
    }

    public Builder transactionFactory(TransactionFactory transactionFactory) {
      this.transactionFactory = transactionFactory;
      return this;
    }

    public Builder dataSource(DataSource dataSource) {
      this.dataSource = dataSource;
      return this;
    }

    public String id() {
      return this.id;
    }

    /**
     * 对外暴露的通过配置相关参数来构建执行环境对象的构建方法
     */
    public Environment build() {
      return new Environment(this.id, this.transactionFactory, this.dataSource);
    }

  }

}
