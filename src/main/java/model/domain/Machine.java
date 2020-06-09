package model.domain;

import model.domain.auth.AuthConfig;

import java.util.ArrayList;
import java.util.List;

public class Machine {
    public String name = "";
    public String opcuaAddress = "";
    public AuthConfig auth = null;
    public List<Subscription> subscriptions = new ArrayList<>();
    public int requestTimeout = 1000;

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public AuthConfig getAuth() {
        return auth;
    }

    public void setAuth(AuthConfig auth) {
        this.auth = auth;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOpcuaAddress() {
        return opcuaAddress;
    }

    public void setOpcuaAddress(String opcuaAddress) {
        this.opcuaAddress = opcuaAddress;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }
}
