/*
 * Copyright (c) 2012 David Campos, University of Aveiro.
 *
 * Neji is a framework for modular biomedical concept recognition made easy, fast and accessible.
 *
 * This project is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/.
 *
 * This project is a free software, you are free to copy, distribute, change and transmit it. However, you may not use
 * it for commercial purposes.
 *
 * It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package pt.ua.tm.neji.core.module;


import monq.jfa.*;
import pt.ua.tm.neji.exception.NejiException;

/**
 * Abstract class that integrates base functionalities of a {@link Module}.
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public abstract class BaseModule implements Module {

    private Dfa dfa;

    public BaseModule() {
        this.dfa = null;
    }

    @Override
    public Nfa getNFA() {
        if (dfa == null) {
            throw new RuntimeException("DFA module was not assigned.");
        }
        return dfa.toNfa();
    }

    @Override
    public void setNFA(final Nfa nfa, final DfaRun.FailedMatchBehaviour failedMatchBehaviour) throws NejiException {
        try {
            this.dfa = nfa.compile(failedMatchBehaviour);
        } catch (CompileDfaException ex) {
            throw new NejiException(ex);
        }
    }

    @Override
    public void setNFA(final Nfa nfa, final DfaRun.FailedMatchBehaviour failedMatchBehaviour, final FaAction eofAction)
            throws NejiException {
        try {
            this.dfa = nfa.compile(failedMatchBehaviour, eofAction);
        } catch (CompileDfaException ex) {
            throw new NejiException(ex);
        }
    }

    @Override
    public Dfa getDFA() {
        return dfa;
    }

    @Override
    public DfaRun getRun() {
        if (dfa == null) {
            throw new RuntimeException("DFA module is not assigned.");
        }
        return dfa.createRun();
    }
}
