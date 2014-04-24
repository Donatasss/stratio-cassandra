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
package org.apache.cassandra.db.index.stratio.query;

import org.apache.cassandra.db.index.stratio.schema.CellMapper;
import org.apache.cassandra.db.index.stratio.schema.Schema;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * 
 * Implements the wildcard search query. Supported wildcards are {@code *}, which matches any
 * character sequence (including the empty one), and {@code ?}, which matches any single character.
 * '\' is the escape character.
 * <p>
 * Note this query can be slow, as it needs to iterate over many terms. In order to prevent
 * extremely slow WildcardQueries, a Wildcard term should not start with the wildcard {@code *}.
 * 
 * @author Andres de la Pena <adelapena@stratio.com>
 */
public class RegexpCondition extends Condition {

	/** The field name */
	private final String field;

	/** The field value */
	private final String value;

	/**
	 * Constructor using the field name and the value to be matched.
	 * 
	 * @param boost
	 *            The boost for this query clause. Documents matching this clause will (in addition
	 *            to the normal weightings) have their score multiplied by {@code boost}. If
	 *            {@code null}, then {@link DEFAULT_BOOST} is used as default.
	 * @param field
	 *            the field name.
	 * @param value
	 *            the field value.
	 */
	@JsonCreator
	public RegexpCondition(@JsonProperty("boost") Float boost,
	                       @JsonProperty("field") String field,
	                       @JsonProperty("value") String value) {
		super(boost);

		this.field = field;
		this.value = value;
	}

	/**
	 * Returns the field name.
	 * 
	 * @return the field name.
	 */
	public String getField() {
		return field;
	}

	/**
	 * Returns the field value.
	 * 
	 * @return the field value.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Query query(Schema schema) {

		if (field == null || field.trim().isEmpty()) {
			throw new IllegalArgumentException("Field name required");
		}
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("Field value required");
		}

		CellMapper<?> cellMapper = schema.getMapper(field);
		Class<?> clazz = cellMapper.baseClass();
		Query query;
		if (clazz == String.class) {
			Term term = new Term(field, value);
			query = new RegexpQuery(term);
		} else {
			String message = String.format("Regexp queries are not supported by %s mapper", clazz.getSimpleName());
			throw new UnsupportedOperationException(message);
		}
		query.setBoost(boost);
		return query;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName());
		builder.append(" [boost=");
		builder.append(boost);
		builder.append(", field=");
		builder.append(field);
		builder.append(", value=");
		builder.append(value);
		builder.append("]");
		return builder.toString();
	}

}
