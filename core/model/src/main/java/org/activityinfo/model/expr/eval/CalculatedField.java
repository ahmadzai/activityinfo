package org.activityinfo.model.expr.eval;

import org.activityinfo.model.expr.ExprNode;
import org.activityinfo.model.expr.ExprParser;
import org.activityinfo.model.expr.diagnostic.CircularReferenceException;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.resource.Resource;
import org.activityinfo.model.type.expr.CalculatedFieldType;
import org.activityinfo.model.type.FieldType;
import org.activityinfo.model.type.FieldValue;

public class CalculatedField implements ValueSource {

    private final FormField field;
    private final ExprNode expr;

    /**
     * True if this expression is being evaluated. Used to trap circular
     * references.
     */
    private boolean evaluating = false;

    public CalculatedField(FormField field) {
        this.field = field;
        CalculatedFieldType type = (CalculatedFieldType) field.getType();
        expr = ExprParser.parse(type.getExpression());
    }

    @Override
    public FieldValue getValue(Resource instance, EvalContext context) {
        if(evaluating) {
            throw new CircularReferenceException(field.getCode());
        }
        evaluating = true;
        try {
            return expr.evaluate(context);
        } finally {
            evaluating = false;
        }
    }

    @Override
    public FieldType resolveType(EvalContext context) {
        return expr.resolveType(context);
    }
}
