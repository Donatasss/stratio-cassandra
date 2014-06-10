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

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.search.Query;
import org.junit.Assert;
import org.junit.Test;

import com.stratio.cassandra.index.query.FuzzyCondition;
import com.stratio.cassandra.index.schema.CellMapper;
import com.stratio.cassandra.index.schema.CellMapperBoolean;
import com.stratio.cassandra.index.schema.Schema;

public class FuzzyConditionTest
{

    @Test
    public void testFuzzyQuery()
    {

        Map<String, CellMapper<?>> map = new HashMap<>();
        map.put("name", new CellMapperBoolean());
        Schema mappers = new Schema(EnglishAnalyzer.class.getName(), map);

        FuzzyCondition fuzzyCondition = new FuzzyCondition(0.5f, "name", "tr", 1, 2, 49, true);
        Query query = fuzzyCondition.query(mappers);

        Assert.assertNotNull(query);
        Assert.assertEquals(org.apache.lucene.search.FuzzyQuery.class, query.getClass());
        org.apache.lucene.search.FuzzyQuery luceneQuery = (org.apache.lucene.search.FuzzyQuery) query;
        Assert.assertEquals("name", luceneQuery.getField());
        Assert.assertEquals("tr", luceneQuery.getTerm().text());
        Assert.assertEquals(1, luceneQuery.getMaxEdits());
        Assert.assertEquals(2, luceneQuery.getPrefixLength());
        Assert.assertEquals(0.5f, query.getBoost(), 0);
    }

}
