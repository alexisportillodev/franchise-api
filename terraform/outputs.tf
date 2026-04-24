output "table_name" {
  description = "Nombre de la tabla DynamoDB."
  value       = aws_dynamodb_table.franchise.name
}

output "table_arn" {
  description = "ARN de la tabla DynamoDB."
  value       = aws_dynamodb_table.franchise.arn
}

output "app_access_key_id" {
  description = "AWS Access Key ID para la app (úsala en application.properties)."
  value       = aws_iam_access_key.franchise_app.id
}

output "app_secret_access_key" {
  description = "AWS Secret Key para la app — guárdala en un lugar seguro."
  value       = aws_iam_access_key.franchise_app.secret
  sensitive   = true
}
