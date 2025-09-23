package com.banking.transactions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class TransactionsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransactionsServiceApplication.class, args);
    }
}
