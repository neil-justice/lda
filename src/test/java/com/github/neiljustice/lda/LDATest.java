package com.github.neiljustice.lda;

import com.github.neiljustice.lda.preprocess.PreprocessingPipeline;
import com.github.neiljustice.lda.topic.Topic;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LDATest {

  LDA lda;

  Corpus corpus;

  @Test
  public void shouldDetectTopics() {
    final PreprocessingPipeline pipeline = PreprocessingPipeline.noOpPipeline();

    final List<String> docs = generateDocs(100, 100);
    corpus = pipeline.preprocess(docs);

    lda = new LDA(corpus, 2);
    lda.train(1000, 10, 5, 20, 10);
    final List<Topic> topics = lda.getTopics(4);
    for (Topic topic : topics) {
      if (topic.getWords().contains("a")) {
        assertTrue(topic.getWords().toString(), topic.getWords().containsAll(Arrays.asList("a", "b", "c", "d")));
      }
      if (topic.getWords().contains("1")) {
        assertTrue(topic.getWords().toString(), topic.getWords().containsAll(Arrays.asList("1", "2", "3", "4")));
      }
    }
  }

  @Test
  public void shouldExtractModel() {
    final PreprocessingPipeline pipeline = PreprocessingPipeline.noOpPipeline();

    final List<String> docs = generateDocs(100, 100);
    corpus = pipeline.preprocess(docs);

    lda = new LDA(corpus, 2);
    final LDAModel model = lda.train(200, 10, 5, 20, 10);

    final LDA lda2 = new LDA(model);
    assertEquals(lda2.perplexity(), lda.perplexity(), 0);
  }

  @Test
  public void shouldGenerateDocs() {
    final PreprocessingPipeline pipeline = PreprocessingPipeline.noOpPipeline();

    final List<String> docs = generateDocs(100, 100);
    corpus = pipeline.preprocess(docs);

    lda = new LDA(corpus, 2);
    final LDAModel model = lda.train(200, 10, 5, 20, 10);

    final List<String> generated = lda.generateTextDocs(10, new double[]{1d, 0d});
    assertEquals(generated.size(), 10);
    // TODO how to test?
  }

  private List<String> generateDocs(int count, int maxWords) {
    final List<List<String>> types = Arrays.asList(
        Arrays.asList("a", "b", "c", "d", "filler", "filler2"),
        Arrays.asList("1", "2", "3", "4", "filler", "filler2"),
        Arrays.asList("a", "b", "3", "4", "filler", "filler2"),
        Arrays.asList("1", "2", "c", "d", "filler", "filler2")
    );
    final List<String> docs = new ArrayList<>();
    for (int doc = 0; doc < count; doc++) {
      final int max = ThreadLocalRandom.current().nextInt(maxWords - 3) + 3;
      final int type = ThreadLocalRandom.current().nextInt(types.size() - 1);
      final StringBuilder builder = new StringBuilder();
      for (int word = 0; word < max; word++) {
        builder.append(" ").append(types.get(type).get(ThreadLocalRandom.current().nextInt(types.get(type).size() - 1)));
      }
      docs.add(builder.toString());
    }
    return docs;
  }
}