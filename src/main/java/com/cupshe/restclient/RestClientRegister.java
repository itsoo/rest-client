package com.cupshe.restclient;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * RestClientRegister
 *
 * @author zxy
 */
public class RestClientRegister implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private Environment environment;
    private ResourceLoader resourceLoader;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @SneakyThrows
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner = getScanner();
        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestClient.class));

        for (String basePackage : getBasePackages(metadata)) {
            Set<BeanDefinition> components = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition component : components) {
                if (component instanceof AnnotatedBeanDefinition) {
                    Map<String, Object> attrs = ((AnnotatedBeanDefinition) component)
                            .getMetadata()
                            .getAnnotationAttributes(RestClient.class.getCanonicalName());
                    Assert.notNull(attrs, "Cannot found interface with @RestClient.");

                    String clazz = component.getBeanClassName();
                    BeanDefinitionBuilder bb = BeanDefinitionBuilder.genericBeanDefinition(RestClientFactoryBean.class);
                    bb.addConstructorArgValue(Class.forName(clazz));
                    bb.addConstructorArgValue(attrs.get("value"));
                    bb.addConstructorArgValue(attrs.get("path"));
                    bb.addConstructorArgValue(attrs.get("loadBalanceType"));
                    bb.addConstructorArgValue(attrs.get("maxAutoRetries"));
                    bb.addConstructorArgValue(attrs.get("fallback"));
                    bb.addConstructorArgValue(attrs.get("connectTimeout"));
                    BeanDefinitionReaderUtils.registerBeanDefinition(
                            new BeanDefinitionHolder(bb.getBeanDefinition(), '$' + clazz, ofArray(clazz)), registry);
                }
            }
        }
    }

    private ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false, environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition def) {
                return def.getMetadata().isIndependent() && !def.getMetadata().isAnnotation();
            }
        };
    }

    private Set<String> getBasePackages(AnnotationMetadata metadata) {
        Map<String, Object> attrs = metadata.getAnnotationAttributes(EnableRestClient.class.getCanonicalName());
        if (attrs == null) {
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<>();
        String[] basePackages = (String[]) attrs.get("basePackages");
        for (String pkg : basePackages) {
            if (StringUtils.hasText(pkg)) {
                result.add(pkg);
            }
        }

        if (result.isEmpty()) {
            result.add(ClassUtils.getPackageName(metadata.getClassName()));
        }

        return result;
    }

    private String[] ofArray(String... args) {
        return args;
    }
}
