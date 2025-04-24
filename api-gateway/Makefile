include ../lib/makefile-go.defs
-include .env

ROOT_DIR := $(abspath $(dir $(lastword $(MAKEFILE_LIST)))/..)

PROJECT := api-gateway

SAST_TRANSLATE_OPTS := -verbose -debug -exclude vendor "**/*.go"
