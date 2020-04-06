package com.github.neiljustice.lda.preprocess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tokenises a list of documents using the given delimiter.  Default is space,
 * because the textcleaner will have collapsed multiple consecutive spaces and
 * converted all other types of whitespace to a single space.
 */
public class Tokeniser {
  private String delimiter;

  public Tokeniser() {
    delimiter = " ";
  }

  public Tokeniser(String delimiter) {
    this.delimiter = delimiter;
  }

  public List<List<String>> tokenise(List<String> documents) {
    final List<List<String>> results = new ArrayList<>();
    for (String document : documents) {
      results.add(new ArrayList<>(Arrays.asList(document.trim().split(delimiter))));
    }

    return results;
  }

  public String delimiter() {
    return delimiter;
  }

  public void delimiter(String delimiter) {
    this.delimiter = delimiter;
  }
}