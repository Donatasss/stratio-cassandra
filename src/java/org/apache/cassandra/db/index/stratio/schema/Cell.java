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
package org.apache.cassandra.db.index.stratio.schema;

import java.nio.ByteBuffer;

import org.apache.cassandra.db.marshal.AbstractType;

/**
 * A cell of a CQL3 logic {@link Cell}, which in most cases is different from a storage engine
 * column.
 * 
 * @author adelapena
 * 
 */
public class Cell {

	/** The column's name */
	private String name;

	private String nameSufix;

	private ByteBuffer value;

	private AbstractType<?> type;

	public Cell(String name, ByteBuffer value, AbstractType<?> type) {
		this.name = name;
		this.value = value;
		this.type = type;
	}

	public Cell(String name, String nameSufix, ByteBuffer value, AbstractType<?> type) {
		this.name = name;
		this.nameSufix = nameSufix;
		this.value = value;
		this.type = type;
	}

	/**
	 * Returns the name.
	 * 
	 * @return the name.
	 */
	public String getName() {
		return name;
	}

	public String getFieldName() {
		return nameSufix == null ? name : name + "." + nameSufix;
	}

	/**
	 * Returns the value.
	 * 
	 * @return the value.
	 */
	public ByteBuffer getRawValue() {
		return value;
	}

	public Object getValue() {
		return type.compose(value);
	}

	public AbstractType<?> getType() {
		return type;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Cell [name=");
		builder.append(name);
		builder.append(", nameSufix=");
		builder.append(nameSufix);
		builder.append(", value=");
		builder.append(value);
		builder.append(", type=");
		builder.append(type);
		builder.append("]");
		return builder.toString();
	}

}
