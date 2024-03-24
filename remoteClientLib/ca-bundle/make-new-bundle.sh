#!/bin/bash

perl mk-ca-bundle.pl
cat my-ca.crt >> ca-bundle.crt
cp ca-bundle.crt ../assets/ca-bundle.crt
