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

import java.util.Collection;

import net.automatalib.commons.smartcollections.ArrayStorage;
import net.automatalib.commons.util.mappings.Mapping;
import net.automatalib.words.SPAAlphabet;

public class AlphabetMapper<I> implements Mapping<I, SymbolWrapper<I>> {

    private final SPAAlphabet<I> source;
    private final ArrayStorage<SymbolWrapper<I>> target;

    public AlphabetMapper(SPAAlphabet<I> source) {
        this.source = source;
        this.target = new ArrayStorage<>(source.size());
    }

    public void set(I symbol, SymbolWrapper<I> representative) {
        this.target.set(source.getSymbolIndex(symbol), representative);
    }

    @Override
    public SymbolWrapper<I> get(I symbol) {
        return this.target.get(source.getSymbolIndex(symbol));
    }

    public Collection<SymbolWrapper<I>> values() {
        return this.target;
    }
}
