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
package org.apache.ibatis.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;

import org.apache.ibatis.io.Resources;
import org.junit.jupiter.api.Test;

/**
 * 用于测试封装的XPathParser解析处理类
 */
class XPathParserTest {

  @Test
  void shouldTestXPathParserMethods() throws Exception {
    //对应的需要解析的xml文件路径
    String resource = "resources/nodelet_test.xml";
    //尝试加载对应的xml文件
    try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
      //创建对应的xml解析对象
      XPathParser parser = new XPathParser(inputStream, false, null, null);
      //分别测试通过表达式获取对应数据的处理
      assertEquals((Long) 1970L, parser.evalLong("/employee/birth_date/year"));
      assertEquals((short) 6, (short) parser.evalShort("/employee/birth_date/month"));
      assertEquals((Integer) 15, parser.evalInteger("/employee/birth_date/day"));
      assertEquals((Float) 5.8f, parser.evalFloat("/employee/height"));
      assertEquals((Double) 5.8d, parser.evalDouble("/employee/height"));
      assertEquals("${id_var}", parser.evalString("/employee/@id"));
      assertEquals(Boolean.TRUE, parser.evalBoolean("/employee/active"));

      assertEquals("<id>${id_var}</id>", parser.evalNode("/employee/@id").toString().trim());
      //注意此处获取的节点已经是封装之后的元素节点了  所以是7个  不包含原生的文本节点了
      assertEquals(7, parser.evalNodes("/employee/*").size());

      XNode node = parser.evalNode("/employee/height");
      assertEquals("employee/height", node.getPath());
      assertEquals("employee[${id_var}]_height", node.getValueBasedIdentifier());
    }
  }

}
