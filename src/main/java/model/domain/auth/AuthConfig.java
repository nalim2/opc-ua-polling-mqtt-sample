package model.domain.auth;

public class AuthConfig {
    private UserAuth userAuth = null;

    public UserAuth getUserAuth() {
        return userAuth;
    }

    public void setUserAuth(UserAuth userAuth) {
        this.userAuth = userAuth;
    }
}
