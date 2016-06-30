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
      String[] cmd = line.split(" ");
      
      switch(cmd[0]) {
        case "p":
        case "process":
          process();
          break;
        case "reload":
          reload();
          break;
        case "load":
          load();
          break;
        case "r":
        case "run":
          run(cmd);
          break;
        case "print":
          print();
          break;
        case "help":
          help();
          break;
        case "q":
        case "quit":
          quit = true;
          if (corpus != null) corpus.closeDB();
          break;
        default:
          System.out.println("Command not recognised.");
      }
    }
  }
  
  private void showInfo() {
    System.out.println("In directory : " + dir);
    System.out.println("Cleaned:   " + ft.isClean());
    System.out.println("Processed: " + ft.isProcessed());
    System.out.println("In DB:     " + ft.isInDB());
  }
  
  private void help() {
    showInfo();
    System.out.println("Commands:");
    System.out.println("  process  -- processes the clean file, removing stop words and so on");
    System.out.println("  reload   -- re-initialises db from processed file");
    System.out.println("  load     -- loads corpus from db");
    System.out.println("  run      -- args [cycles] [topics]");
    System.out.println("  print    -- prints [topic][word matrix and termscore]");
    System.out.println("  help     -- shows this list");
    System.out.println("  quit     -- exits the program");
  }
  
  private void process() {
    Preprocessor p = new Preprocessor();
    p.process(dir);
    System.out.println("Text processed.");
  }
  
  private void reload() {
    corpus = new CorpusBuilder().fromFile(dir).build();
    System.out.println("Corpus reloaded.");
  }
  
  private void load() {
    corpus = new CorpusBuilder().fromDatabase(dir).build();
    System.out.println("Corpus loaded.");
  }
  
  private void run(String[] cmd) {
    int cycles, topics;
    if (corpus == null) {
      System.out.println("Load a corpus first.");
      return;
    }
    
    if (cmd.length == 3) {
      try {
        cycles = Integer.parseInt(cmd[1]);
        topics = Integer.parseInt(cmd[2]);
      } catch (NumberFormatException e) {
        System.out.println("Invalid input. Format is run [cycles] [topics]");
        return;
      }
      corpus.run(cycles, topics);
    }
    else corpus.run(100, 30);
  }
  
  private void print() {
    if (corpus == null) System.out.println("Load a corpus first.");
    else corpus.print();
  }
}