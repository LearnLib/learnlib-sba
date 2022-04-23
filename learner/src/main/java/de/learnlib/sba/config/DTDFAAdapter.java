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
package de.learnlib.sba.config;

import de.learnlib.sba.api.ProceduralLearner;
import de.learnlib.algorithms.spa.adapter.DiscriminationTreeAdapter;
import de.learnlib.api.oracle.MembershipOracle;
import net.automatalib.words.Alphabet;

public class DTDFAAdapter<I> extends DiscriminationTreeAdapter<I> implements ProceduralLearner<I> {

    public DTDFAAdapter(Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle) {
        super(alphabet, oracle);
    }

    @Override
    public Alphabet<I> getInputAlphabet() {
        return super.alphabet;
    }
}

