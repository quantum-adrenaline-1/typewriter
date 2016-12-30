package files.assets

import java.io.File
import java.nio.file.Paths

import files.FileCrawler.{Failure, Result, Success}

import scala.concurrent.{Await, ExecutionContext, Future}
import com.google.javascript.jscomp.{Compiler, CompilerOptions, JSSourceFile}
import files.FileIO

import scala.concurrent.duration.Duration


object JavascriptCompiler {

  def compile(javascriptDirectory: String, outputPath: String)(implicit ec: ExecutionContext): Future[Result] = {
    Future {
      val compiler = new Compiler

      val files = FileIO.files(javascriptDirectory, isJavascriptFile)

      val result = compiler.compile(Array.empty[JSSourceFile], files.map(JSSourceFile.fromFile).toArray, new CompilerOptions)

      val warnings = result.warnings
      val errors = result.errors

      warnings.foreach(println)

      if (errors.nonEmpty) {
        val msg = errors.map(_.toString).reduce(_ + "\n" + _)
        println(msg)
        Failure(msg)
      } else {

        val source = compiler.toSource
        val writeFuture = FileIO.write(source, outputPath).map(_ => Success)
        Await.result(writeFuture, Duration.Inf)
      }
    }
  }

  private def isJavascriptFile(file: File): Boolean = {
    FileIO.extension(file.getName).getOrElse("") == "js"
  }
}