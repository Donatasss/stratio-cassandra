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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.cassandra.io.util.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TrackingIndexWriter;
import org.apache.lucene.index.sorter.EarlyTerminatingSortingCollector;
import org.apache.lucene.index.sorter.SortingMergePolicy;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.ControlledRealTimeReopenThread;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.lucene.util.Version;

import com.stratio.cassandra.index.util.Log;

/**
 * Class wrapping a Lucene's directory and its readers , writers and searchers for NRT.
 * 
 * @author Andres de la Pena <adelapena@stratio.com>
 * 
 */
public class LuceneIndex
{
    private final String path;
    private final Double refreshSeconds;
    private final Integer ramBufferMB;
    private final Integer maxMergeMB;
    private final Integer maxCachedMB;
    private final Analyzer analyzer;

    private File file;
    private Directory directory;
    private IndexWriter indexWriter;
    private TrackingIndexWriter trackingIndexWriter;
    private SearcherManager searcherManager;
    private ControlledRealTimeReopenThread<IndexSearcher> searcherReopener;

    private Sort sort;

    /**
     * Builds a new {@code RowDirectory} using the specified directory path and analyzer.
     * 
     * @param path
     *            The analyzer to be used. The path of the directory in where the Lucene's files will be stored.
     * @param refreshSeconds
     *            The index readers refresh time in seconds. No guarantees that the writings are visible until this
     *            time.
     * @param ramBufferMB
     *            The index writer buffer size in MB.
     * @param maxMergeMB
     *            NRTCachingDirectory max merge size in MB.
     * @param maxCachedMB
     *            NRTCachingDirectory max cached MB.
     * @param analyzer
     *            The default {@link Analyzer}.
     */
    public LuceneIndex(String path,
                       Double refreshSeconds,
                       Integer ramBufferMB,
                       Integer maxMergeMB,
                       Integer maxCachedMB,
                       Analyzer analyzer)
    {
        this.path = path;
        this.refreshSeconds = refreshSeconds;
        this.ramBufferMB = ramBufferMB;
        this.maxMergeMB = maxMergeMB;
        this.maxCachedMB = maxCachedMB;
        this.analyzer = analyzer;
    }

    /**
     * Initializes this using the specified {@link Sort} for trying to keep the {@link Document}s sorted.
     * 
     * @param sort
     *            The {@link Sort} to be used.
     */
    public void init(Sort sort) throws IOException
    {
        this.sort = sort;

        // Get directory file
        file = new File(path);

        // Open or create directory
        FSDirectory fsDirectory = FSDirectory.open(file);
        directory = new NRTCachingDirectory(fsDirectory, maxMergeMB, maxCachedMB);

        // Setup index writer
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_48, analyzer);
        config.setRAMBufferSizeMB(ramBufferMB);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        config.setUseCompoundFile(true);
        config.setMergePolicy(new SortingMergePolicy(config.getMergePolicy(), sort));
        indexWriter = new IndexWriter(directory, config);

        // Setup NRT search
        SearcherFactory searcherFactory = new SearcherFactory()
        {
            public IndexSearcher newSearcher(IndexReader reader) throws IOException
            {
                IndexSearcher searcher = new IndexSearcher(reader);
                searcher.setSimilarity(new NoIDFSimilarity());
                return searcher;
            }
        };
        trackingIndexWriter = new TrackingIndexWriter(indexWriter);
        searcherManager = new SearcherManager(indexWriter, true, searcherFactory);
        searcherReopener = new ControlledRealTimeReopenThread<>(trackingIndexWriter,
                                                                searcherManager,
                                                                refreshSeconds,
                                                                refreshSeconds);
        searcherReopener.start(); // Start the refresher thread
    }

    /**
     * Updates the specified {@link Document} by first deleting the documents containing {@code Term} and then adding
     * the new document. The delete and then add are atomic as seen by a reader on the same index (flush may happen only
     * after the add).
     * 
     * @param term
     *            The {@link Term} to identify the document(s) to be deleted.
     * @param document
     *            The {@link Document} to be added.
     */
    public void upsert(Term term, Document document) throws IOException
    {
        // Log.debug("Updating document %s with term %s", document, term);
        indexWriter.updateDocument(term, document);
    }

    /**
     * Updates the specified {@link Document}s by first deleting the documents containing {@code Term} and then adding
     * the new documents. The delete and then adds are atomic as seen by a reader on the same index (flush may happen
     * only after the add).
     * 
     * @param term
     *            The {@link Term} to identify the document(s) to be deleted.
     * @param documents
     *            The {@link Document}s to be added.
     */
    public void upsert(Term term, Iterable<Document> documents) throws IOException
    {
        // Log.debug("Updating documents %s with term %s", documents, term);
        indexWriter.updateDocuments(term, documents);
    }

    /**
     * Deletes all the {@link Document}s containing the specified {@link Term}.
     * 
     * @param term
     *            The {@link Term} to identify the documents to be deleted.
     */
    public void delete(Term term) throws IOException
    {
        // Log.debug(String.format("Deleting by term %s", term));
        indexWriter.deleteDocuments(term);
    }

    /**
     * Deletes all the {@link Document}s satisfying the specified {@link Query}.
     * 
     * @param query
     *            The {@link Query} to identify the documents to be deleted.
     */
    public void delete(Query query) throws IOException
    {
        // Log.debug("Deleting by query %s", query);
        indexWriter.deleteDocuments(query);
    }

    /**
     * Deletes all the {@link Document}s.
     */
    public void truncate() throws IOException
    {
        Log.info("Deleting all");
        indexWriter.deleteAll();
    }

    /**
     * Commits the pending changes.
     */
    public void commit() throws IOException
    {
        Log.info("Committing");
        indexWriter.commit();
    }

    /**
     * Commits all changes to an index, waits for pending merges to complete, and closes all associated resources.
     */
    public void close() throws IOException
    {
        Log.info("Closing");
        searcherReopener.interrupt();
        searcherManager.close();
        indexWriter.close();
        directory.close();
        analyzer.close();
    }

    /**
     * Closes and removes all the index files.
     * 
     * @return
     */
    public void drop() throws IOException
    {
        Log.info("Removing");
        close();
        FileUtils.deleteRecursive(file);
    }

    /**
     * Returns the total size of all index files currently cached in memory.
     * 
     * @return The total size of all index files currently cached in memory.
     */
    public long getRAMSizeInBytes() throws IOException
    {
        return indexWriter == null ? 0 : indexWriter.ramSizeInBytes();
    }

    /**
     * Finds the top {@code count} hits for {@code query}, applying {@code filter} if non-null, and sorting the hits by
     * the criteria in {@code sort}.
     * 
     * @param after
     * @param query
     *            The {@link Query} to search for.
     * @param sort
     *            The {@link Sort} to be applied.
     * @param after
     * 
     * @param count
     *            Return only the top {@code count} results.
     * @param fieldsToLoad
     *            The name of the fields to be loaded.
     * @return The found documents, sorted according to the supplied {@link Sort} instance.
     */
    public List<ScoredDocument> search(Query query,
                                       Sort sort,
                                       ScoredDocument after,
                                       Integer count,
                                       Set<String> fieldsToLoad) throws IOException
    {
        // Log.debug("Searching with query %s ", query);
        // Log.debug("Searching with count %d", count);
        // Log.debug("Searching with sort %s", sort);
        // Log.debug("Searching with start %s", after == null ? null : after.getScoreDoc());

        // Validate
        if (count == null || count < 0)
        {
            throw new IllegalArgumentException("Positive count required");
        }
        if (fieldsToLoad == null || fieldsToLoad.isEmpty())
        {
            throw new IllegalArgumentException("Fields to load required");
        }
        IndexSearcher searcher = searcherManager.acquire();
        try
        {

            // Search
            ScoreDoc start = after == null ? null : after.getScoreDoc();
            TopDocs topDocs = topDocs(searcher, query, sort, start, count);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            // Collect the documents from query result
            List<ScoredDocument> scoredDocuments = new ArrayList<>(scoreDocs.length);
            for (ScoreDoc scoreDoc : scoreDocs)
            {
                Document document = searcher.doc(scoreDoc.doc, fieldsToLoad);
                ScoredDocument scoredDocument = new ScoredDocument(document, scoreDoc);
                scoredDocuments.add(scoredDocument);
                // Log.debug("Found %s", scoredDocument);
            }

            return scoredDocuments;
        }
        finally
        {
            searcherManager.release(searcher);
        }
    }

    private TopDocs topDocs(IndexSearcher searcher, Query query, Sort sort, ScoreDoc after, int count)
            throws IOException
    {
        if (query == null)
        {
            query = new MatchAllDocsQuery();
            sort = sort == null ? this.sort : sort;
            FieldDoc start = after == null ? null : (FieldDoc) after;
            TopFieldCollector tfc = TopFieldCollector.create(sort, count, start, false, false, false, false);
            Collector collector = new EarlyTerminatingSortingCollector(tfc, sort, count);
            searcher.search(query, collector);
            return tfc.topDocs();
        }
        else
        {
            query = query == null ? new MatchAllDocsQuery() : query;
            if (sort == null)
            {
                return searcher.searchAfter(after, query, count);
            }
            else
            {
                return searcher.searchAfter(after, query, count, sort);
            }
        }
    }

    public void optimize() throws IOException
    {
        indexWriter.forceMerge(1, true);
        indexWriter.commit();
    }
}