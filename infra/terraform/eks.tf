data "aws_iam_policy_document" "eks_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["eks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "eks_cluster" {
  name               = "${var.project}-eks-cluster"
  assume_role_policy = data.aws_iam_policy_document.eks_assume.json
  tags               = local.tags
}

resource "aws_iam_role_policy_attachment" "eks_cluster" {
  role       = aws_iam_role.eks_cluster.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

resource "aws_eks_cluster" "platform" {
  name     = "${var.project}-production"
  role_arn = aws_iam_role.eks_cluster.arn
  vpc_config {
    subnet_ids              = aws_subnet.private[*].id
    security_group_ids      = [aws_security_group.eks_cluster.id]
    endpoint_private_access = true
    endpoint_public_access  = true
  }
  enabled_cluster_log_types = ["api", "audit", "authenticator", "controllerManager", "scheduler"]
  depends_on                = [aws_iam_role_policy_attachment.eks_cluster]
  tags                      = local.tags
}

data "aws_iam_policy_document" "node_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "eks_node" {
  name               = "${var.project}-eks-node"
  assume_role_policy = data.aws_iam_policy_document.node_assume.json
  tags               = local.tags
}

resource "aws_iam_role_policy_attachment" "eks_worker" {
  for_each = toset([
    "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy",
    "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy",
    "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly",
  ])
  role       = aws_iam_role.eks_node.name
  policy_arn = each.value
}

resource "aws_eks_node_group" "platform" {
  cluster_name    = aws_eks_cluster.platform.name
  node_group_name = "platform"
  node_role_arn   = aws_iam_role.eks_node.arn
  subnet_ids      = aws_subnet.private[*].id
  instance_types  = ["m5.large"]
  capacity_type   = "ON_DEMAND"
  scaling_config {
    desired_size = 3
    min_size     = 3
    max_size     = 8
  }
  update_config { max_unavailable = 1 }
  depends_on = [aws_iam_role_policy_attachment.eks_worker]
  tags       = local.tags
}

resource "aws_eks_addon" "vpc_cni" {
  cluster_name = aws_eks_cluster.platform.name
  addon_name   = "vpc-cni"
  depends_on   = [aws_eks_node_group.platform]
}
resource "aws_eks_addon" "coredns" {
  cluster_name = aws_eks_cluster.platform.name
  addon_name   = "coredns"
  depends_on   = [aws_eks_node_group.platform]
}
resource "aws_eks_addon" "kube_proxy" {
  cluster_name = aws_eks_cluster.platform.name
  addon_name   = "kube-proxy"
  depends_on   = [aws_eks_node_group.platform]
}
resource "aws_eks_addon" "pod_identity" {
  cluster_name = aws_eks_cluster.platform.name
  addon_name   = "eks-pod-identity-agent"
  depends_on   = [aws_eks_node_group.platform]
}

resource "aws_ecr_repository" "services" {
  for_each             = toset(["api-gateway", "user-service", "event-service", "ticket-service", "payment-service", "notification-service", "analytics-service", "config-server"])
  name                 = "${var.project}/${each.value}"
  image_tag_mutability = "IMMUTABLE"
  image_scanning_configuration { scan_on_push = true }
  encryption_configuration { encryption_type = "AES256" }
  tags = local.tags
}

resource "aws_ecr_lifecycle_policy" "services" {
  for_each   = aws_ecr_repository.services
  repository = each.value.name
  policy     = jsonencode({ rules = [{ rulePriority = 1, description = "Keep the latest 30 build images", selection = { tagStatus = "tagged", tagPrefixList = ["sha-"], countType = "imageCountMoreThan", countNumber = 30 }, action = { type = "expire" } }] })
}
