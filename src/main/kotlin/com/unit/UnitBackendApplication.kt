package com.unit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UnitBackendApplication

fun main(args: Array<String>) {
    runApplication<UnitBackendApplication>(*args)
}
