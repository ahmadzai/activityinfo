package org.activityinfo.test.webdriver;

import cucumber.api.Scenario;

import java.io.IOException;

public interface SessionReporter {
    
    void start(Scenario scenario);
    
    void finished(Scenario scenario);

    void screenshot();
}
