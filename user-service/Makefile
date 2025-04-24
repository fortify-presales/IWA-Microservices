include ../lib/makefile-node.defs
-include .env

ROOT_DIR := $(abspath $(dir $(lastword $(MAKEFILE_LIST)))/..)

PROJECT := user-service

SAST_TRANSLATE_OPTS := -verbose -debug -exclude "**/package.json" .
