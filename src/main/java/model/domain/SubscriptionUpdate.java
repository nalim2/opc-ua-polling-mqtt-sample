package model.domain;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

import java.sql.Timestamp;
import java.util.Map;

public class SubscriptionUpdate {
    Map<String, DataValue> UpdateValues;
    Timestamp timestamp;
    Machine machine;

    public SubscriptionUpdate(Map<String, DataValue> updateValues, Timestamp timestamp, Machine machine) {
        UpdateValues = updateValues;
        this.timestamp = timestamp;
        this.machine = machine;
    }

    public Map<String, DataValue> getUpdateValues() {
        return UpdateValues;
    }


    public Timestamp getTimestamp() {
        return timestamp;
    }


    public Machine getMachine() {
        return machine;
    }


}
