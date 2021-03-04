package com.cupshe.restclient.parser;

import java.util.Objects;

/**
 * ExpressionParser
 *
 * @author zxy
 */
public interface ExpressionParser<T> {

    String getExpressionPrefix();

    String getExpressionSuffix();

    T getDataSource();

    String getValue(T dataSource, String key);

    default String process(String expression) {
        T dataSource = getDataSource();
        if (Objects.isNull(expression) || Objects.isNull(dataSource)) {
            return expression;
        }

        StringBuilder result = new StringBuilder();
        String prefix = getExpressionPrefix();
        String suffix = getExpressionSuffix();
        int i = 0, j = i, offset = prefix.length();
        while ((i = expression.indexOf(prefix, i)) != -1) {
            result.append(expression, j, i); // no expression template delimiter
            j = expression.indexOf(suffix, i);
            String key = expression.substring(i, j);
            String value = getValue(dataSource, key.substring(offset));
            result.append(value);
            i = ++j;
        }

        return result.append(expression.substring(j)).toString();
    }
}
