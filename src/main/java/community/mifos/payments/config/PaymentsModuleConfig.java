package community.mifos.payments.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = "community.mifos.payments")
@EnableJpaRepositories(basePackages = "community.mifos.payments.core.repository")
public class PaymentsModuleConfig {
}