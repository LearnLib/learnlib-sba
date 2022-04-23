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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.learnlib.sba.api.SBA;
import de.learnlib.sba.impl.StackSBA;
import com.google.common.collect.Maps;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.builders.AutomatonBuilders;
import net.automatalib.util.automata.builders.DFABuilder;
import net.automatalib.util.automata.fsa.MutableDFAs;
import net.automatalib.words.SPAAlphabet;

public class KeylockSBAs {

    public static <I> SBA<?, I> create(SPAAlphabet<I> alphabet, int procedureSize, Random random) {

        assert procedureSize > 2;

        final int numInternals = alphabet.getNumInternals();
        final Map<I, DFA<?, I>> dfas = Maps.newHashMapWithExpectedSize(alphabet.getNumCalls());
        final List<I> calls = new ArrayList<>(alphabet.getCallAlphabet());

        Collections.shuffle(calls, random);

        for (int i = 0; i < calls.size(); i++) {

            final CompactDFA<I> dfa = new CompactDFA<>(alphabet);
            DFABuilder<Integer, I, CompactDFA<I>>.DFABuilder__6 builder;

            if (i + 1 < calls.size()) {
                builder = AutomatonBuilders.forDFA(dfa)
                                           .withInitial("s0")
                                           .withAccepting("s0")
                                           .from("s0")
                                           .on(calls.get(i + 1))
                                           .to("s1");
            } else {
                builder = AutomatonBuilders.forDFA(dfa)
                                           .withInitial("s0")
                                           .withAccepting("s0")
                                           .from("s0")
                                           .on(alphabet.getInternalSymbol(random.nextInt(numInternals)))
                                           .to("s1");
            }

            for (int j = 1; j < procedureSize - 1; j++) {
                builder = builder.withAccepting("s" + j)
                                 .from("s" + j)
                                 .on(alphabet.getInternalSymbol(random.nextInt(numInternals)))
                                 .to("s" + (j + 1));
            }

            builder.withAccepting("s" + (procedureSize - 1))
                   .from("s" + (procedureSize - 1))
                   .on(alphabet.getReturnSymbol())
                   .to("s" + procedureSize)
                   .withAccepting("s" + procedureSize)
                   .create();

            MutableDFAs.complete(dfa, alphabet, true);
            dfas.put(calls.get(i), dfa);
        }

        return new StackSBA<>(alphabet, calls.get(0), dfas);
    }

}