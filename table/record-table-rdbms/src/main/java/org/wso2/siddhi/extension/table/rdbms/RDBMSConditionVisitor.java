/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.siddhi.extension.table.rdbms;

import org.wso2.siddhi.core.table.record.BaseConditionVisitor;
import org.wso2.siddhi.extension.table.rdbms.config.RDBMSQueryConfigurationEntry;
import org.wso2.siddhi.extension.table.rdbms.exception.RDBMSTableException;
import org.wso2.siddhi.extension.table.rdbms.util.Constant;
import org.wso2.siddhi.extension.table.rdbms.util.RDBMSTableConstants;
import org.wso2.siddhi.extension.table.rdbms.util.RDBMSTableUtils;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.expression.condition.Compare;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class RDBMSConditionVisitor extends BaseConditionVisitor {

    private static final String WHITESPACE = " ";

    private static final String SQL_AND = "AND";
    private static final String SQL_OR = "OR";
    private static final String SQL_NOT = "NOT";
    private static final String SQL_IN = "IN";
    private static final String SQL_IS_NULL = "IS NULL";
    private static final String SQL_COMPARE_LESS_THAN = "<";
    private static final String SQL_COMPARE_GREATER_THAN = ">";
    private static final String SQL_COMPARE_LESS_THAN_EQUAL = "<=";
    private static final String SQL_COMPARE_GREATER_THAN_EQUAL = ">=";
    private static final String SQL_COMPARE_EQUAL = "=";
    private static final String SQL_COMPARE_NOT_EQUAL = "!=";   // "<>" ?
    private static final String SQL_MATH_ADD = "+";
    private static final String SQL_MATH_DIVIDE = "/";
    private static final String SQL_MATH_MULTIPLY = "*";
    private static final String SQL_MATH_SUBTRACT = "-";
    private static final String SQL_MATH_MOD = "%";

    private StringBuilder condition;
    private String finalCompiledCondition;
    private RDBMSQueryConfigurationEntry queryConfig;

    private Map<String, Object> placeholders;
    private SortedMap<Integer, Object> parameters;

    private int streamVarCount;
    private int constantCount;

    private RDBMSConditionVisitor() throws IOException {
        //preventing initialization
    }

    public RDBMSConditionVisitor(RDBMSQueryConfigurationEntry entry) {
        this.condition = new StringBuilder();
        this.queryConfig = entry;
        this.streamVarCount = 0;
        this.constantCount = 0;
        this.placeholders = new HashMap<>();
        this.parameters = new TreeMap<>();
    }

    public String returnCondition() {
        this.parametrizeCondition();
        return this.finalCompiledCondition.trim();
    }

    public SortedMap<Integer, Object> getParameters() {
        return this.parameters;
    }

    @Override
    public void beginVisitAnd() {
        condition.append(RDBMSTableConstants.OPEN_PARENTHESIS);
    }

    @Override
    public void endVisitAnd() {
        condition.append(RDBMSTableConstants.CLOSE_PARENTHESIS);
    }

    @Override
    public void beginVisitAndLeftOperand() {
        //Not applicable
    }

    @Override
    public void endVisitAndLeftOperand() {
        //Not applicable
    }

    @Override
    public void beginVisitAndRightOperand() {
        condition.append(SQL_AND).append(WHITESPACE);
    }

    @Override
    public void endVisitAndRightOperand() {
        //Not applicable
    }

    @Override
    public void beginVisitOr() {
        condition.append(RDBMSTableConstants.OPEN_PARENTHESIS);
    }

    @Override
    public void endVisitOr() {
        condition.append(RDBMSTableConstants.CLOSE_PARENTHESIS);
    }

    @Override
    public void beginVisitOrLeftOperand() {
        //Not applicable
    }

    @Override
    public void endVisitOrLeftOperand() {
        //Not applicable
    }

    @Override
    public void beginVisitOrRightOperand() {
        condition.append(SQL_OR).append(WHITESPACE);
    }

    @Override
    public void endVisitOrRightOperand() {
        //Not applicable
    }

    @Override
    public void beginVisitNot() {
        condition.append(SQL_NOT).append(WHITESPACE);
    }

    @Override
    public void endVisitNot() {
        //Not applicable
    }

    @Override
    public void beginVisitCompare(Compare.Operator operator) {
        condition.append(RDBMSTableConstants.OPEN_PARENTHESIS);
    }

    @Override
    public void endVisitCompare(Compare.Operator operator) {
        condition.append(RDBMSTableConstants.CLOSE_PARENTHESIS);
    }

    @Override
    public void beginVisitCompareLeftOperand(Compare.Operator operator) {
        //Not applicable
    }

    @Override
    public void endVisitCompareLeftOperand(Compare.Operator operator) {
        //Not applicable
    }

    @Override
    public void beginVisitCompareRightOperand(Compare.Operator operator) {
        switch (operator) {
            case EQUAL:
                condition.append(SQL_COMPARE_EQUAL);
                break;
            case GREATER_THAN:
                condition.append(SQL_COMPARE_GREATER_THAN);
                break;
            case GREATER_THAN_EQUAL:
                condition.append(SQL_COMPARE_GREATER_THAN_EQUAL);
                break;
            case LESS_THAN:
                condition.append(SQL_COMPARE_LESS_THAN);
                break;
            case LESS_THAN_EQUAL:
                condition.append(SQL_COMPARE_LESS_THAN_EQUAL);
                break;
            case NOT_EQUAL:
                condition.append(SQL_COMPARE_NOT_EQUAL);
                break;
        }
        condition.append(WHITESPACE);
    }

    @Override
    public void endVisitCompareRightOperand(Compare.Operator operator) {
        //Not applicable
    }

    @Override
    public void beginVisitIsNull(String streamId) {
        condition.append(SQL_IS_NULL).append(WHITESPACE);
    }

    @Override
    public void endVisitIsNull(String streamId) {
        //Not applicable
    }

    @Override
    public void beginVisitIn(String storeId) {
        condition.append(SQL_IN).append(WHITESPACE);
    }

    @Override
    public void endVisitIn(String storeId) {
        //Not applicable
    }

    @Override
    public void beginVisitConstant(Object value, Attribute.Type type) {
        String name = this.generateConstantName();
        this.placeholders.put(name, new Constant(value, type));
        condition.append("[").append(name).append("]").append(WHITESPACE);
    }

    @Override
    public void endVisitConstant(Object value, Attribute.Type type) {
        //Not applicable
    }

    @Override
    public void beginVisitMath(MathOperator mathOperator) {
        condition.append(RDBMSTableConstants.OPEN_PARENTHESIS);
    }

    @Override
    public void endVisitMath(MathOperator mathOperator) {
        condition.append(RDBMSTableConstants.CLOSE_PARENTHESIS);
    }

    @Override
    public void beginVisitMathLeftOperand(MathOperator mathOperator) {
        //Not applicable
    }

    @Override
    public void endVisitMathLeftOperand(MathOperator mathOperator) {
        //Not applicable
    }

    @Override
    public void beginVisitMathRightOperand(MathOperator mathOperator) {
        switch (mathOperator) {
            case ADD:
                condition.append(SQL_MATH_ADD);
                break;
            case DIVIDE:
                condition.append(SQL_MATH_DIVIDE);
                break;
            case MOD:
                condition.append(SQL_MATH_MOD);
                break;
            case MULTIPLY:
                condition.append(SQL_MATH_MULTIPLY);
                break;
            case SUBTRACT:
                condition.append(SQL_MATH_SUBTRACT);
                break;
        }
        condition.append(WHITESPACE);
    }

    @Override
    public void endVisitMathRightOperand(MathOperator mathOperator) {
        //Not applicable
    }

    @Override
    public void beginVisitAttributeFunction(String namespace, String functionName) {
        if (RDBMSTableUtils.isEmpty(namespace)) {
            condition.append(functionName).append(WHITESPACE);
        } else {
            throw new RDBMSTableException("The RDBMS Event table does not support function namespaces, but namespace '"
                    + namespace + "' was specified. Please use functions supported by the defined RDBMS data store.");
        }
    }

    @Override
    public void endVisitAttributeFunction(String namespace, String functionName) {
        //Not applicable
    }

    @Override
    public void beginVisitParameterAttributeFunction(int index) {
        //TODO
    }

    @Override
    public void endVisitParameterAttributeFunction(int index) {
        //TODO
    }

    @Override
    public void beginVisitStreamVariable(String id, String streamId, String attributeName, Attribute.Type type) {
        String name = this.generateStreamVarName();
        this.placeholders.put(name, new Attribute(id, type));
        condition.append("[").append(name).append("]").append(WHITESPACE);
    }

    @Override
    public void endVisitStreamVariable(String id, String streamId, String attributeName, Attribute.Type type) {
        //Not applicable
    }

    @Override
    public void beginVisitStoreVariable(String storeId, String attributeName, Attribute.Type type) {
        condition.append(RDBMSTableConstants.PLACEHOLDER_TABLE_NAME).append(".").append(attributeName).append(WHITESPACE);
    }

    @Override
    public void endVisitStoreVariable(String storeId, String attributeName, Attribute.Type type) {
        //Not applicable
    }

    private void parametrizeCondition() {
        String query = this.condition.toString();
        String[] tokens = query.split("\\[");
        int ordinal = 0;
        for (String token : tokens) {
            if (token.contains("]")) {
                String candidate = token.substring(0, token.indexOf("]"));
                if (this.placeholders.containsKey(candidate)) {
                    this.parameters.put(ordinal, this.placeholders.get(candidate));
                    ordinal++;
                }
            }
        }
        for (String placeholder : this.placeholders.keySet()) {
            query = query.replace(placeholder, "?");
        }
        this.finalCompiledCondition = query;
    }

    private String generateStreamVarName() {
        String name = "strVar" + this.streamVarCount;
        this.streamVarCount++;
        return name;
    }

    private String generateConstantName() {
        String name = "const" + this.constantCount;
        this.constantCount++;
        return name;
    }

}
