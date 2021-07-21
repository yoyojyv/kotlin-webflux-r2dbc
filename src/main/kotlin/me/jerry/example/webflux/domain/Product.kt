package me.jerry.example.webflux.domain

import me.jerry.example.webflux.type.ProductCategoryType
import me.jerry.example.webflux.type.YesNoType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("product")
data class Product(
        @Id var id: Long? = null,
        var productCategoryCode: ProductCategoryType = ProductCategoryType.PROPERTY,
        var supplierId: Long,
        var supplierProductId: String,
        var mainCategoryId: Long,
        var name: String,
        var useYn: YesNoType = YesNoType.N,
        // TODO spring data r2dbc audit 부분은 v1.2 이후 추후지원이 될 예정으로 보임
        // @LastModifiedDate
        var updatedAt: LocalDateTime? = null,
        // @CreatedDate
        var createdAt: LocalDateTime? = null
)
