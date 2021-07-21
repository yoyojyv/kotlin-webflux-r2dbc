package me.jerry.example.webflux.config

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.pool.PoolingConnectionFactoryProvider
import io.r2dbc.spi.*
import me.jerry.example.webflux.support.NoArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.autoconfigure.health.CompositeReactiveHealthContributorConfiguration
import org.springframework.boot.actuate.health.ReactiveHealthContributor
import org.springframework.boot.actuate.r2dbc.ConnectionFactoryHealthIndicator
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.connectionfactory.R2dbcTransactionManager
import org.springframework.data.r2dbc.connectionfactory.TransactionAwareConnectionFactoryProxy
import org.springframework.data.r2dbc.connectionfactory.lookup.AbstractRoutingConnectionFactory
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.reactive.TransactionSynchronizationManager
import reactor.core.publisher.Mono
import java.time.Duration

//@Configuration
class R2dbcRoutingConfig {

//    @Configuration
    @EnableR2dbcRepositories(basePackages = ["me.jerry.example.webflux.repository"])
    @EnableTransactionManagement
    class RoutingR2dbcConfig : AbstractR2dbcConfiguration() {

        @Bean("readDbPoolSettings")
        @ConfigurationProperties(prefix = "datasource.read")
        fun readDbPoolSettings(): R2dbcPoolSettings {
            return R2dbcPoolSettings()
        }

        @Bean("readConnectionFactory")
        fun readConnectionFactory(): ConnectionFactory {
            return createNewConnectionPool(readDbPoolSettings())
        }

        @Bean("readTransactionManager")
        fun readTransactionManager(): ReactiveTransactionManager {
            val readOnly = R2dbcTransactionManager(readConnectionFactory())
            readOnly.isEnforceReadOnly = true
            return readOnly
        }

        @Bean("writeDbPoolSettings")
        @ConfigurationProperties(prefix = "datasource.write")
        fun writeDbPoolSettings(): R2dbcPoolSettings {
            return R2dbcPoolSettings()
        }

        @Bean("writeConnectionFactory")
        fun writeConnectionFactory(): ConnectionFactory {
            return createNewConnectionPool(writeDbPoolSettings())
        }

        @Bean("writeTransactionManager")
        fun writeTransactionManager(): ReactiveTransactionManager {
            return R2dbcTransactionManager(writeConnectionFactory())
        }

//        @Bean("routingTransactionManager")
//        fun routingTransactionManager(@Qualifier("connectionFactory") connectionFactory: ConnectionFactory): ReactiveTransactionManager {
//            return R2dbcTransactionManager(connectionFactory)
//        }

//        @Bean
//        fun transactionalOperator(//@Qualifier("routingTransactionManager")
//                                  routingTransactionManager: ReactiveTransactionManager) =
//                TransactionalOperator.create(routingTransactionManager)

//        @Primary
//        @Bean("routingConnectionFactory")
//        override fun connectionFactory(): ConnectionFactory {
//            val connectionFactory = RoutingConnectionFactory()
//            with(connectionFactory) {
//                val factories = mapOf(
//                        RoutingConnectionFactory.TRANSACTION_READ to readConnectionFactory(),
//                        RoutingConnectionFactory.TRANSACTION_WRITE to writeConnectionFactory()
//                )
//                setDefaultTargetConnectionFactory(writeConnectionFactory())
//                setTargetConnectionFactories(factories)
//            }
//            return connectionFactory
//        }

//        @Bean("routingConnectionFactory")
//        override fun connectionFactory(): ConnectionFactory {
//            return writeConnectionFactory()
//        }

//        @Bean("routingConnectionFactory")
//        fun routingConnectionFactory(): ConnectionFactory {
//            val connectionFactory = RoutingConnectionFactory()
//            with(connectionFactory) {
//                val factories = mapOf(
//                        RoutingConnectionFactory.TRANSACTION_READ to readConnectionFactory(),
//                        RoutingConnectionFactory.TRANSACTION_WRITE to writeConnectionFactory()
//                )
//                setDefaultTargetConnectionFactory(writeConnectionFactory())
//                setTargetConnectionFactories(factories)
//            }
//            return connectionFactory
//        }

        @Primary
        @Bean("connectionFactory")
        override fun connectionFactory(): ConnectionFactory {
            val connectionFactory = RoutingConnectionFactory()
            val factories = mapOf(
                    RoutingConnectionFactory.TRANSACTION_READ to readConnectionFactory(),
                    RoutingConnectionFactory.TRANSACTION_WRITE to writeConnectionFactory()
            )

//            with(connectionFactory) {
//                val factories = mapOf(
//                        RoutingConnectionFactory.TRANSACTION_READ to readConnectionFactory(),
//                        RoutingConnectionFactory.TRANSACTION_WRITE to writeConnectionFactory()
//                )
//                setDefaultTargetConnectionFactory(writeConnectionFactory())
//                setTargetConnectionFactories(factories)
//            }
//
            connectionFactory.setDefaultTargetConnectionFactory(writeConnectionFactory())
            connectionFactory.setLenientFallback(true)
            connectionFactory.setTargetConnectionFactories(factories)


//            return connectionFactory
            return connectionFactory
        }

//        @Primary
//        @Bean("defaultTransactionManager")
//        fun defaultTransactionManager(): ReactiveTransactionManager {
//            return R2dbcTransactionManager(connectionFactory())
//        }

    }

//    /**
//     * /actuator/health 에서 RoutingConnectionFactory status 를 "DOWN" 으로 표시하는 부분이 있어 (validationQuery 실패) - 따라서 RoutingConnectionFactory 만 빼고 전달
//     */
//    @Configuration
//    class R2dbcHealthContributor(private val connectionFactories: Map<String, ConnectionFactory>) : CompositeReactiveHealthContributorConfiguration<ConnectionFactoryHealthIndicator, ConnectionFactory>() {
//
//        @Bean("r2dbcHealthContributor")
//        fun r2dbcHealthContributor(): ReactiveHealthContributor {
//            return createContributor(connectionFactories.filter { it.key != "routingConnectionFactory" })
//        }
////        public ReactiveHealthContributor r2dbcHealthContributor() {
////            Map<String, ConnectionFactory> connectionFactoryMap = new HashMap<>();
////            connectionFactoryMap.put("readConnectionFactory", readConnectionFactory());
////            connectionFactoryMap.put("writeConnectionFactory", writeConnectionFactory());
////            return createContributor(connectionFactoryMap);
////        }
//
//    }

    // 사용시 성능 이슈가 있음. validation 체크 하는부분에서 pool size 가 넘어가는 요청이 한꺼번에 왔을때 오류가 나는 것으로 보여짐
    // 또 actuator 에서 자동 등록되는 indicator 에서도 validation 문제(transaction 정의가 되지 않았다는 exception 이 남)가 있어
    // health check 시 DOWN 으로 표시됨
    class RoutingConnectionFactory : AbstractRoutingConnectionFactory() {

        private val logger = LoggerFactory.getLogger(RoutingConnectionFactory::class.java)

        override fun determineCurrentLookupKey(): Mono<Any> {

            return TransactionSynchronizationManager.forCurrentTransaction()
                    .flatMap {

                        logger.info("it.getCurrentTransactionName() : {}", it.currentTransactionName)
                        logger.info("it.isActualTransactionActive() : {}", it.isActualTransactionActive)
                        logger.info("it.isCurrentTransactionReadOnly() : {}", it.isCurrentTransactionReadOnly)

                        val dataSourceType = if (it.isActualTransactionActive) {

                            if (it.isCurrentTransactionReadOnly) {
                                TRANSACTION_READ
                            } else {
                                TRANSACTION_WRITE
                            }

                        } else {
                            TRANSACTION_WRITE
                        }
                        logger.info("> current dataSourceType : {}", dataSourceType)
                        Mono.just(dataSourceType)
            }

//            return TransactionSynchronizationManager.forCurrentTransaction()
//                    .flatMap { it: TransactionSynchronizationManager ->
//                        logger.debug("it.getCurrentTransactionName() : {}", it.currentTransactionName)
//                        logger.debug("it.isActualTransactionActive() : {}", it.isActualTransactionActive)
//                        logger.debug("it.isCurrentTransactionReadOnly() : {}", it.isCurrentTransactionReadOnly)
//                        if (it.isActualTransactionActive) {
//
//                            if (it.isCurrentTransactionReadOnly) {
//                                Mono.just(TRANSACTION_READ)
//                            } else {
//                                Mono.just(TRANSACTION_WRITE)
//                            }
//
//                        } else {
//                            Mono.empty()
//
//                        }
//                    }
        }

        companion object {
            const val TRANSACTION_READ = "read"
            const val TRANSACTION_WRITE = "write"
        }
    }


//    @Configuration
//    @EnableR2dbcRepositories
//    class ReadDbR2dbcConfig : AbstractR2dbcConfiguration() {
//
//        @Bean("readDbPoolSettings")
//        @ConfigurationProperties(prefix = "datasource.read")
//        fun readDbPoolSettings(): R2dbcPoolSettings {
//            return R2dbcPoolSettings()
//        }
//
//        @Bean("readConnectionFactory")
//        override fun connectionFactory(): ConnectionFactory {
//            return createNewConnectionPool(readDbPoolSettings())
//        }
//
//        @Bean("readDatabaseClient")
//        override fun databaseClient(dataAccessStrategy: ReactiveDataAccessStrategy, exceptionTranslator: R2dbcExceptionTranslator): DatabaseClient {
//            return super.databaseClient(dataAccessStrategy, exceptionTranslator)
//        }
//
//        @Bean("readTransactionManager")
//        fun readTransactionManager(): ReactiveTransactionManager {
//            val readOnly = R2dbcTransactionManager(connectionFactory())
//            readOnly.isEnforceReadOnly = true
//            return readOnly
//        }
//
//    }

//    @Configuration
//    @EnableR2dbcRepositories
//    class WriteDbR2dbcConfig : AbstractR2dbcConfiguration() {
//
//        @Bean("writeDbPoolSettings")
//        @ConfigurationProperties(prefix = "datasource.write")
//        fun writeDbPoolSettings(): R2dbcPoolSettings {
//            return R2dbcPoolSettings()
//        }
//
//        @Bean("writeConnectionFactory")
//        override fun connectionFactory(): ConnectionFactory {
//            return createNewConnectionPool(writeDbPoolSettings())
//        }
//
//        @Bean("writeDatabaseClient")
//        override fun databaseClient(dataAccessStrategy: ReactiveDataAccessStrategy, exceptionTranslator: R2dbcExceptionTranslator): DatabaseClient {
//            return super.databaseClient(dataAccessStrategy, exceptionTranslator)
//        }
//
//        @Bean("writeTransactionManager")
//        fun writeTransactionManager(): ReactiveTransactionManager {
//            return R2dbcTransactionManager(connectionFactory())
//        }
//
//    }

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
