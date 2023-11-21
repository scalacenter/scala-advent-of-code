package locations

import scala.quoted.*

object Directory:

  /** The absolute path of the parent directory of the file that calls this method
    * This is stable no matter which directory runs the program.
    */
  inline def currentDir: String = ${ parentDirImpl }

  private def parentDirImpl(using Quotes): Expr[String] =
    // position of the call to `currentDir` in the source code
    val position = quotes.reflect.Position.ofMacroExpansion
    // get the path of the file calling this macro
    val srcFilePath = position.sourceFile.getJPath.get
    // get the parent of the path, which is the directory containing the file
    val parentDir = srcFilePath.getParent().toAbsolutePath
    Expr(parentDir.toString) // convert the String to Expr[String]
