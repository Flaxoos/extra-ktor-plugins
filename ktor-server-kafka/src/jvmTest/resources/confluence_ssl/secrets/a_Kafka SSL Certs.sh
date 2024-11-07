#!/bin/bash

set -o nounset \
    -o errexit \
    -o verbose \
    -o xtrace

# === 1. Cleanup Previously Generated Files ===
echo "=== Cleaning Up Previously Generated Files ==="

# Define the name of this script to exclude it from deletion
SCRIPT_NAME="a_Kafka_SSL_Certs.sh"

# Find and delete all files in the current directory except the script itself
find . -maxdepth 1 -type f ! -name "$SCRIPT_NAME" -exec rm -f {} +

echo "=== Cleanup Completed ==="

# === 2. Define Common Variables ===
DAYS=3650
PASSWORD="flaxoos"
CA_CN="ca1.test.flaxoos.io"
KEY_ALG="RSA"
KEY_SIZE=2048

# === 3. Generate CA Key and Certificate ===
echo "=== Generating CA Key and Certificate ==="
openssl req -new -x509 -keyout snakeoil-ca-1.key -out snakeoil-ca-1.crt -days $DAYS \
  -subj "/CN=$CA_CN/OU=TEST/O=FLAXOOS/L=PaloAlto/ST=CA/C=US" \
  -passin pass:$PASSWORD -passout pass:$PASSWORD

# === 4. Generate Keystores and Truststores for Each Kafka Component ===
echo "=== Generating Keystores and Truststores ==="

# List of Kafka components
# Consolidate admin, producer, consumer, ktor into backend
components=("broker" "backend" "schemaregistry")

for i in "${components[@]}"
do
    # Set CN and SAN based on the component
    case $i in
        broker)
            CN="localhost"
            SAN="DNS:localhost,DNS:kafka"
            ;;
        schemaregistry)
            CN="schemaregistry"
            SAN="DNS:schemaregistry,DNS:localhost"
            ;;
        backend)
            CN="backend"
            SAN="DNS:backend,DNS:localhost"
            ;;
        *)
            CN="unknown"
            SAN="DNS:unknown"
            ;;
    esac

    echo "Processing $i with CN=$CN and SAN=$SAN"

    # Create keystore with private key
    keytool -genkey -noprompt \
        -alias "$i" \
        -dname "CN=$CN, OU=TEST, O=FLAXOOS, L=PaloAlto, ST=CA, C=US" \
        -keystore "kafka.$i.keystore.jks" \
        -keyalg "$KEY_ALG" \
        -keysize "$KEY_SIZE" \
        -storetype PKCS12 \
        -storepass "$PASSWORD" \
        -keypass "$PASSWORD"

    # Create CSR (Certificate Signing Request)
    keytool -keystore "kafka.$i.keystore.jks" -alias "$i" -certreq -file "$i.csr" \
        -storepass "$PASSWORD" -keypass "$PASSWORD"

    # Generate an extension file with SAN entries
    echo "subjectAltName=$SAN" > "$i.ext"

    # Sign the CSR with the CA, including SAN
    openssl x509 -req -CA snakeoil-ca-1.crt -CAkey snakeoil-ca-1.key -in "$i.csr" \
        -out "$i-ca1-signed.crt" -days $DAYS -CAcreateserial -passin pass:$PASSWORD \
        -extfile "$i.ext"

    # Import the CA certificate into keystore
    keytool -keystore "kafka.$i.keystore.jks" -alias CARoot -import -file snakeoil-ca-1.crt \
        -storepass "$PASSWORD" -keypass "$PASSWORD" -noprompt

    # Import the signed certificate into keystore
    keytool -keystore "kafka.$i.keystore.jks" -alias "$i" -import -file "$i-ca1-signed.crt" \
        -storepass "$PASSWORD" -keypass "$PASSWORD" -noprompt

    # Create truststore and import the CA certificate
    keytool -keystore "kafka.$i.truststore.jks" -alias CARoot -import -file snakeoil-ca-1.crt \
        -storepass "$PASSWORD" -keypass "$PASSWORD" -storetype PKCS12 -noprompt

    # Import the broker's certificate into backend and schemaregistry's truststores
    if [ "$i" != "broker" ]; then
        # Export broker's certificate (if not already exported)
        if [ ! -f broker.cer ]; then
            keytool -exportcert -alias broker \
                -keystore kafka.broker.keystore.jks \
                -file broker.cer \
                -storepass "$PASSWORD"
        fi
        # Import broker's certificate into client's truststore
        keytool -keystore "kafka.$i.truststore.jks" \
            -alias broker \
            -import -file broker.cer \
            -storepass "$PASSWORD" \
            -noprompt
    fi

    # Create credentials files
    echo "$PASSWORD" > "${i}_sslkey_creds"
    echo "$PASSWORD" > "${i}_keystore_creds"
    echo "$PASSWORD" > "${i}_truststore_creds"

    # Clean up temporary files
    rm -f "$i.csr" "$i.ext" "$i-ca1-signed.crt"
done

# === 5. Import Clients' Certificates into Broker's Truststore ===
echo "=== Importing Clients' Certificates into Broker's Truststore ==="
# Since we've consolidated clients into 'backend', import 'backend' cert into broker's truststore
keytool -exportcert -alias backend \
    -keystore "kafka.backend.keystore.jks" \
    -file backend.cer \
    -storepass "$PASSWORD"

keytool -keystore kafka.broker.truststore.jks \
    -alias backend \
    -import -file backend.cer \
    -storepass "$PASSWORD" \
    -noprompt

# Clean up client certificate file
rm -f backend.cer

# === 6. Final Cleanup ===
echo "=== Final Cleanup ==="
rm -f *.srl

echo "=== Script Execution Completed Successfully ==="