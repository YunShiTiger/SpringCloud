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
package org.apache.ibatis.jdbc;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 进行执行脚本操作处理的工具对象
 *   可以封装一个自己常用的工具
 *   https://blog.csdn.net/dream_broken/article/details/54340482
 *   使用MyBatis中的ScriptRunner来执行sql文件脚本，实现启动自动部署数据库
 *   https://blog.csdn.net/m0_37456570/article/details/83751401
 */
public class ScriptRunner {

  //获取当前系统对应的回车换行符
  private static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");
  //默认的脚本语句结束符
  private static final String DEFAULT_DELIMITER = ";";
  //
  private static final Pattern DELIMITER_PATTERN = Pattern.compile("^\\s*((--)|(//))?\\s*(//)?\\s*@DELIMITER\\s+([^\\s]+)", Pattern.CASE_INSENSITIVE);

  //用于记录与数据库的连接对象
  private final Connection connection;
  //用于记录在执行脚本语句过程中出现错误是否执行后续的脚本语句的操作标识
  private boolean stopOnError;
  //用于记录是否输出在执行脚本过程中对应的警告异常抛出标识
  private boolean throwWarning;
  //是否进行自动提交配置参数
  private boolean autoCommit;
  //是否进行整体执行还是一行一行进行执行的配置参数
  private boolean sendFullScript;
  //是否进行替换脚本语句中的回车换行符为;
  private boolean removeCRs;
  //是否开启对传入的脚本语句进行转义操作处理
  private boolean escapeProcessing = true;

  //用于记录对应的日志输入对象   默认是控制台输出   即在不进行设置去情况下 将对应的日志输入到控制台上
  private PrintWriter logWriter = new PrintWriter(System.out);
  private PrintWriter errorLogWriter = new PrintWriter(System.err);

  //一个完整脚本语句的结束符
  private String delimiter = DEFAULT_DELIMITER;
  //
  private boolean fullLineDelimiter;

  /**
   * 根据提供的数据库连接对象来构建对应的脚本执行器对象
   */
  public ScriptRunner(Connection connection) {
    this.connection = connection;
  }

  public void setStopOnError(boolean stopOnError) {
    this.stopOnError = stopOnError;
  }

  public void setThrowWarning(boolean throwWarning) {
    this.throwWarning = throwWarning;
  }

  public void setAutoCommit(boolean autoCommit) {
    this.autoCommit = autoCommit;
  }

  public void setSendFullScript(boolean sendFullScript) {
    this.sendFullScript = sendFullScript;
  }

  public void setRemoveCRs(boolean removeCRs) {
    this.removeCRs = removeCRs;
  }

  /**
   * @since 3.1.1
   */
  public void setEscapeProcessing(boolean escapeProcessing) {
    this.escapeProcessing = escapeProcessing;
  }

  public void setLogWriter(PrintWriter logWriter) {
    this.logWriter = logWriter;
  }

  public void setErrorLogWriter(PrintWriter errorLogWriter) {
    this.errorLogWriter = errorLogWriter;
  }

  public void setDelimiter(String delimiter) {
    this.delimiter = delimiter;
  }

  public void setFullLineDelimiter(boolean fullLineDelimiter) {
    this.fullLineDelimiter = fullLineDelimiter;
  }

  public void runScript(Reader reader) {
    //设置配置的自动提交参数
    setAutoCommit();
    try {
      //检测配置的参数是否进行整体执行脚本还是一行一行进行执行
      if (sendFullScript) {
        //触发进行整合脚本进行整体执行的处理函数
        executeFullScript(reader);
      } else {
        //触发脚本中语句依次执行的处理函数
        executeLineByLine(reader);
      }
    } finally {
      //进行回滚操作处理--------->此处有多层含义  如果执行成功了  那么进行回滚已经没有意义了 没实际用途  如果出现了异常 进行回滚操作 那么就直接进行回滚了
      rollbackConnection();
    }
  }

  /**
   * 将脚本文件中的语句进行整体提交进行执行操作处理
   */
  private void executeFullScript(Reader reader) {
    StringBuilder script = new StringBuilder();
    try {
      BufferedReader lineReader = new BufferedReader(reader);
      String line;
      //循环读取脚本文件中的每行数据
      while ((line = lineReader.readLine()) != null) {
        //将每行数据添加到脚本字符串对象中
        script.append(line);
        //添加对应的回车换行符
        script.append(LINE_SEPARATOR);
      }
      //获取所有的脚本文件中拼接后的语句
      String command = script.toString();
      //打印整体的待执行的脚本语句
      println(command);
      //执行对应的脚本语句
      executeStatement(command);
      //执行完毕后进行提交操作处理
      commitConnection();
    } catch (Exception e) {
      String message = "Error executing: " + script + ".  Cause: " + e;
      printlnError(message);
      throw new RuntimeSqlException(message, e);
    }
  }

  /**
   * 根据提供的脚本进行逐行执行脚本语句
   */
  private void executeLineByLine(Reader reader) {
    //创建对应的存储一条完整脚本语句的字符串构造器对象
    StringBuilder command = new StringBuilder();
    try {
      //根据提供的读取对象创建对应的缓存读取对象
      BufferedReader lineReader = new BufferedReader(reader);
      //定义存储脚本文件中每行的语句字符串对象
      String line;
      //循环读取脚本文件中每行的脚本语句
      while ((line = lineReader.readLine()) != null) {
        //完成分析语句并构建完整脚本语句以及执行对应脚本语句的处理函数
        handleLine(command, line);
      }
      //进一步进行确认提交操作处理
      commitConnection();
      //检测是否还有未执行脚本语句
      checkForMissingLineTerminator(command);
    } catch (Exception e) {
      //拼接执行异常的错误信息
      String message = "Error executing: " + command + ".  Cause: " + e;
      //输入对应的异常信息
      printlnError(message);
      //抛出对应的执行异常信息
      throw new RuntimeSqlException(message, e);
    }
  }

  /**
   * 关闭对应的数据库连接对象
   */
  public void closeConnection() {
    try {
      connection.close();
    } catch (Exception e) {
      // ignore
    }
  }

  /**
   * 将配置的是否进行自动提交的配置参数设置给对应的数据库连接对象
   */
  private void setAutoCommit() {
    try {
      //首先检测当前连接对象上配置的自动提交参数和待设定的参数值是否相同
      if (autoCommit != connection.getAutoCommit()) {
        //在不相同的配置前提下进行配置自动提交属性值
        connection.setAutoCommit(autoCommit);
      }
    } catch (Throwable t) {
      throw new RuntimeSqlException("Could not set AutoCommit to " + autoCommit + ". Cause: " + t, t);
    }
  }

  /**
   * 执行完对应的脚本进行最终的提交操作处理
   */
  private void commitConnection() {
    try {
      //首先检测是否设置了自动提交标识
      if (!connection.getAutoCommit()) {
        //在非自动提交的情况下，进行手动提价操作处理
        connection.commit();
      }
    } catch (Throwable t) {
      throw new RuntimeSqlException("Could not commit transaction. Cause: " + t, t);
    }
  }

  /**
   * 执行对应的回滚操作处理
   */
  private void rollbackConnection() {
    try {
      //首先检测是否设置了自动提交标识
      if (!connection.getAutoCommit()) {
        //在非自动提交的情况下，进行手动回滚操作处理
        connection.rollback();
      }
    } catch (Throwable t) {
      // ignore
    }
  }

  /**
   * 检测是否有未完成的脚本语句   即脚本语句最后缺少脚本结束符
   * @param command
   */
  private void checkForMissingLineTerminator(StringBuilder command) {
    if (command != null && command.toString().trim().length() > 0) {
      throw new RuntimeSqlException("Line missing end-of-line terminator (" + delimiter + ") => " + command);
    }
  }

  /**
   * 逐行分析待执行的脚本语句  根据语句类型进行相关处理
   */
  private void handleLine(StringBuilder command, String line) throws SQLException {
    //首先去除对应的空格操作处理
    String trimmedLine = line.trim();
    //检测是否是注释行标识
    if (lineIsComment(trimmedLine)) {
      Matcher matcher = DELIMITER_PATTERN.matcher(trimmedLine);
      if (matcher.find()) {
        delimiter = matcher.group(5);
      }
      println(trimmedLine);
    } else if (commandReadyToExecute(trimmedLine)) {
      //截取到对应的脚本语句部分
      command.append(line.substring(0, line.lastIndexOf(delimiter)));
      //添加对应的语句分割符
      command.append(LINE_SEPARATOR);
      //输出本次待执行的脚本语句
      println(command);
      //执行对应的脚本语句
      executeStatement(command.toString());
      //重置存储脚本的字符串构造器
      command.setLength(0);
    } else if (trimmedLine.length() > 0) {
      //非注释行且非脚本结束行  那么就将对应的脚本语句添加到脚本字符串中进行存储处理
      command.append(line);
      //添加对应的语句分割符
      command.append(LINE_SEPARATOR);
    }
  }

  /**
   * 检测给定的脚本语句是否是注释语句
   */
  private boolean lineIsComment(String trimmedLine) {
    return trimmedLine.startsWith("//") || trimmedLine.startsWith("--");
  }

  /**
   * 检测是否到达了脚本语句的结束位置
   */
  private boolean commandReadyToExecute(String trimmedLine) {
    // issue #561 remove anything after the delimiter
    return !fullLineDelimiter && trimmedLine.contains(delimiter) || fullLineDelimiter && trimmedLine.equals(delimiter);
  }

  /**
   * 此处真正进行提交脚本命令进行执行操作处理
   * Statement
   * https://www.cnblogs.com/rain144576/p/6927367.html?utm_source=itdadao&utm_medium=referral
   */
  private void executeStatement(String command) throws SQLException {
    //根据数据库连接对象创建对应的Statement对象
    Statement statement = connection.createStatement();
    try {
      statement.setEscapeProcessing(escapeProcessing);
      String sql = command;
      //检测是否根据配置来替换执行脚本语句中的回车换行符
      if (removeCRs) {
        sql = sql.replaceAll("\r\n", "\n");
      }
      try {
        //提交并执行对应的sql脚本语句
        //此处这样写的原因 https://blog.csdn.net/lin451791119/article/details/80237080
        boolean hasResults = statement.execute(sql);
        //此处这个地方进行这样处理  是因为我们知道给定的脚本语句都会是具有更新功能的处理脚本语句
        while (!(!hasResults && statement.getUpdateCount() == -1)) {
          //检测是否出现警告异常
          checkWarnings(statement);
          //进行输出对应的结果集
          printResults(statement, hasResults);
          //进一步获取下一个执行结果的数据
          hasResults = statement.getMoreResults();
        }
      } catch (SQLWarning e) {
        //在Oracle数据库中抛出的警告异常
        throw e;
      } catch (SQLException e) {
        //检测是否配置了 遇到错误异常就进行抛出的配置参数
        if (stopOnError) {
          //抛出对应的异常错误信息
          throw e;
        } else {
          //拼接对应的错误日志信息
          String message = "Error executing: " + command + ".  Cause: " + e;
          //输出错误日志信息
          printlnError(message);
        }
      }
    } finally {
      try {
        statement.close();
      } catch (Exception e) {
        // Ignore to workaround a bug in some connection pools (Does anyone know the details of the bug?)
      }
    }
  }

  /**
   * 此处是处理一个兼容性问题  即多种数据库抛出异常的方式可能不尽相同
   */
  private void checkWarnings(Statement statement) throws SQLException {
    //检测是否需要进行抛出异常
    if (!throwWarning) {
      return;
    }
    // In Oracle, CREATE PROCEDURE, FUNCTION, etc. returns warning instead of throwing exception if there is compilation error.
    //获取对应的警告异常
    SQLWarning warning = statement.getWarnings();
    //检测是否存在对应的警告异常
    if (warning != null) {
      //抛出对应的警告异常错误信息
      throw warning;
    }
  }

  /**
   * 打印脚本执行结果
   */
  private void printResults(Statement statement, boolean hasResults) {
    if (!hasResults) {
      return;
    }
    try (ResultSet rs = statement.getResultSet()) {
      ResultSetMetaData md = rs.getMetaData();
      int cols = md.getColumnCount();
      for (int i = 0; i < cols; i++) {
        String name = md.getColumnLabel(i + 1);
        print(name + "\t");
      }
      println("");
      while (rs.next()) {
        for (int i = 0; i < cols; i++) {
          String value = rs.getString(i + 1);
          print(value + "\t");
        }
        println("");
      }
    } catch (SQLException e) {
      printlnError("Error printing results: " + e.getMessage());
    }
  }

  /**
   * 不进行分行输出日志数据
   */
  private void print(Object o) {
    if (logWriter != null) {
      logWriter.print(o);
      logWriter.flush();
    }
  }

  /**
   * 分行输出对应的日志数据
   */
  private void println(Object o) {
    if (logWriter != null) {
      logWriter.println(o);
      logWriter.flush();
    }
  }

  /**
   * 分行输出对应的错误日志数据
   */
  private void printlnError(Object o) {
    if (errorLogWriter != null) {
      errorLogWriter.println(o);
      errorLogWriter.flush();
    }
  }

}
