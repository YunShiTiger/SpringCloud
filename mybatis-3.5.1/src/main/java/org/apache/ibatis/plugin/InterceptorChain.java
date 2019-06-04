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
package org.apache.ibatis.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 插件链:记录配置文件中所有配置的插件信息
 */
public class InterceptorChain {

  //用于记录插件的集合
  private final List<Interceptor> interceptors = new ArrayList<>();

  //执行配置的所有插件动作
  public Object pluginAll(Object target) {
    //循环执行配置的所有插件
    for (Interceptor interceptor : interceptors) {
      //执行对应的插件
      target = interceptor.plugin(target);
    }
    //返回执行完所有插件之后对应的结果
    return target;
  }

  //将对应的插件添加到插件链中
  public void addInterceptor(Interceptor interceptor) {
    interceptors.add(interceptor);
  }

  //获取插件链中配置的所有插件
  public List<Interceptor> getInterceptors() {
    return Collections.unmodifiableList(interceptors);
  }

}
