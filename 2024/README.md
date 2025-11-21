# Scala Advent of Code 2024

Solutions in Scala for the annual [Advent of Code (adventofcode.com)](https://adventofcode.com/) challenge.

_Note: this repo is not affiliated with Advent of Code._

See earlier editions:

- [2021](/2021/README.md)
- [2022](/2022/README.md)
- [2023](/2023/README.md)

## Website

The [Scala Advent of Code](https://scalacenter.github.io/scala-advent-of-code/) website contains:

- some explanation of our solutions to [Advent of Code](https://adventofcode.com/)
- more solutions from the community

## Setup

We use Visual Studio Code with Metals to write Scala code, and scala-cli to compile and run it.

You can follow these [steps](https://scalacenter.github.io/scala-advent-of-code/setup) to set up your environement.

### How to open in Visual Studio Code

After you clone the repository, open a terminal and run:
```
$ cd scala-advent-of-code
$ scala-cli setup-ide 2024
$ mkdir 2024/input
$ code 2024
```

`code 2024` will open Visual Studio Code and start Metals. If not you may have to go to the Metals pane and click
the button labelled "Start Metals".

When you navigate to a file, e.g. `2024/src/day01.scala` metals should index the project, and then display code lenses
above each of the main methods `part1` and `part2`, as shown in this image:
![](../img/code-lenses.png)

To run a solution, first copy your input to the folder `2024/input`.
Then click `run` in VS Code which will run the code and display the results of the program. Or `debug`,
which will let you pause on breakpoints, and execute expressions in the debug console.

### How to run a solution with command line

To run a solution, first copy your input to the folder `2024/input`.

In a terminal you can run:
```
$ scala-cli 2024 -M day01.part1
Compiling project (Scala 3.x.y, JVM)
Compiled project (Scala 3.x.y, JVM)
The solution is 64929
```

Or, to run another solution:
```
$ scala-cli 2024 -M <dayX>.<partX>
```

## Contributing

- Please do not commit your puzzle inputs, we can not accept them as they are protected by copyright
