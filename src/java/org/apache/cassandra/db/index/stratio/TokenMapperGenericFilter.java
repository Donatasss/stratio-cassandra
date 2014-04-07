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
package org.apache.cassandra.db.index.stratio;

import java.io.IOException;

import org.apache.cassandra.db.DataRange;
import org.apache.cassandra.dht.AbstractBounds;
import org.apache.cassandra.dht.Token;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.OpenBitSet;

/**
 * {@link Filter} that filters documents which token field satisfies a certain {@link DataRange}.
 * 
 * @author adelapena
 * 
 */
public class TokenMapperGenericFilter extends Filter {

	private final TokenMapperGeneric tokenMapperGeneric;

	@SuppressWarnings("rawtypes")
	private final AbstractBounds<Token> keyRange;

	/**
	 * Returns a new {@code TokenMapperGeneric} for the specified data range using the specified
	 * token mapper.
	 * 
	 * @param tokenMapperGeneric
	 *            The used token mapper.
	 * @param dataRange
	 *            The partition data range to be filtered.
	 */
	public TokenMapperGenericFilter(TokenMapperGeneric tokenMapperGeneric, DataRange dataRange) {
		this.tokenMapperGeneric = tokenMapperGeneric;
		this.keyRange = dataRange.keyRange().toTokenBounds();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DocIdSet getDocIdSet(AtomicReaderContext context, final Bits acceptDocs) throws IOException {

		AtomicReader atomicReader = context.reader();
		Bits liveDocs = atomicReader.getLiveDocs();

		OpenBitSet bitSet = new OpenBitSet(atomicReader.maxDoc());

		Terms terms = atomicReader.terms(TokenMapperGeneric.FIELD_NAME);
		if (terms == null) {
			return null;
		}

		final TermsEnum termsEnum = terms.iterator(null);

		BytesRef bytesRef = termsEnum.next();
		while (bytesRef != null) {
			DocsEnum docsEnum = termsEnum.docs(liveDocs, null);
			Token<?> token = tokenMapperGeneric.token(bytesRef);
			if (keyRange.contains(token)) {
				Integer docID = docsEnum.nextDoc();
				while (docID != DocIdSetIterator.NO_MORE_DOCS) {
					bitSet.set(docID);
					docID = docsEnum.nextDoc();
				}
			}
			bytesRef = termsEnum.next();
		}
		return bitSet;
	}

}
