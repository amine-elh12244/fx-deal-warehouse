.PHONY: help build test coverage run clean docker-up docker-down docker-logs k6-test up test-api

help:
	@echo "Available commands:"
	@echo "  make build       - Build the application"
	@echo "  make test        - Run tests with coverage"
	@echo "  make test-api    - Run API tests only"
	@echo "  make coverage    - View coverage report"
	@echo "  make run         - Run application locally"
	@echo "  make up          - Complete setup (docker-up + wait)"
	@echo "  make docker-up   - Start Docker containers"
	@echo "  make docker-down - Stop Docker containers"
	@echo "  make docker-logs - View Docker logs"
	@echo "  make k6-test     - Run K6 performance tests"
	@echo "  make clean       - Clean build artifacts"

build:
	@echo "Building application..."
	.\mvnw.cmd clean package -DskipTests

test:
	@echo "Running tests with coverage..."
	.\mvnw.cmd clean test
	@echo "Coverage report: target/site/jacoco/index.html"

test-api:
	@echo "Running API tests..."
	.\mvnw.cmd test -Dtest=FxDealApiTest

coverage:
	@echo "Opening coverage report..."
	@powershell -Command "Start-Process target\site\jacoco\index.html"

up:
	@echo "Complete setup - Starting all services..."
	docker-compose up --build -d
	@echo "Waiting for services to be ready..."
	@powershell -Command "Start-Sleep -Seconds 15"
	@echo "Application is running at http://localhost:8080"
	@echo "Run 'make test' to execute tests"
	@echo "Run 'make k6-test' for performance tests"

run:
	@echo "Running application..."
	.\mvnw.cmd spring-boot:run

docker-up:
	@echo "Starting Docker containers..."
	docker-compose up --build -d
	@echo "Waiting for services to be ready..."
	@powershell -Command "Start-Sleep -Seconds 10"
	@echo "Application is running at http://localhost:8080"

docker-down:
	@echo "Stopping Docker containers..."
	docker-compose down

docker-logs:
	docker-compose logs -f

k6-test:
	@echo "Running K6 performance tests..."
	@powershell -Command "if (Get-Command k6 -ErrorAction SilentlyContinue) { k6 run k6/load-test.js } else { Write-Host 'K6 not installed. Please install from https://k6.io/docs/getting-started/installation/' -ForegroundColor Yellow; Write-Host 'Alternative: Run tests manually after installing K6' -ForegroundColor Yellow; exit 0 }"

clean:
	@echo "Cleaning build artifacts..."
	.\mvnw.cmd clean
