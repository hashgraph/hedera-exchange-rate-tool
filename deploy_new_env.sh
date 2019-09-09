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

echo "Required parameters provided"

read -s -p "Enter database password (at least 8 characters): " PASSWORD
echo
read -s -p "Enter operator key: " OPERATOR_KEY
echo

DATABASE_NAME="$DATABASE_NAME$NAME"

echo "Creating database instance ${DATABASE_NAME}"

#aws rds create-db-instance \
#    --allocated-storage 100 \
#    --max-allocated-storage 500 \
#    --db-instance-class db.m5.xlarge \
#    --db-instance-identifier "$DATABASE_NAME" \
#    --engine postgres \
#    --enable-cloudwatch-logs-exports '["postgresql","upgrade"]' \
#    --master-username "$USERNAME" \
#    --master-user-password "$PASSWORD" \
#    --db-name exchangeRate \
#    --port 5432 \
#    --engine-version 11.4 \
#    --storage-type gp2 \
#    --copy-tags-to-snapshot \
#    --enable-iam-database-authentication \
#    --enable-performance-insights \
#    --publicly-accessible \
#    --region us-east-1
#
#echo "Waiting for database ${DATABASE_NAME} to become available"
#
#aws rds wait db-instance-available \
#    --db-instance-identifier "${DATABASE_NAME}"  \
#    --region us-east-1
#
#echo "Retrieving endpoint for database ${DATABASE_NAME}"
#
#DATABASE_ENDPOINT=$(aws rds describe-db-instances  \
#                        --db-instance-identifier "$DATABASE_NAME" \
#                        --region us-east-1 \
#                        --query 'DBInstances[0].Endpoint.Address' \
#                        --output text)

echo "${DATABASE_NAME} has endpoint ${DATABASE_ENDPOINT}"

echo "Building jar to deploy"
#mvn package

echo "Encrypting password"

KMS_KEY_ID="b475550c-0a43-440e-bf05-d045d6ce3803"


ENCRYPTED_PASSWORD=$(aws kms encrypt \
              --key-id "${KMS_KEY_ID}" \
              --region us-east-1 \
              --plaintext "${PASSWORD}" \
              --output text \
              --query CiphertextBlob)

echo "Encrypting operator key"

ENCRYPTED_OPERATOR_KEY=$(aws kms encrypt \
              --key-id "${KMS_KEY_ID}" \
              --region us-east-1 \
              --plaintext "${OPERATOR_KEY}" \
              --output text \
              --query CiphertextBlob)

JDBC_ENDPOINT="jdbc:postgresql://${DATABASE_ENDPOINT}:5432/"

echo "Encrypting JDBC Endpoint"

ENCRYPTED_JDBC_ENDPOINT=$(aws kms encrypt \
              --key-id "${KMS_KEY_ID}" \
              --region us-east-1 \
              --plaintext "${JDBC_ENDPOINT}" \
              --output text \
              --query CiphertextBlob)

echo "Encrypting username"

ENCRYPTED_USERNAME=$(aws kms encrypt \
              --key-id "${KMS_KEY_ID}" \
              --region us-east-1 \
              --plaintext "${USERNAME}" \
              --output text \
              --query CiphertextBlob)

echo "Encrypting database name"

ENCRYPTED_DATABASE=$(aws kms encrypt \
              --key-id "${KMS_KEY_ID}" \
              --region us-east-1 \
              --plaintext exchangeRate \
              --output text \
              --query CiphertextBlob)

LAMBDA_NAME="exchange-rate-tool-lambda-$NAME"

echo "Creating lambda ${LAMBDA_NAME}"


LAMBDA_ARN=$(aws lambda create-function \
              --function-name "$LAMBDA_NAME" \
              --runtime java8 \
              --handler com.hedera.services.exchange.ExchangeRateTool::main \
              --publish \
              --memory-size 1024 \
              --role arn:aws:iam::772706802921:role/service-role/test \
              --kms-key-arn arn:aws:kms:us-east-1:772706802921:key/b475550c-0a43-440e-bf05-d045d6ce3803 \
              --timeout 60 \
              --zip-file fileb://./target/Exchange-Rate-Tool.jar \
              --environment "Variables={DATABASE=${ENCRYPTED_DATABASE},ENDPOINT=${ENCRYPTED_JDBC_ENDPOINT},OPERATOR_KEY=${ENCRYPTED_OPERATOR_KEY},USERNAME=${ENCRYPTED_USERNAME},PASSWORD=${ENCRYPTED_PASSWORD}}" \
              --region us-east-1 \
              --output text \
              --query 'FunctionArn')

echo "Lambda ${LAMBDA_NAME} created with ARN: ${LAMBDA_ARN}"

SCHEDULER_NAME="exchange-rate-tool-scheduler-$NAME"
echo "Creating Scheduler ${SCHEDULER_NAME}"

RULE_ARN=$(aws events put-rule \
            --name "$SCHEDULER_NAME" \
            --schedule-expression 'rate(60 minutes)' \
            --state ENABLED \
            --description "Executes exchange rate tool ${LAMBDA_NAME}" \
            --region us-east-1 \
            --output text)

echo "Adding permissions so ${SCHEDULER_NAME} can execute ${LAMBDA_NAME}"

aws lambda add-permission \
      --function-name "${LAMBDA_NAME}" \
      --statement-id "${SCHEDULER_NAME}" \
      --action 'lambda:InvokeFunction' \
      --principal events.amazonaws.com \
      --source-arn "${RULE_ARN}" \
      --region us-east-1 \
      --output text

echo "Creating target for rule ${SCHEDULER_NAME}"

aws events put-targets \
    --rule "${SCHEDULER_NAME}" \
    --targets "Id"="1","Arn"="${LAMBDA_ARN}" \
    --region us-east-1


LAMBDA_API_NAME="exchange-rate-tool-lambda-api-$NAME"

echo "Creating lambda ${LAMBDA_API_NAME} for exchange rate api"

LAMBDA_API_ARN=$(aws lambda create-function \
              --function-name "$LAMBDA_API_NAME" \
              --runtime java8 \
              --handler com.hedera.services.exchange.ExchangeRateApi::getLatest \
              --publish \
              --memory-size 1024 \
              --role arn:aws:iam::772706802921:role/service-role/test \
              --kms-key-arn arn:aws:kms:us-east-1:772706802921:key/b475550c-0a43-440e-bf05-d045d6ce3803 \
              --timeout 60 \
              --zip-file fileb://./target/Exchange-Rate-Tool.jar \
              --environment "Variables={DATABASE=exchangeRate,ENDPOINT=${JDBC_ENDPOINT},USERNAME=${USERNAME},PASSWORD=${USERNAME}}" \
              --region us-east-1 \
              --output text \
              --query 'FunctionArn')

API_GATEWAY="exchange-rate-api-gateway-$NAME"

echo "Creating api gateway ${API_GATEWAY}"

API_GATEWAY_ID=$(aws apigateway create-rest-api \
      --name "${API_GATEWAY}" \
      --region us-east-1 \
      --output text \
      --query 'id')

echo "API Gateway ${API_GATEWAY} has ID ${API_GATEWAY_ID}"

ROOT_RESOURCE_ID=$(aws apigateway get-resources \
                    --rest-api-id "${API_GATEWAY_ID}" \
                    --region us-east-1 \
                    --output text \
                    --query 'items[0].id')

echo "Root resource id: ${ROOT_RESOURCE_ID} for Api Gateway ${API_GATEWAY} with id ${API_GATEWAY_ID}"

echo "Creating Api Gateway resource with API Gateway Id ${API_GATEWAY_ID}"

RESOURCE_ID=$(aws apigateway create-resource \
          --rest-api-id "${API_GATEWAY_ID}" \
          --region us-east-1 \
          --parent-id  "${ROOT_RESOURCE_ID}" \
          --path-part pricing \
          --output text \
          --query 'id')

echo "Creating the GET method"

aws apigateway put-method \
          --rest-api-id "${API_GATEWAY_ID}" \
          --region us-east-1 \
          --resource-id "${RESOURCE_ID}" \
          --http-method GET \
          --authorization-type "NONE"

echo "Integrating API Gateway with Lambda ${LAMBDA_API_ARN}"

aws apigateway put-integration \
          --region us-east-1 \
          --rest-api-id "${API_GATEWAY_ID}" \
          --resource-id "${RESOURCE_ID}" \
          --http-method GET \
          --type AWS \
          --integration-http-method POST \
          --uri "arn:aws:apigateway:us-east-1:lambda:path/2015-03-31/functions/${LAMBDA_API_ARN}/invocations"

echo "Setting status code to 200"

aws apigateway put-integration-response \
          --region us-east-1 \
          --rest-api-id "${API_GATEWAY_ID}" \
          --resource-id "${RESOURCE_ID}" \
          --http-method GET \
          --status-code 200 \
          --selection-pattern ""

API_GATEWAY_STAGE="exchange-rate-tool-api-stage-$NAME";

echo "Deploying Api Gateway to stage ${API_GATEWAY_STAGE}"

aws apigateway create-deployment \
          --rest-api-id "${API_GATEWAY_ID}" \
          --stage-name default \
          --region us-east-1


API_URL="https://${API_GATEWAY_ID}.execute-api.us-east-1.amazonaws.com/default/pricing"

echo "Test pricing API with URL ${API_URL}"









