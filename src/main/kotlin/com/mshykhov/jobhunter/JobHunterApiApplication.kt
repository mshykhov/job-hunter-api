package com.mshykhov.jobhunter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JobHunterApiApplication

fun main(args: Array<String>) {
    runApplication<JobHunterApiApplication>(*args)
}
