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

import de.learnlib.sba.api.SBA;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.query.DefaultQuery;
import net.automatalib.words.SPAAlphabet;
import net.automatalib.words.Word;

public class SimulatorEQOracle<I> implements EquivalenceOracle<SBA<?, I>, I, Boolean> {

    private final SBA<?, I> sba;

    public SimulatorEQOracle(SBA<?, I> sba) {
        this.sba = sba;
    }

    @Override
    public DefaultQuery<I, Boolean> findCounterExample(SBA<?, I> hypothesis, Collection<? extends I> inputs) {

        if (!(inputs instanceof SPAAlphabet)) {
            throw new IllegalArgumentException("Inputs are not an SPA alphabet");
        }

        @SuppressWarnings("unchecked")
        final SPAAlphabet<I> alphabet = (SPAAlphabet<I>) inputs;

        final Word<I> sep = SBAUtil.findSeparatingWord(sba, hypothesis, alphabet);

        if (sep == null) {
            return null;
        }

        return new DefaultQuery<>(sep, sba.computeOutput(sep));
    }

}
