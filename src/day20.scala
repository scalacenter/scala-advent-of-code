// using scala 3.1.0

package day20

import scala.util.Using
import scala.io.Source
import scala.annotation.tailrec

@main def part1(): Unit =
  println(s"The solution is ${part1(readInput())}")

@main def part2(): Unit =
  println(s"The solution is ${part2(readInput())}")

def readInput(): String =
  Using.resource(Source.fromFile("input/day20"))(_.mkString)

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

def part2(input: String): Int =
  val (enhancer, image) = parseEnhancerAndImage(input)
  LazyList
    .iterate(image)(enhancer.enhance)
    .apply(50)
    .countLitPixels()
