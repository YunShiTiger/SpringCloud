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

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.ibatis.builder.BuilderException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * 对原始的XPath进行进一步封装处理,用于解析给定的xml的节点信息
 *   通过对代码的分析 可以知道 构建XPathParser对象的主要方式有四种
 *      1 通过传入的xml字符串数据进行构建
 *      2 通过传入的xml文件字节流数据进行构建
 *      3 通过传入的xml文件字符流数据进行构建
 *      4 通过直接传入xml对应的Document对象来构建
 *   其实内部真正完成节点节点操作处理的是XPath对象来完成对应的工作
 */
public class XPathParser {

  //对应的需要解析的xml文档对象
  private final Document document;
  //用于记录是否对对应的xml文件进行效验是否是xml格式标识
  private boolean validation;
  //解析的验证文件XSD来源的解析验证对象
  private EntityResolver entityResolver;
  //用于记录所有配置的属性对象  属性的来源有三个方面  最外边暴露的门面类  配置xml中设置的属性值 配置xml文件中引用的外部属性文件
  private Properties variables;
  //https://blog.csdn.net/qiujiwuhen00/article/details/46120217
  //对应的进行xml文档解析的解析器对象
  private XPath xpath;

  public XPathParser(String xml) {
    commonConstructor(false, null, null);
    this.document = createDocument(new InputSource(new StringReader(xml)));
  }

  public XPathParser(String xml, boolean validation) {
    commonConstructor(validation, null, null);
    this.document = createDocument(new InputSource(new StringReader(xml)));
  }

  public XPathParser(String xml, boolean validation, Properties variables) {
    commonConstructor(validation, variables, null);
    this.document = createDocument(new InputSource(new StringReader(xml)));
  }

  public XPathParser(String xml, boolean validation, Properties variables, EntityResolver entityResolver) {
    commonConstructor(validation, variables, entityResolver);
    this.document = createDocument(new InputSource(new StringReader(xml)));
  }

  public XPathParser(Reader reader) {
    commonConstructor(false, null, null);
    this.document = createDocument(new InputSource(reader));
  }

  public XPathParser(Reader reader, boolean validation) {
    commonConstructor(validation, null, null);
    this.document = createDocument(new InputSource(reader));
  }

  public XPathParser(Reader reader, boolean validation, Properties variables) {
    commonConstructor(validation, variables, null);
    this.document = createDocument(new InputSource(reader));
  }

  public XPathParser(Reader reader, boolean validation, Properties variables, EntityResolver entityResolver) {
    commonConstructor(validation, variables, entityResolver);
    this.document = createDocument(new InputSource(reader));
  }

  public XPathParser(InputStream inputStream) {
    commonConstructor(false, null, null);
    this.document = createDocument(new InputSource(inputStream));
  }

  public XPathParser(InputStream inputStream, boolean validation) {
    commonConstructor(validation, null, null);
    this.document = createDocument(new InputSource(inputStream));
  }

  public XPathParser(InputStream inputStream, boolean validation, Properties variables) {
    commonConstructor(validation, variables, null);
    this.document = createDocument(new InputSource(inputStream));
  }

  public XPathParser(InputStream inputStream, boolean validation, Properties variables, EntityResolver entityResolver) {
    commonConstructor(validation, variables, entityResolver);
    this.document = createDocument(new InputSource(inputStream));
  }

  public XPathParser(Document document) {
    commonConstructor(false, null, null);
    this.document = document;
  }

  public XPathParser(Document document, boolean validation) {
    commonConstructor(validation, null, null);
    this.document = document;
  }

  public XPathParser(Document document, boolean validation, Properties variables) {
    commonConstructor(validation, variables, null);
    this.document = document;
  }

  public XPathParser(Document document, boolean validation, Properties variables, EntityResolver entityResolver) {
    commonConstructor(validation, variables, entityResolver);
    this.document = document;
  }

  /**
   * 私有方法 进行对创建的对象进行统一的属性对象初始化操作处理
   */
  private void commonConstructor(boolean validation, Properties variables, EntityResolver entityResolver) {
    this.validation = validation;
    this.entityResolver = entityResolver;
    this.variables = variables;
    //创建对应的解析器工厂对象
    XPathFactory factory = XPathFactory.newInstance();
    //初始化真正的XPath解析器对象
    this.xpath = factory.newXPath();
  }

  /**
   * 根据传入的数据流对象创建对应的Document对象
   */
  private Document createDocument(InputSource inputSource) {
    // important: this must only be called AFTER common constructor
    try {
      //首先创建对应的Document构建器工厂对象
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      //配置构建器工厂对象的相关参数
      factory.setValidating(validation);
      factory.setNamespaceAware(false);
      factory.setIgnoringComments(true);
      factory.setIgnoringElementContentWhitespace(false);
      factory.setCoalescing(false);
      factory.setExpandEntityReferences(true);

      //获取对应的Document构建器对象
      DocumentBuilder builder = factory.newDocumentBuilder();
      //设置对应的dtd验证器对象
      builder.setEntityResolver(entityResolver);
      //设置解析出现错误的处理器对象   主要是进行抛出异常错误
      builder.setErrorHandler(new ErrorHandler() {
        @Override
        public void error(SAXParseException exception) throws SAXException {
          throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
          throw exception;
        }

        @Override
        public void warning(SAXParseException exception) throws SAXException {
        }
      });
      //通过Document构建器对象创建构建对应的Document对象
      return builder.parse(inputSource);
    } catch (Exception e) {
      throw new BuilderException("Error creating document instance.  Cause: " + e, e);
    }
  }

  /**
   * 对外暴露的设置属性对象的方法
   */
  public void setVariables(Properties variables) {
    this.variables = variables;
  }

  /**
   * 根据给定的表达式获取对应的节点中String数据
   */
  public String evalString(String expression) {
    return evalString(document, expression);
  }

  public String evalString(Object root, String expression) {
    //获取对应表达式节点中的字符串数据
    String result = (String) evaluate(expression, root, XPathConstants.STRING);
    //对获取到的字符串数据进行解析操作处理--------------->即有些字符串数据可以是配置类型的数据${***}  因此需要获取对应的真实对应的数据(即对应的数据在属性集合中应该有对应的配置)
    result = PropertyParser.parse(result, variables);
    //返回解析后的字符串对象
    return result;
  }

  /**
   * 根据给定的表达式获取对应的节点中Boolean数据
   */
  public Boolean evalBoolean(String expression) {
    return evalBoolean(document, expression);
  }

  public Boolean evalBoolean(Object root, String expression) {
    return (Boolean) evaluate(expression, root, XPathConstants.BOOLEAN);
  }

  /**
   * 根据给定的表达式获取对应的节点中Short数据
   */
  public Short evalShort(String expression) {
    return evalShort(document, expression);
  }

  public Short evalShort(Object root, String expression) {
    return Short.valueOf(evalString(root, expression));
  }

  /**
   * 根据给定的表达式获取对应的节点中Integer数据
   */
  public Integer evalInteger(String expression) {
    return evalInteger(document, expression);
  }

  public Integer evalInteger(Object root, String expression) {
    return Integer.valueOf(evalString(root, expression));
  }

  /**
   * 根据给定的表达式获取对应的节点中Long数据
   */
  public Long evalLong(String expression) {
    return evalLong(document, expression);
  }

  public Long evalLong(Object root, String expression) {
    return Long.valueOf(evalString(root, expression));
  }

  /**
   * 根据给定的表达式获取对应的节点中Float数据
   */
  public Float evalFloat(String expression) {
    return evalFloat(document, expression);
  }

  public Float evalFloat(Object root, String expression) {
    return Float.valueOf(evalString(root, expression));
  }

  /**
   * 根据给定的表达式获取对应的节点中Double数据
   */
  public Double evalDouble(String expression) {
    return evalDouble(document, expression);
  }

  public Double evalDouble(Object root, String expression) {
    return (Double) evaluate(expression, root, XPathConstants.NUMBER);
  }

  /**
   * 根据给定的表达式获取对应的节点集合
   */
  public List<XNode> evalNodes(String expression) {
    return evalNodes(document, expression);
  }

  public List<XNode> evalNodes(Object root, String expression) {
    //创建对应的存储封装后的节点集合对象  从这个地方可以看出 返回的节点集合不会是一个null对象
    List<XNode> xnodes = new ArrayList<>();
    //解析并获取给定表达式对应的节点集合
    NodeList nodes = (NodeList) evaluate(expression, root, XPathConstants.NODESET);
    //循环操作处理,将原始的节点转换成对应的封装节点   注意这个地方没有进行判断获取的原始节点集合为空的操作
    for (int i = 0; i < nodes.getLength(); i++) {
      xnodes.add(new XNode(this, nodes.item(i), variables));
    }
    //返回封装后的节点集合
    return xnodes;
  }

  /**
   * 根据给定的表达式获取对应的单节点
   */
  public XNode evalNode(String expression) {
    return evalNode(document, expression);
  }

  public XNode evalNode(Object root, String expression) {
    //解析并获取给定表达式对应的节点
    Node node = (Node) evaluate(expression, root, XPathConstants.NODE);
    //检测是否获取对应的原始节点
    if (node == null) {
      return null;
    }
    //对原生节点进行封装获取封装后对应的节点对象
    return new XNode(this, node, variables);
  }

  /**
   * 根据提供的表达式和查询类型返回对应的查找到的内容
   * XPath 1.0只有四种基本的数据类型：
   *    number（数值型）               XPathConstants.NUMBER
   *    boolean（布尔型）              XPathConstants.BOOLEAN
   *    string（字符串型）             XPathConstants.STRING
   *    node-set（节点型）             XPathConstants.NODESET XPathConstants.NODE
   *
   *    XPathConstants.NODE并没有匹配的XPath类型，它主要适用于当XPath表达式的结果有且只有一个节点。
   *    如果XPath表达式返回了多个节点，却指定类型为XPathConstants.NODE，则evaluate()方法将按照文档顺序返回第一个节点。
   *    如果XPath表达式的结果为一个空集，却指定类型为XPathConstants.NODE，则evaluate( )方法将返回null
   */
  private Object evaluate(String expression, Object root, QName returnType) {
    try {
      //通过xpath来获取指定表达式对应的内容
      return xpath.evaluate(expression, root, returnType);
    } catch (Exception e) {
      throw new BuilderException("Error evaluating XPath.  Cause: " + e, e);
    }
  }

}
