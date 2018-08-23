package com.bitheads.braincloud.client;

/**
 * Created by bradleyh on 4/11/2016.
 */
public class AuthenticationType {
    private String _value;

    public static final AuthenticationType Anonymous = new AuthenticationType("Anonymous");
    public static final AuthenticationType Universal = new AuthenticationType("Universal");
    public static final AuthenticationType Email = new AuthenticationType("Email");
    public static final AuthenticationType Facebook = new AuthenticationType("Facebook");
    public static final AuthenticationType GameCenter = new AuthenticationType("GameCenter");
    public static final AuthenticationType Steam = new AuthenticationType("Steam");
    public static final AuthenticationType Google = new AuthenticationType("Google");
    public static final AuthenticationType Twitter = new AuthenticationType("Twitter");
    public static final AuthenticationType Parse = new AuthenticationType("Parse");
    public static final AuthenticationType External = new AuthenticationType("External");
    public static final AuthenticationType Unknown = new AuthenticationType("Unknown");

    private AuthenticationType(String value) {
        this._value = value;
    }

    @Override
    public String toString() {
        return _value;
    }

    public static AuthenticationType fromString(String s) {
        if (s.equals(Anonymous.toString())) {
            return Anonymous;
        }
        if (s.equals(Universal.toString())) {
            return Universal;
        }
        if (s.equals(Email.toString())) {
            return Email;
        }
        if (s.equals(Facebook.toString())) {
            return Facebook;
        }
        if (s.equals(GameCenter.toString())) {
            return GameCenter;
        }
        if (s.equals(Steam.toString())) {
            return Steam;
        }
        if (s.equals(Google.toString())) {
            return Google;
        }
        if (s.equals(Twitter.toString())) {
            return Twitter;
        }
        if (s.equals(Parse.toString())) {
            return Parse;
        }
        if (s.equals(External.toString())) {
            return External;
        }
        return Unknown;
    }
}
