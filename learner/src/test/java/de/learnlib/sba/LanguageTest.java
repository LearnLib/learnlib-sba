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

import java.util.HashMap;
import java.util.Map;

import de.learnlib.sba.api.SBA;
import de.learnlib.sba.impl.StackSBA;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.MutableDFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.builders.AutomatonBuilders;
import net.automatalib.util.automata.fsa.MutableDFAs;
import net.automatalib.words.Alphabet;
import net.automatalib.words.SPAAlphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;
import net.automatalib.words.impl.DefaultSPAAlphabet;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LanguageTest {

    private final SBA<?, Character> partialSBA;
    private final SBA<?, Character> completeSBA;

    public LanguageTest() {
        final Alphabet<Character> internalAlphabet = Alphabets.characters('a', 'c');
        final Alphabet<Character> callAlphabet = Alphabets.characters('S', 'T');
        final SPAAlphabet<Character> alphabet = new DefaultSPAAlphabet<>(internalAlphabet, callAlphabet, 'R');

        MutableDFA<?, Character> sProcedure = buildSProcedure(alphabet);
        MutableDFA<?, Character> tProcedure = buildTProcedure(alphabet);

        Map<Character, DFA<?, Character>> subModels = new HashMap<>();
        subModels.put('S', sProcedure);
        subModels.put('T', tProcedure);

        this.partialSBA = new StackSBA<>(alphabet, 'S', subModels);

        sProcedure = buildSProcedure(alphabet);
        tProcedure = buildTProcedure(alphabet);

        MutableDFAs.complete(sProcedure, alphabet);
        MutableDFAs.complete(tProcedure, alphabet);

        subModels = new HashMap<>();
        subModels.put('S', sProcedure);
        subModels.put('T', tProcedure);

        this.completeSBA = new StackSBA<>(alphabet, 'S', subModels);
    }

    @Test
    public void testCompleteAcceptance() {
        testAcceptance(this.completeSBA);
    }

    @Test
    public void testPartialAcceptance() {
        testAcceptance(this.partialSBA);
    }

    public void testAcceptance(SBA<?, Character> sba) {
        for (Word<Character> p : Word.fromCharSequence("SaSTcRRaR").prefixes(false)) {
            Assert.assertTrue(sba.accepts(p), p.toString());
        }

        Assert.assertFalse(sba.accepts(Word.fromCharSequence("T")));
        Assert.assertFalse(sba.accepts(Word.fromCharSequence("abc")));
        Assert.assertFalse(sba.accepts(Word.fromCharSequence("Sc")));
        Assert.assertFalse(sba.accepts(Word.fromCharSequence("SaRS")));
        Assert.assertFalse(sba.accepts(Word.fromCharSequence("SaRa")));
        Assert.assertFalse(sba.accepts(Word.fromCharSequence("SaaRa")));
        Assert.assertFalse(sba.accepts(Word.fromCharSequence("SaRR")));
        Assert.assertFalse(sba.accepts(Word.fromCharSequence("SaSRR")));
    }

    private static MutableDFA<?, Character> buildSProcedure(SPAAlphabet<Character> alphabet) {
        // @formatter:off
        return AutomatonBuilders.forDFA(new CompactDFA<>(alphabet))
                                .withInitial("s0")
                                .from("s0").on('T').to("s5")
                                .from("s0").on('a').to("s1")
                                .from("s0").on('b').to("s2")
                                .from("s0").on('R').to("s6")
                                .from("s1").on('S').to("s3")
                                .from("s1").on('R').to("s6")
                                .from("s2").on('S').to("s4")
                                .from("s2").on('R').to("s6")
                                .from("s3").on('a').to("s5")
                                .from("s4").on('b').to("s5")
                                .from("s5").on('R').to("s6")
                                .withAccepting("s0", "s1", "s2", "s3", "s4", "s5", "s6")
                                .create();
        // @formatter:on
    }

    private static MutableDFA<?, Character> buildTProcedure(SPAAlphabet<Character> alphabet) {
        // @formatter:off
        return AutomatonBuilders.forDFA(new CompactDFA<>(alphabet))
                                .withInitial("t0")
                                .from("t0").on('S').to("t3")
                                .from("t0").on('c').to("t1")
                                .from("t1").on('T').to("t2")
                                .from("t1").on('R').to("t4")
                                .from("t2").on('c').to("t3")
                                .from("t3").on('R').to("t4")
                                .withAccepting("t0", "t1", "t2", "t3", "t4")
                                .create();
        // @formatter:on
    }
}
