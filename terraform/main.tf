terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# ======================
# DynamoDB
# ======================
resource "aws_dynamodb_table" "franchise" {
  name         = var.table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "PK"
  range_key    = "SK"

  attribute {
    name = "PK"
    type = "S"
  }

  attribute {
    name = "SK"
    type = "S"
  }

  global_secondary_index {
    name            = "GSI_SK_PK"
    hash_key        = "SK"
    range_key       = "PK"
    projection_type = "ALL"
  }

  tags = {
    Application = "franchise-api"
    ManagedBy   = "terraform"
  }
}

# ======================
# IAM ROLE (EC2 → DynamoDB)
# ======================
resource "aws_iam_role" "ec2_role" {
  name = "franchise-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy" "dynamodb_policy" {
  name = "franchise-dynamodb-policy"
  role = aws_iam_role.ec2_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "dynamodb:PutItem",
        "dynamodb:GetItem",
        "dynamodb:Query",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem"
      ]
      Resource = aws_dynamodb_table.franchise.arn
    }]
  })
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "franchise-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

# ======================
# SECURITY GROUP
# ======================
resource "aws_security_group" "ec2_sg" {
  name = "franchise-api-sg"

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# ======================
# AMI
# ======================
data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }
}

# ======================
# EC2
# ======================
resource "aws_instance" "app_server" {
  ami           = data.aws_ami.amazon_linux.id
  instance_type = "t3.micro"

  iam_instance_profile = aws_iam_instance_profile.ec2_profile.name
  vpc_security_group_ids = [aws_security_group.ec2_sg.id]

  user_data = <<-EOF
              #!/bin/bash
              yum update -y
              amazon-linux-extras install docker -y
              service docker start
              usermod -a -G docker ec2-user

              docker run -d -p 8080:8080 \
              -e AWS_REGION=${var.aws_region} \
              alexisportillo95/franchise-api:latest
              EOF

  tags = {
    Name = "franchise-api"
  }
}
