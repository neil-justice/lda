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
  
  public void clean(String in, String dir) 
  throws FileNotFoundException, IOException {
    
    BufferedReader reader = new BufferedReader(new FileReader(new File(in)));
    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dir + LDA.cleanedFile)));
    String line;
    int cnt = 0;
    
    while ((line = reader.readLine()) != null) {
      cnt++;
      String[] splitLine = line.split("\t");
      if (splitLine.length != 2) {
        throw new Error("length " + splitLine.length + " at " + splitLine[0]);
      }
      String clean = cleanLine(splitLine[0], splitLine[1]);
      writer.write(clean);
      writer.newLine();
      if (cnt % 100 == 0) writer.flush();
    }
    writer.flush();
    wordfreqs.write(dir);
    tagfreqs.write(dir, "tagfreqs.txt");
    mentionfreqs.write(dir, "mentionfreqs.txt");
  }
  
  private String cleanLine(String id, String text) {
    text = text.toLowerCase();
    text = removeURLs(text);
    
    String[] words = text.split("[^@#a-zA-Z0-9_]");
    String tags = getTokens("#", tagfreqs, words);
    String mentions = getTokens("@", mentionfreqs, words);
    
    text = removeTokens("@", text);
    text = removeTokens("#", text);
    text = removePunctuation(text);
    text = collapseWhitespace(text);
    
    buildFrequencyList(text);
    
    return id + "\t" + text + "\t" + tags + "\t" + mentions;
  }
  
  // Remove urls:
  private String removeURLs(String text) {
    return text.replaceAll("\\S+://\\S+", "");
  }
  
  // remove mentions after grabbing them
  private String removeTokens(String startChar, String text) {
    return text.replaceAll(startChar + "[a-zA-Z0-9_]+", "");
  }
  
  // Remove punctuation.  must happen after url and hashtag removal
  private String removePunctuation(String text) {
    text = text.replaceAll("[!,.?/:;-]", " ");
    return text.replaceAll("[^a-zA-Z0-9_@ ]", "");
  }
  
  // Hashtags must start with a space, then a hash.
  // Any punctuation ends the hashtag.
  // Hashtags are not case-sensitive.
  // Mentions obey the same rules but with '@'
  private String getTokens(String startChar, WordFrequencyList freqList, String... words) {
    List<String> tokens = new ArrayList<String>();
    tokens.add(" ");
    
    for (String word: words) {
      if (word.startsWith(startChar)) {
        tokens.add(word);
        if (freqList != null) freqList.add(word);
      }
    }
    
    return String.join(" ", tokens);
  }
  
  private String collapseWhitespace(String text) {
    return text.trim().replaceAll(" +", " ");
  }
  
  private void buildFrequencyList(String text) {
    String[] splitText = text.split(" ");
    for (String s: splitText) {
      wordfreqs.add(s);
    }
  }
  
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
    t.is("ab", tc.removePunctuation("a#£$%b"));
    t.is("ab", tc.removePunctuation("a%^&*()b"));
    t.is("ab", tc.removePunctuation("a+={}[]b"));
    t.is("ab", tc.removePunctuation("a~'><\\|b"));
    t.is("ab", tc.removePunctuation("a¬`¦b"));
    t.is("#a#c", tc.getTokens("#", null, "#a", "he", "da#bd", "df", ";'", "#c;;", "sfsd", "ff", "f"));
    t.is("@a@c", tc.getTokens("@", null, "@a", "he", "da@bd", "df", ";'", "@c;;", "sfsd", "ff", "f"));
    
    t.results();
  }
}