package com.github.neiljustice.lda.preprocess;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PreprocessorTest {

  Preprocessor preprocessor;

  @Before
  public void setUp() throws Exception {
    preprocessor = new Preprocessor()
        .useMaxPerc(false)
        .useMinPerc(false)
        .useMaxDocs(false)
        .useMinDocs(false)
        .useMaxFreq(false)
        .useMinFreq(false)
        .useMaxLength(false)
        .useMinLength(false);
  }

  @Test
  public void shouldUseMaxFreq() {
    preprocessor.withMaxFreq(3);
    final List<List<String>> docs = Arrays.asList(
        new ArrayList<>(Arrays.asList("a", "a", "b")),
        new ArrayList<>(Arrays.asList("a", "a", "b"))
    );
    preprocessor.process(docs);
    assertEquals(docs.get(0).size(), 1);
    assertEquals(docs.get(0).get(0), "b");
    assertEquals(docs.get(1).size(), 1);
    assertEquals(docs.get(1).get(0), "b");
  }

  @Test
  public void shouldUseMinFreq() {
    preprocessor.withMinFreq(3);
    final List<List<String>> docs = Arrays.asList(
        new ArrayList<>(Arrays.asList("a", "a", "b")),
        new ArrayList<>(Arrays.asList("a", "a", "b"))
    );
    preprocessor.process(docs);
    assertEquals(docs.get(0).size(), 2);
    assertEquals(docs.get(0).get(0), "a");
    assertEquals(docs.get(0).get(1), "a");
    assertEquals(docs.get(1).size(), 2);
    assertEquals(docs.get(1).get(0), "a");
    assertEquals(docs.get(1).get(1), "a");
  }

  @Test
  public void shouldUseMaxPerc() {
    preprocessor.withMaxPerc(0.5);
    final List<List<String>> docs = Arrays.asList(
        new ArrayList<>(Arrays.asList("a", "a", "b")),
        new ArrayList<>(Arrays.asList("a", "a", "c"))
    );
    preprocessor.process(docs);
    assertEquals(docs.get(0).size(), 1);
    assertEquals(docs.get(0).get(0), "b");
    assertEquals(docs.get(1).size(), 1);
    assertEquals(docs.get(1).get(0), "c");
  }

  @Test
  public void shouldUseMinPerc() {
    preprocessor.withMinPerc(0.51);
    final List<List<String>> docs = Arrays.asList(
        new ArrayList<>(Arrays.asList("a", "a", "b")),
        new ArrayList<>(Arrays.asList("a", "a", "c"))
    );
    preprocessor.process(docs);
    assertEquals(docs.get(0).size(), 2);
    assertEquals(docs.get(0).get(0), "a");
    assertEquals(docs.get(0).get(1), "a");
    assertEquals(docs.get(1).size(), 2);
    assertEquals(docs.get(1).get(0), "a");
    assertEquals(docs.get(1).get(1), "a");
  }

  @Test
  public void shouldUseMaxDocs() {
    preprocessor.withMaxDocFreq(1);
    final List<List<String>> docs = Arrays.asList(
        new ArrayList<>(Arrays.asList("a", "a", "b")),
        new ArrayList<>(Arrays.asList("a", "a", "c"))
    );
    preprocessor.process(docs);
    assertEquals(docs.get(0).size(), 1);
    assertEquals(docs.get(0).get(0), "b");
    assertEquals(docs.get(1).size(), 1);
    assertEquals(docs.get(1).get(0), "c");
  }

  @Test
  public void shouldUseMinDocs() {
    preprocessor.withMinDocFreq(2);
    final List<List<String>> docs = Arrays.asList(
        new ArrayList<>(Arrays.asList("a", "a", "b")),
        new ArrayList<>(Arrays.asList("a", "a", "c"))
    );
    preprocessor.process(docs);
    assertEquals(docs.get(0).size(), 2);
    assertEquals(docs.get(0).get(0), "a");
    assertEquals(docs.get(0).get(1), "a");
    assertEquals(docs.get(1).size(), 2);
    assertEquals(docs.get(1).get(0), "a");
    assertEquals(docs.get(1).get(1), "a");
  }

  @Test
  public void shouldUseMaxLength() {
    preprocessor.withMaxLength(3);
    final List<List<String>> docs = Arrays.asList(
        new ArrayList<>(Arrays.asList("a", "a", "bbbb")),
        new ArrayList<>(Arrays.asList("a", "a", "cccc"))
    );
    preprocessor.process(docs);
    assertEquals(docs.get(0).size(), 2);
    assertEquals(docs.get(0).get(0), "a");
    assertEquals(docs.get(0).get(1), "a");
    assertEquals(docs.get(1).size(), 2);
    assertEquals(docs.get(1).get(0), "a");
    assertEquals(docs.get(1).get(1), "a");
  }

  @Test
  public void shouldUseMinLength() {
    preprocessor.withMinLength(3);
    final List<List<String>> docs = Arrays.asList(
        new ArrayList<>(Arrays.asList("a", "a", "bbbb")),
        new ArrayList<>(Arrays.asList("a", "a", "cccc"))
    );
    preprocessor.process(docs);
    assertEquals(docs.get(0).size(), 1);
    assertEquals(docs.get(0).get(0), "bbbb");
    assertEquals(docs.get(1).size(), 1);
    assertEquals(docs.get(1).get(0), "cccc");
  }
}