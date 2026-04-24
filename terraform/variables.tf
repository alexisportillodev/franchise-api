variable "aws_region" {
  description = "Región AWS"
  type        = string
  default     = "us-east-2"
}

variable "table_name" {
  description = "Nombre de la tabla DynamoDB"
  type        = string
  default     = "franchise"
}
