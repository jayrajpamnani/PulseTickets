package com.antra.banner;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.sns.SnsClient;

/** Stores each S3 event once, then emits one SNS notification for that banner. */
public class BannerHandler implements RequestHandler<Map<String, Object>, String> {
  @Override @SuppressWarnings("unchecked")
  public String handleRequest(Map<String, Object> event, Context context) {
    Map<String, Object> record = (Map<String, Object>) ((List<?>) event.get("Records")).getFirst();
    Map<String, Object> s3 = (Map<String, Object>) record.get("s3");
    String key = String.valueOf(((Map<?, ?>) s3.get("object")).get("key"));
    String bucket = String.valueOf(((Map<?, ?>) s3.get("bucket")).get("name"));
    String eventId = String.valueOf(record.get("eventID"));
    HeadObjectResponse head = S3Client.create().headObject(b -> b.bucket(bucket).key(key));
    try (DynamoDbClient ddb = DynamoDbClient.create(); SnsClient sns = SnsClient.create()) {
      ddb.putItem(PutItemRequest.builder().tableName(System.getenv("METADATA_TABLE"))
          .conditionExpression("attribute_not_exists(eventId)")
          .item(Map.of("eventId", AttributeValue.fromS(eventId), "s3Key", AttributeValue.fromS(key),
              "contentType", AttributeValue.fromS(String.valueOf(head.contentType())),
              "sizeBytes", AttributeValue.fromN(String.valueOf(head.contentLength())),
              "processedAt", AttributeValue.fromS(Instant.now().toString())))
          .build());
      sns.publish(p -> p.topicArn(System.getenv("TOPIC_ARN")).subject("Event banner processed")
          .message("Processed banner " + key));
    } catch (ConditionalCheckFailedException ignored) { }
    return eventId;
  }
}
