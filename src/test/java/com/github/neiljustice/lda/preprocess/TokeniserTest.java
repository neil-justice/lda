package com.github.neiljustice.lda.preprocess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

public class TokeniserTest {
  private Tokeniser tk;
  
  @Before
  public void init() {
    tk = new Tokeniser();
  }

  @Test
  public void checkTokenising() {
    List<String> documents = new ArrayList<String>();
    documents.add("document 1 test");
    documents.add("7 don't 8-9 ");
    
    List<List<String>> tokenised = new ArrayList<List<String>>();
    List<String> t1 = new ArrayList<String>();
    List<String> t2 = new ArrayList<String>();
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

