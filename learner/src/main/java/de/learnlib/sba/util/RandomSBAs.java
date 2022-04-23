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
package de.learnlib.sba.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

import de.learnlib.sba.api.SBA;
import de.learnlib.sba.impl.StackSBA;
import com.google.common.collect.Maps;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.fsa.DFAs;
import net.automatalib.util.automata.fsa.MutableDFAs;
import net.automatalib.util.automata.random.RandomAutomata;
import net.automatalib.words.SPAAlphabet;

public class RandomSBAs {

    public static <I> SBA<?, I> create(SPAAlphabet<I> alphabet, int procedureSize, Random random) {

        assert procedureSize > 2;

        final I r = alphabet.getReturnSymbol();
        final Map<I, DFA<?, I>> dfas = Maps.newHashMapWithExpectedSize(alphabet.getNumCalls());

        for (final I procedure : alphabet.getCallAlphabet()) {

            final CompactDFA<I> dfa = new CompactDFA<>(alphabet);
            RandomAutomata.randomDeterministic(random,
                                               procedureSize - 2,
                                               alphabet.getProceduralAlphabet(),
                                               Collections.singletonList(Boolean.TRUE),
                                               DFA.TRANSITION_PROPERTIES,
                                               dfa,
                                               false);

            final Collection<Integer> oldStates = dfa.getStates();
            final int successSink = dfa.addState(true);

            for (int s : oldStates) {
                if (random.nextBoolean()) {
                    dfa.setTransition(s, r, successSink, null);
                }
            }

            assert DFAs.isPrefixClosed(dfa, alphabet);
            MutableDFAs.complete(dfa, alphabet, true);
            dfas.put(procedure, dfa);
        }

        return new StackSBA<>(alphabet, alphabet.getCallSymbol(random.nextInt(alphabet.getNumCalls())), dfas);
    }

    public static <I> SBA<?, I> create2(SPAAlphabet<I> alphabet, int procedureSize, Random random) {

        assert procedureSize > 2;

        final I r = alphabet.getReturnSymbol();
        final Map<I, DFA<?, I>> dfas = Maps.newHashMapWithExpectedSize(alphabet.getNumCalls());

        for (final I procedure : alphabet.getCallAlphabet()) {

            final CompactDFA<I> dfa = createProcedure(alphabet, procedureSize, random);

            MutableDFAs.complete(dfa, alphabet, true);
            dfas.put(procedure, dfa);
        }

        return new StackSBA<>(alphabet, alphabet.getCallSymbol(random.nextInt(alphabet.getNumCalls())), dfas);
    }

    public static <I> CompactDFA<I> createProcedure(SPAAlphabet<I> alphabet, int procedureSize, Random random) {

        final int numWithoutSinks = procedureSize - 2;
        final CompactDFA<I> result = new CompactDFA<>(alphabet);
        result.addInitialState(true);

        for (int i = 1; i < numWithoutSinks; i++) {
            result.addState(true);
        }

        for (int i = 0; i < numWithoutSinks; i++) {
            for (I sym : alphabet.getProceduralAlphabet()) {
                if (random.nextBoolean()) {
                    final int state = random.nextInt(result.size());
                    result.setTransition(i, sym, state, null);
                }
            }
        }

        int success = result.addState(true);
        for (int i = 0; i < numWithoutSinks; i++) {
            if (random.nextBoolean()) {
                result.setTransition(i, alphabet.getReturnSymbol(), success, null);
            }
        }

        assert DFAs.isPrefixClosed(result, alphabet);
        return result;
    }

}