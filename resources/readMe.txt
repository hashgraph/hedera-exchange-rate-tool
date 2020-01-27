# when the keys change use tools-cli to get the pivate key given the pem file and the pass phrase

unzip the tool-cli.zip.

cd into the unziped folder

run the follwoing command:

./launch.sh convert-key -f {pem file name}


This will prompt for the pass phrase. Enter it and you will get the public and private key pair of the pem file. 
Use the private key as OPERATOR_KEY on the lambda function.
