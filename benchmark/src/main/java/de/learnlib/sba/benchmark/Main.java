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

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.learnlib.sba.api.LearnerProvider;
import de.learnlib.sba.api.SBA;
import de.learnlib.sba.config.OptimalTTTDFAAdapter;
import de.learnlib.sba.config.TTTDFAAdapter;
import de.learnlib.sba.impl.SymbolWrapper;
import de.learnlib.sba.util.RandomSBAs;
import de.learnlib.sba.util.SBAUtil;
import de.learnlib.algorithms.spa.adapter.TTTAdapter;
import net.automatalib.automata.spa.SPA;
import net.automatalib.words.Alphabet;
import net.automatalib.words.SPAAlphabet;
import net.automatalib.words.abstractimpl.AbstractAlphabet;
import net.automatalib.words.impl.DefaultSPAAlphabet;

public class Main {

    private final static int MAX_THREADS = Math.max(10, Runtime.getRuntime().availableProcessors());
    private final static int MAX_RUNS = 25;
    private final static int[] SIZES = new int[] {10, 25, 50, 100};
    private final static int PROCS = 5;

    public static void main(String[] args) throws InterruptedException {

        final ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);
        final SPAAlphabet<Integer> alphabet = buildAlphabet(10, PROCS);

        for (int i = 0; i < MAX_RUNS; i++) {
            for (int size : SIZES) {
                final SBA<?, Integer> completeSBA = RandomSBAs.create(alphabet, size, new Random(i));
                final SBA<?, Integer> partialSBA = RandomSBAs.create2(alphabet, size, new Random(i));
                final SPA<?, Integer> completeSPA = SBAUtil.reduce(completeSBA);
                final SPA<?, Integer> partialSPA = SBAUtil.reduce(partialSBA);

                pool.submit(new SBABenchmark<>(alphabet,
                                               completeSBA,
                                               (LearnerProvider<SymbolWrapper<Integer>, TTTDFAAdapter<SymbolWrapper<Integer>>>) TTTDFAAdapter::new,
                                               "complete",
                                               size,
                                               i,
                                               false));
                pool.submit(new SPABenchmark<>(alphabet, completeSPA, TTTAdapter::new, "complete", size, i, false));
                pool.submit(new SBABenchmark<>(alphabet,
                                               partialSBA,
                                               (LearnerProvider<SymbolWrapper<Integer>, TTTDFAAdapter<SymbolWrapper<Integer>>>) TTTDFAAdapter::new,
                                               "partial",
                                               size,
                                               i,
                                               false));
                pool.submit(new SPABenchmark<>(alphabet, partialSPA, TTTAdapter::new, "partial", size, i, false));

                pool.submit(new SBABenchmark<>(alphabet,
                                               completeSBA,
                                               (LearnerProvider<SymbolWrapper<Integer>, OptimalTTTDFAAdapter<SymbolWrapper<Integer>>>) OptimalTTTDFAAdapter::new,
                                               "complete",
                                               size,
                                               i,
                                               true));
                pool.submit(new SPABenchmark<>(alphabet,
                                               completeSPA,
                                               OptimalTTTDFAAdapter::new,
                                               "complete",
                                               size,
                                               i,
                                               true));
                pool.submit(new SBABenchmark<>(alphabet,
                                               partialSBA,
                                               (LearnerProvider<SymbolWrapper<Integer>, OptimalTTTDFAAdapter<SymbolWrapper<Integer>>>) OptimalTTTDFAAdapter::new,
                                               "partial",
                                               size,
                                               i,
                                               true));
                pool.submit(new SPABenchmark<>(alphabet,
                                               partialSPA,
                                               OptimalTTTDFAAdapter::new,
                                               "partial",
                                               size,
                                               i,
                                               true));
            }
        }

        pool.shutdown();
        pool.awaitTermination(0, TimeUnit.SECONDS);
    }

    public static SPAAlphabet<Integer> buildAlphabet(int numInts, int numCalls) {

        final Alphabet<Integer> intAlphabet = new IntervalAlphabet(0, numInts - 1);
        final int maxCall = intAlphabet.size() + numCalls - 1;
        final Alphabet<Integer> callAlphabet = new IntervalAlphabet(intAlphabet.size(), maxCall);
        final Integer returnSymbol = -1;

        final Alphabet<Integer> proceduralAlphabet = new IntervalAlphabet(0, maxCall);

        return new DefaultSPAAlphabet<Integer>(intAlphabet, callAlphabet, returnSymbol) {

            @Override
            public Alphabet<Integer> getProceduralAlphabet() {
                return proceduralAlphabet;
            }
        };
    }

    private static final class IntervalAlphabet extends AbstractAlphabet<Integer> implements Alphabet<Integer> {

        private final int min;
        private final int max;

        public IntervalAlphabet(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public int size() {
            return max - min + 1;
        }

        @Override
        public Integer getSymbol(int index) {
            return index + min;
        }

        @Override
        public int getSymbolIndex(Integer symbol) {
            return symbol - min;
        }

        @Override
        public boolean containsSymbol(Integer symbol) {
            return symbol <= max && symbol >= min;
        }
    }
}
