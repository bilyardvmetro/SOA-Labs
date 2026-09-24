package org.example.soalabs.worker.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.example.soalabs.worker.domain.OrganizationType;
import org.example.soalabs.worker.domain.Worker;
import org.example.soalabs.worker.domain.WorkerStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WorkerQueryParser {

    private static final Map<String, FieldDefinition> FIELDS = fields();

    public Sort parseSort(List<String> expressions) {
        if (expressions == null || expressions.isEmpty()) {
            return Sort.by(Sort.Direction.ASC, "id");
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (String expression : expressions) {
            String[] parts = expression.split(",", -1);
            if (parts.length != 2 || !FIELDS.containsKey(parts[0]) || "organization".equals(parts[0])) {
                throw new InvalidRequestException("Invalid sort expression: " + expression);
            }
            Sort.Direction direction;
            try {
                direction = Sort.Direction.fromString(parts[1]);
            } catch (IllegalArgumentException exception) {
                throw new InvalidRequestException("Invalid sort direction in expression: " + expression);
            }
            orders.add(new Sort.Order(direction, FIELDS.get(parts[0]).path()));
        }
        return Sort.by(orders);
    }

    public Specification<Worker> parseFilters(List<String> expressions) {
        Specification<Worker> result = Specification.unrestricted();
        if (expressions == null) {
            return result;
        }

        for (String expression : expressions) {
            result = result.and(parseFilter(expression));
        }
        return result;
    }

    private Specification<Worker> parseFilter(String expression) {
        String[] parts = expression.split(":", 3);
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new InvalidRequestException("Invalid filter expression: " + expression);
        }

        FieldDefinition field = FIELDS.get(parts[0]);
        if (field == null) {
            throw new InvalidRequestException("Unsupported filter field: " + parts[0]);
        }

        String operator = parts[1];
        String value = parts.length == 3 ? parts[2] : null;
        return (root, query, criteriaBuilder) -> predicate(root, criteriaBuilder, field, operator, value, expression);
    }

    private Predicate predicate(Root<Worker> root, CriteriaBuilder cb, FieldDefinition field,
                                String operator, String rawValue, String expression) {
        Path<?> path = resolve(root, field.path());

        if ("isNull".equals(operator) || "notNull".equals(operator)) {
            if (rawValue != null && !rawValue.isEmpty()) {
                throw new InvalidRequestException("Operator " + operator + " does not accept a value");
            }
            return "isNull".equals(operator) ? cb.isNull(path) : cb.isNotNull(path);
        }
        if ("organization".equals(field.apiName())) {
            throw new InvalidRequestException("Field organization supports only isNull and notNull operators");
        }
        if (rawValue == null || rawValue.isEmpty()) {
            throw new InvalidRequestException("Filter value is required: " + expression);
        }

        if ("in".equals(operator)) {
            List<String> values = Arrays.asList(rawValue.split(",", -1));
            if (values.stream().anyMatch(String::isEmpty)) {
                throw new InvalidRequestException("Operator in requires a non-empty value list");
            }
            CriteriaBuilder.In<Object> clause = cb.in(path);
            values.stream().map(value -> parseValue(field, value)).forEach(clause::value);
            return clause;
        }

        Object value = parseValue(field, rawValue);
        return switch (operator) {
            case "eq" -> cb.equal(path, value);
            case "ne" -> cb.notEqual(path, value);
            case "gt", "gte", "lt", "lte" -> comparison(cb, path, value, operator, field);
            case "contains", "startsWith", "endsWith" -> textPredicate(cb, path, rawValue, operator, field);
            default -> throw new InvalidRequestException("Unsupported filter operator: " + operator);
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Predicate comparison(CriteriaBuilder cb, Path<?> path, Object value,
                                 String operator, FieldDefinition field) {
        if (!Comparable.class.isAssignableFrom(field.type())) {
            throw new InvalidRequestException("Operator " + operator + " is not supported for " + field.apiName());
        }
        Expression comparablePath = path;
        Comparable comparableValue = (Comparable) value;
        return switch (operator) {
            case "gt" -> cb.greaterThan(comparablePath, comparableValue);
            case "gte" -> cb.greaterThanOrEqualTo(comparablePath, comparableValue);
            case "lt" -> cb.lessThan(comparablePath, comparableValue);
            case "lte" -> cb.lessThanOrEqualTo(comparablePath, comparableValue);
            default -> throw new IllegalStateException("Unexpected comparison operator: " + operator);
        };
    }

    private Predicate textPredicate(CriteriaBuilder cb, Path<?> path, String value,
                                    String operator, FieldDefinition field) {
        if (field.type() != String.class) {
            throw new InvalidRequestException("Operator " + operator + " is supported only for text fields");
        }
        String escaped = escapeLike(value);
        String pattern = switch (operator) {
            case "contains" -> "%" + escaped + "%";
            case "startsWith" -> escaped + "%";
            case "endsWith" -> "%" + escaped;
            default -> throw new IllegalStateException("Unexpected text operator: " + operator);
        };
        return cb.like(path.as(String.class), pattern, '\\');
    }

    private Object parseValue(FieldDefinition field, String value) {
        try {
            if (field.type() == String.class) {
                return value;
            }
            if (field.type() == Integer.class) {
                return Integer.valueOf(value);
            }
            if (field.type() == Float.class) {
                float parsed = Float.parseFloat(value);
                if (!Float.isFinite(parsed)) {
                    throw new NumberFormatException("non-finite value");
                }
                return parsed;
            }
            if (field.type() == Instant.class) {
                return Instant.parse(value);
            }
            if (field.type() == WorkerStatus.class) {
                return WorkerStatus.valueOf(value);
            }
            if (field.type() == OrganizationType.class) {
                return OrganizationType.valueOf(value);
            }
        } catch (DateTimeParseException | IllegalArgumentException exception) {
            throw new InvalidRequestException("Invalid value for filter field " + field.apiName() + ": " + value);
        }
        throw new IllegalStateException("No parser for field " + field.apiName());
    }

    private Path<?> resolve(Root<Worker> root, String propertyPath) {
        Path<?> result = root;
        for (String part : propertyPath.split("\\.")) {
            result = result.get(part);
        }
        return result;
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static Map<String, FieldDefinition> fields() {
        Map<String, FieldDefinition> fields = new LinkedHashMap<>();
        add(fields, "id", "id", Integer.class);
        add(fields, "name", "name", String.class);
        add(fields, "coordinates.x", "coordinates.x", Integer.class);
        add(fields, "coordinates.y", "coordinates.y", Float.class);
        add(fields, "creationDate", "creationDate", Instant.class);
        add(fields, "salary", "salary", Integer.class);
        add(fields, "startDate", "startDate", Instant.class);
        add(fields, "endDate", "endDate", Instant.class);
        add(fields, "status", "status", WorkerStatus.class);
        add(fields, "organization", "organization.fullName", String.class);
        add(fields, "organization.fullName", "organization.fullName", String.class);
        add(fields, "organization.annualTurnover", "organization.annualTurnover", Integer.class);
        add(fields, "organization.type", "organization.type", OrganizationType.class);
        return Map.copyOf(fields);
    }

    private static void add(Map<String, FieldDefinition> fields, String apiName, String path, Class<?> type) {
        fields.put(apiName, new FieldDefinition(apiName, path, type));
    }

    private record FieldDefinition(String apiName, String path, Class<?> type) {
    }
}
