package com.github.neiljustice.lda.topic;

import java.util.Objects;

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
    return String.format("%s : %.03f", term, score);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final TermScore termScore = (TermScore) o;
    return Double.compare(termScore.getScore(), getScore()) == 0 &&
        Objects.equals(getTerm(), termScore.getTerm());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTerm(), getScore());
  }
}
