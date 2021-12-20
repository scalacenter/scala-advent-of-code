import Solver from "../../../../website/src/components/Solver.js"

# Day 20: Trench Map

## Puzzle description

https://adventofcode.com/2021/day/20

## Modeling and parsing the input

The input is an image enhancement algorithm string, and an initial input image.

The image is a black and white rectangle. We model the pixels with an 
enumeration:

~~~ scala
enum Pixel:
  case Lit, Dark
~~~

A pixel can either be lit, or dark.

In the input text, lit pixels are represented by the character "#", whereas
dark pixels are represented by the character "." (dot). We use pattern 
matching to parse them:

~~~
object Pixel:
  def parse(char: Char): Pixel =
    char match
      case '#' => Pixel.Lit
      case '.' => Pixel.Dark
end Pixel
~~~

In case the input is malformed (ie, it contains a character other than "#" 
or "."), the method `parse` raises an exception.

The enhancement algorithm string is provided as a line of 512 pixels. We 
parse it as follows:

~~~ scala
class Enhancer(enhancementString: IndexedSeq[Pixel])

object Enhancer:
  def parse(input: String): Enhancer =
    Enhancer(input.map(Pixel.parse))
end Enhancer
~~~

There is a subtlety regarding the input image to which we want to apply the 
enhancement algorithm: its size is infinite. Initially, its pixels are all 
dark except in the rectangle we are given as initial input. So, we model an 
image as a two-dimensional indexed sequence, and the color of all the pixels 
that are out of the bounds of that two-dimensional indexed sequence:

~~~ scala
class Image(pixels: IndexedSeq[IndexedSeq[Pixel]], outOfBoundsPixel: Pixel):
  require(pixels.map(_.length).distinct.size == 1, "All the rows must have the same length")
  
  val height = pixels.length
  val width  = pixels(0).length
~~~

Since there is no direct way to model two-dimensional collections in the 
standard library, we model the table of pixels as a collection of rows, 
where each row is a collection of pixels. We add the call to `require` to 
make sure that all the lines have the same length.

We parse the input image by parsing every line of the provided rectangular 
area, and by setting the color of the out-of-bounds pixels to `Dark`:

~~~ scala
object Image:
  def parse(input: String): Image =
    val pixels =
      input
        .linesIterator.map(line => line.map(Pixel.parse))
        .toIndexedSeq
    val outOfBoundsPixel = Pixel.Dark
    Image(pixels, outOfBoundsPixel)
end Image
~~~

Last, we parse both sections of the input with the following method:

~~~ scala
def parseEnhancerAndImage(input: String): (Enhancer, Image) =
  val enhancerAndImage = input.split("\n\n")
  val enhancer         = Enhancer.parse(enhancerAndImage(0))
  val image            = Image.parse(enhancerAndImage(1))
  (enhancer, image)
~~~

## Image enhancement algorithm

The image enhancement algorithm needs to compute an integer value for every 
location of the input image. The integer value is made of 9 bits, whose 
value is taken from the state of the 9 pixels around the location (a lit 
pixel means `1`, and a dark pixel means `0`).

That 9-bits integer value is then used as an index in the enhancement 
algorithm string to find the state of the output pixel at that location.

We implement the algorithm as a method of the class `Enhancer`:

~~~ scala

class Enhancer(enhancementString: IndexedSeq[Pixel]):

  def enhance(image: Image): Image =
    val pixels =
      for y <- -1 until (image.height + 1)
      yield
        for x <- -1 until (image.width + 1)
        yield enhancementString(locationValue(image, x, y))

    val outOfBoundsPixel =
      val value = if image.outOfBoundsPixel == Pixel.Dark then 0 else 511
      enhancementString(value)

    Image(pixels, outOfBoundsPixel)
  end enhance

end Enhancer
~~~

Since we look at the 9 pixels around every location, we look at locations 
outside the image rectangle where some of these 9 pixels overlap with the 
rectangle (hence the bounds `-1` and `height + 1` for the `y` coordinates).

The 9-bits integer value of each location is computed by an auxiliary method,
`locationValue`, which is shown below.

Last, we also compute the “enhancement” of the pixels that are out of the 
bounds of the image rectangle. Since these pixels are all the same, the 9 
pixels around any location out of the bounds of the rectangle will always be 
either all lit or all dark. The binary value corresponding to all pixels 
dark is `000000000`, which equals `0`, and the binary value corresponding to 
all pixels lit is `111111111`, which equals `511`.

Here is the implementation of the auxiliary method `locationValue`:

~~~ scala
def locationValue(image: Image, x: Int, y: Int): Int =
  var result = 0
  for
    yy <- (y - 1) to (y + 1)
    xx <- (x - 1) to (x + 1)
  do
    result = result << 1
    if image.pixel(xx, yy) == Pixel.Lit then
      result = result | 1
    end if
  end for
  result
end locationValue
~~~

We read the 9 pixels around the provided location, starting from the 
top-left corner, and going to the right. If a pixel is lit, we interpret it 
as a `1`. At every iteration, we shift the previously computed result one 
bit to the left before reading the new bit.

To read the pixel value in the image, we use a handy method `pixel`, defined 
in the class `Image`:

~~~ scala
def pixel(x: Int, y: Int): Pixel =
  if y < 0 || y >= height then outOfBoundsPixel
  else if x < 0 || x >= width then outOfBoundsPixel
  else pixels(y)(x)
~~~

This method implements the fact that the image has an infinite size. It 
checks the bounds of the location to access, and when the location is out of 
the bounds of the rectangle image, it returns the `outOfBoundsPixel` color 
of the image.

## Solution of part 1

We were asked to apply two times in a row the enhancement algorithm on the 
input image, and to compute the number of lit pixels in the output image:

~~~ scala
def part1(input: String): Int =
  val (enhancer, image0) = parseEnhancerAndImage(input)
  val image1 = enhancer.enhance(image0)
  val image2 = enhancer.enhance(image1)
  image2.countLitPixels()
~~~

The method `countLitPixels` is defined as follows in the class `Image`:

~~~ scala
class Image(pixels: IndexedSeq[IndexedSeq[Pixel]], outOfBoundsPixel: Pixel):
  def countLitPixels(): Int =
    pixels.view.flatten.count(_ == Pixel.Lit)
~~~

We flatten the rows of pixels into a single collection of pixels, and we 
count the lit pixels on it. The call to `view` before `flatten` allows us to 
traverse the rows of pixels without constructing the flattened collection.

## Full code for part 1

~~~ scala
def part1(input: String): Int =
  val (enhancer, image0) = parseEnhancerAndImage(input)
  val image1 = enhancer.enhance(image0)
  val image2 = enhancer.enhance(image1)
  image2.countLitPixels()

def parseEnhancerAndImage(input: String): (Enhancer, Image) =
  val enhancerAndImage = input.split("\n\n")
  val enhancer         = Enhancer.parse(enhancerAndImage(0))
  val image            = Image.parse(enhancerAndImage(1))
  (enhancer, image)

enum Pixel:
  case Lit, Dark

object Pixel:

  def parse(char: Char): Pixel =
    char match
      case '#' => Pixel.Lit
      case '.' => Pixel.Dark

end Pixel

object Enhancer:

  def parse(input: String): Enhancer =
    Enhancer(input.map(Pixel.parse))

end Enhancer

class Enhancer(enhancementString: IndexedSeq[Pixel]):

  def enhance(image: Image): Image =
    val pixels =
      for y <- -1 until (image.height + 1)
      yield
        for x <- -1 until (image.width + 1)
        yield enhancementString(locationValue(image, x, y))

    val outOfBoundsPixel =
      val value = if image.outOfBoundsPixel == Pixel.Dark then 0 else 511
      enhancementString(value)

    Image(pixels, outOfBoundsPixel)
  end enhance

  private def locationValue(image: Image, x: Int, y: Int): Int =
    var result = 0
    for
      yy <- (y - 1) to (y + 1)
      xx <- (x - 1) to (x + 1)
    do
      result = result << 1
      if image.pixel(xx, yy) == Pixel.Lit then
        result = result | 1
      end if
    end for
    result
  end locationValue

end Enhancer

class Image(pixels: IndexedSeq[IndexedSeq[Pixel]], val outOfBoundsPixel: Pixel):

  require(pixels.map(_.length).distinct.size == 1, "All the rows must have the same length")

  val height = pixels.length
  val width  = pixels(0).length

  def pixel(x: Int, y: Int): Pixel =
    if y < 0 || y >= height then outOfBoundsPixel
    else if x < 0 || x >= width then outOfBoundsPixel
    else pixels(y)(x)

  def countLitPixels(): Int =
    pixels.view.flatten.count(_ == Pixel.Lit)

end Image

object Image:
  def parse(input: String): Image =
    val pixels =
      input
        .linesIterator.map(line => line.map(Pixel.parse))
        .toIndexedSeq
    val outOfBoundsPixel = Pixel.Dark
    Image(pixels, outOfBoundsPixel)
~~~

## Solution for part 2

In part 2, we had to apply the “enhancement algorithm” 50 times instead
of just twice:

~~~ scala
def part2(input: String): Int =
  val (enhancer, image) = parseEnhancerAndImage(input)
  LazyList
    .iterate(image)(enhancer.enhance)
    .apply(50)
    .countLitPixels()
~~~

We parse the input with the same method as in part1, `parseEnhancerAndImage`.

To apply the enhancer 50 times, we use a `LazyList`. First, we create an 
_inifinite_ lazy list whose first element is the parsed input `image`, and 
whose `n + 1` element is computed by calling `enhancer.enhance` on the 
element `n`. Then, we compute its 50th element by calling `.apply(50)`. As a 
consequence, only the first 50 elements will be computed at all.

Finally, we call `countLitPixels()` on the output image to count its number 
of lit pixels.

## Run it in the browser

### Part 1

<Solver puzzle="day20-part1"/>

### Part 2

<Solver puzzle="day20-part2"/>

## Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd advent-of-code
```

You can run it with [scala-cli](https://scala-cli.virtuslab.org/).

```
$ scala-cli src -M day20.part1
The solution is: 5301
$ scala-cli src -M day20.part2
The solution is: 19492
```

## Solutions from the community

Share your solution to the Scala community by editing this page.
