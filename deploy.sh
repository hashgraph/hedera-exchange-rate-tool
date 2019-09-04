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
