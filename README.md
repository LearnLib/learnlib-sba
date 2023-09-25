**This tool will be/is included in LearnLib v0.17**

---

This tool introduces the concept of *Systems of Behavioral Automata* (SBAs), an extension to [SPAs](https://github.com/LearnLib/learnlib-spa) that support prefix-closure.
For running the tool, you need

* a working JDK (8+) installation
* a working Maven installation

---

The tool consists of two modules -- **learner** and **benchmark**.

* ### learner

  The **learner** module contains the core components of the learning algorithm.

    - The `SBALearner` class resembles the main class of the learning algorithm and integrates into the `LearningAlgorithm` framework of the LearnLib.
    - The `StackSBA` class resembles a stack-based implementation that accepts the (instrumented) language of an SBA.
    - The `config` package contains several predefined adapters that can be used to configure, which learning algorithms the `SBALearner` should use for learning the individual sub-procedures of the system under learning.

* ### benchmark

  The **benchmark** module contains a `Main` class that resembles the entry point to the benchmarks.
  You can either start the benchmark from an IDE or build an executable benchmark JAR via the following steps:
  * Run `mvn clean package`,
  * In the `benchmark/target/benchmark/`directory you will find a `learnlib-sba-benchmark-1.0-SNAPSHOT-jar-with-dependencies.jar` which can be executed with `java -jar path/to/jar`
    * Once started, the benchmark will create two files (`sba.csv`, `output.log`) in the directory from which you started the benchmark.
    * The benchmarks run in parallel. Depending on how many cores your system has, the process may require multiple GBs of RAM.
