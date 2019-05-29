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
package org.apache.ibatis.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 对原生的Node节点的进一步封装操作处理---->以满足自己的业务需求
 */
public class XNode {

  //用于记录原生节点
  private final Node node;
  //用于记录节点对应的名称
  private final String name;
  //用于记录节点对应的内容
  private final String body;
  //获取节点上配置的属性信息
  private final Properties attributes;
  //用于记录所有的属性值对象
  private final Properties variables;
  //用于记录对应的解析器对象
  private final XPathParser xpathParser;

  public XNode(XPathParser xpathParser, Node node, Properties variables) {
    this.xpathParser = xpathParser;
    this.node = node;
    this.name = node.getNodeName();
    this.variables = variables;
    this.attributes = parseAttributes(node);
    this.body = parseBody(node);
  }

  public XNode newXNode(Node node) {
    return new XNode(xpathParser, node, variables);
  }

  /**
   * 获取节点对应的父节点
   */
  public XNode getParent() {
    //获取原生节点对应的父原生节点
    Node parent = node.getParentNode();
    //检测对应的父节点是否存在且为元素节点
    if (parent == null || !(parent instanceof Element)) {
      return null;
    } else {
      //创建对应的父节点的封装处理
      return new XNode(xpathParser, parent, variables);
    }
  }

  /**
   * 获取当前节点在对应的文档对象中对应的路径
   *   最终的格式可能有两种信息
   *       有上一级节点  /节点名称/节点名称...
   *       无上一级节点  节点名称
   */
  public String getPath() {
    StringBuilder builder = new StringBuilder();
    Node current = node;
    //循环向上拼接对应的路径信息
    while (current != null && current instanceof Element) {
      //特殊处理非当前节点信息  即插入对应的/
      if (current != node) {
        builder.insert(0, "/");
      }
      //拼接对应的节点名称
      builder.insert(0, current.getNodeName());
      //获取对应的上一级节点对象
      current = current.getParentNode();
    }
    //返回拼接后的路径信息
    return builder.toString();
  }

  /**
   * 获取当前节点对应的Value标识
   * 实例
   * <employee id="${id_var}">
   *   <blah1 something="that"/>
   *   <blah2 id="123"/>
   *   <blah3 id="1.2.3"/>
   * </employee>
   *  其中如果给定是blah1节点 那么对应的返回值是  employee[${id_var}]_blah1
   *  其中如果给定是blah2节点 那么对应的返回值是  employee[${id_var}]_blah2[123]
   *  其中如果给定是blah3节点 那么对应的返回值是  employee[${id_var}]_blah3[1_2_3]
   */
  public String getValueBasedIdentifier() {
    StringBuilder builder = new StringBuilder();
    XNode current = this;
    //循环向上遍历父级节点
    while (current != null) {
      //检测是否是当前节点
      if (current != this) {
        //非当前节点 统一先插入 _ 分隔符
        builder.insert(0, "_");
      }
      //获取当前节点的属性值  属性值的顺序是 id value property 三者都没有就默认是null
      String value = current.getStringAttribute("id", current.getStringAttribute("value", current.getStringAttribute("property", null)));
      //检测是否存在对应的属性值
      if (value != null) {
        //将找到的属性值拼接到字符串中
        value = value.replace('.', '_');
        builder.insert(0, "]");
        builder.insert(0, value);
        builder.insert(0, "[");
      }
      //插入对应节点的名称
      builder.insert(0, current.getName());
      //设置搜索父节点
      current = current.getParent();
    }
    //获取遍历到的字符串对象
    return builder.toString();
  }

  /**
   * 下面一批方法是在当前节点下解析给定表达式对应的数据内容
   */
  public String evalString(String expression) {
    return xpathParser.evalString(node, expression);
  }

  public Boolean evalBoolean(String expression) {
    return xpathParser.evalBoolean(node, expression);
  }

  public Double evalDouble(String expression) {
    return xpathParser.evalDouble(node, expression);
  }

  public List<XNode> evalNodes(String expression) {
    return xpathParser.evalNodes(node, expression);
  }

  public XNode evalNode(String expression) {
    return xpathParser.evalNode(node, expression);
  }

  public Node getNode() {
    return node;
  }

  public String getName() {
    return name;
  }

  /**
   * 下面一批方法是解析对应的body内容转换成对应格式的数据格式的处理方法
   */
  public String getStringBody() {
    return getStringBody(null);
  }

  public String getStringBody(String def) {
    if (body == null) {
      return def;
    } else {
      return body;
    }
  }

  public Boolean getBooleanBody() {
    return getBooleanBody(null);
  }

  public Boolean getBooleanBody(Boolean def) {
    if (body == null) {
      return def;
    } else {
      return Boolean.valueOf(body);
    }
  }

  public Integer getIntBody() {
    return getIntBody(null);
  }

  public Integer getIntBody(Integer def) {
    if (body == null) {
      return def;
    } else {
      return Integer.parseInt(body);
    }
  }

  public Long getLongBody() {
    return getLongBody(null);
  }

  public Long getLongBody(Long def) {
    if (body == null) {
      return def;
    } else {
      return Long.parseLong(body);
    }
  }

  public Double getDoubleBody() {
    return getDoubleBody(null);
  }

  public Double getDoubleBody(Double def) {
    if (body == null) {
      return def;
    } else {
      return Double.parseDouble(body);
    }
  }

  public Float getFloatBody() {
    return getFloatBody(null);
  }

  public Float getFloatBody(Float def) {
    if (body == null) {
      return def;
    } else {
      return Float.parseFloat(body);
    }
  }

  /**
   * 下面的一批方法是在节点配置的属性信息中查询给定键对应的属性值的处理操作
   */
  public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name) {
    return getEnumAttribute(enumType, name, null);
  }

  public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name, T def) {
    String value = getStringAttribute(name);
    if (value == null) {
      return def;
    } else {
      return Enum.valueOf(enumType, value);
    }
  }

  public String getStringAttribute(String name) {
    return getStringAttribute(name, null);
  }

  public String getStringAttribute(String name, String def) {
    String value = attributes.getProperty(name);
    if (value == null) {
      return def;
    } else {
      return value;
    }
  }

  public Boolean getBooleanAttribute(String name) {
    return getBooleanAttribute(name, null);
  }

  public Boolean getBooleanAttribute(String name, Boolean def) {
    String value = attributes.getProperty(name);
    if (value == null) {
      return def;
    } else {
      return Boolean.valueOf(value);
    }
  }

  public Integer getIntAttribute(String name) {
    return getIntAttribute(name, null);
  }

  public Integer getIntAttribute(String name, Integer def) {
    String value = attributes.getProperty(name);
    if (value == null) {
      return def;
    } else {
      return Integer.parseInt(value);
    }
  }

  public Long getLongAttribute(String name) {
    return getLongAttribute(name, null);
  }

  public Long getLongAttribute(String name, Long def) {
    String value = attributes.getProperty(name);
    if (value == null) {
      return def;
    } else {
      return Long.parseLong(value);
    }
  }

  public Double getDoubleAttribute(String name) {
    return getDoubleAttribute(name, null);
  }

  public Double getDoubleAttribute(String name, Double def) {
    String value = attributes.getProperty(name);
    if (value == null) {
      return def;
    } else {
      return Double.parseDouble(value);
    }
  }

  public Float getFloatAttribute(String name) {
    return getFloatAttribute(name, null);
  }

  public Float getFloatAttribute(String name, Float def) {
    String value = attributes.getProperty(name);
    if (value == null) {
      return def;
    } else {
      return Float.parseFloat(value);
    }
  }

  /**
   * 获取当前节点对应的所有下一级元素类型的子节点
   */
  public List<XNode> getChildren() {
    List<XNode> children = new ArrayList<>();
    //获取原生节点对应的下一级所有的子节点
    NodeList nodeList = node.getChildNodes();
    //检测是否存在对应的子节点
    if (nodeList != null) {
      //循环遍历所有的原生子节点
      for (int i = 0, n = nodeList.getLength(); i < n; i++) {
        //获取对应位置的原生子节点
        Node node = nodeList.item(i);
        //检测对应的子节点是否是元素类型
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          //将对应的子节点封装之后插入到子节点集合中
          children.add(new XNode(xpathParser, node, variables));
        }
      }
    }
    //返回对应元素类型的子节点集合
    return children;
  }

  /**
   * 获取当前节点对应元素子节点中配置的name和value属性值对象
   */
  public Properties getChildrenAsProperties() {
    //创建对应的属性对象
    Properties properties = new Properties();
    //循环遍历当前节点对应的元素子节点
    for (XNode child : getChildren()) {
      //获取当前子节点上配置的name属性对应的属性值
      String name = child.getStringAttribute("name");
      //获取当前子节点上配置的value属性对应的属性值
      String value = child.getStringAttribute("value");
      //检测对应的name属性值和value属性值是否存在
      if (name != null && value != null) {
        //将对应的name属性值和value属性值添加到属性对象中
        properties.setProperty(name, value);
      }
    }
    //返回对应的属性对象
    return properties;
  }

  /**
   * 获取本节点上配置的属性信息
   */
  private Properties parseAttributes(Node n) {
    //创建存储属性的属性对象
    Properties attributes = new Properties();
    //获取节点上设置的属性信息对象
    NamedNodeMap attributeNodes = n.getAttributes();
    //检测是否配置对应的属性信息
    if (attributeNodes != null) {
      //循环获取对应的属性信息
      for (int i = 0; i < attributeNodes.getLength(); i++) {
        //获取对应属性转换后的节点对象
        Node attribute = attributeNodes.item(i);
        //解析给定的属性信息----->即获取真实对应的属性值
        String value = PropertyParser.parse(attribute.getNodeValue(), variables);
        //将对应的属性信息添加到属性对象中
        attributes.put(attribute.getNodeName(), value);
      }
    }
    //返回对应的属性对象
    return attributes;
  }

  /**
   * 解析当前给定节点对应的Body数据
   *   通过分析可以知道
   *     如果当前节点不能获取到对应的body数据 那么就会从对应的子节点中进行获取
   *   本结构一般适用于 一个节点里边有一个文本节点的实例   这样可以直接拿到对应的文本节点的内容当做对应的body数据
   */
  private String parseBody(Node node) {
    //首先获取给定节点元素的body数据
    String data = getBodyData(node);
    //检测给定节点是否能够获取对应的body数据
    if (data == null) {
      //获取对应的原生子节点
      NodeList children = node.getChildNodes();
      //循环遍历所有的原生子节点  知道找到一个子节点可以拿到对应的body数据
      for (int i = 0; i < children.getLength(); i++) {
        //获取对应位置上的原生子节点
        Node child = children.item(i);
        //尝试获取对应的body数据
        data = getBodyData(child);
        //检测是否成功获取对应的body数据
        if (data != null) {
          //获取就跳出处理
          break;
        }
      }
    }
    //返回对应的body数据
    return data;
  }

  /**
   * 获取给定节点对应的Body数据
   *   即当前给定的节点时文本节点或者CDATA_SECTION_NODE节点类型才会返回对应的Body内容
   */
  private String getBodyData(Node child) {
    //简单给定的节点是否是文本节点或者CDATA_SECTION_NODE类型的节点
    if (child.getNodeType() == Node.CDATA_SECTION_NODE || child.getNodeType() == Node.TEXT_NODE) {
      //获取对应的数据内容
      String data = ((CharacterData) child).getData();
      //对数据内容进行尝试转换操作处理 获取真实的数据
      data = PropertyParser.parse(data, variables);
      //返回转换后的数据内容
      return data;
    }
    //节点类型不匹配直接返回空对象
    return null;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("<");
    builder.append(name);

    //循环输出节点对应的所有属性信息
    for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
      builder.append(" ");
      builder.append(entry.getKey());
      builder.append("=\"");
      builder.append(entry.getValue());
      builder.append("\"");
    }

    //获取所有的下一级子节点
    List<XNode> children = getChildren();
    if (!children.isEmpty()) {
      builder.append(">\n");

      //循环输出所有的子节点结构
      for (XNode node : children) {
        builder.append(node.toString());
      }

      builder.append("</");
      builder.append(name);
      builder.append(">");
    } else if (body != null) {
      builder.append(">");
      //设置body内容
      builder.append(body);

      builder.append("</");
      builder.append(name);
      builder.append(">");
    } else {
      builder.append("/>");
    }
    builder.append("\n");
    return builder.toString();
  }

}