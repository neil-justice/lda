package com.github.neiljustice.lda.preprocess;

import java.util.*;

/**
 * This class by default removes most punctuation, unifies case,
 * and removes web addresses.  Its behaviour may be configured.
 */
public class TextCleaner {
  private boolean removeURLs = true;
  private boolean removeNumbers = true;
  private boolean makeLowerCase = true;
  private final List<String> keep;
  private final List<String> toSpace;
  
  public TextCleaner() {
    keep = new ArrayList<String>();
    toSpace = new ArrayList<String>();  
    
    keep.add("a-z");
    keep.add("A-Z");
    keep.add("0-9");
    keep.add("-");
    keep.add("'");
    keep.add(" ");
    
    toSpace.add("!");
    toSpace.add(",");
    toSpace.add(".");
    toSpace.add("?");
    toSpace.add("/");
    toSpace.add(":");
    toSpace.add(";");
    toSpace.add("_");
  }
  
  public TextCleaner(String patternToKeep, String patternToConvertToSpace) {
    keep = new ArrayList<String>();
    toSpace = new ArrayList<String>();  
    
    keep.add(patternToKeep);
    toSpace.add(patternToConvertToSpace);
  }
  
  public TextCleaner(List<String> patternsToKeep, 
                     List<String> patternsToConvertToSpace) {
    keep = patternsToKeep;
    toSpace = patternsToConvertToSpace;
  }
  
  public List<String> clean(List<String> documents) {
    List<String> cleaned = new ArrayList<String>();
    for (String document: documents) {
      cleaned.add(cleanText(document));
    }
    return cleaned;
  }
  
  public TextCleaner keep(String pattern) {
    keep.add(pattern);
    return this;
  }
  
  public TextCleaner toSpace(String pattern) { 
    toSpace.add(pattern); 
    return this;
  }
  
  public TextCleaner removeURLs(boolean remove) {
    removeURLs = remove;
    return this;
  }

  public TextCleaner makeLowerCase(boolean make) {
    makeLowerCase = make;
    return this;
  }
  
  public TextCleaner removeNumbers(boolean remove) {
    removeNumbers = remove;
    return this;
  }  
  
  private String cleanText(String text) {
    if (makeLowerCase) text = text.toLowerCase();
    if (removeURLs)    text = removeURLs(text);
    text = convertCharsToSpace(text);
    text = removeCharsExcept(text);
    if (removeNumbers) text = removeNumbers(text);
    return collapseWhitespace(text);
  }
  
  // Remove urls:.  should be done before removing punctuation or they are hard
  // to find.
  private String removeURLs(String text) {
    return text.replaceAll("\\S+://\\S+", "");
  }
  
  // Remove punctuation.  must happen after url removal.  Some
  // are replaced with spaces, and some not, since typically characters like
  // apostrophes and dashes do not mark word endings.
  private String convertCharsToSpace(String text) {
    String pattern = getRegexFromList(toSpace, false);
    return text.replaceAll(pattern, " ");
  }
  
  private String removeCharsExcept(String text) {
    String pattern = getRegexFromList(keep, true);
    return text.replaceAll(pattern, "");
  }
  
  private String getRegexFromList(List<String> list, boolean isNegated) {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    if (isNegated) builder.append("^");
    for (String s: list) {
      builder.append(s);
    }
    builder.append("]");
    
    return builder.toString();
  }
  
  private String removeNumbers(String text) {
    return text.replaceAll("(?<= |^)[0-9]+(?= |$)", "");
  }
  
  private String removeTokensStartingWith(String startChar, String text) {
    return text.replaceAll(startChar + "[a-zA-Z0-9_]+", "");
  }
  
  private String collapseWhitespace(String text) {
    return text.trim().replaceAll(" +", " ");
  }
  
  // public static void main(String[] args) {
  //   Tester t = new Tester();
  //   TextCleaner tc = new TextCleaner();
  //   
  //   t.is("h world", tc.collapseWhitespace(tc.removeURLs("h http://www.google.com world")));
  //   t.is("h world", tc.collapseWhitespace(tc.removeURLs("h https://docs.oracle.com/javase/tutorial/java/package/createpkgs.html world")));
  //   t.is("h world ", tc.removeURLs("h world https://www.google.co.uk/search?q=google&oq=goo&aqs=chrome.0.0j69i60l3j0j69i65.6019j0j4&sourceid=chrome&ie=UTF-8"));
  //   t.is("h world ", tc.removeURLs("h world https://t.co/AK3DBCbNy6"));
  //   t.is("a b", tc.convertCharsToSpace("a,b"));
  //   t.is("a b", tc.convertCharsToSpace("a:b"));
  //   t.is("a b", tc.convertCharsToSpace("a;b"));
  //   t.is("a b", tc.convertCharsToSpace("a.b"));
  //   t.is("a b", tc.convertCharsToSpace("a!b"));
  //   t.is("a b", tc.convertCharsToSpace("a?b"));
  //   t.is("a b", tc.convertCharsToSpace("a/b"));
  //   t.is("a-b", tc.convertCharsToSpace("a-b"));
  //   t.is("a'b", tc.convertCharsToSpace("a'b"));
  //   t.is("ab", tc.removeCharsExcept("a£$%b"));
  //   t.is("ab", tc.removeCharsExcept("a%^&*()b"));
  //   t.is("ab", tc.removeCharsExcept("a+={}[]b"));
  //   t.is("ab", tc.removeCharsExcept("a~'><\\|b"));
  //   t.is("ab", tc.removeCharsExcept("a¬`¦b"));
  //   t.is("",tc.collapseWhitespace(tc.removeNumbers(" 0 ")));
  //   t.is("",tc.collapseWhitespace(tc.removeNumbers("0")));
  //   t.is("",tc.collapseWhitespace(tc.removeNumbers(" 000 ")));
  //   t.is("hell0",tc.collapseWhitespace(tc.removeNumbers("0 1 000 hell0 0")));
  //   t.is("0hell",tc.collapseWhitespace(tc.removeNumbers("0 1 000 0hell 0")));
  //   
  //   t.results();
  // }
}