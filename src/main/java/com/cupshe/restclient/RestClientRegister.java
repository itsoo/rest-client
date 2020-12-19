package com.cupshe.restclient;

import com.cupshe.ak.objects.ObjectClasses;
import com.cupshe.ak.text.StringUtils;
import com.cupshe.restclient.lang.EnableRestClient;
import com.cupshe.restclient.lang.PureFunction;
import com.cupshe.restclient.lang.RestClient;
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
import org.springframework.lang.NonNull;
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
    public void setResourceLoader(@NonNull ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata metadata, @NonNull BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner = getComponentProviderScanner();
        for (String basePackage : getBasePackages(metadata)) {
            for (BeanDefinition component : scanner.findCandidateComponents(basePackage)) {
                if (component instanceof AnnotatedBeanDefinition) {
                    String className = component.getBeanClassName();
                    String classBeanName = ObjectClasses.getShortNameAsProperty(className);
                    RestClient ann = AssertBeforeRegister.assertAndGetAnnotation(className);
                    BeanDefinition beanDefinition = getBeanDefinition(className, ann);
                    String beanName = StringUtils.defaultIfBlank(ann.id(), classBeanName);
                    AssertBeforeRegister.assertSingletonRegister(beanName, className);
                    BeanDefinitionReaderUtils.registerBeanDefinition(
                            new BeanDefinitionHolder(beanDefinition, beanName, ofArray(className)), registry);
                }
            }
        }

        AssertBeforeRegister.clearCheckedRegisterCache();
    }

    @PureFunction
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

    @PureFunction
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
    private BeanDefinition getBeanDefinition(String clazz, RestClient ann) {
        return BeanDefinitionBuilder.genericBeanDefinition(RestClientFactoryBean.class)
                .addConstructorArgValue(Class.forName(clazz))
                .addConstructorArgValue(ann.name())
                .addConstructorArgValue(ann.path())
                .addConstructorArgValue(ann.loadBalanceType())
                .addConstructorArgValue(ann.maxAutoRetries())
                .addConstructorArgValue(ann.fallback())
                .addConstructorArgValue(ann.connectTimeout())
                .addConstructorArgValue(ann.readTimeout())
                .getBeanDefinition();
    }

    private String[] ofArray(String... args) {
        return args;
    }
}
