package com.thinkbiganalytics.metadata.rest.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.thinkbiganalytics.metadata.rest.model.data.Datasource;
import com.thinkbiganalytics.metadata.rest.model.data.DirectoryDatasource;
import com.thinkbiganalytics.metadata.rest.model.data.HiveTableDatasource;
import com.thinkbiganalytics.metadata.rest.model.data.HiveTablePartition;
import com.thinkbiganalytics.metadata.rest.model.extension.ExtensibleTypeDescriptor;
import com.thinkbiganalytics.metadata.rest.model.feed.Feed;
import com.thinkbiganalytics.metadata.rest.model.op.DataOperation;
import com.thinkbiganalytics.metadata.rest.model.op.DataOperation.State;
import com.thinkbiganalytics.metadata.rest.model.op.Dataset;
import com.thinkbiganalytics.metadata.rest.model.op.Dataset.ChangeType;
import com.thinkbiganalytics.metadata.rest.model.op.Dataset.ContentType;
import com.thinkbiganalytics.metadata.rest.model.op.HiveTablePartitions;
import com.thinkbiganalytics.metadata.rest.model.sla.FeedExecutedSinceFeedMetric;
import com.thinkbiganalytics.metadata.rest.model.sla.ServiceLevelAssessment;

//@Ignore  // Requires a running metadata server
public class MetadataClientTest {
    
    private MetadataClient client;

    @BeforeClass
    public void connect() {
        client = new MetadataClient(URI.create("http://localhost:8077/api/metadata/"));
    }
    
    
//    @Test
    public void testGetExtensibleTypes() {
        List<ExtensibleTypeDescriptor> types = client.getExtensibleTypes();
        
        assertThat(types).extracting("name")
            .contains("feed", "feedConnection", "feedSource", "feedDestination", "datasource");
        assertThat(types.get(0).getFields()).extracting("name")
            .isNotEmpty();
    }
    
//    @Test
    public void testGetExtensibleTypeByName() {
        ExtensibleTypeDescriptor type = client.getExtensibleType("feed");
        
        assertThat(type).isNotNull();
        assertThat(type.getName()).isEqualTo("feed");
    }
    
//    @Test(dependsOnMethods="testGetExtensibleTypeByName")
    public void testGetExtensibleTypeById() {
        ExtensibleTypeDescriptor feed = client.getExtensibleType("feed");
        ExtensibleTypeDescriptor type = client.getExtensibleType(feed.getId());
        
        assertThat(type).isNotNull();
        assertThat(type.getName()).isEqualTo("feed");
    }
    
//    @Test()
    public void testCreateFeedSubtype() {
        ExtensibleTypeDescriptor subtype = new ExtensibleTypeDescriptor("testFeed", "feed");
    }

//    @Test
    public void testBuildFeed() throws ParseException {
        Feed feed = buildFeed("feed1").post();
        
        assertThat(feed).isNotNull();
        
        feed = client.getFeed(feed.getId());
        
        assertThat(feed).isNotNull();
    }
    
//    @Test
    public void testMergeFeedProperties() throws ParseException {
        Feed feed = buildFeed("feed1").post();
        
        assertThat(feed).isNotNull();
        assertThat(feed.getProperties()).isNotNull().hasSize(1).containsEntry("key1", "value1");
        
        Properties props = new Properties();
        props.setProperty("testKey", "testValue");
        
        Properties result = client.mergeFeedProperties(feed.getId(), props);
        
        assertThat(result).isNotNull().hasSize(2).containsEntry("testKey", "testValue");
    }
    
//    @Test
    public void testUpdateFeed() throws ParseException {
        Feed feed = buildFeed("feed1").post();
        
        assertThat(feed.getDescription()).isEqualTo("feed1 feed");
        assertThat(feed.getState()).isEqualTo(Feed.State.ENABLED);
        assertThat(feed.isInitialized()).isFalse();
        
        feed.setDescription("Description changed");
        feed.setState(Feed.State.DISABLED);
        feed.setInitialized(true);
        
        Feed result = client.updateFeed(feed);
        
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo("Description changed");
        assertThat(result.getState()).isEqualTo(Feed.State.DISABLED);
        assertThat(feed.isInitialized()).isTrue();
    }
    
//    @Test
    public void testAddFeedSource() throws ParseException {
        Feed feed = buildFeed("feed1").post();
        HiveTableDatasource ds = buildHiveTableDatasource("test-table").post();
        
        Feed result = client.addSource(feed.getId(), ds.getId());
        
        assertThat(result).isNotNull();
    }
    
//    @Test 
    public void testAddFeedDestination() throws ParseException {
        Feed feed = buildFeed("feed1").post();
        HiveTableDatasource ds = buildHiveTableDatasource("test-table").post();
        
        Feed result = client.addDestination(feed.getId(), ds.getId());
        
        assertThat(result).isNotNull();
    }

//    @Test 
    public void testBuildHiveTableDatasource() {
        HiveTableDatasource ds = buildHiveTableDatasource("test-table").post();
        
        assertThat(ds).isNotNull();
    }
    
//    @Test
    public void testBuildDirectoryDatasource() {
        DirectoryDatasource ds = buildDirectoryDatasource("test-dir").post();
        
        assertThat(ds).isNotNull();
    }
    
//    @Test
    public void testListDatasources() {
        buildDirectoryDatasource("ds1").post();
        buildHiveTableDatasource("ds2").post();
        buildDirectoryDatasource("ds3").post();
        
        List<Datasource> list = client.getDatasources();
        
        assertThat(list)
            .isNotNull()
            .isNotEmpty();
    }
    
//    @Test
    public void testBeginOperation() throws ParseException {
        Feed feed = buildFeed("feed1").post();
        HiveTableDatasource ds = buildHiveTableDatasource("test-table").post();
        feed = client.addDestination(feed.getId(), ds.getId());
        String destId = feed.getDestinations().iterator().next().getId();

        DataOperation op = client.beginOperation(destId, "");
        
        assertThat(op).isNotNull();
    }
    
//    @Test
    public void testCompleteOperation() throws ParseException {
        Feed feedA = buildFeed("feedA").post();
        HiveTableDatasource dsA = buildHiveTableDatasource("test-table").post();
        feedA = client.addDestination(feedA.getId(), dsA.getId());
        
        Feed feedB = buildFeed("feedB", "feedA").post();
        feedB = client.addSource(feedB.getId(), dsA.getId());
        String destA = feedA.getDestinations().iterator().next().getId();
        
        DataOperation op = client.beginOperation(destA, "");
        op.setState(State.SUCCESS);
        
        HiveTablePartitions changeSet = new HiveTablePartitions();
        changeSet.setPartitions(Arrays.asList(new HiveTablePartition("month", null, "Jan", "Feb"),
                                              new HiveTablePartition("year", null, "2015", "2016")));
        Dataset dataset = new Dataset(new DateTime(), dsA, ChangeType.UPDATE, ContentType.PARTITIONS, changeSet);
        op.setDataset(dataset);

        op = client.updateDataOperation(op);
        
        assertThat(op).isNotNull();
    }
    
//    @Test
    public void testCheckPrecondition() throws ParseException {
        Feed feedA = buildFeed("feedA").post();
        Feed feedB = buildFeed("feedB", "feedA").post();
        
        HiveTableDatasource dsA = buildHiveTableDatasource("test-table").post();
        feedA = client.addDestination(feedA.getId(), dsA.getId());
        String destIdA = feedA.getDestinations().iterator().next().getId();
        
        DataOperation op = client.beginOperation(destIdA, "");
        op.setState(State.SUCCESS);
        op.setDataset(new Dataset(new DateTime(), dsA, ChangeType.UPDATE, ContentType.PARTITIONS));
        op = client.updateDataOperation(op);
        
        ServiceLevelAssessment assmt = client.assessPrecondition(feedB.getId());
        
        assertThat(assmt).isNotNull();
    }
    
    private FeedBuilder buildFeed(String name) throws ParseException {
        return client.buildFeed(name)
                .description(name + " feed")
                .owner("ownder")
                .displayName(name)
                .property("key1", "value1");
//                .preconditionMetric(FeedExecutedSinceScheduleMetric.named(name, "0 0 6 * * ? *"));
    }
    
    private FeedBuilder buildFeed(String name, String dependent) throws ParseException {
        return client.buildFeed(name)
                .description(name + " feed")
                .owner("ownder")
                .displayName(name)
                .property("key1", "value1")
                .preconditionMetric(FeedExecutedSinceFeedMetric.named(dependent, name));
    }
    
    private DirectoryDatasourceBuilder buildDirectoryDatasource(String name) {
        return client.buildDirectoryDatasource(name)
                .description(name + " datasource")
                .compressed(true)
                .ownder("owner")
                .path("/tmp/test")
                .globPattern("*.txt");
    }
    
    private HiveTableDatasourceBuilder buildHiveTableDatasource(String name) {
        return client.buildHiveTableDatasource(name)
                .description(name + " datasource")
                .encrypted(true)
                .ownder("owner")
                .database("testdb")
                .tableName("test_table")
                .field("key", "INT")
                .field("value", "VARCHAR")
                .partition("month", null, "jan", "feb", "mar")
                .partition("year", null, "2016");
    }
}
