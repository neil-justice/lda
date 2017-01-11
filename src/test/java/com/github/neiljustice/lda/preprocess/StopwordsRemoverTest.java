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

public class StopwordsRemoverTest {
  private StopwordsRemover ns;
  
  @Before
  public void init() {
    ns = new StopwordsRemover();
  }

  @Test
  public void checkTokenising() {
    
    List<List<String>> tokenised = new ArrayList<List<String>>();
    List<String> t1 = new ArrayList<String>();
    List<String> t2 = new ArrayList<String>();
    t1.add("document");
    t1.add("did");
    t1.add("does");
    t2.add("put");
    t2.add("que");
    t2.add("what");
    
    tokenised.add(t1);
    tokenised.add(t2);
    
    List<List<String>> noStopwords = new ArrayList<List<String>>();
    List<String> n1 = new ArrayList<String>();
    List<String> n2 = new ArrayList<String>();
    n1.add("document");
    
    noStopwords.add(n1);
    noStopwords.add(n2);
    
    ns.removeFrom(tokenised);
    
    assertEquals(noStopwords, tokenised);
  }

}

