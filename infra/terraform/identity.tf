data "tls_certificate" "github" { url = "https://token.actions.githubusercontent.com" }

resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.github.certificates[0].sha1_fingerprint]
  tags            = local.tags
}

data "aws_iam_policy_document" "github_actions_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values = [
        "repo:${split("/", var.github_repository)[0]}@${var.github_repository_owner_id}/${split("/", var.github_repository)[1]}@${var.github_repository_id}:ref:refs/heads/main"
      ]
    }
  }
}

resource "aws_iam_role" "github_actions" {
  name               = "${var.project}-github-actions-production"
  assume_role_policy = data.aws_iam_policy_document.github_actions_assume.json
  tags               = local.tags
}

# Terraform needs authority to reconcile the production resources. The trust policy above
# limits this role to OIDC tokens minted for the main branch of the named repository.
resource "aws_iam_role_policy_attachment" "github_actions" {
  role       = aws_iam_role.github_actions.name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess"
}

resource "aws_eks_access_entry" "github_actions" {
  cluster_name  = aws_eks_cluster.platform.name
  principal_arn = aws_iam_role.github_actions.arn
  type          = "STANDARD"
}

resource "aws_eks_access_policy_association" "github_actions" {
  cluster_name  = aws_eks_cluster.platform.name
  principal_arn = aws_iam_role.github_actions.arn
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"
  access_scope { type = "cluster" }
  depends_on = [aws_eks_access_entry.github_actions]
}

data "aws_iam_policy_document" "event_service_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["pods.eks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "event_service" {
  name               = "${var.project}-event-service"
  assume_role_policy = data.aws_iam_policy_document.event_service_assume.json
  tags               = local.tags
}

resource "aws_iam_role_policy" "event_service" {
  role = aws_iam_role.event_service.id
  policy = jsonencode({ Version = "2012-10-17", Statement = [
    { Effect = "Allow", Action = ["s3:PutObject", "s3:AbortMultipartUpload"], Resource = "${aws_s3_bucket.banners.arn}/*" },
    { Effect = "Allow", Action = ["dynamodb:PutItem", "dynamodb:Query"], Resource = aws_dynamodb_table.history.arn }
  ] })
}

resource "aws_eks_pod_identity_association" "event_service" {
  cluster_name    = aws_eks_cluster.platform.name
  namespace       = "pulse-tickets"
  service_account = "event-service"
  role_arn        = aws_iam_role.event_service.arn
  depends_on      = [aws_eks_addon.pod_identity]
}
