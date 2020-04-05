package com.github.neiljustice.lda.preprocess;

import java.util.*;

import static com.github.neiljustice.lda.util.FileUtils.loadResourceToCollection;

/**
 * This class removes all the stopwords from the given list of documents.
 * Stopwords are passed in as a list, or the defaults may be used.
 */
public class StopwordsRemover {
  private Set<String> stopwords;

  public StopwordsRemover() {
    stopwords = new HashSet<>();
    loadResourceToCollection("stopwords.txt", stopwords);
  }

  public StopwordsRemover(Collection<String> stopwords) {
    this.stopwords = new HashSet<>(stopwords);
  }

  public Set<String> getStopwords() {
    return stopwords;
  }

  public void setStopwords(Set<String> stopwords) {
    this.stopwords = stopwords;
  }

  public void removeFrom(List<List<String>> tokenisedDocuments) {
    for (List<String> document : tokenisedDocuments) {
      document.removeAll(stopwords);
    }
  }
}