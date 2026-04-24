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

output "table_name" {
  description = "Nombre de la tabla DynamoDB."
  value       = aws_dynamodb_table.franchise.name
}

output "table_arn" {
  description = "ARN de la tabla DynamoDB."
  value       = aws_dynamodb_table.franchise.arn
}

variable "aws_region" {
  description = "Región AWS donde se crea la tabla."
  type        = string
  default     = "us-east-2"
}

variable "table_name" {
  description = "Nombre de la tabla DynamoDB."
  type        = string
  default     = "franchise"
}
