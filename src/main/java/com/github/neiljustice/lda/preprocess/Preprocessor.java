package com.github.neiljustice.lda.preprocess;

import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Tokens can be removed if:
 * - They occur in more or less than n% of documents.
 * - They occur in more or less than a specific number of documents.
 * - They occur more or less than a set number of times overall.
 * - They are longer or shorter than a given length.
 */
public class Preprocessor {

  private final static Logger LOG = LogManager.getLogger(Preprocessor.class);
  /**
   * Number of times the word occurs:
   */
  private final TObjectIntHashMap<String> wordFreqs = new TObjectIntHashMap<>();
  /**
   * Number of documents containing that word:
   */
  private final TObjectIntHashMap<String> perDocFreqs = new TObjectIntHashMap<>();

  private boolean useMaxFreq = false;
  private int maxFreq = 1000000;

  private boolean useMinFreq = true;
  private int minFreq = 10;

  private boolean useMaxPerc = true;
  private double maxPerc = 0.9d;

  private boolean useMinPerc = false;
  private double minPerc = 0.01d;

  private boolean useMaxDocs = false;
  private int maxDocs = 100000;

  private boolean useMinDocs = true;
  private int minDocs = 5;

  private boolean useMaxLength = false;
  private int maxLength = 50;

  private boolean useMinLength = true;
  private int minLength = 3;

  private int docCount = 0;

  /**
   * Exclude tokens which appear with more than maxFreq frequency across the whole corpus.
   */
  public Preprocessor useMaxFreq(boolean use) {
    this.useMaxFreq = use;
    return this;
  }

  /**
   * Exclude tokens which appear with less than minFreq frequency across the whole corpus.
   */
  public Preprocessor useMinFreq(boolean use) {
    this.useMinFreq = use;
    return this;
  }

  /**
   * Exclude tokens which appear in more percent of documents than maxPerc.
   */
  public Preprocessor useMaxPerc(boolean use) {
    this.useMaxPerc = use;
    return this;
  }

  /**
   * Exclude tokens which appear in less percent of documents than minPerc.
   */
  public Preprocessor useMinPerc(boolean use) {
    this.useMinPerc = use;
    return this;
  }

  /**
   * Exclude tokens which appear in more documents than maxDocs.
   */
  public Preprocessor useMaxDocs(boolean use) {
    this.useMaxDocs = use;
    return this;
  }

  /**
   * Exclude tokens which appear in less documents than minDocs.
   */
  public Preprocessor useMinDocs(boolean use) {
    this.useMinDocs = use;
    return this;
  }

  /**
   * Exclude tokens longer than maxLength.
   */
  public Preprocessor useMaxLength(boolean use) {
    this.useMaxLength = use;
    return this;
  }

  /**
   * Exclude tokens longer than minLength.
   */
  public Preprocessor useMinLength(boolean use) {
    this.useMinLength = use;
    return this;
  }

  /**
   * Exclude tokens which appear with more than maxFreq frequency across the whole corpus.
   */
  public Preprocessor withMaxFreq(int freq) {
    maxFreq = freq;
    useMaxFreq(true);
    return this;
  }

  /**
   * Exclude tokens which appear with less than minFreq frequency across the whole corpus.
   */
  public Preprocessor withMinFreq(int freq) {
    minFreq = freq;
    useMinFreq(true);
    return this;
  }

  /**
   * Exclude tokens which appear in more percent of documents than maxPerc.
   */
  public Preprocessor withMaxPerc(double perc) {
    maxPerc = perc;
    useMaxPerc(true);
    return this;
  }

  /**
   * Exclude tokens which appear in less percent of documents than minPerc.
   */
  public Preprocessor withMinPerc(double perc) {
    minPerc = perc;
    useMinPerc(true);
    return this;
  }

  /**
   * Exclude tokens which appear in more documents than maxDocs.
   */
  public Preprocessor withMaxDocFreq(int max) {
    maxDocs = max;
    useMaxDocs(true);
    return this;
  }

  /**
   * Exclude tokens which appear in less documents than minDocs.
   */
  public Preprocessor withMinDocFreq(int min) {
    minDocs = min;
    useMinDocs(true);
    return this;
  }

  /**
   * Exclude tokens longer than maxLength.
   *
   * @param length the maximum length
   */
  public Preprocessor withMaxLength(int length) {
    maxLength = length;
    useMaxLength(true);
    return this;
  }

  /**
   * Exclude tokens shorter than minLength.
   *
   * @param length the minimum length
   */
  public Preprocessor withMinLength(int length) {
    minLength = length;
    useMinLength(true);
    return this;
  }

  public int getMaxFreq() {
    return maxFreq;
  }

  public int getMinFreq() {
    return minFreq;
  }

  public double getMaxPerc() {
    return maxPerc;
  }

  public double getMinPerc() {
    return minPerc;
  }

  public int getMaxDocs() {
    return maxDocs;
  }

  public int getMinDocs() {
    return minDocs;
  }

  public int getMaxLength() {
    return maxLength;
  }

  public int getMinLength() {
    return minLength;
  }

  public void process(List<List<String>> tokenisedDocuments) {
    docCount = tokenisedDocuments.size();
    buildFreqLists(tokenisedDocuments);
    removeAll(tokenisedDocuments);
  }

  private void buildFreqLists(List<List<String>> tokenisedDocuments) {
    for (List<String> document : tokenisedDocuments) {
      final Set<String> ocurredInDoc = new HashSet<>();
      for (String token : document) {
        wordFreqs.adjustOrPutValue(token, 1, 1);
        if (!ocurredInDoc.contains(token)) {
          perDocFreqs.adjustOrPutValue(token, 1, 1);
          ocurredInDoc.add(token);
        }
      }
    }
  }

  private void removeAll(List<List<String>> tokenisedDocuments) {
    int kept = 0;
    int total = 0;

    for (List<String> document : tokenisedDocuments) {
      final Iterator<String> it = document.iterator();
      while (it.hasNext()) {
        final String token = it.next();
        final int freq = wordFreqs.get(token);
        final int docFreq = perDocFreqs.get(token);
        final double docPerc = (double) docFreq / docCount;
        final int length = token.length();
        total++;
        if (useMaxFreq && freq > maxFreq) {
          it.remove();
        } else if (useMinFreq && freq < minFreq) {
          it.remove();
        } else if (useMaxPerc && docPerc > maxPerc) {
          it.remove();
        } else if (useMinPerc && docPerc < minPerc) {
          it.remove();
        } else if (useMaxDocs && docFreq > maxDocs) {
          it.remove();
        } else if (useMinDocs && docFreq < minDocs) {
          it.remove();
        } else if (useMaxLength && length > maxLength) {
          it.remove();
        } else if (useMinLength && length < minLength) {
          it.remove();
        } else {
          kept++;
          LOG.debug("Kept token: {}", token);
        }
      }
    }

    LOG.info("Kept {} / {} tokens", kept, total);
    if (kept == 0) {
      throw new IllegalStateException("All tokens were removed by the preprocessor - review the token removal criteria");
    }
  }

}