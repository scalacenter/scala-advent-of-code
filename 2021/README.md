# Scala Advent of Code 2021

Scala Center's solutions of [Advent of Code](https://adventofcode.com/).

## Website

The [Scala Advent of Code](https://scalacenter.github.io/scala-advent-of-code/) website contains:
- some explanation of our solutions
- more solutions from the community

## Setup

We use Visual Studio Code with Metals to write Scala code, and scala-cli to compile and run it.

You can follow these [steps](https://scalacenter.github.io/scala-advent-of-code/setup) to set up your environement.

### How to open in Visual Studio Code

After you clone the repository, open a terminal and run:
```
$ cd scala-advent-of-code/2021
$ scala-cli setup-ide src
$ code .
```

`code .` will open Visual Studio Code and start Metals.

### How to run a solution

In a terminal you can run:
```
$ cd scala-advent-of-code/2021
$ scala-cli . -M day1.part1
Compiling project (Scala 3.2.0, JVM)
Compiled project (Scala 3.2.0, JVM)
The solution is 1559
```

Or, to run another solution:
```
$ scala-cli . -M <dayX>.<partX>
```

By default the solution programs run on our input files which are stored in the `input` folder.
To get your solutions you can change the content of those files in the `input` folder.


#### How to run day3

The solution of day 3 is written for the javascript target.
You can run it locally, if you have [Node.js](https://nodejs.org/en/) installed, by adding the `--js` option:
```
$ scala-cli . --js -M day3.part1
```
