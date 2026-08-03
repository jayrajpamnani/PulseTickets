resource "aws_db_subnet_group" "platform" {
  name       = "${var.project}-database"
  subnet_ids = aws_subnet.private[*].id
  tags       = local.tags
}

resource "random_password" "database" {
  for_each         = toset(["users", "events", "tickets", "payments"])
  length           = 32
  special          = true
  override_special = "_!%"
}

resource "aws_db_instance" "service" {
  for_each                        = toset(["users", "events", "tickets", "payments"])
  identifier                      = "${var.project}-${each.value}-production"
  engine                          = "postgres"
  instance_class                  = "db.t4g.medium"
  allocated_storage               = 30
  max_allocated_storage           = 100
  storage_type                    = "gp3"
  storage_encrypted               = true
  multi_az                        = true
  db_name                         = each.value
  username                        = "pulseadmin"
  password                        = random_password.database[each.value].result
  port                            = 5432
  db_subnet_group_name            = aws_db_subnet_group.platform.name
  vpc_security_group_ids          = [aws_security_group.database.id]
  publicly_accessible             = false
  backup_retention_period         = 7
  deletion_protection             = true
  skip_final_snapshot             = false
  final_snapshot_identifier       = "${var.project}-${each.value}-final"
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
  auto_minor_version_upgrade      = true
  copy_tags_to_snapshot           = true
  tags                            = merge(local.tags, { Service = each.value })
}

resource "aws_msk_cluster" "platform" {
  cluster_name           = "${var.project}-production"
  kafka_version          = "3.7.x"
  number_of_broker_nodes = 3
  broker_node_group_info {
    instance_type   = "kafka.m5.large"
    client_subnets  = aws_subnet.private[*].id
    security_groups = [aws_security_group.kafka.id]
    storage_info {
      ebs_storage_info { volume_size = 100 }
    }
  }
  encryption_info {
    encryption_in_transit {
      client_broker = "PLAINTEXT"
      in_cluster    = true
    }
  }
  logging_info {
    broker_logs {
      cloudwatch_logs {
        enabled   = true
        log_group = aws_cloudwatch_log_group.msk.name
      }
    }
  }
  tags = local.tags
}

resource "aws_cloudwatch_log_group" "msk" {
  name              = "/aws/msk/${var.project}-production"
  retention_in_days = 30
  tags              = local.tags
}

resource "random_password" "jwt" {
  length  = 64
  special = false
}
resource "random_password" "admin" {
  length           = 24
  special          = true
  override_special = "_!%"
}

resource "aws_secretsmanager_secret" "platform" {
  name                    = "${var.project}/production/platform"
  recovery_window_in_days = 7
  tags                    = local.tags
}

resource "aws_secretsmanager_secret_version" "platform" {
  secret_id = aws_secretsmanager_secret.platform.id
  secret_string = jsonencode({
    JWT_SECRET                 = random_password.jwt.result
    ADMIN_USERNAME             = var.admin_username
    ADMIN_EMAIL                = var.admin_email
    ADMIN_PASSWORD             = random_password.admin.result
    DATABASE_USER              = "pulseadmin"
    DATABASE_PASSWORD_USERS    = random_password.database["users"].result
    DATABASE_PASSWORD_EVENTS   = random_password.database["events"].result
    DATABASE_PASSWORD_TICKETS  = random_password.database["tickets"].result
    DATABASE_PASSWORD_PAYMENTS = random_password.database["payments"].result
  })
}
