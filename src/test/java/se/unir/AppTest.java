package se.unir;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AppTest {

    Directory memoryIndex;
    IndexWriter writer;
    StandardAnalyzer analyzer;
    IndexWriterConfig indexWriterConfig;

    @BeforeEach
    void setUp() throws IOException {
        memoryIndex = new ByteBuffersDirectory();
        analyzer = new StandardAnalyzer();
        indexWriterConfig = new IndexWriterConfig(analyzer);
        writer = new IndexWriter(memoryIndex, indexWriterConfig);
    }

    @Test
    public void shouldAnswerWithTrue() throws IOException {
        Document document = new Document();
        document.add(new TextField("title", "title1", Field.Store.YES));
        document.add(new TextField("body", "body1", Field.Store.YES));
        writer.addDocument(document);

        document = new Document();
        document.add(new TextField("title", "title2", Field.Store.YES));
        document.add(new TextField("body", "body2", Field.Store.YES));
        writer.addDocument(document);


        try (IndexReader reader = DirectoryReader.open(writer)) {
            assertEquals(2, reader.maxDoc());

            IndexSearcher indexSearcher = new IndexSearcher(reader);
            Term term = new Term("title", "title1");
            TopDocs foundDocuments = indexSearcher.search(new TermQuery(term), 5);

            for (ScoreDoc foundDocument : foundDocuments.scoreDocs) {
                final Document doc = reader.storedFields().document(foundDocument.doc);

                assertEquals("title1", doc.get("title"));

                doc.removeField("title");
                doc.add(new TextField("title", "super fancy new title xyz", Field.Store.YES));
                writer.updateDocument(term, doc);
            }

            assertEquals(2, reader.maxDoc());

            indexSearcher = new IndexSearcher(reader);
            term = new Term("title", "title1");
            foundDocuments = indexSearcher.search(new TermQuery(term), 5);

            assertEquals(0, foundDocuments.scoreDocs.length);

            indexSearcher = new IndexSearcher(reader);
            term = new Term("title", "super fancy new title xyz");
            foundDocuments = indexSearcher.search(new TermQuery(term), 5);

            assertEquals(1, foundDocuments.scoreDocs.length);

            for (ScoreDoc foundDocument : foundDocuments.scoreDocs) {
                final Document doc = reader.storedFields().document(foundDocument.doc);

                assertEquals("super fancy new title xyz", doc.get("title"));
            }
        }
    }
}
