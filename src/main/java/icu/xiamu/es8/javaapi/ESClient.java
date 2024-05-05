package icu.xiamu.es8.javaapi;

import co.elastic.clients.elasticsearch.*;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.CreateOperation;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.*;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.*;
import org.elasticsearch.client.*;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.KeyStore;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author 肉豆蔻吖
 * @date 2024/5/5
 */
public class ESClient {

    private static ElasticsearchClient client;
    private static ElasticsearchAsyncClient asyncClient;
    private static ElasticsearchTransport transport;

    public static void main(String[] args) throws Exception {
        // 初始化ES服务器的连接
        initESConnection();

        // 操作索引
        // operationIndex();
        // operationIndexLambda();

        // 操作文档
        // operationDocument();
        // operationDocumentLambda();

        // 文档查询
        // queryDocument();
        // queryDocumentLambda();

        // 异步操作
        asyncClientOperation();
    }

    private static void asyncClientOperation() {
        // 创建索引
        asyncClient.indices().create(
                req -> {
                    req.index("newindex");
                    return req;
                }
        ).whenComplete(
                (resp, error) -> {
                    System.out.println("回调函数");
                    if ( resp != null ) {
                        System.out.println(resp.acknowledged());
                    } else {
                        error.printStackTrace();
                    }
                }
        );
        System.out.println("主线程操作...");

        // asyncClient.indices().create(req -> {
        //     req.index("newindex");
        //     return req;
        // }).thenApply(resp -> {
        //     return resp.acknowledged();
        // }).whenComplete((resp, error) -> {
        //     System.out.println("回调函数");
        //     if (!resp) {
        //         System.out.println();
        //     } else {
        //         error.printStackTrace();
        //     }
        // });

    }

    private static void queryDocumentLambda() throws IOException {
        client.search(req -> {
            req.query(q -> q.match(m -> m.field("city").query("beijing")));
            return req;
        }, Object.class);

        transport.close();
    }

    private static void operationDocumentLambda() throws IOException {

        User user = new User();
        user.setId(2002);
        user.setName("huanglei");
        user.setAge(22);

        // 创建文档
        System.out.println(client.index(req -> req.index("myindex").id(user.getId().toString()).document(user)).result());

        // List<User> users = new ArrayList<>();
        // for (int i = 1; i <= 5; i++) {
        //     users.add(new User(2000 + i, "list" + i, 30 + i));
        // }
        List<User> users = IntStream.rangeClosed(1, 5).mapToObj(i -> new User(2000 + i, "list" + i, 30 + i)).collect(Collectors.toList());

        // 批量创建文档
        client.bulk(req -> {
            users.forEach(u -> {
                req.operations(b -> {
                    b.create(d -> d.id(u.getId().toString()).index("myindex").document(u));
                    return b;
                });
            });
            return req;
        });

        // 删除文档
        client.delete(req -> req.index("myindex").id("1001"));
        transport.close();
    }

    private static void operationIndexLambda() throws Exception {
        // 创建索引
        final Boolean acknowledged = client.indices().create(p -> p.index("myindex1")).acknowledged();
        System.out.println("创建索引成功");
        // 获取索引
        System.out.println(client.indices().get(req -> req.index("myindex1")).result());
        // 删除索引
        client.indices().delete(reqbuilder -> reqbuilder.index("myindex")).acknowledged();

        transport.close();
    }

    private static void queryDocument() throws Exception {
        final SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder().index("myindex");
        MatchQuery matchQuery = new MatchQuery.Builder().field("city").query(FieldValue.of("beijing")).build();
        Query query = new Query.Builder().match(matchQuery).build();
        searchRequestBuilder.query(query);
        SearchRequest searchRequest = searchRequestBuilder.build();
        final SearchResponse<Object> search = client.search(searchRequest, Object.class);
        System.out.println(search);

        transport.close();

    }

    private static void operationDocument() throws Exception {

        User user = new User();
        user.setId(1001);
        user.setName("zhangsan");
        user.setAge(31);


        // 创建文档
        IndexRequest indexRequest = new IndexRequest.Builder().index("myindex").id(user.getId().toString()).document(user).build();
        final IndexResponse index = client.index(indexRequest);
        System.out.println("文档操作结果:" + index.result());

        // 批量创建文档
        final List<BulkOperation> operations = new ArrayList<BulkOperation>();
        for (int i = 1; i <= 5; i++) {
            final CreateOperation.Builder builder = new CreateOperation.Builder();
            builder.index("myindex");
            builder.id("200" + i);
            builder.document(new User(2000 + i, "zhangsan" + i, 30 + i * 10));
            final CreateOperation<Object> objectCreateOperation = builder.build();
            final BulkOperation bulk = new BulkOperation.Builder().create(objectCreateOperation).build();
            operations.add(bulk);
        }
        BulkRequest bulkRequest = new BulkRequest.Builder().operations(operations).build();
        final BulkResponse bulkResponse = client.bulk(bulkRequest);
        System.out.println("数据操作成功：" + bulkResponse);

        // 删除文档
        DeleteRequest deleteRequest = new DeleteRequest.Builder().index("myindex").id("1001").build();
        client.delete(deleteRequest);

        transport.close();
    }

    private static void initESConnection() throws Exception {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", "L8gKNFZ1aGowfrP0vO*F"));

        Path caCertificatePath = Paths.get("cert/java-ca.crt");
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        Certificate trustedCa;
        try (InputStream is = Files.newInputStream(caCertificatePath)) {
            trustedCa = factory.generateCertificate(is);
        }
        KeyStore trustStore = KeyStore.getInstance("pkcs12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", trustedCa);
        SSLContextBuilder sslContextBuilder = SSLContexts.custom().loadTrustMaterial(trustStore, null);
        final SSLContext sslContext = sslContextBuilder.build();

        RestClientBuilder builder = RestClient.builder(new HttpHost("192.168.1.100", 9201, "https")).setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                return httpClientBuilder.setSSLContext(sslContext).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).setDefaultCredentialsProvider(credentialsProvider);
            }
        });

        RestClient restClient = builder.build();

        transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        client = new ElasticsearchClient(transport);
        asyncClient = new ElasticsearchAsyncClient(transport);

    }

    private static void operationIndex() throws Exception {
        // 创建索引
        CreateIndexRequest request = new CreateIndexRequest.Builder().index("myindex").build();
        final CreateIndexResponse createIndexResponse = client.indices().create(request);
        System.out.println("创建索引成功：" + createIndexResponse.acknowledged());

        // 查询索引
        GetIndexRequest getIndexRequest = new GetIndexRequest.Builder().index("myindex").build();
        final GetIndexResponse getIndexResponse = client.indices().get(getIndexRequest);
        System.out.println("索引查询成功：" + getIndexResponse.result());

        // 删除索引
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest.Builder().index("myindex").build();
        final DeleteIndexResponse delete = client.indices().delete(deleteIndexRequest);
        final boolean acknowledged = delete.acknowledged();
        System.out.println("删除索引成功：" + acknowledged);

        transport.close();
    }
}
