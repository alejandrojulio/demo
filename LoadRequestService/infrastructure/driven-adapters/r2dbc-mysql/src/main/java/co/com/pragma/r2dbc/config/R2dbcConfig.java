package co.com.pragma.r2dbc.config;

import io.asyncer.r2dbc.mysql.MySqlConnectionConfiguration;
import io.asyncer.r2dbc.mysql.MySqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableR2dbcRepositories(basePackages = "co.com.pragma.r2dbc")
@EnableTransactionManagement
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    private final MySqlConnectionProperties properties;

    public R2dbcConfig(MySqlConnectionProperties properties) {
        this.properties = properties;
    }

    @Override
    @Bean
    @Primary
    public ConnectionFactory connectionFactory() {
        return MySqlConnectionFactory.from(
                MySqlConnectionConfiguration.builder()
                        .host(properties.host())
                        .port(properties.port())
                        .database(properties.database())
                        .user(properties.username())
                        .password(properties.password())
                        .build()
        );
    }

    @Bean
    @Primary
    public ReactiveTransactionManager reactiveTransactionManager() {
        return new R2dbcTransactionManager(connectionFactory());
    }
}