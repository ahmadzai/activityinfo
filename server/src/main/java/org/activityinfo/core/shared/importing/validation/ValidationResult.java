package org.activityinfo.core.shared.importing.validation;

import org.activityinfo.core.shared.Pair;
import org.activityinfo.model.resource.ResourceId;

import java.util.List;

public class ValidationResult {

    public enum State {
        OK, MISSING, ERROR, CONFIDENCE
    }

    public static final double MINIMUM_PERSISTENCE_SCORE = 0.5;

    public static final ValidationResult MISSING = new ValidationResult(State.MISSING) {
    };

    public static final ValidationResult OK = new ValidationResult(State.OK) {
    };

    private final State state;
    private Pair<ResourceId, ResourceId> rangeWithInstanceId;
    private ResourceId instanceId;
    private String typeConversionErrorMessage;
    private String targetValue;
    private double confidence;

    private ValidationResult(State state) {
        this.state = state;
    }

    public static ValidationResult error(String message) {
        ValidationResult result = new ValidationResult(State.ERROR);
        result.typeConversionErrorMessage = message;
        return result;
    }

    public static ValidationResult missing() {
        return new ValidationResult(State.MISSING);
    }


    public static ValidationResult converted(String targetValue, double confidence) {
        ValidationResult result = new ValidationResult(State.CONFIDENCE);
        result.targetValue = targetValue;
        result.confidence = confidence;
        return result;
    }

    public boolean hasTypeConversionError() {
        return typeConversionErrorMessage != null;
    }

    public String getTypeConversionErrorMessage() {
        return typeConversionErrorMessage;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public double getConfidence() {
        return confidence;
    }

    public boolean wasConverted() {
        return targetValue != null;
    }

    public State getState() {
        return state;
    }

    public boolean isPersistable() {
        return state == State.OK || (state == State.CONFIDENCE && confidence >= MINIMUM_PERSISTENCE_SCORE);
    }

    public ResourceId getInstanceId() {
        return instanceId;
    }

    public ValidationResult setInstanceId(ResourceId instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    public boolean hasReferenceMatch() {
        return instanceId != null;
    }

    public Pair<ResourceId, ResourceId> getRangeWithInstanceId() {
        return rangeWithInstanceId;
    }

    public void setRangeWithInstanceId(Pair<ResourceId, ResourceId> rangeWithInstanceId) {
        this.rangeWithInstanceId = rangeWithInstanceId;
    }

    public static boolean isPersistable(List<ValidationResult> results) {
        for (ValidationResult result : results) {
            if (!result.isPersistable()) {
                return false;
            }
        }
        return true;
    }
}
