/*
 * Copyright 2020 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.ebi.eva.submission.controller.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    public static final String REALM = "EBI-REALM";

    private static final String ROLE_ADMIN = "ADMIN";

    private final CustomBasicAuthenticationEntryPoint customBasicAuthenticationEntryPoint;

    private final BruteForceProtectionFilter bruteForceProtectionFilter;

    @Value("${controller.auth.admin.username}")
    private String USERNAME_ADMIN;

    @Value("${controller.auth.admin.password}")
    private String PASSWORD_ADMIN;

    @Autowired
    public SecurityConfiguration(CustomBasicAuthenticationEntryPoint customBasicAuthenticationEntryPoint,
                                  BruteForceProtectionFilter bruteForceProtectionFilter) {
        this.customBasicAuthenticationEntryPoint = customBasicAuthenticationEntryPoint;
        this.bruteForceProtectionFilter = bruteForceProtectionFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    public void configureGlobalSecurity(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser(USERNAME_ADMIN)
                .password(passwordEncoder().encode(PASSWORD_ADMIN))
                .roles(ROLE_ADMIN);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
                .antMatchers("/v1/submission/**").permitAll()
                .antMatchers("/v1/call-home/**").permitAll()
                .antMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs").permitAll()
                .antMatchers("/v1/admin/**").hasRole(ROLE_ADMIN)
                .anyRequest().authenticated()
                .and().httpBasic().realmName(REALM)
                .authenticationEntryPoint(customBasicAuthenticationEntryPoint)
                .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().addFilterBefore(bruteForceProtectionFilter, BasicAuthenticationFilter.class);
    }
}
