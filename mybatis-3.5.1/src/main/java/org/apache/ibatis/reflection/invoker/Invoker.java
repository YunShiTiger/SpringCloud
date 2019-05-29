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
package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.InvocationTargetException;

/**
 * 统一的触发器处理接口
 *     可以出发对应的方法执行    可以触发获取对应的属性   可以触发设置对应的属性
 * 通过对代码的进一步分析  可以知道 其实本类就是进行操纵类中对应的可读或者可写属性的统一接口
 *     其中对应的属性有  字段  以get或者is开头的属性方法  以set开头的属性方法
 */
public interface Invoker {

  //统一定义触发指定对应行为的接口
  Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException;

  Class<?> getType();
}
