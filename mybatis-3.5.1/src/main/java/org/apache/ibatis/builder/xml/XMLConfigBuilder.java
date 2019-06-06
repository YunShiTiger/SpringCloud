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
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import javax.sql.DataSource;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;

/**
 * 不错的解析文章
 *   https://blog.csdn.net/qq_35807136/article/details/79002470
 *   系列文章
 *   https://blog.csdn.net/u013510838/article/details/78995542
 *   不错的文章
 *   http://www.cnblogs.com/zhjh256/p/8512392.html
 *
 *   比较详细的介绍属性信息的文章
 *   https://www.cnblogs.com/li3807/p/7061835.html
 *
 *  通过提供的总的xml文件来完成配置信息对象的处理  其实起这个名字就能看出  者也是一种构造器模式  最终要构造的对象时配置信息类对象  而配置的来源可以分为字符流和字节流等不同的xml文件来源
 */
public class XMLConfigBuilder extends BaseBuilder {

  //用于表示是否已经进行过解析处理   即对应的xml文件解析操作只能够解析一次   即通过提供的xml配置文件完成一次配置配置信息类对象的处理
  private boolean parsed;
  //真正进行解析给定xml文件的xpath解析器对象
  private final XPathParser parser;
  //用于记录提供的数据库环境表示
  private String environment;
  //设置默认的反射工厂处理类对象------>即具有缓存功能的反射处理类工厂对象
  private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();

  /**
   * 提供通过字符流方式的xml文件来构建对象的处理
   */
  public XMLConfigBuilder(Reader reader) {
    this(reader, null, null);
  }

  public XMLConfigBuilder(Reader reader, String environment) {
    this(reader, environment, null);
  }

  public XMLConfigBuilder(Reader reader, String environment, Properties props) {
    this(new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  /**
   * 提供通过字节流方式的xml文件来构建对象的处理
   */
  public XMLConfigBuilder(InputStream inputStream) {
    this(inputStream, null, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment) {
    this(inputStream, environment, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
    this(new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  /**
   * 最终需要走的构造函数   前面的构造函数主要完成了相关对象的转换操作处理  即xml文件解析器对象的构建
   *  注意本方法是一个私有的方法  即只能通过上述的构造函数进行调用，不对外提供构建对象的操作处理
   *    这样做的主要原因是 防止误操作对此创建配置信息类对象
   */
  private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {
    //通过这个方法调用可以发现 在子类中创建了配置信息类对象.然后传递给父类中,最后通过父类对象中的引用在子类对象中进行使用
    //这里这样做的原因可以是  统一在父类中使用这个配置对象  同时暴露只有一个地方来进行new对应对象的入口  从而保证配置对象的唯一性
    //即其他子类对象调用此处的方式绝对不是通过new对象来创建此对象  只有此处是通过new对象来传入的  从而完成唯一性的保证
    super(new Configuration());
    ErrorContext.instance().resource("SQL Mapper Configuration");
    //这个地方需要注意  这个地方首次进行了设置配置信息对象中属性列表中的值   即此处的值是通过最外边的门面类对象传入的
    this.configuration.setVariables(props);
    //初始化当前尚未进行解析操作处理标识
    this.parsed = false;
    //记录对应的数据库环境标识
    this.environment = environment;
    //记录对应的xml解析对象
    this.parser = parser;
  }

  /**
   * 对外暴露的根据配置的构建器对象来解析配置的xml文件处理
   *   此处完成对配置信息类对象的一次相关数据信息的初始化操作处理   即根据提供的配置xml来对配置信息类对象完成一些全局方面的配置处理
   *
   *   即配置信息类对象的配置数据来源主要来自两个方面  1 全局性配置的xml文件   2 mapper的xml文件
   */
  public Configuration parse() {
    //首先进行检测是否已经解析过对应配置文件的处理
    if (parsed) {
      //已经解析过就不能进行解析处理了  因此这里抛出错误处理
      throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    //设置解析操作位为已经进行过解析操作处理标识
    parsed = true;
    //触发解析操作处理   内部首先找到对应的configuration节点  即根配置节点
    parseConfiguration(parser.evalNode("/configuration"));
    //返回根据解析的配置文件构建的配置类对象
    return configuration;
  }

  /**
   * 分别进行解析对应节点的框架处理函数    传入的参数为对应的根节点
   * 解析对应的节点参数  https://blog.csdn.net/lovebosom/article/details/52584562
   * mybatis-Config.xml配置文件的内容和配置顺序如下
      properties(属性)
      settings(全局配置参数)
      typeAiases(类型别名)
      typeHandlers(类型处理器)
      objectFactory(对象工厂)
       plugins(插件)
      environments(环境集合属性对象)mappers(映射器)
      environment(环境子属性对象)
      transactionManager(事物管理)
      datesource(数据源)
      mappers(映射器)
   */
  private void parseConfiguration(XNode root) {
    try {
      //issue #117 read properties first
      //加载properties节点,一般是定义一些配置变量
      //解析元素properties，保存在variables中      如配置dataSource的username password
      propertiesElement(root.evalNode("properties"));
      //解析配置文件中settings属性节点配置的属性信息
      Properties settings = settingsAsProperties(root.evalNode("settings"));

      //用于加载用户自己设定的vfs资源加载处理类
      loadCustomVfs(settings);
      //此处是新增的一个功能点  设置通用的日志实现类
      loadCustomLogImpl(settings);

      //加载对应的别名
      //解析元素typeAliases，保存在typeAliasRegistry中      一般用来为Java全路径类型取一个比较短的名字
      typeAliasesElement(root.evalNode("typeAliases"));
      //解析配置文件中plugins节点配置的插件信息
      pluginElement(root.evalNode("plugins"));
      //解析配置文件中objectFactory节点配置的对象工厂信息
      objectFactoryElement(root.evalNode("objectFactory"));
      //解析配置文件中objectWrapperFactory节点配置的包装对象工厂信息
      objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
      //解析配置文件中reflectorFactory节点配置的反射工厂处理类
      reflectorFactoryElement(root.evalNode("reflectorFactory"));

      //通过解析settings节点获取的相关属性来配置对应的配置信息类对象------------------->这个地方为什么不在解析完成节点之后直接执行,而是在执行完一定的解析后再进行设置呢 ？？？？？？？？？？？？
      settingsElement(settings);
      // read it after objectFactory and objectWrapperFactory issue #631
      //解析配置文件中environments节点配置的数据库执行环境信息
      environmentsElement(root.evalNode("environments"));
      //解析配置文件中databaseIdProvider节点配置的当前数据库对应的运行环境标识
      databaseIdProviderElement(root.evalNode("databaseIdProvider"));

      typeHandlerElement(root.evalNode("typeHandlers"));
      mapperElement(root.evalNode("mappers"));
    } catch (Exception e) {
      //在解析对应节点过程出现异常就抛出对应的错误异常  注意这个地方包含了root节点为空的异常  即配置根节点出现了问题
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }

  /** 加载properties节点,一般是定义一些配置变量   需要明确优先级问题
   * 从这个方法中可以看出配置规则
       可以设置url或resource属性从外部文件中加载一个properties文件
       可以通过property子节点进行配置，如果子节点属性的key与外部文件的key重复的话，子节点同名属性值将被覆
       暴露在最外边的门面传入的属性值具有最高优先级
   properties配置示例
     <properties resource="org/mybatis/example/config.properties">
       <property name="username" value="dev_user"/>
       <property name="password" value="F2Fa3!33TYyg"/>
     </properties>

   xxx.properties的配置示例：
     jdbc.driver=com.mysql.jdbc.Driver
     jdbc.url=jdbc:mysql://localhost:3306/mybatis
     jdbc.username=root
     jdbc.password=root
   */
  private void propertiesElement(XNode context) throws Exception {
    //首先检测对应的节点是否存在
    if (context != null) {
      //先加载property子节点下的属性
      Properties defaults = context.getChildrenAsProperties();
      //获取在节点上的两个属性参数是否设置
      String resource = context.getStringAttribute("resource");
      String url = context.getStringAttribute("url");
      //不能同时设置resource属性和url属性
      if (resource != null && url != null) {
        throw new BuilderException("The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
      }
      //此处说明了一处属性值的覆盖问题
      if (resource != null) {
        //加载对应的外部文件配置的属性值 并进行覆盖同名的属性值
        defaults.putAll(Resources.getResourceAsProperties(resource));
      } else if (url != null) {
        //加载对应的外部文件配置的属性值 并进行覆盖同名的属性值
        defaults.putAll(Resources.getUrlAsProperties(url));
      }
      Properties vars = configuration.getVariables();
      //此处说明了一处属性值的覆盖问题
      if (vars != null) {
        //通过暴露在最外边的门面传入的属性值具有最高优先级  即可以覆盖同名的属性值
        defaults.putAll(vars);
      }
      //将全部的属性参数设置到解析器上，方便解析器进行后续解析操作时进行使用
      parser.setVariables(defaults);
      //最终将对应的属性节点的信息设置到配置信息对象中  方便其他类对象进行使用
      configuration.setVariables(defaults);
    }
  }

  /**
   * settings加载全局配置参数 同时完成对配置对象中属性是否存在对应的set方法的检测处理
    官方给的settings配置实例
   	<settings name="cacheEnabled" value="true"/>
      <setting name="lazyLoadingEnabled" value="true"/>
      <setting name="multipleResultSetsEnabled" value="true"/>
      <setting name="useColumnLabel" value="true"/>
      <setting name="useGeneratedKeys" value="false"/>
      <setting name="autoMappingBehavior" value="PARTIAL"/>
      <setting name="defaultExecutorType" value="SIMPLE"/>
      <setting name="defaultStatementTimeout" value="25"/>
      <setting name="safeRowBoundsEnabled" value="false"/>
      <setting name="mapUnderscoreToCamelCase" value="false"/>
      <setting name="localCacheScope" value="SESSION"/>
      <setting name="jdbcTypeForNull" value="OTHER"/>
      <setting name="lazyLoadTriggerMethods" value="equals,clone,hashCode,toString"/>
    </settings>

    本方法的核心目的是验证配置的相关属性参数是否出现问题
        此处需要学习一种思维方式  然后检测一个类上的属性和对应的配置文件中的属性是否相匹配的处理策略
   */
  private Properties settingsAsProperties(XNode context) {
    //首先检查对应的节点是否存在
    if (context == null) {
      //不存在就直接new一个属性对象,同时进行返回处理
      return new Properties();
    }
    //获取配置在settings节点下的所有属性信息
    Properties props = context.getChildrenAsProperties();
    // Check that all settings are known to the configuration class
    //获取对Configuration类的反射属性解析操作处理------->即获取Configuration类的相关属性信息
    MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
    //循环检测对应的属性参数在对应的配置对象中是否有对应的设置属性的方法
    for (Object key : props.keySet()) {
      //检测配置对象上是否有此key对应的属性设置方法
      if (!metaConfig.hasSetter(String.valueOf(key))) {
        //没有对应的属性设置方法就相当于可能配置错了相关的属性  进行异常抛出处理  同时指出了是哪个key可能配置的有问题
        throw new BuilderException("The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
      }
    }
    //返回对应的属性对象
    return props;
  }

  /**
   * 获取用户自定义的vfs的实现，配置在settings元素中。settings中放置自定义vfs实现类的全限定名，以逗号分隔
   * VFS主要用来加载容器内的各种资源，比如jar或者class文件。mybatis提供了2个实现 JBoss6VFS 和 DefaultVFS，并提供了用户扩展点，
   * 用于自定义VFS实现，加载顺序是自定义VFS实现 > 默认VFS实现 取第一个加载成功的，默认情况下会先加载JBoss6VFS，如果classpath下找不到jboss的vfs实现才会加载默认VFS实现，
   * 启动打印的日志如下：
   　　		org.apache.ibatis.io.VFS.getClass(VFS.java:111) Class not found: org.jboss.vfs.VFS
   　　		org.apache.ibatis.io.JBoss6VFS.setInvalid(JBoss6VFS.java:142) JBoss 6 VFS API is not available in this environment.
   　　		org.apache.ibatis.io.VFS.getClass(VFS.java:111) Class not found: org.jboss.vfs.VirtualFile
   　　		org.apache.ibatis.io.VFS$VFSHolder.createVFS(VFS.java:63) VFS implementation org.apache.ibatis.io.JBoss6VFS is not valid in this environment.
   　　		org.apache.ibatis.io.VFS$VFSHolder.createVFS(VFS.java:77) Using VFS adapter org.apache.ibatis.io.DefaultVFS

   　　		jboss vfs的maven仓库坐标为：
               <dependency>
                <groupId>org.jboss</groupId>
                <artifactId>jboss-vfs</artifactId>
                <version>3.2.12.Final</version>
              </dependency>
   　　		找到jboss vfs实现后，输出的日志如下：
   　　			org.apache.ibatis.io.VFS$VFSHolder.createVFS(VFS.java:77) Using VFS adapter org.apache.ibatis.io.JBoss6VFS
   */
  private void loadCustomVfs(Properties props) throws ClassNotFoundException {
    //获取用户配置的vfs实现类
    String value = props.getProperty("vfsImpl");
    //检测是否配置了对应的vfs实现类
    if (value != null) {
      //vfs实现类可以有多个，其实现类全限定名以逗号分隔
      String[] clazzes = value.split(",");
      //循环遍历所有设定的vfs实现类
      for (String clazz : clazzes) {
        //首先检测给定的类是否为null
        if (!clazz.isEmpty()) {
          //反射加载自定义vfs实现类，并设置到Configuration实例中
          @SuppressWarnings("unchecked")
          Class<? extends VFS> vfsImpl = (Class<? extends VFS>)Resources.classForName(clazz);
          //将加载到的vfs实现类设置给配置信息对象中
          configuration.setVfsImpl(vfsImpl);
        }
      }
    }
  }

  /**
   * 给配置信息类对象设置用户配置的通用日志处理实现类
   *  logImpl 可选的值有：SLF4J、LOG4J、LOG4J2、JDK_LOGGING、COMMONS_LOGGING、STDOUT_LOGGING、NO_LOGGING，或者是实现了接口 org.apache.ibatis.logging.Log 的，且构造方法是以字符串为参数的类的完全限定名。
   */
  private void loadCustomLogImpl(Properties props) {
    //解析并加载对应的通用日志实现类
    Class<? extends Log> logImpl = resolveClass(props.getProperty("logImpl"));
    //将对应的通用日志实现类设置给配置信息对象
    configuration.setLogImpl(logImpl);
  }

  /**
   * 解析对应的typeAliases节点的别名信息
   * 通过分析代码发现有两种设置别名的方式
   *    1 通过配置package来进行扫描对应包名下的类来进行设置
   *    2 通过配置alias和type属性进行设置
   */
  private void typeAliasesElement(XNode parent) {
    //检测对应的节点是否存在
    if (parent != null) {
      //解析配置的别名子节点信息
      for (XNode child : parent.getChildren()) {
        //检测是否是通过包名方式进行注册
        if ("package".equals(child.getName())) {
          //获取对应的包名
          String typeAliasPackage = child.getStringAttribute("name");
          //以包名的方式进行批量注册别名处理
          configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
        } else {
          //获取设定的别名
          String alias = child.getStringAttribute("alias");
          //获取设置需要起别名的类
          String type = child.getStringAttribute("type");
          try {
            //加载对应的类资源-------------->如果此处没有加载到对应的资源类就会抛出对应的异常信息
            Class<?> clazz = Resources.classForName(type);
            //检测是否自己设定了对应的别名
            if (alias == null) {
              //在没有显示配置别名的情况下，通过使用简单类名或者注解的方式进行别名注册
              typeAliasRegistry.registerAlias(clazz);
            } else {
              //通过指定别名的方式进行注册处理
              typeAliasRegistry.registerAlias(alias, clazz);
            }
          } catch (ClassNotFoundException e) {
            throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
          }
        }
      }
    }
  }

  /**
   * 解析对应的plugins节点的插件信息
   */
  private void pluginElement(XNode parent) throws Exception {
    //检测对应的节点是否存在
    if (parent != null) {
      //循环遍历所有的插件子节点
      for (XNode child : parent.getChildren()) {
        //获取需要加载的插件处理类
        String interceptor = child.getStringAttribute("interceptor");
        //获取配置字段属性信息
        Properties properties = child.getChildrenAsProperties();
        //加载对应的插件处理类
        Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
        //给对应的插件添加对应的配置属性
        interceptorInstance.setProperties(properties);
        //将配置好的插件添加到插件链中
        configuration.addInterceptor(interceptorInstance);
      }
    }
  }

  /**
   * 解析对应的objectFactory节点的对象工厂
   */
  private void objectFactoryElement(XNode context) throws Exception {
    //检测对应的节点是否存在
    if (context != null) {
      //获取配置的工厂对象的加载位置
      String type = context.getStringAttribute("type");
      //获取配置给本工厂对象的属性信息
      Properties properties = context.getChildrenAsProperties();
      //创建对应的对象工厂类对象
      ObjectFactory factory = (ObjectFactory) resolveClass(type).newInstance();
      //给对象工厂设置对应的属性信息
      factory.setProperties(properties);
      ///将配置的工厂对象设置到配置信息类对象中
      configuration.setObjectFactory(factory);
    }
  }

  /**
   * 解析对应的objectWrapperFactory节点的包装对象工厂
   */
  private void objectWrapperFactoryElement(XNode context) throws Exception {
    //检测对应的节点是否存在
    if (context != null) {
      //获取设置的包装对象工厂的类位置
      String type = context.getStringAttribute("type");
      //加载并创建对应的包装对象工厂
      ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).newInstance();
      //将对应的包装对象工厂设置给配置信息对象
      configuration.setObjectWrapperFactory(factory);
    }
  }

  /**
   * 解析对应的reflectorFactory节点的获取设置的反射工厂处理类
   */
  private void reflectorFactoryElement(XNode context) throws Exception {
    //检测对应的节点是否存在
    if (context != null) {
      //获取配置的反射工厂对应的位置
      String type = context.getStringAttribute("type");
      //加载对应的类对象并创建对应的反射工厂处理类对象
      ReflectorFactory factory = (ReflectorFactory) resolveClass(type).newInstance();
      //给配置信息对象设置对应的反射工厂处理类对象
      configuration.setReflectorFactory(factory);
    }
  }

  /**
   * 根据解析的settings节点获取的属性信息来设置配置信息类对象
   */
  private void settingsElement(Properties props) {
    configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
    configuration.setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
    configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
    configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
    configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
    configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), false));
    configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));
    configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
    configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
    configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
    configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
    configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));
    //是否开启自动驼峰命名规则（camel case）映射，即从经典数据库列名 A_COLUMN 到经典 Java 属性名 aColumn 的类似映射
    configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
    configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
    configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
    configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
    configuration.setLazyLoadTriggerMethods(stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
    configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
    configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
    configuration.setDefaultEnumTypeHandler(resolveClass(props.getProperty("defaultEnumTypeHandler")));
    configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
    configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), true));
    configuration.setReturnInstanceForEmptyRow(booleanValueOf(props.getProperty("returnInstanceForEmptyRow"), false));
    configuration.setLogPrefix(props.getProperty("logPrefix"));
    configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
  }

  /**
   * 解析配置文件中environments节点配置的数据库执行环境信息
   * environments的作用是用来配置数据库信息，可以配置多个，其有两个可配的子元素，分别是：事务管理器transactionManager和数据源dataSource
     数据库环境信息的配置实例
      <environments default="development">
        <!--环境变量 -->
        <environment id="development">
          <!--事务管理器 -->
          <transactionManager type="MANAGED" />
          <!--数据源 -->
          <dataSource type="POOLED">
            <property name="driver" value="${db.driver}" />
            <property name="url" value="${db.url}" />
            <property name="username" value="${db.username}" />
            <property name="password" value="${db.pwd}" />
          </dataSource>
        </environment>
        <!--环境变量 -->
        <environment id="development">
          <!--事务管理器 -->
          <transactionManager type="JDBC" />
           <!--数据源 -->
          <dataSource type="POOLED">
            <property name="driver" value="${db.driver}" />
            <property name="url" value="${db.url}" />
            <property name="username" value="${db.username}" />
            <property name="password" value="${db.pwd}" />
          </dataSource>
        </environment>
      </environments>
   配置项说明
   1 environments-default：该属性指定当前的环境，有development和work两种选择，默认是development开发模式；
   2 environment-id：该属性是每个environment定义的环境，也有development和work两种选择，默认是development开发模式；
   3 transactionManager-type：该属性是配置事务管理器的类型，mybatis中有JDBC和MANAGED两种，一次只能配置一个，后面会介绍；
   4 dataSource－type：该属性用来配置数据源类型，有UNPOOLED、POOLED和JNDI三种选择，后面会介绍；
   5 dataSource中的property元素就是数据库相关的信息
     注意：environment可以配置多个，但是如果同一种模式，如development模式，配置了两个环境environment，mybatis会用后面的覆盖前面的

   通过分析 其实这个地方也有问题  如果配置的数据库环境信息 与 配置的相关环境信息没有匹配的  那么是不是就没有对应的数据库环境可以使用了
   是不是可以在给配置信息类对象设置数据库环境之后 最后进行一次检测是否配置好了数据库环境信息
   */
  private void environmentsElement(XNode context) throws Exception {
    //检测是否配置了对应的节点
    if (context != null) {
      //检测是否通过全局配置了环境信息
      if (environment == null) {
        //获取当前环境节点中对应的默认环境变量信息
        environment = context.getStringAttribute("default");
      }
      //循环遍历环境节点中对应的子节点信息(即environments中可以有多个environment子节点   所有需要根据一定的策略来选择对应的执行环境)
      //此处的目的是  可以配置多个环境对象(例如测试环境 生成环境  就需要两套对应的环境信息配置)
      for (XNode child : context.getChildren()) {
        //获取对应的环境id属性-------->注意此处的id属性在environment节点中必须要进行配置
        String id = child.getStringAttribute("id");
        //检测当前配置的环境信息是否是需要的执行环境信息
        if (isSpecifiedEnvironment(id)) {
          //解析获取对应的事物工厂对象
          TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
          //解析获取对应的数据源工厂对象
          DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
          //通过数据源工厂创建对应的数据源对象
          DataSource dataSource = dsFactory.getDataSource();
          //通过环境id 事物工厂 数据源构建对应的环境装配类对象
          Environment.Builder environmentBuilder = new Environment.Builder(id).transactionFactory(txFactory).dataSource(dataSource);
          //给配置信息对象设置对应的执行环境对象
          configuration.setEnvironment(environmentBuilder.build());
        }
      }
    }
  }

  /**
   * 检测给定id值的环境信息是否是当前需要的执行环境信息
   *   注意配置的environment来源有两条对应的来源
   *     1 通过暴露在最外边的门面对象来传入数据库环境信息
   *     2 通过xml配置文件中指定的默认环境信息
   *    需要注意 外部门面传入的环境变量值要高于xml配置文件中配置的环境信息值
   */
  private boolean isSpecifiedEnvironment(String id) {
    //首先检测是否配置好对应的数据库环境信息
    if (environment == null) {
      //抛出对应的异常 提示配置数据库环境信息
      throw new BuilderException("No environment specified.");
    } else if (id == null) {
      //抛出对应的异常  即 所有的数据库环境都需要带对应的id标识
      throw new BuilderException("Environment requires an id attribute.");
    } else if (environment.equals(id)) {
      //检测对应的id环境与数据库环境是否相匹配
      return true;
    }
    return false;
  }

  /**
   * 解析配置文件中environment节点下配置的transactionManager节点中事物管理器对象的信息
   */
  private TransactionFactory transactionManagerElement(XNode context) throws Exception {
    //检测对应的节点是否存在
    if (context != null) {
      //获取进行事物管理的类型
      String type = context.getStringAttribute("type");
      //获取配置的属性信息
      Properties props = context.getChildrenAsProperties();
      //根据提供的类型创建对应的事物管理器工厂对象
      TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
      //给事物管理器对象设置对应的属性
      factory.setProperties(props);
      //返回对应的事物管理器工厂对象
      return factory;
    }
    //对应的节点不存在就抛出异常------------->所以执行环境中必须要配置对应的transactionManager节点
    throw new BuilderException("Environment declaration requires a TransactionFactory.");
  }

  /**
   * 解析配置文件中environment节点下配置的dataSource节点中数据源工厂的信息
   */
  private DataSourceFactory dataSourceElement(XNode context) throws Exception {
    //检测对应的节点是否存在
    if (context != null) {
      //获取配置的数据源工厂类型
      String type = context.getStringAttribute("type");
      //获取对应的配置属性信息
      Properties props = context.getChildrenAsProperties();
      //创建对应类型的数据源工厂对象
      DataSourceFactory factory = (DataSourceFactory) resolveClass(type).newInstance();
      //给数据源工厂对象配置对应的属性信息
      factory.setProperties(props);
      //返回对应的数据源工厂对象
      return factory;
    }
    //对应的节点不存在就抛出异常------------->所以执行环境中必须要配置对应的dataSource节点
    throw new BuilderException("Environment declaration requires a DataSourceFactory.");
  }

  /**
   * https://www.jianshu.com/p/37c55a90bd28
   * http://www.pianshen.com/article/228224019/
   * https://www.cnblogs.com/happyflyingpig/p/7689289.html
   * https://www.cnblogs.com/hellowhy/p/9676037.html
   * @param context
   * @throws Exception
   */
  private void databaseIdProviderElement(XNode context) throws Exception {
    DatabaseIdProvider databaseIdProvider = null;
    if (context != null) {
      String type = context.getStringAttribute("type");
      // awful patch to keep backward compatibility
      if ("VENDOR".equals(type)) {
        type = "DB_VENDOR";
      }
      Properties properties = context.getChildrenAsProperties();
      databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
      databaseIdProvider.setProperties(properties);
    }
    Environment environment = configuration.getEnvironment();
    if (environment != null && databaseIdProvider != null) {
      String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
      configuration.setDatabaseId(databaseId);
    }
  }

  private void typeHandlerElement(XNode parent) {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        if ("package".equals(child.getName())) {
          String typeHandlerPackage = child.getStringAttribute("name");
          typeHandlerRegistry.register(typeHandlerPackage);
        } else {
          String javaTypeName = child.getStringAttribute("javaType");
          String jdbcTypeName = child.getStringAttribute("jdbcType");
          String handlerTypeName = child.getStringAttribute("handler");
          Class<?> javaTypeClass = resolveClass(javaTypeName);
          JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
          Class<?> typeHandlerClass = resolveClass(handlerTypeName);
          if (javaTypeClass != null) {
            if (jdbcType == null) {
              typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
            } else {
              typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
            }
          } else {
            typeHandlerRegistry.register(typeHandlerClass);
          }
        }
      }
    }
  }

  private void mapperElement(XNode parent) throws Exception {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        if ("package".equals(child.getName())) {
          String mapperPackage = child.getStringAttribute("name");
          configuration.addMappers(mapperPackage);
        } else {
          String resource = child.getStringAttribute("resource");
          String url = child.getStringAttribute("url");
          String mapperClass = child.getStringAttribute("class");
          if (resource != null && url == null && mapperClass == null) {
            ErrorContext.instance().resource(resource);
            InputStream inputStream = Resources.getResourceAsStream(resource);
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
            mapperParser.parse();
          } else if (resource == null && url != null && mapperClass == null) {
            ErrorContext.instance().resource(url);
            InputStream inputStream = Resources.getUrlAsStream(url);
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
            mapperParser.parse();
          } else if (resource == null && url == null && mapperClass != null) {
            Class<?> mapperInterface = Resources.classForName(mapperClass);
            configuration.addMapper(mapperInterface);
          } else {
            throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
          }
        }
      }
    }
  }

}
