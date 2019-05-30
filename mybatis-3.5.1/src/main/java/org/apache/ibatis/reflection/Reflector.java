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
package org.apache.ibatis.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.ReflectPermission;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.invoker.SetFieldInvoker;
import org.apache.ibatis.reflection.property.PropertyNamer;

/**
 * This class represents a cached set of class definition information that allows for easy mapping between property names and getter/setter methods.
 * 本类完成了对给定类的解析,从而可以以反射的方式来进行操作对应类的属性和方法
 * 本类主要完成对给定类的  字段解析 get或者is开头的方法 已经 以set开头的方法的解析  从而完全掌握对应的解析的类中所有可以读取或者设置的属性值,从而进行操纵类的对应属性
 */
public class Reflector {

  //用于记录对应的待解析的类
  private final Class<?> type;
  //用于记录对应的解析类的默认构造函数
  private Constructor<?> defaultConstructor;
  //记录所有可以读取的属性名称
  private final String[] readablePropertyNames;
  //记录所有可以写入的属性名称
  private final String[] writablePropertyNames;
  //记录所有可以读取的属性名称和对应的触发方法(以get或者is开头的方法 或者字段)   注意是放置的是Invoker
  private final Map<String, Invoker> setMethods = new HashMap<>();
  //记录所有可以写入的属性名称和对应的触发方法(以set开头的方法 或者字段)
  private final Map<String, Invoker> getMethods = new HashMap<>();
  //用于存储set类型方法或者字段  属性名称和对应的方法参数类型(以set开头的方法 或者字段)
  private final Map<String, Class<?>> setTypes = new HashMap<>();
  //用于存储get类型方法或者字段  属性名称和对应的返回值类型(以get或者is开头的方法 或者字段)
  private final Map<String, Class<?>> getTypes = new HashMap<>();

  //用于存储不区分大小写的属性集合  存储了所有的属性值 来源是 get is set 方法属性 和 字段属性
  //为什么存在这个集合  目的是能够在转驼峰  即正确识别对应的属性  不区分字符的大小写形式
  private Map<String, String> caseInsensitivePropertyMap = new HashMap<>();

  public Reflector(Class<?> clazz) {
    //用于记录对应的需要待解析的类
    type = clazz;
    //解析类对应的默认构造函数
    addDefaultConstructor(clazz);
    //解析类对应的get方法
    addGetMethods(clazz);
    //解析类对应的set方法
    addSetMethods(clazz);
    //解析类对应的属性字段
    addFields(clazz);
    //获取所有可读的属性
    readablePropertyNames = getMethods.keySet().toArray(new String[getMethods.keySet().size()]);
    //获取所有可写的属性
    writablePropertyNames = setMethods.keySet().toArray(new String[setMethods.keySet().size()]);
    //首先记录所有可读的属性
    for (String propName : readablePropertyNames) {
      caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
    //然后记录所有可读的属性
    for (String propName : writablePropertyNames) {
      caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
  }

  /**
   * 解析并获取对应的默认构造方法
   *   注意在老版本的处理过程中还有检查对应的构造函数是否是私有方法的处理
   */
  private void addDefaultConstructor(Class<?> clazz) {
    //获取在类中所有的构造方法对象
    Constructor<?>[] consts = clazz.getDeclaredConstructors();
    //循环检测获取到的所有构造方法对象
    for (Constructor<?> constructor : consts) {
      //检测对应的方法参数个数是否是0个------------------------->即默认的构造函数的参数一定是0个参数
      if (constructor.getParameterTypes().length == 0) {
        //设置默认的构造函数对象
        this.defaultConstructor = constructor;
      }
    }
  }

  /**
   * 解析并获取对应的get方法
   *   确定是get类型方法的几个基本点
   *   1 方法参数个数为0
   *   2 方法开头为get或者is
   */
  private void addGetMethods(Class<?> cls) {
    Map<String, List<Method>> conflictingGetters = new HashMap<>();
    //获取类中记录的所有方法
    Method[] methods = getClassMethods(cls);
    //遍历所有的方法
    for (Method method : methods) {
      //过滤参数个数大于0的方法
      if (method.getParameterTypes().length > 0) {
        continue;
      }
      //获取对应的方法名称
      String name = method.getName();
      //获取需要的以get或者is开头的方法
      if ((name.startsWith("get") && name.length() > 3) || (name.startsWith("is") && name.length() > 2)) {
        //获取方法名对应的属性名称------>即截取get或者is之后的字符串处理操作
        name = PropertyNamer.methodToProperty(name);
        //将对应的方法添加到对应的集合中进行存储处理---->后期需要进行冲突性解决
        addMethodConflict(conflictingGetters, name, method);
      }
    }
    //进一步解决方法重载造成的影响
    resolveGetterConflicts(conflictingGetters);
  }

  /**
   * 解决在解析所有get属性方法时造成的方法重载问题----------->即找到一个最终合适的方法  后期就使用这个方法来触发对应的属性操作
   *
   *   通过分析可以看出 关键还是看同名属性函数对应的返回值来进行区分
   */
  private void resolveGetterConflicts(Map<String, List<Method>> conflictingGetters) {
    //遍历所有的冲突方法集合
    for (Entry<String, List<Method>> entry : conflictingGetters.entrySet()) {
      //用于记录对应的获胜函数
      Method winner = null;
      //获取对应的属性名称
      String propName = entry.getKey();
      //循环遍历同属性名下的重载方法
      for (Method candidate : entry.getValue()) {
        //处理首次进入记录的获胜的方法
        if (winner == null) {
          //将第一个同名函数做为获胜函数
          winner = candidate;
          //继续遍历后续的同名函数
          continue;
        }
        //获取获胜者方法的返回值
        Class<?> winnerType = winner.getReturnType();
        //获取候选者方法的返回值
        Class<?> candidateType = candidate.getReturnType();
        //根据返回值的类型的检测谁是对应的获胜方    相同   派生
        //https://www.cnblogs.com/hzhuxin/p/7536671.html
        //https://www.cnblogs.com/greatfish/p/6097507.html
        if (candidateType.equals(winnerType)) {
          if (!boolean.class.equals(candidateType)) {
            throw new ReflectionException("Illegal overloaded getter method with ambiguous type for property " + propName + " in class " + winner.getDeclaringClass() + ". This breaks the JavaBeans specification and can cause unpredictable results.");
          } else if (candidate.getName().startsWith("is")) {
            //此处可以发现同名的is要高于对应的get方法对应的属性
            winner = candidate;
          }
        } else if (candidateType.isAssignableFrom(winnerType)) {
          // OK getter type is descendant
        } else if (winnerType.isAssignableFrom(candidateType)) {
          //此处说明了父类或者是父接口要低于子类或者实现
          winner = candidate;
        } else {
          throw new ReflectionException("Illegal overloaded getter method with ambiguous type for property " + propName + " in class " + winner.getDeclaringClass() + ". This breaks the JavaBeans specification and can cause unpredictable results.");
        }
      }
      //将解析完后对应的属性和方法存储到反射类对象中
      addGetMethod(propName, winner);
    }
  }

  /**
   * 存储对应的get类型属性方法和对应的触发方法   即对需要触发的方法进行了进一步的封装操作处理
   */
  private void addGetMethod(String name, Method method) {
    //进一步检测给定的属性名称是否合法
    if (isValidPropertyName(name)) {
      //将对应的属性和方法添加到获取属性方法的集合中--------->注意此处又对对应的方法进行了一层封装操作处理  目的是后期 方法和属性 都通过统一的接口进行触发对应的操作属性的处理
      getMethods.put(name, new MethodInvoker(method));
      //获取对应的返回值类型
      Type returnType = TypeParameterResolver.resolveReturnType(method, type);
      //将对应的属性名称和返回值类型放置到集合中
      getTypes.put(name, typeToClass(returnType));
    }
  }

  /**
   * 解析并获取对应的set类型的属性方法
   *   通过分析代码可以知道
   *     对应是set属性的方法  以set开头  同时有一个传入参数
   */
  private void addSetMethods(Class<?> cls) {
    //创建对应的存储方法重载带来的冲突的集合对象
    Map<String, List<Method>> conflictingSetters = new HashMap<>();
    //获取所有的方法
    Method[] methods = getClassMethods(cls);
    //循环遍历所有的方法
    for (Method method : methods) {
      //获取对应的方法名称
      String name = method.getName();
      //检测方法名是否是set开头
      if (name.startsWith("set") && name.length() > 3) {
        //检测对应的方法参数是否是一个
        if (method.getParameterTypes().length == 1) {
          //获取本方法对应的属性名称
          name = PropertyNamer.methodToProperty(name);
          //将对应的属性和方法添加到冲突集合中
          addMethodConflict(conflictingSetters, name, method);
        }
      }
    }
    //最终分析并处理方法名重载带来的冲突问题
    resolveSetterConflicts(conflictingSetters);
  }

  /**
   * 处理方法名相同带来的重载操作存储处理
   */
  private void addMethodConflict(Map<String, List<Method>> conflictingMethods, String name, Method method) {
    List<Method> list = conflictingMethods.computeIfAbsent(name, k -> new ArrayList<>());
    //将对应的方法存储到对应的list集合中，方便后期进行方法冲突的处理  即此处先进行存储 同名方法 后期来进行解决冲突
    list.add(method);
  }

  /**
   * 解除一个set属性方法对应有多个重载方法的处理
   */
  private void resolveSetterConflicts(Map<String, List<Method>> conflictingSetters) {
    //遍历对应的存储冲突的集合
    for (String propName : conflictingSetters.keySet()) {
      //获取对应属性的冲突函数集合
      List<Method> setters = conflictingSetters.get(propName);
      //在对应的get集合中获取对应属性方法的返回值类型   注意这个地方可能拿到null  即没有对应的get属性方法 则不会有对应的返回值类型
      //此处的这个值 只是起到一个更好的选择的标识
      Class<?> getterType = getTypes.get(propName);
      Method match = null;
      ReflectionException exception = null;
      //循环遍历同属性名下的set方法
      for (Method setter : setters) {
        //获取set属性方法的参数类型
        Class<?> paramType = setter.getParameterTypes()[0];
        //检测对应的参数类型和对应的返回值类型是否匹配
        if (paramType.equals(getterType)) {
          // should be the best match
          //能够匹配说明是最佳的set属性方法
          match = setter;
          //进行跳出处理,不再进行查询处理了
          break;
        }
        //检测在不出现对应的异常的情况下才进行比较处理  即如果在比较中出现了异常 就不进行后续的同属性方法的比较处理了
        if (exception == null) {
          try {
            //进行比较处理---->选择一个最好的匹配方法
            match = pickBetterSetter(match, setter, propName);
          } catch (ReflectionException e) {
            // there could still be the 'best match'
            //在进行比较处理时出现了异常 就设置最终匹配结果为空
            match = null;
            //记录对应的异常  放置后续进行进一步比较操作处理
            exception = e;
          }
        }
      }
      //检测是否找到了本属性对应的最好的方法
      if (match == null) {
        throw exception;
      } else {
        //将对应的属性名称和方法添加到对应的集合中
        addSetMethod(propName, match);
      }
    }
  }

  /**
   * 通过比较两个set属性方法来确定出一个最好的属性方法
   */
  private Method pickBetterSetter(Method setter1, Method setter2, String property) {
    //此处处理循环过程中首次比较的处理
    if (setter1 == null) {
      return setter2;
    }
    //获取对应的参数类型
    Class<?> paramType1 = setter1.getParameterTypes()[0];
    Class<?> paramType2 = setter2.getParameterTypes()[0];
    //通过比对对应的继承类型来进行确认那个方法更合适
    if (paramType1.isAssignableFrom(paramType2)) {
      //此处可以说明子类比对应的父类更合适
      return setter2;
    } else if (paramType2.isAssignableFrom(paramType1)) {
      return setter1;
    }
    //不属于对应的继承关系的直接抛出对应的异常   即同一个属性方法的参数类型不具有继承关系  即对应的类型都不同
    throw new ReflectionException("Ambiguous setters defined for property '" + property + "' in class '" + setter2.getDeclaringClass() + "' with types '" + paramType1.getName() + "' and '" + paramType2.getName() + "'.");
  }

  /**
   * 将对应的set属性名称和方法添加集合中
   */
  private void addSetMethod(String name, Method method) {
    //进一步验证属性名称的合法性
    if (isValidPropertyName(name)) {
      //将对应的属性名称和触发方法放置到set属性方法集合中
      setMethods.put(name, new MethodInvoker(method));
      //获取对应的参数类型
      Type[] paramTypes = TypeParameterResolver.resolveParamTypes(method, type);
      //将对应的属性名称和参数类型放置到集合中
      setTypes.put(name, typeToClass(paramTypes[0]));
    }
  }

  /**
   * 比较不错的两篇文章
   * https://cloud.tencent.com/developer/article/1121266?from=10680
   * https://cloud.tencent.com/developer/article/1121268
   */
  private Class<?> typeToClass(Type src) {
    Class<?> result = null;
    if (src instanceof Class) {
      result = (Class<?>) src;
    } else if (src instanceof ParameterizedType) {
      result = (Class<?>) ((ParameterizedType) src).getRawType();
    } else if (src instanceof GenericArrayType) {
      Type componentType = ((GenericArrayType) src).getGenericComponentType();
      if (componentType instanceof Class) {
        result = Array.newInstance((Class<?>) componentType, 0).getClass();
      } else {
        Class<?> componentClass = typeToClass(componentType);
        result = Array.newInstance(componentClass, 0).getClass();
      }
    }
    //检测上述处理后没有找到对应的类型
    if (result == null) {
      //返回设置默认的对象类型
      result = Object.class;
    }
    return result;
  }

  /**
   * 解析类中对应的属性字段信息
   *   通过解析本函数的处理行为发现
   *      如果对应的字段属性名称在 已经解析过的set或者get属性方法中 那么就不将本字段插入到里边
   *      即set和get方法的同名属性要比同名的字段属性的优先级要高
   */
  private void addFields(Class<?> clazz) {
    //获取类中对应的所有属性字段信息
    Field[] fields = clazz.getDeclaredFields();
    //循环遍历所有的属性字段
    for (Field field : fields) {
      //检测在对应的set属性方法中是否有对应的本字段对应的属性方法
      if (!setMethods.containsKey(field.getName())) {
        // issue #379 - removed the check for final because JDK 1.5 allows
        // modification of final fields through reflection (JSR-133). (JGB)
        // pr #16 - final static can only be set by the classloader
        //获取字段的描述信息
        int modifiers = field.getModifiers();
        //排除静态和final类型的字段属性
        if (!(Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers))) {
          //将对应的字段属性添加到对应的set属性集合--->此处完成了将只有对应的字段没有对应的set方法的那种字段属性添加到set集合中的处理
          addSetField(field);
        }
      }

      //检测在对应的get属性方法中是否有对应的本字段对应的属性方法
      if (!getMethods.containsKey(field.getName())) {
        //将对应的字段属性添加到对应的get属性集合中--->此处完成了将只有对应的字段没有对应的get方法的那种字段属性添加到get属性集合中的处理
        addGetField(field);
      }
    }
    //解析对应的父类中的属性字段信息
    if (clazz.getSuperclass() != null) {
      addFields(clazz.getSuperclass());
    }
  }

  /**
   * 将对应的字段属性添加到对应的set集合中
   */
  private void addSetField(Field field) {
    //检测对应的字段名是否合法
    if (isValidPropertyName(field.getName())) {
      //将对应的字段名和对应的设置字段的方法添加到集合中  注意此处设置的SetFieldInvoker触发器
      setMethods.put(field.getName(), new SetFieldInvoker(field));
      //解析字段的类型
      Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
      //将对应的字段属性名称和字段类型放置到集合中
      setTypes.put(field.getName(), typeToClass(fieldType));
    }
  }

  /**
   * 将对应的字段属性添加到对应的get集合中
   */
  private void addGetField(Field field) {
    //检测对应的字段名是否合法
    if (isValidPropertyName(field.getName())) {
      //将对应的字段名和对应的获取字段的方法添加到集合中  注意此处设置的GetFieldInvoker触发器
      getMethods.put(field.getName(), new GetFieldInvoker(field));
      //解析字段的类型
      Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
      //将对应的字段属性名称和字段类型放置到集合中
      getTypes.put(field.getName(), typeToClass(fieldType));
    }
  }

  /**
   * 检测给定的属性名称是否有效
   * $ serialVersionUID class
   */
  private boolean isValidPropertyName(String name) {
    return !(name.startsWith("$") || "serialVersionUID".equals(name) || "class".equals(name));
  }

  /**
   * This method returns an array containing all methods declared in this class and any superclass.
   * We use this method, instead of the simpler Class.getMethods(),because we want to look for private methods as well.
   * 此处获取给定类的所有方法----->方法包含三种来源
   *   1 类本身中的方法
   *   2 类实现接口中的方法
   *   3 父类对象的方法
   * 注意此处没有使用Class.getMethods()而是使用了Class.getDeclaredMethods() 目的是获取类中对应的私有方法
   * public Method[] getMethods()返回某个类的所有公用（public）方法包括其继承类的公用方法，当然也包括它所实现接口的方法。
   * public Method[] getDeclaredMethods()对象表示的类或接口声明的所有方法，包括公共、保护、默认（包）访问和私有方法，但不包括继承的方法。当然也包括它所实现接口的方法。
   */
  private Method[] getClassMethods(Class<?> cls) {
    //首先定义存储唯一函数的集合对象
    Map<String, Method> uniqueMethods = new HashMap<>();
    Class<?> currentClass = cls;
    //循环遍历当前类对应的继承类和实现接口
    while (currentClass != null && currentClass != Object.class) {
      //首先将本类中的方法添加到唯一集合中
      addUniqueMethods(uniqueMethods, currentClass.getDeclaredMethods());

      // we also need to look for interface methods - because the class may be abstract
      //获取当前类所实现的接口
      Class<?>[] interfaces = currentClass.getInterfaces();
      //循环处理所有的接口
      for (Class<?> anInterface : interfaces) {
        //将接口中所有的方法添加到唯一集合中
        addUniqueMethods(uniqueMethods, anInterface.getMethods());
      }
      //获取对应的父类----->即需要将父类中的方法添加到唯一集合中
      currentClass = currentClass.getSuperclass();
    }
    //获取唯一集合中所有的方法集合
    Collection<Method> methods = uniqueMethods.values();
    //获取对应的方法数组
    return methods.toArray(new Method[methods.size()]);
  }

  /**
   * 将提供的所有方法添加到唯一函数集合中
   *   核心在于解决对应的方法冲突问题
   */
  private void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
    //循环遍历所有需要添加到唯一集合中的方法
    for (Method currentMethod : methods) {
      //桥接方法说明--->本质上还是泛型抹除
      //https://www.cnblogs.com/zsg88/p/7588929.html
      //此处需要过滤对应的桥接方法
      if (!currentMethod.isBridge()) {
        //获取方法对应的签名字符串------->即可以看做是方法的唯一标识属性
        String signature = getSignature(currentMethod);
        // check to see if the method is already known if it is known, then an extended class must have overridden a method
        //检测对应的签名函数是否已经存储在唯一集合中
        if (!uniqueMethods.containsKey(signature)) {
          //将此方法签名和对应的方法添加到唯一集合中
          uniqueMethods.put(signature, currentMethod);
        }
      }
    }
  }

  /**
   * 构建方法的签名
   */
  private String getSignature(Method method) {
    StringBuilder sb = new StringBuilder();
    //首先获取对应的返回值类型
    Class<?> returnType = method.getReturnType();
    //检测对应的返回值类型是否为空
    if (returnType != null) {
      //设置返回值类型到拼接字符串中
      sb.append(returnType.getName()).append('#');
    }
    //拼接函数的名称
    sb.append(method.getName());
    //获取所有的参数类型
    Class<?>[] parameters = method.getParameterTypes();
    //循环处理所有的参数类型,进行对参数类型的拼接操作处理
    for (int i = 0; i < parameters.length; i++) {
      if (i == 0) {
        sb.append(':');
      } else {
        sb.append(',');
      }
      //拼接参数的类型
      sb.append(parameters[i].getName());
    }
    //返回拼接后的函数签名对应的字符串
    return sb.toString();
  }

  /** Checks whether can control member accessible.
   * 检测是否具有访问类中私有方法的权限
   * https://www.programcreek.com/java-api-examples/index.php?api=java.lang.reflect.ReflectPermission
   * https://www.aliyun.com/jiaocheng/774888.html
   * http://www.cnblogs.com/yiwangzhibujian/p/6207212.html
   * https://blog.csdn.net/xiang_shuo/article/details/80250164
   */
  public static boolean canControlMemberAccessible() {
    try {
      //获取对应的安全管理器处理类对象
      SecurityManager securityManager = System.getSecurityManager();
      if (null != securityManager) {
        //检测是否有对应的权限   没有就会抛出对应的异常
        securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
      }
    } catch (SecurityException e) {
      return false;
    }
    return true;
  }

  /**
   * Gets the name of the class the instance provides information for.
   * 获取当前解析的类对象
   */
  public Class<?> getType() {
    return type;
  }

  /**
   * 获取类对象对应的默认构造函数
   */
  public Constructor<?> getDefaultConstructor() {
    if (defaultConstructor != null) {
      return defaultConstructor;
    } else {
      throw new ReflectionException("There is no default constructor for " + type);
    }
  }

  /**
   * 检测是否设置了对应的默认构造函数
   */
  public boolean hasDefaultConstructor() {
    return defaultConstructor != null;
  }

  /**
   * 对外提供的获取对应属性对应的设置属性触发器对象
   */
  public Invoker getSetInvoker(String propertyName) {
    Invoker method = setMethods.get(propertyName);
    if (method == null) {
      throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
    }
    return method;
  }

  /**
   * 对外提供的获取对应属性对应的获取属性触发器对象
   */
  public Invoker getGetInvoker(String propertyName) {
    Invoker method = getMethods.get(propertyName);
    if (method == null) {
      throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
    }
    return method;
  }

  /**
   *  Gets the type for a property setter.
   * 获取设置属性对应的类型  即 set属性方法的参数类型 或者 字段属性的类型
   */
  public Class<?> getSetterType(String propertyName) {
    Class<?> clazz = setTypes.get(propertyName);
    if (clazz == null) {
      throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
    }
    return clazz;
  }

  /**
   *  Gets the type for a property getter.
   * 获取读取属性对应的类型  即 get属性方法的返回值类型 或者 字段属性的类型
   */
  public Class<?> getGetterType(String propertyName) {
    Class<?> clazz = getTypes.get(propertyName);
    if (clazz == null) {
      throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
    }
    return clazz;
  }

  /**
   * Gets an array of the readable properties for an object.
   * 获取所有可以读取的属性集合
   */
  public String[] getGetablePropertyNames() {
    return readablePropertyNames;
  }

  /**
   * Gets an array of the writable properties for an object.
   * 获取所有可以写入的属性集合
   */
  public String[] getSetablePropertyNames() {
    return writablePropertyNames;
  }

  /**
   * Check to see if a class has a writable property by name.
   * 通过传入的属性名称来检测是否有对应的可写属性
   */
  public boolean hasSetter(String propertyName) {
    return setMethods.keySet().contains(propertyName);
  }

  /**
   * Check to see if a class has a readable property by name.
   * 通过传入的属性名称来检测是否有对应的可读属性
   */
  public boolean hasGetter(String propertyName) {
    return getMethods.keySet().contains(propertyName);
  }

  /**
   * 根据传入的属性名来获取对应的真实对应的属性名  即传入的属性名的大小写随便 从而获取真正的属性名
   */
  public String findPropertyName(String name) {
    return caseInsensitivePropertyMap.get(name.toUpperCase(Locale.ENGLISH));
  }
}
