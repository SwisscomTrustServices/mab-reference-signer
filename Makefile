.PHONY: run build test clean

.DEFAULT_GOAL := run

run:
	./gradlew bootRun

build:
	./gradlew build

test:
	./gradlew test

clean:
	./gradlew clean
