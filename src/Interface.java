import java.util.*;

class Interface {
  private final String dir;
  private final Scanner input = new Scanner(System.in);
  private final FileTracker ft;
  private Corpus corpus;
  private boolean quit = false;
  
  public Interface(String dir) {
    this.dir = dir;
    ft = new FileTracker(dir);
    showInfo();
  }
  
  public Interface() {
    System.out.println("Open which directory?");
    DirectoryLoader dl = new DirectoryLoader();
    String[] dirs = dl.directories();
    boolean chosen = false;
    int choice = -1;
    
    for (int i = 0; i < dirs.length; i++) {
      System.out.println(i + " : " + dirs[i]);
    }
    
    while(!chosen) {
      try {
        System.out.println("Enter a number: ");
        String line = input.nextLine();
        choice = Integer.parseInt(line);
        if (choice >= dirs.length) throw new Exception();
        else chosen = true;
      } catch (Exception e) {
        chosen = false;
      }
    }
    
    dl.setDirectory(choice);
    dir = dl.dir();
    ft = new FileTracker(dir);
    showInfo();
  }

  public void loop() {
    while(!quit) {
      System.out.println("Enter a command: ");
      String line = input.nextLine();
      switch(line) {
        case "p":
        case "process":
          process();
          break;
        case "load txt":
        case "load from txt":
          load();
          break;
        case "load db":
        case "load from db":
          loadDB();
          break;
        case "r":
        case "run":
          run();
          break;
        case "print":
          print();
          break;
        case "q":
        case "quit":
          quit = true;
          if (corpus != null) corpus.closeDB();
          break;
      }
    }
  }
  
  private void showInfo() {
    System.out.println("In directory : " + dir);
    System.out.println("Has been cleaned: " + ft.isClean());
    System.out.println("Has been processed: " + ft.isProcessed());
    System.out.println("Has been loaded to db: " + ft.isInDB());
  }
  
  private void process() {
    Preprocessor p = new Preprocessor();
    p.process(dir);
    System.out.println("Text processed.");
  }
  
  private void load() {
    corpus = new CorpusBuilder().fromFile(dir).build();
    System.out.println("Corpus loaded.");
  }
  
  private void loadDB() {
    corpus = new CorpusBuilder().fromDatabase(dir).build();
    System.out.println("Corpus loaded.");
  }
  
  private void run() {
    if (corpus == null) System.out.println("Load a corpus first.");
    else corpus.run();
  }
  
  private void print() {
    if (corpus == null) System.out.println("Load a corpus first.");
    else corpus.print();
  }
}