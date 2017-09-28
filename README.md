# UniLines
Universal Data-Conversion Pipelines

[![Build Status](https://api.travis-ci.org/shaposhnyk/unilines.svg?branch=master)](https://travis-ci.org/rest-assured/rest-assured)

## Motivation
 Several projects, I was working on, were exposing third-system's data over REST interfaces using JSON or XML.
Most of the projects had following particularities:
- input objects with hundreds of fields, usually in proprietary formats or highly generic ones, like JCR
- output objects with only tens of fields, usually in JSON or XML
- output interfaces must have auto-documented schemas/API, using XSD or Swagger
- at initial stages field mapping schema may vary a lot
- occasional data-cleaning or normalization, 
like sub-object creation or transforming a value to uppercase

At some point I saw how all the code I write for data-conversion purposes is same, 
so I generalized it, creating this library: 
a generic pipeline for structured data-processing. 

## Does UniLines fits your needs?

If you need to convert some data 
and answer most of the following questions positively, 
than you definitely should give a try to UniLines:
- has your input objects numerous fields, contrary to the output objects?
- have you tens of object types to convert, some of them quite similar?
- do you need to auto-document your API?

## Underlying technologies
This library is written in Kotlin, but is intended to be used from Java. 
It's only external dependency is kotlin stdlib. 
It supposes quite heavy use of lambdas, so Java 8 is highly recommended.

## Examples