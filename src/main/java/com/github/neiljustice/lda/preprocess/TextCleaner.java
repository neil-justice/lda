package com.github.neiljustice.lda.preprocess;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * This class by default removes most punctuation, unifies case,
 * and removes web addresses and numbers.  Its behaviour may be configured.
 */
public class TextCleaner {
  private List<Function<String, String>> conversions;

  public TextCleaner() {
    conversions = new ArrayList<>();
    conversions.add(TextCleaner::lowerCase);
    conversions.add(TextCleaner::removeURLs);
    conversions.add(TextCleaner::convertCharsToSpace);
    conversions.add(TextCleaner::removeCharsExcept);
    conversions.add(TextCleaner::removeNumbers);
    conversions.add(TextCleaner::collapseWhitespace);
  }

  public TextCleaner(List<Function<String, String>> conversions) {
    this.conversions = conversions;
  }

  public static String lowerCase(String text) {
    return text.toLowerCase();
  }

  public static String removeCharsExcept(String text) {
    return text.replaceAll("[^a-zA-Z0-9-' ]", "");
  }

  public static String convertCharsToSpace(String text) {
    return text.replaceAll("[!,.?/:;_]", " ");
  }

  // Remove urls:.  should be done before removing punctuation or they are hard
  // to find.
  public static String removeURLs(String text) {
    return text.replaceAll("\\S+://\\S+", "");
  }

  public static String removeNumbers(String text) {
    return text.replaceAll("(?<= |^)[0-9]+(?= |$)", "");
  }

  public static String collapseWhitespace(String text) {
    return text.trim().replaceAll(" +", " ");
  }

  public void setConversions(List<Function<String, String>> conversions) {
    this.conversions = conversions;
  }

  public List<String> clean(List<String> documents) {
    final List<String> cleaned = new ArrayList<>();
    for (String document : documents) {
      cleaned.add(cleanText(document));
    }
    return cleaned;
  }

  public String cleanText(String text) {
    for (Function<String, String> action : conversions) {
      text = action.apply(text);
    }
    return text;
  }
}