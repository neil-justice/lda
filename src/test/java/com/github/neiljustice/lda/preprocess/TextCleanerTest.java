package com.github.neiljustice.lda.preprocess;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TextCleanerTest {
  private TextCleaner tc;

  @Before
  public void init() {
    tc = new TextCleaner();
  }

  @Test
  public void checkURLRemoval() {
    assertEquals("hello world", tc.cleanText("hello http://www.google.com world"));
    assertEquals("hello world", tc.cleanText("hello https://docs.oracle.com/javase/tutorial/java/package/createpkgs.html world"));
    assertEquals("hello world", tc.cleanText("hello world https://www.google.co.uk/search?q=google&oq=goo&aqs=chrome.0.0j69i60l3j0j69i65.6019j0j4&sourceid=chrome&ie=UTF-8"));
    assertEquals("hello world", tc.cleanText("hello world https://t.co/AK3DBCbNy6"));
  }

  @Test
  public void checkPunctuationRemoval() {
    assertEquals("a b", tc.cleanText("a,b"));
    assertEquals("a b", tc.cleanText("a:b"));
    assertEquals("a b", tc.cleanText("a;b"));
    assertEquals("a b", tc.cleanText("a.b"));
    assertEquals("a b", tc.cleanText("a!b"));
    assertEquals("a b", tc.cleanText("a?b"));
    assertEquals("a b", tc.cleanText("a/b"));
    assertEquals("a-b", tc.cleanText("a-b"));
    assertEquals("a'b", tc.cleanText("a'b"));
    assertEquals("ab", tc.cleanText("a�$%b"));
    assertEquals("ab", tc.cleanText("a%^&*()b"));
    assertEquals("ab", tc.cleanText("a+={}[]b"));
    assertEquals("ab", tc.cleanText("a~><\\|b"));
    assertEquals("ab", tc.cleanText("a�`�b"));
  }

  @Test
  public void checkNumberRemoval() {
    assertEquals("", tc.cleanText(" 0 "));
    assertEquals("", tc.cleanText("0"));
    assertEquals("", tc.cleanText(" 000 "));
    assertEquals("hell0", tc.cleanText("0 1 000 hell0 0"));
    assertEquals("0hell", tc.cleanText("0 1 000 0hell 0"));
  }

  @Test
  public void checkWhitespaceCollapsing() {
    assertEquals("hello world", tc.cleanText(" hello world "));
    assertEquals("hello world", tc.cleanText("  hello world           "));
    assertEquals("hello world", tc.cleanText(" hello \r  world "));
    assertEquals("hello world", tc.cleanText(" hello \t\t world "));
    assertEquals("hello world", tc.cleanText(" hello \n world "));
    assertEquals("hello world", tc.cleanText(" \r\nhello world\n\r"));
  }
}

