import java.util.*;

public class Translator {
  
  private final List<Long> documents;
  private final List<String> words;

  public Translator(List<String> words, List<Long> documents) {
    this.words = words;
    this.documents = documents;
  }
}