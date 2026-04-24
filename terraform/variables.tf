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
