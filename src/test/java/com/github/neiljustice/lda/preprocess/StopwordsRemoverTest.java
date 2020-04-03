package com.github.neiljustice.lda.preprocess;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class StopwordsRemoverTest {
  private StopwordsRemover ns;

  @Before
  public void init() {
    ns = new StopwordsRemover();
  }

  @Test
  public void checkTokenising() {

    final List<List<String>> tokenised = new ArrayList<>();
    final List<String> t1 = new ArrayList<>();
    final List<String> t2 = new ArrayList<>();
    t1.add("document");
    t1.add("did");
    t1.add("does");
    t2.add("put");
    t2.add("que");
    t2.add("what");

    tokenised.add(t1);
    tokenised.add(t2);

    final List<List<String>> noStopwords = new ArrayList<>();
    final List<String> n1 = new ArrayList<>();
    final List<String> n2 = new ArrayList<>();
    n1.add("document");

    noStopwords.add(n1);
    noStopwords.add(n2);

    ns.removeFrom(tokenised);

    assertEquals(noStopwords, tokenised);
  }

}

