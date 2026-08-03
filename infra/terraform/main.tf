locals {
  tags = { Project = var.project, ManagedBy = "terraform", Environment = "production" }
  azs  = slice(data.aws_availability_zones.available.names, 0, 3)
}

data "aws_caller_identity" "current" {}
data "aws_availability_zones" "available" { state = "available" }

resource "aws_s3_bucket" "banners" {
  bucket = "${var.project}-event-banners-${data.aws_caller_identity.current.account_id}"
  tags   = local.tags
}

resource "aws_s3_bucket_public_access_block" "banners" {
  bucket                  = aws_s3_bucket.banners.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_cors_configuration" "banners" {
  bucket = aws_s3_bucket.banners.id
  cors_rule {
    allowed_methods = ["GET", "HEAD", "PUT"]
    allowed_origins = ["http://localhost:4200", "https://*.cloudfront.net"]
    allowed_headers = ["Content-Type"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
}

resource "aws_cloudfront_origin_access_control" "banners" {
  name                              = "${var.project}-banners-oac"
  description                       = "CloudFront access to private event banners"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "banners" {
  enabled         = true
  is_ipv6_enabled = true
  comment         = "${var.project} event banners"
  origin {
    domain_name              = aws_s3_bucket.banners.bucket_regional_domain_name
    origin_id                = "event-banners-s3"
    origin_access_control_id = aws_cloudfront_origin_access_control.banners.id
  }
  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "event-banners-s3"
    viewer_protocol_policy = "redirect-to-https"
    compress               = true
    forwarded_values {
      query_string = false
      cookies { forward = "none" }
    }
  }
  restrictions {
    geo_restriction { restriction_type = "none" }
  }
  viewer_certificate { cloudfront_default_certificate = true }
  tags = local.tags
}

data "aws_iam_policy_document" "banners" {
  statement {
    sid       = "AllowCloudFrontRead"
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.banners.arn}/*"]
    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.banners.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "banners" {
  bucket = aws_s3_bucket.banners.id
  policy = data.aws_iam_policy_document.banners.json
}

resource "aws_s3_bucket_lifecycle_configuration" "banners" {
  bucket = aws_s3_bucket.banners.id
  rule {
    id     = "expire-old-banners"
    status = "Enabled"
    filter {}
    expiration { days = 365 }
  }
}

resource "aws_dynamodb_table" "metadata" {
  name         = "${var.project}-banner-metadata"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "eventId"
  attribute {
    name = "eventId"
    type = "S"
  }
  tags = local.tags
}

resource "aws_dynamodb_table" "history" {
  name         = "${var.project}-browsing-history"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "userId"
  range_key    = "viewedAt"
  attribute {
    name = "userId"
    type = "S"
  }
  attribute {
    name = "viewedAt"
    type = "N"
  }
  ttl {
    attribute_name = "expireAt"
    enabled        = true
  }
  tags = local.tags
}

resource "aws_sns_topic" "banner_processed" {
  name = "${var.project}-banner-processed"
  tags = local.tags
}

resource "aws_sns_topic_subscription" "alerts" {
  topic_arn = aws_sns_topic.banner_processed.arn
  protocol  = "email"
  endpoint  = var.alert_email
}
