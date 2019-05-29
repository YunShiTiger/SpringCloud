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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.ibatis.reflection.Reflector;

/**
 * 执行对应方法类型属性的处理类
 *   即 通过本类封装对应的触发后期方法的行为
 * get set is 等开头的属性方法
 */
public class MethodInvoker implements Invoker {

  //get或者is等对应的属性方法 此值为 对应的返回值类型
  //set等对应的属性方法  此值为 对应的参数类型
  private final Class<?> type;
  //记录对应的原始方法   后期用于通过反射来触发本方法
  private final Method method;

  public MethodInvoker(Method method) {
    this.method = method;
    //此处通过参数的个数来进一步确实是那种对应的属性方法
    if (method.getParameterTypes().length == 1) {
      type = method.getParameterTypes()[0];
    } else {
      type = method.getReturnType();
    }
  }

  @Override
  public Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException {
    try {
      //触发对应对象对应的属性方法    即获取属性值或者设置属性值的处理
      return method.invoke(target, args);
    } catch (IllegalAccessException e) {
      //反射遇到访问异常  进行检测是否可以进行改动方法的方法权限
      if (Reflector.canControlMemberAccessible()) {
        //设置对应的属性方法是可以进行访问的
        method.setAccessible(true);
        //重新触发对应的属性方法
        return method.invoke(target, args);
      } else {
        //不能改变对应的访问权限 就抛出对应的异常
        throw e;
      }
    }
  }

  @Override
  public Class<?> getType() {
    return type;
  }

}
