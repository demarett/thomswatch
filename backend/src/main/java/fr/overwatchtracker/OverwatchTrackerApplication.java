package fr.overwatchtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class OverwatchTrackerApplication {
  public static void main(String[] args) { SpringApplication.run(OverwatchTrackerApplication.class, args); }
}

