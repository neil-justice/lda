# Latent Dirichlet Allocation

Java implementation of [Latent Dirichlet Allocation](https://en.wikipedia.org/wiki/Latent_Dirichlet_allocation) (LDA), a topic model.

Most of the work here was done as part of the final project of my CS degree 4 years ago, apart from some recent minor improvements, so there's a lot I might have done differently now...

## TODO

* load a test dataset and evaluate its probability/perplexity according to the trained model
* load a test dataset and evaluate the topic distributions of the new documents?

## Features

* Load a corpus of documents from a file (one document per line) or list of strings.
* Detect topics in documents using a [Gibbs sampling](https://en.wikipedia.org/wiki/Gibbs_sampling) implementation of LDA.
* LDA hyperparameter optimisation method from Wallach's [PhD thesis](https://people.cs.umass.edu/~wallach/theses/wallach_phd_thesis.pdf) for adjusting the alpha hyperparameter, which represents the prior knowledge about the distribution of topics across documents.
* Generate the [term scores](http://www.cs.columbia.edu/~blei/papers/BleiLafferty2009.pdf) of the words in each topic.
* Once a model is trained, it can be used to generate new 'documents'.

## Examples

Train the model and get the top 10 words per topic:

```java
    PreprocessingPipeline pipeline = PreprocessingPipeline.defaultPipeline();
    Corpus corpus = pipeline.preprocess("path/to/file");

    LDA lda = new LDA(corpus, 10);
    LDAModel model = lda.train(1000);
    List<Topic> topics = lda.getTopics(10);
```

Train the model and generate 10 new documents drawing from topic 0 only:

```java
    PreprocessingPipeline pipeline = PreprocessingPipeline.defaultPipeline();

    Corpus corpus = pipeline.preprocess("path/to/file");

    LDA lda = new LDA(corpus, 3);
    LDAModel model = lda.train(1000);
    List<String> generated = lda.generateTextDocs(10, new double[]{1d, 0d, 0d});
```

## Licensing

License: MIT

| Dependency  | License |
| ------------- | ------------- |
| log4j2  | Apache 2.0  |
| trove4j  | LGPL  |
| junit  | Eclipse 1.0  |
