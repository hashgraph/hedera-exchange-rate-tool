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
      -p|--password)
      PASSWORD=="$2"
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

if [ -z "$PASSWORD" ]; then
  echo "You must provide a password with the -p/--password option"
  exit 1
fi

TAG="exchange-rate-tool$NAME"
DATABASE_NAME="$DATABASE_NAME$NAME"

echo "Creating database instance ${DATABASE_NAME}"

aws rds create-db-instance \
    --allocated-storage 100 \
    --max-allocated-storage 500 \
    --db-instance-class db.m1.small \
    --db-instance-identifier "$DATABASE_NAME" \
    --engine PostgreSQL \
    --enable-cloudwatch-logs-exports '["audit","error","general","slowquery"]' \
    --master-username "$USERNAME" \
    --master-user-password "$PASSWORD" \
    --db-name exchangeRate \
    --port 5432 \
    --engine-version \
    --engine-version 10.6 \
    --tags "$TAG" \
    --storage-type gp2 \
    --copy-tags-to-snapshot \
    --enable-iam-database-authentication \
    --enable-performance-insights \
    --availability-zone us-east-1 \
    --publicly-accessible \
    > database-deploy.json

echo "Waiting for database ${DATABASE_NAME} to become available"

aws rds wait db-instance-available \
    --db-instance-identifier exchange-rate04 \
    --region us-east-1

echo "Retrieving endpoint for database ${DATABASE_NAME}"

ENDPOINT=`aws rds describe-db-instances  --db-instance-identifier exchange-rate04 --region us-east-1 --query 'DBInstances[0].Endpoint.Address' --output text`

echo "${DATABASE_NAME} has endpoint ${ENDPOINT}"


