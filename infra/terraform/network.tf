resource "aws_vpc" "platform" {
  cidr_block           = "10.32.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true
  tags                 = merge(local.tags, { Name = "${var.project}-production" })
}

resource "aws_internet_gateway" "platform" {
  vpc_id = aws_vpc.platform.id
  tags   = merge(local.tags, { Name = "${var.project}-igw" })
}

resource "aws_subnet" "public" {
  count                   = 3
  vpc_id                  = aws_vpc.platform.id
  availability_zone       = local.azs[count.index]
  cidr_block              = cidrsubnet("10.32.0.0/16", 8, count.index)
  map_public_ip_on_launch = true
  tags = merge(local.tags, {
    Name                     = "${var.project}-public-${local.azs[count.index]}"
    "kubernetes.io/role/elb" = "1"
  })
}

resource "aws_subnet" "private" {
  count             = 3
  vpc_id            = aws_vpc.platform.id
  availability_zone = local.azs[count.index]
  cidr_block        = cidrsubnet("10.32.0.0/16", 8, count.index + 10)
  tags = merge(local.tags, {
    Name                              = "${var.project}-private-${local.azs[count.index]}"
    "kubernetes.io/role/internal-elb" = "1"
  })
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.platform.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.platform.id
  }
  tags = merge(local.tags, { Name = "${var.project}-public" })
}

resource "aws_route_table_association" "public" {
  count          = 3
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_eip" "nat" {
  count  = 3
  domain = "vpc"
  tags   = merge(local.tags, { Name = "${var.project}-nat-${count.index + 1}" })
}

resource "aws_nat_gateway" "platform" {
  count         = 3
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id
  depends_on    = [aws_internet_gateway.platform]
  tags          = merge(local.tags, { Name = "${var.project}-nat-${count.index + 1}" })
}

resource "aws_route_table" "private" {
  count  = 3
  vpc_id = aws_vpc.platform.id
  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.platform[count.index].id
  }
  tags = merge(local.tags, { Name = "${var.project}-private-${count.index + 1}" })
}

resource "aws_route_table_association" "private" {
  count          = 3
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private[count.index].id
}

resource "aws_security_group" "eks_cluster" {
  name        = "${var.project}-eks-cluster"
  description = "Control plane access for EKS"
  vpc_id      = aws_vpc.platform.id
  ingress {
    description = "HTTPS from worker nodes"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    self        = true
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = local.tags
}

resource "aws_security_group" "eks_nodes" {
  name        = "${var.project}-eks-nodes"
  description = "Application nodes"
  vpc_id      = aws_vpc.platform.id
  ingress {
    description = "Pod and node traffic"
    from_port   = 0
    to_port     = 65535
    protocol    = "tcp"
    self        = true
  }
  ingress {
    description     = "Kubernetes API"
    from_port       = 443
    to_port         = 443
    protocol        = "tcp"
    security_groups = [aws_security_group.eks_cluster.id]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = local.tags
}

resource "aws_security_group" "database" {
  name        = "${var.project}-database"
  description = "PostgreSQL access from EKS only"
  vpc_id      = aws_vpc.platform.id
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    # Managed EKS nodes currently receive the cluster security group. Keep the
    # node SG as well so either supported node networking layout can reach RDS.
    security_groups = [aws_security_group.eks_nodes.id, aws_security_group.eks_cluster.id]
  }
  tags = local.tags
}

resource "aws_security_group" "kafka" {
  name        = "${var.project}-kafka"
  description = "Kafka access from EKS only"
  vpc_id      = aws_vpc.platform.id
  ingress {
    from_port       = 9098
    to_port         = 9098
    protocol        = "tcp"
    security_groups = [aws_security_group.eks_nodes.id]
  }
  tags = local.tags
}
