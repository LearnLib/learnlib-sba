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
package de.learnlib.sba.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import de.learnlib.sba.api.SBA;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.ts.simple.SimpleDTS;
import net.automatalib.words.SPAAlphabet;

/**
 * A stack-based implementation for the (instrumented) semantics of a System of Procedural Automata.
 *
 * @param <S>
 *         hypotheses state type
 * @param <I>
 *         input symbol type
 *
 * @author frohme
 */
public class StackSBA<S, I> implements SBA<StackSBAState<I, S>, I>, SimpleDTS<StackSBAState<I, S>, I> {

    private final SPAAlphabet<I> alphabet;
    private final I initialCall;
    private final Map<I, DFA<S, I>> procedures;

    // cast is fine, because we make sure to only query states belonging to the respective procedures
    @SuppressWarnings("unchecked")
    public StackSBA(SPAAlphabet<I> alphabet, I initialCall, Map<I, ? extends DFA<? extends S, I>> procedures) {
        this.alphabet = alphabet;
        this.initialCall = initialCall;
        this.procedures = (Map<I, DFA<S, I>>) procedures;
    }

    @Override
    public StackSBAState<I, S> getTransition(StackSBAState<I, S> state, I input) {
        if (state.isSink() || state.isTerm()) {
            return StackSBAState.sink();
        } else if (alphabet.isInternalSymbol(input)) {
            if (state.isInit()) {
                return StackSBAState.sink();
            }

            final DFA<S, I> model = state.getProcedure();
            final S next = model.getTransition(state.getCurrentState(), input);

            // undefined internal transition
            if (next == null || !model.isAccepting(next)) {
                return StackSBAState.sink();
            }

            return state.updateState(next);
        } else if (alphabet.isCallSymbol(input)) {
            if (state.isInit() && !Objects.equals(this.initialCall, input)) {
                return StackSBAState.sink();
            }

            final DFA<S, I> model = this.procedures.get(input);

            if (model == null) {
                return StackSBAState.sink();
            }

            final S next = model.getInitialState();

            if (next == null) {
                return StackSBAState.sink();
            }

            // store the procedural successor in the stack so that we don't need to look it up on return symbols
            final StackSBAState<I, S> returnState;
            if (state.isInit()) {
                returnState = StackSBAState.term();
            } else {
                final DFA<S, I> p = state.getProcedure();
                final S succ = p.getSuccessor(state.getCurrentState(), input);
                if (succ == null || !p.isAccepting(succ)) {
                    return StackSBAState.sink();
                }
                returnState = state.updateState(succ);
            }

            return returnState.push(model, next);
        } else if (alphabet.isReturnSymbol(input)) {
            if (state.isInit()) {
                return StackSBAState.sink();
            }

            // if we returned the state before, we checked that a procedure is available
            final DFA<S, I> model = state.getProcedure();
            final S succ = model.getSuccessor(state.getCurrentState(), input);

            // cannot return, reject word
            if (succ == null || !model.isAccepting(succ)) {
                return StackSBAState.sink();
            }

            return state.pop();
        } else {
            return StackSBAState.sink();
        }
    }

    @Override
    public boolean isAccepting(StackSBAState<I, S> state) {
        return !state.isSink() &&
               (state.isInit() || state.isTerm() || state.getProcedure().isAccepting(state.getCurrentState()));
    }

    @Override
    public StackSBAState<I, S> getInitialState() {
        return StackSBAState.init();
    }

    @Override
    public I getInitialProcedure() {
        return initialCall;
    }

    @Override
    public SPAAlphabet<I> getInputAlphabet() {
        return this.alphabet;
    }

    @Override
    public Map<I, DFA<?, I>> getProcedures() {
        return Collections.unmodifiableMap(procedures);
    }
}