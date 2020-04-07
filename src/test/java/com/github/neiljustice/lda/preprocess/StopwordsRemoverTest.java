package com.github.neiljustice.lda.preprocess;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
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

    final List<List<String>> tokenised = Arrays.asList(
        new ArrayList<>(Arrays.asList("document", "did", "does")),
        new ArrayList<>(Arrays.asList("put", "que", "what"))
    );

    ns.removeFrom(tokenised);
    assertEquals(tokenised.get(0).size(), 1);
    assertEquals(tokenised.get(0).get(0), "document");
    assertEquals(tokenised.get(1).size(), 0);
  }

}

