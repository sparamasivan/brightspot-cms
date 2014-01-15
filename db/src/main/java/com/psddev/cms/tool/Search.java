package com.psddev.cms.tool;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Directory;
import com.psddev.cms.db.Draft;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUi;
import com.psddev.dari.db.CompoundPredicate;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.DatabaseEnvironment;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectIndex;
import com.psddev.dari.db.ObjectStruct;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Singleton;
import com.psddev.dari.db.Sorter;
import com.psddev.dari.util.HuslColorSpace;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.PaginatedResult;
import com.psddev.dari.util.StringUtils;

public class Search extends Record {

    public static final String ADDITIONAL_PREDICATE_PARAMETER = "aq";
    public static final String ADVANCED_QUERY_PARAMETER = "av";
    public static final String GLOBAL_FILTER_PARAMETER_PREFIX = "gf.";
    public static final String FIELD_FILTER_PARAMETER_PREFIX = "f.";
    public static final String LIMIT_PARAMETER = "l";
    public static final String MISSING_FILTER_PARAMETER_SUFFIX = ".m";
    public static final String NAME_PARAMETER = "n";
    public static final String OFFSET_PARAMETER = "o";
    public static final String COLOR_PARAMETER = "c";
    public static final String ONLY_PATHED_PARAMETER = "p";
    public static final String PARENT_PARAMETER = "pt";
    public static final String QUERY_STRING_PARAMETER = "q";
    public static final String RETURN_QUERY_PARAMETER = "rq";
    public static final String SELECTED_TYPE_PARAMETER = "st";
    public static final String SHOW_DRAFTS_PARAMETER = "d";
    public static final String SHOW_MISSING_PARAMETER = "m";
    public static final String SORT_PARAMETER = "s";
    public static final String SUGGESTIONS_PARAMETER = "sg";
    public static final String TYPES_PARAMETER = "rt";

    public static final String NEWEST_SORT_LABEL = "Newest";
    public static final String NEWEST_SORT_VALUE = "_newest";
    public static final String RELEVANT_SORT_LABEL = "Relevant";
    public static final String RELEVANT_SORT_VALUE = "_relevant";

    public static final double RELEVANT_SORT_LABEL_BOOST = 10.0;

    private String name;
    private Set<ObjectType> types;
    private ObjectType selectedType;
    private String queryString;
    private String color;
    private boolean onlyPathed;
    private String additionalPredicate;
    private String advancedQuery;
    private boolean returnQuery;
    private UUID parentId;
    private Map<String, String> globalFilters;
    private Map<String, Map<String, String>> fieldFilters;
    private String sort;
    private boolean showDrafts;
    private boolean showMissing;
    private boolean suggestions;
    private long offset;
    private int limit;

    public Search() {
    }

    public Search(ObjectField field) {
        getTypes().addAll(field.getTypes());
        setOnlyPathed(ToolUi.isOnlyPathed(field));
        setAdditionalPredicate(field.getPredicate());
    }

    public Search(ToolPageContext page, Iterable<UUID> typeIds) {
        this.page = page;

        setName(page.param(String.class, NAME_PARAMETER));

        if (typeIds != null) {
            for (UUID typeId : typeIds) {
                ObjectType type = ObjectType.getInstance(typeId);

                if (type != null) {
                    getTypes().add(type);
                }
            }
        }

        for (String name : page.paramNamesList()) {
            if (name.startsWith(GLOBAL_FILTER_PARAMETER_PREFIX)) {
                getGlobalFilters().put(name.substring(GLOBAL_FILTER_PARAMETER_PREFIX.length()), page.param(String.class, name));

            } else if (name.startsWith(FIELD_FILTER_PARAMETER_PREFIX)) {
                String filterName = name.substring(FIELD_FILTER_PARAMETER_PREFIX.length());
                int dotAt = filterName.indexOf('.');
                String filterValueKey;

                if (dotAt < 0) {
                    filterValueKey = "";

                } else {
                    filterValueKey = filterName.substring(dotAt + 1);
                    filterName = filterName.substring(0, dotAt);
                }

                Map<String, String> filterValue = getFieldFilters().get(filterName);

                if (filterValue == null) {
                    filterValue = new HashMap<String, String>();
                    getFieldFilters().put(filterName, filterValue);
                }

                filterValue.put(filterValueKey, page.param(String.class, name));
            }
        }

        setSelectedType(ObjectType.getInstance(page.param(UUID.class, SELECTED_TYPE_PARAMETER)));
        setQueryString(page.paramOrDefault(String.class, QUERY_STRING_PARAMETER, "").trim());
        setColor(page.param(String.class, COLOR_PARAMETER));
        setOnlyPathed(page.param(boolean.class, IS_ONLY_PATHED));
        setAdditionalPredicate(page.param(String.class, ADDITIONAL_QUERY_PARAMETER));
        setAdvancedQuery(page.param(String.class, ADVANCED_QUERY_PARAMETER));
        setReturnQuery(page.param(boolean.class, RETURN_QUERY_PARAMETER));
        setParentId(page.param(UUID.class, PARENT_PARAMETER));
        setSort(page.param(String.class, SORT_PARAMETER));
        setShowDrafts(page.param(boolean.class, SHOW_DRAFTS_PARAMETER));
        setShowMissing(page.param(boolean.class, SHOW_MISSING_PARAMETER));
        setSuggestions(page.param(boolean.class, SUGGESTIONS_PARAMETER));
        setOffset(page.param(long.class, OFFSET_PARAMETER));
        setLimit(page.paramOrDefault(int.class, LIMIT_PARAMETER, 10));
    }

    public Search(ToolPageContext page) {
        this(page, page.params(UUID.class, TYPES_PARAMETER));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<ObjectType> getTypes() {
        if (types == null) {
            types = new HashSet<ObjectType>();
        }
        return types;
    }

    public void setTypes(Set<ObjectType> types) {
        this.types = types;
    }

    public ObjectType getSelectedType() {
        return selectedType;
    }

    public void setSelectedType(ObjectType selectedType) {
        this.selectedType = selectedType;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isOnlyPathed() {
        return onlyPathed;
    }

    public void setOnlyPathed(boolean onlyPathed) {
        this.onlyPathed = onlyPathed;
    }

    public String getAdditionalPredicate() {
        return additionalPredicate;
    }

    public void setAdditionalPredicate(String additionalPredicate) {
        this.additionalPredicate = additionalPredicate;
    }

    public String getAdvancedQuery() {
        return advancedQuery;
    }

    public void setAdvancedQuery(String advancedQuery) {
        this.advancedQuery = advancedQuery;
    }

    public boolean isReturnQuery() {
        return returnQuery;
    }

    public void setReturnQuery(boolean returnQuery) {
        this.returnQuery = returnQuery;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public Map<String, String> getGlobalFilters() {
        if (globalFilters == null) {
            globalFilters = new HashMap<String, String>();
        }
        return globalFilters;
    }

    public void setGlobalFilters(Map<String, String> globalFilters) {
        this.globalFilters = globalFilters;
    }

    public Map<String, Map<String, String>> getFieldFilters() {
        if (fieldFilters == null) {
            fieldFilters = new HashMap<String, Map<String, String>>();
        }
        return fieldFilters;
    }

    public void setFieldFilters(Map<String, Map<String, String>> fieldFilters) {
        this.fieldFilters = fieldFilters;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public boolean isShowDrafts() {
        return showDrafts;
    }

    public void setShowDrafts(boolean showDrafts) {
        this.showDrafts = showDrafts;
    }

    public boolean isShowMissing() {
        return showMissing;
    }

    public void setShowMissing(boolean showMissing) {
        this.showMissing = showMissing;
    }

    public boolean isSuggestions() {
        return suggestions;
    }

    public void setSuggestions(boolean suggestions) {
        this.suggestions = suggestions;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public Set<ObjectType> findValidTypes() {
        Set<ObjectType> types = getTypes();
        List<ObjectType> validTypes = new ArrayList<ObjectType>();

        if (types.size() == 1) {
            ObjectType type = types.iterator().next();

            if (type != null && Content.class.equals(type.getObjectClass())) {
                types.clear();
            }
        }

        for (ObjectType type : types) {
            validTypes.addAll(type.as(ToolUi.class).findDisplayTypes());
        }

        Collections.sort(validTypes);
        return new LinkedHashSet<ObjectType>(validTypes);
    }

    /**
     * Finds all sorters that can be applied to this search.
     *
     * @return Never {@code null}. The key is the unique ID, and the value
     * is the label. Sorted by the label.
     */
    public Map<String, String> findSorts() {
        Map<String, String> sorts = new LinkedHashMap<String, String>();
        ObjectType selectedType = getSelectedType();

        if (!ObjectUtils.isBlank(getQueryString())) {
            sorts.put(RELEVANT_SORT_VALUE, RELEVANT_SORT_LABEL);
        }

        addSorts(sorts, selectedType);
        addSorts(sorts, Database.Static.getDefault().getEnvironment());

        List<Map.Entry<String, String>> sortsList = new ArrayList<Map.Entry<String, String>>(sorts.entrySet());

        Collections.sort(sortsList, new Comparator<Map.Entry<String, String>>() {

            @Override
            public int compare(Map.Entry<String, String> x, Map.Entry<String, String> y) {
                return ObjectUtils.compare(x.getValue(), y.getValue(), false);
            }
        });

        sorts.clear();

        for (Map.Entry<String, String> entry : sortsList) {
            sorts.put(entry.getKey(), entry.getValue());
        }

        return sorts;
    }

    private void addSorts(Map<String, String> sorts, ObjectStruct struct) {
        if (struct != null) {
            for (ObjectField field : ObjectStruct.Static.findIndexedFields(struct)) {
                if (field.as(ToolUi.class).isEffectivelySortable()) {
                    sorts.put(field.getInternalName(), field.getDisplayName());
                }
            }
        }
    }

    public Query<?> toQuery(Site site) {
        Query<?> query = null;
        Set<ObjectType> types = getTypes();
        ObjectType selectedType = getSelectedType();
        Set<ObjectType> validTypes = findValidTypes();
        boolean isAllSearchable = true;

        if (selectedType != null) {
            isAllSearchable = Content.Static.isSearchableType(selectedType);
            query = Query.fromType(selectedType);

        } else {
            for (ObjectType type : validTypes) {
                if (!Content.Static.isSearchableType(type)) {
                    isAllSearchable = false;
                }
            }

            if (types.size() == 1) {
                for (ObjectType type : types) {
                    query = Query.fromType(type);
                    break;
                }

            } else {
                query = isAllSearchable ? Query.fromGroup(Content.SEARCHABLE_GROUP) : Query.fromAll();

                if (!validTypes.isEmpty()) {
                    query.where("_type = ?", validTypes);
                }
            }
        }

        String queryString = getQueryString();
        String sort = getSort();
        boolean metricSort = false;

        if (RELEVANT_SORT_VALUE.equals(sort)) {
            if (isAllSearchable && selectedType != null) {
                List<String> labelFields = selectedType.getLabelFields();

                if (labelFields != null && !labelFields.isEmpty()) {
                    Set<String> indexedFields = new HashSet<String>();

                    for (ObjectIndex index : selectedType.getIndexes()) {
                        indexedFields.addAll(index.getFields());
                    }

                    for (ObjectField field : selectedType.getFields()) {
                        String fieldName = field.getInternalName();

                        if (indexedFields.contains(fieldName) &&
                                labelFields.contains(fieldName)) {
                            query.sortRelevant(RELEVANT_SORT_LABEL_BOOST, field.getUniqueName() + " ~= ?", queryString);
                        }
                    }
                }
            }

        } else if (sort != null) {
            ObjectField sortField = selectedType != null ?
                    selectedType.getFieldGlobally(sort) :
                    Database.Static.getDefault().getEnvironment().getField(sort);

            if (sortField != null) {
                if (sortField.isMetric()) {
                    metricSort = true;
                }

                String sortName = selectedType != null ?
                        selectedType.getInternalName() + "/" + sort :
                        sort;

                if (ObjectField.TEXT_TYPE.equals(sortField.getInternalType())) {
                    query.sortAscending(sortName);

                } else {
                    query.sortDescending(sortName);
                }

                if (!isShowMissing()) {
                    query.and(sortName + " != missing");
                }
            }
        }

        if (metricSort) {

        } else if (ObjectUtils.isBlank(queryString)) {
            if (isAllSearchable) {
                query.and("* ~= *");
            }

        } else {

            // Strip http: or https: from the query for search by path below.
            if (queryString.length() > 8
                    && StringUtils.matches(queryString, "(?i)https?://.*")) {
                int slashAt = queryString.indexOf("/", 8);

                if (slashAt > -1) {
                    queryString = queryString.substring(slashAt);
                }
            }

            // Search by path.
            if (isAllSearchable && queryString.startsWith("/") && queryString.length() > 1) {
                List<String> paths = new ArrayList<String>();

                for (Directory directory : Query.
                        from(Directory.class).
                        where("path ^=[c] ?", queryString).
                        selectAll()) {
                    paths.add(directory.getRawPath());
                }

                int lastSlashAt = queryString.lastIndexOf("/");

                if (lastSlashAt > 0) {
                    for (Directory directory : Query.
                            from(Directory.class).
                            where("path ^=[c] ?", queryString.substring(0, lastSlashAt)).
                            selectAll()) {
                        paths.add(directory.getRawPath() + queryString.substring(lastSlashAt + 1));
                    }
                }

                query.and(Directory.PATHS_FIELD + " ^= ?", paths);

            // Full text search.
            } else if (isAllSearchable) {
                int lastSpaceAt = queryString.lastIndexOf(" ");

                if (lastSpaceAt > -1) {
                    query.and("* ~= ?", Arrays.asList(queryString, queryString.substring(0, lastSpaceAt)));

                } else {
                    query.and("* ~= ?", queryString);
                }

            } else if (selectedType != null) {
                for (String field : selectedType.getLabelFields()) {
                    if (selectedType.getIndex(field) != null) {
                        query.and(selectedType.getInternalName() + "/" + field + " contains[c] ?", queryString);
                    }
                    break;
                }

            } else {
                Predicate predicate = null;

                for (ObjectType type : validTypes) {
                    String prefix = type.getInternalName() + "/";

                    for (String field : type.getLabelFields()) {
                        if (type.getIndex(field) != null) {
                            predicate = CompoundPredicate.combine(
                                    PredicateParser.OR_OPERATOR,
                                    predicate,
                                    PredicateParser.Static.parse(prefix + field + " ^=[c] ?", queryString));
                        }
                        break;
                    }
                }

                query.and(predicate);
            }
        }

        if (isOnlyPathed()) {
            query.and(Directory.Static.hasPathPredicate());
        }

        if (isAllSearchable && selectedType == null) {
            DatabaseEnvironment environment = Database.Static.getDefault().getEnvironment();
            String q = getQueryString();

            if (!ObjectUtils.isBlank(q)) {
                q = q.replaceAll("\\s+", "").toLowerCase(Locale.ENGLISH);

                for (ObjectType t : environment.getTypes()) {
                    String name = t.getDisplayName();

                    if (!ObjectUtils.isBlank(name) &&
                            q.contains(name.replaceAll("\\s+", "").toLowerCase(Locale.ENGLISH))) {
                        query.sortRelevant(20.0, "_type = ?", t.as(ToolUi.class).findDisplayTypes());
                    }
                }
            }

            query.sortRelevant(10.0, "_type = ?", environment.getTypeByClass(Singleton.class).as(ToolUi.class).findDisplayTypes());
        }

        String additionalPredicate = getAdditionalPredicate();

        if (!ObjectUtils.isBlank(additionalPredicate)) {
            query.and(additionalPredicate, Query.
                    from(Object.class).
                    where("_id = ?", getParentId()).
                    first());
        }

        String advancedQuery = getAdvancedQuery();

        if (!ObjectUtils.isBlank(advancedQuery)) {
            query.and(advancedQuery);
        }

        for (String filter : getGlobalFilters().values()) {
            if (filter != null) {
                query.and("* matches ?", filter);
            }
        }

        if (selectedType != null) {
            for (Map.Entry<String, Map<String, String>> entry : getFieldFilters().entrySet()) {
                Map<String, String> value = entry.getValue();

                if (value == null) {
                    continue;
                }

                String fieldName = selectedType.getInternalName() + "/" + entry.getKey();

                if (ObjectUtils.to(boolean.class, value.get("m"))) {
                    query.and(fieldName + " = missing");

                } else {
                    String fieldValue = value.get("");

                    if (fieldValue == null) {
                        continue;
                    }

                    String queryType = value.get("t");

                    if ("d".equals(queryType)) {
                        Date start = ObjectUtils.to(Date.class, fieldValue);
                        Date end = ObjectUtils.to(Date.class, value.get("x"));

                        if (start != null) {
                            query.and(fieldName + " >= ?", start);
                        }

                        if (end != null) {
                            query.and(fieldName + " <= ?", end);
                        }

                    } else if ("n".equals(queryType)) {
                        Double minimum = ObjectUtils.to(Double.class, fieldValue);
                        Double maximum = ObjectUtils.to(Double.class, value.get("x"));

                        if (minimum != null) {
                            query.and(fieldName + " >= ?", fieldValue);
                        }

                        if (maximum != null) {
                            query.and(fieldName + " <= ?", maximum);
                        }

                    } else if ("t".equals(queryType)) {
                        query.and(fieldName + " matches ?", fieldValue);

                    } else {
                        query.and(fieldName + " = ?", fieldValue);
                    }
                }
            }
        }

        if (site != null &&
                !site.isAllSitesAccessible()) {
            Set<ObjectType> globalTypes = new HashSet<ObjectType>();

            if (selectedType != null) {
                addGlobalTypes(globalTypes, selectedType);

            } else {
                for (ObjectType type : validTypes) {
                    addGlobalTypes(globalTypes, type);
                }
            }

            query.and(CompoundPredicate.combine(
                    PredicateParser.OR_OPERATOR,
                    site.itemsPredicate(),
                    PredicateParser.Static.parse("_type = ?", globalTypes)));
        }

        if (selectedType == null &&
                isAllSearchable) {
            Set<String> comparisonKeys = new HashSet<String>();
            DatabaseEnvironment environment = Database.Static.getDefault().getEnvironment();

            if (!isReturnQuery()) {
                addVisibilityFields(comparisonKeys, environment);

                for (ObjectType type : environment.getTypes()) {
                    addVisibilityFields(comparisonKeys, type);
                }
            }

            for (String key : comparisonKeys) {
                if (isShowDrafts()) {
                    query.and(key + " = missing or " + key + " = true");

                } else {
                    query.and(key + " = missing");
                }
            }

        } else if (isShowDrafts()) {
            Set<String> comparisonKeys = new HashSet<String>();
            DatabaseEnvironment environment = Database.Static.getDefault().getEnvironment();

            addVisibilityFields(comparisonKeys, environment);
            addVisibilityFields(comparisonKeys, selectedType);

            for (String key : comparisonKeys) {
                query.and(key + " = missing or " + key + " = true");
            }
        }

        query.and("_type != ?", ObjectType.getInstance(Draft.class));

        String color = getColor();

        if (color != null && color.startsWith("#")) {
            int[] husl = HuslColorSpace.Static.toHUSL(new Color(Integer.parseInt(color.substring(1), 16)));
            int normalized0 = husl[0];
            int normalized1 = husl[1];
            int normalized2 = husl[2];

            if (normalized1 < 30 || normalized2 < 15 || normalized2 > 85) {
                normalized0 = 0;
                normalized1 = 0;
            }

            int[] normalized = new int[] {
                    (((int) Math.round(normalized0 / 24.0)) * 24) % 360,
                    ((int) Math.round(normalized1 / 20.0)) * 20,
                    ((int) Math.round(normalized2 / 20.0)) * 20 };

            String fieldName =
                    "color.distribution/n_" + normalized[0] +
                    "_" + normalized[1] +
                    "_" + normalized[2];

            query.and(fieldName + " > 0");
            query.sortDescending(fieldName);

            List<Sorter> sorters = query.getSorters();

            sorters.add(0, sorters.get(sorters.size() - 1));
        }

        return query;
    }

    private void addGlobalTypes(Set<ObjectType> globalTypes, ObjectType type) {
        if (type != null && type.getGroups().contains(ObjectType.class.getName())) {
            globalTypes.add(type);
        }
    }

    private void addVisibilityFields(Set<String> comparisonKeys, ObjectStruct struct) {
        for (ObjectIndex index : struct.getIndexes()) {
            if (index.isVisibility()) {
                for (String fieldName : index.getFields()) {
                    ObjectField field = struct.getField(fieldName);

                    if (field != null) {
                        comparisonKeys.add(field.getUniqueName());
                    }
                }
            }
        }
    }

    /** @deprecated Use {@link #toQuery(Site)} instead. */
    @Deprecated
    public Query<?> toQuery() {
        return toQuery(null);
    }

    // --- Deprecated ---

    /** @deprecated Use {@link #ADDITIONA_PREDICATE_PARAMETER} instead. */
    @Deprecated
    public static final String ADDITIONAL_QUERY_PARAMETER = ADDITIONAL_PREDICATE_PARAMETER;

    /** @deprecated Use {@link #ONLY_PATHED_PARAMETER} instead. */
    @Deprecated
    public static final String IS_ONLY_PATHED = ONLY_PATHED_PARAMETER;

    /** @deprecated Use {@link #TYPES_PARAMETER} instead. */
    @Deprecated
    public static final String REQUESTED_TYPES_PARAMETER = TYPES_PARAMETER;

    /** @deprecated Use {@link #Search(ToolPageContext, Collection)} instead. */
    @Deprecated
    public Search(ToolPageContext page, UUID... typeIds) {
        this(page, typeIds != null ? Arrays.asList(typeIds) : null);
    }

    /** @deprecated Use {@link #getTypes} instead. */
    @Deprecated
    public Set<ObjectType> getRequestedTypes() {
        return getTypes();
    }

    /** @deprecated Use {@link #findValidTypes} instead. */
    public Set<ObjectType> getValidTypes() {
        return findValidTypes();
    }

    /** @deprecated Use {@link #toQuery} instead. */
    @Deprecated
    public Query<?> getQuery() {
        return toQuery();
    }

    @Deprecated
    private transient ToolPageContext page;

    @Deprecated
    private transient PaginatedResult<?> result;

    /** @deprecated Use {@link #toQuery} instead. */
    @Deprecated
    public PaginatedResult<?> getResult() {
        if (result == null) {
            result = toQuery(page != null ? page.getSite() : null).select(getOffset(), getLimit());
        }
        return result;
    }
}
