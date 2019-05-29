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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 默认的处理反射对象的工厂------->主要完成了缓存操作处理
 */
public class DefaultReflectorFactory implements ReflectorFactory {

  //用于标识是否开启对应的缓存操作处理  默认开始缓存功能
  private boolean classCacheEnabled = true;
  //用于记录对应的已经解析的类的反射处理类------------>注意这个地方使用的具有线程同步操作处理的map集合对象
  private final ConcurrentMap<Class<?>, Reflector> reflectorMap = new ConcurrentHashMap<>();

  public DefaultReflectorFactory() {

  }

  @Override
  public boolean isClassCacheEnabled() {
    return classCacheEnabled;
  }

  @Override
  public void setClassCacheEnabled(boolean classCacheEnabled) {
    this.classCacheEnabled = classCacheEnabled;
  }

  @Override
  public Reflector findForClass(Class<?> type) {
    //检测是否开启了缓存功能
    if (classCacheEnabled) {
      // synchronized (type) removed see issue #461
      //此处使用了高级功能  主要是 检测是否存在对应的类的反射处理类对象  如果存在就返回  如果不存在就创建 创建之后插入本集合 同时返回创建的放射处理类对象
      //此处使用了jdk8的新特性
      return reflectorMap.computeIfAbsent(type, Reflector::new);
    } else {
      //没有开始缓存功能,就直接创建对应的反射处理类对象
      return new Reflector(type);
    }
  }

}
