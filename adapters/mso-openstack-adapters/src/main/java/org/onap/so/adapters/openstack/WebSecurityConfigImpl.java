/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 * Copyright (C) 2017 - 2018 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Modifications Copyright (C) 2018 IBM.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.so.adapters.openstack;

import org.onap.so.security.MSOSpringFirewall;
import org.onap.so.security.WebSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
public class WebSecurityConfigImpl extends WebSecurityConfig {

    @Profile({"basic", "test"})
    @Bean
    public WebSecurityConfigurerAdapter basicAuth() {
        return new WebSecurityConfigurerAdapter() {
            @Override
            protected void configure(HttpSecurity http) throws Exception {
                http.csrf().disable().authorizeRequests().antMatchers("/manage/health", "/manage/info").permitAll()
                        .antMatchers("/**").hasAnyRole(StringUtils.collectionToDelimitedString(getRoles(), ",")).and()
                        .httpBasic();
            }

            @Override
            public void configure(WebSecurity web) throws Exception {
                super.configure(web);
                StrictHttpFirewall firewall = new MSOSpringFirewall();
                web.httpFirewall(firewall);
            }

            @Override
            protected void configure(AuthenticationManagerBuilder auth) throws Exception {
                auth.userDetailsService(WebSecurityConfigImpl.this.userDetailsService())
                        .passwordEncoder(WebSecurityConfigImpl.this.passwordEncoder());
            }

        };
    }

    @Profile("aaf")
    @Bean
    public WebSecurityConfigurerAdapter noAuth() {
        return new WebSecurityConfigurerAdapter() {
            @Override
            protected void configure(HttpSecurity http) throws Exception {
                http.authorizeRequests().anyRequest().permitAll();
            }
        };
    }

}
