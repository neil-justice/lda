/* This program performs an initial pass and removes punctuation, unifies case,
 * and removes web addresses.  It also builds the word frequency list and
 * exports it.  */

import java.io.*;
import java.util.*;
import tester.Tester;

public class TextCleaner {
  
  private final WordFrequencyList wordfreqs = new WordFrequencyList();
  private final WordFrequencyList tagfreqs = new WordFrequencyList();
  private final WordFrequencyList mentionfreqs = new WordFrequencyList();
  private final WordFrequencyList termfreqs = new WordFrequencyList();
  private final List<String> searchterms = new ArrayList<String>();
  
  public TextCleaner() {
    ListLoader.load(LDA.searchterms, searchterms);
  }
  
  public void clean(String in, String dir) {
    ListLoader.process(in, dir + LDA.cleanedFile, this::lineOperation);

    wordfreqs.write(dir);
    tagfreqs.write(dir, "tagfreqs.txt");
    mentionfreqs.write(dir, "mentionfreqs.txt");
    termfreqs.write(dir, "termfreqs.txt");
  }
  
  private String lineOperation(String in) {
    String[] words = in.split("\t");
    if (words.length != 2) {
      throw new Error("length " + words.length + " at " + words[0]);
    }
    return cleanLine(words[0], words[1]);
  }
  
  private String cleanLine(String id, String text) {
    text = text.toLowerCase();
    text = removeURLs(text);
    text = removePunctuation(text);
    String tags = getTokensStartingWith("#", tagfreqs, text);
    String mentions = getTokensStartingWith("@", mentionfreqs, text);
    text = removeTokensStartingWith("@", text);
    text = removeTokensStartingWith("#", text);
    // text = removeSearchTerms(text);
    text = collapseWhitespace(text);
    String terms = getSearchTerms(text);
    
    buildFrequencyList(text);
    
    return id + "\t" + text + "\t" + tags + "\t" + mentions + "\t" + terms;
  }
  
  // Remove urls:.  should be done before removing punctuation or they are hard
  // to find.
  private String removeURLs(String text) {
    return text.replaceAll("\\S+://\\S+", "");
  }
  
  
  // Remove punctuation.  must happen after url removal.  Some
  // are replaced with spaces, and some not, since typically characters like
  // apostrophes and dashes do not mark word endings.
  private String removePunctuation(String text) {
    text = text.replaceAll("[!,.?/:;-]", " ");
    return text.replaceAll("[^a-zA-Z0-9_#@ ]", "");
  }
  
  // Hashtags must start with a space, then a hash.
  // Any punctuation ends the hashtag.
  // Hashtags are not case-sensitive.
  // Mentions obey the same rules but with '@'
  private String getTokensStartingWith(String startChar, 
                                       WordFrequencyList freqList, 
                                       String text) {

    List<String> tokens = new ArrayList<String>();
    String[] words = text.split(" ");
    
    for (String word: words) {
      if (word.startsWith(startChar)) {
        tokens.add(word.trim());
        if (freqList != null) freqList.add(word);
      }
    }
    
    return String.join("", tokens);
  }
  
  // remove mentions after grabbing them
  private String removeTokensStartingWith(String startChar, String text) {
    return text.replaceAll(startChar + "[a-zA-Z0-9_]+", "");
  }
  
  private String collapseWhitespace(String text) {
    return text.trim().replaceAll(" +", " ");
  }
  
  private void buildFrequencyList(String text) {
    String[] words = text.split(" ");
    for (String word: words) {
      wordfreqs.add(word);
    }
  }
  
  private String getSearchTerms(String text) {
    List<String> found = new ArrayList<String>();
    found.add(" ");
    for (String term: searchterms) {
      if (text.contains(term)) found.add(term);
    }
    
    return String.join(" ", found);
  }
  
  // private String removeSearchTerms(String text, List<String> termsFound) {
  //   String[] words = text.split(" ");
  //   List<String> ret = new ArrayList<String>();
  //   
  //   for (String word: words) {
  //     for (String term: termsFound) {
  //       if (!word.contains(term)) ret.add(word); //this will not work - will add everything
  //     }
  //   }
  //   
  //   return String.join(" ", ret);
  // }
  
  public static void main(String[] args) {
    Tester t = new Tester();
    TextCleaner tc = new TextCleaner();
    
    t.is("h world", tc.collapseWhitespace(tc.removeURLs("h http://www.google.com world")));
    t.is("h world", tc.collapseWhitespace(tc.removeURLs("h https://docs.oracle.com/javase/tutorial/java/package/createpkgs.html world")));
    t.is("h world ", tc.removeURLs("h world https://www.google.co.uk/search?q=google&oq=goo&aqs=chrome.0.0j69i60l3j0j69i65.6019j0j4&sourceid=chrome&ie=UTF-8"));
    t.is("a b", tc.removePunctuation("a,b"));
    t.is("a b", tc.removePunctuation("a:b"));
    t.is("a b", tc.removePunctuation("a;b"));
    t.is("a b", tc.removePunctuation("a.b"));
    t.is("a b", tc.removePunctuation("a!b"));
    t.is("a b", tc.removePunctuation("a?b"));
    t.is("a b", tc.removePunctuation("a/b"));
    t.is("a b", tc.removePunctuation("a-b"));
    t.is("ab", tc.removePunctuation("a£$%b"));
    t.is("ab", tc.removePunctuation("a%^&*()b"));
    t.is("ab", tc.removePunctuation("a+={}[]b"));
    t.is("ab", tc.removePunctuation("a~'><\\|b"));
    t.is("ab", tc.removePunctuation("a¬`¦b"));
    t.is("#a#c", tc.getTokensStartingWith("#", null, "#a he da#bd df #c sfsd ff f"));
    t.is("@a@c", tc.getTokensStartingWith("@", null, "@a he da#bd df @c sfsd ff f"));
    
    t.results();
  }
}