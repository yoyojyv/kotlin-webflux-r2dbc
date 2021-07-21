package me.jerry.example.webflux.service

import me.jerry.example.webflux.domain.Product
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ProductService {

    fun getProduct(id: Long): Mono<Product>

    fun getProducts(ids: List<Long>): Flux<Product>

    fun saveProduct(product: Product): Mono<Product>

    fun saveExamplesProducts(): Flux<Product>

}
