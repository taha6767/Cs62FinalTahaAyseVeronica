package com.example.datingapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * main entry point for the spring boot application that runs the server
 * @author Taha and LLM
 */
@SpringBootApplication
public class DatingAppApplication {
    /**
     * standard main method that tells spring to start up our web server
     * @param args command line arguments passed in during startup
     */
    public static void main(String[] args) {
        SpringApplication.run(DatingAppApplication.class, args);
    }
}