package icu.xiamu.javaapi;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;

public class ESTest_Client {
    public static void main(String[] args) throws IOException {
        // 创建ES客户端
        RestHighLevelClient esClient = new RestHighLevelClient(
                RestClient.builder(new HttpHost("192.168.1.100", 9200, "http"))
        );
        // 关闭ES客户端
        esClient.close();
    }
}
