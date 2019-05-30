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
package org.apache.ibatis.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * MetaClass是一个保存对象定义比如getter/setter/构造器等的元数据类
 *   注意Reflector只完成单个类的元数据解析  而本类是对Reflector的进一步封装 可以用于复杂关系的元数据解析  主要是借助PropertyTokenizer来完成的
 */
public class MetaClass {

  //对应的反射解析工厂对象
  private final ReflectorFactory reflectorFactory;
  //对应的解析完成的解析类对象
  private final Reflector reflector;

  /**
   * 构造对应的元数据类对象的构造方法----->注意本方法是私有方法  即禁止直接创建元数据类对象
   */
  private MetaClass(Class<?> type, ReflectorFactory reflectorFactory) {
    this.reflectorFactory = reflectorFactory;
    //此处是核心处理  即根据提供的反射工厂处理类来获取对应的解析类对象
    this.reflector = reflectorFactory.findForClass(type);
  }

  /**
   * 对外提供的获取对应类所对应的元数据类对象的处理方法
   */
  public static MetaClass forClass(Class<?> type, ReflectorFactory reflectorFactory) {
    return new MetaClass(type, reflectorFactory);
  }

  /**
   * 内部使用的根据提供的子属性来获取子属性类型对应的元数据对象
   *   即本方法是处理复杂属性中 获取子属性元数据的处理方法
   */
  public MetaClass metaClassForProperty(String name) {
    //获取给定的子属性对应的类型
    Class<?> propType = reflector.getGetterType(name);
    //获取子属性类型对应的元数据对象
    return MetaClass.forClass(propType, reflectorFactory);
  }

  /**
   * 获取提供的字符串对应的属性字符串数据
   */
  public String findProperty(String name, boolean useCamelCaseMapping) {
    //检测是否开启了转驼峰的处理标识
    if (useCamelCaseMapping) {
      //进行替换操作处理  即去除字符串中的_
      name = name.replace("_", "");
    }
    //进一步获取字符串对应的属性字符串数据
    return findProperty(name);
  }

  /**
   * 根据提供的属性字符串获取真实的在类对象中对应的属性字符串数据
   */
  public String findProperty(String name) {
    //构建对应的属性字符串对象   其实prop指向的对象就是后面的new StringBuilder()
    StringBuilder prop = buildProperty(name, new StringBuilder());
    //返回拼接后的字符串属性数据
    return prop.length() > 0 ? prop.toString() : null;
  }

  /**
   * 根据给定的属性字符串来拼接对应的在元数据中对应的属性字符串形式
   *    本函数很好的隐藏了递归操作
   */
  private StringBuilder buildProperty(String name, StringBuilder builder) {
    //根据提供的属性字符串获取对应的属性标记器对象
    PropertyTokenizer prop = new PropertyTokenizer(name);
    //检测是否有子属性
    if (prop.hasNext()) {
      //获取对应的父属性对应的属性字符串形式
      String propertyName = reflector.findPropertyName(prop.getName());
      //检测是否有对应的父属性对应的属性字符串
      if (propertyName != null) {
        //首先将父属性对应的属性字符串添加到字符串构造器中
        builder.append(propertyName);
        //添加对应的父子属性之间的分隔符
        builder.append(".");
        //获取对应的子属性类型对应的元数据对象
        MetaClass metaProp = metaClassForProperty(propertyName);
        //进一步解析子属性的属性信息
        metaProp.buildProperty(prop.getChildren(), builder);
      }
    } else {
      //直接获取对应的属性名称
      String propertyName = reflector.findPropertyName(name);
      //检测对应的属性名称是否存在
      if (propertyName != null) {
        //将对应的属性名称添加到字符串构造器中
        builder.append(propertyName);
      }
    }
    //返回对应的构造器对象
    return builder;
  }

  /**
   * 获取所有的可以读取的属性集合
   */
  public String[] getGetterNames() {
    return reflector.getGetablePropertyNames();
  }

  /**
   * 获取所有的可以写入的属性集合
   */
  public String[] getSetterNames() {
    return reflector.getSetablePropertyNames();
  }

  /**
   * 获取给定写入属性对应的类型  即写入属性是方法 那么就是 方法参数的类型    写入属性是字段 那么就是 字段的类型
   *    本方法内部也隐藏了对应的递归操作处理
   */
  public Class<?> getSetterType(String name) {
    //获取对应的属性标识器对象
    PropertyTokenizer prop = new PropertyTokenizer(name);
    //检测是否存在子属性
    if (prop.hasNext()) {
      //获取对应的子属性类型对应的元数据对象
      MetaClass metaProp = metaClassForProperty(prop.getName());
      //返回对应的子属性元数据中子属性对应的类型
      return metaProp.getSetterType(prop.getChildren());
    } else {
      //直接获取对应的属性对应的类型
      return reflector.getSetterType(prop.getName());
    }
  }

  /**
   * 获取给定读取属性对应的类型  即读取属性是方法 那么就是 方法返回值的类型    读取属性是字段 那么就是 字段的类型
   *    本方法内部也隐藏了对应的递归操作处理
   */
  public Class<?> getGetterType(String name) {
    //获取对应的属性标识器对象
    PropertyTokenizer prop = new PropertyTokenizer(name);
    //检测是否存在子属性
    if (prop.hasNext()) {
      //获取对应的子属性类型对应的元数据对象------->注意此处直接通过的PropertyTokenizer来获取对应的元数据对象
      MetaClass metaProp = metaClassForProperty(prop);
      //返回对应的子属性元数据中子属性对应的类型
      return metaProp.getGetterType(prop.getChildren());
    }
    // issue #506. Resolve the type inside a Collection Object
    //没有子属性 直接获取对应属性字符串对应的类型
    return getGetterType(prop);
  }

  /**
   * 通过传入PropertyTokenizer对象来获取元数据对象的处理函数
   */
  private MetaClass metaClassForProperty(PropertyTokenizer prop) {
    //
    Class<?> propType = getGetterType(prop);
    //
    return MetaClass.forClass(propType, reflectorFactory);
  }

  /**
   *
   */
  private Class<?> getGetterType(PropertyTokenizer prop) {
    Class<?> type = reflector.getGetterType(prop.getName());
    if (prop.getIndex() != null && Collection.class.isAssignableFrom(type)) {
      Type returnType = getGenericGetterType(prop.getName());
      if (returnType instanceof ParameterizedType) {
        Type[] actualTypeArguments = ((ParameterizedType) returnType).getActualTypeArguments();
        if (actualTypeArguments != null && actualTypeArguments.length == 1) {
          returnType = actualTypeArguments[0];
          if (returnType instanceof Class) {
            type = (Class<?>) returnType;
          } else if (returnType instanceof ParameterizedType) {
            type = (Class<?>) ((ParameterizedType) returnType).getRawType();
          }
        }
      }
    }
    return type;
  }

  /**
   * 根据提供的可读属性获取在触发器对象中存储的真实属性类型
   */
  private Type getGenericGetterType(String propertyName) {
    try {
      //获取属性对应的触发器方法对象
      Invoker invoker = reflector.getGetInvoker(propertyName);
      //根据触发器方法对象的类型进行不同的处理
      if (invoker instanceof MethodInvoker) {
        //获取类对应的对应的字段属性
        Field _method = MethodInvoker.class.getDeclaredField("method");
        //设置可以访问权限
        _method.setAccessible(true);
        //获取对应的字段属性  其实这个字段对应的是一个方法
        Method method = (Method) _method.get(invoker);
        //获取对应的方法的返回值类型
        return TypeParameterResolver.resolveReturnType(method, reflector.getType());
      } else if (invoker instanceof GetFieldInvoker) {
        //获取类对应的对应的字段属性
        Field _field = GetFieldInvoker.class.getDeclaredField("field");
        //设置可以访问权限
        _field.setAccessible(true);
        //获取对应的字段属性
        Field field = (Field) _field.get(invoker);
        //获取字段对应的类型
        return TypeParameterResolver.resolveFieldType(field, reflector.getType());
      }
    } catch (NoSuchFieldException | IllegalAccessException ignored) {

    }
    return null;
  }

  /**
   * 检测给定的写属性是否存在
   *    注意此处其实很巧妙的实现了一个对应的递归操作处理
   *      完美的借助了 MetaClass 的 hasSetter 实现了递归操作处理
   */
  public boolean hasSetter(String name) {
    //首先根据传入的写属性字符串构建对应的属性标记器对象  即首先完成复杂属性的分割处理
    PropertyTokenizer prop = new PropertyTokenizer(name);
    //检测是否存在对应的子属性   即有对应的子属性 首先需要拿到对应的父属性类 然后在进行查询子属性
    if (prop.hasNext()) {
      //检测是否有对应的父属性
      if (reflector.hasSetter(prop.getName())) {
        //根据对应的父属性 获取其对应的元数据对象
        MetaClass metaProp = metaClassForProperty(prop.getName());
        //进一步分析是否存在对应的子属性
        return metaProp.hasSetter(prop.getChildren());
      } else {
        //没有对应的父属性 直接返回false  即不存在对应的属性
        return false;
      }
    } else {
      //直接检测是否有对应的属性存在
      return reflector.hasSetter(prop.getName());
    }
  }

  /**
   * 检测给定的读属性是否存在
   */
  public boolean hasGetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      if (reflector.hasGetter(prop.getName())) {
        MetaClass metaProp = metaClassForProperty(prop);
        return metaProp.hasGetter(prop.getChildren());
      } else {
        return false;
      }
    } else {
      return reflector.hasGetter(prop.getName());
    }
  }

  /**
   * 获取触发对应获取属性值的触发方法
   */
  public Invoker getGetInvoker(String name) {
    return reflector.getGetInvoker(name);
  }

  /**
   * 获取触发对应设置属性值的触发方法
   */
  public Invoker getSetInvoker(String name) {
    return reflector.getSetInvoker(name);
  }

  /**
   * 获取默认的解析对象是否存在构造函数
   */
  public boolean hasDefaultConstructor() {
    return reflector.hasDefaultConstructor();
  }

}
