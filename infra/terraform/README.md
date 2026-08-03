# AWS deployment

This Terraform layer provisions only the required serverless AWS workflow: S3 event-banner storage, Lambda metadata processing, DynamoDB metadata/history tables, SNS email notifications, and IAM least-privilege policies. The microservices, PostgreSQL, Kafka, and Kubernetes manifests run locally for the capstone demonstration; this avoids EKS, MSK, RDS, and ECR costs while retaining every required technology.

Before applying, build the Lambda (`mvn -pl banner-processor package`), configure an AWS CLI identity, and create `terraform.tfvars` containing the AWS region, project name, and `alert_email`. Review the plan carefully:

```sh
terraform init
terraform plan -out tfplan
terraform apply tfplan
```

Confirm the SNS subscription email before testing an image upload. The local Docker Compose stack demonstrates PostgreSQL and Kafka; the Kubernetes manifests can be applied to kind or minikube.
