package cucumber.runtime.parallel;

import cucumber.runtime.model.CucumberScenario;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Step;
import org.junit.runner.Description;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runs a scenario, or a "synthetic" scenario derived from an Examples row.
 */
public class ExecutionUnit implements Node {
    private RuntimePool runtimePool;
    private final CucumberScenario cucumberScenario;
    private final Description description;
    private List<Step> steps;
    
    public ExecutionUnit(RuntimePool runtimePool, CucumberScenario cucumberScenario) throws InitializationError {
        this.runtimePool = runtimePool;
        this.cucumberScenario = cucumberScenario;

        this.description = Description.createSuiteDescription(cucumberScenario.getVisualName(), cucumberScenario.getGherkinModel());
        
        this.steps = new ArrayList<>();
        
        if (cucumberScenario.getCucumberBackground() != null) {
            for (Step backgroundStep : cucumberScenario.getCucumberBackground().getSteps()) {
                steps.add(backgroundStep);
            }
        }

        for (Step step : cucumberScenario.getSteps()) {
            steps.add(step);
        }
    }
    
    
    
    private Description describeStep(Step step) {
        return Description.createTestDescription(cucumberScenario.getVisualName(), step.getKeyword() + step.getName(), step);
    }

    @Override
    public Description getDescription() {
        return description;
    }

    @Override
    public List<Node> getBranches() {
        return Collections.emptyList();
    }

    @Override
    public void start(Reporter reporter, Formatter formatter) {
        cucumberScenario.run(formatter, reporter, runtimePool.get());
    }

    @Override
    public void finish(Reporter reporter, Formatter formatter) {

    }

    @Override
    public List<Step> getSteps() {
        return steps;
    }

}
