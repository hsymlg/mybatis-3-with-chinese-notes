/*
 *    Copyright 2009-2021 the original author or authors.
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

/**
 * @author Clinton Begin
 * mybatis通用標記解析器，對xml中屬性中的占位符進行解析
 */
public class GenericTokenParser {

  /**
   * 开始标记符
   * 在TextSqlNode#isDynamic()方法中，为"${"
   */
  private final String openToken;
  /**
   * 结束标记符
   * 在TextSqlNode#isDynamic()方法中，为"}"
   */
  private final String closeToken;
  /**
   * 标记处理接口，具体的处理操作取决于它的实现方法
   */
  private final TokenHandler handler;

  /**
   * 构造函数
   */
  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }

  /**
   * 文本解析方法
   */
  public String parse(String text) {
    //文本空值判断
    if (text == null || text.isEmpty()) {
      return "";
    }
    // search open token
    // 获取开始标记符在文本中的位置
    int start = text.indexOf(openToken);
    //位置索引值为-1，说明不存在该开始标记符
    if (start == -1) {
      return text;
    }
    //将文本转换成字符数组
    char[] src = text.toCharArray();
    //偏移量
    int offset = 0;
    //解析后的字符串
    final StringBuilder builder = new StringBuilder();
    StringBuilder expression = null;
    //#{favouriteSection,jdbcType=VARCHAR}
    //这里是循环解析参数，参考GenericTokenParserTest,比如可以解析${first_name} ${initial} ${last_name} reporting.
    //这样的字符串,里面有3个 ${}
    do {
      //判断一下 ${ 前面是否是反斜杠，如果存在转义字符则移除转义字符
      if (start > 0 && src[start - 1] == '\\') {
        // this open token is escaped. remove the backslash and continue.
        //新版已经没有调用substring了，改为调用如下的offset方式，提高了效率，移除转义字符
        builder.append(src, offset, start - offset - 1).append(openToken);
        //重新计算偏移量
        offset = start + openToken.length();
      } else {
        // found open token. let's search close token.
        //开始查找结束标记符
        if (expression == null) {
          expression = new StringBuilder();
        } else {
          expression.setLength(0);
        }
        builder.append(src, offset, start - offset);
        offset = start + openToken.length();
        //结束标记符的索引值
        int end = text.indexOf(closeToken, offset);
        while (end > -1) {
          //同样判断标识符前是否有转义字符，有就移除
          if (end > offset && src[end - 1] == '\\') {
            // this close token is escaped. remove the backslash and continue.
            expression.append(src, offset, end - offset - 1).append(closeToken);
            //重新计算偏移量
            offset = end + closeToken.length();
            //重新计算结束标识符的索引值
            end = text.indexOf(closeToken, offset);
          } else {
            expression.append(src, offset, end - offset);
            break;
          }
        }
        //没有找到结束标记符
        if (end == -1) {
          // close token was not found.
          builder.append(src, start, src.length - start);
          offset = src.length;
        } else {
          //找到了一组标记符，对该标记符进行值替换
          //注意handler.handleToken()方法，这个方法是核心
          builder.append(handler.handleToken(expression.toString()));
          offset = end + closeToken.length();
        }
      }
      //接着查找下一组标记符
      start = text.indexOf(openToken, offset);
    } while (start > -1);
    if (offset < src.length) {
      builder.append(src, offset, src.length - offset);
    }
    return builder.toString();
  }
}
