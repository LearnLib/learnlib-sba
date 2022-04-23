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

import de.learnlib.sba.api.SBA;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.words.SPAAlphabet;
import org.checkerframework.checker.nullness.qual.Nullable;

public class EmptySBA<I> implements SBA<Void, I> {

    private final SPAAlphabet<I> alphabet;

    public EmptySBA(SPAAlphabet<I> alphabet) {
        this.alphabet = alphabet;
    }

    @Override
    public @Nullable I getInitialProcedure() {
        return null;
    }

    @Override
    public SPAAlphabet<I> getInputAlphabet() {
        return this.alphabet;
    }

    @Override
    public Map<I, DFA<?, I>> getProcedures() {
        return Collections.emptyMap();
    }

    @Override
    public Void getTransition(Void state, I input) {
        return null;
    }

    @Override
    public boolean isAccepting(Void state) {
        return false;
    }

    @Override
    public Void getInitialState() {
        return null;
    }
}
