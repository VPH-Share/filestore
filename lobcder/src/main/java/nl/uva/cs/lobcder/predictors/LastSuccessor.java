/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.predictors;

import java.util.HashMap;
import java.util.Map;
import javax.naming.NamingException;
import nl.uva.cs.lobcder.optimization.LobState;
import nl.uva.cs.lobcder.util.MyDataSource;

/**
 * If the first observed successor of A is B, then B will be predicted as the
 * successor every time A is observed
 *
 * @author S. Koulouzis
 */
public class LastSuccessor extends MyDataSource implements Predictor {

    Map<String, LobState> lastS = new HashMap<>();

    public LastSuccessor() throws NamingException {
    }

    @Override
    public void stop() {
        //Nothing to stop
    }

    @Override
    public LobState getNextState(LobState currentState) {
        LobState nextState = lastS.get(currentState.getID());
        return nextState;
    }

    @Override
    public void setPreviousStateForCurrent(LobState prevState, LobState currentState) {
        lastS.put(prevState.getID(), currentState);
    }
}
