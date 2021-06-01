package com.kripstanx.service.builder;

import com.kripstanx.domain.enumeration.AliasedEnum;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builder implementation for creating Specification for quick search functionalities
 */
public class QuickSearchSpecBuilder<T> {

    private final Logger log = LoggerFactory.getLogger(QuickSearchSpecBuilder.class);

    private List<SingularAttribute> stringAttributes;
    private List<Pair<SingularAttribute, Integer>> paddedStringAttributes;
    private List<Triple<SingularAttribute, String, SingularAttribute>> concatenatedAttributes;
    private List<AliasedBoolAttribute> boolAttributes;
    private List<AliasedStringAttribute> aliasedStringAttributes;
    private Map<SingularAttribute, String> instantAttributes;
    private List<SingularAttribute> dateAttributes;
    private Specification<T> userDefSpecification = null;
    private String quickSearchText;

    public QuickSearchSpecBuilder(String quickSearchText) {
        stringAttributes = new ArrayList<>();
        boolAttributes = new ArrayList<>();
        aliasedStringAttributes = new ArrayList<>();
        concatenatedAttributes = new ArrayList<>();
        paddedStringAttributes = new ArrayList<>();
        instantAttributes = new HashMap<>();
        dateAttributes = new ArrayList<>();
        this.quickSearchText = quickSearchText;
    }

    /**
     * Adding attribute(database column) which has to be taken into account for quick search This particular method
     * requires String kind column type which can be converted to String ambiguously
     *
     * The filtered column will be padded at the beginning to the length of padLength, with zeroes, so for example 12
     * becomes 0012
     *
     * @param attribute the attribute to be added
     * @return this
     */
    public QuickSearchSpecBuilder<T> addPaddedStringAttribute(SingularAttribute attribute, int padLength) {
        paddedStringAttributes.add(Pair.of(attribute, padLength));
        return this;
    }

    /**
     * Adding attribute(database column) which has to be taken into account for quick search This particular method
     * requires String kind column type which can be converted to String ambiguously
     *
     * @param attribute the attribute to be added
     * @return this
     */
    public QuickSearchSpecBuilder<T> addStringAttribute(SingularAttribute attribute) {
        stringAttributes.add(attribute);
        return this;
    }

    /**
     * Adding attribute(database column) which has to be taken into account for quick search This particular method
     * requires Instant (Date) kind column type which will be to converted with TO_CHAR and "DD/MM/YY HH24:MI" pattern
     * @param attribute the attribute to be added
     * @return this
     */
    public QuickSearchSpecBuilder<T> addInstantAttribute(SingularAttribute attribute, String timezone) {
        instantAttributes.put(attribute, timezone);
        return this;
    }

    public QuickSearchSpecBuilder<T> addDateAttribute(SingularAttribute attribute) {
        dateAttributes.add(attribute);
        return this;
    }

    public QuickSearchSpecBuilder<T> addConcatenatedAttributes(SingularAttribute left,
                                                               String separator,
                                                               SingularAttribute right) {
        concatenatedAttributes.add(Triple.of(left, separator, right));
        return this;
    }

    /**
     * Adding attribute(database column) which has to be taken into account for quick search This particular method
     * requires boolean column type and taken into account only if it has true value
     *
     * @param attribute the attribute to be added
     * @return this
     */
    public QuickSearchSpecBuilder<T> addAliasedBooleanAttribute(SingularAttribute attribute, String alias) {
        boolAttributes.add(new AliasedBoolAttribute(attribute, alias));
        return this;
    }

    public QuickSearchSpecBuilder<T> addAliasedStringAttribute(SingularAttribute attribute,
                                                               String valueInDb,
                                                               String aliasToUsedInsteadOfDbValue) {
        aliasedStringAttributes.add(new AliasedStringAttribute(attribute, aliasToUsedInsteadOfDbValue, valueInDb));
        return this;
    }

    public QuickSearchSpecBuilder<T> addAliasedEnumAttribute(SingularAttribute attribute,
                                                             AliasedEnum[] values) {
        Arrays.asList(values).forEach(item -> addAliasedStringAttribute(attribute, item.toString(), item.getAlias()));
        return this;
    }

    /**
     * User defined specification can be added to quick search functionality which is joined to the previously added
     * attributes with OR operation
     *
     * @param specification the specification to be added
     * @return this
     */
    public QuickSearchSpecBuilder<T> addSpecification(Specification<T> specification) {
        this.userDefSpecification = specification;
        return this;
    }

    /**
     * Building the specification based on given attributes and user defined specification
     */
    public Specification<T> build() {
        return createQuickSearchSpecification();
    }

    private Specification<T> createQuickSearchSpecification() {
        Specification<T> specification = Specification.where(null);
        if (StringUtils.isNotEmpty(quickSearchText)) {
            Specification<T> attrSpec = quickSearchSpecificationByAttributes();
            return userDefSpecification == null ? attrSpec : Specification.where(attrSpec.or(userDefSpecification));
        }

        return specification;
    }

    private Specification<T> quickSearchSpecificationByAttributes() {
        return (Specification<T>) (root, criteriaQuery, criteriaBuilder) -> {

            List<Expression> expressions = new ArrayList<>();
            expressions.addAll(createExpressionsFromStringAttributes(root));
            expressions.addAll(createExpressionsFromPaddedStringAttributes(root, criteriaBuilder));
            expressions.addAll(createExpressionsFromBoolAttributes(root, criteriaBuilder));
            expressions.addAll(createExpressionsFromAliasedStringAttributes(root, criteriaBuilder));
            expressions.addAll(createExpressionsFromConcatenatedAttributes(root, criteriaBuilder));
            expressions.addAll(createExpressionsFromInstantAttributes(root, criteriaBuilder));
            expressions.addAll(createExpressionsFromDateAttributes(root, criteriaBuilder));

            List<Predicate> predicates;
            predicates = expressions.stream()
                                    .map(x -> toPredicate(createSafeLike(criteriaBuilder, x, quickSearchText)))
                                    .collect(Collectors.toList());

            return criteriaBuilder.or(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    private Collection<? extends Expression> createExpressionsFromBoolAttributes(Root<T> root, CriteriaBuilder cb) {
        return boolAttributes.stream()
                             .map(attribute -> toExpression(createNewStringExpressionForTrueBooleanColumn(root,
                                                                                                          cb,
                                                                                                          attribute)))
                             .collect(Collectors.toList());
    }

    private Collection<? extends Expression> createExpressionsFromAliasedStringAttributes(Root<T> root,
                                                                                          CriteriaBuilder cb) {
        return aliasedStringAttributes.stream()
                                      .map(attribute -> toExpression(createNewStringExpressionForStringAliasColumn(root,
                                                                                                                   cb,
                                                                                                                   attribute)))
                                      .collect(Collectors.toList());
    }

    private Collection<? extends Expression> createExpressionsFromConcatenatedAttributes(Root<T> root,
                                                                                         CriteriaBuilder cb) {
        List<Expression<String>> expressions = new ArrayList<>();
        for (Triple<SingularAttribute, String, SingularAttribute> triple : concatenatedAttributes) {
            Expression<String> leftSide = cb.concat(root.get(triple.getLeft())
                                                        .as(String.class),
                                                    triple.getMiddle());
            expressions.add(toExpression(cb.concat(leftSide, root.get(triple.getRight()).as(String.class))));
        }

        return expressions;
    }

    private List<Expression> createExpressionsFromStringAttributes(Root<T> root) {
        return stringAttributes.stream()
                               .map(attribute -> root.get(attribute).as(String.class))
                               .collect(Collectors.toList());
    }

    private Collection<? extends Expression> createExpressionsFromPaddedStringAttributes(Root<T> root,
                                                                                         CriteriaBuilder cb) {
        // first we add as much zeroes as padLength is to the value
        // then we take the substring from this padded string, e.g
        // padLength is 5
        // the value of cell is 12 --> we add 5 zeroes, 0000012, then we drop "12".length character, which is 2
        // so it becomes 00012
        // in case of 1234, it is 000001234, then we drop 4 chars, so 01234
        // in case of 12345, it is 0000012345, then we drop 5 chars, so 12345

        // jpa substr indexing starts from 1, that is why the "+1" is there
        List<Expression<String>> expressions = new ArrayList<>();
        for (Pair<SingularAttribute, Integer> pair : paddedStringAttributes) {
            SingularAttribute dbColumn = pair.getLeft();
            int padLength = pair.getRight();
            Expression concatenatedColumn = cb.concat(StringUtils.repeat('0', padLength),
                                                      root.get(dbColumn).as(String.class));
            Expression paddedExpr = cb.substring(concatenatedColumn,
                                                 cb.sum(cb.length(root.get(dbColumn)), 1)
            );
            expressions.add(toExpression(paddedExpr));
        }
        return expressions;
    }

    private List<Expression> createExpressionsFromInstantAttributes(Root<T> root, CriteriaBuilder cb) {
        return instantAttributes.entrySet().stream()
                                .map(entry -> {
                                    if (entry.getValue().equalsIgnoreCase("LONDON")) {
                                        return cb.function("TO_CHAR_LONDON_TIME",
                                                           String.class,
                                                           root.get(entry.getKey()).as(Instant.class),
                                                           cb.literal("DD/MM/YY HH24:MI"));
                                    } else {
                                        return cb.function("TO_CHAR",
                                                           String.class,
                                                           root.get(entry.getKey()).as(Instant.class),
                                                           cb.literal("DD/MM/YY HH24:MI"));
                                    }
                                })
                                .collect(Collectors.toList());
    }

    private List<Expression> createExpressionsFromDateAttributes(Root<T> root, CriteriaBuilder cb) {
        return dateAttributes.stream()
                                .map(attribute -> cb.function("TO_CHAR",
                                                          String.class,
                                                          root.get(attribute).as(Instant.class),
                                                          cb.literal("DD/MM/YYYY")))
                                .collect(Collectors.toList());
    }

    /**
     * eg.: LIKE '%quickSearchText%'
     */
    public static Predicate createSafeLike(CriteriaBuilder cb, Expression<String> expression, String quickSearchText) {
//To replace the duplicated spaces
//        Expression<String> regexp_replace = (Expression<String>) cb.function("regexp_replace",
//                                                                             String.class,
//                                                                             cb.trim(cb.lower(expression)),
//                                                                             cb.literal(" +"),
//                                                                             cb.literal(" "));
        return cb.like(cb.lower(expression),
                       "%" + correctString(quickSearchText) + "%",
                       cb.literal('|'));
    }

    /**
     *  eg.: LIKE 'quickSearchText%'
     * @param cb
     * @param expression
     * @param quickSearchText
     * @return
     */
    public static Predicate createSafeRightLike(CriteriaBuilder cb, Expression<String> expression, String quickSearchText) {
        return cb.like(cb.lower(expression),
                       correctString(quickSearchText) + "%",
                       cb.literal('|'));
    }

    private Predicate toPredicate(Predicate predicate) {
        return predicate;
    }

    private Expression<String> toExpression(Expression<String> expression) {
        return expression;
    }

    private Expression<String> createNewStringExpressionForTrueBooleanColumn(final Root<T> root,
                                                                             CriteriaBuilder cb,
                                                                             final AliasedBoolAttribute aliasedBoolAttribute) {

        Expression<Boolean> booleanExpression = root.get(aliasedBoolAttribute.getAttribute()).as(Boolean.class);
        return cb.selectCase()
                 .when(cb.equal(booleanExpression, true), aliasedBoolAttribute.getAlias()).otherwise("")
                 .as(String.class);
    }

    private Expression<String> createNewStringExpressionForStringAliasColumn(final Root<T> root,
                                                                             CriteriaBuilder cb,
                                                                             final AliasedStringAttribute aliasedAttr) {

        Expression<String> stringExpr = root.get(aliasedAttr.getAttribute()).as(String.class);
        return cb.selectCase()
                 .when(cb.equal(stringExpr, aliasedAttr.getDbValue()), aliasedAttr.getAlias()).otherwise("")
                 .as(String.class);
    }

    private class AliasedStringAttribute {

        final private SingularAttribute<T, String> attribute;
        final private String alias;
        final private String dbValue;

        public AliasedStringAttribute(SingularAttribute<T, String> attribute, String alias, String dbValue) {
            this.attribute = attribute;
            this.alias = alias;
            this.dbValue = dbValue;
        }

        public SingularAttribute<T, String> getAttribute() {
            return attribute;
        }

        public String getAlias() {
            return alias;
        }

        public String getDbValue() {
            return dbValue;
        }
    }

    private class AliasedBoolAttribute {

        SingularAttribute<T, Boolean> attribute;
        String alias;

        public AliasedBoolAttribute(SingularAttribute<T, Boolean> attribute, String alias) {
            this.attribute = attribute;
            this.alias = alias;
        }

        public SingularAttribute<T, Boolean> getAttribute() {
            return attribute;
        }

        public String getAlias() {
            return alias;
        }
    }

    private static String correctString(String str) {
        String decodedParam = null;
        try {
            decodedParam = URLDecoder.decode(str, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            new RuntimeException(e);
        }
        return decodedParam.toLowerCase().replaceAll("%", "|%").replaceAll("_", "|_");
    }
}
