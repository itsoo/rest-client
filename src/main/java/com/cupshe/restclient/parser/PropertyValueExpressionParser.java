package com.cupshe.restclient.parser;

import com.cupshe.ak.text.StringUtils;
import org.springframework.core.env.Environment;

/**
 * PropertyValueExpressionParser
 *
 * @author zxy
 */
public class PropertyValueExpressionParser implements ExpressionParser<Environment> {

    public static final String EXPRESSION_PREFIX = "${";

    public static final String EXPRESSION_SUFFIX = "}";

    private final Environment dataSource;

    public PropertyValueExpressionParser(Environment dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String getExpressionPrefix() {
        return EXPRESSION_PREFIX;
    }

    @Override
    public String getExpressionSuffix() {
        return EXPRESSION_SUFFIX;
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
