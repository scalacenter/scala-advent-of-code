# Scala Advent of Code 2021

Solutions in Scala for the annual [Advent of Code](https://adventofcode.com/) challenge. _Note: this repo is not affiliated with Advent of Code._

## Website

The [Scala Advent of Code](https://scalacenter.github.io/scala-advent-of-code/) website contains:
- some explanation of our solutions to [Advent of Code (adventofcode.com)](https://adventofcode.com/)
- more solutions from the community

## Setup

We use Visual Studio Code with Metals to write Scala code, and scala-cli to compile and run it.

You can follow these [steps](https://scalacenter.github.io/scala-advent-of-code/setup) to set up your environement.

### How to open in Visual Studio Code

After you clone the repository, open a terminal and run:
```
$ cd scala-advent-of-code/2021
$ scala-cli setup-ide src
$ mkdir input
$ code .
```

`code .` will open Visual Studio Code and start Metals.

### How to run a solution

First copy your input to the folder `scala-advent-of-code/2021/input`.

Next, in a terminal you can run:
```
$ cd scala-advent-of-code/2021
$ scala-cli . -M day1.part1
Compiling project (Scala 3.x.y, JVM)
Compiled project (Scala 3.x.y, JVM)
The solution is 1559
```

The result will likely be different for you, as inputs are different for each user.

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

## Contributing
- Please do not commit your puzzle inputs, we can not accept them as they are protected by copyright
