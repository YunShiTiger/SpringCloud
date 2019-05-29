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
 * 获取字段属性值的触发器处理类
 */
public class GetFieldInvoker implements Invoker {

  //记录对应的字段
  private final Field field;

  public GetFieldInvoker(Field field) {
    this.field = field;
  }

  @Override
  public Object invoke(Object target, Object[] args) throws IllegalAccessException {
    try {
      //获取字段对应的值
      return field.get(target);
    } catch (IllegalAccessException e) {
      //遇到访问权限问题 检测是否可以进行改动访问权限
      if (Reflector.canControlMemberAccessible()) {
        //设置此字段可以进行访问
        field.setAccessible(true);
        //重新尝试获取对应字段的值
        return field.get(target);
      } else {
        //不能改动访问权限 就抛出对应的异常
        throw e;
      }
    }
  }

  @Override
  public Class<?> getType() {
    return field.getType();
  }
}
