package dev.vality.wallets.hooker.dao;

import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.wallets.hooker.dao.condition.ConditionField;
import dev.vality.wallets.hooker.dao.condition.ConditionParameterSource;
import org.jooq.Condition;
import org.jooq.Operator;
import org.jooq.impl.DSL;

import javax.sql.DataSource;

public abstract class AbstractDao extends AbstractGenericDao {

    public AbstractDao(DataSource dataSource) {
        super(dataSource);
    }

    protected Condition appendConditions(Condition condition, Operator operator,
                                         ConditionParameterSource conditionParameterSource) {
        for (ConditionField field : conditionParameterSource.getConditionFields()) {
            if (field.getValue() != null) {
                condition = DSL.condition(operator, condition, buildCondition(field));
            }
        }
        return condition;
    }

    private Condition buildCondition(ConditionField field) {
        return field.getField().compare(
                field.getComparator(),
                field.getValue()
        );
    }
}
