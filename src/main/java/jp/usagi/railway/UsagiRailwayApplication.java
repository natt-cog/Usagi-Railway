package jp.usagi.railway;

import java.util.TimeZone;

import org.joda.time.DateTimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;

/**
 * うさぎ鉄道 電力・車両保守システム (URMS) エントリポイント.
 *
 * 指令所サーバ (WebSphere) への WAR デプロイと java -jar 起動の両方に対応する.
 */
@SpringBootApplication
@EnableCaching
public class UsagiRailwayApplication extends SpringBootServletInitializer {

    static {
        // 指令所端末・RTU 時刻はすべて JST 運用
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
        DateTimeZone.setDefault(DateTimeZone.forID("Asia/Tokyo"));
    }

    public static void main(String[] args) {
        SpringApplication.run(UsagiRailwayApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(UsagiRailwayApplication.class);
    }
}
