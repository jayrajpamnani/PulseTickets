variable "aws_region" { type = string }
variable "project" { type = string }
variable "alert_email" { type = string }

variable "github_repository" {
  description = "GitHub owner/repository permitted to assume the deployment role."
  type        = string
  default     = "jayrajpamnani/PulseTickets"
}

variable "admin_email" {
  description = "Bootstrap administrator email. The password is generated in Secrets Manager."
  type        = string
  default     = "jayrajpamnani1610@gmail.com"
}

variable "admin_username" {
  type    = string
  default = "admin"
}

variable "api_origin_domain" {
  description = "Public NLB hostname created by the EKS API Gateway Service. Supplied by CD after the first cluster deployment."
  type        = string
  default     = ""
}
