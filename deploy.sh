#!/bin/bash

if ! [ -x "$(command -v aws)" ]; then
  echo 'AWS CLI not installed. Proceeding to install it'

  echo 'Downloading AWS CLI'
  curl "https://s3.amazonaws.com/aws-cli/awscli-bundle.zip" -o "awscli-bundle.zip"

  echo 'Unzipping AWS CLI'
  unzip awscli-bundle.zip

  echo 'Installing AWS CLI'
  sudo ./awscli-bundle/install -i /usr/local/aws -b /usr/local/bin/aws

  echo 'Deleting downloaded file'
  rm awscli-bundle.zip
  rm -rf awscli-bundle

  echo 'Verifying AWS version'
  aws --version
fi

echo 'AWS CLI installed. Proceeding normally.'

NAME=""
USERNAME=""
PASSWORD=""
OPERATOR_KEY=""
DATABASE_NAME="exchange-rate-tool-db-"

while [[ $# -gt 0 ]]
do
  key="$1"

  case $key in
      -n|--name)
      NAME="$2"
      shift
      shift
      ;;
      -u|--username)
      USERNAME="$2"
      shift
      shift
      ;;
  esac
done

if [ -z "$NAME" ]; then
  echo "You must provide a name with the -n/--name option"
  exit 1
fi

if [ -z "$USERNAME" ]; then
  echo "You must provide a username with the -u/--username option"
  exit 1
fi

read -s -p "Enter database password (at least 8 characters): " PASSWORD

read -s -p "Enter operator key: " OPERATOR_KEY

DATABASE_NAME="$DATABASE_NAME$NAME"

echo "Creating database instance ${DATABASE_NAME}"

aws rds create-db-instance \
    --allocated-storage 100 \
    --max-allocated-storage 500 \
    --db-instance-class db.m5.xlarge \
    --db-instance-identifier "$DATABASE_NAME" \
    --engine postgres \
    --enable-cloudwatch-logs-exports '["postgresql","upgrade"]' \
    --master-username "$USERNAME" \
    --master-user-password "$PASSWORD" \
    --db-name exchangeRate \
    --port 5432 \
    --engine-version 11.4 \
    --storage-type gp2 \
    --copy-tags-to-snapshot \
    --enable-iam-database-authentication \
    --enable-performance-insights \
    --publicly-accessible \
    --region us-east-1

echo "Waiting for database ${DATABASE_NAME} to become available"

aws rds wait db-instance-available \
    --db-instance-identifier "${DATABASE_NAME}"  \
    --region us-east-1

echo "Retrieving endpoint for database ${DATABASE_NAME}"

DATABASE_ENDPOINT=$(aws rds describe-db-instances  --db-instance-identifier "$DATABASE_NAME" --region us-east-1 --query 'DBInstances[0].Endpoint.Address' --output text)

echo "${DATABASE_NAME} has endpoint ${DATABASE_ENDPOINT}"

echo "Building jar to deploy"
mvn package

LAMBDA_NAME="exchange-rate-tool-lambda-$NAME"

echo "Creating lambda ${LAMBDA_NAME}"

JDBC_ENDPOINT="jdbc:postgresql://${DATABASE_ENDPOINT}:5432/"

aws lambda create-function \
    --function-name "$LAMBDA_NAME" \
    --runtime java8 \
    --handler com.hedera.services.exchange.ExchangeRateTool::main \
    --publish \
    --memory-size 1024 \
    --role service-role/test \
    --kms-key-arn arn:aws:kms:us-east-1:772706802921:key/b475550c-0a43-440e-bf05-d045d6ce3803 \
    --timeout 60 \
    --zip-file fileb://./target/Exchange-Rate-Tool.jar \
    --environment Variables={DATABASE=exchangeRate,ENDPOINT="${JDBC_ENDPOINT}",OPERATOR_KEY="${OPERATOR_KEY}",USERNAME="${USERNAME}",PASSWORD="${USERNAME}"}








