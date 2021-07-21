package me.jerry.example.webflux.service.impl

import me.jerry.example.webflux.domain.Product
import me.jerry.example.webflux.repository.ProductRepository
import me.jerry.example.webflux.service.ProductService
import me.jerry.example.webflux.type.ProductCategoryType
import me.jerry.example.webflux.type.YesNoType
import org.slf4j.Logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.reactive.TransactionSynchronizationManager
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.stream.LongStream


@Transactional(readOnly = true, transactionManager = "readTransactionManager")
@Service
class ProductServiceImpl(private val logger: Logger,
        private val productRepository: ProductRepository) : ProductService {

    override fun getProduct(id: Long): Mono<Product> {
        return productRepository.findById(id)
    }

    override fun getProducts(ids: List<Long>): Flux<Product> {
        return Flux.fromIterable(ids)
                .flatMap { id: Long -> productRepository.findById(id) }
        // return productRepository.findAllById()
    }

    @Transactional("writeTransactionManager")
    override fun saveProduct(product: Product): Mono<Product> {
        return productRepository.findBySupplierIdAndSupplierProductId(product.supplierId, product.supplierProductId)
                .switchIfEmpty(productRepository.save(product))
    }

    @Transactional("writeTransactionManager")
    override fun saveExamplesProducts(): Flux<Product> {

//        return Flux.empty();
        return Flux.fromStream(LongStream.rangeClosed(1, 10).boxed())
                .map { it: Long ->
                    Product(null, ProductCategoryType.PROPERTY, 1L,
                            it.toString(), it % 6 + 1, it.toString(), YesNoType.Y,
                            LocalDateTime.now(), LocalDateTime.now())
                }
                .concatMap { it: Product ->
                    productRepository.findBySupplierIdAndSupplierProductId(it.supplierId, it.supplierProductId)
                            .switchIfEmpty(productRepository.save(it))
                }
    }


}
