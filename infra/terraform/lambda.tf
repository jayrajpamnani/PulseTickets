data "aws_iam_policy_document" "lambda_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "banner_lambda" {
  name               = "${var.project}-banner-lambda"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume.json
}

resource "aws_iam_role_policy" "banner_lambda" {
  role = aws_iam_role.banner_lambda.id
  policy = jsonencode({ Version = "2012-10-17", Statement = [
    { Effect = "Allow", Action = ["s3:GetObject", "s3:GetObjectAttributes"], Resource = "${aws_s3_bucket.banners.arn}/*" },
    { Effect = "Allow", Action = ["dynamodb:PutItem"], Resource = aws_dynamodb_table.metadata.arn },
    { Effect = "Allow", Action = ["sns:Publish"], Resource = aws_sns_topic.banner_processed.arn },
    { Effect = "Allow", Action = ["logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"], Resource = "*" }
  ] })
}

resource "aws_lambda_function" "banner" {
  function_name    = "${var.project}-banner-processor"
  role             = aws_iam_role.banner_lambda.arn
  handler          = "com.antra.banner.BannerHandler::handleRequest"
  runtime          = "java21"
  filename         = "../../banner-processor/target/banner-processor-1.0.0.jar"
  source_code_hash = filebase64sha256("../../banner-processor/target/banner-processor-1.0.0.jar")
  timeout          = 30
  memory_size      = 1024
  environment { variables = { METADATA_TABLE = aws_dynamodb_table.metadata.name, TOPIC_ARN = aws_sns_topic.banner_processed.arn } }
  tags = local.tags
}

resource "aws_lambda_permission" "s3" {
  statement_id  = "AllowS3Invoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.banner.function_name
  principal     = "s3.amazonaws.com"
  source_arn    = aws_s3_bucket.banners.arn
}

resource "aws_s3_bucket_notification" "banners" {
  bucket = aws_s3_bucket.banners.id
  lambda_function {
    lambda_function_arn = aws_lambda_function.banner.arn
    events              = ["s3:ObjectCreated:*"]
  }
  depends_on = [aws_lambda_permission.s3]
}
