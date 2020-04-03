package com.github.neiljustice.lda.preprocess;

import java.util.ArrayList;
import java.util.List;

/**
 * This class by default removes most punctuation, unifies case,
 * and removes web addresses.  Its behaviour may be configured.
 */
public class TextCleaner {
  private final List<Action> conversions;

  public TextCleaner() {
    conversions = new ArrayList<>();
    conversions.add(this::lowerCase);
    conversions.add(this::removeURLs);
    conversions.add(this::convertCharsToSpace);
    conversions.add(this::removeCharsExcept);
    conversions.add(this::removeNumbers);
    conversions.add(this::collapseWhitespace);
  }

  public TextCleaner(List<Action> conversions) {
    this.conversions = conversions;
  }

  public List<String> clean(List<String> documents) {
    final List<String> cleaned = new ArrayList<>();
    for (String document : documents) {
      cleaned.add(cleanText(document));
    }
    return cleaned;
  }

  protected String cleanText(String text) {
    for (Action action : conversions) {
      text = action.process(text);
    }
    return text;
  }

  private String lowerCase(String text) {
    return text.toLowerCase();
  }

  private String removeCharsExcept(String text) {
    return text.replaceAll("[^a-zA-Z0-9-' ]", "");
  }

  private String convertCharsToSpace(String text) {
    return text.replaceAll("[!,.?/:;_]", " ");
  }

  // Remove urls:.  should be done before removing punctuation or they are hard
  // to find.
  private String removeURLs(String text) {
    return text.replaceAll("\\S+://\\S+", "");
  }

  private String removeNumbers(String text) {
    return text.replaceAll("(?<= |^)[0-9]+(?= |$)", "");
  }


  private String collapseWhitespace(String text) {
    return text.trim().replaceAll(" +", " ");
  }

  public interface Action {
    String process(String text);
  }
}