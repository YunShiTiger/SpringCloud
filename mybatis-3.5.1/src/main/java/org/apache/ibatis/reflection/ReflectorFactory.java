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
package org.apache.ibatis.reflection;

/**
 * 统一的反射类处理工厂接口
 *   主要用于完成通过本工厂来完成获取对应类所对应的反射操作处理类对象
 *   核心在与是否启动缓存来缓存对应的已经解析完成的反射信息类
 */
public interface ReflectorFactory {

  //检测是否具有缓存反射处理类功能
  boolean isClassCacheEnabled();

  //设置是否开启对应的缓存反射处理类功能
  void setClassCacheEnabled(boolean classCacheEnabled);

  //获取对应的类所对应的反射处理类对象
  Reflector findForClass(Class<?> type);
}