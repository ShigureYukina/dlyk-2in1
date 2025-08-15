package org.example.dlykserver.config;

import org.example.dlykserver.config.handler.MyAuthenticationFailureHandler;
import org.example.dlykserver.config.handler.MyAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;


@EnableWebSecurity
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(c -> c.configurationSource(configurationSource())) // 确保 CORS 配置优先
                .csrf().disable() // 如果不需要 CSRF 保护，可以禁用
                .formLogin(formLogin -> {
                    formLogin.loginProcessingUrl("/api/login")
                            .usernameParameter("loginAct")
                            .passwordParameter("loginPwd")
                            .successHandler(new MyAuthenticationSuccessHandler())
                            .failureHandler(new MyAuthenticationFailureHandler());
                })
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/api/login").permitAll()
                            .anyRequest().authenticated();
                })
                .build();
    }

    @Bean
    public CorsConfigurationSource configurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:8081/")); // 生产环境中应限制来源
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); // 指定允许的方法
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true); // 允许发送 Cookie
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
