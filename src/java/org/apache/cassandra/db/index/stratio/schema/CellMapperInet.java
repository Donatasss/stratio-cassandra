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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.AsciiType;
import org.apache.cassandra.db.marshal.InetAddressType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.codehaus.jackson.annotate.JsonCreator;

/**
 * A {@link CellMapper} to map a string, not tokenized field.
 * 
 * @author Andres de la Pena <adelapena@stratio.com>
 */
public class CellMapperInet extends CellMapper<String> {

	private static final Pattern IPV4_PATTERN = Pattern.compile("(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])");
	private static final Pattern IPV6_PATTERN = Pattern.compile("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");
	private static final Pattern IPV6_COMPRESSED_PATTERN = Pattern.compile("^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$");

	@JsonCreator
	public CellMapperInet() {
		super(new AbstractType<?>[] { AsciiType.instance, UTF8Type.instance, InetAddressType.instance });
	}

	@Override
	public Analyzer analyzer() {
		return EMPTY_ANALYZER;
	}

	@Override
	public String indexValue(String name, Object value) {
		if (value == null) {
			return null;
		} else if (value instanceof InetAddress) {
			InetAddress inetAddress = (InetAddress) value;
			return inetAddress.getHostAddress();
		} else if (value instanceof String) {
			String svalue = (String) value;
			if (IPV4_PATTERN.matcher(svalue).matches() || IPV6_PATTERN.matcher(svalue).matches()
			    || IPV6_COMPRESSED_PATTERN.matcher(svalue).matches()) {
				try {
					return InetAddress.getByName(svalue).getHostAddress();
				} catch (UnknownHostException e) {
				}
			}
		}
		throw new IllegalArgumentException(String.format("Value '%s' cannot be cast to InetAddress", value));
	}

	@Override
	public String queryValue(String name, Object value) {
		if (value == null) {
			return null;
		} else if (value instanceof InetAddress) {
			InetAddress inetAddress = (InetAddress) value;
			return inetAddress.getHostAddress();
		} else if (value instanceof String) {
			String svalue = (String) value;
			if (IPV4_PATTERN.matcher(svalue).matches() || IPV6_PATTERN.matcher(svalue).matches()
			    || IPV6_COMPRESSED_PATTERN.matcher(svalue).matches()) {
				try {
					return InetAddress.getByName(svalue).getHostAddress();
				} catch (UnknownHostException e) {
				}
			} else {
				return svalue;
			}
		}
		throw new IllegalArgumentException(String.format("Value '%s' cannot be cast to InetAddress", value));
	}

	@Override
	public Field field(String name, Object value) {
		String string = indexValue(name, value);
		return new StringField(name, string, STORE);
	}

	@Override
	public Class<String> baseClass() {
		return String.class;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName());
		builder.append(" []");
		return builder.toString();
	}

}
