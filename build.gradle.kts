plugins {
	java
	id("org.springframework.boot") version "3.5.13"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.franchise"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// AWS SDK v2 BOM — pins all AWS SDK module versions
	implementation(platform("software.amazon.awssdk:bom:2.25.60"))

	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webflux")

	// AWS SDK v2 DynamoDB dependencies (versions managed by BOM)
	implementation("software.amazon.awssdk:dynamodb")
	implementation("software.amazon.awssdk:dynamodb-enhanced")
	implementation("software.amazon.awssdk:netty-nio-client")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.mockito:mockito-core")
	testImplementation("net.jqwik:jqwik:1.8.4")
	testImplementation("org.testcontainers:localstack:1.19.8")
	testImplementation("org.testcontainers:junit-jupiter:1.19.8")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
	useJUnitPlatform()

	testLogging {
        events("passed", "skipped", "failed")
    }
}
