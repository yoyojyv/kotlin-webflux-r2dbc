package me.jerry.example.webflux

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KotlinWebfluxR2dbcApplication

fun main(args: Array<String>) {
	runApplication<KotlinWebfluxR2dbcApplication>(*args)
}
