plugins {
	java
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.google.protobuf") version "0.9.4"
}

group = "com.henry"
version = "0.0.1-SNAPSHOT"

val grpcVersion = "1.81.0"
val protobufVersion = "3.25.3"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-kafka")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-validation:4.1.0-M3")
	implementation("org.hibernate:hibernate-core:7.3.4.Final")
	implementation("org.springframework.data:spring-data-jpa:4.1.0-M2")
	implementation("org.flywaydb:flyway-database-postgresql")
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-kafka-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
	implementation("com.google.api.grpc:proto-google-common-protos:2.71.0")
	implementation("io.grpc:grpc-netty-shaded:${grpcVersion}")
	implementation("io.grpc:grpc-protobuf:${grpcVersion}")
	implementation("io.grpc:grpc-stub:${grpcVersion}")
	implementation("net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE")
	implementation("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE")
}

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:$protobufVersion"
	}
	plugins {
		create("grpc") {
			artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
		}
	}
	generateProtoTasks {
		all().forEach {
			it.plugins {
				create("grpc")
			}
		}
	}
}

sourceSets {
	main {
		proto {
			srcDir("src/main/proto")
		}
		java {
			srcDirs("build/generated/source/proto/main/grpc")
			srcDirs("build/generated/source/proto/main/java")
		}
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
