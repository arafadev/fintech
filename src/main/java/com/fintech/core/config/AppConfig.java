package com.fintech.core.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.spi.MatchingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.modelmapper.convention.MatchingStrategies;

@Configuration
public class AppConfig {

    @Bean
    public SpringTemplateEngine templateEngine(){
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver classLoaderTemplateResolver = new ClassLoaderTemplateResolver();
        classLoaderTemplateResolver.setPrefix("templates/");
        classLoaderTemplateResolver.setSuffix(".html");
        classLoaderTemplateResolver.setCharacterEncoding("UTF-8");
        templateEngine.setTemplateResolver(classLoaderTemplateResolver);

        return templateEngine;
    }

    @Bean
    public ModelMapper modelMapper(){
            ModelMapper modelMapper = new ModelMapper();
            modelMapper.getConfiguration().setFieldMatchingEnabled(true)
                    .setFieldMatchingEnabled(true)
                    .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                    .setMatchingStrategy(MatchingStrategies.STANDARD);

                    return modelMapper;
    }
}
