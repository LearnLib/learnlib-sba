/* Copyright (C) 2022 Markus Frohme.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.sba.benchmark;

import java.time.temporal.ChronoUnit;

import com.google.common.base.Stopwatch;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.filter.cache.dfa.DFACacheOracle;
import de.learnlib.filter.cache.dfa.DFACaches;
import de.learnlib.filter.statistic.oracle.JointCounterOracle;
import de.learnlib.util.Experiment;
import net.automatalib.automata.concepts.FiniteRepresentation;
import net.automatalib.words.Alphabet;
import org.slf4j.Logger;

public abstract class AbstractBenchmark<I, A extends FiniteRepresentation> implements Runnable {

    protected final static String LOG_HEAD = "Run,Type,Name,Opt,Size,Queries,UQueries,Symbols,USymbols,CEs,NumStates,Dur";

    private final Logger logger;
    protected final String name, type;
    protected final int run, size;
    protected final boolean optimized;

    public AbstractBenchmark(Logger logger, int run, String type, String name, boolean optimized, int size) {
        this.logger = logger;
        this.run = run;
        this.type = type;
        this.name = name;
        this.optimized = optimized;
        this.size = size;
    }

    public Experiment<A> runExperiment(Alphabet<I> alphabet,
                                       MembershipOracle<I, Boolean> mqo,
                                       EquivalenceOracle<A, I, Boolean> eqo) {

        logger.debug("Starting run ({},{})", name, run);

        final JointCounterOracle<I, Boolean> postCacheMqo = new JointCounterOracle<>(mqo);
        final DFACacheOracle<I> cache = DFACaches.createDAGPCCache(alphabet, postCacheMqo);
        final JointCounterOracle<I, Boolean> preCacheMqo = new JointCounterOracle<>(cache);

        final LearningAlgorithm<A, I, Boolean> learner = getLearner(preCacheMqo);
        final Experiment<A> exp = new Experiment<>(learner, eqo, alphabet);

        final Stopwatch sw = Stopwatch.createUnstarted();

        sw.start();
        exp.run();
        sw.stop();

        final A hyp = exp.getFinalHypothesis();

        logger.info("{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}",
                    run,
                    type,
                    name,
                    optimized,
                    size,
                    preCacheMqo.getQueryCount(),
                    postCacheMqo.getQueryCount(),
                    preCacheMqo.getSymbolCount(),
                    postCacheMqo.getSymbolCount(),
                    exp.getRounds().getCount(),
                    hyp.size(),
                    sw.elapsed().get(ChronoUnit.SECONDS));

        return exp;
    }

    public abstract LearningAlgorithm<A, I, Boolean> getLearner(MembershipOracle<I, Boolean> mqo);

}
