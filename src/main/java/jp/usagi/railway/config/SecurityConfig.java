package jp.usagi.railway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * 認証・認可設定.
 *
 * ロール
 *   DISPATCHER : 電力指令員 (警報確認, 停電作業の承認・き電停止・復電)
 *   MAINTAINER : 保守・検修担当 (停電作業申請, 故障登録, メーカー修理依頼)
 *   MAKER      : 機器メーカー (車両保守の閲覧, 修理進捗の更新のみ. 電力系は閲覧不可)
 *   RTU        : 計測伝文 受信用システムユーザ
 *   ADMIN      : システム管理者
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Configuration
    @Order(1)
    public static class ApiSecurityConfig extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/api/**")
                .csrf().disable()
                .authorizeRequests()
                    .antMatchers("/api/batch/**").hasRole("ADMIN")
                    .antMatchers(HttpMethod.POST, "/api/telemetry").hasAnyRole("RTU", "ADMIN")
                    .antMatchers(HttpMethod.POST, "/api/failures").hasAnyRole("MAINTAINER", "ADMIN")
                    .antMatchers(HttpMethod.GET, "/api/formations/**", "/api/equipment/**", "/api/failures/**")
                        .hasAnyRole("DISPATCHER", "MAINTAINER", "MAKER", "ADMIN")
                    .antMatchers(HttpMethod.GET, "/api/**").hasAnyRole("DISPATCHER", "MAINTAINER", "ADMIN")
                    .anyRequest().hasRole("ADMIN")
                .and()
                .httpBasic();
        }
    }

    @Configuration
    @Order(2)
    public static class WebSecurityConfig extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests()
                    .antMatchers("/static/**", "/login", "/health").permitAll()
                    .antMatchers("/h2-console/**").hasRole("ADMIN")
                    .antMatchers(HttpMethod.POST, "/power/alarms/**").hasAnyRole("DISPATCHER", "ADMIN")
                    .antMatchers(HttpMethod.POST, "/power/outages/*/approve", "/power/outages/*/reject",
                                 "/power/outages/*/start", "/power/outages/*/complete").hasAnyRole("DISPATCHER", "ADMIN")
                    .antMatchers("/power/outages/new").hasAnyRole("MAINTAINER", "ADMIN")
                    .antMatchers(HttpMethod.POST, "/power/outages").hasAnyRole("MAINTAINER", "ADMIN")
                    .antMatchers("/power/**").hasAnyRole("DISPATCHER", "MAINTAINER", "ADMIN")
                    .antMatchers(HttpMethod.POST, "/rolling/repairs/**").hasAnyRole("MAKER", "ADMIN")
                    .antMatchers("/rolling/failures/new").hasAnyRole("MAINTAINER", "ADMIN")
                    .antMatchers(HttpMethod.POST, "/rolling/**").hasAnyRole("MAINTAINER", "ADMIN")
                    .anyRequest().authenticated()
                .and()
                .formLogin()
                    .loginPage("/login")
                    .defaultSuccessUrl("/dashboard", true)
                    .failureUrl("/login?error")
                    .permitAll()
                .and()
                .logout()
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                    .permitAll()
                .and()
                .exceptionHandling().accessDeniedPage("/denied")
                .and()
                .csrf().ignoringAntMatchers("/h2-console/**")
                .and()
                .headers().frameOptions().sameOrigin();
        }
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        // TODO: 2014 年度中に事業者 LDAP 連携へ切替予定 (未対応)
        auth.inMemoryAuthentication()
            .withUser("shirei").password("shirei123").roles("DISPATCHER")
            .and()
            .withUser("kenshu").password("kenshu123").roles("MAINTAINER")
            .and()
            .withUser("maker").password("maker123").roles("MAKER")
            .and()
            .withUser("rtu").password("rtu123").roles("RTU")
            .and()
            .withUser("admin").password("admin123").roles("ADMIN", "DISPATCHER", "MAINTAINER");
    }
}
