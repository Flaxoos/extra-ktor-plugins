#!/bin/bash
# Generate certificates

## ROOT and CA

# === 1. Cleanup Previously Generated Files ===
echo "=== Cleaning Up Previously Generated Files ==="

# Define the name of this script to exclude it from deletion
SCRIPT_NAME="generate_certificates.sh"

# Find and delete all files in the current directory except the script itself
find . -maxdepth 1 -type f ! -name "$SCRIPT_NAME" -exec rm -f {} +

echo "=== Cleanup Completed ==="
keytool \
    -genkeypair \
    -alias root \
    -dname "cn=localhost" \
    -validity 18250 \
    -keyalg RSA \
    -keysize 4096 \
    -ext bc:c \
    -keystore root.jks \
    -keypass test_password \
    -storepass test_password

keytool \
    -genkeypair \
    -alias ca \
    -dname "cn=localhost" \
    -validity 18250 \
    -keyalg RSA \
    -keysize 4096 \
    -ext bc:c \
    -keystore ca.jks \
    -keypass test_password \
    -storepass test_password


### Generate root certificate


keytool \
    -exportcert \
    -rfc \
    -keystore root.jks \
    -alias root \
    -storepass test_password \
    -file root.pem


### Generate a certificate for ca signed by root (root -> ca)


keytool \
    -keystore ca.jks \
    -storepass test_password \
    -certreq \
    -alias ca \
    -file ca.csr

keytool \
    -keystore root.jks \
    -storepass test_password \
    -gencert \
    -alias root \
    -ext bc=0 \
    -ext san=dns:ca \
    -rfc \
    -infile ca.csr \
    -outfile ca.pem


### Import ca cert chain into ca.jks


keytool -keystore ca.jks -storepass test_password -importcert -trustcacerts -noprompt -alias root -file root.pem
keytool -keystore ca.jks -storepass test_password -importcert -alias ca -file ca.pem


## Zookeeper Server

### Generate private keys (for zookeeper)
keytool \
    -genkeypair \
    -alias zookeeper-server \
    -dname cn=zookeeper-server \
    -validity 18250 \
    -keyalg RSA \
    -keysize 4096 \
    -keystore zookeeper.server.keystore.jks \
    -keypass test_password \
    -storepass test_password

### Generate a certificate for server signed by ca (root -> ca -> zookeeper-server)
keytool \
    -keystore zookeeper.server.keystore.jks \
    -storepass test_password \
    -certreq \
    -alias zookeeper-server \
    -file zookeeper-server.csr

keytool \
    -keystore ca.jks \
    -storepass test_password \
    -gencert \
    -alias ca \
    -ext ku:c=dig,keyEnc \
    -ext "san=dns:zookeeper,dns:localhost" \
    -ext eku=sa,ca \
    -rfc \
    -infile zookeeper-server.csr \
    -outfile zookeeper-server.pem

### Import server cert chain into zookeeper.server.keystore.jks
keytool -keystore zookeeper.server.keystore.jks -storepass test_password -importcert -alias zookeeper-server -noprompt -file zookeeper-server.pem

### Import server cert chain into zookeeper.server.truststore.jks
keytool -keystore zookeeper.server.truststore.jks -storepass test_password -importcert -trustcacerts -noprompt -alias root -file root.pem
keytool -keystore zookeeper.server.truststore.jks -storepass test_password -importcert -alias ca -file ca.pem



## Kafka Server

### Generate private keys (for server)


keytool \
    -genkeypair \
    -alias kafka-server \
    -dname cn=kafka-server \
    -validity 18250 \
    -keyalg RSA \
    -keysize 4096 \
    -keystore kafka.server.keystore.jks \
    -keypass test_password \
    -storepass test_password


### Generate a certificate for server signed by ca (root -> ca -> kafka-server)


keytool \
    -keystore kafka.server.keystore.jks \
    -storepass test_password \
    -certreq \
    -alias kafka-server \
    -file kafka-server.csr

keytool \
    -keystore ca.jks \
    -storepass test_password \
    -gencert \
    -alias ca \
    -ext ku:c=dig,keyEnc \
    -ext "san=dns:kafka,dns:localhost" \
    -ext eku=sa,ca \
    -rfc \
    -infile kafka-server.csr \
    -outfile kafka-server.pem


### Import server cert chain into kafka.server.keystore.jks


keytool -keystore kafka.server.keystore.jks -storepass test_password -importcert -alias kafka-server -noprompt -file kafka-server.pem


### Import server cert chain into kafka.server.truststore.jks


keytool -keystore kafka.server.truststore.jks -storepass test_password -importcert -trustcacerts -noprompt -alias root -file root.pem
keytool -keystore kafka.server.truststore.jks -storepass test_password -importcert -alias ca -file ca.pem


## Schema Registry Server

### Generate private keys (for schemaregistry-server)


keytool \
    -genkeypair \
    -alias schemaregistry-server \
    -dname cn=schemaregistry-server \
    -validity 18250 \
    -keyalg RSA \
    -keysize 4096 \
    -keystore schemaregistry.server.keystore.jks \
    -keypass test_password \
    -storepass test_password


### Generate a certificate for server signed by ca (root -> ca -> schemaregistry-server)


keytool \
    -keystore schemaregistry.server.keystore.jks \
    -storepass test_password \
    -certreq \
    -alias schemaregistry-server \
    -file schemaregistry-server.csr

keytool \
    -keystore ca.jks \
    -storepass test_password \
    -gencert \
    -alias ca \
    -ext ku:c=dig,keyEnc \
    -ext "san=dns:schemaregistry,dns:localhost" \
    -ext eku=sa,ca \
    -rfc \
    -infile schemaregistry-server.csr \
    -outfile schemaregistry-server.pem


### Import server cert chain into schemaregistry.server.keystore.jks


keytool -keystore schemaregistry.server.keystore.jks -storepass test_password -importcert -alias schemaregistry-server -noprompt -file schemaregistry-server.pem


### Import server cert chain into schemaregistry.server.truststore.jks


keytool -keystore schemaregistry.server.truststore.jks -storepass test_password -importcert -trustcacerts -noprompt -alias root -file root.pem
keytool -keystore schemaregistry.server.truststore.jks -storepass test_password -importcert -alias ca -file ca.pem


## Schema Registry Client

### Generate private keys (for schemaregistry-client)


keytool \
    -genkeypair \
    -alias schemaregistry-client \
    -dname cn=schemaregistry-client \
    -validity 18250 \
    -keyalg RSA \
    -keysize 4096 \
    -keystore schemaregistry.client.keystore.jks \
    -keypass test_password \
    -storepass test_password


### Generate a certificate for client signed by ca (root -> ca -> schemaregistry-client)



keytool \
    -keystore schemaregistry.client.keystore.jks \
    -storepass test_password \
    -certreq \
    -alias schemaregistry-client \
    -file schemaregistry-client.csr

keytool \
    -keystore ca.jks \
    -storepass test_password \
    -gencert \
    -alias ca \
    -ext ku:c=dig,keyEnc \
    -ext eku=sa,ca \
    -rfc \
    -infile schemaregistry-client.csr \
    -outfile schemaregistry-client.pem


### Import server cert chain into schemaregistry.client.keystore.jks


keytool -keystore schemaregistry.client.keystore.jks -storepass test_password -importcert -alias schemaregistry-client -noprompt -file schemaregistry-client.pem


### Import server cert chain into schemaregistry.client.truststore.jks


keytool -keystore schemaregistry.client.truststore.jks -storepass test_password -importcert -trustcacerts -noprompt -alias root -file root.pem
keytool -keystore schemaregistry.client.truststore.jks -storepass test_password -importcert -alias ca -file ca.pem


## Kafka Client

### Generate private keys (for kafka-client)


keytool \
    -genkeypair \
    -alias kafka-client \
    -dname cn=kafka-client \
    -validity 18250 \
    -keyalg RSA \
    -keysize 4096 \
    -keystore kafka.client.keystore.jks \
    -keypass test_password \
    -storepass test_password


### Generate a certificate for client signed by ca (root -> ca -> kafka-client)


keytool \
    -keystore kafka.client.keystore.jks \
    -storepass test_password \
    -certreq \
    -alias kafka-client \
    -file kafka-client.csr

keytool \
    -keystore ca.jks \
    -storepass test_password \
    -gencert \
    -alias ca \
    -ext ku:c=dig,keyEnc \
    -ext eku=sa,ca \
    -rfc \
    -infile kafka-client.csr \
    -outfile kafka-client.pem


### Import server cert chain into kafka.client.keystore.jks


keytool -keystore kafka.client.keystore.jks -storepass test_password -importcert -alias kafka-client -noprompt -file kafka-client.pem


### Import server cert chain into kafka.client.truststore.jks


keytool -keystore kafka.client.truststore.jks -storepass test_password -importcert -trustcacerts -noprompt -alias root -file root.pem
keytool -keystore kafka.client.truststore.jks -storepass test_password -importcert -alias ca -file ca.pem

