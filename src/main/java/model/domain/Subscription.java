package model.domain;

import java.util.HashMap;
import java.util.Map;

public class Subscription {
    public String name = "";
    public int sampleTime = 1000;
    public Map<String, DataNode> dataPoints = new HashMap<String, DataNode>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSampleTime() {
        return sampleTime;
    }

    public void setSampleTime(int sampleTime) {
        this.sampleTime = sampleTime;
    }

    public Map<String, DataNode> getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(Map<String, DataNode> dataPoints) {
        this.dataPoints = dataPoints;
    }
}
