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
package de.learnlib.sba;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.oracle.SingleQueryOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.oracle.equivalence.SampleSetEQOracle;
import de.learnlib.oracle.membership.SimulatorOracle;
import de.learnlib.sba.api.ATProvider;
import de.learnlib.sba.api.LearnerProvider;
import de.learnlib.sba.api.ProceduralLearner;
import de.learnlib.sba.api.SBA;
import de.learnlib.sba.config.DTDFAAdapter;
import de.learnlib.sba.config.LStarDFAAdapter;
import de.learnlib.sba.config.OptimalTTTDFAAdapter;
import de.learnlib.sba.config.TTTDFAAdapter;
import de.learnlib.sba.config.TTTPCDFAAdapter;
import de.learnlib.sba.impl.DefaultATProvider;
import de.learnlib.sba.impl.OptimizingATProvider;
import de.learnlib.sba.impl.SymbolWrapper;
import de.learnlib.sba.learner.SBALearner;
import de.learnlib.sba.util.KeylockSBAs;
import de.learnlib.sba.util.RandomSBAs;
import de.learnlib.sba.util.SBAUtil;
import de.learnlib.sba.util.SimulatorEQOracle;
import net.automatalib.words.SPAAlphabet;
import net.automatalib.words.impl.Alphabets;
import net.automatalib.words.impl.DefaultSPAAlphabet;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class LearnerTest {

    private SBA<?, Integer> complete;
    private SBA<?, Integer> partial;
    private SBA<?, Integer> keylock;

    @BeforeClass
    public void setUp() {
        final DefaultSPAAlphabet<Integer> alphabet =
                new DefaultSPAAlphabet<>(Alphabets.integers(10, 25), Alphabets.integers(0, 9), 26);
        complete = RandomSBAs.create(alphabet, 10, new Random(69));
        partial = RandomSBAs.create2(alphabet, 10, new Random(69));
        keylock = KeylockSBAs.create(alphabet, 10, new Random(69));
    }

    @DataProvider(name = "configProvider")
    public <I> Object[][] configProvider() {

        final List<SBA<?, Integer>> systems = Arrays.asList(complete, partial, keylock);
        final List<Function<SPAAlphabet<I>, ATProvider<I>>> atProviders =
                Arrays.asList(new DefaultSetup<>(), new OptimizingSetup<>());
        final List<Function<SBA<?, I>, EquivalenceOracle<? super SBA<?, I>, I, Boolean>>> eqProviders =
                Arrays.asList(new SimulatorProvider<>(), new CharacterizingProvider<>());

        final List<Object[]> result = new ArrayList<>(systems.size() * atProviders.size() * eqProviders.size());

        for (SBA<?, Integer> system : systems) {
            for (Function<SPAAlphabet<I>, ATProvider<I>> atProvider : atProviders) {
                for (Function<SBA<?, I>, EquivalenceOracle<? super SBA<?, I>, I, Boolean>> eqProvider : eqProviders) {
                    result.add(new Object[] {system, atProvider, eqProvider});
                }
            }
        }

        return result.toArray(new Object[result.size()][]);
    }

    @Test(dataProvider = "configProvider")
    public void testLStar(SBA<?, Integer> sba,
                          Function<SPAAlphabet<Integer>, ATProvider<Integer>> atProvider,
                          Function<SBA<?, Integer>, EquivalenceOracle<? super SBA<?, Integer>, Integer, Boolean>> eqProvider) {
        learningLoop(sba,
                     (LearnerProvider<SymbolWrapper<Integer>, LStarDFAAdapter<SymbolWrapper<Integer>>>) LStarDFAAdapter::new,
                     atProvider,
                     eqProvider);
    }

    @Test(dataProvider = "configProvider")
    public void testDT(SBA<?, Integer> sba,
                       Function<SPAAlphabet<Integer>, ATProvider<Integer>> atProvider,
                       Function<SBA<?, Integer>, EquivalenceOracle<? super SBA<?, Integer>, Integer, Boolean>> eqProvider) {
        learningLoop(sba,
                     (LearnerProvider<SymbolWrapper<Integer>, DTDFAAdapter<SymbolWrapper<Integer>>>) DTDFAAdapter::new,
                     atProvider,
                     eqProvider);
    }

    @Test(dataProvider = "configProvider")
    public void testOptimalTTT(SBA<?, Integer> sba,
                               Function<SPAAlphabet<Integer>, ATProvider<Integer>> atProvider,
                               Function<SBA<?, Integer>, EquivalenceOracle<? super SBA<?, Integer>, Integer, Boolean>> eqProvider) {
        learningLoop(sba,
                     (LearnerProvider<SymbolWrapper<Integer>, OptimalTTTDFAAdapter<SymbolWrapper<Integer>>>) OptimalTTTDFAAdapter::new,
                     atProvider,
                     eqProvider);
    }

    @Test(dataProvider = "configProvider")
    public void testTTT(SBA<?, Integer> sba,
                        Function<SPAAlphabet<Integer>, ATProvider<Integer>> atProvider,
                        Function<SBA<?, Integer>, EquivalenceOracle<? super SBA<?, Integer>, Integer, Boolean>> eqProvider) {
        learningLoop(sba,
                     (LearnerProvider<SymbolWrapper<Integer>, TTTDFAAdapter<SymbolWrapper<Integer>>>) TTTDFAAdapter::new,
                     atProvider,
                     eqProvider);
    }

    @Test(dataProvider = "configProvider")
    public void testPCTTT(SBA<?, Integer> sba,
                          Function<SPAAlphabet<Integer>, ATProvider<Integer>> atProvider,
                          Function<SBA<?, Integer>, EquivalenceOracle<? super SBA<?, Integer>, Integer, Boolean>> eqProvider) {
        learningLoop(sba,
                     (LearnerProvider<SymbolWrapper<Integer>, TTTPCDFAAdapter<SymbolWrapper<Integer>>>) TTTPCDFAAdapter::new,
                     atProvider,
                     eqProvider);
    }

    private <I, L extends ProceduralLearner<SymbolWrapper<I>>> void learningLoop(SBA<?, I> system,
                                                                                 LearnerProvider<SymbolWrapper<I>, L> adapter,
                                                                                 Function<SPAAlphabet<I>, ATProvider<I>> atProvider,
                                                                                 Function<SBA<?, I>, EquivalenceOracle<? super SBA<?, I>, I, Boolean>> eqProvider) {

        final SPAAlphabet<I> alphabet = system.getInputAlphabet();
        final MembershipOracle<I, Boolean> mqOracle = new SimulatorOracle<>(system);
        final EquivalenceOracle<? super SBA<?, I>, I, Boolean> eqOracle = eqProvider.apply(system);

        final SBALearner<I, ?> learner = new SBALearner<>(alphabet, mqOracle, adapter, atProvider.apply(alphabet));

        learner.startLearning();

        SBA<?, I> hyp = learner.getHypothesisModel();
        DefaultQuery<I, Boolean> ce;

        while ((ce = eqOracle.findCounterExample(hyp, alphabet)) != null) {
            boolean refined = false;
            while (learner.refineHypothesis(ce)) {
                refined = true;
            }

            Assert.assertTrue(refined);
            hyp = learner.getHypothesisModel();
        }

        Assert.assertEquals(system.size(), hyp.size());
        Assert.assertTrue(SBAUtil.testEquivalence(system, hyp, alphabet));
    }

    private static class DefaultSetup<I> implements Function<SPAAlphabet<I>, ATProvider<I>> {

        @Override
        public ATProvider<I> apply(SPAAlphabet<I> inputAlphabet) {
            return new DefaultATProvider<>(inputAlphabet);
        }

        @Override
        public String toString() {
            return "DefaultATProvider";
        }
    }

    private static class OptimizingSetup<I> implements Function<SPAAlphabet<I>, ATProvider<I>> {

        @Override
        public ATProvider<I> apply(SPAAlphabet<I> inputAlphabet) {
            return new OptimizingATProvider<>(inputAlphabet);
        }

        @Override
        public String toString() {
            return "OptimizingATProvider";
        }
    }

    private static class SimulatorProvider<I>
            implements Function<SBA<?, I>, EquivalenceOracle<? super SBA<?, I>, I, Boolean>> {

        @Override
        public EquivalenceOracle<? super SBA<?, I>, I, Boolean> apply(SBA<?, I> sba) {
            return new SimulatorEQOracle<>(sba);
        }

        @Override
        public String toString() {
            return "SimulatorProvider";
        }
    }

    private static class CharacterizingProvider<I>
            implements Function<SBA<?, I>, EquivalenceOracle<? super SBA<?, I>, I, Boolean>> {

        @Override
        public EquivalenceOracle<? super SBA<?, I>, I, Boolean> apply(SBA<?, I> sba) {
            final SampleSetEQOracle<I, Boolean> oracle = new SampleSetEQOracle<>(false);
            oracle.addAll((SingleQueryOracle<I, Boolean>) sba::computeSuffixOutput,
                          SBAUtil.characterizingSet(sba, sba.getInputAlphabet()));
            return oracle;
        }

        @Override
        public String toString() {
            return "CharacterizingProvider";
        }
    }
}
