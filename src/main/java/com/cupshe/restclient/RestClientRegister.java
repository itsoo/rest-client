package com.cupshe.restclient;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RestClientRegister
 *
 * @author zxy
 */
public class RestClientRegister implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @SneakyThrows
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner = getComponentProviderScanner();

        for (String basePackage : getBasePackages(metadata)) {
            Set<BeanDefinition> components = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition component : components) {
                if (component instanceof AnnotatedBeanDefinition) {
                    Map<String, Object> attrs = ((AnnotatedBeanDefinition) component).getMetadata()
                            .getAnnotationAttributes(RestClient.class.getCanonicalName());
                    Assert.notNull(attrs, "Cannot found interface with @RestClient.");

                    String clazz = component.getBeanClassName();
                    BeanDefinitionBuilder bb = BeanDefinitionBuilder.genericBeanDefinition(RestClientFactoryBean.class);
                    bb.addConstructorArgValue(Class.forName(clazz));
                    bb.addConstructorArgValue(getOrDefault(attrs.get("name"), attrs.get("value")));
                    bb.addConstructorArgValue(attrs.get("path"));
                    bb.addConstructorArgValue(attrs.get("loadBalanceType"));
                    bb.addConstructorArgValue(attrs.get("maxAutoRetries"));
                    bb.addConstructorArgValue(attrs.get("fallback"));
                    bb.addConstructorArgValue(attrs.get("connectTimeout"));
                    bb.addConstructorArgValue(attrs.get("readTimeout"));
                    BeanDefinitionReaderUtils.registerBeanDefinition(
                            new BeanDefinitionHolder(bb.getBeanDefinition(), '$' + clazz, ofArray(clazz)), registry);
                }
            }
        }
    }

    private ClassPathScanningCandidateComponentProvider getComponentProviderScanner() {
        ClassPathScanningCandidateComponentProvider result = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition def) {
                return def.getMetadata().isIndependent() && !def.getMetadata().isAnnotation();
            }
        };

        result.setResourceLoader(resourceLoader);
        result.addIncludeFilter(new AnnotationTypeFilter(RestClient.class));
        return result;
    }

    private Set<String> getBasePackages(AnnotationMetadata metadata) {
        Map<String, Object> attrs = metadata.getAnnotationAttributes(EnableRestClient.class.getCanonicalName());
        if (attrs == null) {
            return Collections.emptySet();
        }

        Set<String> result = Arrays.stream((String[]) attrs.get("basePackages"))
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (result.isEmpty()) {
            result.add(ClassUtils.getPackageName(metadata.getClassName()));
        }

        return result;
    }

    private Object getOrDefault(Object arg, Object def) {
        Assert.isTrue(!(StringUtils.isEmpty(arg) && StringUtils.isEmpty(def)), "name or value cannot be all empty.");
        return StringUtils.isEmpty(arg) ? def : arg;
    }

    private String[] ofArray(String... args) {
        return args;
    }
}
