GOOGLEAPIS_DIR := .cache/googleapis

$(GOOGLEAPIS_DIR):
	git clone https://github.com/googleapis/googleapis.git $(GOOGLEAPIS_DIR)

proto-pb: $(GOOGLEAPIS_DIR)
	gradlew.bat generateProto

build:
	gradlew.bat build

run:
	cd scripts && ./start.sh

clean:
	gradlew.bat clean

.PHONY: proto proto-pb build run clean