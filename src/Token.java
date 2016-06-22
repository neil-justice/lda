public class Token {
  private final int word; // which word ID this token is an instance of
  private final int doc;  // the ID of the document this token is in
  private int topic = -1; // the ID of the topic this token belongs to.
  
  public Token(int word, int doc) {
    this.word = word;
    this.doc = doc;
  }
  
  public int word() { return word; }
  public int doc() { return doc; }
  public int topic() {return topic; }
  public void setTopic(int topic) { this.topic = topic; }
}