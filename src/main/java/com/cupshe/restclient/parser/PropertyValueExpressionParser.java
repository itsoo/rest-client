package com.cupshe.restclient.parser;

import com.cupshe.ak.text.StringUtils;
import org.springframework.core.env.Environment;

/**
 * PropertyValueExpressionParser
 *
 * @author zxy
 */
public class PropertyValueExpressionParser implements ExpressionParser<Environment> {

    public static final String EXPRESSION_DELIMITER_PREFIX = "${";

    public static final String EXPRESSION_DELIMITER_SUFFIX = "}";

    private final Environment dataSource;

    public PropertyValueExpressionParser(Environment dataSource) {
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
    public Environment getDataSource() {
        return dataSource;
    }

    @Override
    public String getValue(Environment dataSource, String key) {
        return dataSource.getProperty(key.trim(), StringUtils.EMPTY);
    }
}
