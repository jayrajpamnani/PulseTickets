output "banner_bucket" { value = aws_s3_bucket.banners.id }
output "banner_cdn_url" { value = "https://${aws_cloudfront_distribution.banners.domain_name}" }
output "frontend_url" { value = try("https://${aws_cloudfront_distribution.web[0].domain_name}", "") }
output "eks_cluster_name" { value = aws_eks_cluster.platform.name }
output "ecr_repository_urls" { value = { for name, repo in aws_ecr_repository.services : name => repo.repository_url } }
output "github_actions_role_arn" { value = aws_iam_role.github_actions.arn }
output "deployment_secret_arn" { value = aws_secretsmanager_secret.platform.arn }
output "database_endpoints" { value = { for name, instance in aws_db_instance.service : name => instance.address } }
output "kafka_bootstrap_servers" { value = aws_msk_cluster.platform.bootstrap_brokers }
output "frontend_distribution_id" { value = try(aws_cloudfront_distribution.web[0].id, "") }
