# mts-asn1 [![Build Status](https://travis-ci.org/ericsson-mts/mts-asn1.svg?branch=master)](https://travis-ci.org/ericsson-mts/mts-asn1)

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

## License

This project is licensed under the terms of the MIT license.
