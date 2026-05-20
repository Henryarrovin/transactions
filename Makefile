GOOGLEAPIS_DIR := .cache/googleapis

$(GOOGLEAPIS_DIR):
	git clone https://github.com/googleapis/googleapis.git $(GOOGLEAPIS_DIR)

proto-pb: $(GOOGLEAPIS_DIR)
	mkdir -p src/main/proto/transactionpb
	protoc \
		-I src/main/proto \
		-I $(GOOGLEAPIS_DIR) \
		--go_out=proto/transactionpb \
		--go-grpc_out=proto/transactionpb \
		--grpc-gateway_out=proto/transactionpb \
		src/main/proto/transaction.proto

build:
	gradlew.bat build

run:
	gradlew.bat bootRun

clean:
	gradlew.bat clean

.PHONY: proto proto-pb build run clean