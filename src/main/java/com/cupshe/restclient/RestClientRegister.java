package com.cupshe.restclient;

import com.cupshe.ak.text.StringUtils;
import com.cupshe.restclient.util.ObjectClassUtils;
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
import org.springframework.util.ClassUtils;

import java.util.Arrays;
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

    @Override
    @SneakyThrows
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner = getComponentProviderScanner();
        for (String basePackage : getBasePackages(metadata)) {
            for (BeanDefinition component : scanner.findCandidateComponents(basePackage)) {
                if (component instanceof AnnotatedBeanDefinition) {
                    String clazz = component.getBeanClassName();
                    String classBeanName = ObjectClassUtils.getBeanName(clazz);
                    RestClient annotation = AssertBeforeRegister.assertAndGetAnnotation(clazz);
                    String beanName = StringUtils.defaultIfBlank(annotation.id(), classBeanName);
                    BeanDefinitionBuilder b = BeanDefinitionBuilder.genericBeanDefinition(RestClientFactoryBean.class);
                    b.addConstructorArgValue(Class.forName(clazz));
                    b.addConstructorArgValue(annotation.name());
                    b.addConstructorArgValue(annotation.path());
                    b.addConstructorArgValue(annotation.loadBalanceType());
                    b.addConstructorArgValue(annotation.maxAutoRetries());
                    b.addConstructorArgValue(annotation.fallback());
                    b.addConstructorArgValue(annotation.connectTimeout());
                    b.addConstructorArgValue(annotation.readTimeout());
                    BeanDefinitionReaderUtils.registerBeanDefinition(
                            new BeanDefinitionHolder(b.getBeanDefinition(), beanName, ofArray(clazz)), registry);
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
        EnableRestClient clazz = metadata.getAnnotations().get(EnableRestClient.class).synthesize();
        Set<String> result = Arrays.stream(clazz.basePackages())
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
        if (result.isEmpty()) {
            result.add(ClassUtils.getPackageName(metadata.getClassName()));
        }

        return result;
    }

    private String[] ofArray(String... args) {
        return args;
    }
}
