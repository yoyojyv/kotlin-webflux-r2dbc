package me.jerry.example.webflux.config

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.pool.PoolingConnectionFactoryProvider
import io.r2dbc.spi.*
import me.jerry.example.webflux.support.NoArgsConstructor
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.connectionfactory.R2dbcTransactionManager
import org.springframework.data.r2dbc.connectionfactory.lookup.AbstractRoutingConnectionFactory
import org.springframework.data.r2dbc.core.DatabaseClient
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.data.r2dbc.support.R2dbcExceptionTranslator
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.reactive.TransactionSynchronizationManager
import reactor.core.publisher.Mono
import java.time.Duration

@Configuration
@EnableTransactionManagement
class R2dbcReadWriteConfig {

    @Configuration
    @EnableR2dbcRepositories(basePackages = ["me.jerry.example.webflux.repository"], databaseClientRef = "readDatabaseClient")
    internal class ReadDbR2dbcConfig : AbstractR2dbcConfiguration() {

        @Bean(name = ["readDbPoolSettings"])
        @ConfigurationProperties("datasource.read")
        fun readDbPoolSettings(): R2dbcPoolSettings {
            return R2dbcPoolSettings()
        }

        //        @Bean
        //        public ConnectionPool readConnectionPool() {
        //            return getNewConnectionPool(readDbPoolSettings());
        //        }
        @Bean("readConnectionFactory")
        override fun connectionFactory(): ConnectionFactory {
            return createNewConnectionPool(readDbPoolSettings())
        }

        @Bean(name = ["readTransactionManager"])
        fun readTransactionManager(): ReactiveTransactionManager {
            val readOnly = R2dbcTransactionManager(connectionFactory())
            readOnly.isEnforceReadOnly = true
            return readOnly
        }

        @Bean("readDatabaseClient")
        override fun databaseClient(dataAccessStrategy: ReactiveDataAccessStrategy, exceptionTranslator: R2dbcExceptionTranslator): DatabaseClient {
            return super.databaseClient(dataAccessStrategy, exceptionTranslator)
        }
    }

    @Configuration
    @EnableR2dbcRepositories(basePackages = ["me.jerry.example.webflux.repository"], databaseClientRef = "writeDatabaseClient")
    internal class WriteDbR2dbcConfig : AbstractR2dbcConfiguration() {
        @Bean(name = ["writeDbPoolSettings"])
        @ConfigurationProperties(prefix = "datasource.write")
        fun writeDbPoolSettings(): R2dbcPoolSettings {
            return R2dbcPoolSettings()
        }

        //        @Bean
        //        public ConnectionPool writeConnectionPool() {
        //            return getNewConnectionPool(writeDbPoolSettings());
        //        }
        @Bean("writeConnectionFactory")
        override fun connectionFactory(): ConnectionFactory {
            return createNewConnectionPool(writeDbPoolSettings())
        }

        @Bean(name = ["writeTransactionManager"])
        fun writeTransactionManager(): ReactiveTransactionManager {
            return R2dbcTransactionManager(connectionFactory())
        }

        @Bean("writeDatabaseClient")
        override fun databaseClient(dataAccessStrategy: ReactiveDataAccessStrategy, exceptionTranslator: R2dbcExceptionTranslator): DatabaseClient {
            return super.databaseClient(dataAccessStrategy, exceptionTranslator)
        }
    }

    @NoArgsConstructor
    data class R2dbcPoolSettings(
            var driver: String = "pool",
            var protocol: String = "mysql",
            var host: String = "localhost",
            var port: Int = 3306,
            var username: String = "root",
            var password: String = "password",
            var database: String = "test",
            var connectionTimeout: Duration = Duration.ofSeconds(10),
            var poolName: String = "pool",
            var initialSize: Int = 20,
            var maxSize: Int = 20,
            var maxIdleTime: Duration = Duration.ofSeconds(15),
            var maxLifeTime: Duration = Duration.ofSeconds(20),
            var maxCreateConnectionTime: Duration = Duration.ofSeconds(2),
            var maxAcquireTime: Duration = Duration.ofSeconds(3),
            var acquireRetry: Int = 1
    )

    // ============================= private helper methods  =============================

    companion object {
        private fun createNewConnectionPool(settings: R2dbcPoolSettings): ConnectionPool {
            val connectionFactory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
                    .option(ConnectionFactoryOptions.DRIVER, settings.driver)
                    .option(ConnectionFactoryOptions.PROTOCOL, settings.protocol)
                    .option(ConnectionFactoryOptions.HOST, settings.host)
                    .option(ConnectionFactoryOptions.PORT, settings.port)
                    .option(ConnectionFactoryOptions.USER, settings.username)
                    .option(ConnectionFactoryOptions.PASSWORD, settings.password)
                    .option(ConnectionFactoryOptions.DATABASE, settings.database)
                    .option(ConnectionFactoryOptions.CONNECT_TIMEOUT, settings.connectionTimeout)
                    .option(ConnectionFactoryOptions.SSL, false)
                    .option(Option.valueOf("zeroDate"), "use_null")
                    .option(PoolingConnectionFactoryProvider.MAX_SIZE, settings.maxSize)
//                .option(PoolingConnectionFactoryProvider.VALIDATION_QUERY, "select 1")
                    .option(PoolingConnectionFactoryProvider.VALIDATION_DEPTH, ValidationDepth.LOCAL)
                    .build()
            )
            val configuration = createNewConnectionPoolBuilder(connectionFactory, settings).build()
            return ConnectionPool(configuration)
        }

        private fun createNewConnectionPoolBuilder(connectionFactory: ConnectionFactory, settings: R2dbcPoolSettings): ConnectionPoolConfiguration.Builder {
            return ConnectionPoolConfiguration.builder(connectionFactory)
                    .name(settings.poolName)
                    .initialSize(settings.initialSize)
                    .maxSize(settings.maxSize)
                    .maxIdleTime(settings.maxIdleTime)
                    .maxLifeTime(settings.maxLifeTime)
                    .maxAcquireTime(settings.maxAcquireTime)
                    .acquireRetry(settings.acquireRetry)
                    .maxCreateConnectionTime(settings.maxCreateConnectionTime)
//                .validationQuery("select 1")
                    .validationDepth(ValidationDepth.LOCAL)
                    .registerJmx(true)
        }
    }

}
