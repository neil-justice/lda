package com.github.neiljustice.lda.topic;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Topic {
  private final int index;

  private final List<TermScore> terms;

  public Topic(int index, List<TermScore> terms) {
    this.index = index;
    this.terms = terms;
  }

  public int getIndex() {
    return index;
  }

  public List<TermScore> getTerms() {
    return Collections.unmodifiableList(terms);
  }

  public List<String> getWords() {
    return terms.stream().map(TermScore::getTerm).collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return "" + index + " : " + terms;
  }
}
