/**
 *    Copyright 2009-2015 the original author or authors.
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
package org.apache.ibatis.reflection.property;

import java.util.Locale;

import org.apache.ibatis.reflection.ReflectionException;

/**
 * 属性检测处理类对象
 *   主要用于检测提供的方法名称是否是属性方法,同时获取对应的方法对应的属性名称
 */
public final class PropertyNamer {

  /**
   * 使用私有构造函数 防止创建对应的对象处理
   */
  private PropertyNamer() {
    // Prevent Instantiation of Static Class
  }

  /**
   * 将提供的方法名称转换成对应的属性对应的名称
   */
  public static String methodToProperty(String name) {
    //根据提供的方法名称提供不同的截取操作策略
    if (name.startsWith("is")) {
      name = name.substring(2);
    } else if (name.startsWith("get") || name.startsWith("set")) {
      name = name.substring(3);
    } else {
      //对应非属性类型方法 进行抛出异常操作处理
      throw new ReflectionException("Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
    }
    //将获取的方法对应的属性名称转换成首字母大写的形式
    if (name.length() == 1 || (name.length() > 1 && !Character.isUpperCase(name.charAt(1)))) {
      name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
    }
    //返回此方法名称对应的属性名称
    return name;
  }

  /**
   * 检测提供的方法name是否是操作属性方法
   */
  public static boolean isProperty(String name) {
    return name.startsWith("get") || name.startsWith("set") || name.startsWith("is");
  }

  /**
   * 检测提供的方法name是否是获取属性的方法
   */
  public static boolean isGetter(String name) {
    return name.startsWith("get") || name.startsWith("is");
  }

  /**
   * 检测提供的方法name是否是设置属性的方法
   */
  public static boolean isSetter(String name) {
    return name.startsWith("set");
  }

}
