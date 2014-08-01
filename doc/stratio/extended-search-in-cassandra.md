---
title: Extended Search in Cassandra
---

[Cassandra](http://cassandra.apache.org/ "Apache Cassandra project") index functionality has been extended to provide near
real time search such as [ElasticSearch](http://www.elasticsearch.org/ "ElasticSearch project")
or [Solr](https://lucene.apache.org/solr/ "Apache Solr project"), including full text search capabilities and free
multivariable search. It is achieved through a Lucene based implementation of Cassandra secondary indexes, where each
node of the cluster indexes its own data.

Table of Contents
=================

-   [Overview](#overview)
-   [Index creation](#index-creation)
-   [Queries](#queries)
    -   [Boolean](#boolean-query)
    -   [Fuzzy](#fuzzy-query)
    -   [Match](#match-query)
    -   [Phrase](#phrase-query)
    -   [Prefix](#prefix-query)
    -   [Range](#range-query)
    -   [Regexp](#regexp-query)
    -   [Wildcard](#wildcard-query)
-   [Other interesting queries](#other-interesting-queries)
    -   [Token Function](#token-function)
    -   [Server Side Filtering](#server-side-filtering)
-   [Datatypes Mapping](#datatypes-mapping)
    -   [CQL to Field type](#cql-to-field-type)
    -   [Field type to CQL](#field-type-to-cql)

Overview
========

Lucene search technology integration into Cassandra provides:

-   Big data full text search
-   Relevance scoring and sorting
-   General top-k queries
-   Complex boolean queries (and, or, not)
-   Near real-time search
-   CQL3 support
-   Wide rows support
-   Partition and cluster composite keys support
-   Support for indexing columns part of primary key
-   Stratio Deep Hadoop support compatibility
-   Self contained distribution

Not yet supported:

-   Thrift API
-   Legacy compact storage option
-   Type "counter"
-   Columns with TTL

Index Creation
==============

Syntax
------

~~~~ {.prettyprint .lang-meta}
CREATE CUSTOM INDEX (IF NOT EXISTS)? &lt;index_name>
                                  ON &lt;table_name> ( &lt;magic_column> )
                               USING 'org.apache.cassandra.db.index.stratio.RowIndex'
                        WITH OPTIONS = &lt;options>
~~~~

where:

-   &lt;magic_column> is the name of a text column that does not contain any data and will be used to show the scoring for each resulting row of a query,
-   &lt;options> is a JSON object:

~~~~ {.prettyprint .lang-meta}
&lt;options> := { ('refresh_seconds'    : '&lt;int_value>',)?
               ('num_cached_filters' : '&lt;int_value>',)?
               ('ram_buffer_mb'      : '&lt;int_value>',)?
               ('max_merge_mb'       : '&lt;int_value>',)?
               ('max_cached_mb'      : '&lt;int_value>',)?
               'schema'              : '&lt;schema_definition>'};
~~~~

Options, except “schema”, take a positive integer value enclosed in single quotes:

-   **refresh_seconds**: number of seconds before refreshing the index (between writers and readers). Defaults to ’60′.
-   **num_cached_filters**: should be equal or greater than the number of vnodes plus 1 per node. It uses 1 bit per indexed row. A value of ’0′ means no cache. Defaults to ’0′.
-   **ram_buffer_mb**: size of the write buffer. Its content will be committed to disk when full. Defaults to ’64′.
-   **max_merge_mb**: defaults to ’5′.
-   **max_cached_mb**: defaults to ’30′.
-   **schema**: see below

~~~~ {.prettyprint .lang-meta}
&lt;schema_definition> := {
    (default_analyzer : "&lt;analyzer_class_name>",)?
    fields : { &lt;field_definition> (, &lt;field_definition>)* }
}
~~~~

Where default_analyzer defaults to ‘org.apache.lucene.analysis.standard.StandardAnalyzer’.

~~~~ {.prettyprint .lang-meta}
&lt;field_definition> := {
    type : "&lt;field_type>" (, &lt;option> : "&lt;value>")*
}
~~~~

Field definition options depend on the field type. Details and default values are listed in the table below.

<table>
    <thead>
    <tr>
        <th>Field type</th>
        <th>Option</th>
        <th>Value type</th>
        <th>Default value</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td>bigdec</td>
        <td>integer_digits</td>
        <td>positive integer</td>
        <td>32</td>
    </tr>
    <tr>
        <td></td>
        <td>decimal_digits</td>
        <td>positive integer</td>
        <td>32</td>
    </tr>
    <tr>
        <td>bigint</td>
        <td>digits</td>
        <td>positive integer</td>
        <td>32</td>
    </tr>
    <tr>
        <td>date</td>
        <td>pattern</td>
        <td>date format (string)</td>
        <td>yyyy/MM/dd HH:mm:ss.SSS</td>
    </tr>
    <tr>
        <td>double, float, integer, long</td>
        <td>boost</td>
        <td>float</td>
        <td>0.1f</td>
    </tr>
    <tr>
        <td>text</td>
        <td>analyzer</td>
        <td>class name (string)</td>
        <td>default_analyzer of the schema</td>
    </tr>
    </tbody>
</table>

Note that Cassandra allows one custom index per table. On the other hand, Cassandra does not allow a modify operation on indexes. To modify an index it needs to be deleted first and created again.

Example
-------

This code below and the one for creating the corresponding keyspace and table is available in a CQL script that can be sourced from the Cassandra shell: [test-users-create.cql](http://docs.openstratio.org/resources/cql-scripts/test-users-create.cql "Download CQL script for creating keyspace, table and index").

~~~~ {.prettyprint .lang-meta}
CREATE CUSTOM INDEX IF NOT EXISTS users_index
ON test.users (stratio_col)
USING 'org.apache.cassandra.db.index.stratio.RowIndex'
WITH OPTIONS = {
    'refresh_seconds'    : '1',
    'num_cached_filters' : '1',
    'ram_buffer_mb'      : '64',
    'max_merge_mb'       : '5',
    'max_cached_mb'      : '30',
    'schema' : '{
        default_analyzer : "org.apache.lucene.analysis.standard.StandardAnalyzer",
        fields : {
            name   : {type     : "string"},
            gender : {type     : "string"},
            animal : {type     : "string"},
            age    : {type     : "integer"},
            food   : {type     : "string"},
            number : {type     : "integer"},
            bool   : {type     : "boolean"},
            date   : {type     : "date",
                      pattern  : "yyyy/MM/dd"},
            mapz   : {type     : "string"},
            setz   : {type     : "string"},
            listz  : {type     : "string"},
            phrase : {type     : "text",
                      analyzer : "org.apache.lucene.analysis.es.SpanishAnalyzer"}
        }
    }'
};
~~~~

Queries
=======

Syntax:

~~~~ {.prettyprint .lang-meta}
SELECT ( &lt;fields> | * )
FROM &lt;table_name>
WHERE &lt;magic_column> = '{ (   query  : &lt;query>  )?
                          ( , filter : &lt;filter> )?
                          ( , sort   : &lt;sort>   )?
                        }';
~~~~

where &lt;query\> and &lt;filter\> are a JSON object:

~~~~ {.prettyprint .lang-meta}
&lt;query> := { type : &lt;type> (, &lt;option> : ( &lt;value> | &lt;value_list> ) )+ }
~~~~

and &lt;sort\> is another JSON object:

~~~~ {prettyprint lang-meta}
    &lt;sort> := { fields : &lt;sort_field> (, &lt;sort_field> )* }
    &lt;sort_field> := { field : &lt;field> (, reverse : &lt;reverse> )? }
~~~~

When searching by &lt;query\>, results are returned ***sorted by descending relevance*** without pagination. The results will be located in the column ‘stratio_relevance’.

Filter types and options are the same as the query ones. The difference with queries is that filters have no effect on scoring.

Sort option is used to specify the order in which the indexed rows will be traversed. When sorting is used, the query scoring is delayed.

If no query or sorting options are specified then the results are returned in the Cassandra’s natural order, which is defined by the partitioner and the column name comparator.

Types of query and their options are summarized in the table below. Details for each of them are available in individual sections and the examples can be downloaded as a CQL script: [extended-search-examples.cql](http://docs.openstratio.org/resources/cql-scripts/extended-search-examples.cql "Download CQL script of examples").

In addition to the options described in the table, all query types have a “**boost**” option that acts as a weight on the resulting score.

<table>
<col width="33%" />
<col width="33%" />
<col width="33%" />
<thead>
<tr class="header">
<th align="left">Query type</th>
<th align="left">Supported Field type</th>
<th align="left">Options</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td align="left"><a href="#boolean-query" title="Boolean query details">Boolean</a></td>
<td align="left">subqueries</td>
<td align="left"><ul>
<li><strong>must</strong>: a list of conditions.</li>
<li><strong>should</strong>: a list of conditions.</li>
<li><strong>not</strong>: a list of conditions.</li>
</ul></td>
</tr>
<tr class="even">
<td align="left"><a href="#fuzzy-query">Fuzzy</a></td>
<td align="left">bytes<br /> inet<br /> string<br /> text</td>
<td align="left"><ul>
<li><strong>field</strong>: the field name.</li>
<li><strong>value</strong>: the field value.</li>
<li><strong>max_edits</strong> (default = 2): a integer value between 0 and 2 (the <a href="http://en.wikipedia.org/wiki/Levenshtein_automaton" title="Wikipedia article on Levenshtein Automaton">Levenshtein automaton</a> maximum supported distance).</li>
<li><strong>prefix_length</strong> (default = 0): integer representing the length of common non-fuzzy prefix.</li>
<li><strong>max_expansions</strong> (default = 50): an integer for the maximum number of terms to match.</li>
<li><strong>transpositions</strong> (default = true): if transpositions should be treated as a primitive edit operation (<a href="http://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance" title="Wikipedia article on Damerau-Levenshtein Distance">Damerau-Levenshtein distance</a>). When false, comparisons will implement the classic <a href="http://en.wikipedia.org/wiki/Levenshtein_distance" title="Wikipedia article on Levenshtein Distance">Levenshtein distance</a>.</li>
</ul></td>
</tr>
<tr class="odd">
<td align="left"><a href="#match-query">Match</a></td>
<td align="left">All</td>
<td align="left"><ul>
<li><strong>field</strong>: the field name.</li>
<li><strong>value</strong>: the field value.</li>
</ul></td>
</tr>
<tr class="even">
<td align="left"><a href="#phrase-query">Phrase</a></td>
<td align="left">bytes<br /> inet<br /> text</td>
<td align="left"><ul>
<li><strong>field</strong>: the field name.</li>
<li><strong>values</strong>: list of values.</li>
<li><strong>slop</strong> (default = 0): number of other words permitted between words.</li>
</ul></td>
</tr>
<tr class="odd">
<td align="left"><a href="#prefix-query">Prefix</a></td>
<td align="left">bytes<br /> inet<br /> string<br /> text</td>
<td align="left"><ul>
<li><strong>field</strong>: fieldname.</li>
<li><strong>value</strong>: fieldvalue.</li>
</ul></td>
</tr>
<tr class="even">
<td align="left"><a href="#range-query">Range</a></td>
<td align="left">All</td>
<td align="left"><ul>
<li><strong>field</strong>: field name.</li>
<li><strong>lower</strong> (default = $-\infty$ for number): lower bound of the range.</li>
<li><strong>include_lower</strong> (default = false): if the left value is included in the results (&gt;=)</li>
<li><strong>upper</strong> (default = $+\infty$ for number): upper bound of the range.</li>
<li><strong>include_upper</strong> (default = false): if the right value is included in the results (&lt;=).</li>
</ul></td>
</tr>
<tr class="odd">
<td align="left"><a href="#regexp-query">Regexp</a></td>
<td align="left">bytes<br /> inet<br /> string<br /> text</td>
<td align="left"><ul>
<li><strong>field</strong>: fieldname.</li>
<li><strong>value</strong>: regular expression.</li>
</ul></td>
</tr>
<tr class="even">
<td align="left"><a href="#wildcard-query">Wildcard</a></td>
<td align="left">bytes<br /> inet<br /> string<br /> text</td>
<td align="left"><ul>
<li><strong>field</strong>: field name.</li>
<li><strong>value</strong>: wildcard expression.</li>
</ul></td>
</tr>
</tbody>
</table>

Boolean query
-------------

Syntax:

~~~~ {.prettyprint .lang-meta}
SELECT ( &lt;fields> | * )
FROM &lt;table>
WHERE &lt;magic_column> = '{ query : {
                           type : "boolean",
                           ( not: &lt;query_list> , )?
                           ( must | should ) : &lt;query_list> }}';
~~~~

where:

-   **must**: returns the conjunction of queries: $(q_1 \\land q_2 \\land … \\land q_n)$
-   **should**: returns the disjunction of queries: $(q_1 \\lor q_2 \\lor … \\lor q_n)$
-   **not**: returns the negation of the disjunction of queries: $\\lnot(q_1 \\lor q_2 \\lor … \\lor q_n)$.

Since "not" will be applied to the results of a "must" or "should" condition, it can not be used in isolation.

Example 1: will return rows where name ends with “a” AND food starts with “tu”

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type : "boolean",
                        must : [{type : "wildcard", field : "name", value : "*a"},
                                {type : "wildcard", field : "food", value : "tu*"}]}}';
~~~~

Example 2: will return rows where food starts with “tu” but name does not end with “a”

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type : "boolean",
                        not  : [{type : "wildcard", field : "name", value : "*a"}],
                        must : [{type : "wildcard", field : "food", value : "tu*"}]}}';
~~~~

Example 3: will return rows where name ends with “a” or food starts with “tu”

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type   : "boolean",
                        should : [{type : "wildcard", field : "name", value : "*a"},
                                  {type : "wildcard", field : "food", value : "tu*"}]}}';
~~~~

Fuzzy query
-----------

Syntax:

~~~~ {.prettyprint .lang-meta}
SELECT ( &lt;fields> | * )
FROM &lt;table>
WHERE &lt;magic_column> = '{ query : {
                            type  : "fuzzy",
                            field : &lt;fieldname> ,
                            value : &lt;value>
                            (, max_edits     : &lt;max_edits> )?
                            (, prefix_length : &lt;prefix_length> )?
                            (, max_expansions: &lt;max_expansion> )?
                            (, transpositions: &lt;transposition> )?
                          }}';
~~~~

where:

-   **max_edits** (default = 2): a integer value between 0 and 2. Will return rows which distance from &lt;value\> to &lt;field\> content has a distance of at most &lt;max_edits\>. Distance will be interpreted according to the value of “transpositions”.
-   **prefix_length** (default = 0): an integer value being the length of the common non-fuzzy prefix
-   **max_expansions** (default = 50): an integer for the maximum number of terms to match
-   **transpositions** (default = true): if transpositions should be treated as a primitive edit operation ([Damerau-Levenshtein distance](http://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance "Wikipedia article on Damerau-Levenshtein Distance")). When false, comparisons will implement the classic [Levenshtein distance](http://en.wikipedia.org/wiki/Levenshtein_distance "Wikipedia article on Levenshtein Distance").

Example 1: will return any rows where “phrase” contains a word that differs in one edit operation from “puma”, such as “pumas”.

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : { type      : "fuzzy",
                                field     : "phrase",
                                value     : "puma",
                                max_edits : 1 }}';
~~~~

Example 2: same as example 1 but will limit the results to rows where phrase contains a word that starts with “pu”.

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : { type          : "fuzzy",
                                field         : "phrase",
                                value         : "puma",
                                max_edits     : 1,
                                prefix_length : 2 }}';
~~~~

Match query
-----------

Syntax:

~~~~ {.prettyprint .lang-meta}
SELECT ( &lt;fields> | * )
FROM &lt;table>
WHERE &lt;magic_column> = '{ query : {
                            type  : "match",
                            field : &lt;fieldname> ,
                            value : &lt;value> }}';
~~~~

Example 1: will return rows where name matches “Alicia”

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type  : "match",
                        field : "name",
                        value : "Alicia" }}';
~~~~

Example 2: will return rows where phrase contains “mancha”

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type  : "match",
                        field : "phrase",
                        value : "mancha" }}';
~~~~

Example 3: will return rows where date matches “2014/01/01″

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type  : "match",
                        field : "date",
                        value : "2014/01/01" }}';
~~~~

Phrase query
------------

Syntax:

~~~~ {.prettyprint .lang-meta}
SELECT ( &lt;fields> | * )
FROM &lt;table>
WHERE &lt;magic_column> = '{ query : {
                            type  :"phrase",
                            field : &lt;fieldname> ,
                            values : &lt;value_list>
                            (, slop : &lt;slop> )?
                        }}';
~~~~

where:

-   **values**: an ordered list of values.
-   **slop** (default = 0): number of words permitted between words.

Example 1: will return rows where “phrase” contains the word “camisa” followed by the word “manchada”.

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type   : "phrase",
                        field  : "phrase",
                        values : ["camisa", "manchada"] }}';
~~~~

Example 2: will return rows where “phrase” contains the word “mancha” followed by the word “camisa” having 0 to 2 words in between.

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type   : "phrase",
                        field  : "phrase",
                        values : ["mancha", "camisa"],
                        slop   : 2 }}';
~~~~

Prefix query
------------

Syntax:

~~~~ {.prettyprint .lang-meta}
SELECT ( &lt;fields> | * )
FROM &lt;table>
WHERE &lt;magic_column> = '{ query : {
                            type  : "prefix",
                            field : &lt;fieldname> ,
                            value : &lt;value> }}';
~~~~

Example: will return rows where “phrase” contains a word starting with “lu”. If the column is indexed as “text” and uses an analyzer, words ignored by the analyzer will not be retrieved.

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type          : "prefix",
                        field         : "phrase",
                        value         : "lu" }}';
~~~~

Range query
-----------

Syntax:

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type    : "range",
                        field   : &lt;fieldname>
                        (, lower : &lt;min> , include_lower : &lt;min_included> )?
                        (, upper : &lt;max> , include_upper : &lt;max_included> )?
                     }}';
~~~~

where:

-   **lower**: lower bound of the range.
-   **include_lower** (default = false): if the lower bound is included (left-closed range).
-   **upper**: upper bound of the range.
-   **include_upper** (default = false): if the upper bound is included (right-closed range).

Lower and upper will default to $-/+\\infty$ for number. In the case of byte and string like 
data (bytes, inet, string, text), all values from lower up to upper will be returned if both 
are specified. If only “lower” is specified, all rows with values from “lower” will be returned. 
If only “upper” is specified then all rows with field values up to “upper” will be returned. If 
both are omitted than all rows will be returned.

Example 1: will return rows where $age \\in [1,+\\infty)$

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type          : "range",
                        field         : "age",
                        lower         : 1,
                        include_lower : true }}';
~~~~

Example 2: will return rows where $age \\in (-\\infty,0]$

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type          : "range",
                        field         : "age",
                        upper         : 0,
                        include_upper : true }}';
~~~~

Example 3: will return rows where $age \\in [-1,1]$

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type          : "range",
                        field         : "age",
                        lower         : -1,
                        upper         : 1,
                        include_lower : true,
                        include_upper : true }}';
~~~~

Example 4: will return rows where $date \\ge \\text"2014/01/01" \\land date \\le \\text"2014/01/02"$

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type          : "range",
                        field         : "date",
                        lower         : "2014/01/01",
                        upper         : "2014/01/02",
                        include_lower : true,
                        include_upper : true }}';
~~~~

Regexp query
------------

Syntax:

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type  : "regexp",
                        field : &lt;fieldname>,
                        value : &lt;regexp>
                     }}';
~~~~

where:

-   **value**: a regular expression. See [org.apache.lucene.util.automaton.RegExp](http://lucene.apache.org/core/4_6_1/core/org/apache/lucene/util/automaton/RegExp.html "Reference for Lucene regular expressions") for syntax reference.

Example: will return rows where name contains a word that starts with “p” and a vowel repeated twice (e.g. “pape”).

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type  : "regexp",
                        field : "name",
                        value : "[J][aeiou]{2}.*" }}';
~~~~

Wildcard query
--------------

Syntax:

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type    : "wildcard" ,
                        field   : &lt;fieldname> ,
                        value   : &lt;wildcard_exp>
                     }}';
~~~~

where:

-   **value**: a wildcard expression. Supported wildcards are \*, which matches any character sequence (including the empty one), and ?, which matches any single character. ” is the escape character.

Example: will return rows where food starts with or is “tu”.

~~~~ {.prettyprint .lang-meta}
SELECT * FROM test.users
WHERE stratio_col = '{query : {
                        type  : "wildcard",
                        field : "food",
                        value : "tu*" }}';
~~~~

Other Interesting Queries
=========================

Token Function
--------------

The token function allows computing the token for a given partition key. The primary key of the example table “users” is ((name, gender), animal, age) where (name, gender) is the partition key. When combining the token function and a Lucene-based filter in a where clause, the filter on tokens is applied first and then the condition of the filter clause. These kinds of queries are very useful to paginate results.

Example: will retrieve rows which tokens are greater than (‘Alicia’, ‘female’) and then test them against the match condition.

~~~~ {.prettyprint .lang-meta}
SELECT name,gender
  FROM test.users
 WHERE stratio_col='{filter : {type : "match", field : "food", value : "chips"}}'
   AND token(name, gender) > token('Alicia', 'female');
~~~~

Server Side Filtering
---------------------

By default, CQL does not allow selecting queries to filter on non-indexed columns. The ALLOW FILTERING option allows explicitly this type of filtering and can be used together with a Lucene-based query. Note that using server side filtering will have an unpredictable performance.

Example: will retrieve rows where name starts with “J” and number is greater than 10. Note that number is not part of any index, custom or secondary.

~~~~ {.prettyprint .lang-meta}
SELECT name, number
  FROM test.users
 WHERE stratio_col = '{query : {type : "wildcard", field : "name", value: "J*"}}'
   AND number > 10 ALLOW FILTERING;
~~~~

Datatypes Mapping
=================

CQL to Field type
-----------------

<table>
    <thead>
    <tr>
        <th>CQL type</th>
        <th>Description</th>
        <th>Field type</th>
        <th>Supported in query types</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td>ascii</td>
        <td>US-ASCII character string</td>
        <td>string/text</td>
        <td>All</td>
    </tr>
    <tr>
        <td>bigint</td>
        <td>64-bit signed long</td>
        <td>long</td>
        <td>boolean<br />
            match<br />
            range</td>
    </tr>
    <tr>
        <td>blob</td>
        <td>Arbitrary bytes (no validation), expressed as hexadecimal</td>
        <td>bytes</td>
        <td>All</td>
    </tr>
    <tr>
        <td>boolean</td>
        <td>true or false</td>
        <td>boolean</td>
        <td>All</td>
    </tr>
    <tr>
        <td>counter</td>
        <td>Distributed counter value (64-bit long)</td>
        <td><em>not supported</em></td>
        <td></td>
    </tr>
    <tr>
        <td>decimal</td>
        <td>Variable-precision decimal</td>
        <td>bigdec</td>
        <td>All</td>
    </tr>
    <tr>
        <td>double</td>
        <td>64-bit IEEE-754 floating point</td>
        <td>double</td>
        <td>boolean<br />
            match<br />
            range</td>
    </tr>
    <tr>
        <td>float</td>
        <td>32-bit IEEE-754 floating point</td>
        <td>float</td>
        <td>boolean<br />
            match<br />
            range</td>
    </tr>
    <tr>
        <td>inet</td>
        <td>IP address string in IPv4 or IPv6 format</td>
        <td>inet</td>
        <td>All</td>
    </tr>
    <tr>
        <td>int</td>
        <td>32-bit signed integer</td>
        <td>integer</td>
        <td>boolean<br />
            match<br />
            range</td>
    </tr>
    <tr>
        <td>list&lt;T&gt;</td>
        <td>A collection of one or more ordered elements</td>
        <td>Type of list elements</td>
        <td><em>see element type</em></td>
    </tr>
    <tr>
        <td>map&lt;K,V&gt;</td>
        <td>A JSON-style array of literals: { literal : literal, literal : literal … }</td>
        <td>Type of values</td>
        <td><em>see element type</em></td>
    </tr>
    <tr>
        <td>set&lt;T&gt;</td>
        <td>A collection of one or more elements</td>
        <td>Type of set elements</td>
        <td><em>see element type</em></td>
    </tr>
    <tr>
        <td>text</td>
        <td>UTF-8 encoded string</td>
        <td>string/text</td>
        <td>All</td>
    </tr>
    <tr>
        <td>timestamp</td>
        <td>Date plus time, encoded as 8 bytes since epoch</td>
        <td>date</td>
        <td>boolean<br />
            match<br />
            range</td>
    </tr>
    <tr>
        <td>uuid</td>
        <td>Type 1 or type 4 UUID</td>
        <td>uuid</td>
        <td>All</td>
    </tr>
    <tr>
        <td>timeuuid</td>
        <td>Type 1 UUID only (CQL3)</td>
        <td>uuid</td>
        <td>All</td>
    </tr>
    <tr>
        <td>varchar</td>
        <td>UTF-8 encoded string</td>
        <td>string/text</td>
        <td>All</td>
    </tr>
    <tr>
        <td>varint</td>
        <td>Arbitrary-precision integer</td>
        <td>bigint</td>
        <td>All</td>
    </tr>
    </tbody>
</table>

Field type to CQL
-----------------

<table>
    <thead>
    <tr>
        <th>field type</th>
        <th>CQL type</th>
        <th>Supported in Query types</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td>bigdec</td>
        <td>decimal</td>
        <td>All</td>
    </tr>
    <tr>
        <td>bigint</td>
        <td>varint</td>
        <td>All</td>
    </tr>
    <tr>
        <td>boolean</td>
        <td>boolean</td>
        <td>All</td>
    </tr>
    <tr>
        <td>bytes</td>
        <td>blob</td>
        <td>All</td>
    </tr>
    <tr>
        <td>date</td>
        <td>timestamp</td>
        <td>boolean<br />
            match<br />
            range</td>
    </tr>
    <tr>
        <td>double</td>
        <td>double</td>
        <td>boolean<br />
            match<br />
            range</td>
    </tr>
    <tr>
        <td>float</td>
        <td>float</td>
        <td>boolean<br />
            match<br />
            range</td>
    </tr>
    <tr>
        <td>inet</td>
        <td>inet</td>
        <td>All</td>
    </tr>
    <tr>
        <td>integer</td>
        <td>int</td>
        <td>boolean<br />
            match<br />
            range</td>
    </tr>
    <tr>
        <td>long</td>
        <td>bigint</td>
        <td>boolean<br />
            match<br />
            range</td>
    </tr>
    <tr>
        <td>string/text</td>
        <td>ascii<br />
            text<br />
            varchar</td>
        <td>All</td>
    </tr>
    <tr>
        <td>uuid</td>
        <td>uuid<br />
            timeuuid</td>
        <td>All</td>
    </tr>
    </tbody>
</table>
