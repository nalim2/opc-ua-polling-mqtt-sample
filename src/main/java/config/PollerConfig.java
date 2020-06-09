package config;

import model.domain.Poller;

public class PollerConfig {
    public boolean startOpcExampleServer = true;
    public boolean loggingOfConfig = true;
    public Poller config = new Poller();

    public Poller getConfig() {
        return config;
    }

    public void setConfig(Poller config) {
        this.config = config;
    }

    public boolean isStartOpcExampleServer() {
        return startOpcExampleServer;
    }

    public void setStartOpcExampleServer(boolean startOpcExampleServer) {
        this.startOpcExampleServer = startOpcExampleServer;
    }

    public boolean isLoggingOfConfig() {
        return loggingOfConfig;
    }

    public void setLoggingOfConfig(boolean loggingOfConfig) {
        this.loggingOfConfig = loggingOfConfig;
    }
}
