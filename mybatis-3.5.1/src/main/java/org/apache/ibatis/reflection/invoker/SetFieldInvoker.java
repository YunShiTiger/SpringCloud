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
package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.Field;

import org.apache.ibatis.reflection.Reflector;

/**
 * 设置字段属性值的触发器处理类
 */
public class SetFieldInvoker implements Invoker {

  //记录对应的字段
  private final Field field;

  public SetFieldInvoker(Field field) {
    this.field = field;
  }

  @Override
  public Object invoke(Object target, Object[] args) throws IllegalAccessException {
    try {
      //给对应的字段设置值
      field.set(target, args[0]);
    } catch (IllegalAccessException e) {
      //遇到访问权限问题 检测是否可以进行改动访问权限
      if (Reflector.canControlMemberAccessible()) {
        //设置此字段可以进行访问
        field.setAccessible(true);
        //重新给字段设置值
        field.set(target, args[0]);
      } else {
        //不能改动访问权限 就抛出对应的异常
        throw e;
      }
    }
    return null;
  }

  @Override
  public Class<?> getType() {
    return field.getType();
  }
}
