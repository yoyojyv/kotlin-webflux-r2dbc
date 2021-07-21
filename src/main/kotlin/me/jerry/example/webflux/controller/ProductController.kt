package me.jerry.example.webflux.controller

import me.jerry.example.webflux.domain.Product
import me.jerry.example.webflux.service.ProductService
import me.jerry.example.webflux.type.ProductCategoryType
import me.jerry.example.webflux.type.YesNoType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@RestController
class ProductController(private val productService: ProductService) {

    @GetMapping("/products/{id}")
    fun product(@PathVariable id: Long) = productService.getProduct(id)

    @GetMapping("/products/byIds")
    fun productsByIds(@RequestParam ids: List<Long>) = productService.getProducts(ids)

//    @GetMapping("/products/saveExample")
//    fun saveExampleProducts() = productService.saveExamplesProducts()

    @GetMapping("/products/saveExample")
    fun saveExampleProducts(): Mono<Product> {
        val p = Product(null, ProductCategoryType.PROPERTY, 1L,
                "1", 1, "1", YesNoType.Y,
                LocalDateTime.now(), LocalDateTime.now())
        return productService.saveProduct(p)
    }

    @PostMapping("/products")
    fun saveProduct(@RequestBody product: Product): Mono<Product> {
        return productService.saveProduct(product)
    }

}
