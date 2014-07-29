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
package com.stratio.cassandra.index;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.stratio.cassandra.index.query.Sorting;
import com.stratio.cassandra.index.query.SortingField;
import com.stratio.cassandra.index.schema.Cells;
import com.stratio.cassandra.index.schema.Schema;
import com.stratio.cassandra.index.util.ComparatorChain;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Row;

/**
 * A {@link Comparator} for comparing {@link Row}s according to a certain {@link Sorting}.
 *
 * @author Andres de la Pena <adelapena@stratio.com>
 */
public class RowsComparatorSorting implements RowsComparator
{
    private final CFMetaData metadata;
    private final Schema schema;
    private final ComparatorChain<Cells> comparatorChain;

    private final Map<DecoratedKey, Cells> cellsCache;

    /**
     * @param metadata
     *            The {@link CFMetaData} of the column family of the {@link Row}s to be compared.
     * @param schema
     *            The indexing {@link Schema} of the {@link Row}s to be compared.
     * @param sorting
     *            The {@link Sorting} inf which the {@link Row} comparison is based.
     */
    public RowsComparatorSorting(CFMetaData metadata, Schema schema, Sorting sorting)
    {
        this.metadata = metadata;
        this.schema = schema;
        comparatorChain = new ComparatorChain<>();
        for (SortingField sortingField : sorting.getSortingFields())
        {
            Comparator<Cells> comparator = sortingField.comparator();
            comparatorChain.addComparator(comparator);
        }
        cellsCache = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     *
     * @param row1 A {@link Row}.
     * @param row2 A {@link Row}.
     * @return A negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater
     *         than the second according to a {@link Sorting}.
     */
    @Override
    public int compare(Row row1, Row row2)
    {
        Cells cells1 = cellsCache.get(row1.key);
        if (cells1 == null) {
            cells1 = schema.cells(metadata, row1);
            cellsCache.put(row1.key, cells1);
        }
        Cells cells2 = cellsCache.get(row2.key);
        if (cells2 == null) {
            cells2 = schema.cells(metadata, row2);
            cellsCache.put(row2.key, cells2);
        }
        return comparatorChain.compare(cells1, cells2);
    }
}
