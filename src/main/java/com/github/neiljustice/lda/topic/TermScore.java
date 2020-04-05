package com.github.neiljustice.lda.topic;

public class TermScore {
  private final String term;

  private final double score;

  public TermScore(String term, double score) {
    this.term = term;
    this.score = score;
  }

  public String getTerm() {
    return term;
  }

  public double getScore() {
    return score;
  }

  @Override
  public String toString() {
    return term + " : " + score;
  }
}
