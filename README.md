# spring kotlin r2dbc

r2dbc routing 테스트 목적으로 만듬 

database read/write routing 이 transaction 적용시 정상적으로 이루어지는지 확인하기 위함 

각 API 들이 정상 동작하는 것으로 확인이 된다면, connection pool max 수치보다 많은 요청을 한꺼번에 보내볼 것 

## url (handler) 
1. http://localhost:8080/products/{id}
- read test

1. http://localhost:8080/products/saveExample
- write test

## 소스에서 봐야할 것들
 
R2dbcRoutingConfig 설정, R2dbcReadWriteConfig 설정을 이렇게 바꿨다~ 저렇게 바꿨다~ 여러번 해서 주석이 많이 달려 있음. 

### AbstractRoutingConnectionFactory 를 상속받아 구현한 RoutingConnectionFactory 확인
밑에 정리된 2가지를 모두 수행해봄  
- RoutingConnectionFactory 를 사용하면서... routingTransactionManager 를 밑에 예처럼 등록하는 경우 R2dbcTransactionManager 가 정상동작하는지 확인 (정상 동작하지 않음)
- service 쪽에서 @Transactional 설정시 transactionManager 를 "routingTransactionManager" 로 설정
```
       @Bean("routingTransactionManager")
       fun routingTransactionManager(@Qualifier("connectionFactory") connectionFactory: ConnectionFactory): ReactiveTransactionManager {
           return R2dbcTransactionManager(connectionFactory)
       }
```     
- RoutingConnectionFactory 를 사용하면서... 위의 예처럼 routingTransactionManager 를 사용하지 않고, 밑의 예처럼 read, write transactionManager 를 각각 등록하는 경우 정상동작하는지 확인 
(정상 동작하지만, 요청이 많은 경우 커넥션 풀에서 새로운 연결을 잘 못맺는 문제가 있음. 동시에 커넥션 풀의 개수보다 많은 요청을 받는 경우)
- service 쪽에서 @Transactional 설정시 transactionManager 를 "readTransactionManager", "writeTransactionManager" 중 알맞게 설정
```
       @Bean("readTransactionManager")
       fun readTransactionManager(): ReactiveTransactionManager {
           val readOnly = R2dbcTransactionManager(readConnectionFactory())
           readOnly.isEnforceReadOnly = true
           return readOnly
       }
    
       @Bean("writeTransactionManager")
       fun writeTransactionManager(): ReactiveTransactionManager {
           return R2dbcTransactionManager(writeConnectionFactory())
       }
```

### AbstractRoutingConnectionFactory 를 사용하지 않고 read, write 를 따로따로 설정하는 경우 동작 확인
- RoutingConnectionFactory 를 사용하지 않고 read, write transactionManager 를 따로따로 등록하여 사용하는 경우
- service 쪽에서 @Transactional 설정시 transactionManager 를 "readTransactionManager", "writeTransactionManager" 중 알맞게 설정
- 이 경우 요청이 많이도 정상동작함
 

## 결론 (2020.09.07 기준)
- 현재 spring r2dbc 구현된 내용으로 기존 blocking 방식 사용시 LazyConnectionDataSourceProxy 사용했던 것처럼 구현을 할 수가 없음
- TransactionAwareConnectionFactoryProxy 를 살펴봤으나, 현재 r2dbc 는 지원하지 않음
- read/write 각각 나누어 쓰거나 한쪽만 붙도록 구현을 해야할 것으로 보임

