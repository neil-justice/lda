package com.github.neiljustice.lda.preprocess;

import java.util.*;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.log4j.Logger;

/**
 * Tokens can be removed if:
 * - They ocurr in more or less than n% of documents.
 * - They ocurr in more or less than a specific number of documents.
 * - They ocurr more or less than a set number of times overall.
 * - They are longer or shorter than a given length.
 */
public class Preprocessor {
  
  private final static Logger LOG = Logger.getLogger(Preprocessor.class);  
  // number of times the word ocurrs:
  private final TObjectIntHashMap<String> wordFreqs = new TObjectIntHashMap<>();
  // docs containing that word:
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
  
  public Preprocessor useMaxFreq(boolean use) {
    this.useMaxFreq = use;
    return this;
  }

  public Preprocessor useMinFreq(boolean use) {
    this.useMinFreq = use;
    return this;
  }
  
  public Preprocessor useMaxPerc(boolean use) {
    this.useMaxPerc = use;
    return this;
  }  
  
  public Preprocessor useMinPerc(boolean use) {
    this.useMinPerc = use;
    return this;
  }
  
  public Preprocessor useMaxDocs(boolean use) {
    this.useMaxDocs = use;
    return this;
  }  
  
  public Preprocessor useMinDocs(boolean use) {
    this.useMinDocs = use;
    return this;
  }
  
  public Preprocessor useMaxLength(boolean use) {
    this.useMaxLength = use;
    return this;
  }  
  
  public Preprocessor useMinLength(boolean use) {
    this.useMinLength = use;
    return this;
  }  
  
  public Preprocessor maxFreq(int freq) {
    maxFreq = freq;
    useMaxFreq(true);
    return this;
  }
  
  public Preprocessor minFreq(int freq) {
    minFreq = freq;
    useMinFreq(true);
    return this;
  }
  
  public Preprocessor maxPerc(double perc) {
    maxPerc = perc;
    useMaxPerc(true);
    return this;
  }
  
  public Preprocessor minPerc(double perc) {
    minPerc = perc;
    useMinPerc(true);
    return this;
  }
  
  public Preprocessor maxDocFreq(int max) {
    maxDocs = max;
    useMaxDocs(true);
    return this;
  }
  
  public Preprocessor minDocFreq(int min) {
    minDocs = min;
    useMinDocs(true);
    return this;
  }
    
  public Preprocessor maxLength(int length) {
    maxLength = length;
    useMaxLength(true);
    return this;
  }
  
  public Preprocessor minLength(int length) {
    minLength = length;
    useMinLength(true);
    return this;
  }
      
  public void process(List<List<String>> tokenisedDocuments) {
    docCount = tokenisedDocuments.size();
    buildFreqLists(tokenisedDocuments);
    removeAll(tokenisedDocuments);
  }
  
  private void buildFreqLists(List<List<String>> tokenisedDocuments) {
    for (List<String> document: tokenisedDocuments) {
      Set<String> ocurredInDoc = new HashSet<String>();
      for (String token: document) {
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
    
    for (List<String> document: tokenisedDocuments) {
      Iterator<String> it = document.iterator();
      while (it.hasNext()) {
        String token = it.next();
        int freq = wordFreqs.get(token);
        int docFreq = wordFreqs.get(token);
        double docPerc = docFreq / docCount;
        int length = token.length();
        total++;
        if (useMaxFreq && freq > maxFreq) it.remove();
        else if (useMinFreq && freq < minFreq) it.remove();
        else if (useMaxPerc && docPerc > maxPerc) it.remove();
        else if (useMinPerc && docPerc < minPerc) it.remove();
        else if (useMaxDocs && docFreq > maxDocs) it.remove();
        else if (useMinDocs && docFreq < minDocs) it.remove();
        else if (useMaxLength && length > maxLength) it.remove();
        else if (useMinLength && length < minLength) it.remove();
        else {
          kept++;
          LOG.debug("Kept token: " + token);
        }
      }
    }
    
    LOG.info("Kept " + kept + " / " + total + "words.");
  }
  
}