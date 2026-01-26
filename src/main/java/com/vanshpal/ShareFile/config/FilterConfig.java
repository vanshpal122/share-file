package com.vanshpal.ShareFile.config;

import com.vanshpal.ShareFile.filters.TokenFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//Added HTTP filter

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<TokenFilter> tokenFilter(TokenFilter filter) {
        FilterRegistrationBean<TokenFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(filter);
        reg.addUrlPatterns("/upload", "/download");
        return reg;
    }
}
