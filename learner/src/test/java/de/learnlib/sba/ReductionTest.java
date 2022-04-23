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

import java.util.Random;

import de.learnlib.sba.api.SBA;
import de.learnlib.sba.util.RandomSBAs;
import de.learnlib.sba.util.SBAUtil;
import net.automatalib.automata.spa.SPA;
import net.automatalib.util.automata.conformance.SPATestsIterator;
import net.automatalib.util.automata.conformance.WMethodTestsIterator;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;
import net.automatalib.words.impl.DefaultSPAAlphabet;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ReductionTest {

    private SBA<?, Integer> sba;
    private SPA<?, Integer> spa;

    @BeforeClass
    public void setUp() {
        sba = RandomSBAs.create(new DefaultSPAAlphabet<>(Alphabets.integers(10, 25), Alphabets.integers(0, 9), 26),
                                10,
                                new Random(69));
        spa = SBAUtil.reduce(sba);
    }

    @Test
    public void testReduction() {
        final SPATestsIterator<Integer> iter = new SPATestsIterator<>(spa, WMethodTestsIterator::new);
        while (iter.hasNext()) {
            final Word<Integer> test = iter.next();
            Assert.assertEquals(spa.accepts(test), sba.accepts(test), test.toString());
        }
    }

}
