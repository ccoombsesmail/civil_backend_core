package com.migrations;

import org.flywaydb.core.Flyway;


public class Main {
  public static void wait(int ms) {
    try{
        Thread.sleep(ms);
    } catch(InterruptedException ex) {
        Thread.currentThread().interrupt();
    }
}

  public static void main(String[] args) {
    Main.wait(5000);
    Flyway flyway = Flyway.configure().dataSource("jdbc:postgresql://db:5432/civil_main", "postgres", "postgres")
        .load();
    flyway.migrate();
  }
}
