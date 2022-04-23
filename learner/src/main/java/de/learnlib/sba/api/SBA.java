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
package de.learnlib.sba.api;

import java.util.Map;

import net.automatalib.automata.concepts.FiniteRepresentation;
import net.automatalib.automata.concepts.InputAlphabetHolder;
import net.automatalib.automata.concepts.SuffixOutput;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.simple.SimpleAutomaton;
import net.automatalib.automata.spa.SPA;
import net.automatalib.graphs.Graph;
import net.automatalib.graphs.concepts.GraphViewable;
import net.automatalib.ts.acceptors.DeterministicAcceptorTS;
import net.automatalib.ts.simple.SimpleTS;
import net.automatalib.words.SPAAlphabet;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface SBA<S, I> extends DeterministicAcceptorTS<S, I>,
                                   SuffixOutput<I, Boolean>,
                                   InputAlphabetHolder<I>,
                                   FiniteRepresentation,
                                   GraphViewable {

    /**
     * Refinement of {@link InputAlphabetHolder#getInputAlphabet()}' to add the constraint that an {@link SPA} operates
     * on {@link SPAAlphabet}s.
     *
     * @return the input alphabet
     */
    @Override
    SPAAlphabet<I> getInputAlphabet();

    /**
     * Returns the initial procedure of this {@link SPA}, i.e. the call symbol with which each accepted word has to
     * start.
     *
     * @return the initial procedure
     */
    @Nullable I getInitialProcedure();

    /**
     * In a complete {@link SPA} every {@link #getInputAlphabet() call symbol} should be mapped to a corresponding
     * procedure.
     *
     * @return the procedures of this {@link SPA}
     */
    Map<I, DFA<?, I>> getProcedures();

    /**
     * Return the size of this {@link SPA} which is given by the sum of the sizes of all {@link #getProcedures()
     * procedures}. Note that this value does not necessarily correspond to the classical notion of {@link
     * SimpleAutomaton#size()}, since semantically an {@link SPA}s may be infinite-sized {@link SimpleTS}.
     *
     * @return the size of this {@link SPA}
     */
    @Override
    default int size() {
        int size = 0;

        for (DFA<?, I> p : getProcedures().values()) {
            size += p.size();
        }

        return size;
    }

    @Override
    default Boolean computeOutput(Iterable<? extends I> input) {
        return this.accepts(input);
    }

    @Override
    default Graph<?, ?> graphView() {
        return null;
//        final SPAAlphabet<I> alphabet = this.getInputAlphabet();
//         explicit type specification is required by checker-framework
//        return new SPAGraphView<@Nullable Object, I>(alphabet.getCallAlphabet(),
//                                                     this.getProceduralInputs(alphabet),
//                                                     this.getProcedures());
    }

}
