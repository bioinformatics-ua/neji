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

import monq.jfa.Dfa;
import monq.jfa.DfaRun;
import monq.jfa.FaAction;
import monq.jfa.Nfa;
import pt.ua.tm.neji.exception.NejiException;

/**
 * Interface that defines a module.
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public interface Module {
    Nfa getNFA();

    Dfa getDFA();

    void setNFA(final Nfa nfa, final DfaRun.FailedMatchBehaviour failedMatchBehaviour) throws NejiException;

    void setNFA(final Nfa nfa, final DfaRun.FailedMatchBehaviour failedMatchBehaviour, final FaAction eofAction) throws NejiException;

    DfaRun getRun();
}
