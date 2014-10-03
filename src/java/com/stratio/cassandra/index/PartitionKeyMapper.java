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

import java.nio.ByteBuffer;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.composites.CellName;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import com.stratio.cassandra.index.util.ByteBufferUtils;

/**
 * Class for several partition key mappings between Cassandra and Lucene.
 * 
 * @author Andres de la Pena <adelapena@stratio.com>
 * 
 */
public class PartitionKeyMapper
{

    /** The Lucene's field name. */
    public static final String FIELD_NAME = "_partition_key";

    /** The active active partition key. */
    private final IPartitioner<?> partitioner;

    /**
     * Returns a new {@code PartitionKeyMapper} according to the specified column family meta data.
     */
    private PartitionKeyMapper()
    {
        partitioner = DatabaseDescriptor.getPartitioner();
    }

    /**
     * Returns a new {@code PartitionKeyMapper} according to the specified column family meta data.
     *
     * @return a new {@code PartitionKeyMapper} according to the specified column family meta data.
     */
    public static PartitionKeyMapper instance()
    {
        return new PartitionKeyMapper();
    }

    /**
     * Adds to the specified {@link Document} the {@link Field}s associated to the specified raw partition key.
     * 
     * @param document
     *            The document in which the fields are going to be added.
     * @param partitionKey
     *            The raw partition key to be converted.
     */
    public void addFields(Document document, DecoratedKey partitionKey)
    {
        String serializedKey = ByteBufferUtils.toString(partitionKey.getKey());
        Field field = new StringField(FIELD_NAME, serializedKey, Store.YES);
        document.add(field);
    }

    /**
     * Returns the specified raw partition key as a Lucene's {@link Term}.
     * 
     * @param partitionKey
     *            The raw partition key to be converted.
     * @return The specified raw partition key as a Lucene's {@link Term}.
     */
    public Term term(DecoratedKey partitionKey)
    {
        String serializedKey = ByteBufferUtils.toString(partitionKey.getKey());
        return new Term(FIELD_NAME, serializedKey);
    }

    /**
     * Returns the specified raw partition key as a Lucene's {@link Query}.
     * 
     * @param partitionKey
     *            The raw partition key to be converted.
     * @return The specified raw partition key as a Lucene's {@link Query}.
     */
    public Query query(DecoratedKey partitionKey)
    {
        return new TermQuery(term(partitionKey));
    }

    /**
     * Returns the {@link DecoratedKey} contained in the specified Lucene's {@link Document}.
     * 
     * @param document
     *            the {@link Document} containing the partition key to be get.
     * @return The {@link DecoratedKey} contained in the specified Lucene's {@link Document}.
     */
    public DecoratedKey decoratedKey(Document document)
    {
        String string = document.get(FIELD_NAME);
        ByteBuffer partitionKey = ByteBufferUtils.fromString(string);
        return decoratedKey(partitionKey);
    }

    /**
     * Returns the specified raw partition key as a a {@link DecoratedKey}.
     * 
     * @param partitionKey
     *            The raw partition key to be converted.
     * @return The specified raw partition key as a a {@link DecoratedKey}.
     */
    public DecoratedKey decoratedKey(ByteBuffer partitionKey)
    {
        return partitioner.decorateKey(partitionKey);
    }

}
