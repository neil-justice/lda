package com.github.neiljustice.lda;

import com.github.neiljustice.lda.topic.Topic;

import java.util.List;

public class LDAModel {

  private final Corpus corpus;

  private final double[][] phi;
  private final double[][] theta;
  private final double[] alpha;

  private final int topics;
  private final int samples;
  private final int cycles;

  public LDAModel(Corpus corpus, double[][] phi, double[][] theta, double[] alpha, int topics, int samples, int cycles) {
    this.corpus = corpus;
    this.phi = phi;
    this.theta = theta;
    this.alpha = alpha;
    this.topics = topics;
    this.samples = samples;
    this.cycles = cycles;
  }

  public Corpus getCorpus() {
    return corpus;
  }

  public double[][] getPhi() {
    return phi;
  }

  public double[][] getTheta() {
    return theta;
  }

  public double[] getAlpha() {
    return alpha;
  }

  public int getTopics() {
    return topics;
  }

  public int getSamples() {
    return samples;
  }

  public int getCycles() {
    return cycles;
  }

  /**
   * As laid out in Blei and Lafferty, 2009.  sorts words in topics by
   * phi * log (phi) / geometric mean(phi)
   * and defines topics by their top N words.
   *
   * @param topN the top N words to return per topic. If this is <= 0, all words are returned.
   */
  public List<Topic> termScore(int topN) {
    return LDAUtils.termScore(phi, corpus.dictionary(), topN);
  }
}
