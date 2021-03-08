package com.cupshe.restclient.parser;

import com.cupshe.ak.text.StringUtils;

import java.util.Map;

/**
 * PathVariableExpressionParser
 *
 * @author zxy
 */
public class PathVariableExpressionParser implements ExpressionParser<Map<String, String>> {

    public static final String EXPRESSION_DELIMITER_PREFIX = "{";

    public static final String EXPRESSION_DELIMITER_SUFFIX = "}";

    private final Map<String, String> dataSource;

    public PathVariableExpressionParser(Map<String, String> dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String getExpressionDelimiterPrefix() {
        return EXPRESSION_DELIMITER_PREFIX;
    }

    @Override
    public String getExpressionDelimiterSuffix() {
        return EXPRESSION_DELIMITER_SUFFIX;
    }

    @Override
    public Map<String, String> getDataSource() {
        return dataSource;
    }

    @Override
    public String getValue(Map<String, String> dataSource, String key) {
        return dataSource.getOrDefault(key.trim(), StringUtils.EMPTY);
    }
}
