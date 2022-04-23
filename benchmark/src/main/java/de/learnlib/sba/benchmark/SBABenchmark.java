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

import de.learnlib.sba.api.LearnerProvider;
import de.learnlib.sba.api.ProceduralLearner;
import de.learnlib.sba.api.SBA;
import de.learnlib.sba.impl.SymbolWrapper;
import de.learnlib.sba.learner.SBALearner;
import de.learnlib.sba.util.SimulatorEQOracle;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.oracle.membership.SimulatorOracle;
import net.automatalib.words.SPAAlphabet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SBABenchmark<I, L extends ProceduralLearner<SymbolWrapper<I>>> extends AbstractBenchmark<I, SBA<?, I>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SBABenchmark.class);

    static {
        LOGGER.info(LOG_HEAD);
    }

    private final SPAAlphabet<I> alphabet;
    private final SBA<?, I> sba;
    private final LearnerProvider<SymbolWrapper<I>, L> learnerProvider;

    public SBABenchmark(SPAAlphabet<I> alphabet,
                        SBA<?, I> sba,
                        LearnerProvider<SymbolWrapper<I>, L> learnerProvider,
                        String name,
                        int size,
                        int run,
                        boolean optimized) {
        super(LOGGER, run, "SBA", name, optimized, size);
        this.alphabet = alphabet;
        this.sba = sba;
        this.learnerProvider = learnerProvider;
    }

    @Override
    public void run() {
        try {
            final MembershipOracle<I, Boolean> mqo = new SimulatorOracle<>(sba);
            final EquivalenceOracle<SBA<?, I>, I, Boolean> eqo = new SimulatorEQOracle<>(sba);

            super.runExperiment(this.alphabet, mqo, eqo);
        } catch (Throwable e) {
            LOGGER.error("err", e);
        }
    }

    @Override
    public LearningAlgorithm<SBA<?, I>, I, Boolean> getLearner(MembershipOracle<I, Boolean> mqo) {
        return new SBALearner<>(this.alphabet, mqo, learnerProvider);
    }

}
