# **mts-asn1**
[![Build Status](https://travis-ci.org/ericsson-mts/mts-asn1.svg?branch=master)](https://travis-ci.org/ericsson-mts/mts-asn1) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.ericsson.mts%3Amts-asn1&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.ericsson.mts%3Amts-asn1)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://github.com/ericsson-mts/mts-asn1/blob/master/LICENSE.txt)

## Description

**mts-asn1** project is designed to encode/decode data to/from binary of ASN.1 protocols. Data can be described in various 
dataformat (XML, JSON ...). It use [ANTLR](https://www.antlr.org/) to parse ASN.1 grammar. 

## Table of contents

This project is split in multiple module to facilitate extensibility

[mts-antlr]() : Generate ANTLR source file 

[mts-core]() : Parse grammar and give tools to other module

[mts-json]() : JSON dataformat

[mts-per]() : PER encoding/decoding

[mts-xml]() : XML dataformat

## Installation

Clone this repository and use it as a maven project

(Coming on Maven Central soon)

## Usage

See [here](https://github.com/ericsson-mts/mts-asn1/wiki/User-guide)

## DISCLAIMER

While we do our best to support protocols described in ASN.1 and (U)PER encoding, we cannot guarantee that our project 
is error free. mts-asn1 evolve depending of the needs Please consider to [report]() or [contributing]() if you want a new feature or revise an existing one.
