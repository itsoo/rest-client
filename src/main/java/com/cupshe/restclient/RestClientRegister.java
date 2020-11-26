package com.cupshe.restclient;

import com.cupshe.ak.objects.ObjectClasses;
import com.cupshe.ak.text.StringUtils;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner = getComponentProviderScanner();
        for (String basePackage : getBasePackages(metadata)) {
            for (BeanDefinition component : scanner.findCandidateComponents(basePackage)) {
                if (component instanceof AnnotatedBeanDefinition) {
                    String clazz = component.getBeanClassName();
                    String classBeanName = ObjectClasses.getShortNameAsProperty(clazz);
                    RestClient annotation = AssertBeforeRegister.assertAndGetAnnotation(clazz);
                    AbstractBeanDefinition beanDefinition = getBeanDefinition(clazz, annotation);
                    String beanName = StringUtils.defaultIfBlank(annotation.id(), classBeanName);
                    BeanDefinitionReaderUtils.registerBeanDefinition(
                            new BeanDefinitionHolder(beanDefinition, beanName, ofArray(clazz)), registry);
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
                .parallel()
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
        if (result.isEmpty()) {
            result.add(ClassUtils.getPackageName(metadata.getClassName()));
        }

        return result;
    }

    @SneakyThrows
    private AbstractBeanDefinition getBeanDefinition(String clazz, RestClient annotation) {
        BeanDefinitionBuilder b = BeanDefinitionBuilder.genericBeanDefinition(RestClientFactoryBean.class);
        b.addConstructorArgValue(Class.forName(clazz));
        b.addConstructorArgValue(annotation.name());
        b.addConstructorArgValue(annotation.path());
        b.addConstructorArgValue(annotation.loadBalanceType());
        b.addConstructorArgValue(annotation.maxAutoRetries());
        b.addConstructorArgValue(annotation.fallback());
        b.addConstructorArgValue(annotation.connectTimeout());
        b.addConstructorArgValue(annotation.readTimeout());
        return b.getBeanDefinition();
    }

    @NonNull
    private String[] ofArray(String... args) {
        return args;
    }
}
