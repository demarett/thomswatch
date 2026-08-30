package fr.overwatchtracker.service;

import java.util.regex.Pattern;

public record BattleTag(String value) {
  private static final Pattern FORMAT = Pattern.compile("^[^#\\s-]{2,32}(?:#|-)\\d{3,12}$");

  public static BattleTag parse(String raw) {
    if (raw == null || !FORMAT.matcher(raw.trim()).matches()) {
      throw new IllegalArgumentException("Format attendu : Pseudo#1234");
    }
    return new BattleTag(raw.trim().replaceFirst("-(?=\\d{3,12}$)", "#"));
  }

  public String overfastId() {
    return value.replace('#', '-');
  }

  public String urlValue() {
    return overfastId();
  }
}
