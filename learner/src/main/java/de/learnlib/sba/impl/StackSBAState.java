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

import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.spa.StackSPA;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A state in a {@link StackSPA}. Consists of a back-reference to the previous stack-element a values for the current
 * top-of-stack configuration.
 *
 * @param <I>
 *         input symbol type
 * @param <S>
 *         hypothesis state type
 *
 * @author frohme
 */
final class StackSBAState<I, S> {

    private static final StackSBAState<?, ?> INIT = new StackSBAState<>();
    private static final StackSBAState<?, ?> SINK = new StackSBAState<>();
    private static final StackSBAState<?, ?> TERM = new StackSBAState<>();

    private final @Nullable StackSBAState<I, S> prev;
    private final @Nullable DFA<S, I> procedure;
    private final @Nullable S procedureState;

    private StackSBAState() {
        this.prev = null;
        this.procedure = null;
        this.procedureState = null;
    }

    private StackSBAState(StackSBAState<I, S> prev, DFA<S, I> procedure, S procedureState) {
        this.prev = prev;
        this.procedure = procedure;
        this.procedureState = procedureState;
    }

    StackSBAState<I, S> push(DFA<S, I> newProcedure, S newState) {
        return new StackSBAState<>(this, newProcedure, newState);
    }

    StackSBAState<I, S> pop() {
        assert !isStatic() : "This method should never be called on static states";
        return prev;
    }

    StackSBAState<I, S> updateState(S state) {
        assert !isStatic() : "This method should never be called on static states";
        return new StackSBAState<>(prev, procedure, state);
    }

    DFA<S, I> getProcedure() {
        assert !isStatic() : "This method should never be called on static states";
        return procedure;
    }

    S getCurrentState() {
        assert !isStatic() : "This method should never be called on static states";
        return procedureState;
    }

    @SuppressWarnings("unchecked")
    static <I, S> StackSBAState<I, S> sink() {
        return (StackSBAState<I, S>) SINK;
    }

    boolean isSink() {
        return this == SINK;
    }

    @SuppressWarnings("unchecked")
    static <I, S> StackSBAState<I, S> init() {
        return (StackSBAState<I, S>) INIT;
    }

    boolean isInit() {
        return this == INIT;
    }

    @SuppressWarnings("unchecked")
    static <I, S> StackSBAState<I, S> term() {
        return (StackSBAState<I, S>) TERM;
    }

    boolean isTerm() {
        return this == TERM;
    }

    // contract is satisfied by definition of constructors
    @SuppressWarnings("contracts.conditional.postcondition.not.satisfied")
    @EnsuresNonNullIf(expression = {"this.prev", "this.procedure", "this.procedureState"}, result = false)
    private boolean isStatic() {
        return isInit() || isTerm() || isSink();
    }
}
