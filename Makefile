MODULE = $(shell go list -m)
VERSION ?= $(shell git describe --tags --always --dirty --match=v* 2> /dev/null || echo "1.0.0")
SERVICES := $(shell ls . | grep service)

.PHONY: default
default: help

# generate help info from comments: thanks to https://marmelab.com/blog/2016/02/29/auto-documented-makefile.html
.PHONY: help
help: ## help information about make commands
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

.PHONY: test
test: ## run unit tests
	@$(foreach service,$(SERVICES), \
		echo "Running tests for $(service)..." && \
		cd $(service) && \
		echo "test" \

.PHONY: run
run: ## run the API server
	@echo "Running the API server..."

.PHONY: build
build:  ## build the API server binary
	echo "Building the API server..."

.PHONY: build-docker
build-docker: ## build the API server as a docker image
	docker build -f docker-cdockerfile -t $(MODULE):$(VERSION) .

.PHONY: clean
clean: ## remove temporary files
	rm -rf db .fortify

.PHONY: version
version: ## display the version of the API server
	@echo $(VERSION)

.PHONY: db-start
db-start: ## start the database server
	docker compose -f 'docker-compose.yml' up -d --build 'nosql-db'

.PHONY: db-stop
db-stop: ## stop the database server
	docker stop nosql-db

.PHONY: testdata
testdata: ## populate the database with test data
#	#make migrate-reset
	@echo "Populating test data..."
#	@docker exec -it postgres psql "$(APP_DSN)" -f /testdata/testdata.sql

PHONY: swagger
swagger: ## generate swagger documentation
	@echo "Generating swagger documentation..."

.PHONY: sast-scan
sast-scan: ## run static application security testing
##	gosec -exclude=G104 ./...
	@echo "Running static application security testing..."
	@sourceanalyzer "-Dcom.fortify.sca.ProjectRoot=.fortify" -b "iwa-microservices" -clean
	@sourceanalyzer "-Dcom.fortify.sca.ProjectRoot=.fortify" -b "iwa-microservices" $(SERVICES) -verbose -debug
	@sourceanalyzer "-Dcom.fortify.sca.ProjectRoot=.fortify" -b "iwa-microservices" -verbose -debug -scan -rules etc/sast-custom-rules/example-custom-rules.xml 

