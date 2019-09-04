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
  esac
done

if [ -z "$NAME" ]; then
  echo "You must provide a name with the -n/--name option"
  exit 1
fi

DATABASE_NAME="$DATABASE_NAME$NAME"

echo "Creating database instance ${DATABASE_NAME}"

#aws rds create-db-instance \
#    --allocated-storage 100 \
#    --db-instance-class db.m1.small \
#    --db-instance-identifier

