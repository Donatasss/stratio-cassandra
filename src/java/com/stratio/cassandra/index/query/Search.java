/*
 * Copyright 2014, Stratio.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratio.cassandra.index.query;

import org.apache.lucene.queries.ChainedFilter;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.eclipse.jdt.core.dom.ReturnStatement;

import com.stratio.cassandra.index.schema.Schema;
import com.stratio.cassandra.index.util.JsonSerializer;
import com.stratio.cassandra.index.util.Log;

/**
 * 
 * Class representing an Lucene's index search. It is formed by an optional querying {@link Condition} and an optional
 * filtering {@link Condition}. It can be translated to a Lucene's {@link Query} using a {@link Schema}.
 * 
 * @author Andres de la Pena <adelapena@stratio.com>
 * 
 */
public class Search
{

    private static final boolean DEFAULT_PARALLEL = false;

    /** The querying condition */
    private final Condition queryCondition;

    /** The filtering condition */
    private final Condition filterCondition;

    private final Sorting sorting;

    private final Boolean parallel;

    /**
     * Returns a new {@link Search} composed by the specified querying and filtering conditions.
     * 
     * @param queryCondition
     *            The {@link Condition} for querying, maybe {@code null} meaning no querying.
     * @param filterCondition
     *            The {@link Condition} for filtering, maybe {@code null} meaning no filtering.
     * @param sorting
     *            The {@link Sorting} for the query. Note that is the order in which the data will be read before
     *            querying, not the order of the results after querying.
     */
    @JsonCreator
    public Search(@JsonProperty("query") Condition queryCondition,
                  @JsonProperty("filter") Condition filterCondition,
                  @JsonProperty("sort") Sorting sorting,
                  @JsonProperty("parallel") Boolean parallel)
    {
        this.queryCondition = queryCondition;
        this.filterCondition = filterCondition;
        this.sorting = sorting;
        this.parallel = parallel == null ? DEFAULT_PARALLEL : parallel;
    }

    /**
     * Returns {@code true} if the results must be ordered by relevance. If {@code false}, then the results are sorted
     * by the natural Cassandra's order. Results must be ordered by relevance if the querying condition is not {code
     * null}.
     * 
     * Relevance is used when the query condition is set, and it is not used when only the filter condition is set.
     * 
     * @return {@code true} if the results must be ordered by relevance. If {@code false}, then the results must be
     *         sorted by the natural Cassandra's order.
     */
    public boolean usesRelevanceOrSorting()
    {
        return queryCondition != null || sorting != null;
    }

    public boolean usesRelevance()
    {
        return queryCondition != null;
    }

    public boolean usesSorting()
    {
        return sorting != null;
    }

    /**
     * Returns the {@link Condition} for querying. Maybe {@code null} meaning no querying.
     * 
     * @return The {@link Condition} for querying. Maybe {@code null} meaning no querying.
     */
    public Condition queryCondition()
    {
        return queryCondition;
    }

    /**
     * Returns the {@link Condition} for filtering. Maybe {@code null} meaning no filtering.
     * 
     * @return The {@link Condition} for filtering. Maybe {@code null} meaning no filtering.
     */
    public Condition filterCondition()
    {
        return filterCondition;
    }

    /**
     * Returns the {@link Sorting}. Maybe {@code null} meaning no sorting.
     * 
     * @return The {@link Sorting}. Maybe {@code null} meaning no sorting.
     */
    public Sorting getSorting()
    {
        return sorting;
    }

    /**
     * Returns the Lucene's {@link Query} represented by this querying {@link Condition} using the specified
     * {@link Schema}. Maybe {@code null} meaning no querying.
     * 
     * @param schema
     *            A {@link Schema}.
     * @return The Lucene's {@link Query} represented by this querying {@link Condition} using {@code schema}.
     */
    public Query query(Schema schema)
    {
        return queryCondition == null ? null : queryCondition.query(schema);
    }

    /**
     * Returns the Lucene's {@link Filter} represented by this filtering {@link Condition} using the specified
     * {@link Schema}. Maybe {@code null} meaning no filtering.
     * 
     * @param schema
     *            A {@link Schema}.
     * @return The Lucene's {@link Filter} represented by this filtering {@link Condition} using {@code schema}.
     */
    public Filter filter(Schema schema)
    {
        return filterCondition == null ? null : filterCondition.filter(schema);
    }

    /**
     * Returns the Lucene's {@link Sort} represented by this {@link Sorting} using the specified {@link Schema}. Maybe
     * {@code null} meaning no sorting.
     * 
     * @param schema
     *            A {@link Schema}.
     * @return The Lucene's {@link Sort} represented by this {@link Sorting} using {@code schema}.
     */
    public Sort sort(Schema schema)
    {
        return sorting == null ? null : sorting.sort(schema);
    }

    /**
     * Returns the Lucene's {@link Filter} represented by this filtering {@link Condition} combined with the specified
     * range {@link Filter} using the specified {@link Schema}. Maybe {@code null} meaning no filtering.
     * 
     * @param schema
     *            A {@link Schema}.
     * @param rangeFilter
     *            An additional {@link Filter} to be used.
     * @return The Lucene's {@link Sort} represented by this {@link Sorting} combined with {@code rangeFilter} using
     *         {@code schema}.
     */
    public Filter filter(Schema schema, Filter rangeFilter)
    {
        Filter filter = filter(schema);
        if (filter == null && rangeFilter == null)
        {
            return null;
        }
        else if (filter != null && rangeFilter == null)
        {
            return filter;
        }
        else if (filter == null && rangeFilter != null)
        {
            return rangeFilter;
        }
        else
        {
            Filter[] filters = new Filter[] { filter, rangeFilter };
            return new ChainedFilter(filters, ChainedFilter.AND);
        }
    }

    /**
     * Returns the Lucene's {@link Query} representation of this search. This {@link Query} include both the querying
     * and filtering {@link Condition}s. If none of them is set, then a {@link MatchAllDocsQuery} is returned, so it
     * never {@link ReturnStatement} {@code null}.
     * 
     * @param schema
     *            The {@link Schema} to be used.
     * @param rangeFilter
     *            An additional {@link Filter} to be used.
     * @return The Lucene's {@link Query} representation of this search.
     */
    public Query filteredQuery(Schema schema, Filter rangeFilter)
    {
        Query query = query(schema);
        Filter filter = filter(schema, rangeFilter);

        if (query == null && filter == null)
        {
            return new MatchAllDocsQuery();
        }
        else if (query != null && filter == null)
        {
            return query;
        }
        else if (query == null && filter != null)
        {
            return new ConstantScoreQuery(filter);
        }
        else
        {
            return new FilteredQuery(query, filter);
        }
    }

    /**
     * Returns {@code true} if this search must be performed in a parallel fashion, {@code false} otherwise. Note that
     * this only is applicable for relevance or sorting searches.
     * 
     * @return {@code true} if this search must be performed in a parallel fashion, {@code false} otherwise.
     */
    public Boolean isParallel()
    {
        return parallel;
    }

    /**
     * Returns a new {@link Search} from the specified JSON {@code String}.
     * 
     * @param json
     *            A JSON {@code String} representing a {@link Search}.
     * @return The {@link Search} represented by the specified JSON {@code String}.
     */
    public static Search fromJson(String json)
    {
        try
        {
            return JsonSerializer.fromString(json, Search.class);
        }
        catch (Exception e)
        {
            String message = String.format("Unparseable JSON index expression: %s", e.getMessage());
            Log.error(e, message);
            throw new IllegalArgumentException(message, e);
        }
    }

    /**
     * Validates this {@link Search} against the specified {@link Schema}.
     * 
     * @param schema
     *            A {@link Schema}.
     */
    public void validate(Schema schema)
    {
        if (queryCondition != null)
        {
            queryCondition.query(schema);
        }
        if (filterCondition != null)
        {
            filterCondition.filter(schema);
        }
        if (sorting != null)
        {
            sorting.sort(schema);
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Search [queryCondition=");
        builder.append(queryCondition);
        builder.append(", filterCondition=");
        builder.append(filterCondition);
        builder.append(", sorting=");
        builder.append(sorting);
        builder.append("]");
        return builder.toString();
    }

}
