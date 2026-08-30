package fr.overwatchtracker.integration;

public class OverfastException extends RuntimeException {
  private final String code;
  public OverfastException(String code, String message) { super(message); this.code=code; }
  public String code(){ return code; }
}

