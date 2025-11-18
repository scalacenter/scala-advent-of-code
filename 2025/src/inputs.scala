package inputs

import scala.util.Using
import scala.io.Source

object Input:

  def loadFileSync(path: String): String =
    Using.resource(Source.fromFile(path))(_.mkString)
