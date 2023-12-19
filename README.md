# Scala Advent of Code

Scala Center's solutions of [Advent of Code](https://adventofcode.com/).

## Prerequisites

Node.js v18.12.1+ (LTS)

## Building the website for development

First check out the git submodules necessary
```text
$ git submodule sync
Synchronizing submodule url for 'solutions'

$ git submodule update --init
```

then check you have the `solutions` directory added.

To run the website you need to do the following

```
$ sbtn "docs / mdoc"
$ cd website
$ yarn install
$ yarn start
```
