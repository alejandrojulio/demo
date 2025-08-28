package co.com.pragma.r2dbc.config;

import io.r2dbc.pool.ConnectionPool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "co.com.pragma.r2dbc")
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    private final ConnectionPool connectionPool;

    public R2dbcConfig(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    @Bean
    public ConnectionPool connectionFactory() {
        return connectionPool;
    }
}
