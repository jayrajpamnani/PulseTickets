package com.antra.event;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/** Gives admins a short-lived, single-object S3 upload URL; credentials never reach the browser. */
@RestController @RequestMapping("/api/events")
class BannerController {
  private final EventRepository events; private final String bucket; private final String region; private final String publicBaseUrl;
  BannerController(EventRepository events,@Value("${app.banner.bucket:}") String bucket,@Value("${app.banner.region:us-east-1}") String region,@Value("${app.banner.public-base-url:}") String publicBaseUrl){this.events=events;this.bucket=bucket;this.region=region;this.publicBaseUrl=publicBaseUrl;}
  @PostMapping("/{id}/banner/upload-url") @PreAuthorize("hasRole('ADMIN')") UploadUrl createUpload(@PathVariable("id") Long id,@RequestParam(name="contentType",defaultValue="image/jpeg") String contentType){
    if(bucket.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Banner storage is not configured");
    events.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    if(!contentType.startsWith("image/"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only image uploads are allowed");
    String key="events/"+id+"/"+UUID.randomUUID()+extension(contentType);
    try(S3Presigner presigner=S3Presigner.builder().region(Region.of(region)).build()){
      PresignedPutObjectRequest signed=presigner.presignPutObject(PutObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(5)).putObjectRequest(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build()).build());
      String publicUrl=publicBaseUrl.isBlank()?"https://"+bucket+".s3."+region+".amazonaws.com/"+key:publicBaseUrl.replaceAll("/$","")+"/"+key;
      return new UploadUrl(signed.url().toString(),key,publicUrl);
    }
  }
  @PutMapping("/{id}/banner") @PreAuthorize("hasRole('ADMIN')") Event attach(@PathVariable("id") Long id,@RequestBody BannerUpdate body){Event e=events.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if(body.url()==null||body.url().isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Banner URL is required");e.bannerUrl=body.url();return events.save(e);}
  private String extension(String type){return switch(type){case "image/png"->".png";case "image/webp"->".webp";default->".jpg";};}
  record UploadUrl(String uploadUrl,String key,String publicUrl){} record BannerUpdate(String url){}
}
