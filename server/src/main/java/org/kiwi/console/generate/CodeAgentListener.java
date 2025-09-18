package org.kiwi.console.generate;

public interface CodeAgentListener {

    void onAttemptStart();

    void onAttemptSuccess();

    void onAttemptFailure(String error);

}
