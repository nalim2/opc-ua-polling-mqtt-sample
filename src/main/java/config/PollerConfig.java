package config;

import model.domain.Poller;

public class PollerConfig  {
    public Poller config = new Poller();

    public Poller getConfig() {
        return config;
    }

    public void setConfig(Poller config) {
        this.config = config;
    }
}
