package com.github.neiljustice.lda.preprocess;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.neiljustice.lda.util.FileUtils.loadResourceToCollection;

/**
 * This class removes all the stopwords from the given list of documents.
 * Stopwords are passed in as a list, or the defaults may be used.
 */
public class StopwordsRemover {
  private static final Logger LOGGER = LogManager.getLogger(StopwordsRemover.class);

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
    for (int i = 0; i < tokenisedDocuments.size(); i++) {
      final List<String> document = tokenisedDocuments.get(i);
      document.removeAll(stopwords);
      if (document.isEmpty()) {
        LOGGER.warn("Document " + i + " had no tokens after stopword removal");
      }
    }
  }
}