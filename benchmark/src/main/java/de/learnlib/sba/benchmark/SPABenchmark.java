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

import java.util.function.BiFunction;

import de.learnlib.algorithms.spa.SPALearner;
import de.learnlib.api.AccessSequenceTransformer;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.algorithm.LearningAlgorithm.DFALearner;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.oracle.equivalence.spa.SimulatorEQOracle;
import de.learnlib.oracle.membership.SimulatorOracle;
import net.automatalib.SupportsGrowingAlphabet;
import net.automatalib.automata.spa.SPA;
import net.automatalib.words.Alphabet;
import net.automatalib.words.SPAAlphabet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SPABenchmark<I, L extends DFALearner<I> & SupportsGrowingAlphabet<I> & AccessSequenceTransformer<I>>
        extends AbstractBenchmark<I, SPA<?, I>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SPABenchmark.class);

    private final SPAAlphabet<I> alphabet;
    private final SPA<?, I> spa;
    private final BiFunction<Alphabet<I>, MembershipOracle<I, Boolean>, L> provider;

    public SPABenchmark(SPAAlphabet<I> alphabet,
                        SPA<?, I> spa,
                        BiFunction<Alphabet<I>, MembershipOracle<I, Boolean>, L> provider,
                        String name,
                        int size,
                        int run,
                        boolean optimized) {
        super(LOGGER, run, "SPA", name, optimized, size);
        this.alphabet = alphabet;
        this.spa = spa;
        this.provider = provider;
    }

    @Override
    public void run() {
        try {
            final MembershipOracle<I, Boolean> mqo = new SimulatorOracle<>(spa);
            final EquivalenceOracle<SPA<?, I>, I, Boolean> eqo = new SimulatorEQOracle<>(spa);

            super.runExperiment(this.alphabet, mqo, eqo);
        } catch (Throwable e) {
            LOGGER.error("err", e);
        }
    }

    @Override
    public LearningAlgorithm<SPA<?, I>, I, Boolean> getLearner(MembershipOracle<I, Boolean> mqo) {
        return new SPALearner<>(this.alphabet, mqo, provider);
    }

}
