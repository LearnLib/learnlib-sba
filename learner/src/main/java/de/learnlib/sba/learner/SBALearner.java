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
package de.learnlib.sba.learner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.learnlib.sba.api.ATProvider;
import de.learnlib.sba.api.LearnerProvider;
import de.learnlib.sba.api.ProceduralLearner;
import de.learnlib.sba.api.SBA;
import de.learnlib.sba.impl.AlphabetMapper;
import de.learnlib.sba.impl.EmptySBA;
import de.learnlib.sba.impl.MappedStackSBA;
import de.learnlib.sba.impl.OptimizingATProvider;
import de.learnlib.sba.impl.ProceduralMembershipOracle;
import de.learnlib.sba.impl.SymbolWrapper;
import com.google.common.collect.Maps;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.util.MQUtil;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.commons.util.Pair;
import net.automatalib.util.automata.Automata;
import net.automatalib.words.SPAAlphabet;
import net.automatalib.words.VPDAlphabet.SymbolType;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.GrowingMapAlphabet;

public class SBALearner<I, L extends ProceduralLearner<SymbolWrapper<I>>>
        implements LearningAlgorithm<SBA<?, I>, I, Boolean> {

    private final SPAAlphabet<I> alphabet;
    private final MembershipOracle<I, Boolean> oracle;
    private final LearnerProvider<SymbolWrapper<I>, L> learnerProvider;
    private final ATProvider<I> atManager;

    private final Map<I, L> subLearners;
    private I initialCallSymbol;

    private final AlphabetMapper<I> mapper;

    public SBALearner(final SPAAlphabet<I> alphabet,
                      final MembershipOracle<I, Boolean> oracle,
                      final LearnerProvider<SymbolWrapper<I>, L> learnerProvider) {
        this(alphabet, oracle, learnerProvider, new OptimizingATProvider<>(alphabet));
    }

    public SBALearner(final SPAAlphabet<I> alphabet,
                      final MembershipOracle<I, Boolean> oracle,
                      final LearnerProvider<SymbolWrapper<I>, L> learnerProvider,
                      final ATProvider<I> atManager) {
        this.alphabet = alphabet;
        this.oracle = oracle;
        this.learnerProvider = learnerProvider;
        this.atManager = atManager;

        this.subLearners = Maps.newHashMapWithExpectedSize(this.alphabet.getNumCalls());
        this.mapper = new AlphabetMapper<>(alphabet);

        for (I i : this.alphabet.getCallAlphabet()) {
            final SymbolWrapper<I> wrapper = new SymbolWrapper<>(i, false, SymbolType.CALL);
            this.mapper.set(i, wrapper);
        }
        for (I i : this.alphabet.getInternalAlphabet()) {
            final SymbolWrapper<I> wrapper = new SymbolWrapper<>(i, false, SymbolType.INTERNAL);
            this.mapper.set(i, wrapper);
        }

        final SymbolWrapper<I> wrapper = new SymbolWrapper<>(this.alphabet.getReturnSymbol(), false, SymbolType.RETURN);
        this.mapper.set(this.alphabet.getReturnSymbol(), wrapper);
    }

    @Override
    public void startLearning() {
        // do nothing, as we have to wait for evidence that the potential main procedure actually terminates
    }

    @Override
    public boolean refineHypothesis(DefaultQuery<I, Boolean> defaultQuery) {

        boolean changed = this.extractUsefulInformationFromCounterExample(defaultQuery);

        while (refineHypothesisInternal(defaultQuery)) {
            changed = true;
        }

        ensureReturnClosure();
        return changed;
    }

    private boolean refineHypothesisInternal(DefaultQuery<I, Boolean> defaultQuery) {

        final SBA<?, I> hypothesis = this.getHypothesisModel();

        if (!MQUtil.isCounterexample(defaultQuery, hypothesis)) {
            return false;
        }

        final Word<I> input = defaultQuery.getInput();
        final int mismatchIdx = detectMismatchingIdx(hypothesis, input, defaultQuery.getOutput());

        // extract local ce
        final int callIdx = this.alphabet.findCallIndex(input, mismatchIdx);
        final I procedure = input.getSymbol(callIdx);

        final Word<I> localTrace = this.alphabet.normalize(input.subWord(callIdx + 1, mismatchIdx), 0)
                                                .append(input.getSymbol(mismatchIdx));
        final DefaultQuery<SymbolWrapper<I>, Boolean> localCE = constructLocalCE(localTrace, defaultQuery.getOutput());

        try {
            boolean localRefinement = this.subLearners.get(procedure).refineHypothesis(localCE);
        }
        catch (AssertionError ae) {
            throw new IllegalArgumentException(ae);
        }
//        assert localRefinement;

        return true;
    }

    @Override
    public SBA<?, I> getHypothesisModel() {

        if (this.subLearners.isEmpty()) {
            return new EmptySBA<>(this.alphabet);
        }

        return new MappedStackSBA<>(alphabet, initialCallSymbol, getSubModels(), mapper);
    }

    private boolean extractUsefulInformationFromCounterExample(DefaultQuery<I, Boolean> defaultQuery) {

        if (!defaultQuery.getOutput()) {
            return false;
        }

        boolean update = false;
        final Word<I> input = defaultQuery.getInput();

        // positive CEs should always be rooted at the main procedure
        this.initialCallSymbol = input.firstSymbol();

        final Pair<Set<I>, Set<I>> newSeqs = atManager.scanPositiveCounterexample(input);
        final Set<I> newCalls = newSeqs.getFirst();
        final Set<I> newTerms = newSeqs.getSecond();

        for (I call : newTerms) {
            final SymbolWrapper<I> sym = new SymbolWrapper<>(call, true, SymbolType.CALL);
            this.mapper.set(call, sym);
            for (L learner : this.subLearners.values()) {
                learner.addAlphabetSymbol(sym);
                update = true;
            }
        }

        for (I sym : newCalls) {
            update = true;
            final L newLearner = learnerProvider.createProceduralLearner(new GrowingMapAlphabet<>(this.mapper.values()),
                                                                         new ProceduralMembershipOracle<>(alphabet,
                                                                                                          oracle,
                                                                                                          sym,
                                                                                                          atManager));

            newLearner.startLearning();

            // add new learner here, so that we have an AccessSequenceTransformer available when scanning for shorter ts
            this.subLearners.put(sym, newLearner);

            // try to find a shorter terminating sequence for 'sym' before procedure is added to other hypotheses
            final Set<I> newTS =
                    this.atManager.scanRefinedProcedures(Collections.singletonMap(sym, newLearner.getHypothesisModel()),
                                                         subLearners,
                                                         newLearner.getInputAlphabet());

            for (I call : newTS) {
                final SymbolWrapper<I> wrapper = new SymbolWrapper<>(call, true, SymbolType.CALL);
                this.mapper.set(call, wrapper);
                for (L learner : this.subLearners.values()) {
                    learner.addAlphabetSymbol(wrapper);
                }
            }
        }

        return update;
    }

    private Map<I, DFA<?, SymbolWrapper<I>>> getSubModels() {
        final Map<I, DFA<?, SymbolWrapper<I>>> subModels = Maps.newHashMapWithExpectedSize(this.subLearners.size());

        for (final Map.Entry<I, L> entry : this.subLearners.entrySet()) {
            subModels.put(entry.getKey(), entry.getValue().getHypothesisModel());
        }

        return subModels;
    }

    private <S> int detectMismatchingIdx(SBA<S, I> sba, Word<I> input, boolean output) {

        if (output) {
            S stateIter = sba.getInitialState();
            int idx = 0;

            for (I i : input) {
                final S succ = sba.getSuccessor(stateIter, i);

                if (succ == null || !sba.isAccepting(succ)) {
                    return idx;
                }
                stateIter = succ;
                idx++;
            }
        }

        return input.size() - 1;
    }

    private DefaultQuery<SymbolWrapper<I>, Boolean> constructLocalCE(Word<I> input, boolean output) {

        final WordBuilder<SymbolWrapper<I>> wb = new WordBuilder<>(input.length());
        for (I i : input) {
            wb.append(mapper.get(i));
        }

        return new DefaultQuery<>(wb.toWord(), output);
    }

    private void ensureReturnClosure() {
        for (L learner : this.subLearners.values()) {
            boolean stable = false;

            while (!stable) {
                stable = ensureReturnClosure(learner.getHypothesisModel(), learner.getInputAlphabet(), learner);
            }
        }
    }

    private <S> boolean ensureReturnClosure(DFA<S, SymbolWrapper<I>> hyp,
                                            Collection<SymbolWrapper<I>> inputs,
                                            L learner) {

        final Set<Word<SymbolWrapper<I>>> cover = new HashSet<>();
        for (Word<SymbolWrapper<I>> sc : Automata.stateCover(hyp, inputs)) {
            cover.add(learner.transformAccessSequence(sc));
        }

        for (Word<SymbolWrapper<I>> cov : cover) {
            final S state = hyp.getState(cov);

            for (SymbolWrapper<I> i : inputs) {
                if (i.getType() == SymbolType.RETURN) {
                    final S succ = hyp.getSuccessor(state, i);

                    for (SymbolWrapper<I> next : inputs) {

                        if (hyp.isAccepting(hyp.getSuccessor(succ, next))) { // error closure is violated

                            final DefaultQuery<SymbolWrapper<I>, Boolean> ce =
                                    new DefaultQuery<>(cov.append(i).append(next), false);
                            final boolean refined = learner.refineHypothesis(ce);

                            assert refined;
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }
}
