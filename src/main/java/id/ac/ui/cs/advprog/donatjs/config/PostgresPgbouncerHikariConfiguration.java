package id.ac.ui.cs.advprog.donatjs.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

/**
 * Supabase (and similar) route Postgres through pgBouncer in transaction mode.
 * Server-prepared statements then break at commit with
 * {@code prepared statement "S_n" already exists}. We force the JDBC driver to
 * skip named prepares and use the simple protocol so reads (e.g. subscriptions)
 * and writes (wallet) work reliably. Properties in YAML alone are not always
 * applied; this touches every {@link HikariDataSource} bean before the pool
 * starts.
 */
@Configuration
@ConditionalOnClass(HikariDataSource.class)
public class PostgresPgbouncerHikariConfiguration {

    @Bean
    public static BeanPostProcessor postgresPgbouncerHikariBeanPostProcessor() {
        return new BeanPostProcessor() {

            @Override
            public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
                if (!(bean instanceof HikariDataSource hds)) {
                    return bean;
                }
                String url = hds.getJdbcUrl();
                if (url != null && url.toLowerCase().contains("postgresql")) {
                    hds.addDataSourceProperty("prepareThreshold", "0");
                    hds.addDataSourceProperty("preferQueryMode", "simple");
                }
                return bean;
            }
        };
    }
}
