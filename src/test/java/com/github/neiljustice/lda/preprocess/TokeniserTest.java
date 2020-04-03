package com.github.neiljustice.lda.preprocess;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TokeniserTest {
  private Tokeniser tk;

  @Before
  public void init() {
    tk = new Tokeniser();
  }

  @Test
  public void checkTokenising() {
    final List<String> documents = new ArrayList<>();
    documents.add("document 1 test");
    documents.add("7 don't 8-9 ");

    final List<List<String>> tokenised = new ArrayList<>();
    final List<String> t1 = new ArrayList<>();
    final List<String> t2 = new ArrayList<>();
    t1.add("document");
    t1.add("1");
    t1.add("test");
    t2.add("7");
    t2.add("don't");
    t2.add("8-9");

    tokenised.add(t1);
    tokenised.add(t2);

    assertEquals(tokenised, tk.tokenise(documents));
  }

}

