proto:
	gradlew.bat generateProto

build:
	gradlew.bat build

run:
	gradlew.bat bootRun

clean:
	gradlew.bat clean

.PHONY: proto build run clean