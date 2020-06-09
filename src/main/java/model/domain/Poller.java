package model.domain;

import java.util.ArrayList;
import java.util.List;

public class Poller {
    public String mqttBrokerAddress = "";
    public String mqttTopic = "";
    public int mqttQoS = 2;
    public String mqttClientId = "";
    public List<Machine> machineSubscriptions = new ArrayList<Machine>();

    public String getMqttBrokerAddress() {
        return mqttBrokerAddress;
    }

    public void setMqttBrokerAddress(String mqttBrokerAddress) {
        this.mqttBrokerAddress = mqttBrokerAddress;
    }

    public List<Machine> getMachineSubscriptions() {
        return machineSubscriptions;
    }

    public void setMachineSubscriptions(List<Machine> machineSubscriptions) {
        this.machineSubscriptions = machineSubscriptions;
    }

    public String getMqttTopic() {
        return mqttTopic;
    }

    public void setMqttTopic(String mqttTopic) {
        this.mqttTopic = mqttTopic;
    }

    public int getMqttQoS() {
        return mqttQoS;
    }

    public void setMqttQoS(int mqttQoS) {
        this.mqttQoS = mqttQoS;
    }

    public String getMqttClientId() {
        return mqttClientId;
    }

    public void setMqttClientId(String mqttClientId) {
        this.mqttClientId = mqttClientId;
    }
}
